import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../stores/user'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/login/index.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/forgot-password',
    name: 'ForgotPassword',
    component: () => import('../views/forgot-password/index.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/setup',
    name: 'Setup',
    component: () => import('../views/setup/index.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/',
    component: () => import('../layouts/MainLayout.vue'),
    redirect: '/dashboard',
    meta: { requiresAuth: true },
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('../views/dashboard/index.vue'),
        meta: { title: '控制台', icon: 'Odometer' }
      },
      {
        path: 'users',
        name: 'Users',
        component: () => import('../views/users/index.vue'),
        meta: { title: '用户管理', icon: 'User' }
      },
      {
        path: 'thoughts',
        name: 'Thoughts',
        component: () => import('../views/thoughts/index.vue'),
        meta: { title: '想法管理', icon: 'ChatDotRound' }
      },
      {
        path: 'images',
        name: 'Images',
        component: () => import('../views/images/index.vue'),
        meta: { title: '图片管理', icon: 'Picture' }
      },
      {
        path: 'ai-config',
        name: 'AiConfig',
        component: () => import('../views/ai-config/index.vue'),
        meta: { title: 'AI 配置', icon: 'ChatDotRound' }
      },
      {
        path: 'future-feature-1',
        name: 'FutureFeature1',
        component: () => import('../views/future/index.vue'),
        meta: { title: '待开发功能 1', icon: 'Plus' }
      },
      {
        path: 'future-feature-2',
        name: 'FutureFeature2',
        component: () => import('../views/future/index.vue'),
        meta: { title: '待开发功能 2', icon: 'Plus' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 检查是否需要初始化
const checkNeedsSetup = async () => {
  try {
    const baseUrl = import.meta.env.VITE_API_BASE_URL || ''
    const res = await fetch(`${baseUrl}/api/setup/status`)
    const data = await res.json()
    if (data.success && data.data.needsSetup) {
      return true
    }
  } catch (error) {
    console.error('检查初始化状态失败:', error)
  }
  return false
}

// 路由守卫
router.beforeEach(async (to, from, next) => {
  const userStore = useUserStore()

  // 如果访问 setup 页面，直接放行
  if (to.path === '/setup') {
    next()
    return
  }

  // 如果未登录且不是登录/忘记密码页面，检查是否需要初始化
  if (to.meta.requiresAuth !== false && !userStore.isLoggedIn) {
    // 先检查是否需要初始化
    const needsSetup = await checkNeedsSetup()
    if (needsSetup) {
      next('/setup')
      return
    }
    next('/login')
  } else if (to.path === '/login' && userStore.isLoggedIn) {
    next('/dashboard')
  } else {
    next()
  }
})

export default router

