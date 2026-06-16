package com.timemanager.controller;

import com.timemanager.common.Result;
import com.timemanager.service.DashboardService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

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

    /**
     * 今日概况
     */
    @GetMapping("/today")
    public Result<Map<String, Object>> today(Authentication auth) {
        Long userId = getCurrentUserId(auth);
        String role = getCurrentRole(auth);
        return Result.success(dashboardService.today(userId, role));
    }

    /**
     * 我的待办（待审批数）
     */
    @GetMapping("/pending")
    public Result<Map<String, Object>> pending(Authentication auth) {
        Long userId = getCurrentUserId(auth);
        String role = getCurrentRole(auth);
        return Result.success(dashboardService.pending(userId, role));
    }

    /**
     * 本月完成率
     */
    @GetMapping("/monthly-rate")
    public Result<Map<String, Object>> monthlyRate(Authentication auth) {
        Long userId = getCurrentUserId(auth);
        String role = getCurrentRole(auth);
        return Result.success(dashboardService.monthlyRate(userId, role));
    }

    /**
     * 项目工时分布
     */
    @GetMapping("/project-distribution")
    public Result<List<Map<String, Object>>> projectDistribution(Authentication auth) {
        Long userId = getCurrentUserId(auth);
        String role = getCurrentRole(auth);
        return Result.success(dashboardService.projectDistribution(userId, role));
    }

    /**
     * 整体概览
     */
    @GetMapping("/overview")
    public Result<Map<String, Object>> overview(Authentication auth) {
        Long userId = getCurrentUserId(auth);
        String role = getCurrentRole(auth);
        return Result.success(dashboardService.overview(userId, role));
    }
}
