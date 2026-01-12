<template>
  <view class="ai-chat-page">
    <!-- 顶部栏 -->
    <view class="chat-header">
      <view class="header-left">
        <text class="chat-title">Bezhuang AI</text>
        <text class="chat-subtitle">智能聊天助手</text>
      </view>
    </view>

    <!-- 消息区域 -->
    <scroll-view
      class="messages-container"
      scroll-y
      :lower-threshold="50"
      :scroll-into-view="scrollIntoView"
      @scroll="onScroll"
    >
      <!-- 顶部占位（填满剩余空间） -->
      <view class="flex-placeholder"></view>

      <!-- 时间戳 -->
      <view v-if="showTimeStamp" class="time-stamp">
        <text>{{ lastMessageTime }}</text>
      </view>

      <!-- 欢迎消息 -->
      <view v-if="messages.length === 0" class="welcome-message">
        <view class="welcome-content">
          <text class="welcome-title">你好，我是 Bezhuang AI</text>
          <text class="welcome-desc">我可以进行多轮对话，支持开启深度思考和联网搜索模式。有什么我可以帮你的吗？</text>
        </view>
      </view>

      <!-- 消息项 -->
      <view
        v-for="(msg, index) in messages"
        :key="index"
        :class="['message-item', msg.role === 0 ? 'user' : 'ai']"
        :id="'msg-' + index"
      >
        <view class="message-avatar">
          <text class="avatar-icon">{{ msg.role === 0 ? '👤' : '🤖' }}</text>
        </view>
        <view class="message-content">
          <!-- 每轮思考过程（可折叠） -->
          <view v-if="msg.thinkingRounds && msg.thinkingRounds.length > 0">
            <view
              v-for="(thinking, tIndex) in msg.thinkingRounds"
              :key="tIndex"
              class="thinking-box"
            >
              <view class="thinking-header" @click="toggleThinkingRound(index, tIndex)">
                <text class="thinking-icon">💭</text>
                <text class="thinking-title">第 {{ tIndex + 1 }} 轮思考</text>
                <text class="thinking-text">{{ msg.expandedRounds && msg.expandedRounds[tIndex] ? '收起' : '展开' }}</text>
                <text class="thinking-arrow" :class="{ expanded: msg.expandedRounds && msg.expandedRounds[tIndex] }">▼</text>
              </view>
              <view v-if="msg.expandedRounds && msg.expandedRounds[tIndex]" class="thinking-content">
                <text user-select>{{ thinking }}</text>
              </view>
            </view>
          </view>
          <!-- 用户消息折叠显示（过长时） -->
          <view v-if="msg.role === 0 && msg.content.length > 100">
            <view class="message-text" @click="toggleUserMessage(index)">
              <view v-if="!msg.messageExpanded">
                <text>{{ msg.content.substring(0, 100) }}...</text>
                <text class="expand-text">（展开）</text>
              </view>
              <view v-else>
                <text user-select>{{ msg.content }}</text>
                <text class="collapse-text">（收起）</text>
              </view>
            </view>
          </view>
          <!-- 用户消息正常显示 -->
          <view v-else-if="msg.role === 0" class="message-text">
            <text user-select>{{ msg.content }}</text>
          </view>
          <!-- AI 消息显示 -->
          <view v-else-if="msg.role === 1 && msg.content" class="message-text">
            <text user-select>{{ msg.content }}</text>
          </view>
          <!-- 搜索参考链接（可折叠） -->
          <view v-if="msg.searchLinks && msg.searchLinks.length > 0" class="search-links">
            <view class="links-header" @click="toggleSearchLinks(index)">
              <text class="links-icon">🔗</text>
              <text class="links-title">参考链接</text>
              <text class="links-count">({{ msg.searchLinks.length }})</text>
              <text class="links-arrow" :class="{ expanded: msg.searchLinksExpanded }">▼</text>
            </view>
            <view v-if="msg.searchLinksExpanded" class="links-list">
              <view
                v-for="(link, lIndex) in msg.searchLinks"
                :key="lIndex"
                class="link-item"
                @click="openLink(link.url)"
              >
                <text class="link-index">{{ lIndex + 1 }}.</text>
                <text class="link-title">{{ link.title || link.url }}</text>
                <text class="link-arrow">›</text>
              </view>
            </view>
          </view>
        </view>
      </view>

      <!-- AI 思考中动画 -->
      <view v-if="aiThinking" class="thinking-indicator" id="thinking">
        <text class="thinking-text">{{ aiAction === '联网搜索' ? '联网搜索中...' : '思考中...' }}</text>
        <view class="thinking-dots">
          <view class="thinking-dot"></view>
          <view class="thinking-dot"></view>
          <view class="thinking-dot"></view>
        </view>
      </view>
    </scroll-view>

    <!-- 输入区域 - 固定在底部导航栏上方 -->
    <view class="input-area">
      <!-- 模型选择器 -->
      <view class="model-selector-row">
        <view class="model-selector" @click="showModelSelector = !showModelSelector">
          <text class="model-name">切换模型：{{ currentModel.name }}</text>
          <text class="model-arrow">▼</text>
        </view>
        <!-- 模型选择下拉框 -->
        <view v-if="showModelSelector" class="model-dropdown" @click="showModelSelector = false">
          <view class="model-dropdown-content" @click.stop>
            <view
              v-for="model in MODELS"
              :key="model.id"
              class="model-option"
              :class="{ active: currentModel.id === model.id, disabled: model.requiresAuth && !isLoggedIn }"
              @click="selectModel(model)"
            >
              <view class="model-info">
                <text class="model-option-name">{{ model.name }}</text>
                <text class="model-provider">{{ model.provider }}</text>
              </view>
              <view class="model-badges">
                <text v-if="model.requiresAuth && !isLoggedIn" class="badge auth">需登录</text>
              </view>
              <uni-icons v-if="currentModel.id === model.id" type="checkmark" size="16" color="#667eea"></uni-icons>
            </view>
          </view>
        </view>
      </view>

      <!-- 深度思考、联网搜索和清空按钮行 -->
      <view class="deep-think-row">
        <view
          class="deep-think-btn"
          :class="{ active: enableDeepThink }"
          @click="toggleDeepThink"
        >
          <text class="deep-think-icon">🧠</text>
          <text class="deep-think-label">深度思考</text>
        </view>
        <!-- 联网搜索按钮 -->
        <view
          class="web-search-btn"
          :class="{ active: enableWebSearch }"
          @click="toggleWebSearch"
        >
          <text class="web-search-icon">🌐</text>
          <text class="web-search-label">联网搜索</text>
        </view>
        <!-- 清空消息按钮 -->
        <view class="clear-btn" @click="clearConversation">
          <text class="clear-icon">🗑️</text>
          <text class="clear-label">清空对话</text>
        </view>
        <!-- 配额显示 -->
        <view class="quota-info" v-if="tokensRemaining !== null">
          <text class="quota-token">剩余Token：{{ tokensRemaining }}</text>
          <text class="quota-search">搜索：{{ searchRemaining }}</text>
        </view>
      </view>

      <view class="input-controls">
        <!-- 输入框 -->
        <textarea
          class="message-input"
          v-model="inputMessage"
          placeholder="输入消息..."
          :maxlength="-1"
          :fixed="true"
          @confirm="sendMessage"
        />

        <!-- 发送按钮 -->
        <view
          class="send-btn"
          :class="{ disabled: !inputMessage.trim() || sending || aiThinking }"
          @click="sendMessage"
        >
          <text class="send-text">发送</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted, nextTick, watch } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import userStore from '@/store/user'
