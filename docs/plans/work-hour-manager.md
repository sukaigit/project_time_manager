# 工时管理系统 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use Superpowers TDD（RED→GREEN→REFACTOR）

**目标：** 构建公司研发与交付中心的工时管理系统，覆盖工时填报、审批、大屏、报表全流程。

**架构：** Spring Boot 3.x + MyBatis + MySQL 8.0 后端，原生 HTML/JS + Bootstrap 5 + ECharts 前端，JWT 认证。

**技术栈：** Spring Boot, MyBatis, MySQL, JWT, Bootstrap 5, ECharts, Inter 字体

---

### Task 1: 项目骨架 + 数据库初始化

**文件：**
- Create: `pom.xml`
- Create: `src/main/resources/application.yml`
- Create: `src/main/resources/sql/init.sql`
- Create: `src/main/java/com/timemanager/TimeManagerApplication.java`

- [ ] **Step 1: 创建 Spring Boot 项目骨架**

创建 `pom.xml`：
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.5</version>
    </parent>
    <groupId>com.timemanager</groupId>
    <artifactId>project-time-manager</artifactId>
    <version>1.0.0</version>
    <name>project-time-manager</name>
    <dependencies>
        <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-web</artifactId></dependency>
        <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-security</artifactId></dependency>
        <dependency><groupId>org.mybatis.spring.boot</groupId><artifactId>mybatis-spring-boot-starter</artifactId><version>3.0.3</version></dependency>
        <dependency><groupId>com.mysql</groupId><artifactId>mysql-connector-j</artifactId><scope>runtime</scope></dependency>
        <dependency><groupId>io.jsonwebtoken</groupId><artifactId>jjwt-api</artifactId><version>0.12.5</version></dependency>
        <dependency><groupId>io.jsonwebtoken</groupId><artifactId>jjwt-impl</artifactId><version>0.12.5</version><scope>runtime</scope></dependency>
        <dependency><groupId>io.jsonwebtoken</groupId><artifactId>jjwt-jackson</artifactId><version>0.12.5</version><scope>runtime</scope></dependency>
        <dependency><groupId>com.github.whvcse</groupId><artifactId>easy-captcha</artifactId><version>1.6.2</version></dependency>
        <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-validation</artifactId></dependency>
    </dependencies>
</project>
```

- [ ] **Step 2: 创建 application.yml**

```yaml
server:
  port: 8080
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/project_time_manager?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
    username: root
    password: root
    driver-class-name: com.mysql.cj.jdbc.Driver
