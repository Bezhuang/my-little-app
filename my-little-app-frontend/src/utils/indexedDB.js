/**
 * 本地存储工具类 - 用于存储 AI 对话历史
 * 兼容 H5 和微信小程序环境
 */

// 检测是否在微信小程序环境中
const isMP = typeof uni !== 'undefined' && uni.getSystemInfoSync

const STORAGE_KEY = 'bezhuang_ai_conversations'

let conversationsCache = null

/**
 * 从本地存储获取所有对话
 */
export const getAllConversations = async () => {
  try {
    // 优先使用缓存
    if (conversationsCache) {
      return conversationsCache
    }

    let data = null

    if (isMP) {
      // 微信小程序环境
      data = uni.getStorageSync(STORAGE_KEY)
    } else {
      // H5 环境
      data = localStorage.getItem(STORAGE_KEY)
    }

    conversationsCache = data ? JSON.parse(data) : []

    // 按更新时间降序排序
    return conversationsCache.sort((a, b) =>
      new Date(b.updatedAt) - new Date(a.updatedAt)
    )
  } catch (error) {
    console.error('获取对话列表失败:', error)
    return []
  }
}

/**
 * 获取单个对话
 */
export const getConversation = async (id) => {
  try {
    const conversations = await getAllConversations()
    return conversations.find(c => c.id === id) || null
  } catch (error) {
    console.error('获取对话失败:', error)
    return null
  }
}

/**
 * 保存对话（新增或更新）
 */
export const saveConversation = async (conversation) => {
  try {
    const conversations = await getAllConversations()
    const now = new Date().toISOString()
    conversation.updatedAt = now

    if (conversation.id) {
      // 更新现有对话
      const index = conversations.findIndex(c => c.id === conversation.id)
      if (index !== -1) {
        conversations[index] = conversation
      } else {
        conversations.unshift(conversation)
      }
    } else {
      // 新建对话
      conversation.id = Date.now().toString()
      conversation.createdAt = now
      conversations.unshift(conversation)
    }

    conversationsCache = conversations

    if (isMP) {
      uni.setStorageSync(STORAGE_KEY, JSON.stringify(conversations))
    } else {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(conversations))
    }

    return conversation.id
  } catch (error) {
    console.error('保存对话失败:', error)
    throw error
  }
}

/**
 * 删除对话
 */
export const deleteConversation = async (id) => {
  try {
    let conversations = await getAllConversations()
    conversations = conversations.filter(c => c.id !== id)
    conversationsCache = conversations

    if (isMP) {
      uni.setStorageSync(STORAGE_KEY, JSON.stringify(conversations))
    } else {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(conversations))
    }
  } catch (error) {
    console.error('删除对话失败:', error)
    throw error
  }
}

/**
 * 清空所有对话
 */
export const clearAllConversations = async () => {
  try {
    conversationsCache = []

    if (isMP) {
      uni.removeStorageSync(STORAGE_KEY)
    } else {
      localStorage.removeItem(STORAGE_KEY)
    }
  } catch (error) {
    console.error('清空对话失败:', error)
    throw error
  }
}

/**
 * 创建新对话
 */
export const createConversation = async (systemPrompt = null) => {
  const conversation = {
    title: '新对话',
    messages: [],
    maxMessages: 10,
    systemPrompt: systemPrompt || '你是 Bezhuang AI，一个智能助手。请用简洁清晰的语言回答用户的问题。'
  }
  return await saveConversation(conversation)
}

/**
 * 添加消息到对话
 */
export const addMessageToConversation = async (conversationId, message) => {
  const conversation = await getConversation(conversationId)
  if (!conversation) {
    throw new Error('对话不存在')
  }

  conversation.messages.push({
    ...message,
    timestamp: new Date().toISOString()
  })

  // 如果是第一条用户消息，设置标题
  if (conversation.messages.length === 1 && message.role === 'user') {
    const title = message.content.length > 20
      ? message.content.substring(0, 20) + '...'
      : message.content
    conversation.title = title
  }

  return await saveConversation(conversation)
}

/**
 * 更新对话中的最后一条消息（用于流式响应）
 */
export const updateLastMessage = async (conversationId, content, reasoningContent = null) => {
  const conversation = await getConversation(conversationId)
  if (!conversation || conversation.messages.length === 0) {
    return
  }

  const lastMessage = conversation.messages[conversation.messages.length - 1]
  if (lastMessage.role === 'assistant' || lastMessage.role === 1) {
    lastMessage.content = content
    if (reasoningContent) {
      lastMessage.reasoningContent = reasoningContent
    }
  }

  await saveConversation(conversation)
}

export default {
  openDB: getAllConversations,
  saveConversation,
  getAllConversations,
  getConversation,
  deleteConversation,
  clearAllConversations,
  createConversation,
  addMessageToConversation,
  updateLastMessage
}
