package org.javaup.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.javaup.core.RedisKeyManage;
import org.javaup.core.SpringUtil;
import org.javaup.dto.CancelVoucherOrderDto;
import org.javaup.dto.GetVoucherOrderByVoucherIdDto;
import org.javaup.dto.GetVoucherOrderDto;
import org.javaup.dto.Result;
import org.javaup.dto.VoucherReconcileLogDto;
import org.javaup.entity.SeckillVoucher;
import org.javaup.entity.UserInfo;
import org.javaup.entity.Voucher;
import org.javaup.entity.VoucherOrder;
import org.javaup.entity.VoucherOrderRouter;
import org.javaup.enums.BaseCode;
import org.javaup.enums.BusinessType;
import org.javaup.enums.LogType;
import org.javaup.enums.OrderStatus;
import org.javaup.enums.SeckillVoucherOrderOperate;
import org.javaup.exception.HmdpFrameException;
import org.javaup.kafka.message.SeckillVoucherMessage;
import org.javaup.kafka.producer.SeckillVoucherProducer;
import org.javaup.kafka.redis.RedisVoucherData;
import org.javaup.lua.SeckillVoucherDomain;
import org.javaup.lua.SeckillVoucherOperate;
import org.javaup.mapper.VoucherOrderMapper;
import org.javaup.mapper.VoucherOrderRouterMapper;
import org.javaup.message.MessageExtend;
import org.javaup.model.SeckillVoucherFullModel;
import org.javaup.redis.RedisCacheImpl;
import org.javaup.redis.RedisKeyBuild;
import org.javaup.repeatexecutelimit.annotion.RepeatExecuteLimit;
import org.javaup.service.ISeckillVoucherService;
import org.javaup.service.IUserInfoService;
import org.javaup.service.IVoucherOrderRouterService;
import org.javaup.service.IVoucherOrderService;
import org.javaup.service.IVoucherReconcileLogService;
import org.javaup.service.IVoucherService;
import org.javaup.toolkit.SnowflakeIdGenerator;
import org.javaup.utils.RedisIdWorker;
import org.javaup.utils.UserHolder;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamInfo;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.javaup.constant.Constant.SECKILL_VOUCHER_TOPIC;
import static org.javaup.constant.RepeatExecuteLimitConstants.SECKILL_VOUCHER_ORDER;

/**
 * @program: 黑马点评-plus升级版实战项目。添加 阿星不是程序员 微信，添加时备注 点评 来获取项目的完整资料
 * @description: 优惠券订单 接口实现
 * @author: 阿星不是程序员
 **/