mybatis:
  mapper-locations: classpath:mapper/*.xml
  type-aliases-package: com.timemanager.entity
  configuration:
    map-underscore-to-camel-case: true
jwt:
  secret: YourSuperSecretKeyForJWTTokenGeneration2024MustBe256BitsLong
  expiration: 86400000
```

- [ ] **Step 3: 创建数据库初始化 SQL**

`src/main/resources/sql/init.sql`：
```sql
CREATE DATABASE IF NOT EXISTS project_time_manager DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE project_time_manager;

CREATE TABLE tb_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100),
    phone VARCHAR(20),
    role VARCHAR(20) NOT NULL DEFAULT 'USER' COMMENT 'USER/PM/DEPT_MANAGER/ADMIN',
    status TINYINT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT DEFAULT 0
);

CREATE TABLE tb_project (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    manager_id BIGINT NOT NULL,
    start_date DATE,
    end_date DATE,
    status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT 'ACTIVE/FINISHED',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT DEFAULT 0,
    FOREIGN KEY (manager_id) REFERENCES tb_user(id)
);

CREATE TABLE tb_task_module (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    estimated_hours DECIMAL(10,2) DEFAULT 0 COMMENT '预估工时预算',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT DEFAULT 0,
    FOREIGN KEY (project_id) REFERENCES tb_project(id)
);

CREATE TABLE tb_work_hour (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    project_id BIGINT NOT NULL,
    module_id BIGINT COMMENT '任务模块ID，项目经理填工时可为空',
    work_date DATE NOT NULL,
    hours DECIMAL(4,1) NOT NULL COMMENT '工时（小时）',
    content TEXT COMMENT '工作内容',
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/REJECTED',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES tb_user(id),
    FOREIGN KEY (project_id) REFERENCES tb_project(id),
    FOREIGN KEY (module_id) REFERENCES tb_task_module(id)
);

CREATE TABLE tb_approval (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    work_hour_id BIGINT NOT NULL,
    approver_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL COMMENT 'APPROVED/REJECTED',
    comment TEXT,
    approve_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (work_hour_id) REFERENCES tb_work_hour(id),
    FOREIGN KEY (approver_id) REFERENCES tb_user(id)
);

CREATE TABLE tb_operation_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    action VARCHAR(100) NOT NULL,
    target VARCHAR(200),
    detail TEXT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES tb_user(id)
);

-- 预置系统管理员
INSERT INTO tb_user (username, password, name, role) VALUES
('admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '系统管理员', 'ADMIN');
```

- [ ] **Step 4: 创建 Spring Boot 启动类**

```java
package com.timemanager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TimeManagerApplication {
    public static void main(String[] args) {
        SpringApplication.run(TimeManagerApplication.class, args);
    }
}
```

- [ ] **Step 5: 提交**

```bash
git add pom.xml src/main/resources/ src/main/java/com/timemanager/TimeManagerApplication.java
git commit -m "feat: init spring boot project skeleton"
```

### Task 2: JWT 认证 + Spring Security 配置

**文件：**
- Create: `src/main/java/com/timemanager/config/SecurityConfig.java`
- Create: `src/main/java/com/timemanager/config/JwtUtil.java`
- Create: `src/main/java/com/timemanager/config/JwtAuthFilter.java`
- Create: `src/main/java/com/timemanager/entity/User.java`
- Create: `src/main/java/com/timemanager/entity/Role.java`
- Create: `src/main/java/com/timemanager/mapper/UserMapper.java`
- Create: `src/main/resources/mapper/UserMapper.xml`
- Create: `src/main/java/com/timemanager/dto/LoginRequest.java`
- Create: `src/main/java/com/timemanager/dto/LoginResponse.java`
- Create: `src/main/java/com/timemanager/dto/ApiResponse.java`
- Create: `src/main/java/com/timemanager/controller/AuthController.java`
- Create: `src/main/java/com/timemanager/service/AuthService.java`

- [ ] **Step 1: 创建实体类**

```java
// User.java
package com.timemanager.entity;
import java.time.LocalDateTime;
public class User {
    private Long id;
    private String username;
    private String password;
    private String name;
    private String email;
    private String phone;
    private String role; // USER, PM, DEPT_MANAGER, ADMIN
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer isDeleted;
    // getters/setters
}
```

- [ ] **Step 2: 创建 JwtUtil**

```java
package com.timemanager.config;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {
    @Value("${jwt.secret}")
    private String secret;
    @Value("${jwt.expiration}")
    private long expiration;

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(Long userId, String username, String role) {
        return Jwts.builder()
            .claim("userId", userId)
            .claim("username", username)
            .claim("role", role)
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + expiration))
            .signWith(getKey())
            .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser().verifyWith(getKey()).build()
            .parseSignedClaims(token).getPayload();
    }
}
```

- [ ] **Step 3: 创建 JwtAuthFilter**

```java
package com.timemanager.config;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import io.jsonwebtoken.Claims;
import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;
    public JwtAuthFilter(JwtUtil jwtUtil) { this.jwtUtil = jwtUtil; }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            try {
                Claims claims = jwtUtil.parseToken(auth.substring(7));
                Long userId = claims.get("userId", Long.class);
                String username = claims.get("username", String.class);
                String role = claims.get("role", String.class);
                UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(username, null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role)));
                authToken.setDetails(userId);
                SecurityContextHolder.getContext().setAuthentication(authToken);
            } catch (Exception e) {
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(request, response);
    }
}
```

- [ ] **Step 4: 创建 SecurityConfig**

```java
package com.timemanager.config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private final JwtAuthFilter jwtAuthFilter;
    public SecurityConfig(JwtAuthFilter jwtAuthFilter) { this.jwtAuthFilter = jwtAuthFilter; }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**", "/captcha", "/").permitAll()
                .requestMatchers("/css/**", "/js/**", "/lib/**").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
}
```

- [ ] **Step 5: 创建 AuthController（登录/退出/验证码）**

```java
package com.timemanager.controller;
import com.timemanager.dto.ApiResponse;
import com.timemanager.dto.LoginRequest;
import com.timemanager.dto.LoginResponse;
import com.timemanager.service.AuthService;
import com.wf.captcha.SpecCaptcha;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final Map<String, String> captchaStore = new ConcurrentHashMap<>();

    public AuthController(AuthService authService) { this.authService = authService; }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@RequestBody LoginRequest req) {
        String cacheKey = req.getCaptchaId();
        String cached = captchaStore.remove(cacheKey);
        if (cached == null || !cached.equalsIgnoreCase(req.getCaptchaCode())) {
            return ApiResponse.error(400, "验证码错误");
        }
        LoginResponse resp = authService.login(req);
        return ApiResponse.success(resp);
    }

    @GetMapping("/captcha")
    public void captcha(HttpServletResponse response) throws Exception {
        SpecCaptcha captcha = new SpecCaptcha(130, 48, 4);
        String id = java.util.UUID.randomUUID().toString().substring(0, 8);
        captchaStore.put(id, captcha.text());
        response.setContentType("image/png");
        // 返回图片流 + id 放在响应头或 query
    }
}
```

- [ ] **Step 6: AuthService 登录逻辑**

```java
package com.timemanager.service;
import com.timemanager.config.JwtUtil;
import com.timemanager.dto.LoginRequest;
import com.timemanager.dto.LoginResponse;
import com.timemanager.entity.User;
import com.timemanager.mapper.UserMapper;
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
            throw new RuntimeException("用户名或密码错误");
        }
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        return new LoginResponse(token, user.getName(), user.getRole());
    }
}
```

- [ ] **Step 7: 创建基础 DTO 和 Mapper**

```java
// UserMapper.java
package com.timemanager.mapper;
import com.timemanager.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface UserMapper {
    User findByUsername(@Param("username") String username);
    User findById(@Param("id") Long id);
    List<User> findAll();
    void insert(User user);
    void update(User user);
    void delete(@Param("id") Long id);
}
```

- [ ] **Step 8: 提交**

```bash
git add src/main/java/com/timemanager/config/ src/main/java/com/timemanager/entity/ src/main/java/com/timemanager/mapper/ src/main/java/com/timemanager/dto/ src/main/java/com/timemanager/controller/AuthController.java src/main/java/com/timemanager/service/AuthService.java src/main/resources/mapper/
git commit -m "feat: jwt auth and security config"
```

### Task 3: 登录页 + 首页布局（前端）

**文件：**
- Create: `src/main/resources/static/index.html`
- Create: `src/main/resources/static/login.html`
- Create: `src/main/resources/static/css/style.css`
- Create: `src/main/resources/static/js/api.js`
- Create: `src/main/resources/static/js/app.js`

- [ ] **Step 1: 创建前端目录结构和 api.js**

```javascript
// js/api.js
const API = {
    base: '',
    async request(path, options = {}) {
        const token = localStorage.getItem('token');
        const headers = { 'Content-Type': 'application/json', ...options.headers };
        if (token) headers['Authorization'] = 'Bearer ' + token;
        const res = await fetch(API.base + path, { ...options, headers });
        if (res.status === 401) { localStorage.clear(); window.location.href = '/login.html'; }
        return res.json();
    },
    get(path) { return this.request(path); },
    post(path, data) { return this.request(path, { method: 'POST', body: JSON.stringify(data) }); },
    put(path, data) { return this.request(path, { method: 'PUT', body: JSON.stringify(data) }); },
    del(path) { return this.request(path, { method: 'DELETE' }); }
};
```

- [ ] **Step 2: 创建登录页 login.html**

```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>登录 - 工时管理系统</title>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="css/style.css">
</head>
<body class="login-page">
    <div class="login-container">
        <div class="login-card">
            <h1 class="login-title">工时管理系统</h1>
            <p class="login-subtitle">研发与交付中心</p>
            <form id="loginForm">
                <div class="form-group">
                    <label class="form-label">用户名</label>
                    <input type="text" class="form-input" name="username" placeholder="请输入用户名" required>
                </div>
                <div class="form-group">
                    <label class="form-label">密码</label>
                    <input type="password" class="form-input" name="password" placeholder="请输入密码" required>
                </div>
                <div class="form-group captcha-group">
                    <label class="form-label">验证码</label>
                    <div style="display:flex;gap:8px">
                        <input type="text" class="form-input" name="captchaCode" placeholder="验证码" maxlength="4" required style="flex:1">
                        <img id="captchaImg" src="/api/auth/captcha" onclick="this.src='/api/auth/captcha?'+Date.now()" style="cursor:pointer;border-radius:6px;height:40px">
                    </div>
                </div>
                <button type="submit" class="btn btn-primary btn-login">登 录</button>
            </form>
            <p id="loginError" class="alert alert-error" style="display:none"></p>
        </div>
    </div>
    <script src="js/api.js"></script>
    <script>
        document.getElementById('loginForm').onsubmit = async (e) => {
            e.preventDefault();
            const fd = new FormData(e.target);
            const data = await API.post('/api/auth/login', Object.fromEntries(fd));
            if (data.code === 200) {
                localStorage.setItem('token', data.data.token);
                localStorage.setItem('userName', data.data.name);
                localStorage.setItem('role', data.data.role);
                window.location.href = '/';
            } else {
                document.getElementById('loginError').textContent = data.message;
                document.getElementById('loginError').style.display = 'block';
                document.getElementById('captchaImg').src = '/api/auth/captcha?' + Date.now();
            }
        };
    </script>
</body>
</html>
```

- [ ] **Step 3: 创建 style.css（引入 design.css Token）**

```css
/* style.css - 工时管理系统全局样式 */
@import url('https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600&display=swap');
@import url('../design.css');

