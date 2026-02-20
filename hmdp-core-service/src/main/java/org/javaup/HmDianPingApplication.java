package org.javaup;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * @program: 黑马点评-plus升级版实战项目。
 * @description: 服务启动-黑马点评普通版本和plus版本使用
 **/
@EnableAspectJAutoProxy(exposeProxy = true)
@MapperScan("org.javaup.mapper")
@SpringBootApplication
public class HmDianPingApplication {

    public static void main(String[] args) {
        SpringApplication.run(HmDianPingApplication.class, args);
    }

}
