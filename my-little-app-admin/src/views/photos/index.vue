<script setup>
import { ref, reactive, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const uploadDialogVisible = ref(false)
const albumDialogVisible = ref(false)
const selectedAlbum = ref('all')

const albumForm = reactive({
  id: null,
  name: '',
  description: '',
  cover: ''
})

const albums = ref([
  { id: 1, name: '风景', description: '美丽的自然风光', cover: 'https://picsum.photos/400/300?random=20', count: 12 },
  { id: 2, name: '生活', description: '日常生活记录', cover: 'https://picsum.photos/400/300?random=21', count: 8 },
  { id: 3, name: '旅行', description: '旅途中的精彩瞬间', cover: 'https://picsum.photos/400/300?random=22', count: 15 },
  { id: 4, name: '美食', description: '舌尖上的美味', cover: 'https://picsum.photos/400/300?random=23', count: 6 }
])

const photos = ref([
  { id: 1, url: 'https://picsum.photos/800/600?random=30', thumbnail: 'https://picsum.photos/400/300?random=30', album: 1, title: '山间日落', createTime: '2024-01-15' },
  { id: 2, url: 'https://picsum.photos/800/600?random=31', thumbnail: 'https://picsum.photos/400/300?random=31', album: 1, title: '湖光山色', createTime: '2024-01-14' },
  { id: 3, url: 'https://picsum.photos/800/600?random=32', thumbnail: 'https://picsum.photos/400/300?random=32', album: 2, title: '咖啡时光', createTime: '2024-01-13' },
  { id: 4, url: 'https://picsum.photos/800/600?random=33', thumbnail: 'https://picsum.photos/400/300?random=33', album: 3, title: '海边漫步', createTime: '2024-01-12' },
  { id: 5, url: 'https://picsum.photos/800/600?random=34', thumbnail: 'https://picsum.photos/400/300?random=34', album: 1, title: '云海奇观', createTime: '2024-01-11' },
  { id: 6, url: 'https://picsum.photos/800/600?random=35', thumbnail: 'https://picsum.photos/400/300?random=35', album: 4, title: '精致甜点', createTime: '2024-01-10' },
  { id: 7, url: 'https://picsum.photos/800/600?random=36', thumbnail: 'https://picsum.photos/400/300?random=36', album: 2, title: '阳台花园', createTime: '2024-01-09' },
  { id: 8, url: 'https://picsum.photos/800/600?random=37', thumbnail: 'https://picsum.photos/400/300?random=37', album: 3, title: '古镇小巷', createTime: '2024-01-08' },
  { id: 9, url: 'https://picsum.photos/800/600?random=38', thumbnail: 'https://picsum.photos/400/300?random=38', album: 1, title: '星空露营', createTime: '2024-01-07' },
  { id: 10, url: 'https://picsum.photos/800/600?random=39', thumbnail: 'https://picsum.photos/400/300?random=39', album: 4, title: '地道小吃', createTime: '2024-01-06' },
  { id: 11, url: 'https://picsum.photos/800/600?random=40', thumbnail: 'https://picsum.photos/400/300?random=40', album: 2, title: '书房一角', createTime: '2024-01-05' },
  { id: 12, url: 'https://picsum.photos/800/600?random=41', thumbnail: 'https://picsum.photos/400/300?random=41', album: 3, title: '雪山之巅', createTime: '2024-01-04' }
])

const filteredPhotos = computed(() => {
  if (selectedAlbum.value === 'all') {
    return photos.value
  }
  return photos.value.filter(p => p.album === selectedAlbum.value)
})

const selectedPhotos = ref([])
const previewVisible = ref(false)
const previewIndex = ref(0)

const previewList = computed(() => filteredPhotos.value.map(p => p.url))

const handleSelectPhoto = (photo) => {
  const index = selectedPhotos.value.indexOf(photo.id)
  if (index > -1) {
    selectedPhotos.value.splice(index, 1)
  } else {
    selectedPhotos.value.push(photo.id)
  }
}

const handleSelectAll = () => {
  if (selectedPhotos.value.length === filteredPhotos.value.length) {
    selectedPhotos.value = []
  } else {
    selectedPhotos.value = filteredPhotos.value.map(p => p.id)
  }
}

const handlePreview = (index) => {
  previewIndex.value = index
  previewVisible.value = true
}

const handleUpload = () => {
  // 模拟上传
  const newPhotos = []
  for (let i = 0; i < 3; i++) {
    const id = Date.now() + i
    newPhotos.push({
      id,
      url: `https://picsum.photos/800/600?random=${id}`,
      thumbnail: `https://picsum.photos/400/300?random=${id}`,
      album: selectedAlbum.value === 'all' ? 1 : selectedAlbum.value,
      title: `新照片 ${photos.value.length + i + 1}`,
      createTime: new Date().toISOString().split('T')[0]
    })
  }
  photos.value.unshift(...newPhotos)
  uploadDialogVisible.value = false
  ElMessage.success('上传成功')
}

const handleDeleteSelected = () => {
  if (selectedPhotos.value.length === 0) {
    ElMessage.warning('请先选择照片')
    return
  }
  
  ElMessageBox.confirm(`确定要删除选中的 ${selectedPhotos.value.length} 张照片吗？`, '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    photos.value = photos.value.filter(p => !selectedPhotos.value.includes(p.id))
    selectedPhotos.value = []
    ElMessage.success('删除成功')
  }).catch(() => {})
}

