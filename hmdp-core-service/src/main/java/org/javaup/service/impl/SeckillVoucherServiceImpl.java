package org.javaup.service.impl;

import cn.hutool.core.date.LocalDateTimeUtil;
import org.javaup.cache.SeckillVoucherLocalCache;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.javaup.core.RedisKeyManage;
import org.javaup.entity.SeckillVoucher;
import org.javaup.handler.BloomFilterHandlerFactory;
import org.javaup.mapper.SeckillVoucherMapper;
import org.javaup.redis.RedisCache;
import org.javaup.redis.RedisKeyBuild;
import org.javaup.service.ISeckillVoucherService;
import org.javaup.servicelock.LockType;
import org.javaup.util.ServiceLockTool;
import org.redisson.api.RLock;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static org.javaup.constant.Constant.BLOOM_FILTER_HANDLER_VOUCHER;
import static org.javaup.utils.RedisConstants.CACHE_NULL_TTL;
import static org.javaup.utils.RedisConstants.LOCK_SECKILL_VOUCHER_KEY;

/**
 * <p>
 * 秒杀优惠券表，与优惠券是一对一关系 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2022-01-04
 */
@Slf4j
@Service
public class SeckillVoucherServiceImpl extends ServiceImpl<SeckillVoucherMapper, SeckillVoucher> implements ISeckillVoucherService {
    
    @Resource
    private ServiceLockTool serviceLockTool;
    
    @Resource
    private RedisCache redisCache;
    
    @Resource
    private BloomFilterHandlerFactory bloomFilterHandlerFactory;

    @Resource
    private SeckillVoucherLocalCache seckillVoucherLocalCache;

    
    @Override
    public SeckillVoucher queryByVoucherId(Long voucherId) {
        // 先查本地缓存，命中则直接返回
        SeckillVoucher localCacheHit = seckillVoucherLocalCache.get(voucherId);
        if (Objects.nonNull(localCacheHit)) {
            return localCacheHit;
        }
        // 双重检测解决缓存击穿
        SeckillVoucher seckillVoucher =
                redisCache.get(RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_VOUCHER_TAG_KEY, voucherId), SeckillVoucher.class);
        // 如果缓存中存在就直接返回
        if (Objects.nonNull(seckillVoucher)) {   
            // 写入本地缓存，加快后续访问
            seckillVoucherLocalCache.put(voucherId, seckillVoucher);
            return seckillVoucher;
        }
        log.info("查询秒杀优惠券 从Redis缓存没有查询到 秒杀优惠券的优惠券id : {}",voucherId);
        // 通过布隆过滤器判断是否存在
        if (!bloomFilterHandlerFactory.get(BLOOM_FILTER_HANDLER_VOUCHER).contains(String.valueOf(voucherId))) {
            log.info("查询秒杀优惠券 布隆过滤器判断不存在 秒杀优惠券id : {}",voucherId);
            throw new RuntimeException("查询秒杀优惠券不存在");
        }
        // 解决缓存穿透，从缓存中判断是否存在商铺空值信息，如果有，代表商铺不存在，直接返回
        Boolean existResult = redisCache.hasKey(RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_VOUCHER_NULL_TAG_KEY, voucherId));
        if (existResult){
            throw new RuntimeException("查询秒杀优惠券不存在");
        }
        // 实现双重检测
        // 加锁，解决缓存击穿
        RLock lock = serviceLockTool.getLock(LockType.Reentrant, LOCK_SECKILL_VOUCHER_KEY, new String[]{String.valueOf(voucherId)});
        lock.lock();
        try {
            // 再次从缓存中判断是否存在商铺空值信息，如果有，代表商铺不存在，直接返回
            existResult = redisCache.hasKey(RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_VOUCHER_NULL_TAG_KEY, voucherId));
            if (existResult){
                throw new RuntimeException("查询商铺不存在");
            }
            // 再次从缓存中获取商铺信息，通过此步骤可以避免大量请求在获取锁后，直接击穿缓存访问数据库
            seckillVoucher = redisCache.get(RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_VOUCHER_TAG_KEY, voucherId), SeckillVoucher.class);
            // 如果缓存中存在就直接返回
            if (Objects.nonNull(seckillVoucher)) {
                return seckillVoucher;
            }
            // 如果缓存还不存在，查询数据库
            seckillVoucher = lambdaQuery().eq(SeckillVoucher::getVoucherId,voucherId).one();
            // 如果从数据库查询是空的，将空值写入redis
            if (Objects.isNull(seckillVoucher)) {
                redisCache.set(RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_VOUCHER_NULL_TAG_KEY, voucherId),
                        "这是一个空值",
                        CACHE_NULL_TTL,
                        TimeUnit.MINUTES);
                throw new RuntimeException("查询秒杀优惠券不存在");
            }
            // 如果数据库查询不是空的，将秒杀优惠券信息写入缓存，TTL为距离结束时间的秒数
            long ttlSeconds = Math.max(
                    LocalDateTimeUtil.between(LocalDateTimeUtil.now(), seckillVoucher.getEndTime()).getSeconds(),
                    1L
            );
            // 保存秒杀优惠券库存到Redis中（单槽位Hash Tag键）
            redisCache.set(
                    RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_STOCK_TAG_KEY, voucherId),
                    String.valueOf(seckillVoucher.getStock()),
                    ttlSeconds,
                    TimeUnit.SECONDS
            );
            // 保存秒杀优惠券详情到Redis中（单槽位Hash Tag键）
            seckillVoucher.setStock(null);
            redisCache.set(
                    RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_VOUCHER_TAG_KEY, voucherId),
                    seckillVoucher,
                    ttlSeconds,
                    TimeUnit.SECONDS
            );
            // 同步写入本地缓存
            seckillVoucherLocalCache.put(voucherId, seckillVoucher);
            return seckillVoucher;
        }finally {
            lock.unlock();
        }
    }
}
