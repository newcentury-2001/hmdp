package org.javaup.service;

/**
 * @program: 黑马点评-plus升级版实战项目
 * @description: 秒杀访问令牌服务接口
 * @author: 阿星不是程序员
 **/
public interface ISeckillAccessTokenService {
  
    boolean isEnabled();
 
    String issueAccessToken(Long voucherId, Long userId);
    
    boolean validateAndConsume(Long voucherId, Long userId, String token);
}