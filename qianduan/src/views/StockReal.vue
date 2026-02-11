<template>
  <div class="stock-real-container">
    <el-card class="data-card">
      <template #header>
        <div class="card-header">
          <span>实盘委托买卖数据</span>
        </div>
      </template>
      
      <!-- 股票代码输入 -->
      <el-form :inline="true" class="stock-code-form" @submit.prevent="fetchStockData">
        <el-form-item label="股票代码">
          <el-input
            v-model="stockCode"
            placeholder="请输入股票代码，例如：300059"
            maxlength="6"
            show-word-limit
            @keyup.enter="fetchStockData"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchStockData" :loading="loading">
            确认
          </el-button>
        </el-form-item>
      </el-form>
      
      <div v-loading="loading" element-loading-text="加载中..." class="loading-container">
        <el-alert
          v-if="error"
          :title="error"
          type="error"
          show-icon
          :closable="false"
          class="error-alert"
        />
        
        <template v-if="!error && stockInfo">
          <!-- 股票基本信息 -->
          <el-card class="info-card" :body-style="{ padding: '10px' }">
            <el-descriptions :column="4" border>
              <el-descriptions-item label="股票代码">{{ stockInfo.f57 }}</el-descriptions-item>
              <el-descriptions-item label="股票名称">{{ stockInfo.f58 }}</el-descriptions-item>
              <el-descriptions-item label="所属板块">{{ stockInfo.f128 }}</el-descriptions-item>
              <el-descriptions-item label="均价">{{ formatNumber(stockInfo.f71) }}</el-descriptions-item>
              <el-descriptions-item label="委差">{{ stockInfo.f192 }}</el-descriptions-item>
              <el-descriptions-item label="委比">{{ formatPercent(stockInfo.f191) }}</el-descriptions-item>
              <el-descriptions-item label="内盘">{{ formatVolume(stockInfo.f161) }}</el-descriptions-item>
              <el-descriptions-item label="外盘">{{ formatVolume(stockInfo.f49) }}</el-descriptions-item>
              <el-descriptions-item label="成交额">{{ formatAmount(stockInfo.f48) }}</el-descriptions-item>
              <el-descriptions-item label="换手率">{{ formatPercent(stockInfo.f168) }}</el-descriptions-item>
              <el-descriptions-item label="量比">{{ formatNumber(stockInfo.f50) }}</el-descriptions-item>
              <el-descriptions-item label="总市值">{{ formatAmount(stockInfo.f116) }}</el-descriptions-item>
            </el-descriptions>
          </el-card>
          
          <!-- 买卖盘口 -->
          <el-card class="order-card" :body-style="{ padding: '10px' }">
            <template #header>
              <span>买卖盘口</span>
            </template>
            <div class="order-container">
              <div class="order-side">
                <h4>卖盘</h4>
                <el-table :data="sellOrders" style="width: 100%" size="small" border>
                  <el-table-column label="卖5" width="100">
                    <template #default="scope">{{ formatNumber(scope.row.price) }}</template>
                  </el-table-column>
                  <el-table-column label="卖4" width="100">
                    <template #default="scope">{{ formatNumber(scope.row.price) }}</template>
                  </el-table-column>
                  <el-table-column label="卖3" width="100">
                    <template #default="scope">{{ formatNumber(scope.row.price) }}</template>
                  </el-table-column>
                  <el-table-column label="卖2" width="100">
                    <template #default="scope">{{ formatNumber(scope.row.price) }}</template>
                  </el-table-column>
                  <el-table-column label="卖1" width="100">
                    <template #default="scope">{{ formatNumber(scope.row.price) }}</template>
                  </el-table-column>
                </el-table>
                <el-table :data="sellOrdersVolume" style="width: 100%" size="small" border>
                  <el-table-column label="数量" width="100">
                    <template #default="scope">{{ formatVolume(scope.row.volume) }}</template>
                  </el-table-column>
                  <el-table-column label="数量" width="100">
                    <template #default="scope">{{ formatVolume(scope.row.volume) }}</template>
                  </el-table-column>
                  <el-table-column label="数量" width="100">
                    <template #default="scope">{{ formatVolume(scope.row.volume) }}</template>
                  </el-table-column>
                  <el-table-column label="数量" width="100">
                    <template #default="scope">{{ formatVolume(scope.row.volume) }}</template>
                  </el-table-column>
                  <el-table-column label="数量" width="100">
                    <template #default="scope">{{ formatVolume(scope.row.volume) }}</template>
                  </el-table-column>
                </el-table>
              </div>
              
              <div class="order-side">
                <h4>买盘</h4>
                <el-table :data="buyOrders" style="width: 100%" size="small" border>
                  <el-table-column label="买1" width="100">
                    <template #default="scope">{{ formatNumber(scope.row.price) }}</template>
                  </el-table-column>
                  <el-table-column label="买2" width="100">
                    <template #default="scope">{{ formatNumber(scope.row.price) }}</template>
                  </el-table-column>
                  <el-table-column label="买3" width="100">
                    <template #default="scope">{{ formatNumber(scope.row.price) }}</template>
                  </el-table-column>
                  <el-table-column label="买4" width="100">
                    <template #default="scope">{{ formatNumber(scope.row.price) }}</template>
                  </el-table-column>
                  <el-table-column label="买5" width="100">
                    <template #default="scope">{{ formatNumber(scope.row.price) }}</template>
                  </el-table-column>
                </el-table>
                <el-table :data="buyOrdersVolume" style="width: 100%" size="small" border>
                  <el-table-column label="数量" width="100">
                    <template #default="scope">{{ formatVolume(scope.row.volume) }}</template>
                  </el-table-column>
                  <el-table-column label="数量" width="100">
                    <template #default="scope">{{ formatVolume(scope.row.volume) }}</template>
                  </el-table-column>
                  <el-table-column label="数量" width="100">
                    <template #default="scope">{{ formatVolume(scope.row.volume) }}</template>
                  </el-table-column>
                  <el-table-column label="数量" width="100">
                    <template #default="scope">{{ formatVolume(scope.row.volume) }}</template>
                  </el-table-column>
                  <el-table-column label="数量" width="100">
                    <template #default="scope">{{ formatVolume(scope.row.volume) }}</template>
                  </el-table-column>
                </el-table>
              </div>
            </div>
          </el-card>
          
          <!-- K线图表 -->
          <el-card class="chart-card" v-if="klineData.length > 0">
            <template #header>
              <span>K线数据</span>
            </template>
            <div ref="klineChartRef" class="chart"></div>
          </el-card>
        </template>
        
        <el-empty v-if="!loading && !error && !stockInfo" description="请输入股票代码查询数据" />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue'
