package org.javaup.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.javaup.entity.VoucherReconcileLog;
import org.javaup.mapper.VoucherReconcileLogMapper;
import org.javaup.service.IVoucherReconcileLogService;
import org.springframework.stereotype.Service;

@Service
public class VoucherReconcileLogServiceImpl extends ServiceImpl<VoucherReconcileLogMapper, VoucherReconcileLog>
        implements IVoucherReconcileLogService {
}