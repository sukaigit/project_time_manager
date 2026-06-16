package com.timemanager.controller;

import com.timemanager.common.Result;
import com.timemanager.service.ApprovalService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/approvals")
public class ApprovalController {

    private final ApprovalService approvalService;

    public ApprovalController(ApprovalService approvalService) {
        this.approvalService = approvalService;
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
     * 待审批列表
     */
    @GetMapping("/pending")
    public Result<Map<String, Object>> pending(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication auth) {
        Long userId = getCurrentUserId(auth);
        String role = getCurrentRole(auth);
        Map<String, Object> data = approvalService.getPendingApprovals(page, size, userId, role);
        return Result.success(data);
    }

    /**
     * 批量审批
     */
    @PutMapping("/batch")
    public Result<Void> batch(@RequestBody Map<String, Object> request, Authentication auth) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) request.get("items");
        Long userId = getCurrentUserId(auth);
        String role = getCurrentRole(auth);
        approvalService.batchApprove(items, userId, role);
        return Result.success();
    }

    /**
     * 审批历史
     */
    @GetMapping("/history")
    public Result<Map<String, Object>> history(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication auth) {
        Long userId = getCurrentUserId(auth);
        Map<String, Object> data = approvalService.getApprovalHistory(page, size, userId);
        return Result.success(data);
    }
}
