<script setup>
import { ref, computed } from 'vue'

const tools = ref([
  {
    id: 'opinion',
    name: '观点',
    iconChar: '📝',
    description: 'Bezhuang的长文写作',
    color: '#25b864',
    url: 'https://www.yuque.com/bezhuang/writing'
  },
  {
    id: 'unit',
    name: '开发环境配置',
    iconChar: '⚙️',
    description: '语雀文档',
    color: '#DDA0DD',
    url: 'https://www.yuque.com/bezhuang/knowledge/qq096ivi8gkvux83'
  },
  {
    id: 'index-valuation',
    name: '指数估值',
    iconChar: '📈',
    description: '基金估值',
    color: '#FF6B6B',
    url: 'https://danjuanfunds.com/djmodule/value-center'
  },
  {
    id: 'whiteboard',
    name: '白板',
    iconChar: '✏️',
    description: '绘图工具',
    color: '#4ECDC4',
    // #ifdef MP-WEIXIN
    url: 'https://app.diagrams.net/'
    // #endif
    // #ifndef MP-WEIXIN
    url: 'https://excalidraw.com/'
    // #endif
  },
  {
    id: 'metronome',
    name: '节拍器',
    iconChar: '🎵',
    description: '在线节拍器',
    color: '#FF6B6B',
    url: 'https://metronome-online.com/zh'
  },
  {
    id: 'calendar',
    name: '日历',
    iconChar: '📅',
    description: '查看日期',
    color: '#4ECDC4'
  },
  {
    id: 'notes',
    name: '备忘录',
    iconChar: '📝',
    description: '快速记录',
    color: '#45B7D1'
  },
  {
    id: 'qrcode',
    name: '二维码',
    iconChar: '📱',
    description: '生成二维码',
    color: '#96CEB4'
  },
  {
    id: 'password',
    name: '密码生成',
    iconChar: '🔐',
    description: '安全密码',
    color: '#FFEAA7'
  }
])

const currentTool = ref(null)
const showWebView = ref(false)
const iframeRef = ref(null)
const showCalendar = ref(false)
const showStopwatch = ref(false)

// 秒表相关数据
const stopwatchTime = ref(0)
const stopwatchRunning = ref(false)
const stopwatchInterval = ref(null)
const lapTimes = ref([])

const formattedStopwatchTime = computed(() => {
  const hours = Math.floor(stopwatchTime.value / 3600000)
  const minutes = Math.floor((stopwatchTime.value % 3600000) / 60000)
  const seconds = Math.floor((stopwatchTime.value % 60000) / 1000)
  const ms = Math.floor((stopwatchTime.value % 1000) / 10)

  if (hours > 0) {
    return `${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`
  }
  return `${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}.${ms.toString().padStart(2, '0')}`
})

const startStopwatch = () => {
  if (stopwatchRunning.value) {
    // 暂停
    stopwatchRunning.value = false
    clearInterval(stopwatchInterval.value)
  } else {
    // 开始
    stopwatchRunning.value = true
    const startTime = Date.now() - stopwatchTime.value
    stopwatchInterval.value = setInterval(() => {
      stopwatchTime.value = Date.now() - startTime
    }, 10)
  }
}

const resetStopwatch = () => {
  stopwatchRunning.value = false
  clearInterval(stopwatchInterval.value)
  stopwatchTime.value = 0
  lapTimes.value = []
}

const recordLap = () => {
  if (stopwatchRunning.value && lapTimes.value.length < 99) {
    lapTimes.value.unshift({
      index: lapTimes.value.length + 1,
      time: formattedStopwatchTime.value
    })
  }
}

const closeStopwatch = () => {
  stopwatchRunning.value = false
  clearInterval(stopwatchInterval.value)
  stopwatchTime.value = 0
  lapTimes.value = []
  showStopwatch.value = false
  currentTool.value = null
}
const currentDate = ref(new Date())
const weekDays = ['日', '一', '二', '三', '四', '五', '六']

const calendarTitle = computed(() => {
  const year = currentDate.value.getFullYear()
  const month = currentDate.value.getMonth() + 1
  return `${year}年${month}月`
})

const calendarDays = computed(() => {
  const year = currentDate.value.getFullYear()
  const month = currentDate.value.getMonth()
  const firstDay = new Date(year, month, 1)
  const lastDay = new Date(year, month + 1, 0)
  const daysInMonth = lastDay.getDate()
  const startDayOfWeek = firstDay.getDay()

  const days = []

  // 填充月初空白
  for (let i = 0; i < startDayOfWeek; i++) {
    days.push({ day: '', isCurrentMonth: false })
  }

  // 填充日期
  const today = new Date()
  for (let i = 1; i <= daysInMonth; i++) {
    const date = new Date(year, month, i)
    const isToday = today.getFullYear() === year &&
                    today.getMonth() === month &&
                    today.getDate() === i
    days.push({
      day: i,
      isCurrentMonth: true,
      isToday
    })
  }

  return days
})

