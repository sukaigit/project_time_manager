# 工时管理系统 - 接口设计

## 认证

### POST /api/auth/login
```
Request:
{
  "username": "string",
  "password": "string",
  "captchaId": "string",
  "captchaCode": "string"
}
Response:
{
  "code": 200,
  "message": "success",
  "data": {
    "token": "string",
    "userId": 1,
    "username": "admin",
    "name": "管理员",
    "role": "ADMIN",
    "firstLogin": true
  }
}
```

### GET /api/auth/captcha?t={timestamp}
```
Response: image/png stream
Header: X-Captcha-Id: {captchaId}
```

### PUT /api/auth/password
```
Request:
{
  "oldPassword": "string",
  "newPassword": "string"
}
Response: { "code": 200, "message": "修改成功" }
```

## 用户管理

### GET /api/users?page=1&size=10&keyword=
```
Response:
{
  "code": 200,
  "data": {
    "total": 100,
    "list": [
      {
        "id": 1,
        "username": "admin",
        "name": "管理员",
        "email": "admin@test.com",
        "phone": "13800138000",
        "department": "研发与交付中心",
        "role": "ADMIN",
        "status": 1,
        "firstLogin": false,
        "createTime": "2026-06-14T10:00:00"
      }
    ]
  }
}
```

### POST /api/users
```
Request:
{
  "username": "string",
  "name": "string",
  "email": "string?",
  "phone": "string?",
  "department": "string?",
  "role": "USER",
  "status": 1
}
// password 自动设为 123456, firstLogin=1
Response: { "code": 200, "data": { "id": 2 } }
```

### PUT /api/users/{id}
```
Request: { "name", "email", "phone", "department", "role", "status" }
Response: { "code": 200 }
```

### DELETE /api/users/{id}
```
Response: { "code": 200 }
```

### PUT /api/users/{id}/role
```
Request: { "role": "PM" }
Response: { "code": 200 }
```

### PUT /api/users/{id}/reset-password
```
// 重置为 123456，firstLogin=1
Response: { "code": 200 }
```

## 项目管理

### GET /api/projects?page=1&size=10&keyword=
```
Response:
{
  "code": 200,
  "data": {
    "total": 10,
    "list": [
      {
        "id": 1,
        "name": "项目A",
        "description": "描述",
        "managerId": 1,
        "managerName": "张三",
        "startDate": "2026-01-01",
        "endDate": "2026-12-31",
        "status": "ACTIVE"
      }
    ]
  }
}
```

### POST /api/projects
```
Request:
{
  "name": "string",
  "description": "string?",
  "managerId": 1,
  "startDate": "2026-01-01?",
  "endDate": "2026-12-31?",
  "status": "ACTIVE"
}
Response: { "code": 200, "data": { "id": 1 } }
```

### PUT /api/projects/{id}
```
Request: { "name", "description", "managerId", "startDate", "endDate", "status" }
Response: { "code": 200 }
```

### DELETE /api/projects/{id}
```
Response: { "code": 200 }
```

## 任务模块管理

### GET /api/projects/{projectId}/modules
```
Response:
{
  "code": 200,
  "data": [
    {
      "id": 1,
      "projectId": 1,
      "name": "需求分析",
      "description": "需求调研与文档",
      "estimatedHours": 40,
      "usedHours": 35.5  // 可选：当前已报工时
    }
  ]
}
```

### POST /api/projects/{projectId}/modules
```
Request: { "name": "string", "description": "string?", "estimatedHours": 40 }
Response: { "code": 200, "data": { "id": 1 } }
```

### PUT /api/modules/{id}
```
Request: { "name", "description", "estimatedHours" }
Response: { "code": 200 }
```

### DELETE /api/modules/{id}
```
Response: { "code": 200 }
```

## 工时填报

### GET /api/work-hours?page=1&size=10&projectId=&startDate=&endDate=&status=
```
Response:
{
  "code": 200,
  "data": {
    "total": 50,
    "list": [
      {
        "id": 1,
        "userId": 1,
        "userName": "张三",
        "projectId": 1,
        "projectName": "项目A",
        "moduleId": 1,
        "moduleName": "需求分析",
        "workDate": "2026-06-15",
        "hours": 4.0,
        "content": "编写需求文档",
        "status": "PENDING",
        "createTime": "2026-06-15T09:00:00"
      }
    ]
  }
}
```

### POST /api/work-hours
```
Request:
{
  "projectId": 1,
  "moduleId": 1,       // PM可不传
  "workDate": "2026-06-15",
  "hours": 4.0,
  "content": "编写需求文档"
}
// 校验：普通用户→moduleId必填+预算检查
Response: { "code": 200, "data": { "id": 1 } }
```

### PUT /api/work-hours/{id}
```
Request: { "projectId", "moduleId", "workDate", "hours", "content" }
// 状态恢复为 PENDING
Response: { "code": 200 }
```

### DELETE /api/work-hours/{id}
```
Response: { "code": 200 }
```

### GET /api/work-hours/budget-check?projectId=&moduleId=
// 查询当前模块预算使用情况
```
Response:
{
  "code": 200,
  "data": {
    "estimatedHours": 40,
    "usedHours": 35.5,
    "remainingHours": 4.5,
    "locked": false  // true=预算已满不可再报
  }
}
```

