/**
 * API 接口定义 - JWT 认证
 */
import { get, post, postForm, put } from './request'

/**
 * 认证相关接口
 */
export const authApi = {
  // 登录 - 使用 JSON 格式（JWT 认证）
  login: (username, password) => {
    return post('/auth/login', { username, password })
  },

  // 获取当前用户信息
  getInfo: () => {
    return get('/auth/info')
  },

  // 检查登录状态
  checkLogin: () => {
    return get('/auth/check')
  },
  
  // 注册
  register: (data) => {
    return post('/public/register', data)
  },

  // 发送注册验证码
  sendRegisterCode: (email) => {
    return post('/public/send-register-code', { email })
  },

  // 验证注册验证码
  verifyRegisterCode: (email, code) => {
    return post('/public/verify-register-code', { email, code })
  },

  // 发送验证码（忘记密码）
  sendResetCode: (email) => {
    return post('/public/forgot-password', { email })
  },
  
  // 验证重置码
  verifyResetCode: (email, code) => {
    return post('/public/verify-reset-code', { email, code })
  },
  
  // 重置密码
  resetPassword: (email, code, newPassword) => {
    return post('/public/reset-password', { email, code, newPassword })
  }
}

/**
 * 用户相关接口
 */
export const userApi = {
  // 获取个人信息
  getProfile: () => {
    return get('/user/profile')
  },

  // 更新个人信息
  updateProfile: (data) => {
    return put('/user/profile', data)
  },

  // 修改密码
  changePassword: (oldPassword, newPassword) => {
    return postForm('/user/change-password', { oldPassword, newPassword })
  }
}

/**
 * 公开想法接口
 */
export const publicThoughtApi = {
  // 获取公开想法列表
  getPublicList: (params) => {
    return get('/thoughts/public', { params })
  },

  // 获取想法详情
  getDetail: (id) => {
    return get(`/thoughts/${id}`)
  },

  // 点赞
  like: (id) => {
    return post(`/thoughts/${id}/like`)
  },

  // 取消点赞
  unlike: (id) => {
    return post(`/thoughts/${id}/like`) // 后端使用同一个接口，DELETE方法
  }
}

/**
 * AI 聊天接口
 */
export const aiApi = {
  // 检查 AI 功能权限
  checkAccess: () => {
    return get('/ai/check-access')
  },

  // 创建新对话
  createConversation: () => {
    return post('/ai/conversation/create')
  },

  // 获取对话列表
  getConversations: () => {
    return get('/ai/conversations')
  },

  // 获取对话详情
  getConversation: (id) => {
    return get(`/ai/conversation/${id}`)
  },

  // 获取对话消息
  getMessages: (id) => {
    return get(`/ai/conversation/${id}/messages`)
  },

  // 结束对话
  endConversation: (id) => {
    return post(`/ai/conversation/${id}/end`)
  }
}

export default {
  auth: authApi,
  user: userApi,
  publicThought: publicThoughtApi,
  ai: aiApi
}





