<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const loading = ref(false)

const searchForm = reactive({
  keyword: '',
  status: '',
  category: ''
})

const articles = ref([
  { 
    id: 1, 
    title: 'Vue3 组合式 API 最佳实践', 
    category: '前端开发',
    author: 'Admin',
    cover: 'https://picsum.photos/300/200?random=1',
    views: 1256,
    likes: 89,
    status: 'published',
    createTime: '2024-01-15 10:30:00',
    updateTime: '2024-01-15 14:20:00'
  },
  { 
    id: 2, 
    title: 'TypeScript 高级类型技巧总结', 
    category: '前端开发',
    author: 'Admin',
    cover: 'https://picsum.photos/300/200?random=2',
    views: 890,
    likes: 56,
    status: 'published',
    createTime: '2024-01-14 09:15:00',
    updateTime: '2024-01-14 16:45:00'
  },
  { 
    id: 3, 
    title: 'Vite 构建优化实战指南', 
    category: '工程化',
    author: 'Admin',
    cover: 'https://picsum.photos/300/200?random=3',
    views: 456,
    likes: 23,
    status: 'draft',
    createTime: '2024-01-13 11:20:00',
    updateTime: '2024-01-13 11:20:00'
  },
  { 
    id: 4, 
    title: 'Element Plus 主题定制详解', 
    category: '组件库',
    author: 'Admin',
    cover: 'https://picsum.photos/300/200?random=4',
    views: 678,
    likes: 45,
    status: 'published',
    createTime: '2024-01-12 14:00:00',
    updateTime: '2024-01-12 18:30:00'
  },
  { 
    id: 5, 
    title: 'Pinia 状态管理实践', 
    category: '前端开发',
    author: 'Admin',
    cover: 'https://picsum.photos/300/200?random=5',
    views: 543,
    likes: 34,
    status: 'published',
    createTime: '2024-01-11 16:45:00',
    updateTime: '2024-01-11 20:10:00'
  }
])

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 5
})

const handleSearch = () => {
  ElMessage.info('搜索功能待后端API接入')
}

const handleReset = () => {
  searchForm.keyword = ''
  searchForm.status = ''
  searchForm.category = ''
}

const handleCreate = () => {
  router.push('/blog/create')
}

const handleEdit = (row) => {
  router.push(`/blog/edit/${row.id}`)
}

const handleDelete = (row) => {
  ElMessageBox.confirm(`确定要删除文章 "${row.title}" 吗？`, '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    const index = articles.value.findIndex(a => a.id === row.id)
    if (index > -1) {
      articles.value.splice(index, 1)
      ElMessage.success('删除成功')
    }
  }).catch(() => {})
}

const handlePublish = (row) => {
  row.status = row.status === 'published' ? 'draft' : 'published'
  ElMessage.success(row.status === 'published' ? '发布成功' : '已取消发布')
}

const getStatusTag = (status) => {
  return status === 'published' 
    ? { type: 'success', label: '已发布' }
    : { type: 'warning', label: '草稿' }
}
</script>

<template>
  <div class="blog-page">
    <div class="page-header">
      <div>
        <h1 class="page-title">文章管理</h1>
        <p class="page-subtitle">管理博客文章内容</p>
      </div>
      <el-button type="primary" @click="handleCreate">
        <el-icon><Plus /></el-icon>
        写文章
      </el-button>
    </div>
    
    <!-- 搜索栏 -->
    <div class="search-card">
      <el-form :model="searchForm" inline>
        <el-form-item label="关键词">
          <el-input 
            v-model="searchForm.keyword" 
            placeholder="文章标题"
            clearable
            style="width: 200px"
          />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="searchForm.category" placeholder="全部" clearable style="width: 140px">
            <el-option label="前端开发" value="前端开发" />
            <el-option label="后端开发" value="后端开发" />
            <el-option label="工程化" value="工程化" />
            <el-option label="组件库" value="组件库" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部" clearable style="width: 120px">
            <el-option label="已发布" value="published" />
            <el-option label="草稿" value="draft" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>
    
    <!-- 文章列表 -->
    <div class="articles-grid">
      <div 
        v-for="article in articles" 
        :key="article.id" 
        class="article-card"
      >
        <div class="article-cover">
          <img :src="article.cover" :alt="article.title" />
          <el-tag 
            :type="getStatusTag(article.status).type" 
            class="status-badge"
          >
            {{ getStatusTag(article.status).label }}
          </el-tag>
        </div>
        <div class="article-content">
          <div class="article-category">{{ article.category }}</div>
          <h3 class="article-title">{{ article.title }}</h3>
          <div class="article-meta">
            <span class="meta-item">
              <el-icon><View /></el-icon>
              {{ article.views }}
            </span>
            <span class="meta-item">
              <el-icon><Star /></el-icon>
              {{ article.likes }}
            </span>
            <span class="meta-item">
              <el-icon><Clock /></el-icon>
              {{ article.createTime.split(' ')[0] }}
            </span>
          </div>
        </div>
        <div class="article-actions">
          <el-button type="primary" text size="small" @click="handleEdit(article)">
            <el-icon><Edit /></el-icon>
            编辑
          </el-button>
          <el-button 
            :type="article.status === 'published' ? 'warning' : 'success'" 
            text 
            size="small" 
            @click="handlePublish(article)"
          >
            <el-icon><component :is="article.status === 'published' ? 'Hide' : 'View'" /></el-icon>
            {{ article.status === 'published' ? '下架' : '发布' }}
          </el-button>
          <el-button type="danger" text size="small" @click="handleDelete(article)">
            <el-icon><Delete /></el-icon>
            删除
          </el-button>
        </div>
      </div>
    </div>
    
    <!-- 分页 -->
    <div class="pagination-wrapper">
      <el-pagination
        v-model:current-page="pagination.current"
        v-model:page-size="pagination.pageSize"
        :page-sizes="[10, 20, 50]"
        :total="pagination.total"
        layout="total, sizes, prev, pager, next"
      />
    </div>
  </div>
</template>

<style scoped>
.blog-page {
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

.search-card {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 24px;
}

.articles-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 20px;
  margin-bottom: 24px;
}

.article-card {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: 16px;
  overflow: hidden;
  transition: all 0.3s ease;
}

.article-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.2);
}

.article-cover {
  position: relative;
  height: 180px;
  overflow: hidden;
}

.article-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.article-card:hover .article-cover img {
  transform: scale(1.05);
}

.status-badge {
  position: absolute;
  top: 12px;
  right: 12px;
}

.article-content {
  padding: 20px;
}

.article-category {
  font-size: 12px;
  color: var(--primary-color);
  font-weight: 500;
  margin-bottom: 8px;
}

.article-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  line-height: 1.4;
  margin-bottom: 12px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.article-meta {
  display: flex;
  gap: 16px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: var(--text-muted);
}

.article-actions {
  display: flex;
  justify-content: flex-end;
  gap: 4px;
  padding: 12px 16px;
  border-top: 1px solid var(--border-color);
  background: var(--bg-darker);
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
}
</style>

