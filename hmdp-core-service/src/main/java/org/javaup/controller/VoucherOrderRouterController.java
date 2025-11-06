package org.javaup.controller;


import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.javaup.dto.GetVoucherOrderRouterDto;
import org.javaup.dto.Result;
import org.javaup.service.IVoucherOrderRouterService;
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
@RequestMapping("/voucher-order-router")
public class VoucherOrderRouterController {

    @Resource
    private IVoucherOrderRouterService voucherOrderRouterService;
    
    @PostMapping("/get")
    public Result<Long> get(@Valid @RequestBody GetVoucherOrderRouterDto getVoucherOrderRouterDto) {
        return Result.ok(voucherOrderRouterService.get(getVoucherOrderRouterDto));
    }
}