/* 登录页 */
.login-page { background: var(--color-canvas-soft); display: flex; align-items: center; justify-content: center; min-height: 100vh; }
.login-container { width: 380px; }
.login-card { background: var(--color-canvas); border-radius: var(--rounded-lg); padding: var(--spacing-xxl); box-shadow: var(--shadow-card); border: 1px solid var(--color-hairline); }
.login-title { font-size: 22px; font-weight: 600; color: var(--color-ink); text-align: center; margin-bottom: 4px; }
.login-subtitle { font-size: 13px; color: var(--color-ink-mute); text-align: center; margin-bottom: var(--spacing-xl); }
.btn-login { width: 100%; margin-top: var(--spacing-lg); padding: 10px 16px; font-size: 15px; }

/* 布局 */
.app-layout { display: flex; min-height: 100vh; }
.sidebar { width: 220px; background: var(--color-ink); color: white; display: flex; flex-direction: column; }
.sidebar-header { padding: var(--spacing-lg); border-bottom: 1px solid rgba(255,255,255,0.1); }
.sidebar-header h2 { font-size: 16px; font-weight: 500; margin: 0; }
.sidebar-nav { flex: 1; padding: var(--spacing-sm) 0; }
.sidebar-nav a { display: flex; align-items: center; gap: var(--spacing-sm); padding: 10px var(--spacing-lg); color: rgba(255,255,255,0.7); text-decoration: none; font-size: 14px; transition: background 0.15s; }
.sidebar-nav a:hover { background: rgba(255,255,255,0.1); color: white; }
.sidebar-nav a.active { background: var(--color-primary); color: white; }
.main-content { flex: 1; background: var(--color-canvas-soft); }
.topbar { background: var(--color-canvas); border-bottom: 1px solid var(--color-hairline); padding: 0 var(--spacing-xl); height: 56px; display: flex; align-items: center; justify-content: space-between; }
.page-content { padding: var(--spacing-xl); }
```

- [ ] **Step 4: 创建首页 index.html（侧边栏布局）**

```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>工时管理系统</title>
    <link rel="stylesheet" href="css/style.css">
