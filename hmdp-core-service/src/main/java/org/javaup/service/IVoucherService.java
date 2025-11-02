package org.javaup.service;

import org.javaup.dto.Result;
import org.javaup.dto.SeckillVoucherDto;
import org.javaup.dto.VoucherDto;
import org.javaup.entity.Voucher;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IVoucherService extends IService<Voucher> {

    Long addVoucher(VoucherDto voucherDto);
    
    Result queryVoucherOfShop(Long shopId);

    Long addSeckillVoucher(SeckillVoucherDto seckillVoucherDto);
}
