package com.timemanager.service;

import com.timemanager.common.BusinessException;
import com.timemanager.dto.PageResult;
import com.timemanager.dto.ProjectRequest;
import com.timemanager.entity.Project;
import com.timemanager.mapper.ProjectMapper;
import com.timemanager.service.LogService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class ProjectService {

    private final ProjectMapper projectMapper;
    private final LogService logService;

    public ProjectService(ProjectMapper projectMapper, LogService logService) {
        this.projectMapper = projectMapper;
        this.logService = logService;
    }

    private Long getCurrentUserId() {
        Object details = SecurityContextHolder.getContext().getAuthentication().getDetails();
        return details instanceof Long ? (Long) details : null;
    }

    public PageResult<Project> listProjects(int page, int size, String keyword) {
        int offset = (page - 1) * size;
        List<Project> list = projectMapper.findByPage(offset, size, keyword);
        long total = projectMapper.countByKeyword(keyword);
        return new PageResult<>(total, list);
    }

    public Project createProject(ProjectRequest req) {
        if (req.getName() == null || req.getName().isBlank()) {
            throw new BusinessException("项目名称不能为空");
        }
        if (req.getManagerId() == null) {
            throw new BusinessException("项目经理不能为空");
        }

        Project project = new Project();
        project.setName(req.getName());
        project.setDescription(req.getDescription());
        project.setManagerId(req.getManagerId());
        project.setStartDate(req.getStartDate() != null ? LocalDate.parse(req.getStartDate()) : null);
        project.setEndDate(req.getEndDate() != null ? LocalDate.parse(req.getEndDate()) : null);
        project.setStatus(req.getStatus() != null ? req.getStatus() : "ACTIVE");
        projectMapper.insert(project);
        logService.save(getCurrentUserId(), "CREATE", "项目:" + project.getName(),
                "{\"id\":" + project.getId() + ",\"name\":\"" + project.getName() + "\",\"managerId\":" + project.getManagerId() + "}");
        return project;
    }

    public void updateProject(Long id, ProjectRequest req) {
        Project project = projectMapper.findById(id);
        if (project == null) {
            throw new BusinessException("项目不存在");
        }
        if (req.getName() != null) project.setName(req.getName());
        if (req.getDescription() != null) project.setDescription(req.getDescription());
        if (req.getManagerId() != null) project.setManagerId(req.getManagerId());
        if (req.getStartDate() != null) project.setStartDate(LocalDate.parse(req.getStartDate()));
        if (req.getEndDate() != null) project.setEndDate(LocalDate.parse(req.getEndDate()));
        if (req.getStatus() != null) project.setStatus(req.getStatus());
        projectMapper.update(project);
        logService.save(getCurrentUserId(), "UPDATE", "项目:" + project.getName(),
                "{\"id\":" + id + ",\"name\":\"" + project.getName() + "\"}");
    }

    public void deleteProject(Long id) {
        Project project = projectMapper.findById(id);
        if (project == null) {
            throw new BusinessException("项目不存在");
        }
        projectMapper.delete(id);
        logService.save(getCurrentUserId(), "DELETE", "项目:" + project.getName(),
                "{\"id\":" + id + ",\"name\":\"" + project.getName() + "\"}");
    }

    public Project getProjectById(Long id) {
        Project project = projectMapper.findById(id);
        if (project == null) {
            throw new BusinessException("项目不存在");
        }
        return project;
    }
}
