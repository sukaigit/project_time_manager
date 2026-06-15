package com.timemanager.service;

import com.timemanager.common.BusinessException;
import com.timemanager.entity.Approval;
import com.timemanager.entity.User;
import com.timemanager.entity.WorkHour;
import com.timemanager.mapper.*;
import com.timemanager.service.LogService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class ApprovalService {

    private final WorkHourMapper workHourMapper;
    private final ProjectMapper projectMapper;
    private final ApprovalMapper approvalMapper;
    private final UserMapper userMapper;
    private final LogService logService;

    public ApprovalService(WorkHourMapper workHourMapper,
                           ProjectMapper projectMapper,
                           ApprovalMapper approvalMapper,
                           UserMapper userMapper,
                           LogService logService) {
        this.workHourMapper = workHourMapper;
        this.projectMapper = projectMapper;
        this.approvalMapper = approvalMapper;
        this.userMapper = userMapper;
        this.logService = logService;
    }

    /**
     * 获取待审批列表
     * PM：审批自己项目下普通用户的工时
     * DEPT_MANAGER：审批PM的工时
     */
    public Map<String, Object> getPendingApprovals(int page, int size, Long currentUserId, String role) {
        int offset = (page - 1) * size;
        List<Map<String, Object>> list;
        long total;

        if ("DEPT_MANAGER".equals(role)) {
            // 部门经理：审批PM提交的工时
            list = workHourMapper.findPendingByPm(offset, size);
            total = workHourMapper.countPendingByPm();
        } else if ("PM".equals(role)) {
            // PM：审批自己项目下普通用户的工时
            list = workHourMapper.findPendingByProjectManager(offset, size, currentUserId);
            total = workHourMapper.countPendingByProjectManager(currentUserId);
        } else {
            throw new BusinessException("无审批权限");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("total", total);
        result.put("list", list);
        return result;
    }

    /**
     * 批量审批
     */
    @Transactional
    public void batchApprove(List<Map<String, Object>> items, Long currentUserId, String role) {
        if (items == null || items.isEmpty()) {
            throw new BusinessException("审批列表不能为空");
        }

        for (Map<String, Object> item : items) {
            Object idObj = item.get("id");
            if (idObj == null) {
                throw new BusinessException("审批记录ID不能为空");
            }
            Long workHourId = Long.valueOf(idObj.toString());
            String status = (String) item.get("status");
            String comment = (String) item.get("comment");

            if (status == null || (!"APPROVED".equals(status) && !"REJECTED".equals(status))) {
                throw new BusinessException("审批状态无效");
            }

            // 查询工时记录
            WorkHour wh = workHourMapper.findById(workHourId);
            if (wh == null) {
                throw new BusinessException("工时记录不存在: " + workHourId);
            }
            if (!"PENDING".equals(wh.getStatus())) {
                throw new BusinessException("工时记录状态不是待审批: " + workHourId);
            }

            // 权限校验
            User submitter = userMapper.findById(wh.getUserId());
            if (submitter == null) {
                throw new BusinessException("提交用户不存在");
            }

            if ("PM".equals(role)) {
                // PM必须是自己项目下的
                var project = projectMapper.findById(wh.getProjectId());
                if (project == null || !currentUserId.equals(project.getManagerId())) {
                    throw new BusinessException("无权审批该项目工时: " + workHourId);
                }
                // PM不能审批其他PM的工时
                if ("PM".equals(submitter.getRole())) {
                    throw new BusinessException("无权审批项目经理的工时");
                }
            } else if ("DEPT_MANAGER".equals(role)) {
                // 部门经理只能审批PM的工时
                if (!"PM".equals(submitter.getRole())) {
                    throw new BusinessException("部门经理只能审批项目经理的工时");
                }
            }

            // 更新工时状态
            workHourMapper.updateStatus(workHourId, status);

            // 插入审批记录
            Approval approval = new Approval();
            approval.setWorkHourId(workHourId);
            approval.setApproverId(currentUserId);
            approval.setStatus(status);
            approval.setComment(comment);
            approval.setApproveTime(LocalDateTime.now());
            approvalMapper.insert(approval);

            // 记录操作日志
            String logAction = "APPROVED".equals(status) ? "APPROVE" : "REJECT";
            User approver = userMapper.findById(currentUserId);
            logService.save(currentUserId, logAction, "工时:" + workHourId,
                    "{\"workHourId\":" + workHourId + ",\"approver\":\"" + (approver != null ? approver.getName() : "") + "\",\"status\":\"" + status + "\"}");
        }
    }

    /**
     * 审批历史
     */
    public Map<String, Object> getApprovalHistory(int page, int size, Long currentUserId) {
        int offset = (page - 1) * size;
        var list = approvalMapper.findHistoryByApprover(offset, size, currentUserId);
        long total = approvalMapper.countHistoryByApprover(currentUserId);

        Map<String, Object> result = new HashMap<>();
        result.put("total", total);
        result.put("list", list);
        return result;
    }
}
