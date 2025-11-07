package org.javaup.service;

/**
 * 自动发券成功后的用户通知服务接口
 */
public interface IAutoIssueNotifyService {

    /**
     * 发送自动发券成功通知（带去重控制）
     * @param voucherId 优惠券ID
     * @param userId 用户ID
     * @param orderId 订单ID
     */
    void sendAutoIssueNotify(Long voucherId, Long userId, Long orderId);
}