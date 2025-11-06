package org.javaup.controller;


import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.javaup.dto.CancelVoucherOrderDto;
import org.javaup.dto.GetVoucherOrderByVoucherIdDto;
import org.javaup.dto.GetVoucherOrderDto;
import org.javaup.dto.Result;
import org.javaup.service.IVoucherOrderService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author 虎哥
 */
@RestController
@RequestMapping("/voucher-order")
public class VoucherOrderController {

    @Resource
    private IVoucherOrderService voucherOrderService;

    @PostMapping("/seckill/{id}")
    public Result<Long> seckillVoucher(@PathVariable("id") Long voucherId) {
        return voucherOrderService.seckillVoucher(voucherId);
    }
    
    @PostMapping("/get/seckill/voucher/order-id")
    public Result<Long> getSeckillVoucherOrder(@Valid @RequestBody GetVoucherOrderDto getVoucherOrderDto) {
        return Result.ok(voucherOrderService.getSeckillVoucherOrder(getVoucherOrderDto));
    }
    
    @PostMapping("/get/seckill/voucher/order-id/by/voucher-id")
    public Result<Long> getSeckillVoucherOrderIdByVoucherId(@Valid @RequestBody GetVoucherOrderByVoucherIdDto getVoucherOrderByVoucherIdDto) {
        return Result.ok(voucherOrderService.getSeckillVoucherOrderIdByVoucherId(getVoucherOrderByVoucherIdDto));
    }
    
    @PostMapping("/cancel")
    public Result<Boolean> cancel(@Valid @RequestBody CancelVoucherOrderDto cancelVoucherOrderDto) {
        return Result.ok(voucherOrderService.cancel(cancelVoucherOrderDto));
    }
}
