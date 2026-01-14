<template>
  <view class="login-page">
    <!-- 顶部装饰 -->
    <view class="header-decoration">
      <view class="circle circle-1"></view>
      <view class="circle circle-2"></view>
    </view>

    <!-- Logo 区域 -->
    <view class="logo-section">
      <view class="logo-wrapper">
        <uni-icons type="person-filled" size="60" color="#fff"></uni-icons>
      </view>
      <text class="app-name">{{ appName }}</text>
      <text class="app-desc">欢迎回来</text>
    </view>
    
    <!-- 登录表单 -->
    <view class="form-section">
      <view class="form-card">
        <view class="input-group">
          <view class="input-wrapper">
            <uni-icons type="person" size="20" color="#999"></uni-icons>
            <input 
              class="input-field"
              type="text"
              v-model="formData.username"
              placeholder="请输入用户名"
              placeholder-class="placeholder"
            />
          </view>
        </view>
        
        <view class="input-group">
          <view class="input-wrapper">
            <uni-icons type="locked" size="20" color="#999"></uni-icons>
            <input 
              class="input-field"
              :type="showPassword ? 'text' : 'password'"
              v-model="formData.password"
              placeholder="请输入密码"
              placeholder-class="placeholder"
            />
            <uni-icons 
              :type="showPassword ? 'eye' : 'eye-slash'" 
              size="20" 
              color="#999"
              @click="showPassword = !showPassword"
            ></uni-icons>
          </view>
        </view>
        
        <view class="form-options">
          <view class="remember-me" @click="rememberMe = !rememberMe">
            <uni-icons 
              :type="rememberMe ? 'checkbox-filled' : 'circle'" 
              size="18" 
              :color="rememberMe ? '#667eea' : '#999'"
            ></uni-icons>
            <text class="option-text">记住我</text>
          </view>
          <text class="forgot-link" @click="goToForgotPassword">忘记密码？</text>
        </view>
        
        <button 
          class="login-btn" 
          :loading="loading" 
          :disabled="loading"
          @click="handleLogin"
        >
          {{ loading ? '登录中...' : '登 录' }}
        </button>
        
        <view class="register-section">
          <text class="register-text">还没有账号？</text>
          <text class="register-link" @click="goToRegister">立即注册</text>
        </view>

        <view class="back-btn" @click="goBack">
          <text class="back-icon">‹</text>
          <text class="back-text">返回</text>
        </view>
      </view>
    </view>
    
    <!-- 底部装饰 -->
    <view class="footer-section">
      <text class="footer-text">登录即表示同意</text>
      <text class="link-text">《用户协议》</text>
      <text class="footer-text">和</text>
      <text class="link-text">《隐私政策》</text>
    </view>
  </view>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { BASE_URL } from '../../constant.js'
import userStore from '../../store/user'

const loading = ref(false)
const showPassword = ref(false)
const rememberMe = ref(true)
const appName = ref('My Little App')

const formData = reactive({
  username: '',
  password: ''
})

// 获取应用名称
const fetchAppName = async () => {
  try {
    const res = await uni.request({
      url: `${BASE_URL}/api/setup/app-name`,
      method: 'GET'
    })
    if (res[1].statusCode === 200 && res[1].data.success) {
      appName.value = res[1].data.data
    }
  } catch (error) {
    console.error('获取应用名称失败:', error)
  }
}

onMounted(() => {
  fetchAppName()
})

// 表单验证
const validateForm = () => {
  if (!formData.username.trim()) {
    uni.showToast({ title: '请输入用户名', icon: 'none' })
    return false
  }
  if (!formData.password) {
    uni.showToast({ title: '请输入密码', icon: 'none' })
    return false
  }
  if (formData.password.length < 6) {
    uni.showToast({ title: '密码长度不能少于6位', icon: 'none' })
    return false
  }
  return true
}

// 登录
const handleLogin = async () => {
  if (!validateForm()) return
  
  loading.value = true
  
  try {
    const result = await userStore.login(formData.username, formData.password)
    
    if (result.success) {
      uni.showToast({
        title: '登录成功',
        icon: 'success'
      })
      
      // 保存账号（如果勾选记住我）
      if (rememberMe.value) {
        uni.setStorageSync('savedUsername', formData.username)
      } else {
        uni.removeStorageSync('savedUsername')
      }
      
      // 延迟跳转，让用户看到成功提示
      setTimeout(() => {
        // 跳转到首页（博客页面）
        uni.switchTab({
          url: '/pages/blog/index'
        })
      }, 1000)
    } else {
      uni.showToast({
        title: result.message || '登录失败',
        icon: 'none'
      })
    }
  } catch (error) {
    uni.showToast({
      title: '登录失败，请稍后重试',
      icon: 'none'
    })
  } finally {
    loading.value = false
  }
}

// 跳转注册页
const goToRegister = () => {
  uni.navigateTo({
    url: '/pages/register/index'
  })
}

// 跳转忘记密码页
const goToForgotPassword = () => {
  uni.navigateTo({
    url: '/pages/forgot-password/index'
  })
}

// 返回上一页
const goBack = () => {
  const pages = getCurrentPages()
  if (pages.length > 1) {
    uni.navigateBack()
  } else {
    uni.switchTab({ url: '/pages/blog/index' })
  }
}

