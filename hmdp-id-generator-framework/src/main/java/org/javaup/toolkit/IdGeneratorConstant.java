package org.javaup.toolkit;

/**
 * @program: 黑马点评-plus升级版实战项目
 * @description: ID生成器常量
 * @author: 阿星不是程序员
 **/
public class IdGeneratorConstant {
    /**
     * 机器标识位数
     */
    public static final long WORKER_ID_BITS = 5L;
    public static final long DATA_CENTER_ID_BITS = 5L;
    public static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);
    public static final long MAX_DATA_CENTER_ID = ~(-1L << DATA_CENTER_ID_BITS);
}
