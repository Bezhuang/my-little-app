<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { thoughtApi, imageApi } from '../../utils/api'
import { BASE_URL } from '../../utils/constants'

const loading = ref(false)
const publishDialogVisible = ref(false)
const isEditing = ref(false)
const imagePickerVisible = ref(false)
const thoughts = ref([])
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0
})

const thoughtForm = reactive({
  id: null,
  content: '',
  imageIds: [],
  visibility: 1
})

const availableImages = ref([])
const imagePagination = reactive({
  current: 1,
  pageSize: 12,
  total: 0
})
const selectedImages = ref([])

const previewImages = ref([])
const previewIndex = ref(0)
const showPreview = ref(false)

// 加载想法列表
const fetchThoughts = async () => {
  loading.value = true
  try {
    const res = await thoughtApi.getList({
      page: pagination.current,
      pageSize: pagination.pageSize
    })
    if (res.data) {
      thoughts.value = (res.data.list || []).map(t => ({
        ...t,
        imageIdList: t.imageIds ? t.imageIds.split(',').filter(Boolean).map(id => parseInt(id)) : [],
        isPublic: t.visibility === 1
      }))
      pagination.total = res.data.total || 0
    }
  } catch (error) {
    ElMessage.error(error.message || '获取想法列表失败')
  } finally {
    loading.value = false
  }
}

// 加载可用图片
const fetchAvailableImages = async () => {
  try {
    const res = await imageApi.getList({
      page: imagePagination.current,
      pageSize: imagePagination.pageSize
    })
    if (res.data) {
      availableImages.value = res.data.list || []
      imagePagination.total = res.data.total || 0
    }
  } catch (error) {
    console.error('获取图片列表失败:', error)
  }
}

const handleDelete = (thought) => {
  ElMessageBox.confirm('确定要删除这条想法吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await thoughtApi.delete(thought.id)
      ElMessage.success('删除成功')
      fetchThoughts()
    } catch (error) {
      ElMessage.error(error.message || '删除失败')
    }
  }).catch(() => {})
}

// 打开编辑对话框
const handleEdit = (thought) => {
  isEditing.value = true
  thoughtForm.id = thought.id
  thoughtForm.content = thought.content
  thoughtForm.imageIds = thought.imageIdList || []
  thoughtForm.visibility = thought.visibility || 1
  publishDialogVisible.value = true
}

// 重置表单
const resetForm = () => {
  isEditing.value = false
  thoughtForm.id = null
  thoughtForm.content = ''
  thoughtForm.imageIds = []
  thoughtForm.visibility = 1
}

// 保存想法（新增或更新）
const handleSave = async () => {
  if (!thoughtForm.content.trim()) {
    ElMessage.warning('请输入想法内容')
    return
  }

  loading.value = true
  try {
    const data = {
      content: thoughtForm.content,
      imageIds: thoughtForm.imageIds.join(','),
      visibility: thoughtForm.visibility
    }

    if (isEditing.value) {
      // 更新
      await thoughtApi.update({
        id: thoughtForm.id,
        ...data
      })
      ElMessage.success('更新成功')
    } else {
      // 新增
      await thoughtApi.add(data)
      ElMessage.success('发布成功')
    }

    publishDialogVisible.value = false
    resetForm()
    fetchThoughts()
  } catch (error) {
    ElMessage.error(error.message || (isEditing.value ? '更新失败' : '发布失败'))
  } finally {
    loading.value = false
  }
}

const handleTogglePublic = async (thought) => {
  const newVisibility = thought.isPublic ? 0 : 1
  try {
    await thoughtApi.updateVisibility(thought.id, newVisibility)
    thought.isPublic = !thought.isPublic
    ElMessage.success(thought.isPublic ? '已设为公开' : '已设为私密')
  } catch (error) {
    ElMessage.error(error.message || '操作失败')
  }
}

// 打开图片选择器
const openImagePicker = () => {
  imagePickerVisible.value = true
  fetchAvailableImages()
}

// 选择图片
const toggleImageSelect = (image) => {
  const index = selectedImages.value.findIndex(img => img.id === image.id)
  if (index > -1) {
    selectedImages.value.splice(index, 1)
  } else {
    if (selectedImages.value.length >= 9) {
      ElMessage.warning('最多只能选择9张图片')
      return
    }
    selectedImages.value.push(image)
  }
}

