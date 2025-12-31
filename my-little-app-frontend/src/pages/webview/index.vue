<script setup>
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'

const webViewUrl = ref('')
const pageTitle = ref('')
const iframeRef = ref(null)

onLoad((options) => {
  webViewUrl.value = decodeURIComponent(options.url || '')
  pageTitle.value = decodeURIComponent(options.title || '网页')
})

// 返回上一页
const navigateBack = () => {
  // #ifdef H5
  if (window.history.length > 1) {
    window.history.back()
  } else {
    uni.switchTab({
      url: '/pages/profile/index'
    })
  }
  // #endif
  // #ifndef H5
  uni.navigateBack()
  // #endif
}

// 关闭页面
const closePage = () => {
  uni.switchTab({
    url: '/pages/profile/index'
  })
}

// 刷新页面
const reloadPage = () => {
  if (iframeRef.value) {
    iframeRef.value.src = webViewUrl.value
  }
}
</script>

<template>
  <div class="webview-page">
    <view class="webview-header">
      <view class="header-left">
        <view class="back-btn" @click="navigateBack">
          <uni-icons type="left" size="20" color="#333"></uni-icons>
        </view>
        <view class="close-btn" @click="closePage">
          <uni-icons type="close" size="20" color="#333"></uni-icons>
        </view>
      </view>
      <text class="webview-title">{{ pageTitle }}</text>
      <view class="refresh-btn" @click="reloadPage">
        <uni-icons type="refresh" size="20" color="#333"></uni-icons>
      </view>
    </view>

    <!-- iframe 嵌入 -->
    <iframe
      v-if="webViewUrl"
      ref="iframeRef"
      class="webview-iframe"
      :src="webViewUrl"
      allow="accelerometer; gyroscope; autoplay; fullscreen; picture-in-picture"
      allowfullscreen
    ></iframe>
  </div>
</template>

<style lang="scss" scoped>
.webview-page {
  width: 100%;
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #ffffff;
}

.webview-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 44px 16px 12px;
  background: #ffffff;
  border-bottom: 1px solid #f0f0f0;
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 100;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.back-btn,
.close-btn,
.refresh-btn {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  background: #f5f5f5;
  cursor: pointer;
}

.webview-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  flex: 1;
  text-align: center;
  margin: 0 36px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.webview-iframe {
  flex: 1;
  width: 100%;
  height: 100%;
  border: none;
  margin-top: 88px;
  transform: translateZ(0);
}

// PC端适配
@media screen and (min-width: 768px) {
  .webview-header {
    max-width: 800px;
    margin: 0 auto;
    left: 50%;
    transform: translateX(-50%);
  }

  .webview-page {
    max-width: 800px;
    margin: 0 auto;
  }
}
</style>
