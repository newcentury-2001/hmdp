package org.javaup.core;

import org.javaup.context.DelayQueuePart;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @program: 黑马点评-plus升级版实战项目
 * @description: 延迟队列消费者
 * @author: 阿星不是程序员
 **/
@Slf4j
public class DelayConsumerQueue extends DelayBaseQueue {

    private final AtomicInteger listenStartThreadCount = new AtomicInteger(1);

    private final AtomicInteger executeTaskThreadCount = new AtomicInteger(1);

    private final ThreadPoolExecutor listenStartThreadPool;

    private final ThreadPoolExecutor executeTaskThreadPool;

    private final AtomicBoolean runFlag = new AtomicBoolean(false);

    private final ConsumerTask consumerTask;

    public DelayConsumerQueue(DelayQueuePart delayQueuePart, String relTopic) {
        super(delayQueuePart.getDelayQueueBasePart().getRedissonClient(), relTopic);
        this.listenStartThreadPool = new ThreadPoolExecutor(1, 1, 60,
                TimeUnit.SECONDS, new LinkedBlockingQueue<>(), r -> new Thread(
                        Thread.currentThread().getThreadGroup(), r,
                "listen-start-thread-" + listenStartThreadCount.getAndIncrement()
            )
        );
        this.executeTaskThreadPool = new ThreadPoolExecutor(
                delayQueuePart.getDelayQueueBasePart().getDelayQueueProperties().getCorePoolSize(),
                delayQueuePart.getDelayQueueBasePart().getDelayQueueProperties().getMaximumPoolSize(),
                delayQueuePart.getDelayQueueBasePart().getDelayQueueProperties().getKeepAliveTime(),
                delayQueuePart.getDelayQueueBasePart().getDelayQueueProperties().getUnit(),
                new LinkedBlockingQueue<>(delayQueuePart.getDelayQueueBasePart().getDelayQueueProperties().getWorkQueueSize()),
                r -> new Thread(Thread.currentThread().getThreadGroup(), r,
                        "delay-queue-consume-thread-" + executeTaskThreadCount.getAndIncrement()));
        this.consumerTask = delayQueuePart.getConsumerTask();
    }

    public synchronized void listenStart() {
        if (!runFlag.get()) {
            runFlag.set(true);
            log.info("[DelayConsumerQueue] 启动监听线程: {}", relTopic);
            listenStartThreadPool.execute(() -> {
                log.info("[DelayConsumerQueue] 监听线程已启动: {}", relTopic);
                while (!Thread.interrupted()) {
                    try {
                        assert blockingQueue != null;
                        log.debug("[DelayConsumerQueue] 等待消息: {}", relTopic);
                        String content = blockingQueue.take();
                        log.debug("[DelayConsumerQueue] 接收到消息: {}, 内容长度: {}", relTopic, content.length());
                        executeTaskThreadPool.execute(() -> {
                            try {
                                log.debug("[DelayConsumerQueue] 执行消息消费: {}", relTopic);
                                consumerTask.execute(content);
                                log.debug("[DelayConsumerQueue] 消息消费完成: {}", relTopic);
                            } catch (Exception e) {
                                log.error("[DelayConsumerQueue] 消费者执行错误: {}", relTopic, e);
                            }
                        });
                    } catch (InterruptedException e) {
                        log.info("[DelayConsumerQueue] 监听线程被中断: {}", relTopic);
                        destroy(executeTaskThreadPool);
                        break;
                    } catch (org.redisson.RedissonShutdownException e) {
                        log.info("[DelayConsumerQueue] Redisson已关闭，停止监听: {}", relTopic);
                        destroy(executeTaskThreadPool);
                        break;
                    } catch (Throwable e) {
                        log.error("[DelayConsumerQueue] 阻塞队列获取消息错误: {}", relTopic, e);
                    }
                }
            });
        } else {
            log.info("[DelayConsumerQueue] 监听线程已经启动: {}", relTopic);
        }
    }

    public void destroy(ExecutorService executorService) {
        try {
            if (Objects.nonNull(executorService)) {
                executorService.shutdown();
            }
        } catch (Exception e) {
            log.error("destroy error", e);
        }
    }
}
