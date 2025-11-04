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
local userId = ARGV[2]
-- 订单id（回滚脚本不使用，但保留以兼容消息）
local orderId = ARGV[3]
local traceId = ARGV[4]
local logType = ARGV[5]
-- 日志TTL（秒）
local ttlSeconds = tonumber(ARGV[6])
-- 备注：方案A不分片，所有操作在同槽位单键内完成

-- 3.脚本业务
local stock = redis.call('get', stockKey);
-- 缓存中的秒杀券库存为空，则直接返回
if not stock then
    return 10004
end
-- 恢复库存
local beforeQty = tonumber(stock)
local changeQty = 1
local afterQty = beforeQty + changeQty
redis.call('incrby', stockKey, changeQty)
-- 删除下单记录
redis.call('srem', seckillUserKey, userId)
-- 记录回滚日志
local timeArr = redis.call('TIME')
local nowMillis = tonumber(timeArr[1]) * 1000 + math.floor(tonumber(timeArr[2]) / 1000)
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
redis.call('hset', traceLogKey, traceId, logEntry)
if ttlSeconds and ttlSeconds > 0 then
  redis.call('expire', traceLogKey, ttlSeconds)
end
return 0