package org.javaup.core;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

import static org.javaup.constant.Constant.DEFAULT_PREFIX_DISTINCTION_NAME;
import static org.javaup.constant.Constant.PREFIX_DISTINCTION_NAME;

/**
 * @program: 数据中台实战项目。 添加 阿星不是程序员 微信，添加时备注 中台 来获取项目的完整资料 
 * @description: spring工具
 * @author: 阿星不是程序员
 **/

public class SpringUtil implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    
    private static ConfigurableApplicationContext configurableApplicationContext;
    
    
    public static String getPrefixDistinctionName(){
        return configurableApplicationContext.getEnvironment().getProperty(PREFIX_DISTINCTION_NAME,
                DEFAULT_PREFIX_DISTINCTION_NAME);
    }
    
    @Override
    public void initialize(final ConfigurableApplicationContext applicationContext) {
        configurableApplicationContext = applicationContext;
    }
}
