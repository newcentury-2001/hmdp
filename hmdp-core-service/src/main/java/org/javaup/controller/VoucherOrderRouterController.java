package org.javaup.controller;


import jakarta.annotation.Resource;
import org.javaup.service.IVoucherOrderRouterService;
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
@RequestMapping("/voucher-order-router")
public class VoucherOrderRouterController {

    @Resource
    private IVoucherOrderRouterService voucherOrderRouterService;
}
