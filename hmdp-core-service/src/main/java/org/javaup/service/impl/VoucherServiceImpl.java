package org.javaup.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.javaup.cache.SeckillVoucherCacheInvalidationPublisher;
import org.javaup.core.RedisKeyManage;
import org.javaup.dto.Result;
import org.javaup.dto.SeckillVoucherDto;
import org.javaup.dto.UpdateSeckillVoucherDto;
import org.javaup.dto.UpdateSeckillVoucherStockDto;
import org.javaup.dto.VoucherDto;
import org.javaup.dto.VoucherSubscribeBatchDto;
import org.javaup.dto.VoucherSubscribeDto;
import org.javaup.entity.SeckillVoucher;
import org.javaup.entity.Voucher;
import org.javaup.enums.BaseCode;
import org.javaup.enums.StockUpdateType;
import org.javaup.enums.SubscribeStatus;
import org.javaup.exception.HmdpFrameException;
import org.javaup.handler.BloomFilterHandlerFactory;
import org.javaup.mapper.VoucherMapper;
import org.javaup.redis.RedisCache;
import org.javaup.redis.RedisKeyBuild;
import org.javaup.service.ISeckillVoucherService;
import org.javaup.service.IVoucherService;
import org.javaup.servicelock.LockType;
import org.javaup.servicelock.annotion.ServiceLock;
import org.javaup.toolkit.SnowflakeIdGenerator;
import org.javaup.utils.UserHolder;
import org.javaup.vo.GetSubscribeStatusVo;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static org.javaup.constant.Constant.BLOOM_FILTER_HANDLER_VOUCHER;
import static org.javaup.constant.DistributedLockConstants.UPDATE_SECKILL_VOUCHER_LOCK;
import static org.javaup.utils.RedisConstants.SECKILL_STOCK_KEY;

/**
 * @program: 黑马点评-plus升级版实战项目。添加 阿星不是程序员 微信，添加时备注 点评 来获取项目的完整资料
 * @description: 优惠券 接口实现
 * @author: 阿星不是程序员
 **/
@Service
public class VoucherServiceImpl extends ServiceImpl<VoucherMapper, Voucher> implements IVoucherService {

    @Resource
    private ISeckillVoucherService seckillVoucherService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private SnowflakeIdGenerator snowflakeIdGenerator;
    @Resource
    private BloomFilterHandlerFactory bloomFilterHandlerFactory;
    @Resource
    private RedisCache redisCache;
    @Resource
    private SeckillVoucherCacheInvalidationPublisher seckillVoucherCacheInvalidationPublisher;
    
    @Override
    public Long addVoucher(VoucherDto voucherDto) {
        Voucher one = lambdaQuery().orderByDesc(Voucher::getId).one();
        long newId = 1L;
        if (one != null) {
            newId = one.getId() + 1;
        }
        Voucher voucher = new Voucher();
        BeanUtil.copyProperties(voucherDto, voucher);
        voucher.setId(newId);
        save(voucher);
        bloomFilterHandlerFactory.get(BLOOM_FILTER_HANDLER_VOUCHER).add(voucher.getId().toString());
        return voucher.getId();
    }
    
