package org.javaup.kafka.message;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SeckillVoucherMessage {

    private Long userId;
    
    private Long voucherId;
    
    private Long orderId;
}
