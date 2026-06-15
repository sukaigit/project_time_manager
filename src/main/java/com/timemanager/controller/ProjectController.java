package com.timemanager.controller;

import com.timemanager.common.Result;
import com.timemanager.dto.PageResult;
import com.timemanager.dto.ProjectRequest;
import com.timemanager.entity.Project;
import com.timemanager.service.ProjectService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping
    public Result<PageResult<Project>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        PageResult<Project> result = projectService.listProjects(page, size, keyword);
        return Result.success(result);
    }

    @PostMapping
    public Result<Map<String, Long>> create(@RequestBody ProjectRequest req) {
        if (req.getName() == null || req.getName().isBlank()) {
            return Result.error(400, "项目名称不能为空");
        }
        if (req.getManagerId() == null) {
            return Result.error(400, "项目经理不能为空");
        }
        Project project = projectService.createProject(req);
        return Result.success(Map.of("id", project.getId()));
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody ProjectRequest req) {
        projectService.updateProject(id, req);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        projectService.deleteProject(id);
        return Result.success();
    }
}
