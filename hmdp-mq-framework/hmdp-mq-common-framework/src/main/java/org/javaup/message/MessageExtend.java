package org.javaup.message;

import cn.hutool.core.date.DateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.Map;
import java.util.UUID;


@Data
@NoArgsConstructor(force = true)
@AllArgsConstructor
@RequiredArgsConstructor
public final class MessageExtend<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 消息体
     */
    @NonNull
    private T messageBody;

    /**
     * 消息键（用于Kafka分区和去重）
     */
    private String key;

    /**
     * 业务元数据（将映射到Kafka Headers）
     */
    private Map<String, String> headers;

    /**
     * 唯一标识
     */
    private String uuid = UUID.randomUUID().toString();

    /**
     * 消息发送时间
     */
    private Date producerTime = DateTime.now();

    /**
     * 静态工厂：仅包装消息体
     */
    public static <T> MessageExtend<T> of(T body){
        return new MessageExtend<>(body);
    }

    /**
     * 静态工厂：包装消息体并设置key与headers
     */
    public static <T> MessageExtend<T> of(T body, String key, Map<String, String> headers){
        MessageExtend<T> msg = new MessageExtend<>(body);
        msg.setKey(key);
        msg.setHeaders(headers);
        return msg;
    }
}