import { createConversation, getAllConversations, addMessageToConversation, clearAllConversations } from '@/utils/indexedDB'
import { BASE_URL } from '@/constant'

// 模型列表
const MODELS = [
  { id: 'siliconflow', name: '基础模型', provider: 'Qwen3-8B', requiresAuth: false, hasVision: false, hasDeepThink: true, hasWebSearch: false },
  { id: 'deepseek', name: '高级模型', provider: 'DeepSeek-V3.2', requiresAuth: true, hasVision: false, hasDeepThink: true, hasWebSearch: true }
]

// 状态
const messages = ref([])
const inputMessage = ref('')
const sending = ref(false)
const aiThinking = ref(false)
const lastMessageTime = ref('')
const showTimeStamp = ref(false)
const enableDeepThink = ref(false) // 深度思考开关
const enableWebSearch = ref(false) // 联网搜索开关
const currentConversationId = ref(null) // 当前对话ID
const initialized = ref(false) // 标记是否已初始化
const tokensRemaining = ref(null) // 剩余Token数
const searchRemaining = ref(null) // 剩余搜索次数
const currentModel = ref(MODELS[0]) // 当前选中的模型，默认 SiliconFlow
const showModelSelector = ref(false) // 是否显示模型选择器
const aiAction = ref('') // 当前AI动作：'思考' 或 '联网搜索'
const scrollIntoView = ref('') // 滚动到指定元素
const scrollForce = ref(0) // 强制滚动计数器
const userScrolling = ref(false) // 用户是否正在手动滚动
let scrollTimeout = null // 用户滚动的定时器

// 监听用户手动滚动
const onScroll = (e) => {
  // 标记用户正在手动滚动
  userScrolling.value = true
  // 清除之前的定时器
  if (scrollTimeout) {
    clearTimeout(scrollTimeout)
  }
  // 2秒后认为用户停止滚动
  scrollTimeout = setTimeout(() => {
    userScrolling.value = false
  }, 2000)
}

// 监听消息变化，自动滚动到底部
watch(messages, () => {
  if (userScrolling.value) {
    return // 用户正在手动滚动，不打断
  }
  nextTick(() => {
    uni.createSelectorQuery()
      .select('.messages-container')
      .boundingClientRect((rect) => {
        if (rect) {
          // 设置滚动到底部
          uni.createSelectorQuery()
            .select('.messages-container')
            .scrollOffset((scrollRect) => {
              if (scrollRect && rect.height) {
                // 滚动到总高度减去可视高度
                uni.pageScrollTo({
                  scrollTop: scrollRect.scrollHeight - rect.height + 100,
                  duration: 100
                })
              }
            })
            .exec()
        }
      })
      .exec()
  })
}, { deep: true })

// 监听模型变化
watch(currentModel, (newModel, oldModel) => {
  if (oldModel && newModel.id !== oldModel.id) {
    // 切换模型时重置功能开关
    enableDeepThink.value = false
    enableWebSearch.value = false
    uni.showToast({ title: '已切换模型', icon: 'none' })
  }
})

// 检查登录状态
const isLoggedIn = computed(() => userStore.state.isLogin)

// 选择模型
const selectModel = (model) => {
  if (model.requiresAuth && !isLoggedIn.value) {
    uni.showModal({
      title: '提示',
      content: '该模型需要登录后使用，是否前往登录？',
      confirmText: '去登录',
      success: (res) => {
        if (res.confirm) {
          uni.navigateTo({ url: '/pages/login/index' })
        }
      }
    })
    return
  }
  currentModel.value = model
  showModelSelector.value = false
}

// SiliconFlow 聊天（无需登录）
const siliconFlowChat = async (userContent, messageHistory, aiMsg, enableDeepThink = false, enableWebSearch = false) => {
  try {
    const requestBody = {
      message: userContent,
      history: messageHistory,
      enableDeepThink,
      enableWebSearch
    }

    const responseText = await new Promise((resolve, reject) => {
      uni.request({
        url: `${BASE_URL}/api/ai/siliconflow/chat`,
        method: 'POST',
        timeout: 180000,
        header: {
          'Content-Type': 'application/json;charset=utf-8'
        },
        data: requestBody,
        success: (res) => {
          if (res.statusCode === 200) {
            resolve(typeof res.data === 'string' ? res.data : JSON.stringify(res.data))
          } else {
            reject(new Error(`请求失败: ${res.statusCode}`))
          }
        },
        fail: reject
      })
    })

    let data
    try {
      data = JSON.parse(responseText)
      console.log('SiliconFlow 响应:', data)
    } catch (e) {
      console.error('响应解析失败:', responseText)
      throw new Error('响应解析失败')
    }

    if (data.success) {
      console.log('AI 响应内容:', data.response)
      console.log('思考过程:', data.thinking)
      console.log('isError:', data.isError)

      // 使用 isError 字段检测错误响应
      if (data.isError) {
        console.log('检测到错误响应，直接显示错误消息')
        const lastIndex = messages.value.length - 1
        if (lastIndex >= 0 && messages.value[lastIndex]) {
          messages.value[lastIndex].content = data.response
          messages.value = [...messages.value]
          // 保存到 IndexedDB
          if (currentConversationId.value) {
            await addMessageToConversation(currentConversationId.value, {
              role: 'assistant',
              content: data.response
            })
          }
        }
        sending.value = false
        aiThinking.value = false
        aiAction.value = ''
        return
      }

      const fullContent = data.response || '抱歉，我暂时无法回答这个问题。'
      console.log('fullContent:', fullContent)
      const thinkingContent = data.thinking || '' // 思考过程
      const searchLinks = data.searchLinks || [] // 搜索到的链接

      const lastIndex = messages.value.length - 1
      console.log('lastIndex:', lastIndex)
      // 检查最后一条消息是否存在且是 AI 消息
      if (lastIndex < 0 || !messages.value[lastIndex] || messages.value[lastIndex].role !== 1) {
        console.error('AI 消息未正确创建')
        throw new Error('AI 消息未正确创建')
      }
      console.log('开始处理响应...')

      messages.value[lastIndex].thinkingExpanded = true
      // 初始化思考轮次数组
      messages.value[lastIndex].thinkingRounds = []
      messages.value[lastIndex].expandedRounds = []

      // 解析思考过程（按轮次分割）
      const thinkingRounds = []

      // 先尝试匹配 "=== 第 N 轮思考 ===" 格式（后端格式）
      let roundPattern = /=== 第 (\d+) 轮思考 ===\n([\s\S]*?)(?==== 第 \d+ 轮思考 ===|$)/g
      let match = roundPattern.exec(thinkingContent)
      if (match) {
        while (match !== null) {
          thinkingRounds.push(match[2].trim())
          match = roundPattern.exec(thinkingContent)
        }
      } else {
        // 使用 <｜end▁of▁thinking｜> 分隔符分割（DeepSeek格式）
        const delimiter = '<｜end▁of▁thinking｜>'
        if (thinkingContent.includes(delimiter)) {
          const parts = thinkingContent.split(delimiter)
          parts.forEach((part, index) => {
            const trimmed = part.trim()
            if (trimmed) {
              thinkingRounds.push(trimmed)
            }
          })
        } else if (thinkingContent.length > 0) {
          // 检查是否需要分段 - 如果思考内容超过800字，自动分段
          if (thinkingContent.length > 800) {
            // 按段落分割（根据换行符）
            const paragraphs = thinkingContent.split(/\n\n+/)
            paragraphs.forEach((para) => {
              const trimmed = para.trim()
              if (trimmed) {
                // 如果单个段落仍然很长（超过400字），按句子分割
                if (trimmed.length > 400) {
                  const sentences = trimmed.match(/[^。！？]+[。！？]+/g) || [trimmed]
                  sentences.forEach((sent) => {
                    if (sent.trim()) {
                      thinkingRounds.push(sent.trim())
                    }
                  })
                } else {
                  thinkingRounds.push(trimmed)
                }
              }
            })
            // 如果分段后仍为空，使用原始内容
            if (thinkingRounds.length === 0) {
              thinkingRounds.push(thinkingContent)
            }
          } else {
            // 没有分隔符且内容不长，整个内容作为一轮
            thinkingRounds.push(thinkingContent)
          }
        }
      }
      // 如果没有匹配到轮次格式，整个内容作为一轮
      if (thinkingRounds.length === 0 && thinkingContent.length > 0) {
        thinkingRounds.push(thinkingContent)
      }

      console.log('解析出思考轮次:', thinkingRounds.length, '轮')

      // 逐轮显示思考过程
      const thinkingSpeed = 60 // 打字速度（毫秒）
      const totalRounds = thinkingRounds.length

      for (let roundIdx = 0; roundIdx < totalRounds; roundIdx++) {
        const roundContent = thinkingRounds[roundIdx]

        // 开始新轮思考时，折叠上一轮
        if (roundIdx > 0) {
          messages.value[lastIndex].expandedRounds[roundIdx - 1] = false
          messages.value = [...messages.value]
        }

        // 折叠后滚动到底部
        scrollToBottom()

        // 添加新轮
        messages.value[lastIndex].thinkingRounds.push('')
        messages.value[lastIndex].expandedRounds.push(true)
        messages.value = [...messages.value]

        // 当前轮打字机效果
        let displayedRound = ''
        const scrollInterval = 20 // 每20个字滚动一次
        for (let i = 0; i < roundContent.length; i++) {
          displayedRound += roundContent[i]
          messages.value[lastIndex].thinkingRounds[roundIdx] = displayedRound
          messages.value = [...messages.value]
          // 每20个字滚动一次
          if ((i + 1) % scrollInterval === 0 || i === roundContent.length - 1) {
            scrollToBottom()
          }
          await new Promise(resolve => setTimeout(resolve, thinkingSpeed))
        }
        // 当前轮打字完成后保持展开，直到下一轮开始
      }

      // 显示最终回复前，折叠所有思考轮
      if (totalRounds > 0) {
        for (let i = 0; i < totalRounds; i++) {
          messages.value[lastIndex].expandedRounds[i] = false
        }
        messages.value = [...messages.value]
        // 折叠后滚动到底部
        scrollToBottom()
      }

      // 设置搜索链接（外链形式，默认折叠）
      if (searchLinks.length > 0) {
        messages.value[lastIndex].searchLinks = searchLinks.map(link => ({ url: link.url, title: link.title || link.url }))
        messages.value[lastIndex].searchLinksExpanded = false
        messages.value = [...messages.value]
      }

      // 使用打字机效果逐字显示内容
      console.log('调用 typeWriter，fullContent:', fullContent, 'lastIndex:', lastIndex)
      await typeWriter(fullContent, async () => {
        console.log('typeWriter callback 执行')
        // 保存 AI 响应到 IndexedDB（包括思考过程和搜索链接）
        if (currentConversationId.value) {
          const aiMsg = messages.value[lastIndex]
          await addMessageToConversation(currentConversationId.value, {
            role: 'assistant',
            content: fullContent,
            thinkingRounds: aiMsg?.thinkingRounds || [],
            searchLinks: aiMsg?.searchLinks || []
          })
        }
      }, lastIndex)
      console.log('typeWriter 完成')
    } else {
      throw new Error(data.message || '请求失败')
    }
  } catch (error) {
    console.error('siliconFlowChat 错误:', error)
    // 移除失败的 AI 消息
    const lastIndex = messages.value.findIndex(m => m === aiMsg)
    if (lastIndex !== -1) {
      messages.value.splice(lastIndex, 1)
    }
    throw error
  }
}