// 页面加载时恢复保存的用户名
const onLoad = () => {
  const savedUsername = uni.getStorageSync('savedUsername')
  if (savedUsername) {
    formData.username = savedUsername
  }
}

// 生命周期
onLoad()
</script>

<style lang="scss" scoped>
.login-page {
  min-height: 100vh;
  background: linear-gradient(180deg, #667eea 0%, #764ba2 100%);
  position: relative;
  overflow: hidden;
}

// 返回按钮
.back-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-top: 30rpx;
  padding: 20rpx;
  border-radius: 16rpx;
  background: #f5f5f5;

  .back-icon {
    font-size: 40rpx;
    color: #667eea;
    font-weight: bold;
  }

  .back-text {
    font-size: 28rpx;
    color: #667eea;
    margin-left: 4rpx;
  }
}

.header-decoration {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 400rpx;
  overflow: hidden;
}

.circle {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.1);
}

.circle-1 {
  width: 300rpx;
  height: 300rpx;
  top: -100rpx;
  right: -50rpx;
}

.circle-2 {
  width: 200rpx;
  height: 200rpx;
  top: 100rpx;
  left: -80rpx;
}

.logo-section {
  padding-top: 120rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  position: relative;
  z-index: 1;
}

.logo-wrapper {
  width: 140rpx;
  height: 140rpx;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 30rpx;
  backdrop-filter: blur(10px);
}

.app-name {
  font-size: 44rpx;
  font-weight: 700;
  color: #fff;
  margin-bottom: 12rpx;
}

.app-desc {
  font-size: 28rpx;
  color: rgba(255, 255, 255, 0.8);
}

.form-section {
  padding: 60rpx 40rpx;
  position: relative;
  z-index: 1;
}

.form-card {
  background: #fff;
  border-radius: 30rpx;
  padding: 50rpx 40rpx;
  box-shadow: 0 20rpx 60rpx rgba(0, 0, 0, 0.15);
}

.input-group {
  margin-bottom: 30rpx;
}

.input-wrapper {
  display: flex;
  align-items: center;
  padding: 24rpx 30rpx;
  background: #f8f9fa;
  border-radius: 16rpx;
  border: 2rpx solid transparent;
  transition: all 0.3s;
}

.input-wrapper:focus-within {
  border-color: #667eea;
  background: #fff;
  box-shadow: 0 4rpx 12rpx rgba(102, 126, 234, 0.15);
}

.input-field {
  flex: 1;
  margin-left: 20rpx;
  font-size: 30rpx;
  color: #333;
}

.placeholder {
  color: #bbb;
}

.form-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 40rpx;
}

.remember-me {
  display: flex;
  align-items: center;
}

.option-text {
  margin-left: 10rpx;
  font-size: 26rpx;
  color: #666;
}

.forgot-link {
  font-size: 26rpx;
  color: #667eea;
}

.login-btn {
  width: 100%;
  height: 96rpx;
  line-height: 96rpx;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 48rpx;
  font-size: 32rpx;
  font-weight: 600;
  color: #fff;
  border: none;
  box-shadow: 0 8rpx 24rpx rgba(102, 126, 234, 0.4);
}

.login-btn::after {
  border: none;
}

.login-btn[disabled] {
  opacity: 0.7;
}

.register-section {
  display: flex;
  justify-content: center;
  align-items: center;
  margin-top: 40rpx;
}

.register-text {
  font-size: 26rpx;
  color: #999;
}

.register-link {
  font-size: 26rpx;
  color: #667eea;
  font-weight: 500;
  margin-left: 8rpx;
}

.footer-section {
  position: absolute;
  bottom: 60rpx;
  left: 0;
  right: 0;
  display: flex;
  justify-content: center;
  align-items: center;
  flex-wrap: wrap;
}

.footer-text {
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.7);
}

.link-text {
  font-size: 24rpx;
  color: #fff;
  margin: 0 4rpx;
}

// PC端适配
@media screen and (min-width: 768px) {
  .login-page {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    min-height: 100vh;
  }

  .logo-section {
    padding-top: 0;
    margin-bottom: 40px;
  }

  .logo-wrapper {
    width: 100px;
    height: 100px;
  }

  .app-name {
    font-size: 36px;
  }

  .app-desc {
    font-size: 18px;
  }

  .form-section {
    padding: 0;
    width: 420px;
  }

  .form-card {
    padding: 40px;
  }

  .input-wrapper {
    padding: 16px 20px;
  }

  .input-field {
    font-size: 16px;
  }

  .form-options {
    margin-bottom: 30px;
  }

  .option-text {
    font-size: 14px;
  }

  .forgot-link {
    font-size: 14px;
  }

  .login-btn {
    height: 52px;
    line-height: 52px;
    font-size: 18px;
    border-radius: 26px;
  }

  .register-section {
    margin-top: 24px;
  }

  .register-text,
  .register-link {
    font-size: 14px;
  }

  .footer-section {
    position: relative;
    bottom: auto;
    margin-top: 30px;
  }

  .footer-text,
  .link-text {
    font-size: 13px;
  }
}
</style>







