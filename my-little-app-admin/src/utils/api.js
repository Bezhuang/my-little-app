/**
 * API 接口定义
 */
import request from './request'

/**
 * 认证相关接口 - JWT 认证
 */
export const authApi = {
  // 登录 - 使用 JSON 格式（JWT 认证）
  login: (username, password) => {
    return request.post('/auth/login', {
      username,
      password
    })
  },

  // 获取当前用户信息
  getInfo: () => {
    return request.get('/auth/info')
  },

  // 检查登录状态
  checkLogin: () => {
    return request.get('/auth/check')
  }
}

/**
 * 用户管理接口
 */
export const userApi = {
  // 获取用户列表
  getList: (params) => {
    return request.get('/admin/user/list', { params })
  },

  // 获取用户详情
  getDetail: (id) => {
    return request.get(`/admin/user/${id}`)
  },

  // 新增用户
  add: (data) => {
    return request.post('/admin/user/add', data)
  },

  // 更新用户
  update: (data) => {
    return request.put('/admin/user/update', data)
  },

  // 删除用户
  delete: (id) => {
    return request.delete(`/admin/user/delete/${id}`)
  },

  // 重置用户密码
  resetPassword: (id) => {
    return request.post(`/admin/user/reset-password/${id}`)
  },

  // 更新用户状态
  updateStatus: (id, status) => {
    return request.put(`/admin/user/status/${id}`, null, { params: { status } })
  }
}

/**
 * 管理员管理接口
 */
export const adminApi = {
  // 获取管理员列表
  getList: () => {
    return request.get('/admin/admin/list')
  },

  // 获取管理员详情
  getDetail: (id) => {
    return request.get(`/admin/admin/${id}`)
  },

  // 新增管理员
  add: (data) => {
    return request.post('/admin/admin/add', data)
  },

  // 更新管理员
  update: (data) => {
    return request.put('/admin/admin/update', data)
  },

  // 删除管理员
  delete: (id) => {
    return request.delete(`/admin/admin/delete/${id}`)
  },

  // 重置管理员密码
  resetPassword: (id) => {
    return request.post(`/admin/admin/reset-password/${id}`)
  },

  // 更新管理员状态
  updateStatus: (id, status) => {
    return request.put(`/admin/admin/status/${id}`, null, { params: { status } })
  },

  // 修改密码
  changePassword: (adminId, oldPassword, newPassword) => {
    return request.post('/admin/admin/change-password', {
      adminId,
      oldPassword,
      newPassword
    })
  }
}

/**
 * 想法管理接口
 */
export const thoughtApi = {
  // 获取想法列表
  getList: (params) => {
    return request.get('/admin/thoughts/list', { params })
  },

  // 获取想法详情
  getDetail: (id) => {
    return request.get(`/admin/thoughts/${id}`)
  },

  // 发布想法
  add: (data) => {
    return request.post('/admin/thoughts/add', data)
  },

  // 更新想法
  update: (data) => {
    return request.put('/admin/thoughts/update', data)
  },

  // 删除想法
  delete: (id) => {
    return request.delete(`/admin/thoughts/delete/${id}`)
  },

  // 更新可见性
  updateVisibility: (id, visibility) => {
    return request.put(`/admin/thoughts/visibility/${id}`, null, { params: { visibility } })
  }
}

/**
 * 公开想法接口（前端用户可见）
 */
export const publicThoughtApi = {
  // 获取公开想法列表
  getPublicList: (params) => {
    return request.get('/thoughts/public', { params })
  },

  // 获取想法详情
  getDetail: (id) => {
    return request.get(`/thoughts/${id}`)
  },

  // 点赞
  like: (id) => {
    return request.post(`/thoughts/${id}/like`)
  },

  // 取消点赞
  unlike: (id) => {
    return request.delete(`/thoughts/${id}/like`)
  }
}

/**
 * 图片管理接口
 */
export const imageApi = {
  // 获取图片列表
  getList: (params) => {
    return request.get('/admin/images/list', { params })
  },

  // 获取所有图片
  getAll: () => {
    return request.get('/admin/images/all')
  },

  // 获取图片详情
  getDetail: (id) => {
    return request.get(`/admin/images/${id}`)
  },

  // 上传图片
  upload: (file, onProgress) => {
    const formData = new FormData()
    formData.append('file', file)
    return request.post('/admin/images/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      onUploadProgress: (e) => {
        if (onProgress && e.total) {
          onProgress(Math.round((e.loaded * 100) / e.total))
        }
      }
    })
  },

  // 删除图片
  delete: (id) => {
    return request.delete(`/admin/images/${id}`)
  }
}

/**
 * AI 配置管理接口
 */
export const aiApi = {
  // 获取系统提示词
  getSystemPrompt: () => {
    return request.get('/admin/ai/config/prompt')
  },

  // 更新系统提示词
  updateSystemPrompt: (prompt) => {
    return request.put('/admin/ai/config/prompt', { configValue: prompt })
  },

  // 获取模型温度
  getTemperature: () => {
    return request.get('/admin/ai/config/temperature')
  },

  // 更新模型温度
  updateTemperature: (temperature) => {
    return request.put('/admin/ai/config/temperature', { configValue: temperature })
  },

  // 获取配额列表
  getQuotaList: () => {
    return request.get('/admin/ai/quota/list')
  },

  // 更新用户配额
  updateQuota: (userId, tokensRemaining, searchRemaining) => {
    return request.put(`/admin/ai/quota/${userId}`, {
      tokensRemaining,
      searchRemaining
    })
  }
}

/**
 * 控制台统计接口
 */
export const dashboardApi = {
  // 获取统计数据
  getStats: () => {
    return request.get('/admin/dashboard/stats')
  },

  // 获取最新用户
  getLatestUsers: () => {
    return request.get('/admin/dashboard/latest-users')
  },

  // 获取最新想法
  getLatestThoughts: () => {
    return request.get('/admin/dashboard/latest-thoughts')
  }
}

export default {
  auth: authApi,
  user: userApi,
  admin: adminApi,
  thought: thoughtApi,
  publicThought: publicThoughtApi,
  image: imageApi,
  ai: aiApi,
  dashboard: dashboardApi
}
