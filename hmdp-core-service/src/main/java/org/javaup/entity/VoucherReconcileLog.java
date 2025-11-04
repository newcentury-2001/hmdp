package org.javaup.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 对账日志实体
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tb_voucher_reconcile_log")
public class VoucherReconcileLog implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(value = "id")
    private Long id;

    /** 订单id */
    private Long orderId;

    /** 用户id */
    private Long userId;

    /** 优惠券id */
    private Long voucherId;

    /** 消息id（uuid） */
    private String messageId;
    
    /** 详情描述 */
    private String detail;

    /** 改变之前库存数量 */
    private Integer beforeQty;

    /** 本次改变数量 */
    private Integer changeQty;

    /** 改变之后库存数量 */
    private Integer afterQty;

    /** 追踪唯一标识（与订单关联） */
    private Long traceId;
    
    /** 记录类型 -1:扣减 1:恢复 */
    private Integer logType;
    
    /** 业务类型：1创建订单成功；2创建订单超时；3创建订单失败 */
    private Integer businessType;
    
    /** 对账状态：1待处理；2异常；3不一致；4一致 */
    private Integer reconciliationStatus;
    
    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}