import axios from 'axios'
import * as echarts from 'echarts'

const stockCode = ref('300059') // 默认东方财富
const loading = ref(false)
const error = ref('')
const stockInfo = ref<any>(null)
const klineData = ref<any[]>([])

// 图表引用
const klineChartRef = ref<HTMLElement>()
let klineChart: echarts.ECharts | null = null

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

// 解析K线数据
const parseKlineData = (klines: string[]): any[] => {
  return klines.map(line => {
    const [f51, f52, f53, f54, f55, f56, f57, f58, f59, f60, f61] = line.split(',')
    return {
      time: f51,
      open: parseFloat(f52),
      close: parseFloat(f53),
      high: parseFloat(f54),
      low: parseFloat(f55),
      volume: parseFloat(f56),
      amount: parseFloat(f57),
      amplitude: parseFloat(f58),
      changePercent: parseFloat(f59),
      changeAmount: parseFloat(f60),
      turnover: parseFloat(f61)
    }
  })
}

// 获取股票实时数据
const fetchStockRealData = async (code: string): Promise<any> => {
  try {
    const response = await axios.get(`http://localhost:8080/api/stock/real?stockCode=${code}`)
    console.log('API返回原始数据:', response.data)
    
    // 解析后端返回的JSON字符串
    let parsedData
    if (typeof response.data === 'string') {
      parsedData = JSON.parse(response.data)
    } else {
      parsedData = response.data
    }
    
    console.log('解析后的数据:', parsedData)
    
    if (parsedData && parsedData.data) {
      return parsedData.data
    }
    throw new Error('获取股票数据失败：数据格式不正确')
  } catch (err) {
    console.error('获取股票实时数据失败:', err)
    throw err
  }
}

// 获取股票K线数据
const fetchStockKlineData = async (code: string): Promise<any[]> => {
  try {
    const response = await axios.get(`http://localhost:8080/api/stock/kline?stockCode=${code}`)
    console.log('API返回原始数据:', response.data)
    
    // 解析后端返回的JSON字符串
    let parsedData
    if (typeof response.data === 'string') {
      parsedData = JSON.parse(response.data)
    } else {
      parsedData = response.data
    }
    
    console.log('解析后的数据:', parsedData)
    
    if (parsedData && parsedData.data && parsedData.data.klines) {
      return parseKlineData(parsedData.data.klines)
    }
    return []
  } catch (err) {
    console.error('获取股票K线数据失败:', err)
    return []
  }
}

// 获取股票数据
const fetchStockData = async () => {
  if (!stockCode.value || stockCode.value.length !== 6) {
    error.value = '请输入有效的6位股票代码'
    return
  }
  
  loading.value = true
  error.value = ''
  
  try {
    // 并行请求股票实时数据和K线数据
    const [realData, klineResult] = await Promise.all([
      fetchStockRealData(stockCode.value),
      fetchStockKlineData(stockCode.value)
    ])
    
    stockInfo.value = realData
    klineData.value = klineResult
    
    // 初始化K线图表
    if (klineData.value.length > 0) {
      initKlineChart()
    }
  } catch (err: any) {
    error.value = err.message || '获取数据失败：网络错误'
    stockInfo.value = null
    klineData.value = []
  } finally {
    loading.value = false
  }
}

