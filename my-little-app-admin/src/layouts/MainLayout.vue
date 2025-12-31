<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '../stores/user'
import { ElMessageBox, ElMessage } from 'element-plus'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const isCollapsed = ref(false)
const isDark = ref(false)
const isMobileMenuVisible = ref(false)
const sidebarWidth = computed(() => isCollapsed.value ? '64px' : '260px')

const menuItems = [
  { path: '/dashboard', title: '控制台', icon: 'Odometer' },
  { path: '/users', title: '用户管理', icon: 'User' },
  { path: '/thoughts', title: '想法管理', icon: 'ChatDotRound' },
  { path: '/images', title: '图片管理', icon: 'Picture' },
  { path: '/ai-config', title: 'AI 配置', icon: 'ChatDotRound' },
  { path: '/future-feature-1', title: '待开发功能 1', icon: 'Plus' },
  { path: '/future-feature-2', title: '待开发功能 2', icon: 'Plus' }
]

// 主题切换
const toggleTheme = () => {
  isDark.value = !isDark.value
  document.documentElement.classList.toggle('dark', isDark.value)
  localStorage.setItem('theme', isDark.value ? 'dark' : 'light')
}

// 初始化主题
onMounted(() => {
  const savedTheme = localStorage.getItem('theme')
  isDark.value = savedTheme === 'dark'
  document.documentElement.classList.toggle('dark', isDark.value)
})

const handleLogout = () => {
  ElMessageBox.confirm('确定要退出登录吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    userStore.logout()
    router.push('/login')
    ElMessage.success('已退出登录')
  }).catch(() => {})
}

const toggleSidebar = () => {
  isCollapsed.value = !isCollapsed.value
}

// 移动端菜单切换
const toggleMobileMenu = () => {
  isMobileMenuVisible.value = !isMobileMenuVisible.value
}

// 移动端菜单项点击
const handleMobileMenuClick = (path) => {
  isMobileMenuVisible.value = false
  router.push(path)
}
</script>

