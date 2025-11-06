import request from '@/utils/request'
export const getVoucherList = (shopId) => request.get('/voucher/list/' + shopId)
export const seckillVoucher = (id) =>
  request.post('/voucher-order/seckill/' + id)
// 轮询查询秒杀订单是否生成
export const getSeckillOrder = (orderId) =>
  request.post('/voucher-order/get/seckill/voucher/order', {
    orderId: String(orderId)
  })

// 进入页面或秒杀成功后，查询用户是否已购买该优惠券
export const getVoucherOrderRouter = (voucherId) =>
  request.post('/voucher-order-router/get', {
    voucherId: String(voucherId)
  })
