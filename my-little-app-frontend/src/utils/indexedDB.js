/**
 * IndexedDB 工具类 - 用于本地存储 AI 对话历史
 */

const DB_NAME = 'BezhuangAI'
const DB_VERSION = 1
const STORE_NAME = 'conversations'

let db = null

/**
 * 打开数据库
 */
export const openDB = () => {
  return new Promise((resolve, reject) => {
    if (db) {
      resolve(db)
      return
    }

    const request = indexedDB.open(DB_NAME, DB_VERSION)

    request.onerror = () => {
      reject(new Error('打开数据库失败'))
    }

    request.onsuccess = (event) => {
      db = event.target.result
      resolve(db)
    }

    request.onupgradeneeded = (event) => {
      const database = event.target.result
      if (!database.objectStoreNames.contains(STORE_NAME)) {
        const store = database.createObjectStore(STORE_NAME, { keyPath: 'id', autoIncrement: true })
        store.createIndex('updatedAt', 'updatedAt', { unique: false })
      }
    }
  })
}

/**
 * 保存对话
 */
export const saveConversation = async (conversation) => {
  const database = await openDB()
  return new Promise((resolve, reject) => {
    const transaction = database.transaction([STORE_NAME], 'readwrite')
    const store = transaction.objectStore(STORE_NAME)
    conversation.updatedAt = new Date().toISOString()

    const request = store.put(conversation)

    request.onsuccess = () => {
      resolve(request.result)
    }

    request.onerror = () => {
      reject(new Error('保存对话失败'))
    }
  })
}

/**
 * 获取所有对话
 */
export const getAllConversations = async () => {
  const database = await openDB()
  return new Promise((resolve, reject) => {
    const transaction = database.transaction([STORE_NAME], 'readonly')
    const store = transaction.objectStore(STORE_NAME)
    const index = store.index('updatedAt')

    const request = index.getAll()

    request.onsuccess = () => {
      // 按更新时间降序排序
      const conversations = request.result.sort((a, b) =>
        new Date(b.updatedAt) - new Date(a.updatedAt)
      )
      resolve(conversations)
    }

    request.onerror = () => {
      reject(new Error('获取对话列表失败'))
    }
  })
}

/**
 * 获取单个对话
 */
export const getConversation = async (id) => {
  const database = await openDB()
  return new Promise((resolve, reject) => {
    const transaction = database.transaction([STORE_NAME], 'readonly')
    const store = transaction.objectStore(STORE_NAME)

    const request = store.get(id)

    request.onsuccess = () => {
      resolve(request.result)
    }

    request.onerror = () => {
      reject(new Error('获取对话失败'))
    }
  })
}

/**
 * 删除对话
 */
export const deleteConversation = async (id) => {
  const database = await openDB()
  return new Promise((resolve, reject) => {
    const transaction = database.transaction([STORE_NAME], 'readwrite')
    const store = transaction.objectStore(STORE_NAME)

    const request = store.delete(id)

    request.onsuccess = () => {
      resolve()
    }

    request.onerror = () => {
      reject(new Error('删除对话失败'))
    }
  })
}

/**
 * 清空所有对话
 */
export const clearAllConversations = async () => {
  const database = await openDB()
  return new Promise((resolve, reject) => {
    const transaction = database.transaction([STORE_NAME], 'readwrite')
    const store = transaction.objectStore(STORE_NAME)

    const request = store.clear()

    request.onsuccess = () => {
      resolve()
    }

    request.onerror = () => {
      reject(new Error('清空对话失败'))
    }
  })
}

/**
 * 创建新对话
 */
export const createConversation = async (systemPrompt = null) => {
  const conversation = {
    title: '新对话',
    messages: [],
    maxMessages: 10,
    systemPrompt: systemPrompt || '你是 Bezhuang AI，一个智能助手。请用简洁清晰的语言回答用户的问题。',
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString()
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
  if (lastMessage.role === 'assistant') {
    lastMessage.content = content
    if (reasoningContent) {
      lastMessage.reasoningContent = reasoningContent
    }
  }

  await saveConversation(conversation)
}

export default {
  openDB,
  saveConversation,
  getAllConversations,
  getConversation,
  deleteConversation,
  clearAllConversations,
  createConversation,
  addMessageToConversation,
  updateLastMessage
}
