package org.javaup.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.LocalDateTimeUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.javaup.core.RedisKeyManage;
import org.javaup.dto.Result;
import org.javaup.dto.SeckillVoucherDto;
import org.javaup.dto.VoucherDto;
import org.javaup.entity.SeckillVoucher;
import org.javaup.entity.Voucher;
import org.javaup.handler.BloomFilterHandlerFactory;
import org.javaup.mapper.VoucherMapper;
import org.javaup.redis.RedisCache;
import org.javaup.redis.RedisKeyBuild;
import org.javaup.service.ISeckillVoucherService;
import org.javaup.service.IVoucherService;
import org.javaup.toolkit.SnowflakeIdGenerator;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.javaup.utils.RedisConstants.SECKILL_STOCK_KEY;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
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
    
    @Override
    public Long addVoucher(VoucherDto voucherDto) {
        Voucher voucher = new Voucher();
        BeanUtil.copyProperties(voucherDto, voucher);
        voucher.setId(snowflakeIdGenerator.nextId());
        save(voucher);
        bloomFilterHandlerFactory.get("voucher").add(voucher.getId().toString());
        return voucher.getId();
    }
    
    @Override
    public Result queryVoucherOfShop(Long shopId) {
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
                RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_VOUCHER_KEY, voucherId),
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
        seckillVoucher.setStock(seckillVoucherDto.getStock());
        seckillVoucher.setBeginTime(seckillVoucherDto.getBeginTime());
        seckillVoucher.setEndTime(seckillVoucherDto.getEndTime());
        seckillVoucherService.save(seckillVoucher);
        // 如果数据库查询不是空的，将秒杀优惠券信息写入缓存，TTL为距离结束时间的秒数
        long ttlSeconds = Math.max(
                LocalDateTimeUtil.between(LocalDateTimeUtil.now(), seckillVoucher.getEndTime()).getSeconds(),
                1L
        );
        // 保存秒杀优惠券库存到Redis中
        redisCache.set(
                RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_STOCK_KEY, voucherId),
                seckillVoucher.getStock().toString(),
                ttlSeconds,
                TimeUnit.SECONDS
        );
        // 保存秒杀优惠券详情到Redis中
        seckillVoucher.setStock(null);
        redisCache.set(
                RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_VOUCHER_KEY, voucherId),
                seckillVoucher,
                ttlSeconds,
                TimeUnit.SECONDS
        );
        return voucherId;
    }
}
