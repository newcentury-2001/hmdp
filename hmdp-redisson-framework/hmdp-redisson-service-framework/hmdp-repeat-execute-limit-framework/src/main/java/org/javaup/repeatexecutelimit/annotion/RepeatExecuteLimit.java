package org.javaup.repeatexecutelimit.annotion;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * @program: 黑马点评-plus升级版实战项目
 * @description: 重复执行限制注解
 * @author: 阿星不是程序员
 **/
@Target(value= {ElementType.TYPE, ElementType.METHOD})
@Retention(value= RetentionPolicy.RUNTIME)
public @interface RepeatExecuteLimit {
    
    String name() default "";
   
    String [] keys();
    
    long durationTime() default 0L;
    
    String message() default "提交频繁，请稍后重试";
    
}
