package org.javaup.constant;

/**
 * @program: 极度真实还原大麦网高并发实战项目。 添加 阿星不是程序员 微信，添加时备注 大麦 来获取项目的完整资料 
 * @description: 分布式锁 业务名管理
 * @author: 阿星不是程序员
 **/
public class DistributedLockConstants {
    
    /**
     * 修改用户信息
     * */
    public final static String UPDATE_USER_INFO_LOCK = "h_update_user_info_lock";
    
    /**
     * 修改秒杀优惠券
     * */
    public final static String UPDATE_SECKILL_VOUCHER_LOCK = "h_update_seckill_voucher_lock";
}
