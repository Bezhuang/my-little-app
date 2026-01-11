<template>
  <view class="profile-page">
    <!-- 顶部用户信息区域 -->
    <view class="user-header">
      <view class="header-bg"></view>
      <view class="user-info">
        <view class="avatar-wrapper" @click="onAvatarClick">
          <image 
            v-if="userStore.state.isLogin && userStore.state.userInfo?.avatar" 
            class="avatar" 
            :src="userStore.state.userInfo.avatar" 
            mode="aspectFill"
          />
          <uni-icons v-else type="person-filled" size="80" color="#fff"></uni-icons>
        </view>
        <text class="user-name" @click="onAvatarClick">
          {{ userStore.state.isLogin ? userStore.state.userInfo?.username : '点击登录' }}
        </text>
        <text v-if="userStore.state.isLogin && userStore.state.userInfo?.email" class="user-email">
          {{ userStore.state.userInfo.email }}
        </text>
      </view>
    </view>

    <!-- 快捷功能区 -->
    <view class="quick-actions">
      <view class="action-grid">
        <view
          class="action-item"
          v-for="(action, index) in quickActions"
          :key="index"
          @click="onActionItemClick(action)"
        >
          <view class="action-icon-wrapper" :style="{ background: action.color + '20' }">
            <uni-icons v-if="action.icon" :type="action.icon" size="28" :color="action.color"></uni-icons>
            <view v-else-if="action.iconChar" class="action-icon-text">{{ action.iconChar }}</view>
          </view>
          <text class="action-text">{{ action.name }}</text>
        </view>
      </view>
    </view>

    <!-- 菜单列表 -->
    <view class="menu-wrapper">
      <uni-list>
        <uni-list-item 
          v-for="(menu, index) in menuList" 
          :key="index"
          :title="menu.name"
          clickable
          showArrow
          @click="onMenuClick(menu)"
        >
          <template v-slot:header>
            <view class="menu-icon-wrapper">
              <uni-icons :type="menu.icon" size="22" color="#667eea"></uni-icons>
            </view>
          </template>
        </uni-list-item>
      </uni-list>
    </view>

    <!-- 退出登录按钮 -->
    <view class="logout-wrapper" v-if="userStore.state.isLogin">
      <button class="logout-btn" @click="onLogout">退出登录</button>
    </view>

    <!-- 版本信息 -->
    <view class="version-info">
      <text class="copyright-text">Copyright © Bezhuang</text>
      <text class="version-text">版本号: 1.0.1</text>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import userStore from '../../store/user'

const quickActions = ref([
  { iconChar: '💻', name: '关于开发者', type: 'link', url: 'https://www.baidu.com/s?wd=%E5%BA%84%E4%B9%8B%E7%9A%93%20%E4%B8%8A%E6%B5%B7', color: '#24292e' },
  { iconChar: '📧', name: '联系开发者', type: 'mailto', url: 'mailto:13818993049@163.com', color: '#667eea' },
  { iconChar: '🔔', name: '消息通知', type: 'notifications', color: '#667eea' }
])

const menuList = ref([
  { icon: 'person', name: '编辑资料', type: 'edit-profile', requireLogin: true },
  { icon: 'help', name: '问题与反馈', type: 'feedback' },
  { icon: 'gear', name: '设置', type: 'settings' }
])

// 页面每次显示时刷新用户状态（从登录页返回时也会触发）
onShow(() => {
  // 重新初始化状态，确保从本地存储读取最新数据
  userStore.init()
  if (userStore.state.isLogin) {
    userStore.fetchUserInfo()
  }
})

// 头像点击
const onAvatarClick = () => {
  if (userStore.state.isLogin) {
    // 已登录，跳转编辑资料
    uni.navigateTo({
      url: '/pages/edit-profile/index'
    })
  } else {
    // 未登录，跳转登录页
    uni.navigateTo({
      url: '/pages/login/index'
    })
  }
}

// 快捷操作点击
const onActionItemClick = (action) => {
  // 链接类型直接打开
  if (action.type === 'link') {
    uni.navigateTo({
      url: `/pages/webview/index?url=${encodeURIComponent(action.url)}&title=${encodeURIComponent(action.name)}`
    })
    return
  }

  // 邮件类型
  if (action.type === 'mailto') {
    // #ifdef H5
    window.location.href = action.url
    // #endif
    // #ifdef MP-WEIXIN
    uni.setClipboardData({
      data: action.url.replace('mailto:', ''),
      success: () => {
        uni.showToast({ title: '邮箱已复制', icon: 'none' })
      }
    })
    // #endif
    return
  }

  if (!userStore.state.isLogin) {
    uni.showModal({
      title: '提示',
      content: '请先登录后再使用此功能',
      confirmText: '去登录',
      success: (res) => {
        if (res.confirm) {
          uni.navigateTo({
            url: '/pages/login/index'
          })
        }
      }
    })
    return
  }

  uni.showToast({
    title: `${action.name}功能开发中`,
    icon: 'none'
  })
}

