-- 1.参数列表
-- 秒杀券库存的key
local seckill_stock_key = KEYS[1]
-- 秒杀券的详情
local seckill_voucher_key = KEYS[2]
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
-- 订单key
local orderKey = 'seckill:order:' .. voucherId

-- 3.脚本业务
local stock = redis.call('get',stockKey);
-- 缓存中的秒杀券库存为空，则直接返回
if not stock then
    return string.format('{"%s": %d}', 'code', 10000)
end
local seckillVoucherStr = redis.call('get', stockKey);
-- 缓存中的秒杀券详情为空，则直接返回
if not seckillVoucherStr then
    return string.format('{"%s": %d}', 'code', 10001)
end
local seckillVoucher = cjson.decode(seckillVoucherStr)
local endTime = seckillVoucher.endTime



-- 判断库存是否充足
if(tonumber(stock) <= 0) then
    -- 库存不足，则直接返回
    return string.format('{"%s": %d}', 'code', 10002)
end
-- 3.2.判断用户是否下单 SISMEMBER orderKey userId
if(redis.call('sismember', orderKey, userId) == 1) then
    -- 3.3.存在，说明是重复下单，返回2
    return 2
end
-- 3.4.扣库存 incrby stockKey -1
redis.call('incrby', stockKey, -1)
-- 3.5.下单（保存用户）sadd orderKey userId
redis.call('sadd', orderKey, userId)
-- 3.6.发送消息到队列中， XADD stream.orders * k1 v1 k2 v2 ...
redis.call('xadd', 'stream.orders', '*', 'userId', userId, 'voucherId', voucherId, 'id', orderId)
return 0