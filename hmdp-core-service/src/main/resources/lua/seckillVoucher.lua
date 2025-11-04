-- 1.参数列表
-- 单槽位库存key（已格式化，带HashTag）
local stockKey = KEYS[1]
-- 单槽位用户集合key（已格式化，带HashTag）
local seckillUserKey = KEYS[2]
-- 单槽位扣减日志key（已格式化，带HashTag）
local traceLogKey = KEYS[3]
-- 优惠券id
local voucherId = ARGV[1]
-- 用户id
local userId = ARGV[2]
-- 活动开始/结束时间（毫秒）
local beginTime = tonumber(ARGV[3])
local endTime = tonumber(ARGV[4])
-- 订单id与日志TTL（秒）
local orderId = ARGV[5]
-- traceId
local traceId = ARGV[6]
local logType = ARGV[7]
local ttlSeconds = tonumber(ARGV[8])
-- 备注：方案A不分片，所有操作在同槽位单键内完成

-- 3.脚本业务
-- 当前时间（毫秒）
local timeArr = redis.call('TIME')
local nowMillis = tonumber(timeArr[1]) * 1000 + math.floor(tonumber(timeArr[2]) / 1000)

-- 时间范围判断：未开始或已结束直接返回
if nowMillis < beginTime then
    return string.format('{"%s": %d}', 'code', 10002)
end
if nowMillis > endTime then
    return string.format('{"%s": %d}', 'code', 10003)
end
local stock = redis.call('get', stockKey);
-- 缓存中的秒杀券库存为空，则直接返回
if not stock then
    return string.format('{"%s": %d}', 'code', 10004)
end
-- 判断库存是否充足
if (tonumber(stock) <= 0) then
    -- 库存不足，则直接返回
    return string.format('{"%s": %d}', 'code', 10005)
end
-- 3.2.判断用户是否下单
if (redis.call('sismember', seckillUserKey, userId) == 1) then
    -- 3.3.存在，说明是重复下单，返回2
    return string.format('{"%s": %d}', 'code', 10006)
end
-- 3.4.扣库存 incrby stockKey -1
-- 记录扣减前数量与扣减量/扣减后数量
local beforeQty = tonumber(stock)
local changeQty = 1
local afterQty = beforeQty - changeQty
redis.call('incrby', stockKey, -changeQty)
-- 3.5.下单（保存用户）
redis.call('sadd', seckillUserKey, userId)
-- 3.6.记录扣减日志
local timeArr2 = redis.call('TIME')
local logNowMillis = tonumber(timeArr2[1]) * 1000 + math.floor(tonumber(timeArr2[2]) / 1000)
local logEntry = cjson.encode({
  logType = logType,
  ts = logNowMillis,
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
return string.format('{"%s": %d, "%s": %s, "%s": %s, "%s": %s}', 'code', 0, 'beforeQty', beforeQty, 'deductQty', deductQty, 'afterQty', afterQty)