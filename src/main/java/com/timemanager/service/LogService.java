package com.timemanager.service;

import com.timemanager.dto.PageResult;
import com.timemanager.entity.OperationLog;
import com.timemanager.mapper.OperationLogMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
public class LogService {

    private final OperationLogMapper operationLogMapper;

    public LogService(OperationLogMapper operationLogMapper) {
        this.operationLogMapper = operationLogMapper;
    }

    /**
     * 记录操作日志
     */
    public void save(Long userId, String action, String target, String detail) {
        OperationLog log = new OperationLog();
        log.setUserId(userId);
        log.setAction(action);
        log.setTarget(target);
        log.setDetail(detail);
        operationLogMapper.insert(log);
    }

    /**
     * 分页查询操作日志
     */
    public PageResult<OperationLog> listLogs(int page, int size, Long userId,
                                              String action, String startDate, String endDate) {
        int offset = (page - 1) * size;
        LocalDateTime sd = startDate != null && !startDate.isBlank()
                ? LocalDate.parse(startDate).atStartOfDay() : null;
        LocalDateTime ed = endDate != null && !endDate.isBlank()
                ? LocalDate.parse(endDate).atTime(LocalTime.MAX) : null;

        List<OperationLog> list = operationLogMapper.findByPage(offset, size, userId, action, sd, ed);
        long total = operationLogMapper.countByFilters(userId, action, sd, ed);
        return new PageResult<>(total, list);
    }
}
