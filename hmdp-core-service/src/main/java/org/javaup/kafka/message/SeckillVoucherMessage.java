package org.javaup.kafka.message;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Data;

/**
 * @program: 黑马点评-plus升级版实战项目。
 * @description: 秒杀券消息
 * @author: 阿星不是程序员
 **/

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SeckillVoucherMessage {

    private Long userId;
    
    private Long voucherId;
    
    private Long orderId;

    private Long traceId;

    private Integer beforeQty;
    
    private Integer changeQty;
    
    private Integer afterQty;
    
    private Boolean autoIssue;
}
