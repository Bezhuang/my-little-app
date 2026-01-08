<template>
  <view class="forgot-page">
    <!-- 顶部装饰 -->
    <view class="header-decoration">
      <view class="circle circle-1"></view>
    </view>
    
    <!-- 标题区域 -->
    <view class="title-section">
      <text class="title">忘记密码</text>
      <text class="subtitle">输入注册邮箱，我们将发送验证码</text>
    </view>
    
    <!-- 表单 -->
    <view class="form-section">
      <view class="form-card">
        <!-- 步骤指示器 -->
        <view class="steps">
          <view class="step" :class="{ active: step >= 1, done: step > 1 }">
            <view class="step-circle">1</view>
            <text class="step-text">验证邮箱</text>
          </view>
          <view class="step-line" :class="{ active: step > 1 }"></view>
          <view class="step" :class="{ active: step >= 2, done: step > 2 }">
            <view class="step-circle">2</view>
            <text class="step-text">输入验证码</text>
          </view>
          <view class="step-line" :class="{ active: step > 2 }"></view>
          <view class="step" :class="{ active: step >= 3 }">
            <view class="step-circle">3</view>
            <text class="step-text">重置密码</text>
          </view>
        </view>
        
        <!-- 步骤1：输入邮箱 -->
        <view class="step-content" v-if="step === 1">
          <view class="input-group">
            <view class="input-wrapper">
              <uni-icons type="email" size="20" color="#999"></uni-icons>
              <input 
                class="input-field"
                type="text"
                v-model="email"
                placeholder="请输入注册时使用的邮箱"
                placeholder-class="placeholder"
              />
            </view>
          </view>
          
          <button 
            class="submit-btn" 
            :loading="loading" 
            :disabled="loading"
            @click="sendCode"
          >
            {{ loading ? '发送中...' : '发送验证码' }}
          </button>
        </view>
        
        <!-- 步骤2：输入验证码 -->
        <view class="step-content" v-if="step === 2">
          <view class="code-info">
            <text class="info-text">验证码已发送至</text>
            <text class="info-email">{{ maskEmail(email) }}</text>
          </view>
          
          <view class="code-input-group">
            <input
              v-for="(item, index) in 6"
              :key="index"
              class="code-input"
              type="number"
              maxlength="1"
              :value="codeArray[index]"
              :focus="focusStates[index]"
              @input="onCodeInput($event, index)"
              @blur="onInputBlur(index)"
            />
          </view>
          
          <view class="resend-section">
            <text v-if="countdown > 0" class="countdown-text">{{ countdown }}秒后可重新发送</text>
            <text v-else class="resend-link" @click="sendCode">重新发送验证码</text>
          </view>
          
          <button 
            class="submit-btn" 
            :loading="loading" 
            :disabled="loading || code.length < 6"
            @click="verifyCode"
          >
            {{ loading ? '验证中...' : '验证' }}
          </button>
        </view>
        
        <!-- 步骤3：重置密码 -->
        <view class="step-content" v-if="step === 3">
          <view class="input-group">
            <text class="input-label">新密码</text>
            <view class="input-wrapper">
              <uni-icons type="locked" size="20" color="#999"></uni-icons>
              <input 
                class="input-field"
                :type="showPassword ? 'text' : 'password'"
                v-model="newPassword"
                placeholder="请输入新密码（至少6位）"
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
          
          <view class="input-group">
            <text class="input-label">确认新密码</text>
            <view class="input-wrapper">
              <uni-icons type="locked" size="20" color="#999"></uni-icons>
              <input 
                class="input-field"
                :type="showConfirmPassword ? 'text' : 'password'"
                v-model="confirmPassword"
                placeholder="请再次输入新密码"
                placeholder-class="placeholder"
              />
              <uni-icons 
                :type="showConfirmPassword ? 'eye' : 'eye-slash'" 
                size="20" 
                color="#999"
                @click="showConfirmPassword = !showConfirmPassword"
              ></uni-icons>
            </view>
          </view>
          
          <button 
            class="submit-btn" 
            :loading="loading" 
            :disabled="loading"
            @click="resetPassword"
          >
            {{ loading ? '重置中...' : '重置密码' }}
          </button>
        </view>
        
        <view class="back-section">
          <text class="back-link" @click="goBack">返回登录</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, nextTick, onUnmounted, watch } from 'vue'
