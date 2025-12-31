<script setup>
import { ref, onMounted } from 'vue'
import { dashboardApi } from '../../utils/api'

const loading = ref(true)
const stats = ref([
  { title: '用户总数', value: '0', icon: 'User', color: '#3b82f6' },
  { title: '想法数量', value: '0', icon: 'ChatDotRound', color: '#8b5cf6' },
  { title: '图片数量', value: '0', icon: 'Picture', color: '#22c55e' }
])

const recentUsers = ref([])
const latestThoughts = ref([])

// 格式化日期
const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return `${date.getMonth() + 1}-${date.getDate()}`
}

// 获取统计数据
const fetchStats = async () => {
  try {
    const res = await dashboardApi.getStats()
    if (res && res.data) {
      stats.value[0].value = res.data.userCount?.toLocaleString() || '0'
      stats.value[1].value = res.data.thoughtCount?.toLocaleString() || '0'
      stats.value[2].value = res.data.imageCount?.toLocaleString() || '0'
    }
  } catch (error) {
    console.error('获取统计数据失败:', error)
  }
}

// 获取最新用户
const fetchLatestUsers = async () => {
  try {
    const res = await dashboardApi.getLatestUsers()
    if (res && res.data) {
      recentUsers.value = res.data.map(user => ({
        id: user.id,
        name: user.username,
        email: user.email || '-',
        joinDate: formatDate(user.createTime)
      }))
    }
  } catch (error) {
    console.error('获取最新用户失败:', error)
  }
}

// 获取最新想法
const fetchLatestThoughts = async () => {
  try {
    const res = await dashboardApi.getLatestThoughts()
    if (res && res.data) {
      latestThoughts.value = res.data.map(thought => ({
        id: thought.id,
        title: thought.content ? thought.content.substring(0, 30) + (thought.content.length > 30 ? '...' : '') : '-',
        author: thought.adminUsername || '未知',
        date: formatDate(thought.createTime),
        status: thought.visibility === 1 ? 'published' : 'private'
      }))
    }
  } catch (error) {
    console.error('获取最新想法失败:', error)
  }
}

