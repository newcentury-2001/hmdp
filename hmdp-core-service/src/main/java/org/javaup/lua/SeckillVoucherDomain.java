package org.javaup.lua;

import lombok.Data;

@Data
public class SeckillVoucherDomain {

    private Integer code;
    
    /** 扣减前库存数量 */
    private Integer beforeQty;
    
    /** 本次扣减数量 */
    private Integer deductQty;
    
    /** 扣减后库存数量 */
    private Integer afterQty;

}
