# 工时管理系统 - 设计文档

## 架构
Spring Boot 3.x + MyBatis + MySQL 8.0 + 原生HTML/JS + Bootstrap 5 + ECharts

## 分层结构
- Controller → Service → Mapper → MySQL
- JWT 认证（Spring Security）
- 前端 AJAX 调用 API

## API 设计

### 认证
- POST /api/auth/login — 登录
- GET /api/auth/captcha — 验证码
- PUT /api/auth/password — 修改密码

### 用户管理
- GET /api/users — 用户列表（分页）
- POST /api/users — 新增用户
- PUT /api/users/{id} — 编辑用户
- DELETE /api/users/{id} — 删除用户
- PUT /api/users/{id}/role — 分配角色

### 项目管理
- GET /api/projects — 项目列表
- POST /api/projects — 新增项目
- PUT /api/projects/{id} — 编辑项目
- GET /api/projects/{id}/modules — 项目任务模块列表
- POST /api/projects/{id}/modules — 新增任务模块
- PUT /api/modules/{id} — 编辑任务模块
- DELETE /api/modules/{id} — 删除任务模块

### 工时
- GET /api/work-hours — 我的工时
- POST /api/work-hours — 填报工时
- PUT /api/work-hours/{id} — 修改工时
- DELETE /api/work-hours/{id} — 删除工时

### 审批
- GET /api/approvals/pending — 待审批列表
- PUT /api/approvals/{id}/approve — 通过
- PUT /api/approvals/{id}/reject — 驳回

### 大屏
- GET /api/dashboard/today — 今日概况
- GET /api/dashboard/month — 月度完成率
- GET /api/dashboard/projects — 项目分布
- GET /api/dashboard/overview — 部门概览

### 报表
- GET /api/reports/user — 个人统计
- GET /api/reports/project — 项目统计
- GET /api/reports/department — 部门统计
- GET /api/reports/export — 导出 Excel
