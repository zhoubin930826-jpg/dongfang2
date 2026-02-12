<template>
  <div class="stock-real-container">
    <el-card class="data-card">
      <template #header>
        <div class="card-header">
          <span>实盘委托买卖数据</span>
          <el-button size="small" type="default" @click="handleBack">
            返回
          </el-button>
        </div>
      </template>
      
      <!-- 股票代码输入 -->
      <el-form :inline="true" class="stock-code-form" @submit.prevent="fetchStockData">
        <el-form-item label="股票代码">
          <el-input
            v-model="stockCode"
            placeholder="请输入股票代码，例如：600960"
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
          <!-- 股票数据表格 -->
          <el-table :data="stockDataList" style="width: 100%" border>
            <el-table-column label="参数" width="120" prop="参数" />
            <el-table-column label="值" prop="值" />
          </el-table>
          
          <!-- 时间筛选 -->
          <el-form :inline="true" class="time-filter-form" @submit.prevent="handleTimeFilter">
            <el-form-item label="开始时间">
              <el-date-picker
                v-model="dateRange[0]"
                type="date"
                placeholder="选择开始日期"
                format="YYYY年MM月DD日"
                value-format="YYYY-MM-DD"
                @change="handleTimeFilter"
              />
            </el-form-item>
            <el-form-item label="结束时间">
              <el-date-picker
                v-model="dateRange[1]"
                type="date"
                placeholder="选择结束日期"
                format="YYYY年MM月DD日"
                value-format="YYYY-MM-DD"
                @change="handleTimeFilter"
              />
            </el-form-item>
          </el-form>
          
          <!-- 图表区域 -->
          <div class="charts-container">
            <el-row :gutter="20">
              <!-- 价格类图表 -->
              <el-col :span="24">
                <el-card class="chart-card">
                  <template #header>
                    <span>价格走势</span>
                  </template>
                  <div ref="priceChartRef" class="chart"></div>
                </el-card>
              </el-col>
              
              <!-- 成交量与成交额 -->
              <el-col :span="24">
                <el-card class="chart-card">
                  <template #header>
                    <span>成交量</span>
                  </template>
                  <div ref="volumeChartRef" class="chart"></div>
                </el-card>
              </el-col>
              
              <el-col :span="24">
                <el-card class="chart-card">
                  <template #header>
                    <span>成交额</span>
                  </template>
                  <div ref="amountChartRef" class="chart"></div>
                </el-card>
              </el-col>
              
              <!-- 技术指标 -->
              <el-col :span="24">
                <el-card class="chart-card">
                  <template #header>
                    <span>振幅</span>
                  </template>
                  <div ref="amplitudeChartRef" class="chart"></div>
                </el-card>
              </el-col>
              
              <el-col :span="24">
                <el-card class="chart-card">
                  <template #header>
                    <span>涨跌幅</span>
                  </template>
                  <div ref="changePercentChartRef" class="chart"></div>
                </el-card>
              </el-col>
              
              <el-col :span="24">
                <el-card class="chart-card">
                  <template #header>
                    <span>涨跌额</span>
                  </template>
                  <div ref="changeAmountChartRef" class="chart"></div>
                </el-card>
              </el-col>
              
              <el-col :span="24">
                <el-card class="chart-card">
                  <template #header>
                    <span>换手率</span>
                  </template>
                  <div ref="turnoverChartRef" class="chart"></div>
                </el-card>
              </el-col>
            </el-row>
          </div>
        </template>
        
        <el-empty v-if="!loading && !error && !stockInfo" description="请输入股票代码查询数据" />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount, nextTick, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import axios from 'axios'
import * as echarts from 'echarts'

const router = useRouter()
const route = useRoute()
const stockCode = ref('600960') // 默认东方财富
const loading = ref(false)
const error = ref('')
const stockInfo = ref<any>(null)
const klineData = ref<any[]>([])
const originalKlineData = ref<any[]>([])
const dateRange = ref<string[]>([undefined, undefined]) // 正确初始化数组

// 处理返回按钮
const handleBack = () => {
  router.push('/stock-pool')
}

