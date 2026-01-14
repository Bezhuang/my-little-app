/**
 * 全局常量配置
 * 通过环境变量读取敏感配置
 */

// API 服务器地址
export const BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'