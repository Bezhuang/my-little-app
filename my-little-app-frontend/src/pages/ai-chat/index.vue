<template>
  <view class="ai-chat-page">
    <!-- 顶部栏 -->
    <view class="chat-header">
      <view class="header-left">
        <text class="chat-title">Bezhuang AI</text>
        <text class="chat-subtitle">AI多轮对话用（最多10轮，可以选择是否开启深度思考）</text>
      </view>
      <view class="header-right" @click="handleClose">
        <text class="close-icon">✕</text>
      </view>
    </view>

    <!-- 消息区域 -->
    <scroll-view
      class="messages-container"
      scroll-y
      :scroll-into-view="scrollToView"
      :scroll-top="scrollTop"
      :lower-threshold="50"
      @scroll="handleScroll"
    >
      <!-- 时间戳 -->
      <view v-if="showTimeStamp" class="time-stamp">
        <text>{{ lastMessageTime }}</text>
      </view>

      <!-- 欢迎消息 -->
      <view v-if="messages.length === 0" class="welcome-message">
        <view class="welcome-content">
          <text class="welcome-title">你好，我是 Bezhuang AI</text>
          <text class="welcome-desc">我可以进行多轮对话（最多10轮），支持开启深度思考模式。有什么我可以帮你的吗？</text>
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
          <!-- 思考过程（可折叠） -->
          <view v-if="msg.thinking && msg.thinking.length > 0" class="thinking-box">
            <view class="thinking-header" @click="toggleThinking(index)">
              <text class="thinking-icon">💭</text>
              <text class="thinking-title">思考过程</text>
              <text class="thinking-arrow" :class="{ expanded: msg.thinkingExpanded }">▼</text>
            </view>
            <view v-if="msg.thinkingExpanded" class="thinking-content">
              <text>{{ msg.thinking }}</text>
            </view>
          </view>
          <!-- 消息内容 -->
          <view class="message-text">
            <text>{{ msg.content }}</text>
          </view>
        </view>
      </view>

      <!-- AI 思考中动画 -->
      <view v-if="aiThinking" class="thinking-indicator" id="thinking">
        <text class="thinking-text">思考中...</text>
        <view class="thinking-dots">
          <view class="thinking-dot"></view>
          <view class="thinking-dot"></view>
          <view class="thinking-dot"></view>
        </view>
      </view>
    </scroll-view>

    <!-- 输入区域 -->
    <view class="input-area">
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
          auto-height
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
import { ref, computed, onMounted, nextTick } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import userStore from '@/store/user'
import { createConversation, getAllConversations, addMessageToConversation, clearAllConversations } from '@/utils/indexedDB'
import { BASE_URL } from '@/utils/constants'

// 状态
const messages = ref([])
const inputMessage = ref('')
const sending = ref(false)
const aiThinking = ref(false)
const scrollToView = ref('')
const scrollTop = ref(0)  // 用于控制滚动位置
const lastMessageTime = ref('')
const showTimeStamp = ref(false)
const enableDeepThink = ref(false) // 深度思考开关
const enableWebSearch = ref(false) // 联网搜索开关
const currentConversationId = ref(null) // 当前对话ID
const initialized = ref(false) // 标记是否已初始化
const tokensRemaining = ref(null) // 剩余Token数
const searchRemaining = ref(null) // 剩余搜索次数

