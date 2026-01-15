<template>
  <view class="settings-page">
    <!-- 顶部导航 -->
    <view class="nav-header">
      <view class="nav-left" @click="goBack">
        <uni-icons type="left" size="22" color="#333"></uni-icons>
      </view>
      <text class="nav-title">设置</text>
      <view class="nav-right"></view>
    </view>

    <!-- 内容区域 -->
    <view class="form-section">
      <!-- Token 余额卡片 -->
      <view class="form-card">
        <view class="form-item">
          <view class="form-label-row">
            <uni-icons type="brain" size="22" color="#4CD964"></uni-icons>
            <text class="form-label">Token 余额</text>
          </view>
          <view class="form-value-row">
            <text class="form-value">{{ remainingTokens }}</text>
            <text class="form-unit">tokens</text>
          </view>
          <view class="form-tip">可用于 AI 对话，深度思考消耗更多 Token</view>
          <!-- 签到按钮 -->
          <view class="signin-btn" :class="{ disabled: hasSignedToday }" @click="onSignIn">
            <text class="signin-text">{{ hasSignedToday ? '今日已签到' : '每日签到 +3000' }}</text>
          </view>
        </view>

        <view class="form-item">
          <view class="form-label-row">
            <uni-icons type="search" size="22" color="#667eea"></uni-icons>
            <text class="form-label">搜索次数</text>
          </view>
          <view class="form-value-row">
            <text class="form-value">{{ remainingWebSearches }}</text>
            <text class="form-unit">次</text>
          </view>
          <view class="form-tip">可用于联网搜索，每次消耗 1 次</view>
        </view>
      </view>

      <!-- 充值按钮 -->
      <view class="form-card">
        <view class="form-item clickable" @click="onRecharge">
          <text class="form-label">充值</text>
          <view class="form-arrow">
            <uni-icons type="right" size="18" color="#999"></uni-icons>
          </view>
        </view>
      </view>

      <!-- 使用说明 -->
      <view class="form-card">
        <view class="form-item">
          <text class="form-label">使用说明</text>
          <view class="tip-list">
            <view class="tip-item">
              <text class="tip-bullet">•</text>
              <text class="tip-text">Token 用于 AI 对话，深度思考消耗更多 Token</text>
            </view>
            <view class="tip-item">
              <text class="tip-bullet">•</text>
              <text class="tip-text">联网搜索每次消耗 1 次搜索次数</text>
            </view>
            <view class="tip-item">
              <text class="tip-bullet">•</text>
              <text class="tip-text">新用户注册自动获得 10000 tokens</text>
            </view>
            <view class="tip-item">
              <text class="tip-bullet">•</text>
              <text class="tip-text">每日签到可获得 3000 tokens（总上限 660000）</text>
            </view>
          </view>
        </view>
      </view>
    </view>

    <!-- 充值弹窗 -->
    <uni-popup ref="rechargePopup" type="bottom">
      <view class="recharge-popup">
        <view class="popup-header">
          <text class="popup-title">充值</text>
          <uni-icons type="closeempty" size="24" color="#999" @click="closeRechargePopup"></uni-icons>
        </view>

        <view class="popup-content">
          <view class="developing-container">
            <uni-icons type="info" size="50" color="#999"></uni-icons>
            <text class="developing-text">充值功能开发中</text>
            <text class="developing-desc">敬请期待</text>
          </view>
        </view>
      </view>
    </uni-popup>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import userStore from '../../../store/user'

const rechargePopup = ref(null)
const hasSignedToday = ref(false)
const isSigningIn = ref(false)

// 计算属性获取用户信息
const userInfo = computed(() => userStore.state.userInfo || {})

// 剩余 Token 数量
const remainingTokens = computed(() => {
  return userInfo.value.remainingTokens ?? 0
})

// 剩余搜索次数
const remainingWebSearches = computed(() => {
  return userInfo.value.remainingWebSearches ?? 0
})

