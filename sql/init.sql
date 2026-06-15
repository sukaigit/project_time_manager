-- 项目工时管理系统 - 数据库初始化脚本
-- 数据库：MySQL 8.0
-- 字符集：utf8mb4
-- 对应 application.yml: jdbc:mysql://localhost:3306/project_time_manager

CREATE DATABASE IF NOT EXISTS project_time_manager DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE project_time_manager;

-- 用户表
CREATE TABLE IF NOT EXISTS tb_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码（BCrypt加密）',
    name VARCHAR(50) NOT NULL COMMENT '姓名',
    email VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    phone VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    department VARCHAR(50) DEFAULT '研发与交付中心' COMMENT '部门',
    role VARCHAR(20) NOT NULL DEFAULT 'USER' COMMENT '角色：USER/PM/DEPT_MANAGER/ADMIN',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1正常 0禁用',
    first_login TINYINT NOT NULL DEFAULT 1 COMMENT '首次登录标记：1是 0否',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删 1已删'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 项目表
CREATE TABLE IF NOT EXISTS tb_project (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(200) NOT NULL COMMENT '项目名称',
    description TEXT COMMENT '项目描述',
    manager_id BIGINT NOT NULL COMMENT '项目经理ID',
    start_date DATE DEFAULT NULL COMMENT '开始日期',
    end_date DATE DEFAULT NULL COMMENT '结束日期',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE进行中 FINISHED已结束',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    FOREIGN KEY (manager_id) REFERENCES tb_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目表';

-- 任务模块表
CREATE TABLE IF NOT EXISTS tb_task_module (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL COMMENT '所属项目ID',
    name VARCHAR(200) NOT NULL COMMENT '模块名称',
    description TEXT COMMENT '模块描述',
    estimated_hours INT NOT NULL DEFAULT 0 COMMENT '预估工时预算（整数）',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    FOREIGN KEY (project_id) REFERENCES tb_project(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务模块表';

-- 工时记录表
CREATE TABLE IF NOT EXISTS tb_work_hour (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '填报人ID',
    project_id BIGINT NOT NULL COMMENT '所属项目ID',
    module_id BIGINT DEFAULT NULL COMMENT '任务模块ID（普通用户必选，项目经理可为空）',
    work_date DATE NOT NULL COMMENT '工作日期',
    hours DECIMAL(4,1) NOT NULL COMMENT '工时（小时）',
    content TEXT COMMENT '工作内容描述',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING待审批 APPROVED已通过 REJECTED已驳回',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    FOREIGN KEY (user_id) REFERENCES tb_user(id),
    FOREIGN KEY (project_id) REFERENCES tb_project(id),
    FOREIGN KEY (module_id) REFERENCES tb_task_module(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工时记录表';

-- 审批记录表
CREATE TABLE IF NOT EXISTS tb_approval (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    work_hour_id BIGINT NOT NULL COMMENT '工时记录ID',
    approver_id BIGINT NOT NULL COMMENT '审批人ID',
    status VARCHAR(20) NOT NULL COMMENT 'APPROVED/REJECTED',
    comment TEXT COMMENT '审批意见',
    approve_time DATETIME DEFAULT NULL COMMENT '审批时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (work_hour_id) REFERENCES tb_work_hour(id),
    FOREIGN KEY (approver_id) REFERENCES tb_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批记录表';

-- 操作日志表
CREATE TABLE IF NOT EXISTS tb_operation_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT DEFAULT NULL COMMENT '操作人ID（系统操作可为空）',
    action VARCHAR(50) NOT NULL COMMENT '操作类型：CREATE/UPDATE/DELETE/LOGIN/APPROVE/REJECT',
    target VARCHAR(100) DEFAULT NULL COMMENT '操作目标描述',
    detail TEXT COMMENT '操作详情（JSON格式）',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES tb_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

-- 索引
CREATE INDEX idx_work_hour_user_date ON tb_work_hour(user_id, work_date);
CREATE INDEX idx_work_hour_project_status ON tb_work_hour(project_id, status);
CREATE INDEX idx_approval_approver_status ON tb_approval(approver_id, status);
CREATE INDEX idx_approval_work_hour ON tb_approval(work_hour_id);
CREATE INDEX idx_log_user_time ON tb_operation_log(user_id, create_time);

-- 预置数据：默认系统管理员
-- 密码：admin123（BCrypt加密）
INSERT INTO tb_user (username, password, name, role, status, first_login) VALUES
('admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '系统管理员', 'ADMIN', 1, 1);
