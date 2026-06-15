package com.timemanager.entity;

import java.time.LocalDateTime;

public class OperationLog {
    private Long id;
    private Long userId;
    private String action; // CREATE/UPDATE/DELETE/LOGIN/APPROVE/REJECT
    private String target;
    private String detail;
    private LocalDateTime createTime;
    // 非持久化
    private String userName;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getTarget() { return target; }
    public void setTarget(String target) { this.target = target; }

    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
}
