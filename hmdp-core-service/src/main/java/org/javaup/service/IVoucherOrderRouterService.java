package org.javaup.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.javaup.dto.GetVoucherOrderRouterDto;
import org.javaup.entity.VoucherOrderRouter;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IVoucherOrderRouterService extends IService<VoucherOrderRouter> {
    
    Long get(GetVoucherOrderRouterDto getVoucherOrderRouterDto);
}
