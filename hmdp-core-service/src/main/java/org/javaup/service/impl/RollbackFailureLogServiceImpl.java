package org.javaup.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.javaup.entity.RollbackFailureLog;
import org.javaup.mapper.RollbackFailureLogMapper;
import org.javaup.service.IRollbackFailureLogService;
import org.springframework.stereotype.Service;

/**
 * @program: 黑马点评-plus升级版实战项目
 * @description: 回滚失败日志服务实现
 * @author: 阿星不是程序员
 **/
@Service
public class RollbackFailureLogServiceImpl extends ServiceImpl<RollbackFailureLogMapper, RollbackFailureLog>
        implements IRollbackFailureLogService {
}