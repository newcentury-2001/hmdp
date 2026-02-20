package org.javaup.handler;


import org.javaup.core.SpringUtil;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;

/**
 * @program: 黑马点评-plus升级版实战项目
 * @description: 布隆过滤器处理器
 * @author: 阿星不是程序员
 **/
public class BloomFilterHandler {

    private final RBloomFilter<String> bloomFilter;

    public BloomFilterHandler(RedissonClient redissonClient, 
                              String name, 
                              Long expectedInsertions, 
                              Double falseProbability){
        RBloomFilter<String> bf = redissonClient.getBloomFilter(
                SpringUtil.getPrefixDistinctionName() 
                        + "-" 
                        + name);
        bf.tryInit(expectedInsertions == null ? 
                        20000L : expectedInsertions,
                falseProbability == null ? 
                        0.01D : falseProbability);
        this.bloomFilter = bf;
    }

    public boolean add(String data) {
        return bloomFilter.add(data);
    }

    public boolean contains(String data) {
        return bloomFilter.contains(data);
    }
}