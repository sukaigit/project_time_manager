package com.timemanager.controller;

import com.timemanager.common.Result;
import com.timemanager.dto.PageResult;
import com.timemanager.dto.WorkHourRequest;
import com.timemanager.entity.WorkHour;
import com.timemanager.service.WorkHourService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/work-hours")
public class WorkHourController {

    private final WorkHourService workHourService;

    public WorkHourController(WorkHourService workHourService) {
        this.workHourService = workHourService;
    }

    // ====== 获取当前用户信息 ======
    private Long getCurrentUserId(Authentication auth) {
        return (Long) auth.getDetails();
    }

    private String getCurrentRole(Authentication auth) {
        return auth.getAuthorities().stream()
                .findFirst()
                .map(GrantedAuthority::getAuthority)
                .map(a -> a.replace("ROLE_", ""))
                .orElse("USER");
    }

    // ====== 工时列表 ======
    @GetMapping
    public Result<PageResult<WorkHour>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String status,
            Authentication auth) {
        Long userId = getCurrentUserId(auth);
        String role = getCurrentRole(auth);
        PageResult<WorkHour> result = workHourService.listWorkHours(
                page, size, userId, role, projectId, startDate, endDate, status);
        return Result.success(result);
    }

    // ====== 预算检查 ======
    @GetMapping("/budget-check")
    public Result<Map<String, Object>> budgetCheck(
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) Long moduleId) {
        Map<String, Object> data = workHourService.budgetCheck(projectId, moduleId);
        return Result.success(data);
    }

    // ====== 填报 ======
    @PostMapping
    public Result<Map<String, Long>> create(@RequestBody WorkHourRequest req, Authentication auth) {
        if (req.getProjectId() == null) {
            return Result.error(400, "项目不能为空");
        }
        if (req.getWorkDate() == null || req.getWorkDate().isBlank()) {
            return Result.error(400, "工作日期不能为空");
        }
        if (req.getHours() == null || req.getHours().doubleValue() <= 0) {
            return Result.error(400, "工时必须大于0");
        }

        Long userId = getCurrentUserId(auth);
        String role = getCurrentRole(auth);
        WorkHour wh = workHourService.createWorkHour(req, userId, role);
        return Result.success(Map.of("id", wh.getId()));
    }

    // ====== 修改 ======
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody WorkHourRequest req) {
        workHourService.updateWorkHour(id, req);
        return Result.success();
    }

    // ====== 删除 ======
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        workHourService.deleteWorkHour(id);
        return Result.success();
    }
}
