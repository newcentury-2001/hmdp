package org.javaup.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * @program: 黑马点评-plus升级版实战项目
 * @description: 优惠券订阅批量DTO
 * @author: 阿星不是程序员
 **/
@Data
@EqualsAndHashCode(callSuper = false)
public class VoucherSubscribeBatchDto implements Serializable {
    
    @Serial
    private static final long serialVersionUID = 1L;
    
    /**
     * 优惠券id集合
     * */
    @NotNull
    private List<Long> voucherIdList;
}