// 监听路由参数变化，获取股票代码
watch(
  () => route.query.stockCode,
  (newStockCode) => {
    if (newStockCode) {
      stockCode.value = newStockCode as string
      fetchStockData()
    }
  },
  { immediate: true }
)

// 图表引用
const priceChartRef = ref<HTMLElement>()
const volumeChartRef = ref<HTMLElement>()
const amountChartRef = ref<HTMLElement>()
const amplitudeChartRef = ref<HTMLElement>()
const changePercentChartRef = ref<HTMLElement>()
const changeAmountChartRef = ref<HTMLElement>()
const turnoverChartRef = ref<HTMLElement>()

let priceChart: echarts.ECharts | null = null
let volumeChart: echarts.ECharts | null = null
let amountChart: echarts.ECharts | null = null
let amplitudeChart: echarts.ECharts | null = null
let changePercentChart: echarts.ECharts | null = null
let changeAmountChart: echarts.ECharts | null = null
let turnoverChart: echarts.ECharts | null = null

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

// 处理时间筛选
const handleTimeFilter = () => {
  const [startDate, endDate] = dateRange.value
  
  // 过滤数据
  if (startDate || endDate) {
    klineData.value = originalKlineData.value.filter(item => {
      const itemDate = item.time
      let matchStart = true
      let matchEnd = true
      
      if (startDate) {
        matchStart = itemDate >= startDate
      }
      if (endDate) {
        matchEnd = itemDate <= endDate
      }
      
      return matchStart && matchEnd
    })
  } else {
    // 如果都没选，使用原始数据
    klineData.value = [...originalKlineData.value]
  }
  
  // 重新初始化图表
  nextTick(() => {
    initCharts()
  })
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
    const response = await axios.get(`/api/stock/real?stockCode=${code}`)
    console.log('API返回原始数据:', response.data)
    
    // 解析后端返回的JSON
    const parsedData = response.data
    
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
    const response = await axios.get(`/api/stock/kline?stockCode=${code}`)
    console.log('API返回原始数据:', response.data)
    
    // 解析后端返回的JSON
    const parsedData = response.data
    
    console.log('解析后的数据:', parsedData)
    
    if (parsedData && parsedData.rc === 0 && parsedData.data && parsedData.data.klines) {
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
  if (!stockCode.value) {
    error.value = '请输入有效的股票代码'
    return
  }
  
  loading.value = true
  error.value = ''
  
  try {
    // 并行请求实时数据和K线数据
    const [realData, klineResult] = await Promise.all([
      fetchStockRealData(stockCode.value),
      fetchStockKlineData(stockCode.value)
    ])
    
    stockInfo.value = realData
    klineData.value = klineResult
    originalKlineData.value = [...klineResult]
    
    // 设置默认时间范围（近2个月）
    if (originalKlineData.value.length > 0) {
      // 找出最大时间
      const maxTimeStr = originalKlineData.value.reduce((max, item) => {
        return item.time > max ? item.time : max
      }, originalKlineData.value[0].time)
      // 计算两个月前的时间
      const endDate = new Date(maxTimeStr)
      const startDate = new Date(endDate)
      startDate.setMonth(startDate.getMonth() - 2)
      // 格式化日期为 YYYY-MM-DD
      const formatDate = (date: Date): string => {
        const year = date.getFullYear()
        const month = String(date.getMonth() + 1).padStart(2, '0')
        const day = String(date.getDate()).padStart(2, '0')
        return `${year}-${month}-${day}`
      }
      dateRange.value = [formatDate(startDate), formatDate(endDate)]
      // 应用时间筛选
      handleTimeFilter()
    }
    
    // 初始化图表
    if (klineData.value.length > 0) {
      nextTick(() => {
        initCharts()
      })
    }
  } catch (err: any) {
    error.value = err.message || '获取数据失败：网络错误'
    stockInfo.value = null
    klineData.value = []
  } finally {
    loading.value = false
  }
}

// 股票数据表格的数据源
const stockDataList = computed(() => {
  if (!stockInfo.value) return []
  
  return [
    { 参数: '委差', 值: stockInfo.value.f192 },
    { 参数: '委比', 值: formatPercent(stockInfo.value.f191 / 100) },
    { 参数: '卖5', 值: formatNumber(stockInfo.value.f32) },
    { 参数: '卖4', 值: formatNumber(stockInfo.value.f34) },
    { 参数: '卖3', 值: formatNumber(stockInfo.value.f36) },
    { 参数: '卖2', 值: formatNumber(stockInfo.value.f38) },
    { 参数: '卖1', 值: formatNumber(stockInfo.value.f40) },
    { 参数: '买1', 值: formatNumber(stockInfo.value.f20) },
    { 参数: '买2', 值: formatNumber(stockInfo.value.f18) },
    { 参数: '买3', 值: formatNumber(stockInfo.value.f16) },
    { 参数: '买4', 值: formatNumber(stockInfo.value.f14) },
    { 参数: '买5', 值: formatNumber(stockInfo.value.f12) },
    { 参数: '内盘', 值: formatVolume(stockInfo.value.f161) },
    { 参数: '外盘', 值: formatVolume(stockInfo.value.f49) },
    { 参数: '成交额', 值: formatAmount(stockInfo.value.f48) },
    { 参数: '换手率', 值: formatPercent(stockInfo.value.f168 / 100) },
    { 参数: '量比', 值: formatNumber(stockInfo.value.f50) },
    { 参数: '均价', 值: formatNumber(stockInfo.value.f71 / 100) },
    { 参数: '股票代码', 值: stockInfo.value.f57 },
    { 参数: '股票名称', 值: stockInfo.value.f58 },
    { 参数: '所属板块', 值: stockInfo.value.f128 }
  ]
})

// 初始化图表
const initCharts = () => {
  if (klineData.value.length === 0) return
  
  initPriceChart()
  initVolumeChart()
  initAmountChart()
  initAmplitudeChart()
  initChangePercentChart()
  initChangeAmountChart()
  initTurnoverChart()
}

// 初始化单个折线图的通用函数
const initSingleChart = (chartRef: any, title: string, data: number[], color: string) => {
  if (!chartRef.value) return null
  
  try {
    const chart = echarts.init(chartRef.value)
    
    const times = klineData.value.map(item => item.time)
    
    const option = {
      tooltip: {
        trigger: 'axis',
        axisPointer: {
          type: 'cross'
        }
      },
      legend: {
        data: [title],
        top: 0
      },
      grid: {
        left: '3%',
        right: '4%',
        bottom: '3%',
        containLabel: true
      },
      xAxis: {
        type: 'category',
        boundaryGap: false,
        data: times,
        axisLabel: {
          rotate: 45
        }
      },
      yAxis: {
        type: 'value'
      },
      series: [
        {
          name: title,
          type: 'line',
          data: data,
          smooth: true,
          lineStyle: {
            width: 2,
            color: color
          }
        }
      ]
    }
    
    chart.setOption(option)
    return chart
  } catch (error) {
    console.error(`${title}图表初始化失败:`, error)
    return null
  }
}

// 初始化价格走势图表（包含开盘价、收盘价、最高价、最低价）
const initPriceChart = () => {
  console.log('开始初始化价格走势图表')
  if (!priceChartRef.value) {
    console.error('价格走势图表DOM元素不存在')
    return
  }
  
  try {
    const chart = echarts.init(priceChartRef.value)
    console.log('价格走势图表实例创建成功:', chart)
    
    const times = klineData.value.map(item => item.time)
    const openPrices = klineData.value.map(item => item.open)
    const closePrices = klineData.value.map(item => item.close)
    const highPrices = klineData.value.map(item => item.high)
    const lowPrices = klineData.value.map(item => item.low)
    
    console.log('价格数据准备完成')
    console.log('开盘价数据:', openPrices)
    console.log('收盘价数据:', closePrices)
    console.log('最高价数据:', highPrices)
    console.log('最低价数据:', lowPrices)
    
    const option = {
      tooltip: {
        trigger: 'axis',
        axisPointer: {
          type: 'cross'
        }
      },
      legend: {
        data: ['开盘价', '收盘价', '最高价', '最低价'],
        top: 0
      },
      grid: {
        left: '3%',
        right: '4%',
        bottom: '3%',
        top: '15%',
        containLabel: true
      },
      xAxis: {
        type: 'category',
        boundaryGap: false,
        data: times,
        axisLabel: {
          rotate: 45
        }
      },
      yAxis: {
        type: 'value'
      },
      series: [
        {
          name: '开盘价',
          type: 'line',
          data: openPrices,
          smooth: true,
          lineStyle: {
            width: 2,
            color: '#409eff'
          }
        },
        {
          name: '收盘价',
          type: 'line',
          data: closePrices,
          smooth: true,
          lineStyle: {
            width: 2,
            color: '#f56c6c'
          }
        },
        {
          name: '最高价',
          type: 'line',
          data: highPrices,
          smooth: true,
          lineStyle: {
            width: 2,
            color: '#67c23a'
          }
        },
        {
          name: '最低价',
          type: 'line',
          data: lowPrices,
          smooth: true,
          lineStyle: {
            width: 2,
            color: '#e6a23c'
          }
        }
      ]
    }
    
    console.log('价格走势图表配置:', option)
    chart.setOption(option)
    console.log('价格走势图表配置设置成功')
    
    // 立即调用resize，确保图表适应容器大小
    chart.resize()
    
    priceChart = chart
  } catch (error) {
    console.error('价格走势图表初始化失败:', error)
  }
}

// 初始化成交量图表
const initVolumeChart = () => {
  const volumes = klineData.value.map(item => item.volume)
  volumeChart = initSingleChart(volumeChartRef, '成交量', volumes, '#909399')
}

// 初始化成交额图表
const initAmountChart = () => {
  const amounts = klineData.value.map(item => item.amount)
  amountChart = initSingleChart(amountChartRef, '成交额', amounts, '#409eff')
}

// 初始化振幅图表
const initAmplitudeChart = () => {
  const amplitudes = klineData.value.map(item => item.amplitude)
  amplitudeChart = initSingleChart(amplitudeChartRef, '振幅', amplitudes, '#f56c6c')
}

// 初始化涨跌幅图表
const initChangePercentChart = () => {
  const changePercents = klineData.value.map(item => item.changePercent)
  changePercentChart = initSingleChart(changePercentChartRef, '涨跌幅', changePercents, '#67c23a')
}

// 初始化涨跌额图表
const initChangeAmountChart = () => {
  const changeAmounts = klineData.value.map(item => item.changeAmount)
  changeAmountChart = initSingleChart(changeAmountChartRef, '涨跌额', changeAmounts, '#e6a23c')
}

// 初始化换手率图表
const initTurnoverChart = () => {
  const turnovers = klineData.value.map(item => item.turnover)
  turnoverChart = initSingleChart(turnoverChartRef, '换手率', turnovers, '#909399')
}

// 监听窗口大小变化，调整图表大小
const handleResize = () => {
  priceChart?.resize()
  volumeChart?.resize()
  amountChart?.resize()
  amplitudeChart?.resize()
  changePercentChart?.resize()
  changeAmountChart?.resize()
  turnoverChart?.resize()
}

onMounted(() => {
  // 初始加载默认股票数据
  fetchStockData()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  priceChart?.dispose()
  volumeChart?.dispose()
  amountChart?.dispose()
  amplitudeChart?.dispose()
  changePercentChart?.dispose()
  changeAmountChart?.dispose()
  turnoverChart?.dispose()
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

.time-filter-form {
  margin-bottom: 20px;
}

.loading-container {
  max-height: calc(100vh - 200px);
  overflow-y: auto;
}

.error-alert {
  margin-bottom: 20px;
}

.el-table {
  font-size: 14px;
  width: 80% !important;
  margin: 0 auto 20px;
}

.el-table th {
  background-color: #f5f7fa;
  font-weight: bold;
}

.el-table td:first-child {
  font-weight: 500;
}

.charts-container {
  margin-top: 20px;
}

.chart-card {
  margin-bottom: 20px;
}

.chart {
  width: 100%;
  height: 400px;
}
</style>