// 获取 JWT token
const getToken = () => {
  const userInfo = uni.getStorageSync('userInfo')
  if (userInfo && userInfo.token) {
    return userInfo.token
  }
  return null
}

// 获取用户API配额
const fetchQuota = async () => {
  const token = getToken()
  if (!token) return

  try {
    const response = await new Promise((resolve, reject) => {
      uni.request({
        url: `${BASE_URL}/api/ai/quota`,
        method: 'GET',
        header: {
          'Authorization': `Bearer ${token}`
        },
        success: (res) => {
          if (res.statusCode === 200 && res.data && res.data.success) {
            resolve(res.data)
          } else {
            reject(new Error(res.data?.message || '获取配额失败'))
          }
        },
        fail: reject
      })
    })

    tokensRemaining.value = response.tokensRemaining
    searchRemaining.value = response.searchRemaining

    // 如果有警告，显示提示
    if (response.warning) {
      uni.showToast({
        title: response.warning,
        icon: 'none',
        duration: 3000
      })
    }
  } catch (error) {
    console.error('获取配额失败:', error)
  }
}

// 检查登录并初始化（共享逻辑）
const checkLoginAndInit = async () => {
  updateLastMessageTime()

  // 检查登录状态
  if (!isLoggedIn.value) {
    // 如果当前是 Qwen3-8B 模型（无需登录），可以继续使用
    if (currentModel.value.id === 'siliconflow') {
      await loadRecentConversation()
      initialized.value = true
      return true
    }

    // 其他模型需要登录
    uni.showModal({
      title: '提示',
      content: '请先登录后再使用 DeepSeek 模型',
      confirmText: '去登录',
      success: (res) => {
        if (res.confirm) {
          uni.navigateTo({ url: '/pages/login/index' })
        } else {
          uni.switchTab({ url: '/pages/profile/index' })
        }
      }
    })
    return false
  }

  // 已登录，加载对话历史
  await loadRecentConversation()
  initialized.value = true
  return true
}

// 页面显示时检查登录状态
onShow(async () => {
  // 重新初始化用户状态
  userStore.init()

  // 首次访问检查登录状态并显示提示
  const hasShownLoginTip = uni.getStorageSync('hasShownLoginTip')
  if (!isLoggedIn.value && !hasShownLoginTip && currentModel.value.id === 'siliconflow') {
    uni.setStorageSync('hasShownLoginTip', true)
    uni.showModal({
      title: '提示',
      content: '您当前未登录，可以使用 Qwen3-8B 模型进行 AI 对话。登录后可使用 DeepSeek 模型（支持深度思考和联网搜索）。',
      showCancel: false,
      confirmText: '知道了'
    })
  }

  // 获取配额信息（仅已登录且使用需要认证的模型）
  if (isLoggedIn.value && currentModel.value.requiresAuth) {
    await fetchQuota()
    await loadRecentConversation()
    initialized.value = true
  } else if (currentModel.value.id === 'siliconflow') {
    // 默认模型无需登录，直接加载本地对话历史
    await loadRecentConversation()
    initialized.value = true
  }
})

// 初始化
onMounted(async () => {
  await checkLoginAndInit()
})

// 加载最近的对话历史
const loadRecentConversation = async () => {
  try {
    const conversations = await getAllConversations()
    if (conversations.length > 0) {
      const recent = conversations[0] // 按更新时间排序，最新的在前面
      currentConversationId.value = recent.id
      // 转换消息格式（将 role: 'user'/'assistant' 转换为 0/1）
      messages.value = recent.messages.map(m => ({
        role: m.role === 'user' ? 0 : 1,
        content: m.content,
        timestamp: m.timestamp,
        thinkingRounds: m.thinkingRounds || [],
        // 加载时默认收起所有思考轮次
        expandedRounds: m.expandedRounds
          ? m.expandedRounds.map(() => false)
          : (m.thinkingRounds ? new Array(m.thinkingRounds.length).fill(false) : []),
        searchLinks: m.searchLinks || []
      }))
      if (messages.value.length > 0) {
        showTimeStamp.value = true
        updateLastMessageTime()
      }
    }
  } catch (e) {
    console.warn('加载对话历史失败', e)
  }
}

