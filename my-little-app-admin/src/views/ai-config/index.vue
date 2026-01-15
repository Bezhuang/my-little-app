<template>
  <div class="ai-config">
    <div class="page-header">
      <h2>AI 助手配置</h2>
    </div>

    <el-card class="config-card">
      <template #header>
        <div class="card-header">
          <span>系统提示词配置</span>
          <el-button type="primary" @click="savePrompt" :loading="saving">
            保存配置
          </el-button>
        </div>
      </template>

      <el-form label-position="top">
        <el-form-item label="系统提示词 (System Prompt)">
          <el-input
            v-model="systemPrompt"
            type="textarea"
            :rows="8"
            placeholder="设置 AI 助手的系统提示词，这会影响 AI 的回答风格和行为..."
          />
        </el-form-item>

        <el-form-item>
          <el-alert
            title="提示"
            type="info"
            :closable="false"
          >
            系统提示词决定了 AI 助手的角色定位和行为风格。建议包含：
            <ul style="margin: 10px 0 0 0; padding-left: 20px;">
              <li>AI 助手的身份和职责</li>
              <li>回答问题的风格和方式</li>
              <li>需要遵守的规则和限制</li>
            </ul>
          </el-alert>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="config-card">
      <template #header>
        <div class="card-header">
          <span>模型参数配置</span>
          <el-button type="primary" @click="saveTemperature" :loading="savingTemperature">
            保存配置
          </el-button>
        </div>
      </template>

      <el-form label-position="top">
        <el-form-item label="模型 Temperature (温度)">
          <el-slider
            v-model="temperature"
            :min="0"
            :max="2"
            :step="0.1"
            :format-tooltip="(val) => val.toFixed(1)"
          />
          <div style="color: #909399; font-size: 12px; margin-top: 5px;">
            当前值: {{ temperature }} | 范围: 0.0 (严谨) - 2.0 (创意)
          </div>
        </el-form-item>

        <el-form-item>
          <el-alert
            title="Temperature 说明"
            type="info"
            :closable="false"
          >
            <ul style="margin: 10px 0 0 0; padding-left: 20px;">
              <li>较低值 (0.0-0.3): 输出更确定性、聚焦、简洁</li>
              <li>中等值 (0.4-0.7): 平衡创造性和确定性</li>
              <li>较高值 (0.8-2.0): 输出更有创意、多样化，但可能不够准确</li>
            </ul>
          </el-alert>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="config-card">
      <template #header>
        <div class="card-header">
          <span>用户配额管理</span>
          <el-button type="primary" @click="fetchQuotaList" :loading="loadingQuota">
            刷新列表
          </el-button>
        </div>
      </template>

      <el-table :data="quotaList" v-loading="loadingQuota" stripe style="width: 100%">
        <el-table-column prop="username" label="用户名" min-width="120" />
        <el-table-column prop="role" label="角色" min-width="100">
          <template #default="{ row }">
            <el-tag :type="row.role === 'super_admin' ? 'danger' : row.role === 'admin' ? 'primary' : 'info'">
              {{ row.role === 'super_admin' ? '超级管理员' : row.role === 'admin' ? '管理员' : '用户' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="tokensRemaining" label="剩余 Token" min-width="120">
          <template #default="{ row }">
            <span>{{ formatNumber(row.tokensRemaining) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="searchRemaining" label="剩余搜索次数" min-width="120">
          <template #default="{ row }">
            <span>{{ row.searchRemaining }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openEditDialog(row)">
              修改配额
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 编辑配额对话框 -->
    <el-dialog
      v-model="editDialogVisible"
      title="修改用户配额"
      width="400px"
      :close-on-click-modal="false"
    >
      <el-form label-position="top">
        <el-form-item label="用户名">
          <el-input :value="editingUser?.username" disabled />
        </el-form-item>
        <el-form-item label="角色">
          <el-tag :type="editingUser?.role === 'super_admin' ? 'danger' : editingUser?.role === 'admin' ? 'primary' : 'info'">
            {{ editingUser?.role === 'super_admin' ? '超级管理员' : editingUser?.role === 'admin' ? '管理员' : '用户' }}
          </el-tag>
        </el-form-item>
        <el-form-item label="剩余 Token">
          <el-input-number
            v-model="editForm.tokensRemaining"
            :min="0"
            :max="999999999"
            controls-position="right"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="剩余搜索次数">
          <el-input-number
            v-model="editForm.searchRemaining"
            :min="0"
            :max="9999"
            controls-position="right"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveQuota" :loading="savingQuota">
          保存
        </el-button>
      </template>
    </el-dialog>

    <!-- DeepSeek AI 配置 -->
    <el-card class="config-card">
      <template #header>
        <div class="card-header">
          <span>DeepSeek AI 配置</span>
          <el-button type="primary" @click="saveDeepSeekConfig" :loading="savingDeepSeek">
            保存配置
          </el-button>
        </div>
      </template>

      <el-form label-position="top">
        <el-form-item label="API Key">
          <el-input
            v-model="deepSeekConfig.apiKey"
            type="password"
            show-password
            placeholder="请输入 DeepSeek API Key"
          />
        </el-form-item>
        <el-form-item label="模型名称">
          <el-input
            v-model="deepSeekConfig.model"
            placeholder="例如: deepseek-chat"
          />
        </el-form-item>
        <el-form-item label="Reasoner 模型名称">
          <el-input
            v-model="deepSeekConfig.reasonerModel"
            placeholder="例如: deepseek-reasoner"
          />
        </el-form-item>
        <el-form-item label="Base URL">
          <el-input
            v-model="deepSeekConfig.baseUrl"
            placeholder="例如: https://api.deepseek.com"
          />
        </el-form-item>
        <el-form-item label="最大 Token 数">
          <el-input-number
            v-model="deepSeekConfig.maxTokens"
            :min="1"
            :max="100000"
            controls-position="right"
          />
        </el-form-item>
        <el-form-item label="启用 DeepSeek">
          <el-switch v-model="deepSeekConfig.enabled" />
          <span style="margin-left: 10px; color: #909399; font-size: 12px;">
            {{ deepSeekConfig.enabled ? '已启用' : '已禁用' }}
          </span>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- SiliconFlow AI 配置 -->
    <el-card class="config-card">
      <template #header>
        <div class="card-header">
          <span>SiliconFlow AI 配置</span>
          <el-button type="primary" @click="saveSiliconFlowConfig" :loading="savingSiliconFlow">
            保存配置
          </el-button>
        </div>
      </template>

      <el-form label-position="top">
        <el-form-item label="API Key">
          <el-input
            v-model="siliconFlowConfig.apiKey"
            type="password"
            show-password
            placeholder="请输入 SiliconFlow API Key"
          />
        </el-form-item>
        <el-form-item label="模型名称">
          <el-input
            v-model="siliconFlowConfig.model"
            placeholder="例如: deepseek-ai/DeepSeek-V2.5"
          />
        </el-form-item>
        <el-form-item label="Reasoner 模型名称">
          <el-input
            v-model="siliconFlowConfig.reasonerModel"
            placeholder="例如: deepseek-ai/DeepSeek-V2.5"
          />
        </el-form-item>
        <el-form-item label="Base URL">
          <el-input
            v-model="siliconFlowConfig.baseUrl"
            placeholder="例如: https://api.siliconflow.cn/v1"
          />
        </el-form-item>
        <el-form-item label="最大 Token 数">
          <el-input-number
            v-model="siliconFlowConfig.maxTokens"
            :min="1"
            :max="100000"
            controls-position="right"
          />
        </el-form-item>
        <el-form-item label="启用 SiliconFlow">
          <el-switch v-model="siliconFlowConfig.enabled" />
          <span style="margin-left: 10px; color: #909399; font-size: 12px;">
            {{ siliconFlowConfig.enabled ? '已启用' : '已禁用' }}
          </span>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- Bocha Web Search API 配置 -->
    <el-card class="config-card">
      <template #header>
        <div class="card-header">
          <span>Bocha Web Search API 配置</span>
          <el-button type="primary" @click="saveBochaConfig" :loading="savingBocha">
            保存配置
          </el-button>
        </div>
      </template>

      <el-form label-position="top">
        <el-form-item label="API Key">
          <el-input
            v-model="bochaConfig.apiKey"
            type="password"
            show-password
            placeholder="请输入 Bocha Web Search API Key"
          />
        </el-form-item>
        <el-form-item label="搜索结果数量限制">
          <el-input-number
            v-model="bochaConfig.searchLimit"
            :min="1"
            :max="20"
            controls-position="right"
          />
          <span style="margin-left: 10px; color: #909399; font-size: 12px;">
            范围: 1-20
          </span>
        </el-form-item>
        <el-form-item label="启用 Bocha Web Search">
          <el-switch v-model="bochaConfig.enabled" />
          <span style="margin-left: 10px; color: #909399; font-size: 12px;">
            {{ bochaConfig.enabled ? '已启用' : '已禁用' }}
          </span>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { aiApi } from '../../utils/api'

const saving = ref(false)
const systemPrompt = ref('')

// 温度配置相关
const savingTemperature = ref(false)
const temperature = ref(0.7)

// 配额管理相关
const loadingQuota = ref(false)
const quotaList = ref([])
const editDialogVisible = ref(false)
const savingQuota = ref(false)
const editingUser = ref(null)
const editForm = reactive({
  tokensRemaining: 0,
  searchRemaining: 0
})

// DeepSeek AI 配置
const savingDeepSeek = ref(false)
const deepSeekConfig = reactive({
  apiKey: '',
  model: 'deepseek-chat',
  reasonerModel: 'deepseek-reasoner',
  baseUrl: 'https://api.deepseek.com',
  maxTokens: 4096,
  enabled: true
})

// SiliconFlow AI 配置
const savingSiliconFlow = ref(false)
const siliconFlowConfig = reactive({
  apiKey: '',
  model: 'deepseek-ai/DeepSeek-V2.5',
  reasonerModel: 'deepseek-ai/DeepSeek-V2.5',
  baseUrl: 'https://api.siliconflow.cn/v1',
  maxTokens: 4096,
  enabled: false
})

// Bocha Web Search API 配置
const savingBocha = ref(false)
const bochaConfig = reactive({
  apiKey: '',
  searchLimit: 5,
  enabled: false
})

// 格式化数字
const formatNumber = (num) => {
  if (num === null || num === undefined) return '0'
  return num.toLocaleString()
}

// 获取系统提示词
const fetchSystemPrompt = async () => {
  try {
    const res = await aiApi.getSystemPrompt()
    // request.js 返回的是 { success: true, data: {...} } 格式
    // res.data 包含实际的配置数据
    if (res && res.data && res.data.configValue !== undefined) {
      systemPrompt.value = res.data.configValue || ''
    } else if (res && res.configValue !== undefined) {
      systemPrompt.value = res.configValue || ''
    }
  } catch (error) {
    ElMessage.error('获取系统提示词失败: ' + error.message)
  }
}

// 保存系统提示词
const savePrompt = async () => {
  if (!systemPrompt.value.trim()) {
    ElMessage.warning('系统提示词不能为空')
    return
  }

  try {
    saving.value = true
    await aiApi.updateSystemPrompt(systemPrompt.value)
    ElMessage.success('保存成功')
  } catch (error) {
    ElMessage.error('保存失败：' + error.message)
  } finally {
    saving.value = false
  }
}

// 获取模型温度
const fetchTemperature = async () => {
  try {
    const res = await aiApi.getTemperature()
    if (res && res.data && res.data.configValue !== undefined) {
      temperature.value = parseFloat(res.data.configValue) || 0.7
    }
  } catch (error) {
    ElMessage.error('获取温度配置失败: ' + error.message)
  }
}

// 保存模型温度
const saveTemperature = async () => {
  try {
    savingTemperature.value = true
    await aiApi.updateTemperature(temperature.value.toString())
    ElMessage.success('保存成功')
  } catch (error) {
    ElMessage.error('保存失败：' + error.message)
  } finally {
    savingTemperature.value = false
  }
}

// 获取配额列表
const fetchQuotaList = async () => {
  try {
    loadingQuota.value = true
    const res = await aiApi.getQuotaList()
    if (res && res.success && res.data) {
      quotaList.value = res.data
    }
  } catch (error) {
    ElMessage.error('获取配额列表失败: ' + error.message)
  } finally {
    loadingQuota.value = false
  }
}

// 打开编辑对话框
const openEditDialog = (user) => {
  editingUser.value = user
  editForm.tokensRemaining = user.tokensRemaining || 0
  editForm.searchRemaining = user.searchRemaining || 0
  editDialogVisible.value = true
}

// 保存配额
const saveQuota = async () => {
  if (!editingUser.value) return

  try {
    savingQuota.value = true
    await aiApi.updateQuota(
      editingUser.value.userId,
      editForm.tokensRemaining,
      editForm.searchRemaining
    )
    ElMessage.success('配额更新成功')
    editDialogVisible.value = false
    // 刷新列表
    await fetchQuotaList()
  } catch (error) {
    ElMessage.error('更新配额失败: ' + error.message)
  } finally {
    savingQuota.value = false
  }
}

// DeepSeek AI 配置相关方法
const fetchDeepSeekConfig = async () => {
  try {
    const res = await aiApi.getDeepSeekConfig()
    if (res && res.data) {
      deepSeekConfig.apiKey = res.data.apiKey || ''
      deepSeekConfig.model = res.data.model || 'deepseek-chat'
      deepSeekConfig.reasonerModel = res.data.reasonerModel || 'deepseek-reasoner'
      deepSeekConfig.baseUrl = res.data.baseUrl || 'https://api.deepseek.com'
      deepSeekConfig.maxTokens = parseInt(res.data.maxTokens) || 4096
      deepSeekConfig.enabled = res.data.enabled === 'true'
    }
  } catch (error) {
    ElMessage.error('获取 DeepSeek 配置失败: ' + error.message)
  }
}

const saveDeepSeekConfig = async () => {
  try {
    savingDeepSeek.value = true
    await aiApi.updateDeepSeekConfig({
      apiKey: deepSeekConfig.apiKey,
      model: deepSeekConfig.model,
      reasonerModel: deepSeekConfig.reasonerModel,
      baseUrl: deepSeekConfig.baseUrl,
      maxTokens: deepSeekConfig.maxTokens.toString(),
      enabled: deepSeekConfig.enabled.toString()
    })
    ElMessage.success('DeepSeek 配置保存成功')
  } catch (error) {
    ElMessage.error('保存 DeepSeek 配置失败: ' + error.message)
  } finally {
    savingDeepSeek.value = false
  }
}

// SiliconFlow AI 配置相关方法
const fetchSiliconFlowConfig = async () => {
  try {
    const res = await aiApi.getSiliconFlowConfig()
    if (res && res.data) {
      siliconFlowConfig.apiKey = res.data.apiKey || ''
      siliconFlowConfig.model = res.data.model || 'deepseek-ai/DeepSeek-V2.5'
      siliconFlowConfig.reasonerModel = res.data.reasonerModel || 'deepseek-ai/DeepSeek-V2.5'
      siliconFlowConfig.baseUrl = res.data.baseUrl || 'https://api.siliconflow.cn/v1'
      siliconFlowConfig.maxTokens = parseInt(res.data.maxTokens) || 4096
      siliconFlowConfig.enabled = res.data.enabled === 'true'
    }
  } catch (error) {
    ElMessage.error('获取 SiliconFlow 配置失败: ' + error.message)
  }
}

const saveSiliconFlowConfig = async () => {
  try {
    savingSiliconFlow.value = true
    await aiApi.updateSiliconFlowConfig({
      apiKey: siliconFlowConfig.apiKey,
      model: siliconFlowConfig.model,
      reasonerModel: siliconFlowConfig.reasonerModel,
      baseUrl: siliconFlowConfig.baseUrl,
      maxTokens: siliconFlowConfig.maxTokens.toString(),
      enabled: siliconFlowConfig.enabled.toString()
    })
    ElMessage.success('SiliconFlow 配置保存成功')
  } catch (error) {
    ElMessage.error('保存 SiliconFlow 配置失败: ' + error.message)
  } finally {
    savingSiliconFlow.value = false
  }
}

// Bocha Web Search API 配置相关方法
const fetchBochaConfig = async () => {
  try {
    const res = await aiApi.getBochaConfig()
    if (res && res.data) {
      bochaConfig.apiKey = res.data.apiKey || ''
      bochaConfig.searchLimit = parseInt(res.data.searchLimit) || 5
      bochaConfig.enabled = res.data.enabled === 'true'
    }
  } catch (error) {
    ElMessage.error('获取 Bocha 配置失败: ' + error.message)
  }
}

const saveBochaConfig = async () => {
  try {
    savingBocha.value = true
    await aiApi.updateBochaConfig({
      apiKey: bochaConfig.apiKey,
      searchLimit: bochaConfig.searchLimit.toString(),
      enabled: bochaConfig.enabled.toString()
    })
    ElMessage.success('Bocha 配置保存成功')
  } catch (error) {
    ElMessage.error('保存 Bocha 配置失败: ' + error.message)
  } finally {
    savingBocha.value = false
  }
}

onMounted(() => {
  fetchSystemPrompt()
  fetchTemperature()
  fetchQuotaList()
  fetchDeepSeekConfig()
  fetchSiliconFlowConfig()
  fetchBochaConfig()
})
</script>

<style scoped>
.ai-config {
  padding: 20px;
}

.page-header {
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0;
  font-size: 24px;
  color: #303133;
}

.config-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
