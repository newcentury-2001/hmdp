package org.javaup.utils;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * @program: 黑马点评-plus升级版实战项目
 * @description: Redis数据
 * @author: 阿星不是程序员
 **/
@Data
public class RedisData {
    private LocalDateTime expireTime;
    private Object data;
}
