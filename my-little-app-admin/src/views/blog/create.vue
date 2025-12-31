<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'

const router = useRouter()
const route = useRoute()

const isEdit = computed(() => !!route.params.id)
const loading = ref(false)
const saving = ref(false)

const articleForm = reactive({
  title: '',
  category: '',
  tags: [],
  summary: '',
  content: '',
  cover: '',
  status: 'draft'
})

const rules = {
  title: [{ required: true, message: '请输入文章标题', trigger: 'blur' }],
  category: [{ required: true, message: '请选择分类', trigger: 'change' }],
  content: [{ required: true, message: '请输入文章内容', trigger: 'blur' }]
}

const formRef = ref(null)

const categories = [
  '前端开发',
  '后端开发',
  '移动开发',
  '工程化',
  '组件库',
  '算法',
  '面试',
  '其他'
]

const availableTags = [
  'Vue', 'React', 'TypeScript', 'JavaScript', 
  'Node.js', 'CSS', 'HTML', 'Webpack', 'Vite',
  'Element Plus', 'Ant Design', 'Tailwind CSS'
]

onMounted(() => {
  if (isEdit.value) {
    // 模拟加载文章数据
    loading.value = true
    setTimeout(() => {
      Object.assign(articleForm, {
        title: 'Vue3 组合式 API 最佳实践',
        category: '前端开发',
        tags: ['Vue', 'TypeScript'],
        summary: '本文将详细介绍Vue3组合式API的使用技巧和最佳实践...',
        content: `# Vue3 组合式 API 最佳实践

## 前言

Vue3 引入了组合式 API（Composition API），这是一种全新的组织组件逻辑的方式。

## 为什么使用组合式 API

1. 更好的逻辑复用
2. 更灵活的代码组织
3. 更好的类型推断

## 示例代码

\`\`\`javascript
import { ref, computed, onMounted } from 'vue'

export function useCounter() {
  const count = ref(0)
  const double = computed(() => count.value * 2)
  
  function increment() {
    count.value++
  }
  
  return { count, double, increment }
}
\`\`\`

## 总结

组合式 API 让我们能够更好地组织和复用代码逻辑。`,
        cover: 'https://picsum.photos/800/400?random=1',
        status: 'published'
      })
      loading.value = false
    }, 500)
  }
})

const handleSave = async (publish = false) => {
  if (!formRef.value) return
  
  await formRef.value.validate(async (valid) => {
    if (valid) {
      saving.value = true
      articleForm.status = publish ? 'published' : 'draft'
      
      setTimeout(() => {
        ElMessage.success(publish ? '发布成功' : '保存成功')
        saving.value = false
        router.push('/blog/list')
      }, 1000)
    }
  })
}

const handleCancel = () => {
  router.push('/blog/list')
}

const handleCoverUpload = () => {
  // 模拟上传
  articleForm.cover = `https://picsum.photos/800/400?random=${Date.now()}`
  ElMessage.success('封面上传成功')
}
</script>

