package com.timemanager.controller;

import com.timemanager.common.Result;
import com.timemanager.dto.PageResult;
import com.timemanager.entity.OperationLog;
import com.timemanager.service.LogService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/logs")
public class SystemController {

    private final LogService logService;

    public SystemController(LogService logService) {
        this.logService = logService;
    }

    @GetMapping
    public Result<PageResult<OperationLog>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        PageResult<OperationLog> result = logService.listLogs(page, size, userId, action, startDate, endDate);
        return Result.success(result);
    }
}
