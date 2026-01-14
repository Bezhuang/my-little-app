<script setup>
import { ref, onUnmounted, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, ArrowLeft } from '@element-plus/icons-vue'
import { BASE_URL } from '../../constant'
import { getAppName } from '../../composables/useAppConfig'

const router = useRouter()
const appName = ref('My Little App')

onMounted(async () => {
  appName.value = await getAppName()
})

const step = ref(1)
const loading = ref(false)
const email = ref('')
const code = ref('')
const codeArray = ref(['', '', '', '', '', ''])
const codeInputRefs = ref([])
const countdown = ref(0)
const newPassword = ref('')
const confirmPassword = ref('')
const showPassword = ref(false)
const showConfirmPassword = ref(false)

let countdownTimer = null

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
    ElMessage.warning('请输入邮箱')
    return
  }

  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  if (!emailRegex.test(email.value)) {
    ElMessage.warning('邮箱格式不正确')
    return
  }

  loading.value = true

  try {
    const response = await fetch(`${BASE_URL}/api/public/admin/forgot-password`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({ email: email.value })
    })

    const result = await response.json()

    if (result.success) {
      ElMessage.success('验证码已发送')
      step.value = 2
      startCountdown()
    } else {
      ElMessage.warning(result.message || '发送失败')
    }
  } catch (error) {
    ElMessage.error(error.message || '发送失败，请稍后重试')
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
const onCodeInput = (e, index) => {
  const value = e.target.value
  codeArray.value[index] = value
  code.value = codeArray.value.join('')

  if (value && index < 5) {
    codeInputRefs.value[index + 1]?.focus()
  }
}

// 验证码删除
const onCodeKeydown = (e, index) => {
  if (e.key === 'Backspace' && !codeArray.value[index] && index > 0) {
    codeInputRefs.value[index - 1]?.focus()
  }
}

// 验证验证码
const verifyCode = async () => {
  if (code.value.length < 6) {
    ElMessage.warning('请输入完整验证码')
    return
  }

  loading.value = true

  try {
    const response = await fetch(`${BASE_URL}/api/public/admin/verify-reset-code`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({ email: email.value, code: code.value })
    })

    const result = await response.json()

    if (result.success) {
      step.value = 3
    } else {
      ElMessage.warning(result.message || '验证码错误')
    }
  } catch (error) {
    ElMessage.error(error.message || '验证失败')
  } finally {
    loading.value = false
  }
}

