CREATE DATABASE IF NOT EXISTS shortlink_db
DEFAULT CHARACTER SET utf8mb4
DEFAULT COLLATE utf8mb4_unicode_ci;

USE shortlink_db;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS tb_user;
CREATE TABLE tb_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    username VARCHAR(64) NOT NULL COMMENT '用户名',
    phone VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    password VARCHAR(100) NOT NULL COMMENT '密码',
    nickname VARCHAR(64) DEFAULT NULL COMMENT '昵称',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1正常 0禁用',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_username (username),
    UNIQUE KEY uk_phone (phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

DROP TABLE IF EXISTS tb_short_link;
CREATE TABLE tb_short_link (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '创建用户ID',
    short_code VARCHAR(32) NOT NULL COMMENT '短码',
    short_url VARCHAR(255) NOT NULL COMMENT '完整短链',
    origin_url VARCHAR(2048) NOT NULL COMMENT '原始长链接',
    title VARCHAR(255) DEFAULT NULL COMMENT '短链标题',
    description VARCHAR(500) DEFAULT NULL COMMENT '描述',
    expire_time DATETIME DEFAULT NULL COMMENT '过期时间，为NULL表示永久有效',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1正常 0禁用 2已过期 3已删除',
    pv_count BIGINT NOT NULL DEFAULT 0 COMMENT '总访问量PV',
    uv_count BIGINT NOT NULL DEFAULT 0 COMMENT '独立访客数UV',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_short_code (short_code),
    KEY idx_user_id (user_id),
    KEY idx_status (status),
    KEY idx_create_time (create_time),
    CONSTRAINT fk_short_link_user FOREIGN KEY (user_id) REFERENCES tb_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='短链接主表';

DROP TABLE IF EXISTS tb_link_access_log;
CREATE TABLE tb_link_access_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '日志ID',
    link_id BIGINT NOT NULL COMMENT '短链ID',
    short_code VARCHAR(32) NOT NULL COMMENT '短码',
    visitor_id VARCHAR(64) DEFAULT NULL COMMENT '访客标识，用于UV统计',
    user_ip VARCHAR(64) DEFAULT NULL COMMENT '访问IP',
    user_agent VARCHAR(512) DEFAULT NULL COMMENT 'User-Agent',
    referer VARCHAR(512) DEFAULT NULL COMMENT '来源页',
    device_type VARCHAR(32) DEFAULT NULL COMMENT '设备类型',
    browser VARCHAR(64) DEFAULT NULL COMMENT '浏览器',
    os VARCHAR(64) DEFAULT NULL COMMENT '操作系统',
    access_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '访问时间',
    visit_date DATE NOT NULL COMMENT '访问日期',
    KEY idx_link_id (link_id),
    KEY idx_short_code (short_code),
    KEY idx_visit_date (visit_date),
    KEY idx_access_time (access_time),
    CONSTRAINT fk_access_log_link FOREIGN KEY (link_id) REFERENCES tb_short_link(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='短链访问日志表';

DROP TABLE IF EXISTS tb_link_uv_record;
CREATE TABLE tb_link_uv_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    link_id BIGINT NOT NULL COMMENT '短链ID',
    short_code VARCHAR(32) NOT NULL COMMENT '短码',
    visitor_id VARCHAR(64) NOT NULL COMMENT '访客标识',
    visit_date DATE NOT NULL COMMENT '访问日期',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_link_visitor_date (link_id, visitor_id, visit_date),
    KEY idx_short_code (short_code),
    KEY idx_visit_date (visit_date),
    CONSTRAINT fk_uv_record_link FOREIGN KEY (link_id) REFERENCES tb_short_link(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='UV去重记录表';

DROP TABLE IF EXISTS tb_mq_message_log;
CREATE TABLE tb_mq_message_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    message_id VARCHAR(64) NOT NULL COMMENT '消息ID',
    biz_type VARCHAR(32) NOT NULL COMMENT '业务类型',
    biz_key VARCHAR(64) NOT NULL COMMENT '业务键',
    consume_status TINYINT NOT NULL DEFAULT 0 COMMENT '消费状态：0未消费 1成功 2失败',
    retry_count INT NOT NULL DEFAULT 0 COMMENT '重试次数',
    error_msg VARCHAR(500) DEFAULT NULL COMMENT '错误信息',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_message_id (message_id),
    KEY idx_biz_type (biz_type),
    KEY idx_biz_key (biz_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MQ消息日志表';

INSERT INTO tb_user (username, phone, password, nickname, status)
VALUES
('zhangsan', '13800000001', '123456', '张三', 1),
('lisi', '13800000002', '123456', '李四', 1),
('wangwu', '13800000003', '123456', '王五', 1);

INSERT INTO tb_short_link
(user_id, short_code, short_url, origin_url, title, description, expire_time, status, pv_count, uv_count)
VALUES
(1, 'abc123', 'http://localhost:20080/s/abc123', 'https://www.baidu.com', '百度首页', '测试短链-百度', NULL, 1, 0, 0),
(1, 'java001', 'http://localhost:20080/s/java001', 'https://javaguide.cn', 'JavaGuide', '测试短链-JavaGuide', NULL, 1, 0, 0),
(2, 'spring1', 'http://localhost:20080/s/spring1', 'https://spring.io', 'Spring官网', '测试短链-Spring', '2026-12-31 23:59:59', 1, 0, 0);

SET FOREIGN_KEY_CHECKS = 1;