onMounted(async () => {
  loading.value = true
  try {
    await Promise.all([
      fetchStats(),
      fetchLatestUsers(),
      fetchLatestThoughts()
    ])
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="dashboard">
    <div class="page-header">
      <h1 class="page-title">控制台</h1>
      <p class="page-subtitle">欢迎回来，查看您的数据概览</p>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-grid">
      <div
        v-for="stat in stats"
        :key="stat.title"
        class="stat-card"
      >
        <div class="stat-icon" :style="{ background: `${stat.color}20`, color: stat.color }">
          <el-icon size="24"><component :is="stat.icon" /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ stat.value }}</div>
          <div class="stat-title">{{ stat.title }}</div>
        </div>
      </div>
    </div>

    <!-- 主要内容区 -->
    <div class="dashboard-grid">
      <!-- 最新想法 -->
      <div class="dashboard-card thoughts-card">
        <div class="card-header">
          <h3>最新想法</h3>
          <router-link to="/thoughts" class="view-all">
            查看全部 <el-icon><ArrowRight /></el-icon>
          </router-link>
        </div>
        <div class="thoughts-list">
          <div
            v-for="thought in latestThoughts"
            :key="thought.id"
            class="thought-item"
          >
            <div class="thought-info">
              <div class="thought-title">{{ thought.title }}</div>
              <div class="thought-meta">
                <span>{{ thought.author }}</span>
                <span class="separator">·</span>
                <span>{{ thought.date }}</span>
              </div>
            </div>
            <el-tag
              :type="thought.status === 'published' ? 'success' : 'info'"
              size="small"
            >
              {{ thought.status === 'published' ? '公开' : '私密' }}
            </el-tag>
          </div>
          <div v-if="latestThoughts.length === 0" class="empty-tip">
            暂无想法
          </div>
        </div>
      </div>

      <!-- 侧边栏 -->
      <div class="sidebar-grid">
        <!-- 最新用户 -->
        <div class="dashboard-card users-card">
          <div class="card-header">
            <h3>最新用户</h3>
            <router-link to="/users" class="view-all">
              查看全部 <el-icon><ArrowRight /></el-icon>
            </router-link>
          </div>
          <div class="users-list">
            <div
              v-for="user in recentUsers"
              :key="user.id"
              class="user-item"
            >
              <el-avatar :size="40" class="user-avatar">
                {{ user.name[0]?.toUpperCase() || 'U' }}
              </el-avatar>
              <div class="user-info">
                <div class="user-name">{{ user.name }}</div>
                <div class="user-email">{{ user.email }}</div>
              </div>
              <div class="user-date">{{ user.joinDate }}</div>
            </div>
            <div v-if="recentUsers.length === 0" class="empty-tip">
              暂无用户
            </div>
          </div>
        </div>

        <!-- 快捷操作 -->
        <div class="dashboard-card quick-actions">
          <div class="card-header">
            <h3>快捷操作</h3>
          </div>
          <div class="actions-grid">
            <router-link to="/thoughts" class="action-item">
              <el-icon size="28"><ChatDotRound /></el-icon>
              <span>想法管理</span>
            </router-link>
            <router-link to="/images" class="action-item">
              <el-icon size="28"><Picture /></el-icon>
              <span>图片管理</span>
            </router-link>
            <router-link to="/users" class="action-item">
              <el-icon size="28"><User /></el-icon>
              <span>用户管理</span>
            </router-link>
            <router-link to="/ai-config" class="action-item">
              <el-icon size="28"><ChatDotRound /></el-icon>
              <span>AI 配置</span>
            </router-link>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.dashboard {
  max-width: 1400px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 32px;
}

.page-title {
  font-size: 28px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 4px;
}

.page-subtitle {
  font-size: 14px;
  color: var(--text-muted);
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
  margin-bottom: 24px;
}

.stat-card {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: 16px;
  padding: 24px;
  display: flex;
  align-items: center;
  gap: 16px;
  transition: all 0.3s ease;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.2);
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.stat-content {
  flex: 1;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: var(--text-primary);
}

.stat-title {
  font-size: 13px;
  color: var(--text-muted);
  margin-top: 2px;
}

.dashboard-grid {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 24px;
}

.sidebar-grid {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.dashboard-card {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: 16px;
  padding: 24px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.card-header h3 {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
}

.view-all {
  font-size: 13px;
  color: var(--primary-color);
  text-decoration: none;
  display: flex;
  align-items: center;
  gap: 4px;
}

.view-all:hover {
  text-decoration: underline;
}

.thoughts-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.thought-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  background: var(--bg-darker);
  border-radius: 12px;
  transition: background 0.2s;
}

.thought-item:hover {
  background: var(--bg-card-hover);
}

.thought-title {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
  margin-bottom: 4px;
}

.thought-meta {
  font-size: 12px;
  color: var(--text-muted);
}

.thought-meta .separator {
  margin: 0 6px;
}

.users-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.user-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px;
  border-radius: 8px;
  transition: background 0.2s;
}

.user-item:hover {
  background: var(--bg-darker);
}

.user-avatar {
  background: var(--gradient-primary);
  flex-shrink: 0;
}

.user-info {
  flex: 1;
  min-width: 0;
}

.user-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.user-email {
  font-size: 12px;
  color: var(--text-muted);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.user-date {
  font-size: 12px;
  color: var(--text-muted);
  flex-shrink: 0;
}

.quick-actions {
  flex: 1;
}

.actions-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.action-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 20px 16px;
  background: var(--bg-darker);
  border-radius: 12px;
  color: var(--text-secondary);
  text-decoration: none;
  transition: all 0.2s;
}

.action-item:hover {
  background: var(--gradient-primary);
  color: white;
  transform: translateY(-2px);
}

.action-item span {
  font-size: 13px;
  font-weight: 500;
}

.empty-tip {
  text-align: center;
  padding: 20px;
  color: var(--text-muted);
  font-size: 14px;
}

@media (max-width: 1200px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .dashboard-grid {
    grid-template-columns: 1fr;
  }

  .sidebar-grid {
    flex-direction: row;
  }

  .sidebar-grid > .dashboard-card {
    flex: 1;
  }
}

@media (max-width: 768px) {
  .stats-grid {
    grid-template-columns: 1fr;
    gap: 12px;
  }

  .sidebar-grid {
    flex-direction: column;
  }

  /* 移动端卡片样式 */
  .dashboard-card {
    padding: 16px;
    border-radius: 12px;
  }

  .stat-card {
    padding: 16px;
  }

  .stat-icon {
    width: 44px;
    height: 44px;
  }

  .stat-value {
    font-size: 24px;
  }

  .page-header {
    margin-bottom: 20px;
  }

  .page-title {
    font-size: 22px;
  }

  .card-header {
    margin-bottom: 12px;
  }

  .card-header h3 {
    font-size: 14px;
  }

  .thought-item {
    padding: 12px;
  }

  .thought-title {
    font-size: 13px;
  }

  .user-item {
    padding: 6px;
  }

  .actions-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 10px;
  }

  .action-item {
    padding: 16px 12px;
  }

  .action-item span {
    font-size: 12px;
  }
}
</style>
