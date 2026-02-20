package org.javaup.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

/**
 * @program: 黑马点评-plus升级版实战项目
 * @description: 延迟优惠券提醒DTO
 * @author: 阿星不是程序员
 **/
@Data
@EqualsAndHashCode(callSuper = false)
public class DelayVoucherReminderDto implements Serializable {
    
    @Serial
    private static final long serialVersionUID = 1L;
    
    /**
     * 优惠券id
     * */
    @NotNull
    private Long voucherId;
    
    /**
     * 延迟时间，单位秒
     * */
    @NotNull
    private Integer delaySeconds;
}
