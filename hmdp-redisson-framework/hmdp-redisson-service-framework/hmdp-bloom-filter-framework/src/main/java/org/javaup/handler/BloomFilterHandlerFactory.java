package org.javaup.handler;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * @program: 黑马点评-plus升级版实战项目
 * @description: 布隆过滤器处理工厂
 * @author: 阿星不是程序员
 **/
public class BloomFilterHandlerFactory implements ApplicationContextAware {

    private ApplicationContext applicationContext;

    public BloomFilterHandler get(String name){
        return applicationContext.getBean(name, BloomFilterHandler.class);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}