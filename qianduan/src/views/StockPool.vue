<template>
  <div class="stock-pool-container">
    <el-card class="data-card">
      <template #header>
        <div class="card-header">
          <span>读取股票池</span>
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
        
        <div v-if="!error && stockPoolData.length > 0" class="table-container">
          <el-table :data="stockPoolData" style="width: 100%" border height="calc(100vh - 300px)" max-height="calc(100vh - 300px)" :row-key="row => row.f12">
            <el-table-column prop="f12" label="代码" width="100" />
            <el-table-column prop="f14" label="名称" width="120" />
            <el-table-column prop="f2" label="最新价" width="100">
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
            <el-table-column prop="f5" label="成交量(手)" width="120">
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
            <el-table-column prop="f15" label="最高" width="100">
              <template #default="scope">
                {{ formatNumber(scope.row.f15) }}
              </template>
            </el-table-column>
            <el-table-column prop="f16" label="最低" width="100">
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
            <el-table-column prop="f23" label="市净率" width="100">
              <template #default="scope">
                {{ formatNumber(scope.row.f23) }}
              </template>
            </el-table-column>
            <el-table-column prop="f24" label="60日涨跌幅" width="120">
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
            <el-table-column prop="f62" label="主力净流入额(元)" width="140">
              <template #default="scope">
                <span :class="scope.row.f62 >= 0 ? 'rise' : 'fall'">
                  {{ formatAmount(scope.row.f62) }}
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="f115" label="市盈率TTM" width="120">
              <template #default="scope">
                {{ formatNumber(scope.row.f115) }}
              </template>
            </el-table-column>
            <el-table-column prop="f100" label="行业板块" width="120" />
            <el-table-column prop="f102" label="地区板块" width="120" />
            <el-table-column prop="f103" label="概念板块" width="300" show-overflow-tooltip />
          </el-table>
          
          <!-- 分页 -->
          <div class="pagination-container">
            <el-pagination
              v-model:current-page="currentPage"
              v-model:page-size="pageSize"
              :page-sizes="[50]"
              :small="false"
              layout="total, sizes, prev, pager, next, jumper"
              :total="total"
              @size-change="handleSizeChange"
              @current-change="handleCurrentChange"
            />
          </div>
        </div>
        
        <el-empty v-if="!loading && !error && stockPoolData.length === 0" description="暂无数据" />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import axios from 'axios'

const loading = ref(false)
const error = ref('')
const stockPoolData = ref<any[]>([])
const currentPage = ref(1)
const pageSize = ref(50)
const total = ref(0)

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
  return isNaN(num) ? '-' : (num / 100).toFixed(2) + '%'
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

// 获取股票池数据
const fetchStockPoolData = async (page: number = 1) => {
  loading.value = true
  error.value = ''
  
  try {
    const response = await axios.get(`/api/stock/pool?pn=${page}`)
    console.log('API返回原始数据:', response.data)
    
    // 解析后端返回的JSON
    const parsedData = response.data
    
    console.log('解析后的数据:', parsedData)
    
    if (parsedData && parsedData.data && parsedData.data.diff) {
      // 处理diff数据，确保是数组格式
      const diff = parsedData.data.diff
      if (Array.isArray(diff)) {
        stockPoolData.value = diff
      } else if (typeof diff === 'object') {
        // 如果diff是对象格式，转换为数组
        stockPoolData.value = Object.values(diff)
      } else {
        stockPoolData.value = []
      }
      
      // 设置总数据量（这里假设后端返回总数据量，暂时使用一个较大的值）
      total.value = 10000 // 实际应该从后端返回的total字段获取
    } else {
      error.value = '获取数据失败：数据格式不正确'
      stockPoolData.value = []
      total.value = 0
    }
  } catch (err) {
    console.error('获取股票池数据失败:', err)
    error.value = '获取数据失败：网络错误'
    stockPoolData.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

// 处理页码变化
const handleCurrentChange = (val: number) => {
  currentPage.value = val
  fetchStockPoolData(val)
}

// 处理每页条数变化
const handleSizeChange = (val: number) => {
  pageSize.value = val
  currentPage.value = 1
  fetchStockPoolData(1)
}

onMounted(() => {
  fetchStockPoolData()
})
</script>

<style scoped>
.stock-pool-container {
  padding: 20px;
  height: 100%;
  box-sizing: border-box;
  overflow: auto;
}

.data-card {
  height: 100%;
  overflow: hidden;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.loading-container {
  min-height: 400px;
  overflow-x: auto;
}

.error-alert {
  margin-bottom: 20px;
}

.table-container {
  width: 100%;
  overflow-x: auto;
}

.el-table {
  font-size: 12px;
  min-width: 100%;
}

.el-table th {
  background-color: #f5f7fa;
  font-weight: bold;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.rise {
  color: #f56c6c;
}

.fall {
  color: #67c23a;
}
</style>
