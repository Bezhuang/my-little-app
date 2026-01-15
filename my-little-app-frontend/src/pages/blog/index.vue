<template>
  <view class="moments-page">
    <!-- 顶部栏 -->
    <view class="blog-header">
      <view class="header-left">
        <text class="blog-title">{{ appName }} - 想法</text>
        <text class="blog-subtitle">分享生活点滴</text>
      </view>
    </view>

    <!-- 想法列表 -->
    <scroll-view
      scroll-y
      class="moments-list"
      :scroll-top="scrollTop"
      :refresher-enabled="true"
      :refresher-triggered="isRefreshing"
      @refresherrefresh="onRefresh"
      @scrolltolower="loadMore"
    >
      <!-- 朋友圈列表 -->
      <view
        v-for="(item, index) in thoughts"
        :key="item.id || index"
        class="moment-item"
      >
        <!-- 文字内容 -->
        <view class="moment-content">
          <text>{{ item.content }}</text>
        </view>

        <!-- 图片展示 -->
        <view
          v-if="item.imageUrlList && item.imageUrlList.length"
          class="moment-images"
          :class="`images-${Math.min(item.imageUrlList.length, 3)}`"
        >
          <image
            v-for="(img, imgIndex) in item.imageUrlList.slice(0, 9)"
            :key="imgIndex"
            :src="img"
            mode="aspectFill"
            class="moment-image"
            @click="previewImage(item.imageUrlList, imgIndex)"
          />
        </view>

        <!-- 时间（右下角） -->
        <view class="moment-time-bottom">
          <text>{{ formatTime(item.createTime) }}</text>
        </view>
      </view>

      <!-- 加载状态 -->
      <view v-if="thoughts.length > 0" class="load-more">
        <view v-if="loadStatus === 'loading'" class="loading">
          <view class="loading-dot"></view>
          <view class="loading-dot"></view>
          <view class="loading-dot"></view>
        </view>
        <text v-else-if="loadStatus === 'noMore'" class="load-text">没有更多了</text>
        <text v-else class="load-text">上拉加载更多</text>
      </view>

      <!-- 空状态 -->
      <view v-if="thoughts.length === 0 && !loading" class="empty-state">
        <uni-icons type="chatbubble" size="60" color="#ddd" />
        <text class="empty-text">暂无想法</text>
        <text class="empty-hint">管理员还没发布任何想法</text>
      </view>
    </scroll-view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { publicThoughtApi } from '../../utils/api'
import { BASE_URL } from '../../constant'

const thoughts = ref([])
const loading = ref(false)
const isRefreshing = ref(false)
const hasMore = ref(true)
const currentPage = ref(1)
const pageSize = ref(10)
const scrollTop = ref(0) // 滚动位置
const initialized = ref(false) // 标记是否已初始化
const appName = ref('My Little App')

// 获取应用名称
const fetchAppName = async () => {
  try {
    const res = await uni.request({
      url: `${BASE_URL}/api/setup/app-name`,
      method: 'GET'
    })
    if (res.statusCode === 200 && res.data && res.data.success) {
      appName.value = res.data.data
    }
  } catch (error) {
    console.error('获取应用名称失败:', error)
  }
}

const loadStatus = computed(() => {
  if (loading.value) return 'loading'
  return hasMore.value ? 'more' : 'noMore'
})

// 获取公开想法列表
const fetchThoughts = async (refresh = false) => {
  if (loading.value) return

  loading.value = true

  if (refresh) {
    currentPage.value = 1
    hasMore.value = true
  }

  try {
    const res = await publicThoughtApi.getPublicList({
      page: currentPage.value,
      pageSize: pageSize.value
    })

    if (res.data) {
      const list = (res.data.list || []).map(t => ({
        ...t,
        imageUrlList: t.imageIds ? t.imageIds.split(',').filter(Boolean).map(id => `${BASE_URL}/api/admin/images/public/${id}/data`) : []
      }))

      if (refresh) {
        thoughts.value = list
      } else {
        thoughts.value = [...thoughts.value, ...list]
      }

      hasMore.value = list.length >= pageSize.value
      if (hasMore.value) {
        currentPage.value++
      }
    }
  } catch (error) {
    console.log('获取想法列表失败', error)
    // 401 未登录错误，弹出提示框引导用户登录
    if (error.code === 401 || error.message?.includes('请先登录')) {
      uni.showModal({
        title: '提示',
        content: '请先登录后再查看想法',
        confirmText: '去登录',
        cancelText: '取消',
        success: (res) => {
          if (res.confirm) {
            uni.navigateTo({ url: '/pages/login/index' })
          } else {
            // 点击取消，跳转到"我的"页面
            uni.switchTab({ url: '/pages/profile/index' })
          }
        }
      })
    } else {
      uni.showToast({
        title: error.message || '获取想法失败',
        icon: 'none'
      })
    }
  } finally {
    loading.value = false
    isRefreshing.value = false
    // 只在首次加载时滚动到顶部
    if (!initialized.value) {
      initialized.value = true
      scrollTop.value = 0
    }
  }
}

