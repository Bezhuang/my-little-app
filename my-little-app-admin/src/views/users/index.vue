<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { userApi } from '../../utils/api'

const loading = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('新增用户')
const isEdit = ref(false)

const searchForm = reactive({
  keyword: '',
  status: ''
})

const userForm = reactive({
  id: null,
  username: '',
  email: '',
  phone: '',
  status: 1
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ]
}

const formRef = ref(null)

const users = ref([])
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0
})

// 加载用户列表
const fetchUsers = async () => {
  loading.value = true
  try {
    const res = await userApi.getList({
      page: pagination.current,
      pageSize: pagination.pageSize,
      keyword: searchForm.keyword || undefined,
      status: searchForm.status ? parseInt(searchForm.status) : undefined
    })
    if (res.data) {
      users.value = res.data.list || []
      pagination.total = res.data.total || 0
    }
  } catch (error) {
    ElMessage.error(error.message || '获取用户列表失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.current = 1
  fetchUsers()
}

const handleReset = () => {
  searchForm.keyword = ''
  searchForm.status = ''
  handleSearch()
}

const handleAdd = () => {
  isEdit.value = false
  dialogTitle.value = '新增用户'
  Object.assign(userForm, {
    id: null,
    username: '',
    email: '',
    phone: '',
    status: 1
  })
  dialogVisible.value = true
}

const handleEdit = (row) => {
  isEdit.value = true
  dialogTitle.value = '编辑用户'
  Object.assign(userForm, {
    id: row.id,
    username: row.username,
    email: row.email,
    phone: row.phone,
    status: row.status
  })
  dialogVisible.value = true
}

const handleDelete = (row) => {
  ElMessageBox.confirm(`确定要删除用户 "${row.username}" 吗？`, '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await userApi.delete(row.id)
      ElMessage.success('删除成功')
      fetchUsers()
    } catch (error) {
      ElMessage.error(error.message || '删除失败')
    }
  }).catch(() => {})
}

const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (valid) {
      try {
        if (isEdit.value) {
          await userApi.update(userForm)
          ElMessage.success('更新成功')
        } else {
          await userApi.add(userForm)
          ElMessage.success('添加成功')
        }
        dialogVisible.value = false
        fetchUsers()
      } catch (error) {
        ElMessage.error(error.message || '操作失败')
      }
    }
  })
}

const handleResetPassword = (row) => {
  ElMessageBox.confirm(`确定要将用户 "${row.username}" 的密码重置为默认密码吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await userApi.resetPassword(row.id)
      ElMessage.success('密码已重置为 123456')
    } catch (error) {
      ElMessage.error(error.message || '重置失败')
    }
  }).catch(() => {})
}

const handleSizeChange = (size) => {
  pagination.pageSize = size
  fetchUsers()
}

const handleCurrentChange = (page) => {
  pagination.current = page
  fetchUsers()
}

const getStatusTag = (status) => {
  return status === 1
    ? { type: 'success', label: '正常' }
    : { type: 'danger', label: '禁用' }
}

// 页面加载时获取数据
onMounted(() => {
  fetchUsers()
})
</script>

<template>
  <div class="users-page">
    <div class="page-header">
      <div>
        <h1 class="page-title">用户管理</h1>
        <p class="page-subtitle">管理系统用户账号信息</p>
      </div>
      <el-button type="primary" @click="handleAdd">
        <el-icon><Plus /></el-icon>
        新增用户
      </el-button>
    </div>
    
    <!-- 搜索栏 -->
    <div class="search-card">
      <el-form :model="searchForm" inline>
        <el-form-item label="关键词">
          <el-input 
            v-model="searchForm.keyword" 
            placeholder="用户名/邮箱/手机号"
            clearable
            style="width: 200px"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部" clearable style="width: 120px">
            <el-option label="正常" :value="1" />
            <el-option label="禁用" :value="0" />
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
    
    <!-- 用户表格 -->
    <div class="table-card">
      <el-table 
        :data="users" 
        v-loading="loading"
        stripe
        style="width: 100%"
      >
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" min-width="120">
          <template #default="{ row }">
            <div class="user-cell">
              <el-avatar :size="32" class="user-avatar">
                {{ row.username ? row.username[0].toUpperCase() : '?' }}
              </el-avatar>
              <span>{{ row.username }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="email" label="邮箱" min-width="180" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="getStatusTag(row.status).type" size="small">
              {{ getStatusTag(row.status).label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button type="primary" text size="small" @click="handleEdit(row)" title="编辑">
                <el-icon><Edit /></el-icon>
              </el-button>
              <el-button type="warning" text size="small" @click="handleResetPassword(row)" title="重置密码">
                <el-icon><Key /></el-icon>
              </el-button>
              <el-button type="danger" text size="small" @click="handleDelete(row)" title="删除">
                <el-icon><Delete /></el-icon>
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
      
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.current"
          v-model:page-size="pagination.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </div>
    
    <!-- 用户表单对话框 -->
    <el-dialog 
      v-model="dialogVisible" 
      :title="dialogTitle"
      width="500px"
      destroy-on-close
    >
      <el-form 
        ref="formRef"
        :model="userForm" 
        :rules="rules" 
        label-width="80px"
      >
        <el-form-item label="用户名" prop="username">
          <el-input v-model="userForm.username" placeholder="请输入用户名" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="userForm.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="userForm.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="userForm.status">
            <el-radio :value="1">正常</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.users-page {
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
  margin-bottom: 20px;
}

.table-card {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  padding: 20px;
}

.user-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.user-avatar {
  background: var(--gradient-primary);
}

.pagination-wrapper {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

:deep(.el-form-item__label) {
  color: var(--text-secondary);
}

.action-buttons {
  display: flex;
  gap: 4px;
}

.action-buttons .el-button {
  padding: 4px 8px;
  margin: 0;
}

/* 移动端适配 */
@media (max-width: 768px) {
  .users-page {
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

  .search-card {
    padding: 12px;
    border-radius: 8px;
    margin-bottom: 12px;
  }

  .search-card .el-form {
    display: flex;
    flex-direction: column;
    gap: 8px;
  }

  .search-card .el-form-item {
    margin-bottom: 0;
    margin-right: 0;
  }

  .search-card .el-input,
  .search-card .el-select {
    width: 100% !important;
  }

  .search-card .el-form-item:last-child {
    display: flex;
    justify-content: flex-end;
    gap: 8px;
  }

  .table-card {
    padding: 12px;
    border-radius: 8px;
    overflow-x: auto;
  }

  .el-table {
    font-size: 13px;
  }

  .el-table .cell {
    padding: 8px 12px;
  }

  .user-cell {
    gap: 8px;
  }

  .user-avatar {
    width: 28px !important;
    height: 28px !important;
    font-size: 12px;
  }

  .action-buttons .el-button {
    padding: 6px;
    min-width: 28px;
  }

  .pagination-wrapper {
    margin-top: 12px;
    justify-content: center;
  }

  .el-pagination {
    justify-content: center;
    flex-wrap: wrap;
    gap: 8px;
  }
}
</style>