// 获取今日日期字符串 (yyyy-MM-dd)
const getTodayString = () => {
  const date = new Date()
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

// 存储签到日期（兼容 H5 和小程序）
const setSignInDate = (date) => {
  // #ifdef H5
  localStorage.setItem('lastSignInDate', date)
  // #endif
  // #ifndef H5
  uni.setStorageSync('lastSignInDate', date)
  // #endif
}

// 获取签到日期
const getSignInDate = () => {
  // #ifdef H5
  return localStorage.getItem('lastSignInDate')
  // #endif
  // #ifndef H5
  return uni.getStorageSync('lastSignInDate')
  // #endif
}

// 检查今日是否已签到
const checkSignInStatus = () => {
  const today = getTodayString()
  const lastSignInDate = getSignInDate()
  hasSignedToday.value = lastSignInDate === today
}

// 页面显示时刷新用户信息
onShow(() => {
  userStore.init()
  if (userStore.state.isLogin) {
    userStore.fetchUserInfo()
  }
  checkSignInStatus()
})

// 返回上一页
const goBack = () => {
  uni.switchTab({
    url: '/pages/profile/index'
  })
}

// 签到
const onSignIn = async () => {
  // 重新初始化以确保获取最新状态
  userStore.init()
  if (!userStore.state.isLogin || !userStore.state.userInfo) {
    uni.showModal({
      title: '提示',
      content: '请先登录后再签到',
      confirmText: '去登录',
      success: (res) => {
        if (res.confirm) {
          uni.switchTab({ url: '/pages/login/index' })
        }
      }
    })
    return
  }
  if (hasSignedToday.value || isSigningIn.value) {
    return
  }

  isSigningIn.value = true
  try {
    const res = await uni.request({
      url: '/api/user/signin',
      method: 'POST',
      header: {
        'Authorization': 'Bearer ' + userStore.state.userInfo.token
      }
    })

    if (res.statusCode === 200 && res.data.code === 200) {
      // 签到成功
      const today = getTodayString()
      setSignInDate(today)
      hasSignedToday.value = true

      // 更新本地显示
      userStore.state.userInfo.remainingTokens = res.data.data.newTokens
      uni.setStorageSync('userInfo', userStore.state.userInfo)

      uni.showToast({
        title: `签到成功 +3000 tokens`,
        icon: 'success'
      })
    } else if (res.statusCode === 401) {
      // token 过期，清除登录状态
      uni.showModal({
        title: '登录已过期',
        content: '请重新登录',
        confirmText: '去登录',
        success: (res) => {
          if (res.confirm) {
            userStore.logout()
            uni.switchTab({ url: '/pages/login/index' })
          }
        }
      })
    } else {
      uni.showToast({
        title: res.data.message || '签到失败',
        icon: 'none'
      })
    }
  } catch (error) {
    console.error('签到失败:', error)
    uni.showToast({ title: '签到失败，请稍后重试', icon: 'none' })
  } finally {
    isSigningIn.value = false
  }
}

// 打开充值弹窗
const onRecharge = () => {
  rechargePopup.value.open()
}

// 关闭充值弹窗
const closeRechargePopup = () => {
  rechargePopup.value.close()
}
</script>

<style lang="scss" scoped>
.settings-page {
  min-height: 100vh;
  background: #f5f5f5;
}

.form-section {
  padding: 30rpx;
  padding-top: 140rpx;
}

/* 顶部导航 */
.nav-header {
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
  z-index: 999;
}

.nav-left,
.nav-right {
  width: 80rpx;
}

.nav-left {
  display: flex;
  align-items: center;
}

.nav-title {
  flex: 1;
  text-align: center;
  font-size: 36rpx;
  font-weight: 600;
  color: #333;
}

/* PC端适配 */
@media screen and (min-width: 768px) {
  .nav-header {
    left: 50%;
    right: auto;
    transform: translateX(-50%);
    width: 100%;
    max-width: 800px;
    padding-left: 30rpx;
    padding-right: 30rpx;
  }

  .form-section {
    max-width: 800px;
    margin: 0 auto;
    padding-left: 30rpx;
    padding-right: 30rpx;
    padding-top: 170rpx;
  }
}

.form-card {
  background: #fff;
  border-radius: 20rpx;
  padding: 0 30rpx;
  margin-bottom: 30rpx;
}

.form-item {
  padding: 30rpx 0;
  border-bottom: 1rpx solid #f0f0f0;
}

.form-item:last-child {
  border-bottom: none;
}

.form-item.clickable {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.form-label-row {
  display: flex;
  align-items: center;
  margin-bottom: 16rpx;
}

.form-label {
  margin-left: 12rpx;
  font-size: 28rpx;
  color: #333;
  font-weight: 500;
}

.form-value-row {
  display: flex;
  align-items: baseline;
  margin-bottom: 12rpx;
}

.form-value {
  font-size: 56rpx;
  font-weight: 700;
  color: #333;
}

.form-unit {
  font-size: 24rpx;
  color: #999;
  margin-left: 10rpx;
}

.form-tip {
  display: block;
  font-size: 24rpx;
  color: #999;
  margin-top: 4rpx;
}

/* 签到按钮 */
.signin-btn {
  margin-top: 20rpx;
  padding: 16rpx 32rpx;
  background: linear-gradient(135deg, #4CD964 0%, #2ecc71 100%);
  border-radius: 40rpx;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.signin-btn.disabled {
  background: #ccc;
}

.signin-text {
  font-size: 26rpx;
  color: #fff;
  font-weight: 500;
}

.form-arrow {
  display: flex;
  align-items: center;
}

.tip-list {
  margin-top: 10rpx;
}

.tip-item {
  display: flex;
  align-items: flex-start;
  margin-bottom: 12rpx;
}

.tip-bullet {
  color: #4CD964;
  margin-right: 10rpx;
}

.tip-text {
  flex: 1;
  font-size: 26rpx;
  color: #666;
  line-height: 1.6;
}

/* 充值弹窗样式 */
.recharge-popup {
  background: #fff;
  border-radius: 30rpx 30rpx 0 0;
  padding: 30rpx 40rpx 60rpx;
}

.popup-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 40rpx;
}

.popup-title {
  font-size: 34rpx;
  font-weight: 600;
  color: #333;
}

.popup-content {

}

.developing-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 60rpx 0;
}

.developing-text {
  margin-top: 30rpx;
  font-size: 32rpx;
  font-weight: 600;
  color: #333;
}

.developing-desc {
  margin-top: 10rpx;
  font-size: 26rpx;
  color: #999;
}
</style>
