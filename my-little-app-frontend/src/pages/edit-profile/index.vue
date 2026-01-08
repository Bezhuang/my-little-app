<template>
  <view class="edit-profile-page">
    <view class="form-section">
      <!-- 头像 -->
      <view class="avatar-section" @click="chooseAvatar">
        <view class="avatar-wrapper">
          <image 
            v-if="formData.avatar" 
            class="avatar" 
            :src="formData.avatar" 
            mode="aspectFill"
          />
          <uni-icons v-else type="person-filled" size="60" color="#999"></uni-icons>
        </view>
        <text class="avatar-tip">点击更换头像</text>
      </view>
      
      <!-- 表单 -->
      <view class="form-card">
        <view class="form-item">
          <text class="form-label">用户名</text>
          <view class="form-input-wrapper">
            <input 
              class="form-input"
              type="text"
              v-model="formData.username"
              placeholder="请输入用户名"
              disabled
            />
            <uni-icons type="locked" size="18" color="#ccc"></uni-icons>
          </view>
          <text class="form-tip">用户名不可修改</text>
        </view>
        
        <view class="form-item">
          <text class="form-label">邮箱</text>
          <view class="form-input-wrapper">
            <input 
              class="form-input"
              type="text"
              v-model="formData.email"
              placeholder="请输入邮箱"
            />
          </view>
        </view>
        
        <view class="form-item">
          <text class="form-label">手机号</text>
          <view class="form-input-wrapper">
            <input 
              class="form-input"
              type="number"
              v-model="formData.phone"
              placeholder="请输入手机号"
            />
          </view>
        </view>
      </view>
      
      <!-- 修改密码入口 -->
      <view class="form-card">
        <view class="form-item clickable" @click="goToChangePassword">
          <text class="form-label">修改密码</text>
          <view class="form-arrow">
            <uni-icons type="right" size="18" color="#999"></uni-icons>
          </view>
        </view>
      </view>
      
      <!-- 保存按钮 -->
      <button 
        class="save-btn"
        :loading="saving"
        :disabled="saving"
        @click="handleSave"
      >
        {{ saving ? '保存中...' : '保存修改' }}
      </button>
    </view>
    
    <!-- 修改密码弹窗 -->
    <uni-popup ref="passwordPopup" type="bottom">
      <view class="password-popup">
        <view class="popup-header">
          <text class="popup-title">修改密码</text>
          <uni-icons type="closeempty" size="24" color="#999" @click="closePasswordPopup"></uni-icons>
        </view>
        
        <view class="popup-content">
          <view class="input-group">
            <text class="input-label">原密码</text>
            <view class="input-wrapper">
              <input 
                class="input-field"
                type="password"
                v-model="passwordForm.oldPassword"
                placeholder="请输入原密码"
              />
            </view>
          </view>
          
          <view class="input-group">
            <text class="input-label">新密码</text>
            <view class="input-wrapper">
              <input 
                class="input-field"
                type="password"
                v-model="passwordForm.newPassword"
                placeholder="请输入新密码（至少6位）"
              />
            </view>
          </view>
          
          <view class="input-group">
            <text class="input-label">确认新密码</text>
            <view class="input-wrapper">
              <input 
                class="input-field"
                type="password"
                v-model="passwordForm.confirmPassword"
                placeholder="请再次输入新密码"
              />
            </view>
          </view>
          
          <button 
            class="confirm-btn"
            :loading="changingPassword"
            :disabled="changingPassword"
            @click="handleChangePassword"
          >
            {{ changingPassword ? '提交中...' : '确认修改' }}
          </button>
        </view>
      </view>
    </uni-popup>
  </view>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import userStore from '../../store/user'

const saving = ref(false)
const changingPassword = ref(false)
const passwordPopup = ref(null)

const formData = reactive({
  username: '',
  email: '',
  phone: '',
  avatar: ''
})

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

// 初始化表单数据
onMounted(() => {
  const userInfo = userStore.state.userInfo
  if (userInfo) {
    formData.username = userInfo.username || ''
    formData.email = userInfo.email || ''
    formData.phone = userInfo.phone || ''
    formData.avatar = userInfo.avatar || ''
  }
})

// 选择头像
const chooseAvatar = () => {
  uni.chooseImage({
    count: 1,
    sizeType: ['compressed'],
    sourceType: ['album', 'camera'],
    success: (res) => {
      formData.avatar = res.tempFilePaths[0]
      // 实际项目中需要上传图片到服务器
      uni.showToast({
        title: '头像更换成功（本地预览）',
        icon: 'none'
      })
    }
  })
}