@Slf4j
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Resource
    private IVoucherService voucherService;
    
    @Resource
    private ISeckillVoucherService seckillVoucherService;

    @Resource
    private RedisIdWorker redisIdWorker;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RedissonClient redissonClient;
    
    @Resource
    private SnowflakeIdGenerator snowflakeIdGenerator;
    
    @Resource
    private SeckillVoucherOperate seckillVoucherOperate;
    
    @Resource
    private SeckillVoucherProducer seckillVoucherProducer;
    
    @Resource
    private RedisCacheImpl redisCache;
    
    @Resource
    private IVoucherOrderRouterService voucherOrderRouterService;
    
    @Resource
    private IUserInfoService userInfoService;
    
    @Resource
    private VoucherOrderMapper voucherOrderMapper;
    
    @Resource
    private VoucherOrderRouterMapper voucherOrderRouterMapper;
    
    @Resource
    private RedisVoucherData redisVoucherData;
    
    @Resource
    private IVoucherReconcileLogService voucherReconcileLogService;
    

    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;

    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }

    private static final ThreadPoolExecutor SECKILL_ORDER_EXECUTOR =
            new ThreadPoolExecutor(
                    1,
                    1,
                    0L,
                    TimeUnit.MILLISECONDS,
                    new LinkedBlockingQueue<>(1024),
                    new NamedThreadFactory("seckill-order-", false),
                    new ThreadPoolExecutor.CallerRunsPolicy()
            );

    private static class NamedThreadFactory implements ThreadFactory {
        private final String namePrefix;
        private final boolean daemon;
        private final AtomicInteger index = new AtomicInteger(1);

        public NamedThreadFactory(String namePrefix, boolean daemon) {
            this.namePrefix = namePrefix;
            this.daemon = daemon;
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, namePrefix + index.getAndIncrement());
            t.setDaemon(daemon);
            t.setUncaughtExceptionHandler((thread, ex) ->
                    log.error("未捕获异常，线程={}, err={}", thread.getName(), ex.getMessage(), ex)
            );
            return t;
        }
    }
    
    
    @PostConstruct
    private void init(){
        // 这是黑马点评的普通版本，升级版本中不再使用此方式
        //SECKILL_ORDER_EXECUTOR.submit(new VoucherOrderHandler());
    }

    @PreDestroy
    private void destroy(){
        try {
            SECKILL_ORDER_EXECUTOR.shutdown();
            if (!SECKILL_ORDER_EXECUTOR.awaitTermination(5, TimeUnit.SECONDS)) {
                SECKILL_ORDER_EXECUTOR.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            SECKILL_ORDER_EXECUTOR.shutdownNow();
        }
    }

    private class VoucherOrderHandler implements Runnable{
        private final String queueName = "stream.orders";
        @Override
        public void run() {
            while (true) {
                try {
                    // 0.初始化stream
                    initStream();
                    // 1.获取消息队列中的订单信息 XREADGROUP GROUP g1 c1 COUNT 1 BLOCK 2000 STREAMS s1 >
                    List<MapRecord<String, Object, Object>> list = stringRedisTemplate.opsForStream().read(
                            Consumer.from("g1", "c1"),
                            StreamReadOptions.empty().count(1).block(Duration.ofSeconds(2)),
                            StreamOffset.create(queueName, ReadOffset.lastConsumed())
                    );
                    // 2.判断订单信息是否为空
                    if (list == null || list.isEmpty()) {
                        // 如果为null，说明没有消息，继续下一次循环
                        continue;
                    }
                    // 解析数据
                    MapRecord<String, Object, Object> record = list.get(0);
                    Map<Object, Object> value = record.getValue();
                    VoucherOrder voucherOrder = BeanUtil.fillBeanWithMap(value, new VoucherOrder(), true);
                    // 3.创建订单
                    handleVoucherOrder(voucherOrder);
                    // 4.确认消息 XACK stream.orders g1 id
                    stringRedisTemplate.opsForStream().acknowledge(queueName, "g1", record.getId());
                } catch (Exception e) {
                    log.error("处理订单异常", e);
                    handlePendingList();
                }
            }
        }

        public void initStream(){
            Boolean exists = stringRedisTemplate.hasKey(queueName);
            if (BooleanUtil.isFalse(exists)) {
                log.info("stream不存在，开始创建stream");
                // 不存在，需要创建
                stringRedisTemplate.opsForStream().createGroup(queueName, ReadOffset.latest(), "g1");
                log.info("stream和group创建完毕");
                return;
            }
            // stream存在，判断group是否存在
            StreamInfo.XInfoGroups groups = stringRedisTemplate.opsForStream().groups(queueName);
            if(groups.isEmpty()){
                log.info("group不存在，开始创建group");
                // group不存在，创建group
                stringRedisTemplate.opsForStream().createGroup(queueName, ReadOffset.latest(), "g1");
                log.info("group创建完毕");
            }
        }

        private void handlePendingList() {
            while (true) {
                try {
                    // 1.获取消息队列中的订单信息 XREADGROUP GROUP g1 c1 COUNT 1 STREAMS s1 0
                    List<MapRecord<String, Object, Object>> list = stringRedisTemplate.opsForStream().read(
                            Consumer.from("g1", "c1"),
                            StreamReadOptions.empty().count(1),
                            StreamOffset.create(queueName, ReadOffset.from("0"))
                    );
                    // 2.判断订单信息是否为空
                    if (list == null || list.isEmpty()) {
                        // 如果为null，说明没有消息，继续下一次循环
                        break;
                    }
                    // 解析数据
                    MapRecord<String, Object, Object> record = list.get(0);
                    Map<Object, Object> value = record.getValue();
                    VoucherOrder voucherOrder = BeanUtil.fillBeanWithMap(value, new VoucherOrder(), true);
                    // 3.创建订单
                    handleVoucherOrder(voucherOrder);
                    // 4.确认消息 XACK stream.orders g1 id
                    stringRedisTemplate.opsForStream().acknowledge(queueName, "g1", record.getId());
                } catch (Exception e) {
                    log.error("处理订单异常", e);
                }
            }
        }
    }

    private void handleVoucherOrder(VoucherOrder voucherOrder) {
        Long userId = voucherOrder.getId();
        // 创建锁对象
        // SimpleRedisLock lock = new SimpleRedisLock("order:" + userId, stringRedisTemplate);
        RLock lock = redissonClient.getLock("lock:order:" + userId);
        // 获取锁
        boolean isLock = lock.tryLock();
        // 判断是否获取锁成功
        if(!isLock){
            // 获取锁失败，返回错误或重试
            log.error("不允许重复下单");
            return;
        }
        try {
            // 获取代理对象（事务）
            createVoucherOrderV1(voucherOrder);
        } finally {
            // 释放锁
            lock.unlock();
        }
    }

    IVoucherOrderService proxy;
    /**
     * 抢优惠券下单
     * */
    @Override
    public Result<Long> seckillVoucher(Long voucherId) {
        //黑马点评原始版版本
        //return doSeckillVoucherV1(voucherId);
        //黑马点评升级版本
        return doSeckillVoucherV2(voucherId);
    }
    
    public Result<Long> doSeckillVoucherV1(Long voucherId) {
        Long userId = UserHolder.getUser().getId();
        long orderId = snowflakeIdGenerator.nextId();
        // 1.执行lua脚本
        Long result = stringRedisTemplate.execute(
                SECKILL_SCRIPT,
                Collections.emptyList(),
                voucherId.toString(), userId.toString(), String.valueOf(orderId)
        );
        int r = result.intValue();
        // 2.判断结果是否为0
        if (r != 0) {
            // 2.1.不为0 ，代表没有购买资格
            return Result.fail(r == 1 ? "库存不足" : "不能重复下单");
        }
        // 3.获取代理对象
        proxy = (IVoucherOrderService) AopContext.currentProxy();
        // 4.返回订单id
        return Result.ok(orderId);
    }
    
    public Result<Long> doSeckillVoucherV2(Long voucherId) {
        //查询秒杀优惠券
        SeckillVoucherFullModel seckillVoucherFullModel = seckillVoucherService.queryByVoucherId(voucherId);
        //加载优惠券库存
        seckillVoucherService.loadVoucherStock(voucherId);
        Long userId = UserHolder.getUser().getId();
        //验证会员等级
        verifyUserLevel(seckillVoucherFullModel,userId);
        // 限流统一在控制器层执行，避免重复计数与双重拦截
        long orderId = snowflakeIdGenerator.nextId();
        long traceId = snowflakeIdGenerator.nextId();
        // 执行lua脚本需要的key（单槽位Hash Tag键，不分片）
        List<String> keys = ListUtil.of(
                RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_STOCK_TAG_KEY, voucherId).getRelKey(),
                RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_USER_TAG_KEY, voucherId).getRelKey(),
                RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_TRACE_LOG_TAG_KEY, voucherId).getRelKey()
        );
        //执行lua中需要的数据
        String[] args = new String[9];
        args[0] = voucherId.toString();
        args[1] = userId.toString();
        args[2] = String.valueOf(LocalDateTimeUtil.toEpochMilli(seckillVoucherFullModel.getBeginTime()));
        args[3] = String.valueOf(LocalDateTimeUtil.toEpochMilli(seckillVoucherFullModel.getEndTime()));
        args[4] = String.valueOf(seckillVoucherFullModel.getStatus());
        args[5] = String.valueOf(orderId);
        args[6] = String.valueOf(traceId);
        args[7] = String.valueOf(LogType.DEDUCT.getCode());
        long secondsUntilEnd = Duration.between(LocalDateTimeUtil.now(), seckillVoucherFullModel.getEndTime()).getSeconds();
        long ttlSeconds = Math.max(1L, secondsUntilEnd + Duration.ofDays(1).getSeconds());
        args[8] = String.valueOf(ttlSeconds);
        //执行lua
        SeckillVoucherDomain seckillVoucherDomain = seckillVoucherOperate.execute(
                keys,
                args
        );
        //验证是否成功
        if (!seckillVoucherDomain.getCode().equals(BaseCode.SUCCESS.getCode())) {
            //终止执行
            throw new HmdpFrameException(Objects.requireNonNull(BaseCode.getRc(seckillVoucherDomain.getCode())));
        }
        SeckillVoucherMessage seckillVoucherMessage = new SeckillVoucherMessage(
                userId,
                voucherId,
                orderId,
                traceId,
                seckillVoucherDomain.getBeforeQty(),
                seckillVoucherDomain.getDeductQty(),
                seckillVoucherDomain.getAfterQty(),
                Boolean.FALSE
        );
        // 发送kafka
        seckillVoucherProducer.sendPayload(
                SpringUtil.getPrefixDistinctionName() + "-" + SECKILL_VOUCHER_TOPIC, 
                seckillVoucherMessage);
        
        // 返回订单id
        return Result.ok(orderId);
    }
    
    /**
     * 校验用户是否满足参与券活动的人群规则：
     * - allowedLevels: 逗号分隔的会员等级白名单（示例："1,2,3"）
     * - minLevel: 允许参与的最低会员等级（含）
     * 规则说明：
     * - 若两者均为空，则视为无门槛，直接通过
     * - 若存在 allowedLevels，则用户等级需在白名单中
     * - 若存在 minLevel，则用户等级需 >= minLevel
     * - 两者并存时，需同时满足（先白名单，再最低等级）
     * 校验失败将抛出业务异常以终止抢券
     */
    public void verifyUserLevel(SeckillVoucherFullModel seckillVoucherFullModel,Long userId){
        // 从券模型读取允许的等级白名单字符串（可能为空）
        String allowedLevelsStr = seckillVoucherFullModel.getAllowedLevels();
        // 从券模型读取最低等级限制（可能为空）
        Integer minLevel = seckillVoucherFullModel.getMinLevel();
        // 是否存在任何等级规则：白名单或最低等级其一即可视为有门槛
        boolean hasLevelRule = StrUtil.isNotBlank(allowedLevelsStr) || Objects.nonNull(minLevel);
        // 无等级规则时直接放行，避免不必要的数据库查询
        if (!hasLevelRule) {
            return;
        }
        // 查询用户基本信息（含会员等级等）
        UserInfo userInfo = userInfoService.getByUserId(userId);
        // 用户不存在直接抛错，防止后续空指针并阻断抢券
        if (Objects.isNull(userInfo)) {
            throw new HmdpFrameException(BaseCode.USER_NOT_EXIST);
        }
        // allowed 标识当前用户是否通过规则校验，默认通过
        boolean allowed = true;
        // 当前用户的会员等级（可能为空）
        Integer level = userInfo.getLevel();
        // 处理 allowedLevels 白名单规则
        if (StrUtil.isNotBlank(allowedLevelsStr)) {
            try {
                // 将逗号分隔的字符串解析为去空格的整型集合
                Set<Integer> allowedLevels = Arrays.stream(allowedLevelsStr.split(","))
                        .map(String::trim)
                        .filter(StrUtil::isNotBlank)
                        .map(Integer::valueOf)
                        .collect(Collectors.toSet());
                // 白名单非空时要求用户等级在集合之中
                if (CollectionUtil.isNotEmpty(allowedLevels)) {
                    allowed = allowedLevels.contains(level);
                }
            } catch (Exception parseEx) {
                // 解析失败记录日志但不中断流程，保持 allowed = true 让后续 minLevel 规则继续判断
                log.warn("allowedLevels 解析失败, voucherId={}, raw={}",
                        seckillVoucherFullModel.getVoucherId(), 
                        allowedLevelsStr, parseEx);
            }
        }
        // 处理最低等级 minLevel 规则：仅当之前仍允许时再判断
        if (allowed && Objects.nonNull(minLevel)) {
            // 用户等级不为空且 >= 最低等级方可通过
            allowed = Objects.nonNull(level) && level >= minLevel;
        }
        // 最终不满足规则则抛出业务异常，终止本次抢券
        if (!allowed) {
            throw new HmdpFrameException("当前会员级别不满足参与条件");
        }
    }

    /**
     * 简单的受众规则载体，按存在字段进行校验
     * */
    private static class AudienceRule {
        public Set<Integer> allowedLevels;
        public Integer minLevel;
        public Set<String> allowedCities;
        
        boolean hasLevelRule(){
            return (allowedLevels != null && !allowedLevels.isEmpty()) || minLevel != null;
        }
        boolean hasCityRule(){
            return allowedCities != null && !allowedCities.isEmpty();
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createVoucherOrderV1(VoucherOrder voucherOrder) {
        // 5.一人一单
        Long userId = voucherOrder.getUserId();

        // 5.1.查询订单
        Long count = query().eq("user_id", userId).eq("voucher_id", voucherOrder.getVoucherId()).count();
        // 5.2.判断是否存在
        if (count > 0) {
            // 用户已经购买过了
            log.error("用户已经购买过一次！");
            return;
        }
        // 6.扣减库存
        boolean success = seckillVoucherService.update()
                // set stock = stock - 1
                .setSql("stock = stock - 1")
                // where id = ? and stock > 0
                .eq("voucher_id", voucherOrder.getVoucherId()).gt("stock", 0) 
                .update();
        if (!success) {
            // 扣减失败
            log.error("库存不足！");
            return;
        }
        // 7.创建订单
        save(voucherOrder);
    }
    
    
    @Override
    @RepeatExecuteLimit(name = SECKILL_VOUCHER_ORDER,keys = {"#message.uuid"})
    @Transactional(rollbackFor = Exception.class)
    public boolean createVoucherOrderV2(MessageExtend<SeckillVoucherMessage> message) {
        //获取消息体
        SeckillVoucherMessage messageBody = message.getMessageBody();
        Long userId = messageBody.getUserId();
        //根据优惠券id和用户id查询是否已经存在正常的订单
        VoucherOrder normalVoucherOrder = lambdaQuery()
                .eq(VoucherOrder::getVoucherId, messageBody.getVoucherId())
                .eq(VoucherOrder::getUserId, userId)
                .eq(VoucherOrder::getStatus,OrderStatus.NORMAL.getCode())
                .one();
        //如果存在，则直接结束运行
        if (Objects.nonNull(normalVoucherOrder)) {
            log.warn("已存在此订单，voucherId：{},userId：{}", normalVoucherOrder.getVoucherId(), userId);
            throw new HmdpFrameException(BaseCode.VOUCHER_ORDER_EXIST);
        }
        //扣减库存
        boolean success = seckillVoucherService.update()
                // set stock = stock - 1
                .setSql("stock = stock - 1")
                // where id = ? and stock > 0
                .eq("voucher_id", messageBody.getVoucherId())
                .gt("stock", 0)
                .update();
        if (!success) {
            //扣减失败：触发消息侧回滚Redis数据
            throw new HmdpFrameException("优惠券库存不足！优惠券id:" + messageBody.getVoucherId());
        }
        //创建订单
        VoucherOrder voucherOrder = new VoucherOrder();
        voucherOrder.setId(messageBody.getOrderId());
        voucherOrder.setUserId(messageBody.getUserId());
        voucherOrder.setVoucherId(messageBody.getVoucherId());
        voucherOrder.setCreateTime(LocalDateTimeUtil.now());
        save(voucherOrder);
        //创建订单路由
        VoucherOrderRouter voucherOrderRouter = new VoucherOrderRouter();
        voucherOrderRouter.setId(snowflakeIdGenerator.nextId());
        voucherOrderRouter.setOrderId(voucherOrder.getId());
        voucherOrderRouter.setUserId(userId);
        voucherOrderRouter.setVoucherId(voucherOrder.getVoucherId());
        voucherOrderRouter.setCreateTime(LocalDateTimeUtil.now());
        voucherOrderRouter.setUpdateTime(LocalDateTimeUtil.now());
        voucherOrderRouterService.save(voucherOrderRouter);
        //订单存放到redis
        redisCache.set(RedisKeyBuild.createRedisKey(
                RedisKeyManage.DB_SECKILL_ORDER_KEY,messageBody.getOrderId()),
                voucherOrder,
                60, 
                TimeUnit.SECONDS
        );
        //对账日志：一致-消费成功
        voucherReconcileLogService.saveReconcileLog(
                LogType.DEDUCT.getCode(),
                BusinessType.SUCCESS.getCode(),
                "order created",
                message
        );
        return true;
    }
    
    @Override
    public Long getSeckillVoucherOrder(GetVoucherOrderDto getVoucherOrderDto) {
        VoucherOrder voucherOrder = 
                redisCache.get(RedisKeyBuild.createRedisKey(
                        RedisKeyManage.DB_SECKILL_ORDER_KEY, 
                        getVoucherOrderDto.getOrderId()), 
                        VoucherOrder.class);
        if (Objects.nonNull(voucherOrder)) {
            return voucherOrder.getId();
        }
        VoucherOrderRouter voucherOrderRouter = 
                voucherOrderRouterService.lambdaQuery()
                        .eq(VoucherOrderRouter::getOrderId, getVoucherOrderDto.getOrderId())
                        .one();
        if (Objects.nonNull(voucherOrderRouter)) {
            return voucherOrderRouter.getOrderId();
        }
        return null;
    }
    
    @Override
    public Long getSeckillVoucherOrderIdByVoucherId(GetVoucherOrderByVoucherIdDto getVoucherOrderByVoucherIdDto) {
        VoucherOrder voucherOrder = lambdaQuery()
                .eq(VoucherOrder::getUserId, UserHolder.getUser().getId())
                .eq(VoucherOrder::getVoucherId, getVoucherOrderByVoucherIdDto.getVoucherId())
                .eq(VoucherOrder::getStatus, OrderStatus.NORMAL.getCode())
                .one();
        if (Objects.nonNull(voucherOrder)) {
            return voucherOrder.getId();
        }
        return null;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean cancel(CancelVoucherOrderDto cancelVoucherOrderDto) {
        VoucherOrder voucherOrder = lambdaQuery()
                .eq(VoucherOrder::getUserId, UserHolder.getUser().getId())
                .eq(VoucherOrder::getVoucherId, cancelVoucherOrderDto.getVoucherId())
                .eq(VoucherOrder::getStatus, OrderStatus.NORMAL.getCode())
                .one();
        if (Objects.isNull(voucherOrder)) {
            throw new HmdpFrameException(BaseCode.SECKILL_VOUCHER_ORDER_NOT_EXIST);
        }
        SeckillVoucher seckillVoucher = seckillVoucherService.lambdaQuery()
                .eq(SeckillVoucher::getVoucherId, cancelVoucherOrderDto.getVoucherId())
                .one();
        if (Objects.isNull(seckillVoucher)) {
            throw new HmdpFrameException(BaseCode.SECKILL_VOUCHER_NOT_EXIST);
        }
        boolean updateResult = lambdaUpdate().set(VoucherOrder::getStatus, OrderStatus.CANCEL.getCode())
                .set(VoucherOrder::getUpdateTime, LocalDateTimeUtil.now())
                .eq(VoucherOrder::getUserId, UserHolder.getUser().getId())
                .eq(VoucherOrder::getVoucherId, cancelVoucherOrderDto.getVoucherId())
                .update();
        // 对账日志
        long traceId = snowflakeIdGenerator.nextId();
        VoucherReconcileLogDto voucherReconcileLogDto = new VoucherReconcileLogDto();
        voucherReconcileLogDto.setOrderId(voucherOrder.getId());
        voucherReconcileLogDto.setUserId(voucherOrder.getUserId());
        voucherReconcileLogDto.setVoucherId(voucherOrder.getVoucherId());
        voucherReconcileLogDto.setDetail("cancel voucher order ");
        voucherReconcileLogDto.setBeforeQty(seckillVoucher.getStock());
        voucherReconcileLogDto.setChangeQty(1);
        voucherReconcileLogDto.setAfterQty(seckillVoucher.getStock() + 1);
        voucherReconcileLogDto.setTraceId(traceId);
        voucherReconcileLogDto.setLogType(LogType.RESTORE.getCode());
        voucherReconcileLogDto.setBusinessType( BusinessType.CANCEL.getCode());
        boolean saveReconcileLogResult = voucherReconcileLogService.saveReconcileLog(voucherReconcileLogDto);
        
        // 恢复库存
        boolean rollbackStockResult = seckillVoucherService.rollbackStock(cancelVoucherOrderDto.getVoucherId());
        
        Boolean result = updateResult && saveReconcileLogResult && rollbackStockResult;
        if (result) {
            redisVoucherData.rollbackRedisVoucherData(
                    SeckillVoucherOrderOperate.YES,
                    traceId,
                    voucherOrder.getVoucherId(),
                    voucherOrder.getUserId(),
                    voucherOrder.getId(),
                    seckillVoucher.getStock(),
                    1,
                    seckillVoucher.getStock() + 1
            );
            // 将自己移除订阅队列
            redisCache.delForHash(RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_SUBSCRIBE_STATUS_TAG_KEY, 
                    cancelVoucherOrderDto.getVoucherId()),
                    String.valueOf(voucherOrder.getUserId()));
            // 将自己在每日Top买家统计的zset中减1
            Voucher voucher = voucherService.getById(voucherOrder.getVoucherId());
            if (Objects.nonNull(voucher)) {
                String day = voucherOrder.getCreateTime().format(DateTimeFormatter.BASIC_ISO_DATE);
                RedisKeyBuild dailyKey = RedisKeyBuild.createRedisKey(
                        RedisKeyManage.SECKILL_SHOP_TOP_BUYERS_DAILY_TAG_KEY,
                        voucher.getShopId(),
                        day
                );
                redisCache.incrementScoreForSortedSet(dailyKey, String.valueOf(voucherOrder.getUserId()), -1.0);
            }
            
            // 回滚成功后，尝试将资格自动分配给订阅队列中最早的未购用户
            try {
                autoIssueVoucherToEarliestSubscriber(
                        voucherOrder.getVoucherId(), 
                        voucherOrder.getUserId()
                );
            } catch (Exception e) {
                log.warn("自动发券失败，voucherId={}, err=\n{}", voucherOrder.getVoucherId(), e.getMessage());
            }
        }
        return result;
    }
    
    /**
     * 回滚后自动发券：挑选订阅ZSET中按加入时间最早的未购用户，执行Lua扣减并下发Kafka消息。
     * 说明：
     * - 不修改订阅集合与状态，成功下单后用户将出现在已购集合，状态查询会返回SUCCESS；
     * - 为避免重复，筛选时排除已购用户与当前取消用户；
     * - 采用范围批量读取前N条并按score最小选取候选，避免由于Set去序导致的顺序丢失。
     */
    private boolean autoIssueVoucherToEarliestSubscriber(final Long voucherId, final Long excludeUserId) {
        // 查询券信息，用于校验和TTL计算
        SeckillVoucherFullModel seckillVoucherFullModel = seckillVoucherService.queryByVoucherId(voucherId);
        // 校验券数据完整性（开始/结束时间必须存在）
        if (Objects.isNull(seckillVoucherFullModel) 
                || 
                Objects.isNull(seckillVoucherFullModel.getBeginTime()) 
                ||
                Objects.isNull(seckillVoucherFullModel.getEndTime())) {
            // 数据不完整时终止自动发券
            return false;
        }
        //需要再加载一次库存，防止修改数据或者对账执行时将此redis中的库存删除
        seckillVoucherService.loadVoucherStock(voucherId);
        // 在订阅ZSET中查找最早且未购的候选用户（排除本次取消用户）
        String candidateUserIdStr = findEarliestCandidate(voucherId, excludeUserId);
        // 没有候选用户则结束流程
        if (StrUtil.isBlank(candidateUserIdStr)) {
            return false;
        }
        // 执行扣减与消息下发，并在成功后移除候选的ZSET位置
        return issueToCandidate(voucherId, candidateUserIdStr, seckillVoucherFullModel);
    }

    /**
     * 按分数升序使用 LIMIT 递增分页，找出第一个符合条件的候选用户
     * 条件：排除当前取消用户，且未在已购集合中
     * */
    private String findEarliestCandidate(final Long voucherId, final Long excludeUserId) {
        // 订阅ZSET key（按加入时间的score存储）
        RedisKeyBuild subscribeZSetKey = RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_SUBSCRIBE_ZSET_TAG_KEY, voucherId);
        // 已购用户集合 key
        RedisKeyBuild purchasedSetKey = RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_USER_TAG_KEY, voucherId);
        // 待排除的取消用户id字符串
        String excludeStr = String.valueOf(excludeUserId);
        // 每页仅取一个成员
        final long pageCount = 1L;
        // 从最早的成员开始递增偏移
        long offset = 0L;
        // 循环分页，直到找到符合者或没有更多成员
        while (true) {
            // 读取按score（加入时间）升序的一个成员（带score）
            Set<ZSetOperations.TypedTuple<String>> page = redisCache.rangeByScoreWithScoreForSortedSet(
                    subscribeZSetKey,
                    Double.NEGATIVE_INFINITY,
                    Double.POSITIVE_INFINITY,
                    offset,
                    pageCount,
                    String.class
            );
            // 没有更多成员，返回null
            if (CollectionUtil.isEmpty(page)) {
                return null;
            }
            // 取出当前页的唯一成员
            ZSetOperations.TypedTuple<String> tuple = page.iterator().next();
            // 无效项，跳过并递增偏移
            if (Objects.isNull(tuple) || Objects.isNull(tuple.getValue())) {
                offset++;
                continue;
            }
            // 候选用户id字符串
            String uidStr = tuple.getValue();
            // 空白id，跳过并递增偏移
            if (StrUtil.isBlank(uidStr)) {
                offset++;
                continue;
            }
            // 排除本次取消的用户
            if (Objects.equals(uidStr, excludeStr)) {
                offset++;
                continue;
            }
            // 判断是否已购（true 则跳过）
            Boolean purchased = redisCache.isMemberForSet(purchasedSetKey, uidStr);
            if (BooleanUtil.isTrue(purchased)) {
                offset++;
                continue;
            }
            // 找到第一个符合条件的用户，直接返回
            return uidStr;
        }
    }

    /**
     * 对候选用户执行Lua扣减与消息下发，并从订阅ZSET移除（成功后）
     * */
    private boolean issueToCandidate(final Long voucherId, 
                                     final String candidateUserIdStr, 
                                     final SeckillVoucherFullModel seckillVoucherFullModel) {
        // 将候选用户id转换为Long
        Long candidateUserId = Long.valueOf(candidateUserIdStr);
        // 校验人群规则（不满足则跳过）
        try {
            verifyUserLevel(seckillVoucherFullModel, candidateUserId);
        } catch (Exception e) {
            // 校验失败记录日志并返回
            log.info("候选用户不满足人群规则，自动发券跳过。voucherId={}, userId={}", voucherId, candidateUserId);
            return false;
        }
        // 构建Lua脚本keys（库存、已购集合、trace日志集合）
        List<String> keys = buildSeckillKeys(voucherId);
        // 生成订单id
        long orderId = snowflakeIdGenerator.nextId();
        // 生成traceId
        long traceId = snowflakeIdGenerator.nextId();
        // 构建Lua入参数组
        String[] args = buildSeckillArgs(voucherId, candidateUserIdStr, seckillVoucherFullModel, orderId, traceId);
        // 执行秒杀扣减Lua脚本
        SeckillVoucherDomain domain = seckillVoucherOperate.execute(keys, args);
        // 校验Lua执行结果是否成功
        if (!Objects.equals(domain.getCode(), BaseCode.SUCCESS.getCode())) {
            // 扣减失败记录日志并返回
            log.info("自动发券Lua扣减失败，code={}, voucherId={}, userId={}", domain.getCode(), voucherId, candidateUserId);
            return false;
        }
        // 构造Kafka消息体
        SeckillVoucherMessage message = new SeckillVoucherMessage(
                candidateUserId,
                voucherId,
                orderId,
                traceId,
                domain.getBeforeQty(),
                domain.getDeductQty(),
                domain.getAfterQty(),
                Boolean.TRUE
        );
        // 发送Kafka消息（失败将由生产者回调触发Redis回滚）
        seckillVoucherProducer.sendPayload(
                SpringUtil.getPrefixDistinctionName() + "-" + SECKILL_VOUCHER_TOPIC,
                message
        );
        // 注意：不在此处移除订阅ZSET成员，也不记录“成功”日志。
        // 订阅ZSET的移除应在消息消费成功并创建订单后进行，避免发送或消费失败导致丢号。
        // 返回成功
        return true;
    }

    /**
     * 构建Lua脚本所需的Redis key列表
     * */
    private List<String> buildSeckillKeys(final Long voucherId) {
        // 库存key
        String stockKey = RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_STOCK_TAG_KEY, voucherId).getRelKey();
        // 已购用户集合key
        String userKey = RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_USER_TAG_KEY, voucherId).getRelKey();
        // trace日志集合key
        String traceKey = RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_TRACE_LOG_TAG_KEY, voucherId).getRelKey();
        // 返回keys列表
        return ListUtil.of(stockKey, userKey, traceKey);
    }

    /**
     * 构建Lua脚本的入参数组（voucherId、userId、开始时间、结束时间、orderId、traceId、日志类型、TTL秒数）
     * */
    private String[] buildSeckillArgs(final Long voucherId,
                                      final String userIdStr,
                                      final SeckillVoucherFullModel seckillVoucherFullModel,
                                      final long orderId,
                                      final long traceId) {
        // 初始化数组长度为8
        String[] args = new String[9];
        // 写入voucherId
        args[0] = voucherId.toString();
        // 写入userId
        args[1] = userIdStr;
        // 写入券开始时间（毫秒）
        args[2] = String.valueOf(LocalDateTimeUtil.toEpochMilli(seckillVoucherFullModel.getBeginTime()));
        // 写入券结束时间（毫秒）
        args[3] = String.valueOf(LocalDateTimeUtil.toEpochMilli(seckillVoucherFullModel.getEndTime()));
        // 写入优惠券状态
        args[4] = String.valueOf(seckillVoucherFullModel.getStatus());
        // 写入订单id
        args[5] = String.valueOf(orderId);
        // 写入traceId
        args[6] = String.valueOf(traceId);
        // 写入扣减日志类型
        args[7] = String.valueOf(LogType.DEDUCT.getCode());
        // 计算TTL秒数并写入
        args[8] = String.valueOf(computeTtlSeconds(seckillVoucherFullModel));
        // 返回入参数组
        return args;
    }

    /**
     * 计算缓存TTL秒数：至结束时间的剩余秒数+1天，至少为1秒
     * */
    private long computeTtlSeconds(final SeckillVoucherFullModel seckillVoucherFullModel) {
        // 距离结束时间的秒数
        long secondsUntilEnd = Duration.between(LocalDateTimeUtil.now(), seckillVoucherFullModel.getEndTime()).getSeconds();
        // 叠加额外一天，并保证最小值为1秒
        return Math.max(1L, secondsUntilEnd + Duration.ofDays(1).getSeconds());
    }

    /*
    private BlockingQueue<VoucherOrder> orderTasks = new ArrayBlockingQueue<>(1024 * 1024);
    private class VoucherOrderHandler implements Runnable{
        @Override
        public void run() {
            while (true){
                try {
                    // 1.获取队列中的订单信息
                    VoucherOrder voucherOrder = orderTasks.take();
                    // 2.创建订单
                    handleVoucherOrder(voucherOrder);
                } catch (Exception e) {
                    log.error("处理订单异常", e);
                }
            }
        }
    }

    @Override
    public Result seckillVoucher(Long voucherId) {
        Long userId = UserHolder.getUser().getId();
        // 1.执行lua脚本
        Long result = stringRedisTemplate.execute(
                SECKILL_SCRIPT,
                Collections.emptyList(),
                voucherId.toString(), userId.toString()
        );
        int r = result.intValue();
        // 2.判断结果是否为0
        if (r != 0) {
            // 2.1.不为0 ，代表没有购买资格
            return Result.fail(r == 1 ? "库存不足" : "不能重复下单");
        }
        // 2.2.为0 ，有购买资格，把下单信息保存到阻塞队列
        VoucherOrder voucherOrder = new VoucherOrder();
        // 2.3.订单id
        long orderId = redisIdWorker.nextId("order");
        voucherOrder.setId(orderId);
        // 2.4.用户id
        voucherOrder.setUserId(userId);
        // 2.5.代金券id
        voucherOrder.setVoucherId(voucherId);
        // 2.6.放入阻塞队列
        orderTasks.add(voucherOrder);
        // 3.获取代理对象
        proxy = (IVoucherOrderService) AopContext.currentProxy()
        // 4.返回订单id
        return Result.ok(orderId);
    }*/
    /*@Override
    public Result seckillVoucher(Long voucherId) {
        // 1.查询优惠券
        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
        // 2.判断秒杀是否开始
        if (voucher.getBeginTime().isAfter(LocalDateTime.now())) {
            // 尚未开始
            return Result.fail("秒杀尚未开始！");
        }
        // 3.判断秒杀是否已经结束
        if (voucher.getEndTime().isBefore(LocalDateTime.now())) {
            // 尚未开始
            return Result.fail("秒杀已经结束！");
        }
        // 4.判断库存是否充足
        if (voucher.getStock() < 1) {
            // 库存不足
            return Result.fail("库存不足！");
        }

        Long userId = UserHolder.getUser().getId();
        // 创建锁对象
        // SimpleRedisLock lock = new SimpleRedisLock("order:" + userId, stringRedisTemplate);
        RLock lock = redissonClient.getLock("lock:order:" + userId);
        // 获取锁
        boolean isLock = lock.tryLock();
        // 判断是否获取锁成功
        if(!isLock){
            // 获取锁失败，返回错误或重试
            return Result.fail("不允许重复下单");
        }
        try {
            // 获取代理对象（事务）
            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
            return proxy.createVoucherOrder(voucherId);
        } finally {
            // 释放锁
            lock.unlock();
        }
    }*/


    /*@Transactional
    public Result createVoucherOrder(Long voucherId) {
        // 5.一人一单
        Long userId = UserHolder.getUser().getId();

        synchronized (userId.toString().intern()) {
            // 5.1.查询订单
            int count = query().eq("user_id", userId).eq("voucher_id", voucherId).count();
            // 5.2.判断是否存在
            if (count > 0) {
                // 用户已经购买过了
                return Result.fail("用户已经购买过一次！");
            }

            // 6.扣减库存
            boolean success = seckillVoucherService.update()
                    .setSql("stock = stock - 1") // set stock = stock - 1
                    .eq("voucher_id", voucherId).gt("stock", 0) // where id = ? and stock > 0
                    .update();
            if (!success) {
                // 扣减失败
                return Result.fail("库存不足！");
            }

            // 7.创建订单
            VoucherOrder voucherOrder = new VoucherOrder();
            // 7.1.订单id
            long orderId = redisIdWorker.nextId("order");
            voucherOrder.setId(orderId);
            // 7.2.用户id
            voucherOrder.setUserId(userId);
            // 7.3.代金券id
            voucherOrder.setVoucherId(voucherId);
            save(voucherOrder);

            // 7.返回订单id
            return Result.ok(orderId);
        }
    }*/

}