const prevMonth = () => {
  currentDate.value = new Date(currentDate.value.getFullYear(), currentDate.value.getMonth() - 1, 1)
}

const nextMonth = () => {
  currentDate.value = new Date(currentDate.value.getFullYear(), currentDate.value.getMonth() + 1, 1)
}

const handleToolClick = (tool) => {
  if (tool.id === 'calendar') {
    currentTool.value = tool
    showCalendar.value = true
  } else if (tool.id === 'stopwatch') {
    currentTool.value = tool
    showStopwatch.value = true
  } else if (tool.url) {
    // #ifdef H5
    // H5/PC 跳转到 webview 页面
    uni.navigateTo({
      url: `/pages/webview/index?url=${encodeURIComponent(tool.url)}&title=${encodeURIComponent(tool.name)}`
    })
    // #endif
    // #ifdef MP-WEIXIN
    // 小程序显示提示
    uni.showModal({
      title: '提示',
      content: '如需浏览，请长按网址复制后使用浏览器访问 https://bezhuang.cn',
      showCancel: false,
      confirmText: '我知道了'
    })
    // #endif
  } else {
    uni.showToast({
      title: `${tool.name} 功能开发中`,
      icon: 'none'
    })
  }
}

const closeWebView = () => {
  showWebView.value = false
  currentTool.value = null
}

const closeCalendar = () => {
  showCalendar.value = false
  currentTool.value = null
}

const reloadPage = () => {
  if (iframeRef.value) {
    iframeRef.value.src = currentTool.value.url
  }
}
</script>

<template>
  <div class="tools-page">
    <div class="page-header">
      <h1 class="page-title">工具集</h1>
      <p class="page-subtitle">实用小工具箱</p>
    </div>

    <div class="tools-grid">
      <div
        v-for="tool in tools"
        :key="tool.id"
        class="tool-card"
        @click="handleToolClick(tool)"
      >
        <div class="tool-icon" :style="{ background: tool.color + '20' }">
          <img
            v-if="tool.favicon"
            :src="tool.favicon"
            class="tool-logo"
            :alt="tool.name"
          />
          <text v-else class="tool-icon-text">{{ tool.iconChar }}</text>
        </div>
        <div class="tool-info">
          <div class="tool-name">{{ tool.name }}</div>
          <div class="tool-desc">{{ tool.description }}</div>
        </div>
      </div>
    </div>

    <!-- #ifndef MP-WEIXIN -->
    <!-- 网页嵌入层 - H5 使用 iframe -->
    <div v-if="showWebView" class="webview-overlay">
      <div class="webview-container">
        <div class="webview-header">
          <div class="header-left">
            <div class="back-btn" @click="closeWebView">
              <uni-icons type="left" size="18" color="#666"></uni-icons>
            </div>
            <div class="refresh-btn" @click="reloadPage">
              <uni-icons type="refresh" size="18" color="#666"></uni-icons>
            </div>
          </div>
          <text class="webview-title">{{ currentTool?.name }}</text>
          <div class="close-btn" @click="closeWebView">
            <uni-icons type="close" size="18" color="#666"></uni-icons>
          </div>
        </div>
        <iframe
          ref="iframeRef"
          class="webview-iframe"
          :src="currentTool?.url"
          allow="accelerometer; gyroscope; autoplay; fullscreen; picture-in-picture"
          allowfullscreen
        ></iframe>
      </div>
    </div>
    <!-- #endif -->

    <!-- 日历弹窗 -->
    <div v-if="showCalendar" class="calendar-overlay">
      <div class="calendar-container">
        <div class="calendar-header">
          <div class="calendar-nav-btn" @click="prevMonth">
            <uni-icons type="left" size="18" color="#666"></uni-icons>
          </div>
          <text class="calendar-title">{{ calendarTitle }}</text>
          <div class="calendar-nav-btn" @click="nextMonth">
            <uni-icons type="right" size="18" color="#666"></uni-icons>
          </div>
        </div>

        <div class="calendar-weekdays">
          <text v-for="day in weekDays" :key="day" class="weekday">{{ day }}</text>
        </div>

        <div class="calendar-grid">
          <div
            v-for="(item, index) in calendarDays"
            :key="index"
            class="calendar-day"
            :class="{
              'other-month': !item.isCurrentMonth,
              'today': item.isToday
            }"
          >
            <text>{{ item.day }}</text>
          </div>
        </div>

        <div class="calendar-footer">
          <div class="today-info">
            <text>今天: {{ new Date().toLocaleDateString('zh-CN') }}</text>
          </div>
          <div class="close-calendar-btn" @click="closeCalendar">关闭</div>
        </div>
      </div>
    </div>

    <!-- 秒表弹窗 -->
    <div v-if="showStopwatch" class="stopwatch-overlay">
      <div class="stopwatch-container">
        <div class="stopwatch-header">
          <text class="stopwatch-title">秒表</text>
          <div class="close-btn" @click="closeStopwatch">
            <uni-icons type="close" size="18" color="#666"></uni-icons>
          </div>
        </div>

        <div class="stopwatch-display">
          <text class="stopwatch-time">{{ formattedStopwatchTime }}</text>
        </div>

        <div class="stopwatch-controls">
          <div class="stopwatch-btn reset-btn" @click="resetStopwatch">
            <text>复位</text>
          </div>
          <div class="stopwatch-btn lap-btn" @click="recordLap" :class="{ disabled: !stopwatchRunning }">
            <text>计次</text>
          </div>
          <div class="stopwatch-btn start-btn" @click="startStopwatch" :class="{ running: stopwatchRunning }">
            <text>{{ stopwatchRunning ? '暂停' : '开始' }}</text>
          </div>
        </div>

        <div class="lap-times-container" v-if="lapTimes.length > 0">
          <div class="lap-times-header">计次 ({{ lapTimes.length }})</div>
          <div class="lap-times-list">
            <div v-for="lap in lapTimes" :key="lap.index" class="lap-item">
              <text class="lap-index">#{{ lap.index }}</text>
              <text class="lap-time">{{ lap.time }}</text>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.tools-page {
  min-height: 100vh;
  background: #f5f5f5;
  padding: 16px;
  padding-top: calc(120px + env(safe-area-inset-top));
  padding-left: calc(16px + env(safe-area-inset-left));
  padding-right: calc(16px + env(safe-area-inset-right));
  padding-bottom: calc(16px + env(safe-area-inset-bottom));
  box-sizing: border-box;
}