// 更新时间戳
const updateLastMessageTime = () => {
  const now = new Date()
  const diff = Date.now() - (messages.value.length > 0 ? new Date(messages.value[messages.value.length - 1].timestamp).getTime() : now.getTime())
  if (diff < 60000) {
    lastMessageTime.value = '刚刚'
  } else if (diff < 3600000) {
    lastMessageTime.value = Math.floor(diff / 60000) + '分钟前'
  } else {
    lastMessageTime.value = now.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  }
}

// 切换深度思考（需要用户确认，DeepSeek 和默认模型可用）
const toggleDeepThink = () => {
  if (enableDeepThink.value) {
    // 关闭深度思考
    enableDeepThink.value = false
    uni.showToast({ title: '深度思考已关闭', icon: 'none' })
  } else {
    // 检查是否可以使用深度思考
    if (currentModel.value.id !== 'deepseek' && currentModel.value.id !== 'siliconflow') {
      uni.showModal({
        title: '提示',
        content: '深度思考功能在当前模型下不可用，请切换模型后重试。',
        showCancel: false,
        confirmText: '知道了'
      })
      return
    }

    // SiliconFlow 模型不需要登录
    if (currentModel.value.id === 'siliconflow') {
      uni.showModal({
        title: '深度思考模式',
        content: '深度思考模式使用更大的模型，响应会更慢但思考更深入。确定要开启吗？',
        confirmText: '开启',
        cancelText: '取消',
        success: (res) => {
          if (res.confirm) {
            enableDeepThink.value = true
            uni.showToast({ title: '深度思考已开启', icon: 'none' })
          }
        }
      })
      return
    }

    // DeepSeek 模型需要登录
    if (!isLoggedIn.value) {
      uni.showModal({
        title: '提示',
        content: '深度思考功能需要登录后使用，是否前往登录？',
        confirmText: '去登录',
        success: (res) => {
          if (res.confirm) {
            uni.navigateTo({ url: '/pages/login/index' })
          }
        }
      })
      return
    }

    // 开启前显示确认对话框
    uni.showModal({
      title: '深度思考模式',
      content: '深度思考模式使用更大的模型，响应会更慢但思考更深入。确定要开启吗？',
      confirmText: '开启',
      cancelText: '取消',
      success: (res) => {
        if (res.confirm) {
          enableDeepThink.value = true
          uni.showToast({ title: '深度思考已开启', icon: 'none' })
        }
      }
    })
  }
}

// 切换联网搜索
const toggleWebSearch = () => {
  if (enableWebSearch.value) {
    // 关闭搜索
    enableWebSearch.value = false
    uni.showToast({ title: '联网搜索已关闭', icon: 'none' })
  } else {
    // 检查是否可以使用联网搜索（仅 DeepSeek 模型可用）
    if (currentModel.value.id !== 'deepseek') {
      uni.showModal({
        title: '提示',
        content: '联网搜索功能仅在高级模型下可用，请切换模型后重试。',
        showCancel: false,
        confirmText: '知道了'
      })
      return
    }

    // 检查登录状态
    if (!isLoggedIn.value) {
      uni.showModal({
        title: '提示',
        content: '联网搜索功能需要登录后使用，是否前往登录？',
        confirmText: '去登录',
        success: (res) => {
          if (res.confirm) {
            uni.navigateTo({ url: '/pages/login/index' })
          }
        }
      })
      return
    }

    // 开启前显示安全提示
    uni.showModal({
      title: '联网搜索提示',
      content: '开启联网搜索后，AI 将从互联网获取实时信息。搜索结果来自第三方平台，内容仅供参考，确定要开启吗？',
      confirmText: '开启搜索',
      cancelText: '取消',
      success: (res) => {
        if (res.confirm) {
          enableWebSearch.value = true
          uni.showToast({ title: '联网搜索已开启', icon: 'none' })
        }
      }
    })
  }
}

// 清理对话（带确认框）
const clearConversation = async () => {
  uni.showModal({
    title: '提示',
    content: '确定要清空当前对话吗？',
    success: async (res) => {
      if (res.confirm) {
        await doClearConversation()
      }
    }
  })
}

// 直接清空对话（无确认框，用于轮数超限后）
const doClearConversation = async () => {
  messages.value = []
  showTimeStamp.value = false
  currentConversationId.value = null
  // 清除 IndexedDB 中的所有对话
  await clearAllConversations()
  uni.showToast({ title: '已开启新对话', icon: 'none' })
}

// 关闭页面
const handleClose = () => {
  uni.switchTab({ url: '/pages/blog/index' })
}

// 切换用户消息展开/折叠
const toggleUserMessage = (index) => {
  if (messages.value[index]) {
    messages.value[index].messageExpanded = !messages.value[index].messageExpanded
    messages.value = [...messages.value]
  }
}

// 切换思考过程展开/折叠
const toggleThinkingRound = (index, roundIndex) => {
  if (messages.value[index] && messages.value[index].thinkingRounds) {
    // 初始化 expandedRounds 数组
    if (!messages.value[index].expandedRounds) {
      messages.value[index].expandedRounds = new Array(messages.value[index].thinkingRounds.length).fill(true)
    }
    // 切换当前轮的展开状态
    messages.value[index].expandedRounds[roundIndex] = !messages.value[index].expandedRounds[roundIndex]
    // 切换整体展开状态（如果所有轮都展开则thinkingExpanded为true）
    messages.value[index].thinkingExpanded = messages.value[index].expandedRounds.every(r => r)
    // 强制触发响应式更新
    messages.value = [...messages.value]
  }
}

// 切换所有思考过程
const toggleThinking = (index) => {
  if (messages.value[index] && messages.value[index].thinkingRounds) {
    const newState = !messages.value[index].thinkingExpanded
    messages.value[index].thinkingExpanded = newState
    if (!messages.value[index].expandedRounds) {
      messages.value[index].expandedRounds = new Array(messages.value[index].thinkingRounds.length).fill(newState)
    } else {
      messages.value[index].expandedRounds = messages.value[index].expandedRounds.map(() => newState)
    }
    // 强制触发响应式更新
    messages.value = [...messages.value]
  }
}

// 打开外部链接
const openLink = (url) => {
  if (!url) return
  // #ifdef H5
  window.open(url, '_blank')
  // #endif
  // #ifdef MP-WEIXIN
  uni.setClipboardData({
    data: url,
    success: () => {
      uni.showToast({ title: '链接已复制', icon: 'none' })
    }
  })
  // #endif
}

// 从URL提取标题
const extractTitleFromUrl = (url) => {
  try {
    const urlObj = new URL(url)
    const path = urlObj.pathname
    const segments = path.split('/').filter(s => s.length > 0)

    if (segments.length > 0) {
      const lastSegment = segments[segments.length - 1]
      // 移除文件扩展名
      let title = lastSegment.replace(/\.(html?|php|jsp|aspx?)$/i, '')

      // 如果标题太短或像纯数字，尝试用上一段
      if (title.length < 5 || /^\d+$/.test(title)) {
        if (segments.length >= 2) {
          const prevSegment = segments[segments.length - 2]
          title = prevSegment.replace(/\.(html?|php|jsp|aspx?)$/i, '')
        }
      }

      // 尝试将短横线/下划线替换为空格，并首字母大写
      title = title.replace(/[-_]/g, ' ').replace(/\b\w/g, c => c.toUpperCase())

      // 如果结果还是太短或无意义，使用域名
      if (title.length < 5 || /^\d+$/.test(title)) {
        return urlObj.hostname.replace('www.', '')
      }
      return title
    }
    return urlObj.hostname.replace('www.', '')
  } catch (e) {
    return url
  }
}

