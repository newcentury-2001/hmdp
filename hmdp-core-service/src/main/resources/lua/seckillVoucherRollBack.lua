-- 1.参数列表
-- 单槽位库存key（已格式化，带HashTag）
local stockKey = KEYS[1]
-- 单槽位用户集合key（已格式化，带HashTag）
local seckillUserKey = KEYS[2]
-- 优惠券id
local voucherId = ARGV[1]
-- 用户id
local userId = ARGV[2]
-- 订单id（回滚脚本不使用，但保留以兼容消息）
local orderId = ARGV[3]
-- 备注：方案A不分片，所有操作在同槽位单键内完成

-- 3.脚本业务
local stock = redis.call('get', stockKey);
-- 缓存中的秒杀券库存为空，则直接返回
if not stock then
    return 10004
end
-- 恢复库存
redis.call('incrby', stockKey, 1)
-- 删除下单记录
redis.call('srem', seckillUserKey, userId)
return 0