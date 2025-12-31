/**
 * 用户状态管理
 */
import { reactive } from 'vue'
import { authApi, userApi } from '../utils/api'

// 用户状态
const state = reactive({
  isLogin: false,
  userInfo: null
})

// 初始化 - 从本地存储恢复状态
const init = () => {
  const userInfo = uni.getStorageSync('userInfo')
  
  if (userInfo) {
    state.isLogin = true
    state.userInfo = userInfo
  }
}

// 登录
const login = async (username, password) => {
  try {
    const res = await authApi.login(username, password)
    
    if (res.success) {
      state.isLogin = true
      state.userInfo = res.data
      
      // 保存到本地存储
      uni.setStorageSync('userInfo', res.data)
      
      // 登录成功后尝试获取完整用户信息（失败不影响登录结果）
      try {
        await fetchUserInfo()
      } catch (e) {
        console.log('fetchUserInfo after login error:', e)
      }
      
      return { success: true, data: state.userInfo }
    }
    
    return { success: false, message: res.message }
  } catch (error) {
    return { success: false, message: error.message || '登录失败' }
  }
}

// 登出
const logout = async () => {
  try {
    await authApi.logout()
  } catch (error) {
    console.log('logout error:', error)
  } finally {
    // 无论成功失败都清除本地状态
    state.isLogin = false
    state.userInfo = null
    
    uni.removeStorageSync('userInfo')
  }
}

// 获取用户信息
const fetchUserInfo = async () => {
  try {
    const res = await userApi.getProfile()
    
    if (res.success) {
      // 合并用户信息，保留原有的 userType 等字段
      state.userInfo = { ...state.userInfo, ...res.data }
      uni.setStorageSync('userInfo', state.userInfo)
      return { success: true, data: res.data }
    }
    
    return { success: false, message: res.message }
  } catch (error) {
    console.log('fetchUserInfo error:', error)
    return { success: false, message: error.message || '获取用户信息失败' }
  }
}

// 更新用户信息
const updateProfile = async (data) => {
  try {
    const res = await userApi.updateProfile(data)
    
    if (res.success) {
      // 更新本地状态
      state.userInfo = { ...state.userInfo, ...data }
      uni.setStorageSync('userInfo', state.userInfo)
      return { success: true }
    }
    
    return { success: false, message: res.message }
  } catch (error) {
    return { success: false, message: error.message || '更新失败' }
  }
}

// 修改密码
const changePassword = async (oldPassword, newPassword) => {
  try {
    const res = await userApi.changePassword(oldPassword, newPassword)
    return { success: res.success, message: res.message }
  } catch (error) {
    return { success: false, message: error.message || '修改密码失败' }
  }
}

// 检查登录状态
const checkLoginStatus = async () => {
  if (!state.isLogin) {
    return false
  }
  
  try {
    const res = await authApi.checkLogin()
    if (!res.success || !res.data) {
      // 登录已失效
      logout()
      return false
    }
    return true
  } catch (error) {
    logout()
    return false
  }
}

// 初始化
init()

export default {
  state,
  init,
  login,
  logout,
  fetchUserInfo,
  updateProfile,
  changePassword,
  checkLoginStatus
}
