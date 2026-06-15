# 项目工时管理系统 - 架构设计

## 系统架构

```
┌──────────────────────────────────────────────────────────────┐
│                    前端（原生 HTML/JS）                        │
│  ├─ 登录页        ─ 登录/退出/修改密码/验证码                 │
│  ├─ 管理后台      ─ 用户管理/项目管理/系统管理                │
│  ├─ 工时模块      ─ 工时填报/工时审批                          │
│  └─ 数据看板      ─ 首页大屏/统计报表                          │
├──────────────────────────────────────────────────────────────┤
│                  API 层（Spring Boot REST）                    │
│  ├─ /api/auth/*       ─ 登录/退出/验证码/密码修改             │
│  ├─ /api/users/*      ─ 用户管理（CRUD + 角色 + 重置密码）     │
│  ├─ /api/projects/*   ─ 项目管理                               │
│  ├─ /api/modules/*    ─ 任务模块管理                            │
│  ├─ /api/work-hours/* ─ 工时填报（CRUD + 预算检查）             │
│  ├─ /api/approvals/*  ─ 工时审批（待批/批量/历史）              │
│  ├─ /api/dashboard/*  ─ 首页大屏（5个接口）                     │
│  ├─ /api/reports/*    ─ 统计报表（个人/项目/部门/导出）          │
│  └─ /api/logs         ─ 操作日志                                │
├──────────────────────────────────────────────────────────────┤
│                Service 层（业务逻辑）                          │
│  ├─ AuthService         ─ 认证鉴权（JWT + BCrypt）              │
│  ├─ UserService         ─ 用户管理（含firstLogin逻辑）          │
│  ├─ ProjectService      ─ 项目管理                              │
│  ├─ ModuleService       ─ 任务模块管理（含预算校验）             │
│  ├─ WorkHourService     ─ 工时填报 + 预算约束检查               │
│  ├─ ApprovalService     ─ 工时审批（二级审批流）                │
│  ├─ DashboardService    ─ 大屏数据聚合                          │
│  ├─ ReportService       ─ 统计报表生成 + Excel导出              │
│  └─ LogService          ─ 操作日志记录                          │
├──────────────────────────────────────────────────────────────┤
│                Mapper 层（MyBatis XML）                        │
│  ├─ UserMapper         → tb_user                                │
│  ├─ ProjectMapper      → tb_project                             │
│  ├─ TaskModuleMapper   → tb_task_module                         │
│  ├─ WorkHourMapper     → tb_work_hour                           │
│  ├─ ApprovalMapper     → tb_approval                            │
│  └─ OperationLogMapper → tb_operation_log                       │
├──────────────────────────────────────────────────────────────┤
│                       MySQL 数据库                             │
└──────────────────────────────────────────────────────────────┘
```

## 技术选型

| 层级 | 技术 | 版本 | 用途 |
|:-----|:-----|:-----|:-----|
| 前端 | 原生 HTML + CSS + JS | — | 页面渲染、交互 |
| CSS 框架 | Bootstrap 5 | — | UI 组件库 |
| 图表 | ECharts | — | 大屏和报表可视化 |
| 后端 | Spring Boot | 3.x | REST API |
| ORM | MyBatis | 3.x | 数据访问 |
| 认证 | JWT + Spring Security | — | 无状态鉴权 |
| 验证码 | easy-captcha | 1.6.2 | 图形验证码 |
| 数据库 | MySQL | 8.0 | 数据存储 |
| Excel | Apache POI | — | 报表导出 |
| 前端构建 | 无（原生HTML） | — | 直接放在 static/ 下 |

## 数据库设计

### 表结构

#### tb_user - 用户表
| 字段 | 类型 | 约束 | 说明 |
|:-----|:-----|:-----|:-----|
| id | BIGINT | PK, AUTO_INCREMENT | |
| username | VARCHAR(50) | NOT NULL, UNIQUE | 用户名 |
| password | VARCHAR(255) | NOT NULL | BCrypt 加密 |
| name | VARCHAR(50) | NOT NULL | 姓名 |
| email | VARCHAR(100) | DEFAULT NULL | 邮箱 |
| phone | VARCHAR(20) | DEFAULT NULL | 手机号 |
| department | VARCHAR(50) | DEFAULT '研发与交付中心' | 部门 |
| role | VARCHAR(20) | NOT NULL DEFAULT 'USER' | USER/PM/DEPT_MANAGER/ADMIN |
| status | TINYINT | NOT NULL DEFAULT 1 | 1启用 0禁用 |
| first_login | TINYINT | NOT NULL DEFAULT 1 | 1首次登录 0已修改 |
| create_time | DATETIME | DEFAULT CURRENT_TIMESTAMP | |
| update_time | DATETIME | DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP | |
| is_deleted | TINYINT | DEFAULT 0 | 逻辑删除 |

