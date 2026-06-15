# 工时管理系统 - 功能规格（字段级）

## 1. 系统基础
- 登录：用户名 + 密码 + 图形验证码 → JWT token
- 退出：前端清除 token，跳转登录页
- 密码修改：需旧密码验证，新密码 ≥ 6 位，BCrypt 加密
- 首次登录：默认密码 123456，first_login=1 标记，需强制修改
- 认证方式：JWT（Spring Security），前端 Authorization header

## 2. 用户管理（系统管理员）

### 字段
| 字段 | 类型 | 约束 |
|:-----|:-----|:-----|
| id | BIGINT | PK 自增 |
| username | VARCHAR(50) | 唯一 |
| password | VARCHAR(255) | BCrypt |
| name | VARCHAR(50) | NOT NULL |
| email | VARCHAR(100) | 可空 |
| phone | VARCHAR(20) | 可空 |
| department | VARCHAR(50) | 默认'研发与交付中心' |
| role | VARCHAR(20) | USER/PM/DEPT_MANAGER/ADMIN |
| status | TINYINT | 1启用 0禁用 |
| first_login | TINYINT | 1是 0否 |
| is_deleted | TINYINT | 逻辑删除 |

### API
- GET /api/users — 分页列表，支持搜索
- POST /api/users — 新增（密码默认 123456）
- PUT /api/users/{id} — 编辑
- DELETE /api/users/{id} — 逻辑删除
- PUT /api/users/{id}/role — 分配角色
- PUT /api/users/{id}/reset-password — 重置密码为 123456

## 3. 项目管理（项目经理/管理员）

### 字段
| 字段 | 类型 | 约束 |
|:-----|:-----|:-----|
| id | BIGINT | PK 自增 |
| name | VARCHAR(200) | NOT NULL |
| description | TEXT | 可空 |
| manager_id | BIGINT | FK→tb_user，仅限 PM 角色 |
| start_date | DATE | 可空 |
| end_date | DATE | 可空 |
| status | VARCHAR(20) | ACTIVE/FINISHED |
| is_deleted | TINYINT | 逻辑删除 |

### API
- GET /api/projects — 分页列表
- POST /api/projects — 新增（manager_id 限 PM 角色）
- PUT /api/projects/{id} — 编辑
- DELETE /api/projects/{id} — 逻辑删除

## 4. 任务模块管理（项目经理/管理员）

### 字段
| 字段 | 类型 | 约束 |
|:-----|:-----|:-----|
| id | BIGINT | PK 自增 |
| project_id | BIGINT | FK→tb_project |
| name | VARCHAR(200) | NOT NULL |
| description | TEXT | 可空 |
| estimated_hours | INT | NOT NULL，默认0（整数） |
| is_deleted | TINYINT | 逻辑删除 |

### API
- GET /api/projects/{projectId}/modules — 按项目查询
- POST /api/projects/{projectId}/modules — 新增
- PUT /api/modules/{id} — 编辑
- DELETE /api/modules/{id} — 逻辑删除

## 5. 工时填报

### 字段
| 字段 | 类型 | 约束 |
|:-----|:-----|:-----|
| id | BIGINT | PK 自增 |
| user_id | BIGINT | FK→tb_user |
| project_id | BIGINT | FK→tb_project |
| module_id | BIGINT | FK→tb_task_module，PM可空 |
| work_date | DATE | NOT NULL |
| hours | DECIMAL(4,1) | NOT NULL |
| content | TEXT | 可空 |
| status | VARCHAR(20) | PENDING/APPROVED/REJECTED |
| is_deleted | TINYINT | 逻辑删除 |

### 规则
- 工时任意填，每日不限制
- 普通用户累计工时 ≥ estimated_hours 时不可再报该模块
- 已审批工时不可修改/删除
- 未审批工时可修改/删除

### API
- GET /api/work-hours — 我的工时（分页+筛选）
- POST /api/work-hours — 填报
- PUT /api/work-hours/{id} — 修改
- DELETE /api/work-hours/{id} — 删除

## 6. 工时审批

### 字段
| 字段 | 类型 | 约束 |
|:-----|:-----|:-----|
| id | BIGINT | PK 自增 |
| work_hour_id | BIGINT | FK→tb_work_hour |
| approver_id | BIGINT | FK→tb_user |
| status | VARCHAR(20) | APPROVED/REJECTED |
| comment | TEXT | 可空 |
| approve_time | DATETIME | 可空 |

### 流程
- USER → PM 审批，PM → DEPT_MANAGER 审批
- 部门经理不填工时
- 支持批量审批
- 驳回重提生成新审批记录

### API
- GET /api/approvals/pending — 我的待审批列表
- PUT /api/approvals/batch — 批量审批（[{id, status, comment}]）
- GET /api/approvals/history — 审批历史

## 7. 首页大屏

### API
- GET /api/dashboard/today — 今日概况（提交人数、总工时）
- GET /api/dashboard/pending — 我的待办（待审批数）
- GET /api/dashboard/monthly-rate — 本月完成率
- GET /api/dashboard/project-distribution — 项目工时分布
- GET /api/dashboard/overview — 部门整体概览

## 8. 统计报表

### API
- GET /api/reports/personal?year=&month= — 个人统计
- GET /api/reports/project?year=&month=&projectId= — 项目统计
- GET /api/reports/department?year=&month= — 部门统计
- GET /api/reports/export?type=&year=&month= — Excel 导出

## 9. 系统管理

### API
- GET /api/logs — 操作日志列表（分页+筛选）
- PUT /api/auth/password — 修改密码（需旧密码验证）

## 10. 权限矩阵

| 功能 | ADMIN | DEPT_MANAGER | PM | USER |
|:-----|:-----:|:------------:|:--:|:----:|
| 用户管理 | ✅ | — | — | — |
| 项目管理 | ✅ | — | ✅ | — |
| 模块管理 | ✅ | — | ✅(所属项目) | — |
| 工时填报 | ✅ | — | ✅ | ✅ |
| 工时审批 | — | ✅(PM的) | ✅(USER的) | — |
| 首页大屏 | ✅(全) | ✅(部门) | ✅(项目) | ✅(个人) |
| 统计报表 | ✅(全) | ✅(部门) | ✅(项目) | ✅(个人) |
| 操作日志 | ✅ | — | — | — |
