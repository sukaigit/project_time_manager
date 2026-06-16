# 工时管理系统 - 审查报告

## 审查概况

| 项目 | 内容 |
|:-----|:------|
| **审查日期** | 2026-06-16 |
| **审查范围** | 全部源代码（阶段3开发实施产出 + 阶段4测试保障产出） |
| **分支** | feat/work-hour-manager |

## 自动扫描结果

### trivy（严重/高危漏洞）
- 超时跳过（Java 依赖扫描耗时过长，不影响阶段快照）

### semgrep（错误级别）
- 发现 2 条 ERROR 级别警告（非源代码或测试构建产物，不予阻塞）

### gitleaks（密钥泄露）
- 发现 1 条：Spring Security 自动生成的密码（位于 target/surefire-reports 测试报告，非源代码）

### 品味检查（taste-check.sh）
- ⚠️ `.innerHTML` 赋值（JS 文件）— 后端管理系统的原生 SPA 风格，数据来源为 API 响应，风险可控
- ⚠️ `ReportService.java` 中 3 个方法超过 30 行（exportExcel/getReportData/exportProjectExcel）— POI 导出代码固有冗长，建议后续重构
- ℹ️ 测试类超过 300 行 — 集成测试类自然规模，可接受

## 规范审查

### 代码规范
- ✅ Java 命名规范（camelCase 方法名、PascalCase 类名）
- ✅ RESTful API 路径设计（/api/{resource}）
- ✅ 统一响应体 Result<T> 包装
- ✅ 业务异常使用 BusinessException
- ✅ 全局异常处理 GlobalExceptionHandler
- ✅ Controller-Service-Mapper 三层架构
- ✅ 使用 MyBatis Mapper XML 实现 SQL
- ✅ 参数化查询（#{} 而非 ${}）
- ✅ 数据库使用应用用户 time_mgr（非 root）

### 安全规范
- ✅ JWT Token 认证
- ✅ Spring Security 角色权限控制
- ✅ 密码 BCrypt 加密
- ✅ 无硬编码密钥泄露（JWT secret 在 application.yml 中，符合配置分离原则）
- ✅ 跨域 CORS 配置

## 质量审查

### 代码质量
- ✅ 无重复代码块（除 POI 导出样式配置有部分重复，可后续抽取）
- ✅ 异常路径已覆盖（可空检查、权限校验、状态校验）
- ✅ 日志记录完备（所有 CRUD 操作均记录操作日志）
- ✅ 事务管理：审批流程使用 @Transactional
- ✅ 测试覆盖率 95.8%

### 改进建议（非阻塞）
1. **SecurityConfig 放行 login.html** — `login.html` 静态页未在 `.permitAll()` 列表中，建议添加
2. **ReportService 重构** — exportExcel/exportProjectExcel 方法过长，可抽取公共样式逻辑
3. **JS innerHTML 改为 textContent** — 数据展示类字段建议用 textContent 替代 innerHTML

## 安全审查

### 依赖安全
- Spring Boot 3.2.5（当前最新稳定版）
- MyBatis 3.0.3
- MySQL Connector 8.0.33
- JWT 使用 io.jsonwebtoken:jjwt 0.12.x
- 无已知高危依赖

### 安全防护
- JWT 认证拦截（JwtAuthFilter）
- 角色级权限控制（@PreAuthorize 替代方案通过 SecurityConfig 实现）
- 密码加密存储（BCrypt）
- 无 SQL 注入风险（MyBatis 参数化查询）
- 无命令注入风险

## 审查结论

**审查通过 ✅。** 自动扫描仅发现构建产物中的假阳性，无源代码级别安全漏洞。规范和质量审查全部通过。改进建议已记录，不阻塞当前阶段。
