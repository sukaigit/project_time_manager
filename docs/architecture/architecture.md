# 项目工时管理系统 - 架构设计

## 系统架构

```
┌──────────────────────────────────────────────────────────────┐
│                    前端（原生 HTML/JS）                        │
│  ├─ 登录页        ─ 登录/退出                                 │
│  ├─ 管理后台      ─ 用户管理/项目管理/系统管理                  │
│  ├─ 工时模块      ─ 工时填报/工时审批                          │
│  └─ 数据看板      ─ 首页大屏/统计报表                          │
├──────────────────────────────────────────────────────────────┤
│                  API 层（Spring Boot REST）                    │
│  ├─ /api/auth/*      ─ 登录/退出/密码修改                     │
│  ├─ /api/users/*     ─ 用户管理                               │
│  ├─ /api/projects/*  ─ 项目管理                               │
│  ├─ /api/modules/*   ─ 任务模块管理                           │
│  ├─ /api/work-hours/* ─ 工时填报/审批                          │
│  ├─ /api/dashboard/* ─ 首页大屏                               │
│  └─ /api/reports/*   ─ 统计报表                               │
├──────────────────────────────────────────────────────────────┤
│                Service 层（业务逻辑）                          │
│  ├─ AuthService      ─ 认证鉴权（JWT + Spring Security）       │
│  ├─ UserService      ─ 用户管理                               │
│  ├─ ProjectService   ─ 项目管理                               │
│  ├─ ModuleService    ─ 任务模块管理（含预算校验）               │
│  ├─ WorkHourService  ─ 工时填报/审批逻辑                      │
│  ├─ DashboardService ─ 大屏数据聚合                            │
│  └─ ReportService    ─ 统计报表生成/导出                       │
├──────────────────────────────────────────────────────────────┤
│                Mapper 层（MyBatis 数据访问）                    │
│  ├─ UserMapper       → tb_user 表                               │
│  ├─ ProjectMapper    → tb_project 表                            │
│  ├─ TaskModuleMapper → tb_task_module 表                        │
│  ├─ WorkHourMapper   → tb_work_hour 表                          │
│  ├─ ApprovalMapper   → tb_approval 表                           │
│  └─ OperationLogMapper → tb_operation_log 表                    │
├──────────────────────────────────────────────────────────────┤
│                       MySQL 数据库                             │
└──────────────────────────────────────────────────────────────┘
```

## 技术选型

| 层级 | 技术 | 版本 | 用途 |
|:-----|:-----|:-----|:-----|
| 前端 | 原生 HTML + CSS + JS | — | 页面渲染、交互 |
| 后端 | Spring Boot | 3.x | REST API |
| ORM | MyBatis | — | 数据访问 |
| 认证 | JWT + Spring Security | — | 登录鉴权 |
| 数据库 | MySQL | 8.0 | 数据存储 |
| 图表 | ECharts | — | 大屏和报表可视化 |
| 模板 | Bootstrap 5 | — | UI 组件库 |

## 数据库设计

### 核心表

| 表名 | 说明 | 命名规范 |
|:-----|:-----|:---------|
| tb_user | 用户（含角色字段） | tb_前缀 + lower_snake_case |
| tb_project | 项目 | |
| tb_task_module | 任务模块（含预估工时预算） | |
| tb_work_hour | 工时记录 | |
| tb_approval | 审批记录 | |
| tb_operation_log | 操作日志 | |

### 关键字段命名

所有表统一：
- 主键：`id`（BIGINT，自增）
- 创建时间：`create_time`
- 更新时间：`update_time`
- 逻辑删除：`is_deleted`
- 外键：`{表名}_id`（如 `project_id`、`user_id`）

### 关键关系

```
tb_user ──1:N──> tb_work_hour          # 用户填写工时
tb_user ──1:1──> role (枚举字段)       # 角色
tb_project ──1:N──> tb_task_module     # 项目下挂任务模块
tb_project ──N:1──> tb_user (项目经理)  # 项目关联项目经理
tb_work_hour ──N:1──> tb_project       # 工时归属项目
tb_work_hour ──N:1──> tb_task_module   # 工时归属任务模块
tb_work_hour ──1:N──> tb_approval      # 工时审批链
```

## 页面路由

| 页面 | 路径 | 角色 |
|:-----|:-----|:-----|
| 登录页 | /login | 全部 |
| 首页大屏 | / | 全部（登录后） |
| 我的工时 | /work-hours | 普通用户、项目经理 |
| 工时审批 | /approval | 项目经理、部门经理 |
| 项目管理 | /projects | 项目经理 |
| 用户管理 | /users | 系统管理员 |
| 统计报表 | /reports | 部门经理 |
| 系统管理 | /system | 系统管理员 |