    @Override
    public Result<List<Voucher>> queryVoucherOfShop(Long shopId) {
        // 查询优惠券信息
        List<Voucher> vouchers = getBaseMapper().queryVoucherOfShop(shopId);
        // 返回结果
        return Result.ok(vouchers);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addSeckillVoucher(SeckillVoucherDto seckillVoucherDto) {
        //黑马点评v1版本
        //return doAddSeckillVoucherV1(seckillVoucherDto);
        //黑马点评v2版本
        return doAddSeckillVoucherV2(seckillVoucherDto);
    }
    
    @Override
    @ServiceLock(lockType= LockType.Write,name = UPDATE_SECKILL_VOUCHER_LOCK,keys = {"#updateSeckillVoucherDto.voucherId"})
    @Transactional(rollbackFor = Exception.class)
    public void updateSeckillVoucher(UpdateSeckillVoucherDto updateSeckillVoucherDto) {
        Long voucherId = updateSeckillVoucherDto.getVoucherId();
        // 更新 tb_voucher 表的非空字段
        boolean updatedVoucher = false;
        var voucherUpdate = this.lambdaUpdate().eq(Voucher::getId, voucherId);
        if (updateSeckillVoucherDto.getTitle() != null) {
            voucherUpdate.set(Voucher::getTitle, updateSeckillVoucherDto.getTitle());
            updatedVoucher = true;
        }
        if (updateSeckillVoucherDto.getSubTitle() != null) {
            voucherUpdate.set(Voucher::getSubTitle, updateSeckillVoucherDto.getSubTitle());
            updatedVoucher = true;
        }
        if (updateSeckillVoucherDto.getRules() != null) {
            voucherUpdate.set(Voucher::getRules, updateSeckillVoucherDto.getRules());
            updatedVoucher = true;
        }
        if (updateSeckillVoucherDto.getPayValue() != null) {
            voucherUpdate.set(Voucher::getPayValue, updateSeckillVoucherDto.getPayValue());
            updatedVoucher = true;
        }
        if (updateSeckillVoucherDto.getActualValue() != null) {
            voucherUpdate.set(Voucher::getActualValue, updateSeckillVoucherDto.getActualValue());
            updatedVoucher = true;
        }
        if (updateSeckillVoucherDto.getType() != null) {
            voucherUpdate.set(Voucher::getType, updateSeckillVoucherDto.getType());
            updatedVoucher = true;
        }
        if (updateSeckillVoucherDto.getStatus() != null) {
            voucherUpdate.set(Voucher::getStatus, updateSeckillVoucherDto.getStatus());
            updatedVoucher = true;
        }
        if (updatedVoucher) {
            voucherUpdate.set(Voucher::getUpdateTime, LocalDateTimeUtil.now()).update();
        }

        // 更新 tb_seckill_voucher 表的非空字段（仅时间相关）
        boolean updatedSeckill = false;
        var seckillUpdate = seckillVoucherService.lambdaUpdate().eq(SeckillVoucher::getVoucherId, voucherId);
        if (updateSeckillVoucherDto.getBeginTime() != null) {
            seckillUpdate.set(SeckillVoucher::getBeginTime, updateSeckillVoucherDto.getBeginTime());
            updatedSeckill = true;
        }
        if (updateSeckillVoucherDto.getEndTime() != null) {
            seckillUpdate.set(SeckillVoucher::getEndTime, updateSeckillVoucherDto.getEndTime());
            updatedSeckill = true;
        }
        // 受众规则字段更新
        if (updateSeckillVoucherDto.getAllowedLevels() != null) {
            seckillUpdate.set(SeckillVoucher::getAllowedLevels, updateSeckillVoucherDto.getAllowedLevels());
            updatedSeckill = true;
        }
        if (updateSeckillVoucherDto.getMinLevel() != null) {
            seckillUpdate.set(SeckillVoucher::getMinLevel, updateSeckillVoucherDto.getMinLevel());
            updatedSeckill = true;
        }
        if (updatedSeckill) {
            seckillUpdate.set(SeckillVoucher::getUpdateTime, LocalDateTimeUtil.now()).update();
        }

        // 更新后清理缓存，等待读路径按新数据重建缓存
        if (updatedVoucher || updatedSeckill) {
            seckillVoucherCacheInvalidationPublisher.publishInvalidate(voucherId, "update");
        }
    }
    
    @Override
    @ServiceLock(lockType= LockType.Write,name = UPDATE_SECKILL_VOUCHER_LOCK,keys = {"#updateSeckillVoucherDto.voucherId"})
    @Transactional(rollbackFor = Exception.class)
    public void updateSeckillVoucherStock(UpdateSeckillVoucherStockDto updateSeckillVoucherDto) {
        SeckillVoucher seckillVoucher = seckillVoucherService.lambdaQuery()
                .eq(SeckillVoucher::getVoucherId, updateSeckillVoucherDto.getVoucherId()).one();
        if (Objects.isNull(seckillVoucher)) {
            throw new HmdpFrameException(BaseCode.SECKILL_VOUCHER_NOT_EXIST);
        }
        Integer oldStock = seckillVoucher.getStock();
        Integer oldInitStock = seckillVoucher.getInitStock();
        Integer newInitStock = updateSeckillVoucherDto.getInitStock();
        int changeStock = newInitStock - oldInitStock;
        if (changeStock == 0) {
            return;
        }
        int newStock = oldStock + changeStock;
        if (newStock < 0 ) {
            throw new HmdpFrameException(BaseCode.AFTER_SECKILL_VOUCHER_REMAIN_STOCK_NOT_NEGATIVE_NUMBER);
        }
        Integer stockUpdateType = StockUpdateType.INCREASE.getCode();
        if (changeStock < 0) {
            stockUpdateType = StockUpdateType.DECREASE.getCode();
        }
        seckillVoucherService.lambdaUpdate()
                .set(SeckillVoucher::getStock, newStock)
                .set(SeckillVoucher::getInitStock, newInitStock)
                .set(SeckillVoucher::getUpdateTime, LocalDateTimeUtil.now())
                .update();
        String oldRedisStockStr = redisCache.get(RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_STOCK_TAG_KEY, 
                updateSeckillVoucherDto.getVoucherId()), String.class);
        if (StrUtil.isBlank(oldRedisStockStr)) {
            redisCache.set(RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_STOCK_TAG_KEY,
                    updateSeckillVoucherDto.getVoucherId()),String.valueOf(newInitStock));
        }else {
            int oldRedisStock = Integer.parseInt(oldRedisStockStr);
            int newRedisStock = oldRedisStock + changeStock;
            if (newRedisStock < 0 ) {
                throw new HmdpFrameException(BaseCode.AFTER_SECKILL_VOUCHER_REMAIN_STOCK_NOT_NEGATIVE_NUMBER);
            }
            redisCache.set(RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_STOCK_TAG_KEY,
                    updateSeckillVoucherDto.getVoucherId()),String.valueOf(newRedisStock));
        }
    }
    
    @Override
    public void subscribe(final VoucherSubscribeDto voucherSubscribeDto) {
        Long voucherId = voucherSubscribeDto.getVoucherId();
        Long userId = UserHolder.getUser().getId();
        String userIdStr = String.valueOf(userId);

        Long ttlSeconds = redisCache.getExpire(
                RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_VOUCHER_TAG_KEY, voucherId),
                TimeUnit.SECONDS
        );
        if (Objects.isNull(ttlSeconds) || ttlSeconds <= 0) {
            SeckillVoucher sv = seckillVoucherService.lambdaQuery()
                    .eq(SeckillVoucher::getVoucherId, voucherId)
                    .one();
            if (Objects.nonNull(sv) && Objects.nonNull(sv.getEndTime())) {
                ttlSeconds = Math.max(
                        LocalDateTimeUtil.between(LocalDateTimeUtil.now(), sv.getEndTime()).getSeconds(),
                        1L
                );
            } else {
                ttlSeconds = 3600L;
            }
        }

        boolean purchased = Boolean.TRUE.equals(redisCache.isMemberForSet(
                RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_USER_TAG_KEY, voucherId),
                userIdStr
        ));

        RedisKeyBuild statusKey = RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_SUBSCRIBE_STATUS_TAG_KEY, voucherId);
        if (purchased) {
            redisCache.putHash(statusKey, userIdStr, SubscribeStatus.SUCCESS.getCode(), ttlSeconds, TimeUnit.SECONDS);
            redisCache.expire(statusKey, ttlSeconds, TimeUnit.SECONDS);
            return;
        }

        // 加入订阅集合（SET），幂等
        RedisKeyBuild setKey = RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_SUBSCRIBE_USER_TAG_KEY, voucherId);
        Long added = redisCache.addForSet(setKey, userIdStr);
        redisCache.expire(setKey, ttlSeconds, TimeUnit.SECONDS);

        // 加入订阅队列（ZSET），仅首次加入时写入顺序分数
        RedisKeyBuild zsetKey = RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_SUBSCRIBE_ZSET_TAG_KEY, voucherId);
        if (Objects.nonNull(added) && added > 0) {
            redisCache.addForSortedSet(zsetKey, userIdStr, (double) System.currentTimeMillis(), ttlSeconds, TimeUnit.SECONDS);
        } else {
            // 已存在则仅对齐TTL
            redisCache.expire(zsetKey, ttlSeconds, TimeUnit.SECONDS);
        }

        // 更新订阅状态为 SUBSCRIBED（如已是 SUCCESS 则不降级）
        Integer prev = redisCache.getForHash(statusKey, userIdStr, Integer.class);
        if (!SubscribeStatus.SUCCESS.getCode().equals(prev)) {
            redisCache.putHash(statusKey, userIdStr, SubscribeStatus.SUBSCRIBED.getCode(), ttlSeconds, TimeUnit.SECONDS);
        }
        redisCache.expire(statusKey, ttlSeconds, TimeUnit.SECONDS);
    }
    
    @Override
    public void unsubscribe(final VoucherSubscribeDto voucherSubscribeDto) {
        Long voucherId = voucherSubscribeDto.getVoucherId();
        Long userId = UserHolder.getUser().getId();
        String userIdStr = String.valueOf(userId);

        RedisKeyBuild setKey = RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_SUBSCRIBE_USER_TAG_KEY, voucherId);
        RedisKeyBuild zsetKey = RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_SUBSCRIBE_ZSET_TAG_KEY, voucherId);
        RedisKeyBuild statusKey = RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_SUBSCRIBE_STATUS_TAG_KEY, voucherId);

        // 从订阅集合与队列移除
        redisCache.removeForSet(setKey, userIdStr);
        redisCache.delForSortedSet(zsetKey, userIdStr);

        // 已购则维持 SUCCESS，否则置为 UNSUBSCRIBED
        boolean purchased = Boolean.TRUE.equals(redisCache.isMemberForSet(
                RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_USER_TAG_KEY, voucherId),
                userIdStr
        ));
        Long ttlSeconds = redisCache.getExpire(
                RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_VOUCHER_TAG_KEY, voucherId),
                TimeUnit.SECONDS
        );
        if (ttlSeconds == null || ttlSeconds <= 0) {
            ttlSeconds = 3600L;
        }
        redisCache.putHash(
                statusKey, 
                userIdStr,
                purchased ? SubscribeStatus.SUCCESS.getCode() : SubscribeStatus.UNSUBSCRIBED.getCode(),
                ttlSeconds, TimeUnit.SECONDS);
        redisCache.expire(statusKey, ttlSeconds, TimeUnit.SECONDS);
    }
    
    @Override
    public Integer getSubscribeStatus(final VoucherSubscribeDto voucherSubscribeDto) {
        Long voucherId = voucherSubscribeDto.getVoucherId();
        Long userId = UserHolder.getUser().getId();
        String userIdStr = String.valueOf(userId);

        RedisKeyBuild statusKey = RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_SUBSCRIBE_STATUS_TAG_KEY, voucherId);
        Integer st = redisCache.getForHash(statusKey, userIdStr, Integer.class);
        if (st != null) {
            return st;
        }

        // 先判断是否已购
        boolean purchased = Boolean.TRUE.equals(redisCache.isMemberForSet(
                RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_USER_TAG_KEY, voucherId),
                userIdStr
        ));
        if (purchased) {
            Long ttlSeconds = redisCache.getExpire(
                    RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_VOUCHER_TAG_KEY, voucherId),
                    TimeUnit.SECONDS
            );
            if (ttlSeconds == null || ttlSeconds <= 0) {
                ttlSeconds = 3600L;
            }
            redisCache.putHash(statusKey, userIdStr, SubscribeStatus.SUCCESS.getCode(), ttlSeconds, TimeUnit.SECONDS);
            redisCache.expire(statusKey, ttlSeconds, TimeUnit.SECONDS);
            return SubscribeStatus.SUCCESS.getCode();
        }

        // 判断是否在订阅集合
        boolean inQueue = Boolean.TRUE.equals(redisCache.isMemberForSet(
                RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_SUBSCRIBE_USER_TAG_KEY, voucherId),
                userIdStr
        ));
        return inQueue ? SubscribeStatus.SUBSCRIBED.getCode() : SubscribeStatus.UNSUBSCRIBED.getCode();
    }
    
    @Override
    public List<GetSubscribeStatusVo> getSubscribeStatusBatch(final VoucherSubscribeBatchDto voucherSubscribeBatchDto) {
        Long userId = UserHolder.getUser().getId();
        String userIdStr = String.valueOf(userId);
        List<GetSubscribeStatusVo> res = new ArrayList<>();
        for (Long voucherId : voucherSubscribeBatchDto.getVoucherIdList()) {
            // 优先使用HASH缓存
            RedisKeyBuild statusKey = RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_SUBSCRIBE_STATUS_TAG_KEY, voucherId);
            Integer st = redisCache.getForHash(statusKey, userIdStr, Integer.class);
            if (st != null) {
                res.add(new GetSubscribeStatusVo(voucherId, st));
                continue;
            }
            boolean purchased = Boolean.TRUE.equals(redisCache.isMemberForSet(
                    RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_USER_TAG_KEY, voucherId),
                    userIdStr
            ));
            if (purchased) {
                Long ttlSeconds = redisCache.getExpire(
                        RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_VOUCHER_TAG_KEY, voucherId),
                        TimeUnit.SECONDS
                );
                if (ttlSeconds == null || ttlSeconds <= 0) {
                    ttlSeconds = 3600L;
                }
                redisCache.putHash(statusKey, userIdStr, SubscribeStatus.SUCCESS.getCode(), ttlSeconds, TimeUnit.SECONDS);
                redisCache.expire(statusKey, ttlSeconds, TimeUnit.SECONDS);
                res.add(new GetSubscribeStatusVo(voucherId, SubscribeStatus.SUCCESS.getCode()));
                continue;
            }
            boolean inQueue = Boolean.TRUE.equals(redisCache.isMemberForSet(
                    RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_SUBSCRIBE_USER_TAG_KEY, voucherId),
                    userIdStr
            ));
            res.add(new GetSubscribeStatusVo(voucherId, inQueue ? SubscribeStatus.SUBSCRIBED.getCode() : SubscribeStatus.UNSUBSCRIBED.getCode()));
        }
        return res;
    }
    
    public Long doAddSeckillVoucherV1(SeckillVoucherDto seckillVoucherDto) {
        // 保存优惠券
        VoucherDto voucherDto = new VoucherDto();
        BeanUtil.copyProperties(seckillVoucherDto, voucherDto);
        Long voucherId = addVoucher(voucherDto);
        // 保存秒杀信息
        SeckillVoucher seckillVoucher = new SeckillVoucher();
        seckillVoucher.setId(snowflakeIdGenerator.nextId());
        seckillVoucher.setVoucherId(voucherId);
        seckillVoucher.setStock(seckillVoucherDto.getStock());
        seckillVoucher.setBeginTime(seckillVoucherDto.getBeginTime());
        seckillVoucher.setEndTime(seckillVoucherDto.getEndTime());
        seckillVoucherService.save(seckillVoucher);
        // 保存秒杀库存到Redis中
        stringRedisTemplate.opsForValue().set(SECKILL_STOCK_KEY + voucherId, seckillVoucher.getStock().toString());
        // 如果数据库查询不是空的，将秒杀优惠券信息写入缓存，TTL为距离结束时间的秒数
        long ttlSeconds = Math.max(
                LocalDateTimeUtil.between(LocalDateTimeUtil.now(), seckillVoucher.getEndTime()).getSeconds(),
                1L
        );
        seckillVoucher.setStock(null);
        redisCache.set(
                RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_VOUCHER_TAG_KEY, voucherId),
                seckillVoucher,
                ttlSeconds,
                TimeUnit.SECONDS
        );
        return voucherId;
    }
    
    public Long doAddSeckillVoucherV2(SeckillVoucherDto seckillVoucherDto) {
        // 保存优惠券
        VoucherDto voucherDto = new VoucherDto();
        BeanUtil.copyProperties(seckillVoucherDto, voucherDto);
        Long voucherId = addVoucher(voucherDto);
        // 保存秒杀信息
        SeckillVoucher seckillVoucher = new SeckillVoucher();
        seckillVoucher.setId(snowflakeIdGenerator.nextId());
        seckillVoucher.setVoucherId(voucherId);
        seckillVoucher.setInitStock(seckillVoucherDto.getStock());
        seckillVoucher.setStock(seckillVoucherDto.getStock());
        seckillVoucher.setBeginTime(seckillVoucherDto.getBeginTime());
        seckillVoucher.setEndTime(seckillVoucherDto.getEndTime());
        // 受众规则字段
        seckillVoucher.setAllowedLevels(seckillVoucherDto.getAllowedLevels());
        seckillVoucher.setMinLevel(seckillVoucherDto.getMinLevel());
        seckillVoucherService.save(seckillVoucher);
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
        return voucherId;
    }
}