</head>
<body>
<div class="app-layout">
    <aside class="sidebar">
        <div class="sidebar-header"><h2>工时管理</h2></div>
        <nav class="sidebar-nav" id="sidebarNav"></nav>
    </aside>
    <main class="main-content">
        <header class="topbar">
            <span id="pageTitle">首页大屏</span>
            <div><span id="userName"></span> | <a href="#" onclick="logout()" style="color:var(--color-ink-mute);text-decoration:none">退出</a></div>
        </header>
        <div class="page-content" id="pageContent"></div>
    </main>
</div>
<script src="js/api.js"></script>
<script src="js/app.js"></script>
</body>
</html>
```

- [ ] **Step 5: 提交**

```bash
git add src/main/resources/static/
git commit -m "feat: login page and app layout"
```

### Task 4: 用户管理（后端 + 前端）

**文件：**
- Create: `src/main/java/com/timemanager/service/UserService.java`
- Create: `src/main/java/com/timemanager/controller/UserController.java`
- Create: `src/main/resources/static/js/pages/user.js`
- Modify: `src/main/resources/static/js/app.js` 添加页面路由

- [ ] **Step 1: 创建 UserService + UserController**

- [ ] **Step 2: 创建用户管理前端页面**

- [ ] **Step 3: 提交**

### Task 5: 项目管理 + 任务模块管理（后端 + 前端）

**文件：**
- Create: `src/main/java/com/timemanager/entity/Project.java`
- Create: `src/main/java/com/timemanager/entity/TaskModule.java`
- Create: `src/main/java/com/timemanager/mapper/ProjectMapper.java`
- Create: `src/main/java/com/timemanager/mapper/TaskModuleMapper.java`
- Create: `src/main/resources/mapper/ProjectMapper.xml`
- Create: `src/main/resources/mapper/TaskModuleMapper.xml`
- Create: `src/main/java/com/timemanager/service/ProjectService.java`
- Create: `src/main/java/com/timemanager/service/ModuleService.java`
- Create: `src/main/java/com/timemanager/controller/ProjectController.java`
- Create: `src/main/java/com/timemanager/controller/ModuleController.java`
- Create: `src/main/resources/static/js/pages/project.js`

### Task 6: 工时填报（后端 + 前端）

**文件：**
- Create: `src/main/java/com/timemanager/entity/WorkHour.java`
- Create: `src/main/java/com/timemanager/mapper/WorkHourMapper.java`
- Create: `src/main/resources/mapper/WorkHourMapper.xml`
- Create: `src/main/java/com/timemanager/service/WorkHourService.java`
- Create: `src/main/java/com/timemanager/controller/WorkHourController.java`
- Create: `src/main/resources/static/js/pages/workhour.js`

### Task 7: 工时审批（后端 + 前端）

**文件：**
- Create: `src/main/java/com/timemanager/entity/Approval.java`
- Create: `src/main/java/com/timemanager/mapper/ApprovalMapper.java`
- Create: `src/main/resources/mapper/ApprovalMapper.xml`
- Create: `src/main/java/com/timemanager/service/ApprovalService.java`
- Create: `src/main/java/com/timemanager/controller/ApprovalController.java`
- Create: `src/main/resources/static/js/pages/approval.js`

### Task 8: 首页大屏（后端 + 前端）

**文件：**
- Create: `src/main/java/com/timemanager/service/DashboardService.java`
- Create: `src/main/java/com/timemanager/controller/DashboardController.java`
- Create: `src/main/resources/static/js/pages/dashboard.js`
- CDN: ECharts

### Task 9: 统计报表 + 导出（后端 + 前端）

**文件：**
- Create: `src/main/java/com/timemanager/service/ReportService.java`
- Create: `src/main/java/com/timemanager/controller/ReportController.java`
- Create: `src/main/resources/static/js/pages/report.js`

### Task 10: 系统管理 + 操作日志（后端 + 前端）

**文件：**
- Create: `src/main/java/com/timemanager/controller/SystemController.java`
- Create: `src/main/java/com/timemanager/mapper/OperationLogMapper.java`
- Create: `src/main/resources/mapper/OperationLogMapper.xml`
- Create: `src/main/resources/static/js/pages/system.js`
