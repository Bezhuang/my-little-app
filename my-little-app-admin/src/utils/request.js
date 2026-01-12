/**
 * 网络请求封装 - JWT 认证
 */

import axios from 'axios'
import { BASE_URL } from '../constant'

// API 基础地址
const API_BASE_URL = BASE_URL + '/api'

// 创建 axios 实例
const request = axios.create({
  baseURL: API_BASE_URL,
  timeout: 15000
  // JWT 认证不使用 withCredentials，使用 Authorization header
})

// 请求拦截器
request.interceptors.request.use(
  (config) => {
    // 从 localStorage 获取 token
    const adminUser = localStorage.getItem('admin_user')
    if (adminUser) {
      try {
        const user = JSON.parse(adminUser)
        if (user.token) {
          config.headers['Authorization'] = `Bearer ${user.token}`
        }
      } catch (e) {
        console.error('解析用户信息失败', e)
      }
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器
request.interceptors.response.use(
  (response) => {
    const { data, config } = response

    // 登录接口返回格式：{ code, message, success, data, token }
    // 检测是否是登录请求的响应
    if (config.url && config.url.includes('/auth/login')) {
      if (data.success) {
        // 登录成功，返回包含 token 的数据
        return {
          success: true,
          data: data.data  // 包含 token, id, username 等
        }
      } else {
        return Promise.reject(new Error(data.message || '登录失败'))
      }
    }

    // 获取用户信息接口
    if (config.url && config.url.includes('/auth/info')) {
      if (data.success) {
        return data.data  // 直接返回用户信息对象
      } else {
        return Promise.reject(new Error(data.message || '获取用户信息失败'))
      }
    }

    // 其他接口（Result 格式）：{ code, message, data, success }
    if (data && data.success !== undefined) {
      if (data.success) {
        return data
      } else {
        return Promise.reject(new Error(data.message || '请求失败'))
      }
    }

    // 其他情况直接返回
    return data
  },
  (error) => {
    let message = error.message

    if (error.response) {
      const { status, data } = error.response

      switch (status) {
        case 401:
          message = data?.message || '用户名或密码错误'
          // 清除本地登录状态
          localStorage.removeItem('admin_user')
          break
        case 403:
          message = '权限不足'
          break
        case 404:
          message = '请求的资源不存在'
          break
        case 500:
          message = '服务器错误'
          break
        default:
          message = data?.message || error.message || '请求失败'
      }
    } else if (error.request) {
      message = '网络错误，请检查网络连接'
    }

    return Promise.reject(new Error(message))
  }
)

export default request
