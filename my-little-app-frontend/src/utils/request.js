/**
 * 网络请求封装 - JWT 认证
 */

import { BASE_URL } from './constants'

// API 基础地址
const API_BASE_URL = BASE_URL + '/api'

/**
 * 获取 JWT token
 */
const getToken = () => {
  const userInfo = uni.getStorageSync('userInfo')
  if (userInfo && userInfo.token) {
    return userInfo.token
  }
  return null
}

/**
 * 封装 uni.request
 * @param {Object} options 请求配置
 * @returns {Promise}
 */
const request = (options) => {
  return new Promise((resolve, reject) => {
    // 获取 JWT token
    const token = getToken()

    uni.request({
      url: API_BASE_URL + options.url,
      method: options.method || 'GET',
      timeout: options.timeout || 10000, // 默认10秒超时
      data: options.data || {},
      header: {
        'Content-Type': options.contentType || 'application/json',
        'Authorization': token ? `Bearer ${token}` : '',
        ...options.header
      },
      success: (res) => {
        const { statusCode, data } = res

        if (statusCode === 200) {
          // 请求成功
          if (data.success) {
            resolve(data)
          } else {
            // 业务错误
            uni.showToast({
              title: data.message || '请求失败',
              icon: 'none'
            })
            reject(data)
          }
        } else if (statusCode === 401) {
          // 未登录、登录过期或认证失败
          const errorMsg = data.message || '请先登录'

          // 只有在非登录请求时才清除用户信息
          if (!options.url.includes('/auth/login')) {
            uni.removeStorageSync('userInfo')
          }

          uni.showToast({
            title: errorMsg,
            icon: 'none'
          })
          reject({ code: 401, message: errorMsg })
        } else if (statusCode === 403) {
          uni.showToast({
            title: '权限不足',
            icon: 'none'
          })
          reject({ code: 403, message: '权限不足' })
        } else {
          uni.showToast({
            title: data.message || '请求失败',
            icon: 'none'
          })
          reject(data)
        }
      },
      fail: (err) => {
        uni.showToast({
          title: '网络错误，请稍后重试',
          icon: 'none'
        })
        reject(err)
      }
    })
  })
}

/**
 * GET 请求
 */
export const get = (url, data = {}) => {
  return request({ url, method: 'GET', data })
}

/**
 * POST 请求
 */
export const post = (url, data = {}) => {
  return request({ url, method: 'POST', data })
}

/**
 * POST 表单请求
 */
export const postForm = (url, data = {}) => {
  // 将对象转换为 URL 编码字符串
  const formData = Object.keys(data)
    .map(key => `${encodeURIComponent(key)}=${encodeURIComponent(data[key])}`)
    .join('&')

  return request({
    url,
    method: 'POST',
    data: formData,
    contentType: 'application/x-www-form-urlencoded'
  })
}

/**
 * PUT 请求
 */
export const put = (url, data = {}) => {
  return request({ url, method: 'PUT', data })
}

/**
 * DELETE 请求
 */
export const del = (url, data = {}) => {
  return request({ url, method: 'DELETE', data })
}

export default request
