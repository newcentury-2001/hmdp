package org.javaup.kafka.message;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SeckillVoucherMessage {

    private Long userId;
    
    private Long voucherId;
    
    private Long orderId;
    
    /** 唯一追踪标识，与订单记录关联 */
    private Long traceId;
    
    /** 扣减前库存数量 */
    private Integer beforeQty;
    
    /** 本次扣减数量 */
    private Integer changeQty;
    
    /** 扣减后库存数量 */
    private Integer afterQty;
}
