-- 1.参数列表
-- 单槽位库存key（已格式化，带HashTag）
local stockKey = KEYS[1]
-- 单槽位用户集合key（已格式化，带HashTag）
local seckillUserKey = KEYS[2]
-- 优惠券id
local voucherId = ARGV[1]
-- 用户id
local userId = ARGV[2]
-- 活动开始/结束时间（毫秒）
local beginTime = tonumber(ARGV[3])
local endTime = tonumber(ARGV[4])
-- 备注：方案A不分片，所有操作在同槽位单键内完成

-- 3.脚本业务
-- 当前时间（毫秒）
local timeArr = redis.call('TIME')
local nowMillis = tonumber(timeArr[1]) * 1000 + math.floor(tonumber(timeArr[2]) / 1000)

-- 时间范围判断：未开始或已结束直接返回
if nowMillis < beginTime then
    return 10002
end
if nowMillis > endTime then
    return 10003
end
local stock = redis.call('get', stockKey);
-- 缓存中的秒杀券库存为空，则直接返回
if not stock then
    return 10004
end
-- 判断库存是否充足
if (tonumber(stock) <= 0) then
    -- 库存不足，则直接返回
    return 10005
end
-- 3.2.判断用户是否下单
if (redis.call('sismember', seckillUserKey, userId) == 1) then
    -- 3.3.存在，说明是重复下单，返回2
    return 10006
end
-- 3.4.扣库存 incrby stockKey -1
redis.call('incrby', stockKey, -1)
-- 3.5.下单（保存用户）
redis.call('sadd', seckillUserKey, userId)
return 0