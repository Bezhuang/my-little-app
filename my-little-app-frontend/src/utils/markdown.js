/**
 * Markdown 解析工具
 * 支持粗体、斜体、代码块、行内代码、链接
 */

/**
 * 解析 Markdown 文本，返回带样式的文本片段
 * @param {string} text - 原始 Markdown 文本
 * @returns {Array} - 文本片段数组 { type, content, lang? }
 */
export function parseMarkdown(text) {
  if (!text) return []

  const segments = []
  let remaining = text

  // 处理代码块 ```code```
  const codeBlockRegex = /```([\s\S]*?)```/g
  let lastIndex = 0
  let match

  while ((match = codeBlockRegex.exec(text)) !== null) {
    // 添加代码块前的普通文本
    if (match.index > lastIndex) {
      segments.push(...parseInlineMarkdown(text.substring(lastIndex, match.index)))
    }

    // 添加代码块
    const codeContent = match[1].trim()
    segments.push({
      type: 'code-block',
      content: codeContent
    })

    lastIndex = codeBlockRegex.lastIndex
  }

  // 添加剩余的普通文本
  if (lastIndex < text.length) {
    segments.push(...parseInlineMarkdown(text.substring(lastIndex)))
  }

  return segments
}

/**
 * 解析行内 Markdown
 * @param {string} text - 文本
 * @returns {Array} - 文本片段数组
 */
function parseInlineMarkdown(text) {
  const segments = []
  let remaining = text
  let index = 0

  while (index < remaining.length) {
    // 处理链接 [text](url)
    const linkMatch = remaining.match(/\[([^\]]+)\]\(([^)]+)\)/)
    if (linkMatch && linkMatch.index === 0) {
      if (index > 0) {
        segments.push({
          type: 'text',
          content: remaining.substring(0, linkMatch.index)
        })
      }
      segments.push({
        type: 'link',
        content: linkMatch[1],
        url: linkMatch[2]
      })
      remaining = remaining.substring(linkMatch.index + linkMatch[0].length)
      index = 0
      continue
    }

    // 处理粗体 **text** 或 __text__
    const boldMatch = remaining.match(/(\*\*|__)([^\1]+?)\1/)
    if (boldMatch && boldMatch.index === 0) {
      if (index > 0) {
        segments.push({
          type: 'text',
          content: remaining.substring(0, boldMatch.index)
        })
      }
      segments.push({
        type: 'bold',
        content: boldMatch[2]
      })
      remaining = remaining.substring(boldMatch.index + boldMatch[0].length)
      index = 0
      continue
    }

    // 处理斜体 *text* 或 _text_
    const italicMatch = remaining.match(/(\*|_)([^\1]+?)\1/)
    if (italicMatch && italicMatch.index === 0) {
      if (index > 0) {
        segments.push({
          type: 'text',
          content: remaining.substring(0, italicMatch.index)
        })
      }
      segments.push({
        type: 'italic',
        content: italicMatch[2]
      })
      remaining = remaining.substring(italicMatch.index + italicMatch[0].length)
      index = 0
      continue
    }

    // 处理行内代码 `code`
    const inlineCodeMatch = remaining.match(/`([^`]+)`/)
    if (inlineCodeMatch && inlineCodeMatch.index === 0) {
      if (index > 0) {
        segments.push({
          type: 'text',
          content: remaining.substring(0, inlineCodeMatch.index)
        })
      }
      segments.push({
        type: 'code',
        content: inlineCodeMatch[1]
      })
      remaining = remaining.substring(inlineCodeMatch.index + inlineCodeMatch[0].length)
      index = 0
      continue
    }

    // 处理删除线 ~~text~~
    const strikeMatch = remaining.match(/~~([^~]+)~~/)
    if (strikeMatch && strikeMatch.index === 0) {
      if (index > 0) {
        segments.push({
          type: 'text',
          content: remaining.substring(0, strikeMatch.index)
        })
      }
      segments.push({
        type: 'strike',
        content: strikeMatch[1]
      })
      remaining = remaining.substring(strikeMatch.index + strikeMatch[0].length)
      index = 0
      continue
    }

    // 处理纯文本中的 URL
    const urlMatch = remaining.match(/^(https?:\/\/[^\s<]+[^<.,:;"')\]\s])/)
    if (urlMatch && urlMatch.index === 0) {
      if (index > 0) {
        segments.push({
          type: 'text',
          content: remaining.substring(0, urlMatch.index)
        })
      }
      segments.push({
        type: 'link',
        content: urlMatch[1],
        url: urlMatch[1]
      })
      remaining = remaining.substring(urlMatch.index + urlMatch[0].length)
      index = 0
      continue
    }

    // 普通字符
    index++
  }

  if (remaining) {
    segments.push({
      type: 'text',
      content: remaining
    })
  }

  return segments
}

/**
 * 将 Markdown 转换为 HTML 字符串（用于 H5）
 * @param {string} text - Markdown 文本
 * @returns {string} - HTML 字符串
 */
export function markdownToHtml(text) {
  if (!text) return ''

  let html = text
    // 代码块
    .replace(/```([\s\S]*?)```/g, '<pre><code>$1</code></pre>')
    // 行内代码
    .replace(/`([^`]+)`/g, '<code>$1</code>')
    // 粗体
    .replace(/\*\*([^*]+?)\*\*/g, '<strong>$1</strong>')
    .replace(/__([^_]+?)__/g, '<strong>$1</strong>')
    // 斜体
    .replace(/\*([^*]+?)\*/g, '<em>$1</em>')
    .replace(/_([^_]+?)_/g, '<em>$1</em>')
    // 删除线
    .replace(/~~([^~]+?)~~/g, '<s>$1</s>')
    // 链接
    .replace(/\[([^\]]+)\]\(([^)]+)\)/g, '<a href="$2" target="_blank">$1</a>')
    // 自动链接
    .replace(/(https?:\/\/[^\s<]+[^<.,:;"')\]\s])/g, '<a href="$1" target="_blank">$1</a>')
    // 换行
    .replace(/\n/g, '<br>')

  return html
}

/**
 * 检测文本中是否包含链接
 * @param {string} text - 文本
 * @returns {Array} - 链接数组
 */
export function extractLinks(text) {
  if (!text) return []

  const links = []
  const urlRegex = /(https?:\/\/[^\s<]+[^<.,:;"')\]\s])/g
  let match

  while ((match = urlRegex.exec(text)) !== null) {
    links.push(match[1])
  }

  return links
}

/**
 * 检测是否为 URL
 * @param {string} str - 字符串
 * @returns {boolean}
 */
export function isUrl(str) {
  if (!str) return false
  return /^https?:\/\/[^\s<]+[^<.,:;"')\]\s]$/.test(str)
}
