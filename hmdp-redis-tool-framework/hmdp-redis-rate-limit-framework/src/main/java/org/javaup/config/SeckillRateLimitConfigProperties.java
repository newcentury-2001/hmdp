package org.javaup.config;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.io.Serializable;

@Data
@ConfigurationProperties(prefix = SeckillRateLimitConfigProperties.PREFIX)
public class SeckillRateLimitConfigProperties implements Serializable {
    
    public static final String PREFIX = "rate-limit";

    /** 是否启用滑动窗口限流（默认关闭，使用固定窗口计数） */
    private Boolean enableSlidingWindow = false;

    /** IP限流窗口毫秒数 */
    private Integer ipWindowMillis = 5000;

    /** IP最大尝试次数 */
    private Integer ipMaxAttempts = 3;

    /** 用户限流窗口毫秒数 */
    private Integer userWindowMillis = 60000;

    /** 用户最大尝试次数 */
    private Integer userMaxAttempts = 5;
}