// 切换搜索链接展开/折叠
const toggleSearchLinks = (index) => {
  if (messages.value[index] && messages.value[index].searchLinks) {
    messages.value[index].searchLinksExpanded = !messages.value[index].searchLinksExpanded
    messages.value = [...messages.value]
  }
}

// 发送消息
const sendMessage = async () => {
  if (!inputMessage.value.trim() || sending.value || aiThinking.value) return

  // DeepSeek 模型需要登录
  if (currentModel.value.requiresAuth && !isLoggedIn.value) {
    uni.showModal({
      title: '提示',
      content: '该模型需要登录后使用，是否前往登录？',
      confirmText: '去登录',
      success: (res) => {
        if (res.confirm) {
          uni.navigateTo({ url: '/pages/login/index' })
        }
      }
    })
    return
  }

  // 检查对话轮数（用户消息数量）
  const userMessageCount = messages.value.filter(m => m.role === 0).length
  if (userMessageCount >= 10) {
    uni.showModal({
      title: '提示',
      content: '对话轮数已达上限（10轮），请清除会话后继续对话。',
      confirmText: '清除会话',
      cancelText: '取消',
      success: (res) => {
        if (res.confirm) {
          doClearConversation()
        }
      }
    })
    return
  }

  const userContent = inputMessage.value.trim()

  // 添加用户消息
  const userMsg = {
    role: 0,
    content: userContent,
    timestamp: new Date().toISOString()
  }
  messages.value.push(userMsg)
  inputMessage.value = ''

  // 更新时间戳显示
  if (messages.value.length >= 1) {
    showTimeStamp.value = true
    updateLastMessageTime()
  }

  // 添加 AI 占位消息
  const aiMsg = {
    role: 1,
    content: '',
    thinking: '',
    thinkingExpanded: true, // 默认展开思考过程
    timestamp: new Date().toISOString()
  }
  messages.value.push(aiMsg)

  // 滚动到底部显示用户消息和AI占位消息
  nextTick(() => {
    scrollToBottom()
  })

  sending.value = true
  aiThinking.value = true
  // 设置当前AI动作：联网搜索模式下显示"联网搜索中..."
  aiAction.value = enableWebSearch.value ? '联网搜索' : '思考'

  try {
    // 创建或获取对话ID（使用 IndexedDB）
    if (!currentConversationId.value) {
      currentConversationId.value = await createConversation()
    }

    // 保存用户消息到 IndexedDB
    await addMessageToConversation(currentConversationId.value, {
      role: 'user',
      content: userContent
    })

    // 构建消息历史（最近10轮），过滤掉空的 assistant 消息
    const recentMessages = messages.value
      .filter(m => !(m.role === 1 && !m.content)) // 过滤掉空的 assistant 消息
      .slice(-20)
      .map(m => ({
        role: m.role === 0 ? 'user' : 'assistant',
        content: m.content
      }))

    // 流式发送消息
    await streamMessage(currentConversationId.value, recentMessages, aiMsg)

  } catch (error) {
    console.error('发送消息失败', error)
    // 移除失败的 AI 消息
    const lastIndex = messages.value.findIndex(m => m === aiMsg)
    if (lastIndex !== -1) {
      messages.value.splice(lastIndex, 1)
    }

    // 检查是否是未登录错误
    if (error.message === '未登录或登录已过期') {
      uni.showModal({
        title: '提示',
        content: '登录已过期，请重新登录',
        confirmText: '去登录',
        success: (res) => {
          if (res.confirm) {
            uni.navigateTo({ url: '/pages/login/index' })
          }
        }
      })
      return
    }

    // 弹出错误提示框
    uni.showModal({
      title: '提示',
      content: error.message || 'AI 响应失败，请稍后重试',
      showCancel: false,
      confirmText: '确定'
    })
  } finally {
    sending.value = false
    aiThinking.value = false
    aiAction.value = ''
  }
}

// 发送消息（根据模型选择调用不同 API）
const streamMessage = async (conversationId, messageHistory, aiMsg) => {
  // 根据模型选择调用不同的 API
  if (currentModel.value.id === 'siliconflow') {
    // SiliconFlow 不需要登录
    // 过滤掉最后一条用户消息（因为 userContent 已经单独传递了）
    const lastUserMessage = messageHistory.length > 0 ? messageHistory[messageHistory.length - 1].content : ''
    const filteredHistory = messageHistory.filter(m => {
      // 保留所有非最后一条用户消息的消息
      // 前端 role: 0=user, 1=assistant
      if (m.content === lastUserMessage && m.role === 0) {
        return false
      }
      return true
    })
    await siliconFlowChat(
      lastUserMessage,
      filteredHistory,
      aiMsg,
      enableDeepThink.value,
      enableWebSearch.value
    )
  } else {
    // DeepSeek 需要登录
    const requestBody = {
      messages: messageHistory,
      enableDeepThink: enableDeepThink.value,
      enableWebSearch: enableWebSearch.value
    }

    // 检查认证状态
    const token = getToken()
    if (!token) {
      throw new Error('未登录或登录已过期')
    }

    // 使用现有的 DeepSeek API
    await syncRequest(requestBody, aiMsg, conversationId)
  }
}

// 打字机效果：逐字显示文本
const typeWriter = async (text, callback, targetIndex = -1) => {
  const typingSpeed = 60 // 打字速度（毫秒）
  const scrollInterval = 20 // 每20个字滚动一次

  // 找到目标消息索引（指定索引或最后一条AI消息）
  let targetMsgIndex = targetIndex
  if (targetMsgIndex < 0) {
    // 找到最后一条 role === 1 的消息
    for (let i = messages.value.length - 1; i >= 0; i--) {
      if (messages.value[i] && messages.value[i].role === 1) {
        targetMsgIndex = i
        break
      }
    }
  }

  // 如果内容为空，显示默认消息
  if (!text || text.trim() === '') {
    console.warn('AI 响应内容为空')
    if (targetMsgIndex >= 0 && messages.value[targetMsgIndex]) {
      messages.value[targetMsgIndex].content = '抱歉，我暂时无法回答这个问题。'
      messages.value = [...messages.value]
    }
    if (callback) {
      await callback()
    }
    return
  }

  let displayedText = ''
  console.log('typeWriter: 开始循环，text.length:', text.length, 'targetMsgIndex:', targetMsgIndex)
  for (let i = 0; i < text.length; i++) {
    displayedText += text[i]
    // 更新目标 AI 消息
    if (targetMsgIndex >= 0 && messages.value[targetMsgIndex]) {
      messages.value[targetMsgIndex].content = displayedText
      messages.value = [...messages.value]
      console.log('typeWriter: 更新了消息', targetMsgIndex, '内容:', displayedText)
    } else {
      console.error('typeWriter: 无法更新消息，targetMsgIndex:', targetMsgIndex, 'messages.value:', messages.value)
    }
    // 每20个字滚动一次
    if ((i + 1) % scrollInterval === 0 || i === text.length - 1) {
      scrollToBottom()
    }
    // 延迟
    await new Promise(resolve => setTimeout(resolve, typingSpeed))
  }
  console.log('typeWriter: 循环完成，最终内容:', messages.value[targetMsgIndex]?.content)

  if (callback) {
    await callback()
  }
  // 完成后滚动到底部
  scrollToBottom()
}

