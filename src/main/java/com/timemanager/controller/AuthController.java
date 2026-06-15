package com.timemanager.controller;

import com.timemanager.common.Result;
import com.timemanager.dto.LoginRequest;
import com.timemanager.dto.LoginResponse;
import com.timemanager.dto.PasswordRequest;
import com.timemanager.service.AuthService;
import com.wf.captcha.SpecCaptcha;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final Map<String, String> captchaStore = new ConcurrentHashMap<>();

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        String cacheKey = req.getCaptchaId();
        String cached = captchaStore.remove(cacheKey);
        if (cached == null || !cached.equalsIgnoreCase(req.getCaptchaCode())) {
            return Result.error(400, "验证码错误");
        }
        LoginResponse resp = authService.login(req);
        return Result.success(resp);
    }

    @GetMapping("/captcha")
    public void captcha(@RequestParam(value = "t", required = false) String t,
                        HttpServletResponse response) throws Exception {
        SpecCaptcha captcha = new SpecCaptcha(130, 48, 4);
        String id = java.util.UUID.randomUUID().toString().replaceAll("-", "").substring(0, 16);
        captchaStore.put(id, captcha.text());
        response.setContentType("image/png");
        response.setHeader("X-Captcha-Id", id);
        captcha.out(response.getOutputStream());
    }

    @PutMapping("/password")
    public Result<Void> changePassword(@Valid @RequestBody PasswordRequest req) {
        authService.changePassword(req);
        return Result.success();
    }
}