.page-header {
  padding: 16px 8px;
  margin-bottom: 16px;
}

.page-title {
  font-size: 28px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 4px;
}

.page-subtitle {
  font-size: 14px;
  color: #999;
}

.tools-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.tool-card {
  background: #ffffff;
  border-radius: 12px;
  padding: 16px 12px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  transition: all 0.2s;

  &:active {
    transform: scale(0.96);
    box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  }
}

.tool-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.tool-logo {
  width: 32px;
  height: 32px;
  border-radius: 6px;
}

.tool-info {
  text-align: center;
}

.tool-name {
  font-size: 13px;
  font-weight: 500;
  color: #333;
  margin-bottom: 2px;
}

.tool-desc {
  font-size: 11px;
  color: #999;
}

.tool-icon-text {
  font-size: 28px;
  line-height: 1;
}

// 网页嵌入层
.webview-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 999;
}

.webview-container {
  position: absolute;
  top: calc(60px + env(safe-area-inset-top));
  left: 4px;
  right: 4px;
  bottom: 20px;
  background: #ffffff;
  border-radius: 16px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2);
}

.webview-header {
  display: flex;
  align-items: center;
  padding: 14px 18px;
  background: #ffffff;
  border-bottom: 1px solid #eee;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.webview-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  flex: 1;
  text-align: center;
  margin: 0 36px;
}

.webview-actions {
  display: flex;
  gap: 12px;
}

.back-btn,
.refresh-btn,
.close-btn {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  background: #f5f5f5;
  cursor: pointer;
}

.webview-iframe {
  flex: 1;
  width: 100%;
  height: 100%;
  transform: translateZ(0);
}

// PC端适配
@media screen and (min-width: 768px) {
  .tools-page {
    max-width: 800px;
    margin: 0 auto;
    padding: 24px;
  }

  .page-header {
    padding: 24px 16px;
  }

  .page-title {
    font-size: 32px;
  }

  .page-subtitle {
    font-size: 16px;
  }

  .tools-grid {
    grid-template-columns: repeat(6, 1fr);
    gap: 20px;
  }

  .tool-card {
    padding: 20px 16px;
    border-radius: 16px;
  }

  .tool-icon {
    width: 56px;
    height: 56px;
  }

  .tool-logo {
    width: 40px;
    height: 40px;
  }

  .tool-name {
    font-size: 14px;
  }

  .tool-desc {
    font-size: 12px;
  }

  .webview-container {
    top: 20px;
    left: 20px;
    right: 20px;
    bottom: 20px;
    border-radius: 24px;
  }

  .webview-header {
    padding: 16px 24px;
  }

  .refresh-btn,
  .close-btn {
    width: 40px;
    height: 40px;
  }
}