// 滚动到底部
const scrollToBottom = async (force = false) => {
  // 如果用户正在手动滚动，不打断用户操作
  if (!force && userScrolling.value) {
    return
  }
  // 使用 nextTick 确保 DOM 更新后再滚动
  nextTick(() => {
    if (messages.value.length > 0) {
      const targetId = 'msg-' + (messages.value.length - 1)
      // 先清空再设置，强制重新滚动
      scrollIntoView.value = ''
      setTimeout(() => {
        scrollIntoView.value = targetId
      }, 10)
    }
  })
}

// 显示最大轮数警告弹窗
const showMaxRoundsWarning = () => {
  uni.showModal({
    title: '提示',
    content: '对话轮数已达上限（10轮），请清除会话后继续对话。',
    confirmText: '清除会话',
    cancelText: '取消',
    success: (res) => {
      if (res.confirm) {
        clearConversation()
      }
    }
  })
}

// 同步请求（带前端打字机效果）- 兼容小程序和H5
const syncRequest = async (requestBody, aiMsg, conversationId) => {
  try {
    const token = getToken()

    // 小程序和H5统一的请求方式
    const responseText = await new Promise((resolve, reject) => {
      uni.request({
        url: `${BASE_URL}/api/ai/chat`,
        method: 'POST',
        timeout: 180000, // 180秒超时
        header: {
          'Content-Type': 'application/json;charset=utf-8',
          'Authorization': token ? `Bearer ${token}` : ''
        },
        data: requestBody,
        success: (res) => {
          if (res.statusCode === 200) {
            resolve(typeof res.data === 'string' ? res.data : JSON.stringify(res.data))
          } else {
            reject(new Error(`请求失败: ${res.statusCode}`))
          }
        },
        fail: reject
      })
    })

    // 尝试解析 JSON
    let data
    try {
      data = JSON.parse(responseText)
    } catch (e) {
      console.error('JSON解析失败:', responseText)
      throw new Error('响应解析失败')
    }

    // 检查是否是警告
    if (data.warning) {
      // 移除 AI 占位消息
      const lastIndex = messages.value.findIndex(m => m === aiMsg)
      if (lastIndex !== -1) {
        messages.value.splice(lastIndex, 1)
      }

      // 根据消息内容显示不同的警告
      if (data.message && (data.message.includes('Token') || data.message.includes('搜索'))) {
        // Token 或搜索次数不足，弹窗提示
        uni.showModal({
          title: '提示',
          content: data.message,
          confirmText: '联系充值',
          cancelText: '取消',
          success: (res) => {
            if (res.confirm) {
              // 跳转拨打电话
              uni.makePhoneCall({
                phoneNumber: '13818993049'
              })
            } else {
              // 取消则跳回我的界面
              uni.switchTab({
                url: '/pages/profile/index'
              })
            }
          }
        })
      } else {
        // 对话轮数达上限
        showMaxRoundsWarning()
      }
      sending.value = false
      aiThinking.value = false
      return
    }

    if (data.success) {
      // 使用 isError 字段检测错误响应（如果后端支持）
      if (data.isError) {
        console.log('DeepSeek 检测到错误响应:', data.response)
        const lastIndex = messages.value.length - 1
        if (lastIndex >= 0 && messages.value[lastIndex]) {
          messages.value[lastIndex].content = data.response
          messages.value = [...messages.value]
        }
        sending.value = false
        aiThinking.value = false
        aiAction.value = ''
        return
      }

      const fullContent = data.response || '抱歉，我暂时无法回答这个问题。'
      const thinkingContent = data.thinking || '' // 思考过程
      const searchLinks = data.searchLinks || [] // 搜索到的链接

      const lastIndex = messages.value.length - 1
      if (lastIndex >= 0) {
        messages.value[lastIndex].thinkingExpanded = true
        // 初始化思考轮次数组
        messages.value[lastIndex].thinkingRounds = []
        messages.value[lastIndex].expandedRounds = []
      }

      // 解析思考过程（按轮次分割）
      const thinkingRounds = []

      // 先尝试匹配 "=== 第 N 轮思考 ===" 格式（后端格式）
      let roundPattern = /=== 第 (\d+) 轮思考 ===\n([\s\S]*?)(?==== 第 \d+ 轮思考 ===|$)/g
      let match = roundPattern.exec(thinkingContent)
      if (match) {
        while (match !== null) {
          thinkingRounds.push(match[2].trim())
          match = roundPattern.exec(thinkingContent)
        }
      } else {
        // 使用 <｜end▁of▁thinking｜> 分隔符分割（DeepSeek格式）
        const delimiter = '<｜end▁of▁thinking｜>'
        if (thinkingContent.includes(delimiter)) {
          const parts = thinkingContent.split(delimiter)
          parts.forEach((part, index) => {
            const trimmed = part.trim()
            if (trimmed) {
              thinkingRounds.push(trimmed)
            }
          })
        } else if (thinkingContent.length > 0) {
          // 检查是否需要分段 - 如果思考内容超过800字，自动分段
          if (thinkingContent.length > 800) {
            // 按段落分割（根据换行符）
            const paragraphs = thinkingContent.split(/\n\n+/)
            paragraphs.forEach((para) => {
              const trimmed = para.trim()
              if (trimmed) {
                // 如果单个段落仍然很长（超过400字），按句子分割
                if (trimmed.length > 400) {
                  const sentences = trimmed.match(/[^。！？]+[。！？]+/g) || [trimmed]
                  sentences.forEach((sent) => {
                    if (sent.trim()) {
                      thinkingRounds.push(sent.trim())
                    }
                  })
                } else {
                  thinkingRounds.push(trimmed)
                }
              }
            })
            // 如果分段后仍为空，使用原始内容
            if (thinkingRounds.length === 0) {
              thinkingRounds.push(thinkingContent)
            }
          } else {
            // 没有分隔符且内容不长，整个内容作为一轮
            thinkingRounds.push(thinkingContent)
          }
        }
      }
      // 如果没有匹配到轮次格式，整个内容作为一轮
      if (thinkingRounds.length === 0 && thinkingContent.length > 0) {
        thinkingRounds.push(thinkingContent)
      }

      console.log('DeepSeek 解析出思考轮次:', thinkingRounds.length, '轮')

      // 如果找不到 AI 消息，跳过思考过程显示
      if (lastIndex < 0 || !messages.value[lastIndex]) {
        console.error('无法找到 AI 消息，跳过思考过程显示')
      } else {
        // 逐轮显示思考过程
        const thinkingSpeed = 60 // 打字速度（毫秒）
        const totalRounds = thinkingRounds.length

        for (let roundIdx = 0; roundIdx < totalRounds; roundIdx++) {
          const roundContent = thinkingRounds[roundIdx]

          // 开始新轮思考时，折叠上一轮
          if (roundIdx > 0) {
            messages.value[lastIndex].expandedRounds[roundIdx - 1] = false
            messages.value = [...messages.value]
          }

          // 折叠后滚动到底部
          scrollToBottom()

          // 添加新轮
          messages.value[lastIndex].thinkingRounds.push('')
          messages.value[lastIndex].expandedRounds.push(true)
          messages.value = [...messages.value]

          // 当前轮打字机效果
          let displayedRound = ''
          const scrollInterval = 20 // 每20个字滚动一次
          for (let i = 0; i < roundContent.length; i++) {
            displayedRound += roundContent[i]
            messages.value[lastIndex].thinkingRounds[roundIdx] = displayedRound
            messages.value = [...messages.value]
            // 每20个字滚动一次
            if ((i + 1) % scrollInterval === 0 || i === roundContent.length - 1) {
              scrollToBottom()
            }
            await new Promise(resolve => setTimeout(resolve, thinkingSpeed))
          }
          // 当前轮打字完成后保持展开，直到下一轮开始
        }

        // 显示最终回复前，折叠所有思考轮
        if (totalRounds > 0) {
          for (let i = 0; i < totalRounds; i++) {
            messages.value[lastIndex].expandedRounds[i] = false
          }
          messages.value = [...messages.value]
          // 折叠后滚动到底部
          scrollToBottom()
        }
      }

      // 设置搜索链接（外链形式，默认折叠）
      if (searchLinks.length > 0 && lastIndex >= 0) {
        messages.value[lastIndex].searchLinks = searchLinks.map(link => ({ url: link.url, title: link.title || link.url }))
        messages.value[lastIndex].searchLinksExpanded = false
        messages.value = [...messages.value]
      }

      // 使用打字机效果逐字显示内容
      await typeWriter(fullContent, async () => {
        // 更新配额显示
        if (data.tokensRemaining !== undefined) {
          tokensRemaining.value = data.tokensRemaining
        }
        if (data.searchRemaining !== undefined) {
          searchRemaining.value = data.searchRemaining
        }
        // 显示配额警告
        if (data.warning) {
          uni.showToast({
            title: data.warning,
            icon: 'none',
            duration: 3000
          })
        }
        // 保存 AI 响应到 IndexedDB（包括思考过程和搜索链接）
        if (conversationId) {
          const aiMsg = messages.value[lastIndex]
          await addMessageToConversation(conversationId, {
            role: 'assistant',
            content: fullContent,
            thinkingRounds: aiMsg?.thinkingRounds || [],
            searchLinks: aiMsg?.searchLinks || []
          })
        }
      }, lastIndex)
    } else {
      throw new Error(data.message || '请求失败')
    }
  } catch (error) {
    // 移除失败的 AI 消息
    const lastIndex = messages.value.findIndex(m => m === aiMsg)
    if (lastIndex !== -1) {
      messages.value.splice(lastIndex, 1)
    }
    throw error
  }
}
</script>

