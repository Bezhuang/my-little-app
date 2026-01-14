<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

const router = useRouter()

const loading = ref(false)
const setupForm = reactive({
  appName: 'My Little App',
  username: '',
  email: '',
  password: '',
  confirmPassword: ''
})

const rules = {
  appName: [
    { required: true, message: '请输入应用名称', trigger: 'blur' },
    { min: 2, max: 50, message: '应用名称长度2-50位', trigger: 'blur' }
  ],
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度3-20位', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度至少6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== setupForm.password) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

const formRef = ref(null)

const checkSetupStatus = async () => {
  try {
    const baseUrl = import.meta.env.VITE_API_BASE_URL || ''
    const res = await fetch(`${baseUrl}/api/setup/status`)
    const data = await res.json()

    if (data.success && !data.data.needsSetup) {
      // 已有管理员，跳转到登录页
      router.push('/login')
    }
  } catch (error) {
    console.error('检查初始化状态失败:', error)
    // 如果请求失败，假设需要初始化，继续显示设置页面
  }
}

const handleSetup = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true

      try {
        const baseUrl = import.meta.env.VITE_API_BASE_URL || ''
        const res = await fetch(`${baseUrl}/api/setup/admin`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify({
            appName: setupForm.appName,
            username: setupForm.username,
            email: setupForm.email,
            password: setupForm.password
          })
        })

        const data = await res.json()

        if (data.success) {
          ElMessage.success('初始化成功，请登录')
          router.push('/login')
        } else {
          ElMessage.error(data.message || '初始化失败')
        }
      } catch (error) {
        ElMessage.error(error.message || '初始化失败')
      } finally {
        loading.value = false
      }
    }
  })
}

onMounted(() => {
  checkSetupStatus()
})
</script>

<template>
  <div class="setup-page">
    <!-- 背景装饰 -->
    <div class="bg-decoration">
      <div class="circle circle-1"></div>
      <div class="circle circle-2"></div>
      <div class="circle circle-3"></div>
      <div class="grid-pattern"></div>
    </div>

    <div class="setup-container">
      <!-- 左侧品牌区 -->
      <div class="brand-section">
        <div class="brand-content">
          <div class="brand-icon">
            <el-icon size="48"><Setting /></el-icon>
          </div>
          <h1 class="brand-title">系统初始化</h1>
          <p class="brand-subtitle">{{ setupForm.appName }}</p>
          <div class="setup-info">
            <div class="info-item">
              <el-icon><Check /></el-icon>
              <span>设置管理员账号</span>
            </div>
            <div class="info-item">
              <el-icon><Check /></el-icon>
              <span>完成系统配置</span>
            </div>
            <div class="info-item">
              <el-icon><Check /></el-icon>
              <span>开始使用系统</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧设置区 -->
      <div class="setup-section">
        <div class="setup-card">
          <div class="setup-header">
            <h2>创建管理员</h2>
            <p>首次部署，请设置系统管理员信息</p>
          </div>

          <el-form
            ref="formRef"
            :model="setupForm"
            :rules="rules"
            class="setup-form"
          >
            <el-form-item prop="appName">
              <el-input
                v-model="setupForm.appName"
                placeholder="给你的应用起个名字"
                size="large"
                :prefix-icon="Setting"
              />
            </el-form-item>

            <el-form-item prop="username">
              <el-input
                v-model="setupForm.username"
                placeholder="用户名"
                size="large"
                :prefix-icon="User"
              />
            </el-form-item>

            <el-form-item prop="email">
              <el-input
                v-model="setupForm.email"
                placeholder="邮箱"
                size="large"
                :prefix-icon="Message"
              />
            </el-form-item>

            <el-form-item prop="password">
              <el-input
                v-model="setupForm.password"
                type="password"
                placeholder="密码（至少6位）"
                size="large"
                :prefix-icon="Lock"
                show-password
              />
            </el-form-item>

            <el-form-item prop="confirmPassword">
              <el-input
                v-model="setupForm.confirmPassword"
                type="password"
                placeholder="确认密码"
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
                @click="handleSetup"
                class="setup-btn"
              >
                {{ loading ? '创建中...' : '创 建 管 理 员' }}
              </el-button>
            </el-form-item>
          </el-form>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { User, Lock, Message, Setting, Check } from '@element-plus/icons-vue'

export default {
  components: {
    User,
    Lock,
    Message,
    Setting,
    Check
  }
}
</script>

<style scoped>
.setup-page {
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
  width: 600px;
  height: 600px;
  background: radial-gradient(circle, rgba(13, 148, 136, 0.15) 0%, transparent 70%);
  top: -200px;
  right: -100px;
}

.circle-2 {
  width: 400px;
  height: 400px;
  background: radial-gradient(circle, rgba(59, 130, 246, 0.1) 0%, transparent 70%);
  bottom: -100px;
  left: -100px;
}

.circle-3 {
  width: 300px;
  height: 300px;
  background: radial-gradient(circle, rgba(139, 92, 246, 0.1) 0%, transparent 70%);
  top: 50%;
  left: 30%;
}

.grid-pattern {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.02) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.02) 1px, transparent 1px);
  background-size: 50px 50px;
}

.setup-container {
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
  position: relative;
  overflow: hidden;
}

.brand-section::before {
  content: '';
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 30% 20%, rgba(255, 255, 255, 0.1) 0%, transparent 50%),
    radial-gradient(circle at 70% 80%, rgba(0, 0, 0, 0.1) 0%, transparent 50%);
}

.brand-content {
  position: relative;
  z-index: 1;
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
  backdrop-filter: blur(10px);
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

.setup-info {
  text-align: left;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 0;
  font-size: 15px;
}

.info-item .el-icon {
  width: 24px;
  height: 24px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.setup-section {
  flex: 1;
  padding: 60px 40px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.setup-card {
  width: 100%;
  max-width: 320px;
}

.setup-header {
  text-align: center;
  margin-bottom: 32px;
}

.setup-header h2 {
  font-size: 24px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 8px;
}

.setup-header p {
  font-size: 14px;
  color: var(--text-muted);
}

.setup-form {
  margin-bottom: 24px;
}

.setup-form :deep(.el-form-item) {
  margin-bottom: 20px;
}

.setup-form :deep(.el-input__wrapper) {
  padding: 0 16px;
  height: 48px;
  border-radius: 12px;
}

.setup-btn {
  width: 100%;
  height: 48px;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 500;
}

@media (max-width: 768px) {
  .setup-container {
    flex-direction: column;
  }

  .brand-section {
    padding: 40px 24px;
  }

  .setup-info {
    display: none;
  }

  .setup-section {
    padding: 40px 24px;
  }
}
</style>