const handleCreateAlbum = () => {
  if (!albumForm.name.trim()) {
    ElMessage.warning('请输入相册名称')
    return
  }
  
  const newAlbum = {
    id: Date.now(),
    name: albumForm.name,
    description: albumForm.description,
    cover: `https://picsum.photos/400/300?random=${Date.now()}`,
    count: 0
  }
  albums.value.push(newAlbum)
  albumForm.name = ''
  albumForm.description = ''
  albumDialogVisible.value = false
  ElMessage.success('相册创建成功')
}

const getAlbumName = (albumId) => {
  const album = albums.value.find(a => a.id === albumId)
  return album ? album.name : '未分类'
}
</script>

<template>
  <div class="photos-page">
    <div class="page-header">
      <div>
        <h1 class="page-title">照片管理</h1>
        <p class="page-subtitle">管理你的图库和相册</p>
      </div>
      <div class="header-actions">
        <el-button @click="albumDialogVisible = true">
          <el-icon><FolderAdd /></el-icon>
          新建相册
        </el-button>
        <el-button type="primary" @click="uploadDialogVisible = true">
          <el-icon><Upload /></el-icon>
          上传照片
        </el-button>
      </div>
    </div>
    
    <!-- 相册选择 -->
    <div class="albums-section">
      <div class="section-header">
        <h2>相册</h2>
      </div>
      <div class="albums-scroll">
        <div 
          class="album-item"
          :class="{ active: selectedAlbum === 'all' }"
          @click="selectedAlbum = 'all'"
        >
          <div class="album-cover all-photos">
            <el-icon size="32"><PictureFilled /></el-icon>
          </div>
          <div class="album-info">
            <div class="album-name">全部照片</div>
            <div class="album-count">{{ photos.length }} 张</div>
          </div>
        </div>
        
        <div 
          v-for="album in albums"
          :key="album.id"
          class="album-item"
          :class="{ active: selectedAlbum === album.id }"
          @click="selectedAlbum = album.id"
        >
          <div class="album-cover">
            <img :src="album.cover" :alt="album.name" />
          </div>
          <div class="album-info">
            <div class="album-name">{{ album.name }}</div>
            <div class="album-count">{{ album.count }} 张</div>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 操作栏 -->
    <div class="toolbar">
      <div class="toolbar-left">
        <el-checkbox 
          :model-value="selectedPhotos.length === filteredPhotos.length && filteredPhotos.length > 0"
          :indeterminate="selectedPhotos.length > 0 && selectedPhotos.length < filteredPhotos.length"
          @change="handleSelectAll"
        >
          全选
        </el-checkbox>
        <span v-if="selectedPhotos.length" class="selected-count">
          已选择 {{ selectedPhotos.length }} 张
        </span>
      </div>
      <div class="toolbar-right">
        <el-button 
          v-if="selectedPhotos.length"
          type="danger" 
          text
          @click="handleDeleteSelected"
        >
          <el-icon><Delete /></el-icon>
          删除选中
        </el-button>
      </div>
    </div>
    
    <!-- 照片网格 -->
    <div class="photos-grid">
      <div 
        v-for="(photo, index) in filteredPhotos"
        :key="photo.id"
        class="photo-item"
        :class="{ selected: selectedPhotos.includes(photo.id) }"
      >
        <div class="photo-checkbox" @click.stop="handleSelectPhoto(photo)">
          <el-checkbox :model-value="selectedPhotos.includes(photo.id)" />
        </div>
        <img 
          :src="photo.thumbnail" 
          :alt="photo.title"
          @click="handlePreview(index)"
        />
        <div class="photo-overlay">
          <div class="photo-title">{{ photo.title }}</div>
          <div class="photo-meta">
            <span>{{ getAlbumName(photo.album) }}</span>
            <span>{{ photo.createTime }}</span>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 上传对话框 -->
    <el-dialog 
      v-model="uploadDialogVisible" 
      title="上传照片"
      width="600px"
    >
      <div class="upload-area">
        <el-upload
          drag
          multiple
          action="#"
          :auto-upload="false"
          accept="image/*"
        >
          <el-icon size="48" class="upload-icon"><UploadFilled /></el-icon>
          <div class="upload-text">
            <p>将图片拖拽到此处，或<em>点击上传</em></p>
            <p class="upload-hint">支持 JPG、PNG、GIF 格式，单张最大 10MB</p>
          </div>
        </el-upload>
        
        <div class="upload-options">
          <el-form-item label="选择相册">
            <el-select v-model="selectedAlbum" style="width: 200px">
              <el-option label="全部照片" value="all" />
              <el-option 
                v-for="album in albums"
                :key="album.id"
                :label="album.name"
                :value="album.id"
              />
            </el-select>
          </el-form-item>
        </div>
      </div>
      
      <template #footer>
        <el-button @click="uploadDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleUpload">开始上传</el-button>
      </template>
    </el-dialog>
    
    <!-- 新建相册对话框 -->
    <el-dialog 
      v-model="albumDialogVisible" 
      title="新建相册"
      width="400px"
    >
      <el-form label-width="80px">
        <el-form-item label="相册名称">
          <el-input v-model="albumForm.name" placeholder="请输入相册名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input 
            v-model="albumForm.description" 
            type="textarea" 
            :rows="3"
            placeholder="请输入相册描述（可选）" 
          />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="albumDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCreateAlbum">创建</el-button>
      </template>
    </el-dialog>
    
    <!-- 图片预览 -->
    <el-image-viewer
      v-if="previewVisible"
      :url-list="previewList"
      :initial-index="previewIndex"
      @close="previewVisible = false"
    />
  </div>