#### tb_project - 项目表
| 字段 | 类型 | 约束 | 说明 |
|:-----|:-----|:-----|:-----|
| id | BIGINT | PK, AUTO_INCREMENT | |
| name | VARCHAR(200) | NOT NULL | 项目名称 |
| description | TEXT | | 项目描述 |
| manager_id | BIGINT | NOT NULL, FK→tb_user(id) | 项目经理（仅限 PM 角色） |
| start_date | DATE | | 开始日期 |
| end_date | DATE | | 结束日期 |
| status | VARCHAR(20) | DEFAULT 'ACTIVE' | ACTIVE / FINISHED |
| create_time | DATETIME | DEFAULT CURRENT_TIMESTAMP | |
| update_time | DATETIME | DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP | |
| is_deleted | TINYINT | DEFAULT 0 | 逻辑删除 |

#### tb_task_module - 任务模块表
| 字段 | 类型 | 约束 | 说明 |
|:-----|:-----|:-----|:-----|
| id | BIGINT | PK, AUTO_INCREMENT | |
| project_id | BIGINT | NOT NULL, FK→tb_project(id) | 所属项目 |
| name | VARCHAR(200) | NOT NULL | 模块名称 |
| description | TEXT | | 模块描述 |
| estimated_hours | INT | NOT NULL DEFAULT 0 | 预估工时预算（整数） |
| create_time | DATETIME | DEFAULT CURRENT_TIMESTAMP | |
| update_time | DATETIME | DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP | |
| is_deleted | TINYINT | DEFAULT 0 | 逻辑删除 |

#### tb_work_hour - 工时记录表
| 字段 | 类型 | 约束 | 说明 |
|:-----|:-----|:-----|:-----|
| id | BIGINT | PK, AUTO_INCREMENT | |
| user_id | BIGINT | NOT NULL, FK→tb_user(id) | 填报人 |
| project_id | BIGINT | NOT NULL, FK→tb_project(id) | 所属项目 |
| module_id | BIGINT | FK→tb_task_module(id) | 任务模块（PM可为空） |
| work_date | DATE | NOT NULL | 工作日期 |
| hours | DECIMAL(4,1) | NOT NULL | 工时（任意填） |
| content | TEXT | | 工作内容 |
| status | VARCHAR(20) | NOT NULL DEFAULT 'PENDING' | PENDING/APPROVED/REJECTED |
| create_time | DATETIME | DEFAULT CURRENT_TIMESTAMP | |
| update_time | DATETIME | DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP | |
| is_deleted | TINYINT | DEFAULT 0 | 逻辑删除 |

#### tb_approval - 审批记录表
| 字段 | 类型 | 约束 | 说明 |
|:-----|:-----|:-----|:-----|
| id | BIGINT | PK, AUTO_INCREMENT | |
| work_hour_id | BIGINT | NOT NULL, FK→tb_work_hour(id) | 关联工时记录 |
| approver_id | BIGINT | NOT NULL, FK→tb_user(id) | 审批人 |
| status | VARCHAR(20) | NOT NULL | APPROVED / REJECTED |
| comment | TEXT | | 审批意见 |
| approve_time | DATETIME | | 审批时间 |
| create_time | DATETIME | DEFAULT CURRENT_TIMESTAMP | |

#### tb_operation_log - 操作日志表
| 字段 | 类型 | 约束 | 说明 |
|:-----|:-----|:-----|:-----|
| id | BIGINT | PK, AUTO_INCREMENT | |
| user_id | BIGINT | FK→tb_user(id) | 操作人（系统操作可为null）|
| action | VARCHAR(50) | NOT NULL | CREATE/UPDATE/DELETE/LOGIN/APPROVE/REJECT |
| target | VARCHAR(100) | | 操作目标描述 |
| detail | TEXT | | 操作详情（JSON） |
| create_time | DATETIME | DEFAULT CURRENT_TIMESTAMP | |

### 核心关系
```
tb_user     ──1:N──> tb_work_hour        # 用户填报工时
tb_user     ──1:N──> tb_approval         # 用户审批记录
tb_project  ──1:N──> tb_task_module      # 项目下挂任务模块
tb_project  ──N:1──> tb_user(manager)    # 项目经理
tb_work_hour─N:1──> tb_project           # 工时归属项目
tb_work_hour─N:1──> tb_task_module       # 工时归属模块（可空）
tb_work_hour─1:N──> tb_approval          # 工时审批链
```

### 索引建议
- tb_work_hour: (user_id, work_date), (project_id, status)
- tb_approval: (approver_id, status), (work_hour_id)
- tb_operation_log: (user_id, create_time)

## 页面路由

| 页面 | 路径 | 角色 |
|:-----|:-----|:-----|
| 登录页 | /login | 全部 |
| 首页大屏 | / | 全部（登录后） |
| 我的工时 | /work-hours | USER、PM |
| 工时审批 | /approval | PM、DEPT_MANAGER |
| 项目管理 | /projects | PM、ADMIN |
| 用户管理 | /users | ADMIN |
| 统计报表 | /reports | 全部 |
| 系统管理（日志） | /system/logs | ADMIN |

## 错误编码

| code | 说明 |
|:-----|:-----|
| 200 | 成功 |
| 400 | 参数错误/业务校验失败 |
| 401 | 未登录/token过期 |
| 403 | 无权限 |
| 404 | 资源不存在 |
| 500 | 服务端异常 |

统一响应结构：
```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```
