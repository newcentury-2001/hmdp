package org.javaup.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

/**
 * @program: 黑马点评-plus升级版实战项目
 * @description: 优惠券订单DTO
 * @author: 阿星不是程序员
 **/
@Data
@EqualsAndHashCode(callSuper = false)
public class VoucherOrderDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    private Long id;

    /**
     * 下单的用户id
     */
    private Long userId;

    /**
     * 购买的代金券id
     */
    private Long voucherId;
    
    private String messageId;
    
    private Boolean autoIssue;

}
