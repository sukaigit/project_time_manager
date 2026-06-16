package com.timemanager.entity;

import java.time.LocalDateTime;

public class Approval {
    private Long id;
    private Long workHourId;
    private Long approverId;
    private String status; // APPROVED, REJECTED
    private String comment;
    private LocalDateTime approveTime;
    private LocalDateTime createTime;
    // 非持久化字段
    private String approverName;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getWorkHourId() { return workHourId; }
    public void setWorkHourId(Long workHourId) { this.workHourId = workHourId; }

    public Long getApproverId() { return approverId; }
    public void setApproverId(Long approverId) { this.approverId = approverId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public LocalDateTime getApproveTime() { return approveTime; }
    public void setApproveTime(LocalDateTime approveTime) { this.approveTime = approveTime; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public String getApproverName() { return approverName; }
    public void setApproverName(String approverName) { this.approverName = approverName; }
}