// 确认选择图片
const confirmImageSelect = () => {
  thoughtForm.imageIds = selectedImages.value.map(img => img.id)
  imagePickerVisible.value = false
}

// 从已选图片中移除
const removeSelectedImage = (image) => {
  const index = selectedImages.value.findIndex(img => img.id === image.id)
  if (index > -1) {
    selectedImages.value.splice(index, 1)
  }
}

// 移除已选择的图片
const handleRemoveImage = (index) => {
  thoughtForm.imageIds.splice(index, 1)
}

// 获取图片URL
const getImageUrl = (id) => {
  return `${BASE_URL}/api/admin/images/public/${id}/data`
}

// 格式化时间
const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now - date

  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)} 分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)} 小时前`
  if (diff < 604800000) return `${Math.floor(diff / 86400000)} 天前`

  return time.split(' ')[0]
}

// 图片预览
const previewVisible = ref(false)
const previewImageUrl = ref('')

const openImagePreview = (imgId) => {
  previewImageUrl.value = getImageUrl(imgId)
  previewVisible.value = true
}

onMounted(() => {
  fetchThoughts()
})
</script>

<template>
  <div class="thoughts-page">
    <div class="page-header">
      <div>
        <h1 class="page-title">想法管理</h1>
        <p class="page-subtitle">记录和分享你的灵感瞬间</p>
      </div>
      <el-button type="primary" @click="() => { resetForm(); publishDialogVisible = true; }">
        <el-icon><EditPen /></el-icon>
        发布想法
      </el-button>
    </div>

    <!-- 想法列表 -->
    <div class="thoughts-list">
      <div
        v-for="thought in thoughts"
        :key="thought.id"
        class="thought-card"
      >
        <div class="thought-header">
          <div class="author-info">
            <el-avatar :size="44" class="author-avatar">A</el-avatar>
            <div class="author-details">
              <div class="author-name">管理员</div>
              <div class="thought-meta">
                <span>{{ formatTime(thought.createTime) }}</span>
              </div>
            </div>
          </div>
          <el-dropdown trigger="click">
            <el-button text circle>
              <el-icon><MoreFilled /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="handleEdit(thought)">
                  <el-icon><EditPen /></el-icon>
                  编辑
                </el-dropdown-item>
                <el-dropdown-item @click="handleTogglePublic(thought)">
                  <el-icon><component :is="thought.isPublic ? 'Lock' : 'Unlock'" /></el-icon>
                  {{ thought.isPublic ? '设为私密' : '设为公开' }}
                </el-dropdown-item>
                <el-dropdown-item divided @click="handleDelete(thought)">
                  <el-icon><Delete /></el-icon>
                  删除
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>

        <div class="thought-content">
          {{ thought.content }}
        </div>

        <div
          v-if="thought.imageIdList && thought.imageIdList.length"
          class="thought-images"
          :class="`images-${Math.min(thought.imageIdList.length, 3)}`"
        >
          <div
            v-for="(imgId, index) in thought.imageIdList"
            :key="index"
            class="image-item"
            @click="openImagePreview(imgId)"
          >
            <img :src="getImageUrl(imgId)" :alt="`图片${index + 1}`" />
          </div>
        </div>

        <div class="thought-footer">
          <div class="footer-left">
            <el-tag
              :type="thought.isPublic ? 'success' : 'info'"
              size="small"
            >
              {{ thought.isPublic ? '公开' : '私密' }}
            </el-tag>
          </div>
          <div class="footer-stats">
            <span class="stat-item">
              <el-icon><Star /></el-icon>
              {{ thought.likes || 0 }}
            </span>
            <span class="stat-item">
              <el-icon><ChatDotRound /></el-icon>
              {{ thought.comments || 0 }}
            </span>
          </div>
        </div>
      </div>
    </div>

    <!-- 发布/编辑对话框 -->
    <el-dialog
      v-model="publishDialogVisible"
      :title="isEditing ? '编辑想法' : '发布想法'"
      width="600px"
      destroy-on-close
      @close="resetForm"
    >
      <div class="publish-form">
        <el-input
          v-model="thoughtForm.content"
          type="textarea"
          placeholder="分享你此刻的想法..."
          :autosize="{ minRows: 4, maxRows: 8 }"
          maxlength="500"
          show-word-limit
        />

        <!-- 已选择的图片 -->
        <div v-if="thoughtForm.imageIds.length" class="upload-preview">
          <div
            v-for="(imgId, index) in thoughtForm.imageIds"
            :key="index"
            class="preview-item"
          >
            <img :src="getImageUrl(imgId)" alt="预览" />
            <div class="remove-btn" @click="handleRemoveImage(index)">
              <el-icon><Close /></el-icon>
            </div>
          </div>
          <div
            class="add-image-btn"
            @click="openImagePicker"
          >
            <el-icon><Plus /></el-icon>
          </div>
        </div>

        <div class="publish-actions">
          <div class="action-buttons">
            <el-button text @click="openImagePicker">
              <el-icon><Picture /></el-icon>
              选择图片
            </el-button>
          </div>
          <div class="action-right">
            <el-switch
              v-model="thoughtForm.visibility"
              :active-value="1"
              :inactive-value="0"
              active-text="公开"
              inactive-text="私密"
            />
          </div>
        </div>
      </div>

      <template #footer>
        <el-button @click="() => { publishDialogVisible = false; resetForm(); }">取消</el-button>
        <el-button type="primary" @click="handleSave" :loading="loading">{{ isEditing ? '保存' : '发布' }}</el-button>
      </template>
    </el-dialog>

    <!-- 图片选择对话框 -->
    <el-dialog
      v-model="imagePickerVisible"
      title="选择图片"
      width="800px"
      destroy-on-close
    >
      <div class="image-picker">
        <div class="selected-section" v-if="selectedImages.length">
          <div class="section-title">已选择 ({{ selectedImages.length }}):</div>
          <div class="selected-list">
            <div
              v-for="img in selectedImages"
              :key="img.id"
              class="selected-item"
            >
              <img :src="getImageUrl(img.id)" />
              <div class="remove-btn" @click="toggleImageSelect(img)">
                <el-icon><Close /></el-icon>
              </div>
            </div>
          </div>
        </div>

        <div class="images-grid">
          <div
            v-for="image in availableImages"
            :key="image.id"
            class="image-item"
            :class="{ selected: selectedImages.some(img => img.id === image.id) }"
            @click="toggleImageSelect(image)"
          >
            <img :src="getImageUrl(image.id)" :alt="image.filename" />
            <div class="select-overlay">
              <el-icon v-if="selectedImages.some(img => img.id === image.id)"><Check /></el-icon>
            </div>
          </div>
        </div>

        <div class="pagination-wrapper" v-if="imagePagination.total > imagePagination.pageSize">
          <el-pagination
            v-model:current-page="imagePagination.current"
            :page-size="imagePagination.pageSize"
            :total="imagePagination.total"
            layout="prev, pager, next"
            @current-change="fetchAvailableImages"
          />
        </div>
      </div>

      <template #footer>
        <el-button @click="imagePickerVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmImageSelect">确认选择</el-button>
      </template>
    </el-dialog>

    <!-- 图片预览对话框 -->
    <el-dialog
      v-model="previewVisible"
      :show-close="true"
      width="90%"
      destroy-on-close
    >
      <div class="image-preview-container">
        <img :src="previewImageUrl" alt="预览" class="preview-image" />
      </div>
    </el-dialog>
  </div>
</template>

<style scoped>
.thoughts-page {
  max-width: 800px;
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

.thoughts-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.thought-card {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: 16px;
  padding: 24px;
  transition: all 0.3s ease;
}

.thought-card:hover {
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
}

.thought-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
}

.author-info {
  display: flex;
  gap: 12px;
}

.author-avatar {
  background: var(--gradient-primary);
}

.author-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
}

.thought-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 13px;
  color: var(--text-muted);
  margin-top: 2px;
}

.thought-content {
  font-size: 15px;
  line-height: 1.7;
  color: var(--text-primary);
  margin-bottom: 16px;
  white-space: pre-wrap;
}

.thought-images {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 16px;
}

.image-item {
  width: 120px;
  height: 120px;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  flex-shrink: 0;
  transition: transform 0.2s;
}

.image-item:hover {
  transform: scale(1.05);
}

.image-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

/* 图片预览样式 */
.image-preview-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 300px;
}

.preview-image {
  max-width: 100%;
  max-height: 70vh;
  object-fit: contain;
  border-radius: 8px;
}

.thought-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 16px;
  border-top: 1px solid var(--border-color);
}

.footer-stats {
  display: flex;
  gap: 20px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  color: var(--text-muted);
}

.publish-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.upload-preview {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.preview-item {
  position: relative;
  width: 100px;
  height: 100px;
  border-radius: 8px;
  overflow: hidden;
}

.preview-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.remove-btn {
  position: absolute;
  top: 4px;
  right: 4px;
  width: 24px;
  height: 24px;
  background: rgba(0, 0, 0, 0.6);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: white;
}

.add-image-btn {
  width: 100px;
  height: 100px;
  border: 2px dashed var(--border-color);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: var(--text-muted);
  transition: all 0.2s;
}

.add-image-btn:hover {
  border-color: var(--primary-color);
  color: var(--primary-color);
}

.publish-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 12px;
  border-top: 1px solid var(--border-color);
}

.action-buttons {
  display: flex;
  gap: 8px;
}

/* 图片选择器样式 */
.image-picker {
  max-height: 500px;
  overflow-y: auto;
}

.selected-section {
  margin-bottom: 16px;
  padding: 12px;
  background: var(--bg-darker);
  border-radius: 8px;
}

.section-title {
  font-size: 14px;
  color: var(--text-secondary);
  margin-bottom: 12px;
}

.selected-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.selected-item {
  position: relative;
  width: 60px;
  height: 60px;
  border-radius: 6px;
  overflow: hidden;
}

.selected-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.selected-item .remove-btn {
  width: 18px;
  height: 18px;
  font-size: 12px;
}

.images-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

.images-grid .image-item {
  position: relative;
  cursor: pointer;
  border-radius: 8px;
  overflow: hidden;
  border: 2px solid transparent;
  transition: all 0.2s;
}

.images-grid .image-item:hover {
  border-color: var(--primary-light);
}

.images-grid .image-item.selected {
  border-color: var(--primary-color);
}

.images-grid .image-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.select-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.3);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.2s;
}

.image-item.selected .select-overlay {
  opacity: 1;
}

.select-overlay .el-icon {
  color: white;
  font-size: 24px;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 16px;
}

/* 移动端适配 */
@media (max-width: 768px) {
  .thoughts-page {
    max-width: 100%;
  }

  .page-header {
    flex-direction: column;
    gap: 12px;
    margin-bottom: 16px;
  }

  .page-title {
    font-size: 20px;
  }

  .thought-card {
    padding: 16px;
    border-radius: 12px;
  }

  .thought-header {
    flex-direction: column;
    gap: 12px;
    margin-bottom: 12px;
  }

  .author-info {
    gap: 10px;
  }

  .author-name {
    font-size: 14px;
  }

  .thought-meta {
    font-size: 12px;
    flex-wrap: wrap;
  }

  .thought-content {
    font-size: 14px;
    margin-bottom: 12px;
  }

  .thought-images {
    margin-bottom: 12px;
  }

  .image-item {
    width: 80px;
    height: 80px;
  }

  .thought-footer {
    padding-top: 12px;
    flex-direction: column;
    gap: 12px;
    align-items: flex-start;
  }

  .footer-stats {
    gap: 16px;
  }

  .stat-item {
    font-size: 12px;
  }

  .publish-actions {
    flex-direction: column;
    gap: 12px;
  }

  .action-buttons {
    width: 100%;
  }

  .action-buttons .el-button {
    flex: 1;
  }

  .preview-item {
    width: 70px;
    height: 70px;
  }

  .add-image-btn {
    width: 70px;
    height: 70px;
  }

  .images-grid {
    grid-template-columns: repeat(3, 1fr);
    gap: 8px;
  }

  .selected-item {
    width: 50px;
    height: 50px;
  }
}
</style>
