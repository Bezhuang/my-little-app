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

-- 插入测试用户
INSERT INTO `t_user` (username, email, password, phone, status)
VALUES ('zhangsan', 'zhangsan@example.com', 'e10adc3949ba59abbe56e057f20f883e', '13800000002', 1);

INSERT INTO `t_user` (username, email, password, phone, status)
VALUES ('lisi', 'lisi@example.com', 'e10adc3949ba59abbe56e057f20f883e', '13800000003', 1);

INSERT INTO `t_user` (username, email, password, phone, status)
VALUES ('wangwu', 'wangwu@example.com', 'e10adc3949ba59abbe56e057f20f883e', '13800000004', 0);

-- ----------------------------
-- 确保 t_user 表自增从 5 开始
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
SELECT 1, 999999, 100
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
('system_prompt', '回答必须精简，输出内容不要包含Markdown格式符号。', 'AI系统提示词')
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value);

-- 模型温度配置 (0.0-2.0，越高越有创意)
INSERT INTO `ai_config` (config_key, config_value, description) VALUES
('temperature', '0.5', 'AI模型温度参数 (0.0-2.0)')
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value);

-- ----------------------------
-- DeepSeek AI 配置
-- ----------------------------
INSERT INTO `ai_config` (config_key, config_value, description) VALUES
('deepseek_api_key', '', 'DeepSeek API Key')
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value);

INSERT INTO `ai_config` (config_key, config_value, description) VALUES
('deepseek_model', 'deepseek-chat', 'DeepSeek 模型名称')
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value);

INSERT INTO `ai_config` (config_key, config_value, description) VALUES
('deepseek_enabled', 'true', '是否启用 DeepSeek (true/false)')
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value);

INSERT INTO `ai_config` (config_key, config_value, description) VALUES
('deepseek_reasoner_model', 'deepseek-reasoner', 'DeepSeek 推理模型名称')
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value);

INSERT INTO `ai_config` (config_key, config_value, description) VALUES
('deepseek_base_url', 'https://api.deepseek.com', 'DeepSeek API Base URL')
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value);

INSERT INTO `ai_config` (config_key, config_value, description) VALUES
('deepseek_max_tokens', '4096', 'DeepSeek 最大 Token 数')
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value);

INSERT INTO `ai_config` (config_key, config_value, description) VALUES
('deepseek_temperature', '0.7', 'DeepSeek 温度参数 (0.0-2.0)')
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value);

-- ----------------------------
-- SiliconFlow AI 配置
-- ----------------------------
INSERT INTO `ai_config` (config_key, config_value, description) VALUES
('siliconflow_api_key', '', 'SiliconFlow API Key')
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value);

INSERT INTO `ai_config` (config_key, config_value, description) VALUES
('siliconflow_model', 'deepseek-ai/DeepSeek-V2.5', 'SiliconFlow 模型名称')
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value);

INSERT INTO `ai_config` (config_key, config_value, description) VALUES
('siliconflow_reasoner_model', 'deepseek-ai/DeepSeek-V2.5', 'SiliconFlow 推理模型名称')
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value);

INSERT INTO `ai_config` (config_key, config_value, description) VALUES
('siliconflow_enabled', 'false', '是否启用 SiliconFlow (true/false)')
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value);

INSERT INTO `ai_config` (config_key, config_value, description) VALUES
('siliconflow_base_url', 'https://api.siliconflow.cn/v1', 'SiliconFlow API Base URL')
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value);

INSERT INTO `ai_config` (config_key, config_value, description) VALUES
('siliconflow_max_tokens', '4096', 'SiliconFlow 最大 Token 数')
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value);

INSERT INTO `ai_config` (config_key, config_value, description) VALUES
('siliconflow_temperature', '0.7', 'SiliconFlow 温度参数 (0.0-2.0)')
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value);

-- ----------------------------
-- Bocha Web Search API 配置
-- ----------------------------
INSERT INTO `ai_config` (config_key, config_value, description) VALUES
('bocha_api_key', '', 'Bocha Web Search API Key')
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value);

INSERT INTO `ai_config` (config_key, config_value, description) VALUES
('bocha_enabled', 'false', '是否启用 Bocha Web Search (true/false)')
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value);

INSERT INTO `ai_config` (config_key, config_value, description) VALUES
('bocha_search_limit', '5', 'Bocha 搜索结果数量限制')
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value);

-- ----------------------------
-- 系统配置表
-- ----------------------------
DROP TABLE IF EXISTS `system_config`;
CREATE TABLE `system_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `config_key` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '配置键',
  `config_value` text COLLATE utf8mb4_unicode_ci COMMENT '配置值',
  `description` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '描述',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置表';

-- 标记系统是否已初始化管理员
INSERT INTO `system_config` (config_key, config_value, description) VALUES
('admin_initialized', 'false', '管理员是否已初始化');

-- 应用名称
INSERT INTO `system_config` (config_key, config_value, description) VALUES
('app_name', 'My Little App', '应用名称');

-- AI 配置缓存过期时间（秒）
INSERT INTO `system_config` (config_key, config_value, description) VALUES
('cache_ai_config_ttl', '3600', 'AI 配置缓存过期时间（秒），默认 1 小时');

SET FOREIGN_KEY_CHECKS = 1;