import { authApi } from '../../utils/api'

const step = ref(1)
const loading = ref(false)
const email = ref('')
const code = ref('')
const codeArray = ref(['', '', '', '', '', ''])
const focusStates = ref([false, false, false, false, false, false]) // 控制每个输入框的聚焦状态
const countdown = ref(0)
const newPassword = ref('')
const confirmPassword = ref('')
const showPassword = ref(false)
const showConfirmPassword = ref(false)
const codeInputSectionVisible = ref(false) // 控制验证码输入区域显示

let countdownTimer = null

// 监听验证码输入区域显示，自动聚焦第一个输入框
watch(step, (newStep) => {
  if (newStep === 2) {
    nextTick(() => {
      setTimeout(() => {
        focusStates.value = [true, false, false, false, false, false]
      }, 100)
    })
  }
})

// 邮箱脱敏
const maskEmail = (email) => {
  const [local, domain] = email.split('@')
  if (local.length <= 2) {
    return `${local[0]}***@${domain}`
  }
  return `${local.substring(0, 2)}***@${domain}`
}

// 发送验证码
const sendCode = async () => {
  if (!email.value.trim()) {
    uni.showToast({ title: '请输入邮箱', icon: 'none' })
    return
  }
  
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  if (!emailRegex.test(email.value)) {
    uni.showToast({ title: '邮箱格式不正确', icon: 'none' })
    return
  }
  
  loading.value = true
  
  try {
    const result = await authApi.sendResetCode(email.value)
    
    if (result.success) {
      uni.showToast({
        title: '验证码已发送',
        icon: 'success'
      })
      codeInputSectionVisible.value = true
      step.value = 2
      startCountdown()
    } else {
      uni.showToast({
        title: result.message || '发送失败',
        icon: 'none'
      })
    }
  } catch (error) {
    uni.showToast({
      title: error.message || '发送失败，请稍后重试',
      icon: 'none'
    })
  } finally {
    loading.value = false
  }
}

// 倒计时
const startCountdown = () => {
  countdown.value = 60
  countdownTimer = setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) {
      clearInterval(countdownTimer)
    }
  }, 1000)
}

// 验证码输入
const onCodeInput = async (e, index) => {
  const value = e.detail.value
  codeArray.value[index] = value
  code.value = codeArray.value.join('')

  // 自动跳转到下一个输入框
  if (value && index < 5) {
    await nextTick()
    // 延迟确保焦点切换成功
    setTimeout(() => {
      focusStates.value[index] = false
      focusStates.value[index + 1] = true
    }, 50)
  }
}

// 输入框失焦时聚焦到下一个（兼容 uni-app）
const onInputBlur = async (index) => {
  if (codeArray.value[index] && index < 5) {
    await nextTick()
    setTimeout(() => {
      focusStates.value[index] = false
      focusStates.value[index + 1] = true
    }, 50)
  }
}

// 验证验证码
const verifyCode = async () => {
  if (code.value.length < 6) {
    uni.showToast({ title: '请输入完整验证码', icon: 'none' })
    return
  }
  
  loading.value = true
  
  try {
    const result = await authApi.verifyResetCode(email.value, code.value)
    
    if (result.success) {
      step.value = 3
    } else {
      uni.showToast({
        title: result.message || '验证码错误',
        icon: 'none'
      })
    }
  } catch (error) {
    uni.showToast({
      title: error.message || '验证失败',
      icon: 'none'
    })
  } finally {
    loading.value = false
  }
}