// 检查登录状态
const isLoggedIn = computed(() => userStore.state.isLogin)

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

  // 检查登录状态，未登录则弹出提示后跳转
  if (!isLoggedIn.value) {
    uni.showModal({
      title: '提示',
      content: '请先登录后再使用 AI 助手',
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

  // 每次显示页面时都检查登录状态
  if (!isLoggedIn.value) {
    uni.showModal({
      title: '提示',
      content: '请先登录后再使用 AI 助手',
      confirmText: '去登录',
      success: (res) => {
        if (res.confirm) {
          uni.navigateTo({ url: '/pages/login/index' })
        } else {
          uni.switchTab({ url: '/pages/profile/index' })
        }
      }
    })
    return
  }

  // 获取配额信息
  await fetchQuota()

  // 已登录，加载对话历史
  await loadRecentConversation()
  initialized.value = true
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
        timestamp: m.timestamp
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

// 切换深度思考（需要用户确认）
const toggleDeepThink = () => {
  if (!enableDeepThink.value) {
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
  } else {
    enableDeepThink.value = false
    uni.showToast({ title: '深度思考已关闭', icon: 'none' })
  }
}

// 切换联网搜索
const toggleWebSearch = () => {
  if (enableWebSearch.value) {
    // 关闭搜索
    enableWebSearch.value = false
    uni.showToast({ title: '联网搜索已关闭', icon: 'none' })
  } else {
    // 开启前显示安全提示
    uni.showModal({
      title: '联网搜索提示',
      content: '开启联网搜索后，AI将从互联网获取实时信息。\n\n⚠️ 注意：\n- 联网搜索会消耗搜索次数配额\n- 搜索结果来自第三方平台，内容仅供参考\n- 请勿完全依赖搜索结果进行重要决策\n- 建议核实重要信息的准确性',
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

// 切换思考过程展开/折叠
const toggleThinking = (index) => {
  if (messages.value[index] && messages.value[index].thinking) {
    messages.value[index].thinkingExpanded = !messages.value[index].thinkingExpanded
    // 强制触发响应式更新
    messages.value = [...messages.value]
  }
}

// 处理滚动
const handleScroll = (e) => {
  // 可以在这里处理滚动事件
}

// 强制刷新 scroll-view（解决内容更新后不渲染问题）
const refreshScrollView = async () => {
  // 通过设置空值触发 scroll-view 更新
  scrollToView.value = ''
  await nextTick()
}

// 滚动到底部（带自动刷新和多次尝试）
const scrollToBottom = async () => {
  await nextTick()
  await new Promise(resolve => setTimeout(resolve, 50))

  // 使用 scroll-into-view 滚动到最后一个消息
  const lastIndex = messages.value.length - 1
  if (lastIndex >= 0) {
    scrollToView.value = 'msg-' + lastIndex
  }

  // 多次尝试确保滚动成功
  for (let i = 0; i < 3; i++) {
    await new Promise(resolve => setTimeout(resolve, 50 + i * 30))
    scrollTop.value = 999999
  }
}

// 发送消息
const sendMessage = async () => {
  if (!inputMessage.value.trim() || sending.value || aiThinking.value) return
  if (!isLoggedIn.value) {
    uni.navigateTo({ url: '/pages/login/index' })
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
  scrollToBottom()

  // 更新时间戳显示
  if (messages.value.length > 1) {
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
  sending.value = true
  aiThinking.value = true

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

    // 弹出错误提示框
    uni.showModal({
      title: '提示',
      content: 'AI 响应失败，请联系超级管理员',
      showCancel: false,
      confirmText: '确定'
    })
  } finally {
    sending.value = false
    aiThinking.value = false
  }

  // 在 finally 块之后，确保 AI 响应显示后滚动到底部
  // syncRequest 内部已经会滚动，这里再次确保
  await new Promise(resolve => setTimeout(resolve, 200))
  await scrollToBottom()
}

// 发送消息（普通请求 + 前端打字机效果）
const streamMessage = async (conversationId, messageHistory, aiMsg) => {
  const requestBody = {
    messages: messageHistory,
    enableDeepThink: enableDeepThink.value,
    enableWebSearch: enableWebSearch.value
  }

  // 直接使用同步请求，然后在前端实现打字机效果
  await syncRequest(requestBody, aiMsg, conversationId)
}

// 打字机效果：逐字显示文本
const typeWriter = async (text, callback) => {
  let displayedText = ''
  const typingSpeed = 50 // 打字速度（毫秒），越小越快
  let scrollCounter = 0

  for (let i = 0; i < text.length; i++) {
    displayedText += text[i]
    // 更新最后一条 AI 消息
    const lastIndex = messages.value.length - 1
    if (lastIndex >= 0 && messages.value[lastIndex].role === 1) {
      messages.value[lastIndex].content = displayedText
      messages.value = [...messages.value]
    }
    // 每 50 个字符滚动一次，避免过于频繁
    scrollCounter++
    if (scrollCounter >= 50) {
      scrollCounter = 0
      await scrollToBottom()
    }
    // 延迟
    await new Promise(resolve => setTimeout(resolve, typingSpeed))
  }

  // 最后再滚动一次确保到位
  await scrollToBottom()

  if (callback) {
    await callback()
  }
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

// 同步请求（带前端打字机效果）
const syncRequest = async (requestBody, aiMsg, conversationId) => {
  try {
    const token = getToken()
    const response = await fetch(`${BASE_URL}/api/ai/chat`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json;charset=utf-8',
        'Authorization': token ? `Bearer ${token}` : ''
      },
      body: JSON.stringify(requestBody)
    })

    // 先获取文本，确保编码正确
    const responseText = await response.text()
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
      const fullContent = data.response || '抱歉，我暂时无法回答这个问题。'
      const thinkingContent = data.thinking || '' // 思考过程

      // 先用打字机效果显示思考过程
      const lastIndex = messages.value.length - 1
      if (lastIndex >= 0) {
        messages.value[lastIndex].thinkingExpanded = true
      }

      // 思考过程打字机效果
      let displayedThinking = ''
      const thinkingSpeed = 50
      let scrollCounter = 0
      for (let i = 0; i < thinkingContent.length; i++) {
        displayedThinking += thinkingContent[i]
        if (lastIndex >= 0) {
          messages.value[lastIndex].thinking = displayedThinking
          messages.value = [...messages.value]
        }
        // 每 50 个字符滚动一次
        scrollCounter++
        if (scrollCounter >= 50) {
          scrollCounter = 0
          await scrollToBottom()
        }
        await new Promise(resolve => setTimeout(resolve, thinkingSpeed))
      }
      // 最后滚动一次确保到位
      await scrollToBottom()

      // 使用打字机效果逐字显示内容
      await typeWriter(fullContent, async () => {
        // 打字完成后的回调
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
        // 保存 AI 响应到 IndexedDB
        if (conversationId) {
          await addMessageToConversation(conversationId, {
            role: 'assistant',
            content: fullContent
          })
        }
        // 强制刷新并滚动到底部
        await refreshScrollView()
        await scrollToBottom()
      })
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
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 30rpx;
  padding-top: calc(30rpx + env(safe-area-inset-top));
  background-color: #fff;
  border-bottom: 1rpx solid #eee;
  flex-shrink: 0;
  flex: 0 0 auto;

  .header-left {
    flex: 1;

    .chat-title {
      display: block;
      font-size: 36rpx;
      font-weight: bold;
      color: #333;
    }

    .chat-subtitle {
      display: block;
      font-size: 24rpx;
      color: #999;
      margin-top: 8rpx;
    }
  }

  .header-right {
    padding: 10rpx;

    .close-icon {
      font-size: 32rpx;
      color: #999;
    }
  }
}

/* 消息区域 - 只占中间空间，可滚动 */
.messages-container {
  flex: 1;
  width: 100%;
  min-height: 0;
  padding: 20rpx;
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
  }

  .message-text {
    padding: 20rpx 24rpx;
    font-size: 28rpx;
    line-height: 1.6;
  }
}

/* 思考过程框 */
.thinking-box {
  background-color: #f8f9fa;
  border-radius: 12rpx;
  margin-bottom: 16rpx;
  overflow: hidden;
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

  .thinking-arrow {
    font-size: 20rpx;
    color: #999;
    transition: transform 0.3s ease;

    &.expanded {
      transform: rotate(180deg);
    }
  }
}

.thinking-content {
  padding: 16rpx;
  border-top: 1rpx solid #e8f4fd;

  text {
    font-size: 24rpx;
    color: #666;
    line-height: 1.6;
  }
}

/* 输入区域 - 固定在底部，不随滚动 */
.input-area {
  background-color: #f5f5f5;
  padding: 20rpx;
  padding-left: 20rpx;
  padding-right: 20rpx;
  padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
  border-top: 1rpx solid #eee;
  flex: 0 0 auto;
}

/* 深度思考行 */
.deep-think-row {
  display: flex;
  align-items: center;
  gap: 16rpx;
  margin-bottom: 16rpx;
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
  max-height: 200rpx;
  min-height: 72rpx;
  background-color: #fff;
  border-radius: 8rpx;
  padding: 16rpx 20rpx;
  font-size: 28rpx;
  line-height: 1.5;
}

/* 发送按钮 */
.send-btn {
  width: 120rpx;
  height: 72rpx;
  background-color: #4CD964;
  border-radius: 8rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;

  &.disabled {
    background-color: #ccc;
  }

  .send-text {
    font-size: 28rpx;
    color: #fff;
  }
}
</style>
