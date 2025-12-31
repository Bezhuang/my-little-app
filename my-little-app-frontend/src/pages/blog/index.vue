<template>
  <view class="moments-page">
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
      <uni-load-more
        v-if="thoughts.length > 0"
        :status="loadStatus"
        :content-text="loadTextConfig"
      />

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
import { BASE_URL } from '../../utils/constants'

const thoughts = ref([])
const loading = ref(false)
const isRefreshing = ref(false)
const hasMore = ref(true)
const currentPage = ref(1)
const pageSize = ref(10)
const scrollTop = ref(0) // 滚动位置
const initialized = ref(false) // 标记是否已初始化

const loadTextConfig = {
  contentdown: '上拉加载更多',
  contentrefresh: '加载中...',
  contentnomore: '没有更多了'
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
    initialized.value = true
    // 滚动到顶部
    scrollTop.value = 0
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
  // 刷新想法列表
  fetchThoughts(true)
})

onMounted(() => {
  fetchThoughts(true)
})
</script>

<style lang="scss" scoped>
.moments-page {
  min-height: 100vh;
  background-color: #f5f5f5;
  padding: 20rpx;
  padding-bottom: 40rpx;
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
</style>
