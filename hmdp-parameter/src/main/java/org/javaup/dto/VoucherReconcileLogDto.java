package org.javaup.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * @program: 黑马点评-plus升级版实战项目。添加 阿星不是程序员 微信，添加时备注 点评 来获取项目的完整资料
 * @description: 对账日志
 * @author: 阿星不是程序员
 **/
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
public class VoucherReconcileLogDto implements Serializable {

    private static final long serialVersionUID = 1L;
    

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
}