// 刷新
const onRefresh = () => {
  isRefreshing.value = true
  fetchThoughts(true)
}

// 加载更多
const loadMore = () => {
  if (hasMore.value && !loading.value) {
    fetchThoughts()
  }
}

// 预览图片
const previewImage = (images, index) => {
  uni.previewImage({
    urls: images,
    current: index
  })
}

// 格式化时间
const formatTime = (time) => {
  if (!time) return ''

  const date = new Date(time)
  const now = new Date()
  const diff = (now - date) / 1000

  if (diff < 60) return '刚刚'
  if (diff < 3600) return `${Math.floor(diff / 60)}分钟前`
  if (diff < 86400) return `${Math.floor(diff / 3600)}小时前`
  if (diff < 604800) return `${Math.floor(diff / 86400)}天前`

  const year = date.getFullYear()
  const month = date.getMonth() + 1
  const day = date.getDate()
  return `${year}-${month}-${day}`
}

onShow(() => {
  // 刷新想法列表（用户可能发布了新想法）
  fetchThoughts(true)
})

onMounted(async () => {
  await fetchAppName()
  await fetchThoughts(true)
})
</script>

<style lang="scss" scoped>
// 顶部栏 - 全屏覆盖
.blog-header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  padding: 30rpx;
  padding-top: calc(env(safe-area-inset-top) + 60rpx);
  padding-left: calc(30rpx + env(safe-area-inset-left));
  padding-right: calc(30rpx + env(safe-area-inset-right));
  padding-bottom: 20rpx;
  background-color: #fff;
  border-bottom: 1rpx solid #eee;
  z-index: 999;
}

/* PC端适配 */
@media screen and (min-width: 768px) {
  .blog-header {
    left: 50%;
    right: auto;
    transform: translateX(-50%);
    width: 100%;
    max-width: 800px;
    padding-left: 30rpx;
    padding-right: 30rpx;
  }
}

.blog-header .header-left {
  flex: 1;
}

.blog-header .header-left .blog-title {
  display: block;
  font-size: 40rpx;
  font-weight: bold;
  color: #333;
}

.blog-header .header-left .blog-subtitle {
  display: block;
  font-size: 24rpx;
  color: #999;
  margin-top: 8rpx;
}

.moments-page {
  min-height: 100vh;
  background-color: #f5f5f5;
  padding: 20rpx;
  padding-top: calc(210rpx + env(safe-area-inset-top));
  padding-bottom: 40rpx;
}

/* PC端适配 */
@media screen and (min-width: 768px) {
  .moments-page {
    padding-left: 20rpx;
    padding-right: 20rpx;
  }
}

.moments-list {
  padding-bottom: 40rpx;
}

.moment-item {
  background: #fff;
  border-radius: 16rpx;
  padding: 30rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.05);
  position: relative;
}

.moment-content {
  font-size: 36rpx;
  font-weight: bold;
  color: #333;
  line-height: 1.6;
  margin-bottom: 20rpx;
}

.moment-images {
  display: grid;
  gap: 8rpx;
  margin-bottom: 20rpx;
}

.moment-images.images-1 {
  grid-template-columns: 1fr;
}

.moment-images.images-2,
.moment-images.images-4 {
  grid-template-columns: repeat(2, 1fr);
}

.moment-images.images-3,
.moment-images.images-5,
.moment-images.images-6,
.moment-images.images-7,
.moment-images.images-8,
.moment-images.images-9 {
  grid-template-columns: repeat(3, 1fr);
}

.moment-image {
  width: 100%;
  aspect-ratio: 1;
  border-radius: 8rpx;
}

// 时间（右下角）
.moment-time-bottom {
  text-align: right;
  font-size: 24rpx;
  color: #999;
  margin-top: 10rpx;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 100rpx 0;
}

.empty-text {
  font-size: 28rpx;
  color: #999;
  margin-top: 20rpx;
}

.empty-hint {
  font-size: 24rpx;
  color: #bbb;
  margin-top: 10rpx;
}

// 加载更多
.load-more {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 30rpx 0;
}

.load-text {
  font-size: 26rpx;
  color: #999;
}

.loading {
  display: flex;
  align-items: center;
  gap: 8rpx;
}

.loading-dot {
  width: 16rpx;
  height: 16rpx;
  background-color: #999;
  border-radius: 50%;
  animation: loadingPulse 1.4s infinite ease-in-out;

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

@keyframes loadingPulse {
  0%, 80%, 100% {
    transform: scale(0.6);
    opacity: 0.5;
  }
  40% {
    transform: scale(1);
    opacity: 1;
  }
}
</style>
