import { ref } from 'vue'
import { BASE_URL } from '../constant'

const appName = ref('My Little App')
const isLoaded = ref(false)

/**
 * 获取应用名称
 */
export const getAppName = async () => {
  if (isLoaded.value) {
    return appName.value
  }

  try {
    const res = await fetch(`${BASE_URL}/api/setup/app-name`)
    const data = await res.json()
    if (data.success) {
      appName.value = data.data
    }
  } catch (error) {
    console.error('获取应用名称失败:', error)
  } finally {
    isLoaded.value = true
  }

  return appName.value
}

/**
 * 设置页面标题
 */
export const setPageTitle = (pageName) => {
  document.title = `${appName.value} - ${pageName}`
}

/**
 * 更新页面 title 标签内容
 */
export const updateDocumentTitle = () => {
  const titleEl = document.querySelector('title')
  if (titleEl) {
    titleEl.textContent = `${appName.value} - 管理后台`
  }
}

export { appName }
