package com.timemanager.controller;

import com.timemanager.common.BusinessException;
import com.timemanager.common.Result;
import com.timemanager.dto.PageResult;
import com.timemanager.dto.RoleRequest;
import com.timemanager.dto.UserRequest;
import com.timemanager.entity.User;
import com.timemanager.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/pm-list")
    public Result<List<User>> listPms() {
        return Result.success(userService.listByRole("PM"));
    }

    @GetMapping
    public Result<PageResult<User>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        PageResult<User> result = userService.listUsers(page, size, keyword);
        return Result.success(result);
    }

    @PostMapping
    public Result<Map<String, Long>> create(@RequestBody UserRequest req) {
        if (req.getUsername() == null || req.getUsername().isBlank()) {
            return Result.error(400, "用户名不能为空");
        }
        if (req.getName() == null || req.getName().isBlank()) {
            return Result.error(400, "姓名不能为空");
        }
        User user = userService.createUser(req);
        return Result.success(Map.of("id", user.getId()));
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody UserRequest req) {
        userService.updateUser(id, req);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        userService.deleteUser(id);
        return Result.success();
    }

    @PutMapping("/{id}/role")
    public Result<Void> updateRole(@PathVariable Long id, @RequestBody RoleRequest req) {
        if (req.getRole() == null || req.getRole().isBlank()) {
            return Result.error(400, "角色不能为空");
        }
        userService.updateRole(id, req.getRole());
        return Result.success();
    }

    @PutMapping("/{id}/reset-password")
    public Result<Void> resetPassword(@PathVariable Long id) {
        userService.resetPassword(id);
        return Result.success();
    }
}