<style lang="scss" scoped>
/* 防止页面滚动 */
page {
  height: 100%;
  overflow: hidden;
}

.ai-chat-page {
  height: 100vh;
  height: 100%;
  background-color: #f5f5f5;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* 顶部栏 */
.chat-header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 30rpx;
  padding-top: calc(80rpx + env(safe-area-inset-top));
  padding-left: calc(30rpx + env(safe-area-inset-left));
  padding-right: calc(30rpx + env(safe-area-inset-right));
  padding-bottom: 30rpx;
  background-color: #fff;
  border-bottom: 1rpx solid #eee;
  flex-shrink: 0;
  flex: 0 0 auto;
  z-index: 999;
}

/* PC端适配 */
@media screen and (min-width: 768px) {
  .chat-header {
    left: 50%;
    right: auto;
    transform: translateX(-50%);
    width: 100%;
    max-width: 800px;
    padding-left: 30rpx;
    padding-right: 30rpx;
  }
}

.chat-header .header-left {
  flex: 1;
}

.chat-header .header-left .chat-title {
  display: block;
  font-size: 36rpx;
  font-weight: bold;
  color: #333;
}

.chat-header .header-left .chat-subtitle {
  display: block;
  font-size: 24rpx;
  color: #999;
  margin-top: 8rpx;
}

.chat-header .header-right {
  display: flex;
  align-items: center;
  gap: 16rpx;
}

/* 模型选择器行 */
.model-selector-row {
  position: relative;
  margin-bottom: 8rpx;
}

/* 模型选择器 */
.model-selector {
  display: flex;
  align-items: center;
  gap: 8rpx;
  padding: 8rpx 16rpx;
  background-color: #fff;
  border-radius: 20rpx;
  border: 1rpx solid #eee;

  .model-name {
    font-size: 24rpx;
    color: #667eea;
    font-weight: 500;
  }

  .model-arrow {
    font-size: 16rpx;
    color: #667eea;
  }
}

/* 模型选择下拉框 */
.model-dropdown {
  position: absolute;
  bottom: 100%;
  left: 0;
  z-index: 1000;
  margin-bottom: 8rpx;
}

.model-dropdown-content {
  background-color: #fff;
  border-radius: 16rpx;
  box-shadow: 0 -4rpx 20rpx rgba(0, 0, 0, 0.1);
  min-width: 200rpx;
  overflow: hidden;
}

.model-option {
  display: flex;
  align-items: center;
  padding: 24rpx;
  gap: 16rpx;
  border-bottom: 1rpx solid #f0f0f0;

  &:last-child {
    border-bottom: none;
  }

  &.active {
    background-color: #f8f9fa;
  }

  &.disabled {
    opacity: 0.5;
  }

  .model-info {
    flex: 1;

    .model-option-name {
      display: block;
      font-size: 28rpx;
      color: #333;
      font-weight: 500;
    }

    .model-provider {
      display: block;
      font-size: 22rpx;
      color: #999;
      margin-top: 4rpx;
    }
  }

  .model-badges {
    display: flex;
    gap: 8rpx;

    .badge {
      font-size: 18rpx;
      padding: 4rpx 8rpx;
      border-radius: 8rpx;

      &.vision {
        background-color: #e8f5e9;
        color: #4CD964;
      }

      &.auth {
        background-color: #fff3e0;
        color: #ff9800;
      }
    }
  }
}

/* 消息区域 - 占满中间空间 */
.messages-container {
  flex: 1;
  width: 100%;
  min-height: 0;
  padding: 20rpx;
  padding-top: calc(200rpx + env(safe-area-inset-top));
  padding-bottom: calc(270rpx + env(safe-area-inset-bottom));
  background-color: #f5f5f5;
  overflow-y: auto;

  /* 隐藏滚动条 */
  ::-webkit-scrollbar {
    display: none;
    width: 0;
    height: 0;
    background: transparent;
  }

  scrollbar-width: none;
  -ms-overflow-style: none;
}

/* 顶部占位（填满剩余空间，让消息贴底显示） */
.flex-placeholder {
  flex: 1;
  min-height: 0;
}

/* 时间戳 */
.time-stamp {
  text-align: center;
  padding: 20rpx 0;

  text {
    font-size: 24rpx;
    color: #999;
  }
}

/* 欢迎消息 */
.welcome-message {
  display: flex;
  justify-content: center;
  padding: 60rpx 40rpx;

  .welcome-content {
    max-width: 80%;
    text-align: center;

    .welcome-title {
      display: block;
      font-size: 32rpx;
      font-weight: bold;
      color: #333;
      margin-bottom: 20rpx;
    }

    .welcome-desc {
      font-size: 28rpx;
      color: #666;
      line-height: 1.6;
    }
  }
}

/* 思考中动画 */
.thinking-indicator {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40rpx 30rpx;
  gap: 16rpx;

  .thinking-text {
    font-size: 28rpx;
    color: #666;
    font-weight: 500;
  }

  .thinking-dots {
    display: flex;
    align-items: center;
  }

  .thinking-dot {
    width: 12rpx;
    height: 12rpx;
    background-color: #999;
    border-radius: 50%;
    margin: 0 4rpx;
    animation: thinkingPulse 1.4s infinite ease-in-out;

    &:nth-child(1) {
      animation-delay: 0s;
    }

    &:nth-child(2) {
      animation-delay: 0.2s;
    }

    &:nth-child(3) {
      animation-delay: 0.4s;
    }
  }
}

