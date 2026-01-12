<template>
  <view class="register-page">
    <!-- 顶部装饰 -->
    <view class="header-decoration">
      <view class="circle circle-1"></view>
      <view class="circle circle-2"></view>
    </view>

    <!-- 标题区域 -->
    <view class="title-section">
      <text class="title">创建账号</text>
      <text class="subtitle">加入我们，开启精彩之旅</text>
    </view>

    <!-- 注册表单 -->
    <view class="form-section">
      <view class="form-card">
        <!-- 步骤指示器 -->
        <view class="steps" v-if="step > 1">
          <view class="step" :class="{ active: step >= 1, done: step > 1 }">
            <view class="step-circle">1</view>
            <text class="step-text">验证邮箱</text>
          </view>
          <view class="step-line" :class="{ active: step > 1 }"></view>
          <view class="step" :class="{ active: step >= 2 }">
            <view class="step-circle">2</view>
            <text class="step-text">填写信息</text>
          </view>
        </view>

        <!-- 步骤1：验证邮箱 -->
        <view class="step-content" v-if="step === 1">
          <view class="input-group">
            <text class="input-label">邮箱</text>
            <view class="input-wrapper">
              <uni-icons type="email" size="20" color="#999"></uni-icons>
              <input
                class="input-field"
                type="text"
                v-model="email"
                placeholder="请输入邮箱地址"
                placeholder-class="placeholder"
                :disabled="sendingCode"
              />
            </view>
          </view>

          <button
            class="send-code-btn"
            :loading="sendingCode"
            :disabled="sendingCode || !isValidEmail"
            @click="sendCode"
          >
            {{ sendingCode ? '发送中...' : '发送验证码' }}
          </button>

          <view class="code-input-section" v-if="codeInputSectionVisible">
            <text class="code-hint">验证码已发送至 {{ maskEmail(email) }}</text>
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
          </view>

          <button
            class="next-btn"
            :loading="verifying"
            :disabled="!isCodeComplete || verifying"
            @click="verifyCode"
          >
            {{ verifying ? '验证中...' : '下一步' }}
          </button>
        </view>

        <!-- 步骤2：填写信息 -->
        <view class="step-content" v-if="step === 2">
          <view class="input-group">
            <text class="input-label">用户名</text>
            <view class="input-wrapper">
              <uni-icons type="person" size="20" color="#999"></uni-icons>
              <input
                class="input-field"
                type="text"
                v-model="formData.username"
                placeholder="请输入用户名（4-20位）"
                placeholder-class="placeholder"
              />
            </view>
          </view>

          <view class="input-group">
            <text class="input-label">手机号（可选）</text>
            <view class="input-wrapper">
              <uni-icons type="phone" size="20" color="#999"></uni-icons>
              <input
                class="input-field"
                type="number"
                v-model="formData.phone"
                placeholder="请输入手机号"
                placeholder-class="placeholder"
              />
            </view>
          </view>

          <view class="input-group">
            <text class="input-label">密码</text>
            <view class="input-wrapper">
              <uni-icons type="locked" size="20" color="#999"></uni-icons>
              <input
                class="input-field"
                :type="showPassword ? 'text' : 'password'"
                v-model="formData.password"
                placeholder="请输入密码（至少6位）"
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
            <text class="input-label">确认密码</text>
            <view class="input-wrapper">
              <uni-icons type="locked" size="20" color="#999"></uni-icons>
              <input
                class="input-field"
                :type="showConfirmPassword ? 'text' : 'password'"
                v-model="formData.confirmPassword"
                placeholder="请再次输入密码"
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

          <view class="agreement-section" @click="agreed = !agreed">
            <uni-icons
              :type="agreed ? 'checkbox-filled' : 'circle'"
              size="18"
              :color="agreed ? '#667eea' : '#999'"
            ></uni-icons>
            <text class="agreement-text">我已阅读并同意</text>
            <text class="agreement-link">《用户协议》</text>
            <text class="agreement-text">和</text>
            <text class="agreement-link">《隐私政策》</text>
          </view>

          <button
            class="register-btn"
            :loading="loading"
            :disabled="loading || !agreed"
            @click="handleRegister"
          >
            {{ loading ? '注册中...' : '立即注册' }}
          </button>
        </view>

        <view class="login-section">
          <text class="login-text">已有账号？</text>
          <text class="login-link" @click="goToLogin">立即登录</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, reactive, computed, nextTick, onUnmounted, watch } from 'vue'
