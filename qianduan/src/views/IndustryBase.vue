<template>
  <div class="industry-base-container">
    <el-card class="data-card">
      <template #header>
        <div class="card-header">
          <span>行业板块基础数据</span>
        </div>
      </template>
      
      <div v-loading="loading" element-loading-text="加载中..." class="loading-container">
        <el-alert
          v-if="error"
          :title="error"
          type="error"
          show-icon
          :closable="false"
          class="error-alert"
        />
        
        <div class="table-wrapper">
          <el-table v-if="!error && industryData.length > 0" :data="industryData" style="width: 100%" border>
          <el-table-column label="操作" width="120" fixed="left">
            <template #default="scope">
              <el-button size="small" type="primary" @click="handleHistoryDetail(scope.row.f12)">
                历史详细
              </el-button>
            </template>
          </el-table-column>
          <el-table-column prop="f12" label="行业代码" width="100" />
          <el-table-column prop="f14" label="行业名称" width="120" />
          <el-table-column prop="f2" label="收盘价" width="100">
            <template #default="scope">
              {{ formatNumber(scope.row.f2) }}
            </template>
          </el-table-column>
          <el-table-column prop="f3" label="涨跌幅" width="100">
            <template #default="scope">
              <span :class="scope.row.f3 >= 0 ? 'rise' : 'fall'">
                {{ formatPercent(scope.row.f3) }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="f4" label="涨跌额" width="100">
            <template #default="scope">
              <span :class="scope.row.f4 >= 0 ? 'rise' : 'fall'">
                {{ formatNumber(scope.row.f4) }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="f5" label="成交量" width="120">
            <template #default="scope">
              {{ formatVolume(scope.row.f5) }}
            </template>
          </el-table-column>
          <el-table-column prop="f6" label="成交额" width="120">
            <template #default="scope">
              {{ formatAmount(scope.row.f6) }}
            </template>
          </el-table-column>
          <el-table-column prop="f7" label="振幅" width="100">
            <template #default="scope">
              {{ formatPercent(scope.row.f7) }}
            </template>
          </el-table-column>
          <el-table-column prop="f8" label="换手率" width="100">
            <template #default="scope">
              {{ formatPercent(scope.row.f8) }}
            </template>
          </el-table-column>
          <el-table-column prop="f9" label="市盈率(动)" width="120">
            <template #default="scope">
              {{ formatNumber(scope.row.f9) }}
            </template>
          </el-table-column>
          <el-table-column prop="f10" label="量比" width="100">
            <template #default="scope">
              {{ formatNumber(scope.row.f10) }}
            </template>
          </el-table-column>
          <el-table-column prop="f15" label="最高价" width="100">
            <template #default="scope">
              {{ formatNumber(scope.row.f15) }}
            </template>
          </el-table-column>
          <el-table-column prop="f16" label="最低价" width="100">
            <template #default="scope">
              {{ formatNumber(scope.row.f16) }}
            </template>
          </el-table-column>
          <el-table-column prop="f17" label="今开" width="100">
            <template #default="scope">
              {{ formatNumber(scope.row.f17) }}
            </template>
          </el-table-column>
          <el-table-column prop="f18" label="昨收" width="100">
            <template #default="scope">
              {{ formatNumber(scope.row.f18) }}
            </template>
          </el-table-column>
          <el-table-column prop="f20" label="总市值" width="120">
            <template #default="scope">
              {{ formatAmount(scope.row.f20) }}
            </template>
          </el-table-column>
          <el-table-column prop="f21" label="流动市值" width="120">
            <template #default="scope">
              {{ formatAmount(scope.row.f21) }}
            </template>
          </el-table-column>
          <el-table-column prop="f22" label="涨速" width="100">
            <template #default="scope">
              {{ formatPercent(scope.row.f22) }}
            </template>
          </el-table-column>
          <el-table-column prop="f24" label="60日涨幅" width="100">
            <template #default="scope">
              <span :class="scope.row.f24 >= 0 ? 'rise' : 'fall'">
                {{ formatPercent(scope.row.f24) }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="f25" label="今年涨跌幅" width="120">
            <template #default="scope">
              <span :class="scope.row.f25 >= 0 ? 'rise' : 'fall'">
                {{ formatPercent(scope.row.f25) }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="f62" label="主力净流入额" width="120">
            <template #default="scope">
              <span :class="scope.row.f62 >= 0 ? 'rise' : 'fall'">
                {{ formatAmount(scope.row.f62) }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="f104" label="涨家数" width="100" />
          <el-table-column prop="f105" label="跌家数" width="100" />
          <el-table-column prop="f106" label="平家数" width="100" />
          <el-table-column prop="f115" label="市盈率TTM" width="120">
            <template #default="scope">
              {{ formatNumber(scope.row.f115) }}
            </template>
          </el-table-column>
          <el-table-column prop="f128" label="领涨股编码" width="120" />
          <el-table-column prop="f140" label="领涨股代码" width="120" />
          </el-table>
        </div>
        
        <el-empty v-if="!loading && !error && industryData.length === 0" description="暂无数据" />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'

const router = useRouter()
const loading = ref(false)
const error = ref('')
const industryData = ref<any[]>([])

// 格式化数字
const formatNumber = (value: number | string): string => {
  if (value === undefined || value === null || value === '-') return '-'
  const num = Number(value)
  return isNaN(num) ? '-' : num.toFixed(2)
}

// 格式化百分比
const formatPercent = (value: number | string): string => {
  if (value === undefined || value === null || value === '-') return '-'
  const num = Number(value)
  return isNaN(num) ? '-' : (num).toFixed(2) + '%'
}

// 格式化成交量
const formatVolume = (value: number | string): string => {
  if (value === undefined || value === null || value === '-') return '-'
  const num = Number(value)
  if (isNaN(num)) return '-'
  if (num >= 100000000) {
    return (num / 100000000).toFixed(2) + '亿'
  } else if (num >= 10000) {
    return (num / 10000).toFixed(2) + '万'
  }
  return num.toString()
}

// 格式化成交额
const formatAmount = (value: number | string): string => {
  if (value === undefined || value === null || value === '-') return '-'
  const num = Number(value)
  if (isNaN(num)) return '-'
  if (num >= 100000000) {
    return (num / 100000000).toFixed(2) + '亿'
  } else if (num >= 10000) {
    return (num / 10000).toFixed(2) + '万'
  }
  return num.toString()
}

// 处理历史详细按钮点击
const handleHistoryDetail = (industryCode: string) => {
  router.push({
    path: '/industry-kline',
    query: { industryCode }
  })
}

// 获取行业板块基础数据
const fetchIndustryBaseData = async () => {
  loading.value = true
  error.value = ''
  
  try {
    const response = await axios.get('http://localhost:8080/api/industry/base')
    console.log('API返回原始数据:', response.data)
    
    // 解析后端返回的JSON字符串
    let parsedData
    if (typeof response.data === 'string') {
      parsedData = JSON.parse(response.data)
    } else {
      parsedData = response.data
    }
    
    console.log('解析后的数据:', parsedData)
    
    if (parsedData && parsedData.data && parsedData.data.diff) {
      industryData.value = parsedData.data.diff
    } else {
      error.value = '获取数据失败：数据格式不正确'
    }
  } catch (err) {
    console.error('获取行业板块基础数据失败:', err)
    error.value = '获取数据失败：网络错误'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchIndustryBaseData()
})
</script>

<style scoped>
.industry-base-container {
  padding: 20px;
  height: 100%;
  box-sizing: border-box;
  overflow: auto;
}

.data-card {
  height: 100%;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.loading-container {
  flex: 1;
  min-height: 300px;
  max-height: calc(100vh - 200px);
  overflow: auto;
  overflow-x: auto;
}

.table-wrapper {
  width: 100%;
  overflow-x: auto;
}

.el-table {
  width: auto !important;
  min-width: 100%;
  max-width: none !important;
}

.loading-container::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

.loading-container::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 4px;
}

.loading-container::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 4px;
}

.loading-container::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}

.error-alert {
  margin-bottom: 20px;
}

.rise {
  color: #f56c6c;
}

.fall {
  color: #67c23a;
}

.el-table {
  font-size: 12px;
}

.el-table th {
  background-color: #f5f7fa;
  font-weight: bold;
}
</style>
