-- 1.参数列表
-- 单槽位库存key（已格式化，带HashTag）
local stockKey = KEYS[1]
-- 单槽位用户集合key（已格式化，带HashTag）
local seckillUserKey = KEYS[2]
-- 单槽位操作日志key（已格式化，带HashTag）
local traceLogKey = KEYS[3]
-- 优惠券id
local voucherId = ARGV[1]
-- 用户id
local userId = (ARGV[2])
-- 订单id
local orderId = ARGV[3]
-- 操作码
local seckillVoucherOrderOperate = tonumber(ARGV[4])
local traceId = ARGV[5]
local logType = ARGV[6]
local beforeQty = tonumber(ARGV[7])
local changeQty = tonumber(ARGV[8])
local afterQty = tonumber(ARGV[9])
-- 2.脚本业务
local stock = redis.call('get', stockKey);
-- 缓存中的秒杀券库存为空，则直接返回
if not stock then
    return 10004
end

-- 把库存删除
redis.call('del', stockKey)
if seckillVoucherOrderOperate == 1 then
    -- 删除下单记录（先判断存在再移除更稳妥）
    if (redis.call('sismember', seckillUserKey, userId) == 1) then
        redis.call('srem', seckillUserKey, userId)
    end
end
-- 记录回滚日志
local timeArr = redis.call('TIME')
local nowMillis = tonumber(timeArr[1]) * 1000 + math.floor(tonumber(timeArr[2]) / 1000)
-- 构建回滚日志信息
local logEntry = cjson.encode({
    logType = logType,
    ts = nowMillis,
    orderId = orderId,
    traceId = traceId,
    userId = userId,
    voucherId = voucherId,
    beforeQty = beforeQty,
    changeQty = changeQty,
    afterQty = afterQty
})
-- 向Redis存放回滚日志
redis.call('hset', traceLogKey, traceId, logEntry)
return 0
