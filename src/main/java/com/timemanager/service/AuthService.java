package com.timemanager.service;

import com.timemanager.common.BusinessException;
import com.timemanager.config.JwtUtil;
import com.timemanager.dto.LoginRequest;
import com.timemanager.dto.LoginResponse;
import com.timemanager.dto.PasswordRequest;
import com.timemanager.entity.User;
import com.timemanager.mapper.UserMapper;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder encoder;

    public AuthService(UserMapper userMapper, JwtUtil jwtUtil, BCryptPasswordEncoder encoder) {
        this.userMapper = userMapper;
        this.jwtUtil = jwtUtil;
        this.encoder = encoder;
    }

    public LoginResponse login(LoginRequest req) {
        User user = userMapper.findByUsername(req.getUsername());
        if (user == null || !encoder.matches(req.getPassword(), user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }
        if (user.getStatus() == 0) {
            throw new BusinessException("账户已被禁用");
        }
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        boolean firstLogin = user.getFirstLogin() != null && user.getFirstLogin() == 1;
        return new LoginResponse(token, user.getId(), user.getUsername(), user.getName(), user.getRole(), firstLogin);
    }

    public void changePassword(PasswordRequest req) {
        String username = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = userMapper.findByUsername(username);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (!encoder.matches(req.getOldPassword(), user.getPassword())) {
            throw new BusinessException("旧密码错误");
        }
        user.setPassword(encoder.encode(req.getNewPassword()));
        user.setFirstLogin(0);
        userMapper.update(user);
    }
}