// 重置密码
const resetPassword = async () => {
  if (!newPassword.value) {
    uni.showToast({ title: '请输入新密码', icon: 'none' })
    return
  }
  if (newPassword.value.length < 6) {
    uni.showToast({ title: '密码长度不能少于6位', icon: 'none' })
    return
  }
  if (newPassword.value !== confirmPassword.value) {
    uni.showToast({ title: '两次密码输入不一致', icon: 'none' })
    return
  }
  
  loading.value = true
  
  try {
    const result = await authApi.resetPassword(email.value, code.value, newPassword.value)
    
    if (result.success) {
      uni.showToast({
        title: '密码重置成功',
        icon: 'success'
      })
      
      setTimeout(() => {
        uni.redirectTo({
          url: '/pages/login/index'
        })
      }, 1500)
    } else {
      uni.showToast({
        title: result.message || '重置失败',
        icon: 'none'
      })
    }
  } catch (error) {
    uni.showToast({
      title: error.message || '重置失败，请稍后重试',
      icon: 'none'
    })
  } finally {
    loading.value = false
  }
}

// 返回登录
const goBack = () => {
  uni.navigateBack()
}

// 清理定时器
onUnmounted(() => {
  if (countdownTimer) {
    clearInterval(countdownTimer)
  }
})
</script>

<style lang="scss" scoped>
.forgot-page {
  min-height: 100vh;
  background: linear-gradient(180deg, #667eea 0%, #764ba2 100%);
  position: relative;
}

.header-decoration {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 300rpx;
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

.title-section {
  padding-top: 80rpx;
  padding-left: 50rpx;
  position: relative;
  z-index: 1;
}

.title {
  display: block;
  font-size: 48rpx;
  font-weight: 700;
  color: #fff;
  margin-bottom: 12rpx;
}

.subtitle {
  font-size: 28rpx;
  color: rgba(255, 255, 255, 0.8);
}

.form-section {
  padding: 40rpx;
  position: relative;
  z-index: 1;
}

.form-card {
  background: #fff;
  border-radius: 30rpx;
  padding: 40rpx;
  box-shadow: 0 20rpx 60rpx rgba(0, 0, 0, 0.15);
}

.steps {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 50rpx;
}

.step {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.step-circle {
  width: 48rpx;
  height: 48rpx;
  border-radius: 50%;
  background: #e0e0e0;
  color: #999;
  font-size: 24rpx;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 10rpx;
  transition: all 0.3s;
}

.step.active .step-circle {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
}

.step.done .step-circle {
  background: #52c41a;
  color: #fff;
}

.step-text {
  font-size: 22rpx;
  color: #999;
}

.step.active .step-text {
  color: #667eea;
  font-weight: 500;
}

.step-line {
  width: 60rpx;
  height: 4rpx;
  background: #e0e0e0;
  margin: 0 16rpx;
  margin-bottom: 30rpx;
  transition: all 0.3s;
}

.step-line.active {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.step-content {
  padding: 20rpx 0;
}

.input-group {
  margin-bottom: 30rpx;
}

.input-label {
  display: block;
  font-size: 26rpx;
  color: #666;
  margin-bottom: 12rpx;
  font-weight: 500;
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

.code-info {
  text-align: center;
  margin-bottom: 40rpx;
}

.info-text {
  font-size: 28rpx;
  color: #666;
}

.info-email {
  font-size: 28rpx;
  color: #667eea;
  font-weight: 500;
}

.code-input-group {
  display: flex;
  justify-content: center;
  gap: 16rpx;
  margin-bottom: 30rpx;
}

.code-input {
  width: 80rpx;
  height: 96rpx;
  border: 2rpx solid #e0e0e0;
  border-radius: 12rpx;
  text-align: center;
  font-size: 40rpx;
  font-weight: 600;
  color: #333;
}

.code-input:focus {
  border-color: #667eea;
  box-shadow: 0 4rpx 12rpx rgba(102, 126, 234, 0.15);
}

.resend-section {
  text-align: center;
  margin-bottom: 40rpx;
}

.countdown-text {
  font-size: 26rpx;
  color: #999;
}

.resend-link {
  font-size: 26rpx;
  color: #667eea;
}

.submit-btn {
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

.submit-btn::after {
  border: none;
}

.submit-btn[disabled] {
  opacity: 0.6;
}

.back-section {
  text-align: center;
  margin-top: 30rpx;
}

.back-link {
  font-size: 28rpx;
  color: #667eea;
}
</style>







