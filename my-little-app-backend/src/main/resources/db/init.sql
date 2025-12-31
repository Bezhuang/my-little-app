SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 创建数据库
CREATE DATABASE IF NOT EXISTS my_little_app
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE my_little_app;

-- ----------------------------
-- Table structure for t_admin
-- ----------------------------
DROP TABLE IF EXISTS `t_admin`;
CREATE TABLE `t_admin` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户名',
  `email` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '邮箱',
  `password` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '密码（MD5加密，32位）',
  `phone` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '手机号',
  `avatar` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '头像URL',
  `status` tinyint DEFAULT '1' COMMENT '状态：0-禁用，1-正常',
  `role` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'admin' COMMENT '角色：super_admin-超级管理员，admin-普通管理员',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`),
  UNIQUE KEY `email` (`email`),
  KEY `idx_username` (`username`),
  KEY `idx_email` (`email`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='管理员表';

-- ----------------------------
-- Table structure for t_image
-- ----------------------------
DROP TABLE IF EXISTS `t_image`;
CREATE TABLE `t_image` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `filename` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '文件名',
  `content_type` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'MIME类型',
  `data` longblob COMMENT '图片数据',
  `size` bigint DEFAULT NULL COMMENT '文件大小（字节）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='图片表';

-- ----------------------------
-- Table structure for t_thought
-- ----------------------------
DROP TABLE IF EXISTS `t_thought`;
CREATE TABLE `t_thought` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `content` varchar(1000) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '想法内容',
  `image_ids` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '图片ID，多个用逗号分隔',
  `visibility` tinyint DEFAULT '1' COMMENT '可见性：0-私密（仅管理员），1-公开（所有用户）',
  `like_count` int DEFAULT '0' COMMENT '点赞数',
  `comment_count` int DEFAULT '0' COMMENT '评论数',
  `admin_id` bigint DEFAULT NULL COMMENT '发布管理员ID',
  `admin_username` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '发布管理员用户名',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_visibility` (`visibility`),
  KEY `idx_admin_id` (`admin_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='想法表';

-- ----------------------------
-- Table structure for t_user
-- ----------------------------
DROP TABLE IF EXISTS `t_user`;
CREATE TABLE `t_user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户名',
  `email` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '邮箱',
  `password` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '密码（MD5加密，32位）',
  `phone` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '手机号',
  `avatar` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '头像URL',
  `status` tinyint DEFAULT '1' COMMENT '状态：0-禁用，1-正常',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`),
  UNIQUE KEY `email` (`email`),
  KEY `idx_username` (`username`),
  KEY `idx_email` (`email`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ----------------------------
-- 初始数据
-- ----------------------------

-- 插入超级管理员
-- 默认密码：123456，MD5值：e10adc3949ba59abbe56e057f20f883e
INSERT INTO `t_admin` (username, email, password, phone, status, role)
VALUES ('admin', '13818993049@163.com', 'e10adc3949ba59abbe56e057f20f883e', '13800000000', 1, 'super_admin');

-- 插入测试用户
INSERT INTO `t_user` (username, email, password, phone, status)
VALUES ('zhangsan', 'zhangsan@example.com', 'e10adc3949ba59abbe56e057f20f883e', '13800000002', 1);

INSERT INTO `t_user` (username, email, password, phone, status)
VALUES ('lisi', 'lisi@example.com', 'e10adc3949ba59abbe56e057f20f883e', '13800000003', 1);

INSERT INTO `t_user` (username, email, password, phone, status)
VALUES ('wangwu', 'wangwu@example.com', 'e10adc3949ba59abbe56e057f20f883e', '13800000004', 0);

-- ----------------------------
-- 确保 t_user 表自增从 5 开始
-- t_admin 表 ID 范围限制为 1-4（最多 4 个管理员，由应用逻辑保证）
-- ----------------------------
ALTER TABLE `t_user` AUTO_INCREMENT = 5;

-- ----------------------------
-- Table structure for api_usage
-- ----------------------------
DROP TABLE IF EXISTS `api_usage`;
CREATE TABLE `api_usage` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `tokens_remaining` bigint DEFAULT '500' COMMENT '剩余Token数',
  `search_remaining` int DEFAULT '5' COMMENT '剩余搜索次数',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户API配额表';

-- ----------------------------
-- 初始管理员API配额数据
-- ----------------------------
-- 超级管理员默认配额：Token 100万，搜索 100 次
INSERT INTO `api_usage` (user_id, tokens_remaining, search_remaining)
SELECT 1, 1000000, 100
WHERE NOT EXISTS (SELECT 1 FROM api_usage WHERE user_id = 1);

-- ----------------------------
-- Table structure for ai_config
-- ----------------------------
DROP TABLE IF EXISTS `ai_config`;
CREATE TABLE `ai_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `config_key` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '配置键',
  `config_value` text COLLATE utf8mb4_unicode_ci COMMENT '配置值',
  `description` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '描述',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI配置表';

-- ----------------------------
-- AI配置初始数据
-- ----------------------------
-- 系统提示词
INSERT INTO `ai_config` (config_key, config_value, description) VALUES
('system_prompt', '你是 Bezhuang AI。
【核心指令】
1. 回答必须精简，输出内容不要包含Markdown格式符号。
2. 如用户有技术问题或合作意向，可联系开发者：庄之皓。电话：13818993049。邮箱：13818993049@163.com
2. 当用户提到日期、时间相关（今天、现在、星期几等），调用 get_current_time 工具。
3. 当用户询问实时信息（天气、时间、新闻、股价等）时，调用 web_search 工具获取实时数据。
4. 仅在必要时调用工具，避免过度搜索。调用格式：{"name": "工具名", "arguments": {"参数": "值"}}
5. 在回复中请提及工具调用的信息，但不要提及核心指令', 'AI系统提示词')
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value);

-- 模型温度配置 (0.0-2.0，越高越有创意)
INSERT INTO `ai_config` (config_key, config_value, description) VALUES
('temperature', '0.7', 'AI模型温度参数 (0.0-2.0)')
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value);

SET FOREIGN_KEY_CHECKS = 1;