// 重置密码
const resetPassword = async () => {
  if (!newPassword.value) {
    ElMessage.warning('请输入新密码')
    return
  }
  if (newPassword.value.length < 6) {
    ElMessage.warning('密码长度不能少于6位')
    return
  }
  if (newPassword.value !== confirmPassword.value) {
    ElMessage.warning('两次密码输入不一致')
    return
  }

  loading.value = true

  try {
    const response = await fetch(`${BASE_URL}/api/public/admin/reset-password`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        email: email.value,
        code: code.value,
        newPassword: newPassword.value
      })
    })

    const result = await response.json()

    if (result.success) {
      ElMessage.success('密码重置成功')
      setTimeout(() => {
        router.push('/login')
      }, 1500)
    } else {
      ElMessage.warning(result.message || '重置失败')
    }
  } catch (error) {
    ElMessage.error(error.message || '重置失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

// 返回登录
const goBack = () => {
  router.push('/login')
}

// 清理定时器
onUnmounted(() => {
  if (countdownTimer) {
    clearInterval(countdownTimer)
  }
})
</script>

<template>
  <div class="forgot-page">
    <div class="bg-decoration">
      <div class="circle circle-1"></div>
      <div class="circle circle-2"></div>
    </div>

    <div class="login-container">
      <!-- 左侧品牌区 -->
      <div class="brand-section">
        <div class="brand-content">
          <div class="brand-icon">
            <el-icon size="48"><Management /></el-icon>
          </div>
          <h1 class="brand-title">{{ appName }}</h1>
          <p class="brand-subtitle">后台管理系统</p>
          <div class="brand-features">
            <div class="feature-item">
              <el-icon><Check /></el-icon>
              <span>用户管理</span>
            </div>
            <div class="feature-item">
              <el-icon><Check /></el-icon>
              <span>内容发布</span>
            </div>
            <div class="feature-item">
              <el-icon><Check /></el-icon>
              <span>数据统计</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧表单区 -->
      <div class="form-section">
        <div class="form-card">
          <div class="form-header">
            <el-button class="back-btn" text @click="goBack">
              <el-icon><ArrowLeft /></el-icon>
              返回登录
            </el-button>
            <h2>忘记密码</h2>
            <p>输入管理员邮箱，重置密码</p>
          </div>

          <!-- 步骤指示器 -->
          <div class="steps">
            <div class="step" :class="{ active: step >= 1, done: step > 1 }">
              <div class="step-circle">1</div>
              <span class="step-text">验证邮箱</span>
            </div>
            <div class="step-line" :class="{ active: step > 1 }"></div>
            <div class="step" :class="{ active: step >= 2, done: step > 2 }">
              <div class="step-circle">2</div>
              <span class="step-text">输入验证码</span>
            </div>
            <div class="step-line" :class="{ active: step > 2 }"></div>
            <div class="step" :class="{ active: step >= 3 }">
              <div class="step-circle">3</div>
              <span class="step-text">重置密码</span>
            </div>
          </div>

          <!-- 步骤1：输入邮箱 -->
          <div class="step-content" v-if="step === 1">
            <el-form @submit.prevent="sendCode">
              <el-form-item>
                <el-input
                  v-model="email"
                  placeholder="请输入管理员邮箱"
                  size="large"
                  :prefix-icon="User"
                />
              </el-form-item>
              <el-form-item>
                <el-button
                  type="primary"
                  size="large"
                  :loading="loading"
                  @click="sendCode"
                  class="submit-btn"
                >
                  {{ loading ? '发送中...' : '发送验证码' }}
                </el-button>
              </el-form-item>
            </el-form>
          </div>

          <!-- 步骤2：输入验证码 -->
          <div class="step-content" v-if="step === 2">
            <div class="code-info">
              <span>验证码已发送至 </span>
              <span class="info-email">{{ maskEmail(email) }}</span>
            </div>

            <div class="code-input-group">
              <input
                v-for="(item, index) in 6"
                :key="index"
                class="code-input"
                type="text"
                maxlength="1"
                :value="codeArray[index]"
                @input="onCodeInput($event, index)"
                @keydown="onCodeKeydown($event, index)"
                :ref="el => codeInputRefs[index] = el"
              />
            </div>

            <div class="resend-section">
              <span v-if="countdown > 0" class="countdown-text">{{ countdown }}秒后可重新发送</span>
              <el-button v-else type="primary" text @click="sendCode">重新发送验证码</el-button>
            </div>

            <el-form-item>
              <el-button
                type="primary"
                size="large"
                :loading="loading"
                :disabled="code.length < 6"
                @click="verifyCode"
                class="submit-btn"
              >
                {{ loading ? '验证中...' : '验证' }}
              </el-button>
            </el-form-item>
          </div>

          <!-- 步骤3：重置密码 -->
          <div class="step-content" v-if="step === 3">
            <el-form @submit.prevent="resetPassword">
              <el-form-item>
                <el-input
                  v-model="newPassword"
                  :type="showPassword ? 'text' : 'password'"
                  placeholder="请输入新密码（至少6位）"
                  size="large"
                  :prefix-icon="Lock"
                  show-password
                />
              </el-form-item>
              <el-form-item>
                <el-input
                  v-model="confirmPassword"
                  :type="showConfirmPassword ? 'text' : 'password'"
                  placeholder="请再次输入新密码"
                  size="large"
                  :prefix-icon="Lock"
                  show-password
                />
              </el-form-item>
              <el-form-item>
                <el-button
                  type="primary"
                  size="large"
                  :loading="loading"
                  @click="resetPassword"
                  class="submit-btn"
                >
                  {{ loading ? '重置中...' : '重置密码' }}
                </el-button>
              </el-form-item>
            </el-form>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { Management, Check } from '@element-plus/icons-vue'

export default {
  components: {
    Management,
    Check
  }
}
</script>

<style scoped>
.forgot-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-darker);
  position: relative;
  overflow: hidden;
}

