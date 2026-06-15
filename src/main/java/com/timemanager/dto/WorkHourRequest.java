package com.timemanager.dto;

import java.math.BigDecimal;

public class WorkHourRequest {
    private Long projectId;
    private Long moduleId;
    private String workDate;
    private BigDecimal hours;
    private String content;

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }

    public Long getModuleId() { return moduleId; }
    public void setModuleId(Long moduleId) { this.moduleId = moduleId; }

    public String getWorkDate() { return workDate; }
    public void setWorkDate(String workDate) { this.workDate = workDate; }

    public BigDecimal getHours() { return hours; }
    public void setHours(BigDecimal hours) { this.hours = hours; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
