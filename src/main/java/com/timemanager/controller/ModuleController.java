package com.timemanager.controller;

import com.timemanager.common.Result;
import com.timemanager.dto.ModuleRequest;
import com.timemanager.entity.TaskModule;
import com.timemanager.service.ModuleService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class ModuleController {

    private final ModuleService moduleService;

    public ModuleController(ModuleService moduleService) {
        this.moduleService = moduleService;
    }

    @GetMapping("/api/projects/{projectId}/modules")
    public Result<List<TaskModule>> list(@PathVariable Long projectId) {
        List<TaskModule> list = moduleService.listModules(projectId);
        return Result.success(list);
    }

    @PostMapping("/api/projects/{projectId}/modules")
    public Result<Map<String, Long>> create(@PathVariable Long projectId, @RequestBody ModuleRequest req) {
        if (req.getName() == null || req.getName().isBlank()) {
            return Result.error(400, "模块名称不能为空");
        }
        TaskModule module = moduleService.createModule(projectId, req);
        return Result.success(Map.of("id", module.getId()));
    }

    @PutMapping("/api/modules/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody ModuleRequest req) {
        moduleService.updateModule(id, req);
        return Result.success();
    }

    @DeleteMapping("/api/modules/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        moduleService.deleteModule(id);
        return Result.success();
    }
}
