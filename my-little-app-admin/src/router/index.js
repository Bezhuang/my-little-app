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

// 路由守卫
router.beforeEach((to, from, next) => {
  const userStore = useUserStore()

  if (to.meta.requiresAuth !== false && !userStore.isLoggedIn) {
    next('/login')
  } else if (to.path === '/login' && userStore.isLoggedIn) {
    next('/dashboard')
  } else {
    next()
  }
})

export default router