// 菜单点击
const onMenuClick = (menu) => {
  // 需要登录的功能
  if (menu.requireLogin && !userStore.state.isLogin) {
    uni.showModal({
      title: '提示',
      content: '请先登录后再使用此功能',
      confirmText: '去登录',
      success: (res) => {
        if (res.confirm) {
          uni.navigateTo({
            url: '/pages/login/index'
          })
        }
      }
    })
    return
  }
  
  switch (menu.type) {
    case 'edit-profile':
      uni.navigateTo({
        url: '/pages/edit-profile/index'
      })
      break
    case 'checkin':
      uni.showToast({
        title: '签到功能开发中',
        icon: 'success'
      })
      break
    case 'points':
      uni.showToast({
        title: '积分功能开发中',
        icon: 'none'
      })
      break
    case 'feedback':
      uni.showToast({
        title: '反馈功能开发中',
        icon: 'none'
      })
      break
    case 'settings':
      if (!userStore.state.isLogin) {
        uni.showModal({
          title: '提示',
          content: '请先登录后再使用此功能',
          confirmText: '去登录',
          success: (res) => {
            if (res.confirm) {
              uni.navigateTo({
                url: '/pages/login/index'
              })
            }
          }
        })
      } else {
        uni.navigateTo({
          url: '/pages/profile/settings/index'
        })
      }
      break
    default:
      break
  }
}

// 退出登录
const onLogout = () => {
  uni.showModal({
    title: '提示',
    content: '确定要退出登录吗？',
    success: async (res) => {
      if (res.confirm) {
        await userStore.logout()
        uni.showToast({
          title: '已退出登录',
          icon: 'success'
        })
      }
    }
  })
}
</script>

<style lang="scss" scoped>
/* 防止页面滚动 */
page {
  height: 100%;
  overflow: hidden;
}

.profile-page {
  min-height: 100vh;
  min-height: 100%;
  background-color: #f5f5f5;
  overflow: hidden;
  padding-top: 100rpx;
}

.user-header {
  position: relative;
  padding-bottom: 40rpx;
  margin-top: -100rpx;
}

.header-bg {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 400rpx;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.user-info {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 100rpx;
}

.avatar-wrapper {
  width: 160rpx;
  height: 160rpx;
  border-radius: 50%;
  overflow: hidden;
  border: 6rpx solid #fff;
  box-shadow: 0 8rpx 30rpx rgba(0, 0, 0, 0.15);
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
}

.avatar {
  width: 100%;
  height: 100%;
}

.user-name {
  margin-top: 24rpx;
  font-size: 40rpx;
  color: #fff;
  font-weight: 700;
  text-shadow: 0 2rpx 10rpx rgba(0, 0, 0, 0.2);
}

.user-email {
  margin-top: 8rpx;
  font-size: 28rpx;
  color: rgba(255, 255, 255, 0.95);
  text-shadow: 0 1rpx 5rpx rgba(0, 0, 0, 0.15);
}

.quick-actions {
  background-color: #fff;
  margin: 20rpx 30rpx;
  border-radius: 20rpx;
  padding: 30rpx 20rpx;
  box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.05);
}

.action-grid {
  display: flex;
  justify-content: space-around;
}

.action-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.action-icon-wrapper {
  width: 80rpx;
  height: 80rpx;
  border-radius: 50%;
  background: linear-gradient(135deg, #f0f4ff 0%, #e8efff 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 12rpx;
}

.action-text {
  font-size: 26rpx;
  color: #333;
}

.action-icon-text {
  font-size: 28px;
  line-height: 1;
}

.menu-wrapper {
  margin: 0 30rpx;
  border-radius: 20rpx;
  overflow: hidden;
  box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.05);
}

.menu-icon-wrapper {
  width: 50rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 20rpx;
}

.logout-wrapper {
  margin: 40rpx 30rpx;
}

.logout-btn {
  background: linear-gradient(135deg, #ff6b6b 0%, #ee5a5a 100%);
  color: #fff;
  font-size: 32rpx;
  border-radius: 50rpx;
  border: none;
  height: 90rpx;
  line-height: 90rpx;
}

.logout-btn::after {
  border: none;
}

.version-info {
  text-align: center;
  padding: 40rpx 0;
}

.version-text {
  font-size: 24rpx;
  color: #999;
}

.copyright-text {
  display: block;
  font-size: 24rpx;
  color: #999;
  margin-top: 10rpx;
}
</style>
