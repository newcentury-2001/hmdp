package org.javaup.service;

import org.javaup.dto.Result;
import org.javaup.dto.VoucherOrderDto;
import org.javaup.entity.VoucherOrder;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IVoucherOrderService extends IService<VoucherOrder> {

    Result<Long> seckillVoucher(Long voucherId);

    void createVoucherOrderV1(VoucherOrder voucherOrder);
    
    void createVoucherOrderV2(VoucherOrderDto voucherOrderDto);
}
