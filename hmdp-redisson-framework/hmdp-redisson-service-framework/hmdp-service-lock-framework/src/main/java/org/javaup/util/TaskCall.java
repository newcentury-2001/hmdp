package org.javaup.util;

/**
 * @program: 黑马点评-plus升级版实战项目
 * @description: 任务调用类
 * @author: 阿星不是程序员
 **/
@FunctionalInterface
public interface TaskCall<V> {

    /**
     * 执行任务
     * @return 结果
     * */
    V call();
}