## 工时审批

### GET /api/approvals/pending?page=1&size=10
```
Response:
{
  "code": 200,
  "data": {
    "total": 5,
    "list": [
      {
        "id": 1,
        "workHourId": 1,
        "userId": 2,
        "userName": "李四",
        "projectName": "项目A",
        "moduleName": "需求分析",
        "workDate": "2026-06-15",
        "hours": 4.0,
        "content": "编写需求文档",
        "createTime": "2026-06-15T09:00:00"
      }
    ]
  }
}
```

### PUT /api/approvals/batch
```
Request:
{
  "items": [
    { "id": 1, "status": "APPROVED", "comment": "同意" },
    { "id": 2, "status": "REJECTED", "comment": "重新填写" }
  ]
}
Response: { "code": 200 }
```

### GET /api/approvals/history?page=1&size=10
```
Response:
{
  "code": 200,
  "data": {
    "total": 20,
    "list": [
      {
        "id": 1,
        "workHourId": 1,
        "approverName": "管理员",
        "status": "APPROVED",
        "comment": "同意",
        "approveTime": "2026-06-15T10:00:00"
      }
    ]
  }
}
```

## 大屏

### GET /api/dashboard/today
```
Response:
{
  "code": 200,
  "data": {
    "submitCount": 15,    // 今日提交人数
    "totalHours": 68.5    // 今日总工时
  }
}
```

### GET /api/dashboard/pending
```
{
  "code": 200,
  "data": { "count": 5 }  // 我的待审批条数
}
```

### GET /api/dashboard/monthly-rate
```
{
  "code": 200,
  "data": {
    "totalHours": 1200,
    "rate": 0.75           // 完成率 75%
  }
}
```

### GET /api/dashboard/project-distribution
```
{
  "code": 200,
  "data": [
    { "projectName": "项目A", "hours": 500 },
    { "projectName": "项目B", "hours": 300 }
  ]
}
```

### GET /api/dashboard/overview
```
{
  "code": 200,
  "data": {
    "totalUsers": 50,
    "totalHours": 1200,
    "avgHours": 24.0
  }
}
```

## 报表

### GET /api/reports/personal?year=2026&month=6
```
{
  "code": 200,
  "data": {
    "totalHours": 120.0,
    "details": [
      { "projectName": "项目A", "hours": 80.0, "ratio": 0.67 },
      { "projectName": "项目B", "hours": 40.0, "ratio": 0.33 }
    ]
  }
}
```

### GET /api/reports/project?year=2026&month=6&projectId=1
```
{
  "code": 200,
  "data": {
    "totalHours": 200.0,
    "details": [
      {
        "moduleName": "需求分析",
        "estimatedHours": 40,
        "actualHours": 38.5,
        "ratio": 0.96
      }
    ]
  }
}
```

### GET /api/reports/department?year=2026&month=6
```
{
  "code": 200,
  "data": {
    "totalHours": 1200,
    "avgHours": 24.0,
    "userCount": 50,
    "details": [
      { "projectName": "项目A", "hours": 500, "ratio": 0.42 }
    ]
  }
}
```

### GET /api/reports/export?type=personal&year=2026&month=6
```
Response: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet (Excel)
```

## 操作日志

### GET /api/logs?page=1&size=10&userId=&action=&startDate=&endDate=
```
{
  "code": 200,
  "data": {
    "total": 100,
    "list": [
      {
        "id": 1,
        "userId": 1,
        "userName": "管理员",
        "action": "LOGIN",
        "target": "用户登录",
        "detail": "{}",
        "createTime": "2026-06-15T09:00:00"
      }
    ]
  }
}
```

## 权限校验规则

| API | ADMIN | DEPT_MANAGER | PM | USER |
|:----|:-----:|:------------:|:--:|:----:|
| GET /api/users/* | ✅ | — | — | — |
| POST/PUT/DELETE /api/users/* | ✅ | — | — | — |
| GET /api/projects | ✅ | ✅ | ✅(所属+自己) | ✅(参与) |
| POST /api/projects | ✅ | — | ✅ | — |
| PUT/DELETE /api/projects/* | ✅ | — | ✅(所属) | — |
| GET /api/projects/*/modules | ✅ | ✅ | ✅(所属) | ✅(参与) |
| POST /api/projects/*/modules | ✅ | — | ✅(所属) | — |
| PUT/DELETE /api/modules/* | ✅ | — | ✅(所属) | — |
| GET /api/work-hours | ✅(全部) | ✅(全部) | ✅(项目下) | ✅(本人) |
| POST/PUT/DELETE /api/work-hours/* | ✅ | — | ✅(本人) | ✅(本人) |
| GET /api/approvals/pending | — | ✅(PM报的) | ✅(USER报的) | — |
| PUT /api/approvals/batch | — | ✅ | ✅ | — |
| GET /api/dashboard/* | ✅(全) | ✅(部门) | ✅(项目) | ✅(个人) |
| GET /api/reports/* | ✅(全) | ✅(部门) | ✅(项目) | ✅(个人) |
| GET /api/logs | ✅ | — | — | — |
