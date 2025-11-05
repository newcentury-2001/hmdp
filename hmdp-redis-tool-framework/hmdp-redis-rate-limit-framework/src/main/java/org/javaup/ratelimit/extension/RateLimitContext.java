package org.javaup.ratelimit.extension;

import java.util.List;

/**
 * 限流执行上下文：包含当前执行的关键参数，用于扩展点消费。
 */
public class RateLimitContext {

    private Long voucherId;
    private Long userId;
    private String clientIp;

    private List<String> keys;
    private boolean useSliding;

    private int ipWindowMillis;
    private int ipMaxAttempts;
    private int userWindowMillis;
    private int userMaxAttempts;

    private Integer result;

    public RateLimitContext() {}

    public RateLimitContext(Long voucherId, Long userId, String clientIp,
                            List<String> keys, boolean useSliding,
                            int ipWindowMillis, int ipMaxAttempts,
                            int userWindowMillis, int userMaxAttempts) {
        this.voucherId = voucherId;
        this.userId = userId;
        this.clientIp = clientIp;
        this.keys = keys;
        this.useSliding = useSliding;
        this.ipWindowMillis = ipWindowMillis;
        this.ipMaxAttempts = ipMaxAttempts;
        this.userWindowMillis = userWindowMillis;
        this.userMaxAttempts = userMaxAttempts;
    }

    public Long getVoucherId() {
        return voucherId;
    }

    public void setVoucherId(Long voucherId) {
        this.voucherId = voucherId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getClientIp() {
        return clientIp;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    public List<String> getKeys() {
        return keys;
    }

    public void setKeys(List<String> keys) {
        this.keys = keys;
    }

    public boolean isUseSliding() {
        return useSliding;
    }

    public void setUseSliding(boolean useSliding) {
        this.useSliding = useSliding;
    }

    public int getIpWindowMillis() {
        return ipWindowMillis;
    }

    public void setIpWindowMillis(int ipWindowMillis) {
        this.ipWindowMillis = ipWindowMillis;
    }

    public int getIpMaxAttempts() {
        return ipMaxAttempts;
    }

    public void setIpMaxAttempts(int ipMaxAttempts) {
        this.ipMaxAttempts = ipMaxAttempts;
    }

    public int getUserWindowMillis() {
        return userWindowMillis;
    }

    public void setUserWindowMillis(int userWindowMillis) {
        this.userWindowMillis = userWindowMillis;
    }

    public int getUserMaxAttempts() {
        return userMaxAttempts;
    }

    public void setUserMaxAttempts(int userMaxAttempts) {
        this.userMaxAttempts = userMaxAttempts;
    }

    public Integer getResult() {
        return result;
    }

    public void setResult(Integer result) {
        this.result = result;
    }
}