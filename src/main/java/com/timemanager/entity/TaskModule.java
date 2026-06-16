package com.timemanager.entity;

import java.time.LocalDateTime;

public class TaskModule {
    private Long id;
    private Long projectId;
    private String name;
    private String description;
    private Integer estimatedHours;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer isDeleted;
    // 非持久化字段，查询时计算
    private Double usedHours;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getEstimatedHours() { return estimatedHours; }
    public void setEstimatedHours(Integer estimatedHours) { this.estimatedHours = estimatedHours; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }

    public Integer getIsDeleted() { return isDeleted; }
    public void setIsDeleted(Integer isDeleted) { this.isDeleted = isDeleted; }

    public Double getUsedHours() { return usedHours; }
    public void setUsedHours(Double usedHours) { this.usedHours = usedHours; }
}
