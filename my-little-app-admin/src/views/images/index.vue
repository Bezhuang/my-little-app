<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { imageApi } from '../../utils/api'
import { BASE_URL } from '../../constant'

const loading = ref(false)
const uploadLoading = ref(false)
const images = ref([])
const pagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0
})

const uploadDialogVisible = ref(false)
const selectedFile = ref(null)
const previewVisible = ref(false)
const previewImage = ref('')

// 加载图片列表
const fetchImages = async () => {
  loading.value = true
  try {
    const res = await imageApi.getList({
      page: pagination.current,
      pageSize: pagination.pageSize
    })
    if (res.data) {
      images.value = res.data.list || []
      pagination.total = res.data.total || 0
    }
  } catch (error) {
    ElMessage.error(error.message || '获取图片列表失败')
  } finally {
    loading.value = false
  }
}

// 处理文件选择
const handleFileChange = (file) => {
  selectedFile.value = file.raw
}

// 上传图片
const handleUpload = async () => {
  if (!selectedFile.value) {
    ElMessage.warning('请选择要上传的图片')
    return
  }

  uploadLoading.value = true
  try {
    await imageApi.upload(selectedFile.value, (progress) => {
      // 可以显示上传进度
    })
    ElMessage.success('上传成功')
    uploadDialogVisible.value = false
    selectedFile.value = null
    fetchImages()
  } catch (error) {
    ElMessage.error(error.message || '上传失败')
  } finally {
    uploadLoading.value = false
  }
}

// 删除图片
const handleDelete = (image) => {
  ElMessageBox.confirm('确定要删除这张图片吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await imageApi.delete(image.id)
      ElMessage.success('删除成功')
      fetchImages()
    } catch (error) {
      ElMessage.error(error.message || '删除失败')
    }
  }).catch(() => {})
}

// 预览图片
const handlePreview = (image) => {
  // 使用后端的数据接口获取图片
  previewImage.value = `${BASE_URL}/api/admin/images/public/${image.id}/data`
  previewVisible.value = true
}

// 格式化文件大小
const formatSize = (bytes) => {
  if (!bytes) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

// 格式化时间
const formatTime = (time) => {
  if (!time) return ''
  return time.split(' ')[0]
}

onMounted(() => {
  fetchImages()
})
</script>

<template>
  <div class="images-page">
    <div class="page-header">
      <div>
        <h1 class="page-title">图片管理</h1>
        <p class="page-subtitle">管理上传到数据库的图片文件</p>
      </div>
      <el-button type="primary" @click="uploadDialogVisible = true">
        <el-icon><Upload /></el-icon>
        上传图片
      </el-button>
    </div>

    <!-- 图片列表 -->
    <div class="images-grid" v-loading="loading">
      <div
        v-for="image in images"
        :key="image.id"
        class="image-card"
      >
        <div class="image-preview" @click="handlePreview(image)">
          <img :src="`${BASE_URL}/api/admin/images/public/${image.id}/data`" :alt="image.filename" />
          <div class="image-overlay">
            <el-icon><ZoomIn /></el-icon>
          </div>
        </div>
        <div class="image-info">
          <div class="image-name" :title="image.filename">{{ image.filename }}</div>
          <div class="image-meta">
            <span>{{ formatSize(image.size) }}</span>
            <span>{{ formatTime(image.createTime) }}</span>
          </div>
          <div class="image-actions">
            <el-button text size="small" type="danger" @click="handleDelete(image)">
              <el-icon><Delete /></el-icon>
            </el-button>
          </div>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-if="images.length === 0 && !loading" class="empty-state">
        <el-icon><Picture /></el-icon>
        <text>暂无图片</text>
        <text class="empty-hint">点击上方按钮上传图片</text>
      </div>
    </div>

    <!-- 分页 -->
    <div class="pagination-wrapper" v-if="pagination.total > pagination.pageSize">
      <el-pagination
        v-model:current-page="pagination.current"
        :page-size="pagination.pageSize"
        :total="pagination.total"
        layout="prev, pager, next"
        @current-change="fetchImages"
      />
    </div>

    <!-- 上传对话框 -->
    <el-dialog
      v-model="uploadDialogVisible"
      title="上传图片"
      width="400px"
    >
      <el-upload
        class="upload-area"
        drag
        :auto-upload="false"
        :show-file-list="false"
        accept="image/*"
        :on-change="handleFileChange"
      >
        <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
        <div class="el-upload__text">
          拖拽图片到此处，或 <em>点击选择</em>
        </div>
        <template #tip>
          <div class="el-upload__tip">
            支持 jpg、png、gif 格式，单个文件不超过 10MB
          </div>
        </template>
      </el-upload>
      <div v-if="selectedFile" class="selected-file">
        已选择: {{ selectedFile.name }} ({{ formatSize(selectedFile.size) }})
      </div>
      <template #footer>
        <el-button @click="uploadDialogVisible = false">取消</el-button>
        <el-button
          type="primary"
          :loading="uploadLoading"
          @click="handleUpload"
          :disabled="!selectedFile"
        >
          上传
        </el-button>
      </template>
    </el-dialog>

    <!-- 图片预览 -->
    <el-image-viewer
      v-if="previewVisible"
      :url-list="[previewImage]"
      @close="previewVisible = false"
    />
  </div>
</template>

<style scoped>
.images-page {
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 4px;
}

.page-subtitle {
  font-size: 14px;
  color: var(--text-muted);
}

.images-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  gap: 12px;
  min-height: 300px;
}

.image-card {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  overflow: hidden;
  transition: all 0.2s ease;
}

.image-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.image-preview {
  position: relative;
  aspect-ratio: 1;
  overflow: hidden;
  cursor: pointer;
}

.image-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.image-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.image-preview:hover .image-overlay {
  opacity: 1;
}

.image-overlay .el-icon {
  color: white;
  font-size: 32px;
}

.image-info {
  padding: 8px;
}

.image-name {
  font-size: 12px;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-bottom: 4px;
}

.image-meta {
  display: flex;
  justify-content: space-between;
  font-size: 11px;
  color: var(--text-muted);
  margin-bottom: 4px;
}

.image-actions {
  display: flex;
  justify-content: center;
}

.image-actions .el-button {
  padding: 4px 8px;
  margin: 0;
}

.empty-state {
  grid-column: 1 / -1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 0;
  color: var(--text-muted);
}

.empty-state .el-icon {
  font-size: 60px;
  margin-bottom: 16px;
}

.empty-hint {
  font-size: 12px;
  margin-top: 8px;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}

.upload-area {
  width: 100%;
}

.upload-area :deep(.el-upload) {
  width: 100%;
}

.upload-area :deep(.el-upload-dragger) {
  width: 100%;
  padding: 40px 0;
}

.selected-file {
  margin-top: 16px;
  padding: 12px;
  background: var(--bg-darker);
  border-radius: 8px;
  font-size: 13px;
  color: var(--text-secondary);
}
</style>
