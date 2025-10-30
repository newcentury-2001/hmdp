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
    
    SYSTEM_ERROR(-1,"系统异常，请稍后重试"),
    
    PARAMETER_ERROR(-2,"参数验证异常"),
    
    DATA_SOURCE_CLOSE_ERROR(-3,"数据源关闭失败"),
    
    USER_NOT_LOGIN(1001,"用户未登录"),
    
    UID_WORK_ID_ERROR(5000,"uid_work_id设置失败"),
    
    COLLECT_TYPE_NOT_EXIST(5001,"采集方式不存在"),
    
    METRIC_COLLECT_TYPE(5002,"指标收集类型错误"),
    
    COLLECT_SOURCE_NAME_EMPTY(5003,"收集源头名字为空"),
    
    COLLECT_TYPE_EMPTY(5004,"收集方式为空"),
    
    COLLECT_DETAIL_EMPTY(5005,"具体的收集实现为空"),
    
    METRIC_TIME_FORMAT_EMPTY(5006,"统计的时间格式为空"),
    
    ARGUMENTS_EMPTY(5007,"指标统计的参数为空"),
    
    FUNCTION_TYPE_EMPTY(5008,"函数类型为空"),
    
    EXPRESSION_EMPTY(5009,"表达式为空"),
    
    VIDEO_DIMENSION_TYPE_NOT_EXIST(5010,"视频维度不存在"),
    
    VIDEO_DIMENSION_TYPE_IMPL_NOT_EXIST(5011,"视频维度查询具体实现不存在"),
    
    RULE_NOT_EXIST(511,"规则不存在"),
    
    METRIC_NOT_EXIST(512,"指标不存在"), 
    
    DATA_SAVE_NOT_EXIST(513, "数据保存方式不存在"),
    
    DATE_TYPE_NOT_EXIST(514, "时间类型不存在"),
    
    MESSAGE_NOT_EXIST(515, "时间类型不存在"),
    
    RECONCILIATION_NOT_EXIST(516, "对账不存在"),
    
    COLLECT_TYPE_HANDLER_NOT_EXIST(517, "采集类型处理器不存在"),
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
