-- 1.参数列表
-- 秒杀券库存的key
local seckill_stock_key = KEYS[1]
-- 秒杀券的详情
local seckill_voucher_key = KEYS[2]
-- 秒杀券用户的key
local seckill_user_key = KEYS[3]
-- 优惠券id
local voucherId = ARGV[1]
-- 用户id
local userId = ARGV[2]
-- 订单id
local orderId = ARGV[3]

-- 2.数据key
-- 库存key
local stockKey = string.format(seckill_stock_key, voucherId)
-- 秒杀券的详情key
local seckillVoucherKey = string.format(seckill_voucher_key, voucherId)
-- 优惠券购买记录
local seckillUserKey = string.format(seckill_user_key, voucherId)

-- 3.脚本业务
local stock = redis.call('get',stockKey);
-- 缓存中的秒杀券库存为空，则直接返回
if not stock then
    return 10004
end
-- 恢复库存
redis.call('incrby', stockKey, 1)
-- 删除下单记录
redis.call('srem', seckillUserKey, userId)
return 0