@keyframes thinkingPulse {
  0%, 80%, 100% {
    transform: scale(0.6);
    opacity: 0.5;
  }
  40% {
    transform: scale(1);
    opacity: 1;
  }
}

/* 消息项 */
.message-item {
  display: flex;
  margin-bottom: 30rpx;
  margin-top: 30rpx;

  &.user {
    flex-direction: row-reverse;

    .message-avatar {
      margin-left: 0;
      margin-right: 16rpx;
    }

    .message-content {
      align-items: flex-end;

      .message-text {
        background-color: #4CD964;
        color: #333;
        border-radius: 8rpx;
      }
    }
  }

  &.ai {
    .message-avatar {
      margin-right: 16rpx;
    }

    .message-content {
      align-items: flex-start;

      .message-text {
        background-color: #fff;
        color: #333;
        border-radius: 8rpx;
      }
    }
  }

  .message-avatar {
    width: 72rpx;
    height: 72rpx;
    background-color: #f5f5f5;
    border-radius: 8rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;

    .avatar-icon {
      font-size: 40rpx;
    }
  }

  .message-content {
    max-width: 70%;
    display: flex;
    flex-direction: column;
    overflow: hidden;
    word-break: break-word;
  }

  .message-text {
    padding: 20rpx 24rpx;
    font-size: 28rpx;
    line-height: 1.6;
    overflow: hidden;
    word-break: break-word;
  }

  .expand-text {
    color: #667eea;
    font-size: 24rpx;
    margin-left: 8rpx;
    font-weight: 500;
  }

  .collapse-text {
    color: #999;
    font-size: 22rpx;
    margin-left: 8rpx;
  }
}

/* 思考过程框 */
.thinking-box {
  background-color: #f8f9fa;
  border-radius: 12rpx;
  margin-bottom: 16rpx;
  overflow: hidden;
  width: 100%;
  box-sizing: border-box;
}

.thinking-header {
  display: flex;
  align-items: center;
  padding: 12rpx 16rpx;
  background-color: #e8f4fd;
  cursor: pointer;

  .thinking-icon {
    font-size: 24rpx;
    margin-right: 8rpx;
  }

  .thinking-title {
    font-size: 24rpx;
    color: #667eea;
    font-weight: 500;
    flex: 1;
  }

  .thinking-text {
    font-size: 22rpx;
    color: #999;
    margin-left: 16rpx;
    margin-right: 8rpx;
  }

  .thinking-arrow {
    font-size: 24rpx;
    color: #667eea;
    transition: transform 0.3s ease;
    font-weight: bold;

    &.expanded {
      transform: rotate(180deg);
    }
  }
}

.thinking-content {
  padding: 16rpx;
  border-top: 1rpx solid #e8f4fd;
  overflow: hidden;

  text {
    font-size: 24rpx;
    color: #666;
    line-height: 1.6;
    word-break: break-word;
  }
}

/* 搜索参考链接（可折叠） */
.search-links {
  margin-top: 16rpx;
  background-color: #f0f7ff;
  border-radius: 12rpx;
  padding: 12rpx;
  border: 1rpx solid #e0eaff;
}

.links-header {
  display: flex;
  align-items: center;
  padding: 8rpx 4rpx;

  .links-icon {
    font-size: 22rpx;
    margin-right: 6rpx;
  }

  .links-title {
    font-size: 22rpx;
    color: #667eea;
    font-weight: 500;
  }

  .links-count {
    font-size: 22rpx;
    color: #999;
    margin-left: 4rpx;
  }

  .links-arrow {
    font-size: 20rpx;
    color: #667eea;
    margin-left: auto;
    transition: transform 0.3s ease;

    &.expanded {
      transform: rotate(180deg);
    }
  }
}

.links-list {
  border-top: 1rpx solid #e0eaff;
  padding-top: 8rpx;
}

.link-item {
  display: flex;
  align-items: center;
  padding: 10rpx 8rpx;
  border-bottom: 1rpx solid #e0eaff;
  gap: 10rpx;

  &:last-child {
    border-bottom: none;
  }

  .link-index {
    font-size: 22rpx;
    color: #667eea;
    flex-shrink: 0;
  }

  .link-title {
    flex: 1;
    font-size: 22rpx;
    color: #333;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .link-arrow {
    font-size: 24rpx;
    color: #999;
    flex-shrink: 0;
  }
}

/* 输入区域 - 固定在底部导航栏上方（小程序使用 10px） */
.input-area {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 50px;
  background-color: #f5f5f5;
  padding: 12rpx 20rpx;
  padding-left: calc(20rpx + env(safe-area-inset-left));
  padding-right: calc(20rpx + env(safe-area-inset-right));
  padding-bottom: calc(12rpx + env(safe-area-inset-bottom));
  border-top: 1rpx solid #eee;
  z-index: 100;
}

/* #ifdef MP-WEIXIN */
.input-area {
  bottom: 0;
  padding-bottom: env(safe-area-inset-bottom) !important;
}
/* #endif */

/* PC端适配 */
@media screen and (min-width: 768px) {
  .input-area {
    bottom: calc(50px + env(safe-area-inset-bottom));
  }
}

/* 深度思考行 */
.deep-think-row {
  display: flex;
  align-items: center;
  gap: 16rpx;
  margin-bottom: 8rpx;
}

.deep-think-btn {
  display: flex;
  align-items: center;
  gap: 8rpx;
  padding: 8rpx 16rpx;
  background-color: #fff;
  border-radius: 20rpx;
  border: 1rpx solid #eee;

  &.active {
    background-color: #e8f5e9;
    border-color: #4CD964;

    .deep-think-label {
      color: #4CD964;
    }
  }

  .deep-think-icon {
    font-size: 24rpx;
  }

  .deep-think-label {
    font-size: 24rpx;
    color: #666;
  }
}

/* 清空按钮 */
.clear-btn {
  display: flex;
  align-items: center;
  gap: 6rpx;
  padding: 8rpx 16rpx;
  background-color: #fff;
  border-radius: 20rpx;
  border: 1rpx solid #eee;

  .clear-icon {
    font-size: 24rpx;
  }

  .clear-label {
    font-size: 24rpx;
    color: #666;
  }
}

/* 联网搜索按钮 */
.web-search-btn {
  display: flex;
  align-items: center;
  gap: 6rpx;
  padding: 8rpx 16rpx;
  background-color: #fff;
  border-radius: 20rpx;
  border: 1rpx solid #eee;

  &.active {
    background-color: #e3f2fd;
    border-color: #2196F3;

    .web-search-label {
      color: #2196F3;
    }
  }

  .web-search-icon {
    font-size: 24rpx;
  }

  .web-search-label {
    font-size: 24rpx;
    color: #666;
  }
}

.token-count {
  font-size: 22rpx;
  color: #999;
  margin-left: auto;
}

/* 配额显示 */
.quota-info {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4rpx;
  margin-left: auto;
  font-size: 16rpx;
  color: #999;

  .quota-token {
    color: #4CD964;
  }

  .quota-search {
    color: #2196F3;
  }
}

.input-controls {
  display: flex;
  align-items: flex-end;
  gap: 16rpx;
}

.message-input {
  flex: 1;
  height: 72rpx;
  max-height: 200rpx;
  background-color: #fff;
  border-radius: 8rpx;
  padding: 16rpx 20rpx;
  font-size: 28rpx;
  line-height: 1.5;
  overflow-y: auto;
}

/* 发送按钮 */
.send-btn {
  width: 120rpx;
  background-color: #4CD964;
  border-radius: 8rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  align-self: stretch;

  &.disabled {
    background-color: #ccc;
  }

  .send-text {
    font-size: 28rpx;
    color: #fff;
  }
}
</style>