</template>

<style scoped>
.photos-page {
  max-width: 1400px;
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

.header-actions {
  display: flex;
  gap: 12px;
}

.albums-section {
  margin-bottom: 24px;
}

.section-header {
  margin-bottom: 16px;
}

.section-header h2 {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
}

.albums-scroll {
  display: flex;
  gap: 16px;
  overflow-x: auto;
  padding-bottom: 8px;
}

.album-item {
  flex-shrink: 0;
  width: 140px;
  cursor: pointer;
  transition: all 0.2s;
}

.album-item:hover {
  transform: translateY(-2px);
}

.album-item.active .album-cover {
  border-color: var(--primary-color);
  box-shadow: 0 0 0 2px rgba(13, 148, 136, 0.2);
}

.album-cover {
  width: 140px;
  height: 100px;
  border-radius: 12px;
  overflow: hidden;
  border: 2px solid transparent;
  margin-bottom: 8px;
}

.album-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.album-cover.all-photos {
  background: var(--gradient-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.album-info {
  text-align: center;
}

.album-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
  margin-bottom: 2px;
}

.album-count {
  font-size: 12px;
  color: var(--text-muted);
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  margin-bottom: 20px;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.selected-count {
  font-size: 14px;
  color: var(--primary-color);
}

.photos-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 16px;
}

.photo-item {
  position: relative;
  aspect-ratio: 4/3;
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  border: 2px solid transparent;
  transition: all 0.2s;
}

.photo-item:hover {
  transform: scale(1.02);
}

.photo-item.selected {
  border-color: var(--primary-color);
}

.photo-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.photo-checkbox {
  position: absolute;
  top: 8px;
  left: 8px;
  z-index: 10;
  width: 28px;
  height: 28px;
  background: rgba(0, 0, 0, 0.5);
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.2s;
}

.photo-item:hover .photo-checkbox,
.photo-item.selected .photo-checkbox {
  opacity: 1;
}

.photo-overlay {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 40px 12px 12px;
  background: linear-gradient(to top, rgba(0, 0, 0, 0.7), transparent);
  opacity: 0;
  transition: opacity 0.2s;
}

.photo-item:hover .photo-overlay {
  opacity: 1;
}

.photo-title {
  font-size: 14px;
  font-weight: 500;
  color: white;
  margin-bottom: 4px;
}

.photo-meta {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.7);
}

.upload-area {
  text-align: center;
}

.upload-area :deep(.el-upload-dragger) {
  background: var(--bg-darker);
  border-color: var(--border-color);
  border-radius: 12px;
  padding: 40px;
}

.upload-area :deep(.el-upload-dragger:hover) {
  border-color: var(--primary-color);
}

.upload-icon {
  color: var(--text-muted);
  margin-bottom: 16px;
}

.upload-text p {
  color: var(--text-secondary);
  margin-bottom: 8px;
}

.upload-text em {
  color: var(--primary-color);
  font-style: normal;
}

.upload-hint {
  font-size: 12px;
  color: var(--text-muted);
}

.upload-options {
  margin-top: 20px;
  text-align: left;
}
</style>