// 初始化K线图表
const initKlineChart = () => {
  if (!klineChartRef.value || klineData.value.length === 0) return
  
  klineChart = echarts.init(klineChartRef.value)
  
  const times = klineData.value.map(item => item.time)
  const closePrices = klineData.value.map(item => item.close)
  const volumes = klineData.value.map(item => item.volume)
  
  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'cross'
      }
    },
    legend: {
      data: ['收盘价', '成交量'],
      top: 0
    },
    grid: [
      {
        left: '3%',
        right: '4%',
        top: '15%',
        height: '50%',
        containLabel: true
      },
      {
        left: '3%',
        right: '4%',
        top: '70%',
        height: '20%',
        containLabel: true
      }
    ],
    xAxis: [
      {
        type: 'category',
        boundaryGap: false,
        data: times,
        axisLabel: {
          rotate: 45
        },
        axisLine: {
          lineStyle: {
            color: '#999'
          }
        }
      },
      {
        type: 'category',
        boundaryGap: false,
        data: times,
        gridIndex: 1,
        axisLabel: {
          rotate: 45
        },
        axisLine: {
          lineStyle: {
            color: '#999'
          }
        }
      }
    ],
    yAxis: [
      {
        type: 'value',
        name: '价格',
        position: 'left',
        axisLine: {
          lineStyle: {
            color: '#409eff'
          }
        }
      },
      {
        type: 'value',
        name: '成交量',
        position: 'right',
        gridIndex: 1,
        axisLine: {
          lineStyle: {
            color: '#f56c6c'
          }
        }
      }
    ],
    series: [
      {
        name: '收盘价',
        type: 'line',
        data: closePrices,
        smooth: true,
        lineStyle: {
          width: 2,
          color: '#409eff'
        }
      },
      {
        name: '成交量',
        type: 'bar',
        xAxisIndex: 1,
        yAxisIndex: 1,
        data: volumes,
        itemStyle: {
          color: '#f56c6c'
        }
      }
    ]
  }
  
  klineChart.setOption(option)
}

// 监听窗口大小变化，调整图表大小
const handleResize = () => {
  klineChart?.resize()
}

// 计算买卖盘口数据
const sellOrders = ref([
  { price: 0 }
])
const sellOrdersVolume = ref([
  { volume: 0 }
])
const buyOrders = ref([
  { price: 0 }
])
const buyOrdersVolume = ref([
  { volume: 0 }
])

// 监听stockInfo变化，更新买卖盘口数据
const updateOrderData = () => {
  if (!stockInfo.value) return
  
  // 卖盘
  sellOrders.value = [
    { price: stockInfo.value.f32 }, // 卖5
    { price: stockInfo.value.f34 }, // 卖4
    { price: stockInfo.value.f36 }, // 卖3
    { price: stockInfo.value.f38 }, // 卖2
    { price: stockInfo.value.f40 }  // 卖1
  ]
  
  sellOrdersVolume.value = [
    { volume: stockInfo.value.f31 }, // 卖5
    { volume: stockInfo.value.f33 }, // 卖4
    { volume: stockInfo.value.f35 }, // 卖3
    { volume: stockInfo.value.f37 }, // 卖2
    { volume: stockInfo.value.f39 }  // 卖1
  ]
  
  // 买盘
  buyOrders.value = [
    { price: stockInfo.value.f20 }, // 买1
    { price: stockInfo.value.f18 }, // 买2
    { price: stockInfo.value.f16 }, // 买3
    { price: stockInfo.value.f14 }, // 买4
    { price: stockInfo.value.f12 }  // 买5
  ]
  
  buyOrdersVolume.value = [
    { volume: stockInfo.value.f19 }, // 买1
    { volume: stockInfo.value.f17 }, // 买2
    { volume: stockInfo.value.f15 }, // 买3
    { volume: stockInfo.value.f13 }, // 买4
    { volume: stockInfo.value.f11 }  // 买5
  ]
}

// 监听stockInfo变化
const { watch } = require('vue')
watch(stockInfo, () => {
  updateOrderData()
}, { deep: true })

onMounted(() => {
  // 初始加载默认股票数据
  fetchStockData()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  klineChart?.dispose()
})
</script>

<style scoped>
.stock-real-container {
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

.stock-code-form {
  margin-bottom: 20px;
}

.loading-container {
  min-height: 400px;
}

.error-alert {
  margin-bottom: 20px;
}

.info-card {
  margin-bottom: 20px;
}

.order-card {
  margin-bottom: 20px;
}

.order-container {
  display: flex;
  justify-content: space-between;
  gap: 20px;
}

.order-side {
  flex: 1;
}

.order-side h4 {
  margin-top: 0;
  margin-bottom: 10px;
  text-align: center;
}

.chart-card {
  margin-top: 20px;
}

.chart {
  width: 100%;
  height: 400px;
}

.el-table {
  font-size: 12px;
  margin-bottom: 10px;
}

.el-table th {
  background-color: #f5f7fa;
  font-weight: bold;
}
</style>
