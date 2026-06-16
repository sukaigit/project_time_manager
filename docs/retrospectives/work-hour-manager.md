# 工时管理系统 开发回顾

## 做了什么
项目工时管理系统，面向研发与交付中心。从零开始的 0→1 新项目。

### 功能模块
- 用户管理（ADMIN 管理用户/角色/权限）
- 项目管理（PM 创建维护项目）
- 模块管理（项目下的任务模块及工时预算）
- 工时填报（普通用户按小时填报，含预算检查）
- 工时审批（PM 审批普通用户，部门经理审批 PM）
- 首页大屏（今日概况、待审批数、月度完成率、项目分布）
- 统计报表（个人/项目/部门统计 + Excel 导出）
- 系统管理（操作日志）

### 技术栈
- Spring Boot 3.2.5 + MyBatis + MySQL 8.0
- 原生 HTML/JS SPA（无前端框架）
- JWT 认证 + Spring Security 角色权限
- JaCoCo 覆盖率（95.8%）
- GitHub Actions CI

## 踩过的坑

### [问题1] 数据库凭据被子代理篡改
- **现象**：开发子代理在 application.yml 中替换了密码
- **方案**：流程文档阶段3 增加「禁止修改数据库凭据」硬约束，防止子代理自行修改连接配置

### [问题2] Jacoco 列号混淆
- **现象**：jacoco.csv 的 LINE_MISSED/COVERED 在第 8/9 列而非 6/7 列（第 6/7 列为 BRANCH 信息）
- **方案**：核对 CSV 列头后再进行聚合计算

### [问题3] login.html 未放行
- **现象**：SecurityConfig 仅 permitAll `/api/auth/**` 但未放行 `/login.html` 静态页面
- **方案**：需在 SecurityConfig 中添加 `.requestMatchers("/login.html").permitAll()`

### [问题4] spring-boot:run 输出被 pipe 缓冲
- **现象**：使用 `mvn spring-boot:run 2>&1 | tail -50` 导致 stdout 缓冲，看不到启动日志
- **方案**：直接运行 mvn spring-boot:run 不使用 pipe

## 下次改进
- 阶段1 需求分析时直接一问一答澄清需求，避免 AI 猜字段
- 阶段3 子代理 context 开头声明使用 Superpowers TDD 方法论前缀
- 数据库设计时字段级细化到每个 API 的请求/响应结构

## 耗时
- 阶段1 需求分析：约 30 分钟
- 阶段2 架构设计：约 60 分钟
- 阶段3 开发实施：约 4 小时
- 阶段4 测试保障：约 2 小时
- 阶段5 审查：约 30 分钟
- 阶段6 CI/CD：约 15 分钟
- **总计：约 8 小时 15 分钟**
