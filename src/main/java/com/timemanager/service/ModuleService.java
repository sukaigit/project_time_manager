package com.timemanager.service;

import com.timemanager.common.BusinessException;
import com.timemanager.dto.ModuleRequest;
import com.timemanager.entity.TaskModule;
import com.timemanager.mapper.TaskModuleMapper;
import com.timemanager.service.LogService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ModuleService {

    private final TaskModuleMapper moduleMapper;
    private final LogService logService;

    public ModuleService(TaskModuleMapper moduleMapper, LogService logService) {
        this.moduleMapper = moduleMapper;
        this.logService = logService;
    }

    private Long getCurrentUserId() {
        Object details = SecurityContextHolder.getContext().getAuthentication().getDetails();
        return details instanceof Long ? (Long) details : null;
    }

    public List<TaskModule> listModules(Long projectId) {
        return moduleMapper.findByProjectId(projectId);
    }

    public TaskModule createModule(Long projectId, ModuleRequest req) {
        if (req.getName() == null || req.getName().isBlank()) {
            throw new BusinessException("模块名称不能为空");
        }

        TaskModule module = new TaskModule();
        module.setProjectId(projectId);
        module.setName(req.getName());
        module.setDescription(req.getDescription());
        module.setEstimatedHours(req.getEstimatedHours() != null ? req.getEstimatedHours() : 0);
        moduleMapper.insert(module);
        logService.save(getCurrentUserId(), "CREATE", "模块:" + module.getName(),
                "{\"id\":" + module.getId() + ",\"name\":\"" + module.getName() + "\",\"projectId\":" + projectId + "}");
        return module;
    }

    public void updateModule(Long id, ModuleRequest req) {
        TaskModule module = moduleMapper.findById(id);
        if (module == null) {
            throw new BusinessException("模块不存在");
        }
        if (req.getName() != null) module.setName(req.getName());
        if (req.getDescription() != null) module.setDescription(req.getDescription());
        if (req.getEstimatedHours() != null) module.setEstimatedHours(req.getEstimatedHours());
        moduleMapper.update(module);
        logService.save(getCurrentUserId(), "UPDATE", "模块:" + module.getName(),
                "{\"id\":" + id + ",\"name\":\"" + module.getName() + "\"}");
    }

    public void deleteModule(Long id) {
        TaskModule module = moduleMapper.findById(id);
        if (module == null) {
            throw new BusinessException("模块不存在");
        }
        moduleMapper.delete(id);
        logService.save(getCurrentUserId(), "DELETE", "模块:" + module.getName(),
                "{\"id\":" + id + ",\"name\":\"" + module.getName() + "\"}");
    }
}
