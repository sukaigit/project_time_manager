package com.timemanager.controller;

import com.timemanager.common.Result;
import com.timemanager.service.ReportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
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
     * 个人统计
     * GET /api/reports/personal?year=2026&month=6
     */
    @GetMapping("/personal")
    public Result<Map<String, Object>> personal(
            @RequestParam(defaultValue = "2026") int year,
            @RequestParam(defaultValue = "6") int month,
            Authentication auth) {
        Long userId = getCurrentUserId(auth);
        String role = getCurrentRole(auth);
        return Result.success(reportService.personalReport(year, month, userId, role));
    }

    /**
     * 项目统计
     * GET /api/reports/project?year=2026&month=6&projectId=1
     */
    @GetMapping("/project")
    public Result<Map<String, Object>> project(
            @RequestParam(defaultValue = "2026") int year,
            @RequestParam(defaultValue = "6") int month,
            @RequestParam Long projectId,
            Authentication auth) {
        Long userId = getCurrentUserId(auth);
        String role = getCurrentRole(auth);
        return Result.success(reportService.projectReport(year, month, projectId, userId, role));
    }

    /**
     * 部门统计
     * GET /api/reports/department?year=2026&month=6
     */
    @GetMapping("/department")
    public Result<Map<String, Object>> department(
            @RequestParam(defaultValue = "2026") int year,
            @RequestParam(defaultValue = "6") int month,
            Authentication auth) {
        Long userId = getCurrentUserId(auth);
        String role = getCurrentRole(auth);
        return Result.success(reportService.departmentReport(year, month, userId, role));
    }

    /**
     * Excel 导出
     * GET /api/reports/export?type=personal&year=2026&month=6&projectId=
     */
    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
            @RequestParam String type,
            @RequestParam(defaultValue = "2026") int year,
            @RequestParam(defaultValue = "6") int month,
            @RequestParam(required = false) Long projectId,
            Authentication auth) {
        Long userId = getCurrentUserId(auth);
        String role = getCurrentRole(auth);

        byte[] data;
        if ("project".equals(type) && projectId != null) {
            data = reportService.exportProjectExcel(year, month, projectId, userId, role);
        } else {
            data = reportService.exportExcel(type, year, month, userId, role);
        }

        String filename = type + "_" + year + "_" + month + ".xlsx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }
}