// 日历样式
.calendar-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 999;
  display: flex;
  align-items: center;
  justify-content: center;
}

.calendar-container {
  width: 320px;
  background: #ffffff;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2);
}

.calendar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  background: #f8f8f8;
}

.calendar-nav-btn {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  background: #ffffff;
  cursor: pointer;
}

.calendar-title {
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.calendar-weekdays {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  padding: 12px 8px;
  border-bottom: 1px solid #eee;
}

.weekday {
  text-align: center;
  font-size: 13px;
  font-weight: 500;
  color: #999;
}

.calendar-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  padding: 8px;
  gap: 4px;
}

.calendar-day {
  aspect-ratio: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  color: #333;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    background: #f0f0f0;
  }
}

.calendar-day.other-month {
  color: #ccc;
}

.calendar-day.today {
  background: #4ECDC4;
  color: #ffffff;
  font-weight: 600;
}

.calendar-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: #f8f8f8;
  border-top: 1px solid #eee;
}

.today-info {
  font-size: 12px;
  color: #999;
}

.close-calendar-btn {
  padding: 6px 16px;
  background: #4ECDC4;
  color: #ffffff;
  font-size: 13px;
  border-radius: 6px;
  cursor: pointer;
}

// PC端日历适配
@media screen and (min-width: 768px) {
  .calendar-container {
    width: 400px;
    border-radius: 20px;
  }

  .calendar-header {
    padding: 20px 24px;
  }

  .calendar-title {
    font-size: 20px;
  }

  .calendar-nav-btn {
    width: 36px;
    height: 36px;
  }

  .weekday {
    font-size: 14px;
  }

  .calendar-day {
    font-size: 15px;
  }

  .calendar-footer {
    padding: 16px 20px;
  }

  .close-calendar-btn {
    padding: 8px 20px;
    font-size: 14px;
    border-radius: 8px;
  }
}

// 秒表样式
.stopwatch-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 999;
  display: flex;
  align-items: center;
  justify-content: center;
}

.stopwatch-container {
  width: 320px;
  background: #ffffff;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2);
  max-height: 80vh;
  display: flex;
  flex-direction: column;
}

.stopwatch-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  background: #f8f8f8;
  border-bottom: 1px solid #eee;
}

.stopwatch-title {
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.stopwatch-display {
  padding: 40px 20px;
  text-align: center;
  background: #ffffff;
}

.stopwatch-time {
  font-size: 48px;
  font-weight: 600;
  color: #333;
  font-family: 'SF Mono', 'Consolas', monospace;
  letter-spacing: 2px;
}

.stopwatch-controls {
  display: flex;
  justify-content: center;
  gap: 12px;
  padding: 20px;
  background: #f8f8f8;
  border-top: 1px solid #eee;
}

.stopwatch-btn {
  padding: 12px 24px;
  border-radius: 25px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  text-align: center;
  min-width: 70px;
}

.reset-btn {
  background: #f0f0f0;
  color: #666;
}

.reset-btn:active {
  background: #e0e0e0;
}

.lap-btn {
  background: #f0f0f0;
  color: #666;
}

.lap-btn.disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.lap-btn:active:not(.disabled) {
  background: #e0e0e0;
}

.start-btn {
  background: #FF6B6B;
  color: #ffffff;
}

.start-btn.running {
  background: #4ECDC4;
}

.start-btn:active {
  opacity: 0.9;
}

.lap-times-container {
  flex: 1;
  overflow-y: auto;
  max-height: 200px;
  padding: 12px 20px;
}

.lap-times-header {
  font-size: 13px;
  color: #999;
  margin-bottom: 8px;
}

.lap-times-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.lap-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  background: #f8f8f8;
  border-radius: 8px;
}

.lap-index {
  font-size: 13px;
  color: #999;
}

.lap-time {
  font-size: 14px;
  font-weight: 500;
  color: #333;
  font-family: 'SF Mono', 'Consolas', monospace;
}

// PC端秒表适配
@media screen and (min-width: 768px) {
  .stopwatch-container {
    width: 400px;
    border-radius: 20px;
  }

  .stopwatch-header {
    padding: 20px 24px;
  }

  .stopwatch-title {
    font-size: 20px;
  }

  .stopwatch-display {
    padding: 60px 24px;
  }

  .stopwatch-time {
    font-size: 64px;
  }

  .stopwatch-controls {
    padding: 24px;
    gap: 16px;
  }

  .stopwatch-btn {
    padding: 14px 32px;
    font-size: 15px;
    min-width: 80px;
  }

  .lap-times-container {
    padding: 16px 24px;
    max-height: 250px;
  }
}
</style>
