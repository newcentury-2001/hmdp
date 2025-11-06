package org.javaup.kafka.redis;

import cn.hutool.core.collection.ListUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.javaup.core.RedisKeyManage;
import org.javaup.enums.BaseCode;
import org.javaup.enums.LogType;
import org.javaup.enums.SeckillVoucherOrderOperate;
import org.javaup.lua.SeckillVoucherRollBackOperate;
import org.javaup.redis.RedisKeyBuild;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class RedisVoucherData {
    
    @Resource
    private SeckillVoucherRollBackOperate seckillVoucherRollBackOperate;
    
    public void rollbackRedisVoucherData(SeckillVoucherOrderOperate seckillVoucherOrderOperate,
                                         Long traceId,
                                         Long voucherId,
                                         Long userId,
                                         Long orderId) {
        try {
            // 回滚redis中的数据
            List<String> keys = ListUtil.of(
                    RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_STOCK_TAG_KEY, voucherId).getRelKey(),
                    RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_USER_TAG_KEY, voucherId).getRelKey(),
                    RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_TRACE_LOG_TAG_KEY, voucherId).getRelKey()
            );
            String[] args = new String[7];
            args[0] = String.valueOf(voucherId);
            args[1] = String.valueOf(userId);
            args[2] = String.valueOf(orderId);
            args[3] = String.valueOf(seckillVoucherOrderOperate.getCode());
            args[4] = String.valueOf(traceId);
            args[5] = String.valueOf(LogType.RESTORE.getCode());
            Integer result = seckillVoucherRollBackOperate.execute(
                    keys,
                    args
            );
            if (!result.equals(BaseCode.SUCCESS.getCode())) {
                //TODO
                log.error("回滚失败");
            }
        }catch (Exception e){
            //TODO
            log.error("回滚失败",e);
        }
    }
}
