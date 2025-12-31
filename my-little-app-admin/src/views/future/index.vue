<template>
  <div class="future-page">
    <div class="coming-soon">
      <el-icon size="64" color="#909399"><Tools /></el-icon>
      <h2>功能开发中</h2>
      <p>此功能正在开发中，敬请期待...</p>
    </div>
  </div>
</template>

<script setup>
import { Tools } from '@element-plus/icons-vue'
</script>

<style scoped>
.future-page {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 400px;
}

.coming-soon {
  text-align: center;
  color: var(--text-muted);
}

.coming-soon h2 {
  margin: 24px 0 12px;
  color: var(--text-secondary);
}

.coming-soon p {
  font-size: 14px;
}
</style>
