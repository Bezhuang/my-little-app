<script>
import userStore from './store/user'

export default {
  onLaunch: function () {
    console.log('App Launch')
    // 初始化用户状态（从本地存储恢复 JWT token 等信息）
    userStore.init()
  },
  onShow: function () {
    console.log('App Show')
    // 每次显示应用时刷新用户状态
    userStore.init()
  },
  onHide: function () {
    console.log('App Hide')
  },
}
</script>

<style>
/* 全局公共样式 */
page {
  background-color: #f5f5f5;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
}

/* 重置uni-ui组件默认样式 */
.uni-card {
  margin: 0 !important;
}

/* 安全区域 */
.safe-area-bottom {
  padding-bottom: constant(safe-area-inset-bottom);
  padding-bottom: env(safe-area-inset-bottom);
}

/* ==================== PC端适配 ==================== */
@media screen and (min-width: 768px) {
  /* PC/平板端容器最大宽度限制 */
  .pc-container {
    max-width: 1200px;
    margin: 0 auto;
  }

  /* 想法列表页 PC 样式 */
  .moments-page {
    max-width: 800px;
    margin: 0 auto;
  }

  .moment-item {
    border-radius: 12px;
    margin: 16px;
    padding: 24px;
  }

  .moment-content {
    font-size: 18px;
    line-height: 1.8;
  }

  .moment-images {
    margin-top: 16px;
  }

  .moment-image {
    border-radius: 8px;
  }

  /* AI 聊天页面 PC 样式 */
  .ai-chat-page {
    max-width: 900px;
    margin: 0 auto;
    height: 100vh;
  }

  .chat-header {
    padding: 20px 30px;
  }

  .chat-title {
    font-size: 24px;
  }

  .chat-subtitle {
    font-size: 14px;
  }

  .messages-container {
    max-width: 900px;
    margin: 0 auto;
  }

  .message-content {
    max-width: 600px;
    padding: 16px 20px;
  }

  .message-text {
    font-size: 16px;
    line-height: 1.6;
  }

  .input-area {
    padding: 16px 30px;
  }

  .message-input {
    font-size: 16px;
    min-height: 50px;
  }

  .send-text {
    font-size: 16px;
  }

  /* 个人中心页面 PC 样式 */
  .profile-page {
    max-width: 800px;
    margin: 0 auto;
  }

  .profile-header {
    padding: 40px 30px;
  }

  .avatar-large {
    width: 100px;
    height: 100px;
  }

  .nickname {
    font-size: 24px;
  }

  .info-card {
    border-radius: 12px;
    margin: 20px;
    padding: 24px;
  }

  /* 登录/注册页面 PC 样式 */
  .auth-page {
    display: flex;
    align-items: center;
    justify-content: center;
    min-height: 100vh;
  }

  .auth-container {
    width: 420px;
    padding: 40px;
    border-radius: 16px;
  }

  .auth-title {
    font-size: 28px;
    margin-bottom: 30px;
  }

  .auth-input {
    height: 52px;
    font-size: 16px;
  }

  .auth-btn {
    height: 52px;
    font-size: 18px;
  }

  .auth-links {
    font-size: 15px;
    margin-top: 24px;
  }

  /* TabBar 在 PC 端不显示（因为有原生标题栏） */
  /* uni-app H5 端会自动隐藏 */
}

/* 大屏幕优化 */
@media screen and (min-width: 1200px) {
  .moment-content {
    font-size: 20px;
  }

  .message-content {
    max-width: 700px;
  }

  .ai-chat-page,
  .moments-page,
  .profile-page {
    max-width: 1000px;
  }
}
</style>
