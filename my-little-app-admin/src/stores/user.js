import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useUserStore = defineStore('user', () => {
  const userInfo = ref(JSON.parse(localStorage.getItem('admin_user') || 'null'))

  const isLoggedIn = computed(() => !!userInfo.value)

  function setUserInfo(info) {
    userInfo.value = info
    localStorage.setItem('admin_user', JSON.stringify(info))
  }

  function login(info) {
    setUserInfo(info)
  }

  function logout() {
    userInfo.value = null
    localStorage.removeItem('admin_user')
  }

  function getId() {
    return userInfo.value?.id || null
  }

  function getUsername() {
    return userInfo.value?.username || ''
  }

  function isSuperAdmin() {
    return userInfo.value?.role === 'super_admin'
  }

  return {
    userInfo,
    isLoggedIn,
    login,
    logout,
    setUserInfo,
    getId,
    getUsername,
    isSuperAdmin
  }
})