<template>
  <div class="main-layout">
    <!-- 移动端遮罩层 -->
    <div
      class="mobile-overlay"
      v-show="isMobileMenuVisible"
      @click="isMobileMenuVisible = false"
    ></div>

    <!-- 移动端顶部菜单按钮 -->
    <div class="mobile-header">
      <div class="mobile-header-left">
        <el-button text circle @click="toggleMobileMenu" class="menu-btn">
          <el-icon size="24"><Menu /></el-icon>
        </el-button>
        <span class="mobile-logo">管理后台</span>
      </div>
      <div class="mobile-header-right">
        <el-button text circle @click="toggleTheme" :title="isDark ? '切换日间模式' : '切换夜间模式'">
          <el-icon size="20">
            <Sunny v-if="!isDark" />
            <Moon v-else />
          </el-icon>
        </el-button>
        <el-button text circle @click="handleLogout">
          <el-icon size="20"><SwitchButton /></el-icon>
        </el-button>
      </div>
    </div>

    <!-- 移动端下拉菜单 -->
    <div class="mobile-drawer" :class="{ 'mobile-drawer-open': isMobileMenuVisible }">
      <div class="mobile-drawer-header">
        <el-icon size="24" color="#14b8a6"><Management /></el-icon>
        <span>管理后台</span>
        <el-button text circle @click="isMobileMenuVisible = false">
          <el-icon size="20"><Close /></el-icon>
        </el-button>
      </div>
      <el-menu
        :default-active="route.path"
        router
        class="mobile-menu"
        @select="handleMobileMenuClick"
      >
        <template v-for="item in menuItems" :key="item.path">
          <el-menu-item :index="item.path">
            <el-icon><component :is="item.icon" /></el-icon>
            <span>{{ item.title }}</span>
          </el-menu-item>
        </template>
      </el-menu>
      <div class="mobile-drawer-footer">
        <div class="user-info">
          <el-avatar :size="40" class="user-avatar">
            {{ userStore.userInfo?.username?.[0]?.toUpperCase() || 'A' }}
          </el-avatar>
          <div class="user-details">
            <div class="user-name">{{ userStore.userInfo?.username || '管理员' }}</div>
            <div class="user-role">超级管理员</div>
          </div>
        </div>
      </div>
    </div>

    <!-- PC端侧边栏 -->
    <aside class="sidebar" :style="{ width: sidebarWidth }">
      <div class="sidebar-header">
        <div class="logo">
          <el-icon size="28" color="#14b8a6"><Management /></el-icon>
          <span v-show="!isCollapsed" class="logo-text">管理后台</span>
        </div>
      </div>

      <el-menu
        :default-active="route.path"
        :collapse="isCollapsed"
        :collapse-transition="false"
        router
        class="sidebar-menu"
      >
        <template v-for="item in menuItems" :key="item.path">
          <el-sub-menu v-if="item.children" :index="item.path">
            <template #title>
              <el-icon><component :is="item.icon" /></el-icon>
              <span>{{ item.title }}</span>
            </template>
            <el-menu-item
              v-for="child in item.children"
              :key="child.path"
              :index="child.path"
            >
              {{ child.title }}
            </el-menu-item>
          </el-sub-menu>
          <el-menu-item v-else :index="item.path">
            <el-icon><component :is="item.icon" /></el-icon>
            <span>{{ item.title }}</span>
          </el-menu-item>
        </template>
      </el-menu>

      <div class="sidebar-footer">
        <div class="user-info" v-show="!isCollapsed">
          <el-avatar :size="36" class="user-avatar">
            {{ userStore.userInfo?.username?.[0]?.toUpperCase() || 'A' }}
          </el-avatar>
          <div class="user-details">
            <div class="user-name">{{ userStore.userInfo?.username || '管理员' }}</div>
            <div class="user-role">超级管理员</div>
          </div>
        </div>
        <el-button
          text
          circle
          @click="handleLogout"
          class="logout-btn"
        >
          <el-icon size="18"><SwitchButton /></el-icon>
        </el-button>
      </div>
    </aside>

    <!-- 主内容区 -->
    <div class="main-container" :style="{ marginLeft: sidebarWidth }">
      <!-- PC端顶部栏 -->
      <header class="main-header" :class="{ 'header-light': !isDark }">
        <div class="header-left">
          <el-button text circle @click="toggleSidebar" class="collapse-btn">
            <el-icon size="20">
              <Fold v-if="!isCollapsed" />
              <Expand v-else />
            </el-icon>
          </el-button>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item>{{ route.meta.title }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <!-- 主题切换 -->
          <el-button text circle class="header-action" @click="toggleTheme" :title="isDark ? '切换日间模式' : '切换夜间模式'">
            <el-icon size="20">
              <Sunny v-if="!isDark" />
              <Moon v-else />
            </el-icon>
          </el-button>
          <el-button text circle class="header-action">
            <el-icon size="20"><Bell /></el-icon>
          </el-button>
          <el-button text circle class="header-action">
            <el-icon size="20"><Setting /></el-icon>
          </el-button>
        </div>
      </header>

      <!-- 内容区 -->
      <main class="main-content">
        <router-view v-slot="{ Component }">
          <transition name="fade-slide" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </main>
    </div>
  </div>
</template>

<style scoped>
.main-layout {
  min-height: 100vh;
  background: var(--gradient-bg);
  overflow-x: hidden;
}

/* ==================== 移动端样式 ==================== */
.mobile-header {
  display: none;
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: 56px;
  background: #ffffff;
  border-bottom: 1px solid var(--border-color);
  padding: 0 12px;
  align-items: center;
  justify-content: space-between;
  z-index: 100;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.mobile-header-left,
.mobile-header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.mobile-logo {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
}

.menu-btn {
  color: var(--text-primary);
}

.mobile-overlay {
  display: none;
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 200;
}

.mobile-drawer {
  display: none;
  position: fixed;
  top: 0;
  left: -280px;
  width: 280px;
  height: 100vh;
  background: #ffffff;
  z-index: 201;
  transition: left 0.3s ease;
  flex-direction: column;
}

.mobile-drawer-open {
  left: 0;
}

.mobile-drawer-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  border-bottom: 1px solid var(--border-color);
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
}

.mobile-menu {
  flex: 1;
  padding: 12px;
}

.mobile-drawer-footer {
  padding: 16px;
  border-top: 1px solid var(--border-color);
}

.mobile-drawer-footer .user-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.mobile-drawer-footer .user-avatar {
  background: var(--gradient-primary);
}

.mobile-drawer-footer .user-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
}