import { authApi } from '../../utils/api'

const step = ref(1)
const loading = ref(false)
const sendingCode = ref(false)
const verifying = ref(false)
const email = ref('')
const code = ref('')
const codeArray = ref(['', '', '', '', '', ''])
const focusStates = ref([false, false, false, false, false, false]) // 控制每个输入框的聚焦状态
const countdown = ref(0)
const showPassword = ref(false)
const showConfirmPassword = ref(false)
const agreed = ref(false)
const codeInputSectionVisible = ref(false) // 控制验证码输入区域显示

const formData = reactive({
  username: '',
  phone: '',
  password: '',
  confirmPassword: ''
})

let countdownTimer = null

// 监听验证码输入区域显示，自动聚焦第一个输入框
watch(codeInputSectionVisible, (visible) => {
  if (visible) {
    nextTick(() => {
      setTimeout(() => {
        focusStates.value = [true, false, false, false, false, false]
      }, 100)
    })
  }
})

// 邮箱验证
const isValidEmail = computed(() => {
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  return emailRegex.test(email.value)
})

// 验证码是否完整
const isCodeComplete = computed(() => code.value.length >= 6)

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
  if (!isValidEmail.value) {
    uni.showToast({ title: '请输入有效邮箱', icon: 'none' })
    return
  }

  sendingCode.value = true

  try {
    const result = await authApi.sendRegisterCode(email.value)

    if (result.success) {
      codeInputSectionVisible.value = true
      uni.showToast({
        title: '验证码已发送',
        icon: 'success'
      })
      startCountdown()
    } else {
      // 发送失败也显示输入框，让用户可以输入
      codeInputSectionVisible.value = true
      uni.showToast({
        title: result.message || '发送失败',
        icon: 'none'
      })
    }
  } catch (error) {
    // 发送失败也显示输入框
    codeInputSectionVisible.value = true
    uni.showToast({
      title: error.data?.message || error.message || '发送失败，请稍后重试',
      icon: 'none'
    })
  } finally {
    sendingCode.value = false
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
  if (!isCodeComplete.value) {
    uni.showToast({ title: '请输入完整验证码', icon: 'none' })
    return
  }

  verifying.value = true

  try {
    const result = await authApi.verifyRegisterCode(email.value, code.value)

    if (result.success) {
      step.value = 2
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
    verifying.value = false
  }
}

// 表单验证
const validateForm = () => {
  // 用户名验证
  if (!formData.username.trim()) {
    uni.showToast({ title: '请输入用户名', icon: 'none' })
    return false
  }
  if (formData.username.length < 4 || formData.username.length > 20) {
    uni.showToast({ title: '用户名长度为4-20位', icon: 'none' })
    return false
  }

  // 手机号验证（可选）
  if (formData.phone && !/^1[3-9]\d{9}$/.test(formData.phone)) {
    uni.showToast({ title: '手机号格式不正确', icon: 'none' })
    return false
  }

  // 密码验证
  if (!formData.password) {
    uni.showToast({ title: '请输入密码', icon: 'none' })
    return false
  }
  if (formData.password.length < 6) {
    uni.showToast({ title: '密码长度不能少于6位', icon: 'none' })
    return false
  }

  // 确认密码
  if (formData.password !== formData.confirmPassword) {
    uni.showToast({ title: '两次密码输入不一致', icon: 'none' })
    return false
  }

  // 协议
  if (!agreed.value) {
    uni.showToast({ title: '请先同意用户协议', icon: 'none' })
    return false
  }

  return true
}

// 注册
const handleRegister = async () => {
  if (!validateForm()) return

  loading.value = true

  try {
    const result = await authApi.register({
      username: formData.username,
      email: email.value,
      phone: formData.phone || null,
      password: formData.password
    })

    if (result.success) {
      uni.showToast({
        title: '注册成功',
        icon: 'success'
      })

      // 延迟跳转到登录页
      setTimeout(() => {
        uni.redirectTo({
          url: '/pages/login/index'
        })
      }, 1500)
    } else {
      uni.showToast({
        title: result.message || '注册失败',
        icon: 'none'
      })
    }
  } catch (error) {
    uni.showToast({
      title: error.message || '注册失败，请稍后重试',
      icon: 'none'
    })
  } finally {
    loading.value = false
  }
}

// 跳转登录页
const goToLogin = () => {
  uni.redirectTo({
    url: '/pages/login/index'
  })
}

// 清理定时器
onUnmounted(() => {
  if (countdownTimer) {
    clearInterval(countdownTimer)
  }
})
</script>

<style lang="scss" scoped>
.register-page {
  min-height: 100vh;
  background: linear-gradient(180deg, #667eea 0%, #764ba2 100%);
  position: relative;
  overflow: hidden;
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
  width: 250rpx;
  height: 250rpx;
  top: -80rpx;
  right: -30rpx;
}

.circle-2 {
  width: 180rpx;
  height: 180rpx;
  top: 80rpx;
  left: -60rpx;
}

.title-section {
  padding-top: 180rpx;
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

.input-group {
  margin-bottom: 24rpx;
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
  padding: 22rpx 26rpx;
  background: #f8f9fa;
  border-radius: 14rpx;
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
  margin-left: 16rpx;
  font-size: 28rpx;
  color: #333;
}

.placeholder {
  color: #bbb;
}

.agreement-section {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  margin-top: 20rpx;
  margin-bottom: 30rpx;
}

.agreement-text {
  font-size: 24rpx;
  color: #999;
  margin-left: 8rpx;
}

.agreement-link {
  font-size: 24rpx;
  color: #667eea;
}

.register-btn {
  width: 100%;
  height: 92rpx;
  line-height: 92rpx;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 46rpx;
  font-size: 32rpx;
  font-weight: 600;
  color: #fff;
  border: none;
  box-shadow: 0 8rpx 24rpx rgba(102, 126, 234, 0.4);
}

.register-btn::after {
  border: none;
}

.register-btn[disabled] {
  opacity: 0.6;
}

.login-section {
  display: flex;
  justify-content: center;
  align-items: center;
  margin-top: 30rpx;
}

.login-text {
  font-size: 26rpx;
  color: #999;
}

.login-link {
  font-size: 26rpx;
  color: #667eea;
  font-weight: 500;
  margin-left: 8rpx;
}

// 步骤指示器
.steps {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 40rpx;
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

// 发送验证码按钮
.send-code-btn {
  width: 100%;
  height: 80rpx;
  line-height: 80rpx;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 40rpx;
  font-size: 28rpx;
  font-weight: 500;
  color: #fff;
  border: none;
  margin-bottom: 30rpx;
}

.send-code-btn::after {
  border: none;
}

.send-code-btn[disabled] {
  opacity: 0.6;
}

// 下一步按钮
.next-btn {
  width: 100%;
  height: 80rpx;
  line-height: 80rpx;
  background: linear-gradient(135deg, #52c41a 0%, #389e0d 100%);
  border-radius: 40rpx;
  font-size: 28rpx;
  font-weight: 500;
  color: #fff;
  border: none;
  margin-top: 20rpx;
}

.next-btn::after {
  border: none;
}

.next-btn[disabled] {
  opacity: 0.6;
}

// 验证码输入区域
.code-input-section {
  margin-bottom: 30rpx;
}

.code-hint {
  display: block;
  text-align: center;
  font-size: 26rpx;
  color: #666;
  margin-bottom: 30rpx;
}

.code-input-group {
  display: flex;
  justify-content: center;
  gap: 16rpx;
  margin-bottom: 20rpx;
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
  background: #f8f9fa;
}

.code-input:focus {
  border-color: #667eea;
  box-shadow: 0 4rpx 12rpx rgba(102, 126, 234, 0.15);
  background: #fff;
}

.resend-section {
  text-align: center;
  margin-bottom: 20rpx;
}

.countdown-text {
  font-size: 26rpx;
  color: #999;
}

.resend-link {
  font-size: 26rpx;
  color: #667eea;
}
</style>