<template>
  <div class="create-article" v-loading="loading">
    <div class="page-header">
      <div>
        <h1 class="page-title">{{ isEdit ? '编辑文章' : '写文章' }}</h1>
        <p class="page-subtitle">{{ isEdit ? '修改文章内容' : '创作新的博客文章' }}</p>
      </div>
      <div class="header-actions">
        <el-button @click="handleCancel">取消</el-button>
        <el-button @click="handleSave(false)" :loading="saving">保存草稿</el-button>
        <el-button type="primary" @click="handleSave(true)" :loading="saving">
          <el-icon><Upload /></el-icon>
          {{ isEdit ? '更新发布' : '立即发布' }}
        </el-button>
      </div>
    </div>
    
    <div class="editor-container">
      <div class="editor-main">
        <el-form 
          ref="formRef"
          :model="articleForm"
          :rules="rules"
          label-position="top"
        >
          <el-form-item prop="title">
            <el-input 
              v-model="articleForm.title"
              placeholder="请输入文章标题..."
              class="title-input"
            />
          </el-form-item>
          
          <el-form-item prop="content" label="文章内容">
            <el-input
              v-model="articleForm.content"
              type="textarea"
              placeholder="开始写作... (支持 Markdown 语法)"
              :autosize="{ minRows: 20, maxRows: 40 }"
              class="content-editor"
            />
          </el-form-item>
        </el-form>
      </div>
      
      <div class="editor-sidebar">
        <div class="sidebar-card">
          <h3 class="sidebar-title">文章设置</h3>
          
          <el-form 
            :model="articleForm"
            label-position="top"
          >
            <el-form-item label="分类">
              <el-select 
                v-model="articleForm.category" 
                placeholder="选择分类"
                style="width: 100%"
              >
                <el-option 
                  v-for="cat in categories" 
                  :key="cat" 
                  :label="cat" 
                  :value="cat" 
                />
              </el-select>
            </el-form-item>
            
            <el-form-item label="标签">
              <el-select
                v-model="articleForm.tags"
                multiple
                filterable
                allow-create
                default-first-option
                placeholder="选择或输入标签"
                style="width: 100%"
              >
                <el-option
                  v-for="tag in availableTags"
                  :key="tag"
                  :label="tag"
                  :value="tag"
                />
              </el-select>
            </el-form-item>
            
            <el-form-item label="摘要">
              <el-input
                v-model="articleForm.summary"
                type="textarea"
                placeholder="文章摘要（可选）"
                :rows="3"
              />
            </el-form-item>
          </el-form>
        </div>
        
        <div class="sidebar-card">
          <h3 class="sidebar-title">封面图片</h3>
          
          <div class="cover-upload">
            <div 
              v-if="articleForm.cover" 
              class="cover-preview"
            >
              <img :src="articleForm.cover" alt="封面" />
              <div class="cover-overlay">
                <el-button 
                  type="primary" 
                  size="small" 
                  @click="handleCoverUpload"
                >
                  更换
                </el-button>
                <el-button 
                  size="small" 
                  @click="articleForm.cover = ''"
                >
                  删除
                </el-button>
              </div>
            </div>
            <div 
              v-else 
              class="cover-placeholder"
              @click="handleCoverUpload"
            >
              <el-icon size="32"><Picture /></el-icon>
              <span>点击上传封面</span>
            </div>
          </div>
        </div>
        
        <div class="sidebar-card">
          <h3 class="sidebar-title">发布状态</h3>
          <el-radio-group v-model="articleForm.status" class="status-group">
            <el-radio value="draft">
              <div class="status-option">
                <el-icon><Edit /></el-icon>
                <span>草稿</span>
              </div>
            </el-radio>
            <el-radio value="published">
              <div class="status-option">
                <el-icon><Check /></el-icon>
                <span>已发布</span>
              </div>
            </el-radio>
          </el-radio-group>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.create-article {
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

.editor-container {
  display: grid;
  grid-template-columns: 1fr 320px;
  gap: 24px;
}

.editor-main {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: 16px;
  padding: 24px;
}

.title-input :deep(.el-input__wrapper) {
  box-shadow: none !important;
  background: transparent !important;
  padding: 0;
}

.title-input :deep(.el-input__inner) {
  font-size: 28px;
  font-weight: 600;
  height: 48px;
}

.content-editor :deep(.el-textarea__inner) {
  font-family: 'SF Mono', 'Fira Code', 'Monaco', monospace;
  font-size: 15px;
  line-height: 1.8;
  padding: 16px;
  border-radius: 12px;
}

.editor-sidebar {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.sidebar-card {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: 16px;
  padding: 20px;
}

.sidebar-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 16px;
}

.cover-upload {
  border-radius: 12px;
  overflow: hidden;
}

.cover-preview {
  position: relative;
  aspect-ratio: 16/9;
}

.cover-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cover-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  opacity: 0;
  transition: opacity 0.2s;
}

.cover-preview:hover .cover-overlay {
  opacity: 1;
}

.cover-placeholder {
  aspect-ratio: 16/9;
  background: var(--bg-darker);
  border: 2px dashed var(--border-color);
  border-radius: 12px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: var(--text-muted);
  cursor: pointer;
  transition: all 0.2s;
}

.cover-placeholder:hover {
  border-color: var(--primary-color);
  color: var(--primary-color);
}

.cover-placeholder span {
  font-size: 13px;
}

.status-group {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.status-option {
  display: flex;
  align-items: center;
  gap: 8px;
}

@media (max-width: 1024px) {
  .editor-container {
    grid-template-columns: 1fr;
  }
}
</style>

