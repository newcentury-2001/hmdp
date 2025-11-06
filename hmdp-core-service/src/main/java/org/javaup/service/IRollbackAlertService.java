package org.javaup.service;

import org.javaup.entity.RollbackFailureLog;

/**
 * 回滚失败通知服务：用于发送短信/邮件告警（可插拔实现）。
 */
public interface IRollbackAlertService {
    /** 发送回滚失败告警 */
    void sendRollbackAlert(RollbackFailureLog log);
}