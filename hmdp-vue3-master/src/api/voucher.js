import request from '@/utils/request'
export const getVoucherList = (shopId) => request.get('/voucher/list/' + shopId)
export const seckillVoucher = (id) =>
  request.post('/voucher-order/seckill/' + id)
// 轮询查询秒杀订单是否生成
export const getSeckillOrder = (orderId) =>
  request.post('/voucher-order/get/seckill/voucher/order', {
    orderId: String(orderId)
  })