// 保存修改
const handleSave = async () => {
  // 邮箱验证
  if (formData.email) {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
    if (!emailRegex.test(formData.email)) {
      uni.showToast({ title: '邮箱格式不正确', icon: 'none' })
      return
    }
  }
  
  // 手机号验证
  if (formData.phone && !/^1[3-9]\d{9}$/.test(formData.phone)) {
    uni.showToast({ title: '手机号格式不正确', icon: 'none' })
    return
  }
  
  saving.value = true
  
  try {
    const result = await userStore.updateProfile({
      email: formData.email,
      phone: formData.phone,
      avatar: formData.avatar
    })
    
    if (result.success) {
      uni.showToast({
        title: '保存成功',
        icon: 'success'
      })
      
      setTimeout(() => {
        uni.navigateBack()
      }, 1000)
    } else {
      uni.showToast({
        title: result.message || '保存失败',
        icon: 'none'
      })
    }
  } catch (error) {
    uni.showToast({
      title: '保存失败，请稍后重试',
      icon: 'none'
    })
  } finally {
    saving.value = false
  }
}

// 打开修改密码弹窗
const goToChangePassword = () => {
  passwordForm.oldPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
  passwordPopup.value.open()
}

// 关闭修改密码弹窗
const closePasswordPopup = () => {
  passwordPopup.value.close()
}

// 修改密码
const handleChangePassword = async () => {
  if (!passwordForm.oldPassword) {
    uni.showToast({ title: '请输入原密码', icon: 'none' })
    return
  }
  if (!passwordForm.newPassword) {
    uni.showToast({ title: '请输入新密码', icon: 'none' })
    return
  }
  if (passwordForm.newPassword.length < 6) {
    uni.showToast({ title: '新密码长度不能少于6位', icon: 'none' })
    return
  }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    uni.showToast({ title: '两次密码输入不一致', icon: 'none' })
    return
  }
  
  changingPassword.value = true
  
  try {
    const result = await userStore.changePassword(
      passwordForm.oldPassword,
      passwordForm.newPassword
    )
    
    if (result.success) {
      uni.showToast({
        title: '密码修改成功',
        icon: 'success'
      })
      closePasswordPopup()
    } else {
      uni.showToast({
        title: result.message || '修改失败',
        icon: 'none'
      })
    }
  } catch (error) {
    uni.showToast({
      title: '修改失败，请稍后重试',
      icon: 'none'
    })
  } finally {
    changingPassword.value = false
  }
}
</script>

<style lang="scss" scoped>
.edit-profile-page {
  min-height: 100vh;
  background: #f5f5f5;
}

.form-section {
  padding: 30rpx;
}

.avatar-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 40rpx 0;
}

.avatar-wrapper {
  width: 160rpx;
  height: 160rpx;
  border-radius: 50%;
  overflow: hidden;
  background: #e8e8e8;
  display: flex;
  align-items: center;
  justify-content: center;
}

.avatar {
  width: 100%;
  height: 100%;
}

.avatar-tip {
  margin-top: 16rpx;
  font-size: 26rpx;
  color: #667eea;
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

.form-label {
  display: block;
  font-size: 28rpx;
  color: #333;
  font-weight: 500;
  margin-bottom: 16rpx;
}

.form-input-wrapper {
  display: flex;
  align-items: center;
  background: #f8f9fa;
  border-radius: 12rpx;
  padding: 20rpx 24rpx;
}

.form-input {
  flex: 1;
  font-size: 28rpx;
  color: #333;
}

.form-input[disabled] {
  color: #999;
}

.form-tip {
  display: block;
  font-size: 24rpx;
  color: #999;
  margin-top: 10rpx;
}

.form-arrow {
  display: flex;
  align-items: center;
}

.save-btn {
  width: 100%;
  height: 96rpx;
  line-height: 96rpx;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 48rpx;
  font-size: 32rpx;
  font-weight: 600;
  color: #fff;
  border: none;
  margin-top: 40rpx;
}

.save-btn::after {
  border: none;
}

.save-btn[disabled] {
  opacity: 0.6;
}

/* 密码弹窗样式 */
.password-popup {
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

.input-group {
  margin-bottom: 30rpx;
}

.input-label {
  display: block;
  font-size: 26rpx;
  color: #666;
  margin-bottom: 12rpx;
}

.input-wrapper {
  background: #f8f9fa;
  border-radius: 12rpx;
  padding: 24rpx;
}

.input-field {
  width: 100%;
  font-size: 28rpx;
  color: #333;
}

.confirm-btn {
  width: 100%;
  height: 92rpx;
  line-height: 92rpx;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 46rpx;
  font-size: 32rpx;
  font-weight: 600;
  color: #fff;
  border: none;
  margin-top: 20rpx;
}

.confirm-btn::after {
  border: none;
}

.confirm-btn[disabled] {
  opacity: 0.6;
}
</style>







