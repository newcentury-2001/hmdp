package org.javaup.enums;

/**
 * @program: 数据中台实战项目。 添加 阿星不是程序员 微信，添加时备注 中台 来获取项目的完整资料 
 * @description: 接口返回code码
 * @author: 阿星不是程序员
 **/
public enum BaseCode {
    /**
     * 基础code码
     * */
    SUCCESS(0, "OK"),
    
    SECKILL_VOUCHER_NOT_EXIST(10001, "秒杀优惠券不存在"),
    
    SECKILL_VOUCHER_NOT_BEGIN(10002, "秒杀优惠券未开始"),
    
    SECKILL_VOUCHER_IS_OVER(10003, "秒杀优惠券已结束"),
    
    SECKILL_VOUCHER_STOCK_NOT_EXIST(10004, "秒杀优惠券库存不存在"),
    
    SECKILL_VOUCHER_STOCK_INSUFFICIENT(10005, "秒杀优惠券库存不足"),
    
    SECKILL_VOUCHER_CLAIM(10006, "秒杀优惠券已领取"),
    
    SECKILL_RATE_LIMIT_IP_EXCEEDED(10007, "请求过于频繁，请稍后再试"),
    
    SECKILL_RATE_LIMIT_USER_EXCEEDED(10008, "操作过于频繁，请稍后再试"),
    
    USER_NOT_EXIST(20000, "用户不存在"),
    ;
    
    private final Integer code;
    
    private String msg = "";
    
    BaseCode(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }
    
    public Integer getCode() {
        return this.code;
    }
    
    public String getMsg() {
        return this.msg == null ? "" : this.msg;
    }
    
    public static String getMsg(Integer code) {
        for (BaseCode re : BaseCode.values()) {
            if (re.code.intValue() == code.intValue()) {
                return re.msg;
            }
        }
        return "";
    }
    
    public static BaseCode getRc(Integer code) {
        for (BaseCode re : BaseCode.values()) {
            if (re.code.intValue() == code.intValue()) {
                return re;
            }
        }
        return null;
    }
}
