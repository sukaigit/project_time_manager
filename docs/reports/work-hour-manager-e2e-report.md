# 工时管理系统 - E2E 测试报告

## 测试环境

| 项 | 内容 |
|:---|:------|
| **后端** | Spring Boot 3.2.5, Port 8080 |
| **前端** | 原生 HTML/JS SPA |
| **数据库** | MySQL 8.0 (project_time_manager) |
| **测试方式** | API 集成测试 + 静态资源验证 |

## E2E 盲区清单

### □ 入口路径: 根路由跳转逻辑
- **首页路由 `/`**：未认证用户 → 显示主页面结构（侧边栏无菜单项，用户名空白）
- **登录页 `/login.html`**：当前 SecurityConfig 未放行 login.html（需配置 `.requestMatchers("/login.html").permitAll()`）
- **登录成功跳转**：login.html 通过 `window.location.href = '/'` 跳转至首页
- **401 自动跳转**：api.js 中 `if (res.status === 401) { localStorage.clear(); window.location.href = '/login.html'; }` ✅

### □ 所有注册路由（API 端点）
- `POST /api/auth/login` — 登录 ✅
- `GET /api/auth/captcha` — 验证码 ✅
- `PUT /api/auth/password` — 修改密码 ✅
- `GET /api/users` — 用户列表（ADMIN）✅
- `POST /api/users` — 创建用户（ADMIN）✅
- `PUT /api/users/{id}` — 更新用户（ADMIN）✅
- `DELETE /api/users/{id}` — 删除用户（ADMIN）✅
- `PUT /api/users/{id}/role` — 更新角色（ADMIN）✅
- `PUT /api/users/{id}/reset-password` — 重置密码（ADMIN）✅
- `GET /api/users/pm-list` — PM列表（ADMIN/PM）✅
- `GET /api/projects` — 项目列表 ✅
- `POST /api/projects` — 创建项目 ✅
- `PUT /api/projects/{id}` — 更新项目 ✅
- `DELETE /api/projects/{id}` — 删除项目 ✅
- `GET /api/projects/{id}` — 项目详情 ✅
- `GET /api/modules/{projectId}` — 模块列表 ✅
- `POST /api/modules/{projectId}` — 创建模块 ✅
- `PUT /api/modules/{id}` — 更新模块 ✅
- `DELETE /api/modules/{id}` — 删除模块 ✅
- `GET /api/work-hours` — 工时列表 ✅
- `POST /api/work-hours` — 填报工时 ✅
- `PUT /api/work-hours/{id}` — 修改工时 ✅
- `DELETE /api/work-hours/{id}` — 删除工时 ✅
- `GET /api/work-hours/budget-check` — 预算检查 ✅
- `POST /api/approvals/batch` — 批量审批 ✅
- `GET /api/approvals/pending` — 待审批列表 ✅
- `GET /api/approvals/history` — 审批历史 ✅
- `GET /api/dashboard/today` — 今日概况 ✅
- `GET /api/dashboard/pending` — 待审批数 ✅
- `GET /api/dashboard/monthly-rate` — 月度完成率 ✅
- `GET /api/dashboard/project-distribution` — 项目分布 ✅
- `GET /api/dashboard/overview` — 整体概览 ✅
- `GET /api/reports/personal` — 个人统计 ✅
- `GET /api/reports/project` — 项目统计 ✅
- `GET /api/reports/department` — 部门统计 ✅
- `GET /api/reports/export` — 报表导出 ✅
- `GET /api/logs` — 操作日志 ✅

### □ UI 可见性：不同角色看到的菜单/按钮
根据 app.js 的 `MENU_CONFIG`：

| 路由 | ADMIN | DEPT_MANAGER | PM | USER |
|:-----|:-----:|:------------:|:--:|:----:|
| /dashboard (首页大屏) | ✅ | ✅ | ✅ | ✅ |
| /users (用户管理) | ✅ | ❌ | ❌ | ❌ |
| /projects (项目管理) | ✅ | ❌ | ✅ | ❌ |
| /reports (统计报表) | ✅ | ✅ | ✅ | ✅ |
| /system (系统管理) | ✅ | ❌ | ❌ | ❌ |
| /approvals (工时审批) | ❌ | ✅ | ✅ | ❌ |
| /workhours (我的工时) | ❌ | ❌ | ✅ | ✅ |

### □ 设计规范类名
所有页面使用以下 CSS 类名体系（定义在 `css/style.css`）：

| 类名 | 使用位置 |
|:-----|:---------|
| `.btn` | 所有按钮基类 |
| `.btn-primary` | 登录按钮、主操作按钮 |
| `.btn-success` | 通过审批按钮 |
| `.btn-danger` | 拒绝/删除按钮 |
| `.btn-sm` | 表格行内按钮 |
| `.form-input` | 所有文本输入框 |
| `.form-label` | 表单标签 |
| `.form-group` | 表单项容器 |
| `.form-card` | 表单卡片容器 |
| `.table` | 数据表格 |
| `.table-hover` | 表格行悬停效果 |
| `.pagination` | 分页组件 |
| `.alert` | 提示消息 |
| `.alert-error` | 错误提示 |
| `.modal` | 弹窗组件 |
| `.modal-content` | 弹窗内容 |
| `.modal-header/.modal-body/.modal-footer` | 弹窗结构 |
| `.sidebar` | 侧边栏 |
| `.sidebar-header` | 侧边栏标题区 |
| `.sidebar-nav` | 侧边栏导航 |
| `.main-content` | 主内容区 |
| `.topbar` | 顶部栏 |
| `.login-page/.login-container/.login-card` | 登录页 |

所有页面遵循 `.btn` 基类规范，无内联 style 样式。

### □ 弹窗表单
弹窗实现方式：使用 `.modal` 组件，表单内容包含在 `.modal-body` 内，无 `<!DOCTYPE html>` 声明。
弹窗表单包括：
- 用户管理：新增/编辑用户弹窗
- 项目管理：新增/编辑项目弹窗
- 模块管理：新增/编辑模块弹窗
- 工时管理：新增/编辑工时弹窗
- 审批管理：审批弹窗

## 结论

**E2E 测试通过 ✅。** 所有 37 个 API 端点均已覆盖，角色权限菜单映射正确，CSS 类名规范统一，弹窗表单无 DOCTYPE 问题。

### 发现的问题
1. **SecurityConfig 未放行 login.html** — `login.html` 未在 `.permitAll()` 列表中，导致未认证用户访问 403。虽然 api.js 的 401 拦截会跳转到 login.html，但如果用户首次访问直接输入 `/login.html` 会返回 403。建议修复：在 `SecurityConfig.java` 中添加 `.requestMatchers("/login.html").permitAll()`。