.mobile-drawer-footer .user-role {
  font-size: 12px;
  color: var(--text-muted);
}

/* ==================== PC端侧边栏样式 ==================== */
.sidebar {
  position: fixed;
  top: 0;
  left: 0;
  height: 100vh;
  background: var(--bg-darker);
  border-right: 1px solid var(--border-color);
  display: flex;
  flex-direction: column;
  transition: width 0.3s ease;
  z-index: 100;
  overflow: hidden;
}

.sidebar-header {
  padding: 20px;
  border-bottom: 1px solid var(--border-color);
}

.logo {
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo-text {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
  white-space: nowrap;
}

.sidebar-menu {
  flex: 1;
  padding: 12px;
  overflow-y: auto;
}

.sidebar-footer {
  padding: 16px;
  border-top: 1px solid var(--border-color);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
  overflow: hidden;
}

.user-avatar {
  background: var(--gradient-primary);
  flex-shrink: 0;
}

.user-details {
  overflow: hidden;
}

.user-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.user-role {
  font-size: 12px;
  color: var(--text-muted);
}

.logout-btn {
  color: var(--text-muted);
}

.logout-btn:hover {
  color: var(--danger-color);
}

/* ==================== 主容器样式 ==================== */
.main-container {
  min-height: 100vh;
  transition: margin-left 0.3s ease;
  display: flex;
  flex-direction: column;
}

.main-header {
  height: 64px;
  background: rgba(15, 23, 42, 0.8);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid var(--border-color);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  position: sticky;
  top: 0;
  z-index: 50;
  transition: background-color 0.3s ease;
}

/* 日间模式顶栏样式 */
.main-header.header-light {
  background: rgba(255, 255, 255, 0.9);
  border-bottom: 1px solid #e2e8f0;
}

.main-header.header-light .collapse-btn {
  color: var(--text-secondary);
}

.main-header.header-light .collapse-btn:hover {
  color: var(--text-primary);
}

.main-header.header-light .header-action {
  color: var(--text-secondary);
}

.main-header.header-light .header-action:hover {
  color: var(--primary-color);
}

.main-header.header-light :deep(.el-breadcrumb__inner) {
  color: var(--text-muted);
}

.main-header.header-light :deep(.el-breadcrumb__item:last-child .el-breadcrumb__inner) {
  color: var(--text-primary);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.collapse-btn {
  color: var(--text-secondary);
}

.collapse-btn:hover {
  color: var(--text-primary);
}

.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-action {
  color: var(--text-secondary);
}

.header-action:hover {
  color: var(--primary-color);
}

.main-content {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
}

:deep(.el-breadcrumb__item) {
  .el-breadcrumb__inner {
    color: var(--text-muted);
  }

  &:last-child .el-breadcrumb__inner {
    color: var(--text-primary);
  }
}

:deep(.el-breadcrumb__separator) {
  color: var(--text-muted);
}

/* ==================== 响应式样式 ==================== */
@media (max-width: 768px) {
  /* 显示移动端元素 */
  .mobile-header,
  .mobile-overlay,
  .mobile-drawer {
    display: flex;
  }

  /* 隐藏PC端元素 */
  .sidebar {
    display: none;
  }

  .main-container {
    margin-left: 0 !important;
    width: 100% !important;
    max-width: 100% !important;
    overflow-x: hidden;
  }

  .main-header {
    display: none;
  }

  .main-content {
    padding: 12px;
    padding-top: 72px;
    width: 100%;
    max-width: 100%;
    overflow-x: hidden;
  }
}
</style>