.bg-decoration {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.circle {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
}

.circle-1 {
  width: 400px;
  height: 400px;
  background: radial-gradient(circle, rgba(13, 148, 136, 0.15) 0%, transparent 70%);
  top: -100px;
  right: -100px;
}

.circle-2 {
  width: 300px;
  height: 300px;
  background: radial-gradient(circle, rgba(59, 130, 246, 0.1) 0%, transparent 70%);
  bottom: -50px;
  left: -50px;
}

.login-container {
  display: flex;
  width: 900px;
  max-width: 95vw;
  background: var(--bg-card);
  border-radius: 24px;
  overflow: hidden;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.5);
  border: 1px solid var(--border-color);
  position: relative;
  z-index: 1;
}

.brand-section {
  flex: 1;
  background: var(--gradient-primary);
  padding: 60px 40px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.brand-content {
  text-align: center;
  color: white;
}

.brand-icon {
  width: 80px;
  height: 80px;
  background: rgba(255, 255, 255, 0.15);
  border-radius: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 24px;
}

.brand-title {
  font-size: 28px;
  font-weight: 700;
  margin-bottom: 8px;
}

.brand-subtitle {
  font-size: 16px;
  opacity: 0.9;
  margin-bottom: 40px;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 0;
  font-size: 15px;
}

.feature-item .el-icon {
  width: 24px;
  height: 24px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.form-section {
  flex: 1;
  padding: 60px 40px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.form-card {
  width: 100%;
  max-width: 340px;
}

.form-header {
  margin-bottom: 32px;
}

.back-btn {
  color: var(--text-muted);
  margin-bottom: 16px;
}

.form-header h2 {
  font-size: 24px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 8px;
}

.form-header p {
  font-size: 14px;
  color: var(--text-muted);
}

.steps {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 32px;
}

.step {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.step-circle {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: var(--bg-darker);
  color: var(--text-muted);
  font-size: 14px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 6px;
}

.step.active .step-circle {
  background: var(--gradient-primary);
  color: white;
}

.step.done .step-circle {
  background: var(--success-color);
  color: white;
}

.step-text {
  font-size: 12px;
  color: var(--text-muted);
}

.step.active .step-text {
  color: var(--primary-color);
}

.step-line {
  width: 40px;
  height: 2px;
  background: var(--bg-darker);
  margin: 0 8px;
  margin-bottom: 20px;
}

.step-line.active {
  background: var(--gradient-primary);
}

.step-content {
  padding: 20px 0;
}

.submit-btn {
  width: 100%;
  height: 48px;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 500;
}

.code-info {
  text-align: center;
  margin-bottom: 24px;
  font-size: 14px;
  color: var(--text-muted);
}

.info-email {
  color: var(--primary-color);
  font-weight: 500;
}

.code-input-group {
  display: flex;
  justify-content: center;
  gap: 10px;
  margin-bottom: 24px;
}

.code-input {
  width: 44px;
  height: 52px;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  text-align: center;
  font-size: 20px;
  font-weight: 600;
  color: var(--text-primary);
  background: var(--bg-darker);
}

.code-input:focus {
  border-color: var(--primary-color);
  outline: none;
}

.resend-section {
  text-align: center;
  margin-bottom: 24px;
}

.countdown-text {
  font-size: 13px;
  color: var(--text-muted);
}

@media (max-width: 768px) {
  .login-container {
    flex-direction: column;
  }

  .brand-section {
    padding: 40px 24px;
  }

  .brand-features {
    display: none;
  }

  .form-section {
    padding: 40px 24px;
  }
}
</style>
