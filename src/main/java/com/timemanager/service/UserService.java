package com.timemanager.service;

import com.timemanager.common.BusinessException;
import com.timemanager.dto.PageResult;
import com.timemanager.dto.UserRequest;
import com.timemanager.entity.User;
import com.timemanager.mapper.UserMapper;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    private final UserMapper userMapper;
    private final BCryptPasswordEncoder encoder;
    private final LogService logService;

    public UserService(UserMapper userMapper, BCryptPasswordEncoder encoder, LogService logService) {
        this.userMapper = userMapper;
        this.encoder = encoder;
        this.logService = logService;
    }

    private Long getCurrentUserId() {
        Object details = SecurityContextHolder.getContext().getAuthentication().getDetails();
        return details instanceof Long ? (Long) details : null;
    }

    public PageResult<User> listUsers(int page, int size, String keyword) {
        int offset = (page - 1) * size;
        List<User> list = userMapper.findByPage(offset, size, keyword);
        long total = userMapper.countByKeyword(keyword);
        return new PageResult<>(total, list);
    }

    public User createUser(UserRequest req) {
        // check username uniqueness
        User exist = userMapper.findByUsername(req.getUsername());
        if (exist != null) {
            throw new BusinessException("用户名已存在");
        }

        User user = new User();
        user.setUsername(req.getUsername());
        user.setPassword(encoder.encode("123456"));
        user.setName(req.getName());
        user.setEmail(req.getEmail());
        user.setPhone(req.getPhone());
        user.setDepartment(req.getDepartment() != null ? req.getDepartment() : "研发与交付中心");
        user.setRole(req.getRole() != null ? req.getRole() : "USER");
        user.setStatus(req.getStatus() != null ? req.getStatus() : 1);
        user.setFirstLogin(1);
        userMapper.insert(user);
        logService.save(getCurrentUserId(), "CREATE", "用户:" + user.getUsername(), "{\"id\":" + user.getId() + ",\"name\":\"" + user.getName() + "\"}");
        return user;
    }

    public void updateUser(Long id, UserRequest req) {
        User user = userMapper.findById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (req.getName() != null) user.setName(req.getName());
        if (req.getEmail() != null) user.setEmail(req.getEmail());
        if (req.getPhone() != null) user.setPhone(req.getPhone());
        if (req.getDepartment() != null) user.setDepartment(req.getDepartment());
        if (req.getRole() != null) user.setRole(req.getRole());
        if (req.getStatus() != null) user.setStatus(req.getStatus());
        userMapper.update(user);
        logService.save(getCurrentUserId(), "UPDATE", "用户:" + user.getUsername(),
                "{\"id\":" + id + ",\"name\":\"" + (req.getName() != null ? req.getName() : "") + "\"}");
    }

    public void deleteUser(Long id) {
        User user = userMapper.findById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        userMapper.delete(id);
        logService.save(getCurrentUserId(), "DELETE", "用户:" + user.getUsername(),
                "{\"id\":" + id + ",\"username\":\"" + user.getUsername() + "\"}");
    }

    public void updateRole(Long id, String role) {
        User user = userMapper.findById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setRole(role);
        userMapper.update(user);
        logService.save(getCurrentUserId(), "UPDATE", "用户角色:" + user.getUsername(),
                "{\"id\":" + id + ",\"role\":\"" + role + "\"}");
    }

    public void resetPassword(Long id) {
        User user = userMapper.findById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setPassword(encoder.encode("123456"));
        user.setFirstLogin(1);
        userMapper.update(user);
        logService.save(getCurrentUserId(), "UPDATE", "用户密码重置:" + user.getUsername(),
                "{\"id\":" + id + ",\"username\":\"" + user.getUsername() + "\"}");
    }

    public List<User> listByRole(String role) {
        return userMapper.findByRole(role);
    }
}
