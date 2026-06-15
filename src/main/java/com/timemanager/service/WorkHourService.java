package com.timemanager.service;

import com.timemanager.common.BusinessException;
import com.timemanager.dto.PageResult;
import com.timemanager.dto.WorkHourRequest;
import com.timemanager.entity.TaskModule;
import com.timemanager.entity.User;
import com.timemanager.entity.WorkHour;
import com.timemanager.mapper.TaskModuleMapper;
import com.timemanager.mapper.UserMapper;
import com.timemanager.mapper.WorkHourMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class WorkHourService {

    private final WorkHourMapper workHourMapper;
    private final TaskModuleMapper moduleMapper;
    private final UserMapper userMapper;

    public WorkHourService(WorkHourMapper workHourMapper, TaskModuleMapper moduleMapper, UserMapper userMapper) {
        this.workHourMapper = workHourMapper;
        this.moduleMapper = moduleMapper;
        this.userMapper = userMapper;
    }

    // ====== 查询 ======

    public PageResult<WorkHour> listWorkHours(int page, int size, Long currentUserId, String role,
                                               Long projectId, String startDate, String endDate, String status) {
        int offset = (page - 1) * size;
        boolean isAdmin = "ADMIN".equals(role);
        LocalDate sd = startDate != null && !startDate.isBlank() ? LocalDate.parse(startDate) : null;
        LocalDate ed = endDate != null && !endDate.isBlank() ? LocalDate.parse(endDate) : null;

        List<WorkHour> list = workHourMapper.findByPage(offset, size, currentUserId, isAdmin,
                projectId, sd, ed, status);
        long total = workHourMapper.countByFilters(currentUserId, isAdmin, projectId, sd, ed, status);
        return new PageResult<>(total, list);
    }

    // ====== 填报 ======

    public WorkHour createWorkHour(WorkHourRequest req, Long currentUserId, String role) {
        // 1. 校验必填字段
        if (req.getProjectId() == null) {
            throw new BusinessException("项目不能为空");
        }
        if (req.getWorkDate() == null || req.getWorkDate().isBlank()) {
            throw new BusinessException("工作日期不能为空");
        }
        if (req.getHours() == null || req.getHours().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("工时必须大于0");
        }

        // 2. 普通用户(USER)必传 moduleId
        boolean isUser = "USER".equals(role);
        if (isUser && req.getModuleId() == null) {
            throw new BusinessException("请选择任务模块");
        }

        // 3. 普通用户：预算检查（moduleId 不为空时）
        if (isUser && req.getModuleId() != null) {
            TaskModule module = moduleMapper.findById(req.getModuleId());
            if (module == null) {
                throw new BusinessException("模块不存在");
            }
            BigDecimal usedHours = workHourMapper.sumHoursByModule(req.getModuleId());
            BigDecimal estimated = BigDecimal.valueOf(module.getEstimatedHours());
            // 如果累计工时 >= 预估工时 → 已达上限
            if (usedHours.compareTo(estimated) >= 0) {
                throw new BusinessException("该模块预算已达上限");
            }
            // 加上本次填报的工时后是否超过
            if (usedHours.add(req.getHours()).compareTo(estimated) > 0) {
                throw new BusinessException("该模块预算已达上限");
            }
        }

        // 4. 创建工时记录
        WorkHour wh = new WorkHour();
        wh.setUserId(currentUserId);
        wh.setProjectId(req.getProjectId());
        wh.setModuleId(req.getModuleId());
        wh.setWorkDate(LocalDate.parse(req.getWorkDate()));
        wh.setHours(req.getHours());
        wh.setContent(req.getContent());
        wh.setStatus("PENDING");
        workHourMapper.insert(wh);
        return wh;
    }

    // ====== 修改 ======

    public void updateWorkHour(Long id, WorkHourRequest req) {
        WorkHour wh = workHourMapper.findById(id);
        if (wh == null) {
            throw new BusinessException("工时记录不存在");
        }

        // 审批状态检查：只有 PENDING/REJECTED 可修改
        if ("APPROVED".equals(wh.getStatus())) {
            throw new BusinessException("已审批的工时不可修改");
        }

        // 更新字段
        if (req.getProjectId() != null) wh.setProjectId(req.getProjectId());
        if (req.getModuleId() != null) wh.setModuleId(req.getModuleId());
        if (req.getWorkDate() != null && !req.getWorkDate().isBlank()) wh.setWorkDate(LocalDate.parse(req.getWorkDate()));
        if (req.getHours() != null) wh.setHours(req.getHours());
        if (req.getContent() != null) wh.setContent(req.getContent());
        // 修改后状态恢复 PENDING
        wh.setStatus("PENDING");

        workHourMapper.update(wh);
    }

    // ====== 删除 ======

    public void deleteWorkHour(Long id) {
        WorkHour wh = workHourMapper.findById(id);
        if (wh == null) {
            throw new BusinessException("工时记录不存在");
        }

        // 审批状态检查：只有 PENDING/REJECTED 可删除
        if ("APPROVED".equals(wh.getStatus())) {
            throw new BusinessException("已审批的工时不可删除");
        }

        workHourMapper.delete(id);
    }

    // ====== 预算检查 ======

    public Map<String, Object> budgetCheck(Long projectId, Long moduleId) {
        if (moduleId == null) {
            throw new BusinessException("模块ID不能为空");
        }
        TaskModule module = moduleMapper.findById(moduleId);
        if (module == null) {
            throw new BusinessException("模块不存在");
        }

        BigDecimal usedHours = workHourMapper.sumHoursByModule(moduleId);
        BigDecimal estimatedHours = BigDecimal.valueOf(module.getEstimatedHours());
        BigDecimal remainingHours = estimatedHours.subtract(usedHours);
        if (remainingHours.compareTo(BigDecimal.ZERO) < 0) {
            remainingHours = BigDecimal.ZERO;
        }
        boolean locked = usedHours.compareTo(estimatedHours) >= 0;

        Map<String, Object> result = new HashMap<>();
        result.put("estimatedHours", estimatedHours);
        result.put("usedHours", usedHours);
        result.put("remainingHours", remainingHours);
        result.put("locked", locked);
        return result;
    }
}
