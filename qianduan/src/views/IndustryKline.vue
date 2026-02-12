<template>
  <div class="industry-kline-container">
    <el-card class="data-card">
      <template #header>
        <div class="card-header">
          <span>行业板块 K 线数据</span>
          <el-button size="small" type="default" @click="handleBack">
            返回
          </el-button>
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
        
        <template v-if="!error && industryInfo">
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
          
          <!-- 基础信息 -->
          <el-card class="info-card" :body-style="{ padding: '10px' }">
            <el-descriptions :column="6" border>
              <el-descriptions-item label="代码">{{ industryInfo.code }}</el-descriptions-item>
              <el-descriptions-item label="市场">{{ industryInfo.market }}</el-descriptions-item>
              <el-descriptions-item label="名称">{{ industryInfo.name }}</el-descriptions-item>
              <el-descriptions-item label="小数位">{{ industryInfo.decimal }}</el-descriptions-item>
              <el-descriptions-item label="总数">{{ industryInfo.dktotal }}</el-descriptions-item>
              <el-descriptions-item label="前值">{{ industryInfo.preKPrice }}</el-descriptions-item>
            </el-descriptions>
          </el-card>
          
          <!-- 图表区域 -->
          <div class="charts-container" style="overflow-y: auto; max-height: calc(100vh - 300px);">
            <el-row :gutter="20">
              <!-- 价格类图表 -->
              <el-col :span="24">
                <el-card class="chart-card">
                  <template #header>
                    <span>开盘价</span>
                  </template>
                  <div ref="openChartRef" class="chart"></div>
                </el-card>
              </el-col>
              
              <el-col :span="24">
                <el-card class="chart-card">
                  <template #header>
                    <span>收盘价</span>
                  </template>
                  <div ref="closeChartRef" class="chart"></div>
                </el-card>
              </el-col>
              
              <el-col :span="24">
                <el-card class="chart-card">
                  <template #header>
                    <span>最高价</span>
                  </template>
                  <div ref="highChartRef" class="chart"></div>
                </el-card>
              </el-col>
              
              <el-col :span="24">
                <el-card class="chart-card">
                  <template #header>
                    <span>最低价</span>
                  </template>
                  <div ref="lowChartRef" class="chart"></div>
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
        
        <el-empty v-if="!loading && !error && !industryInfo" description="暂无数据" />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import axios from 'axios'
import * as echarts from 'echarts'

const router = useRouter()
const route = useRoute()
const loading = ref(false)
const error = ref('')
const industryInfo = ref<any>(null)
const klineData = ref<any[]>([])
const originalKlineData = ref<any[]>([]) // 存储原始数据
const dateRange = ref<string[]>([undefined, undefined]) // 正确初始化数组

// 图表引用
const openChartRef = ref<HTMLElement>()
const closeChartRef = ref<HTMLElement>()
const highChartRef = ref<HTMLElement>()
const lowChartRef = ref<HTMLElement>()
const volumeChartRef = ref<HTMLElement>()
const amountChartRef = ref<HTMLElement>()
const amplitudeChartRef = ref<HTMLElement>()
const changePercentChartRef = ref<HTMLElement>()
const changeAmountChartRef = ref<HTMLElement>()
const turnoverChartRef = ref<HTMLElement>()

let openChart: echarts.ECharts | null = null
let closeChart: echarts.ECharts | null = null
let highChart: echarts.ECharts | null = null
let lowChart: echarts.ECharts | null = null
let volumeChart: echarts.ECharts | null = null
let amountChart: echarts.ECharts | null = null
let amplitudeChart: echarts.ECharts | null = null
let changePercentChart: echarts.ECharts | null = null
let changeAmountChart: echarts.ECharts | null = null
let turnoverChart: echarts.ECharts | null = null

// 处理返回按钮
const handleBack = () => {
  router.push('/industry-base')
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

// 获取行业代码
const getIndustryCode = (): string => {
  return route.query.industryCode as string || 'BK0486' // 默认文化传媒
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

// 获取行业板块K线数据
const fetchIndustryKlineData = async () => {
  const industryCode = getIndustryCode()
  loading.value = true
  error.value = ''
  
  try {
    const response = await axios.get(`http://localhost:8080/api/industry/kline?industryCode=${industryCode}`)
    console.log('API返回原始数据:', response.data)
    
    const parsedData = response.data
    
    console.log('解析后的数据:', parsedData)
    
    // 检查请求状态
    if (parsedData && parsedData.rc === 0 && parsedData.data) {
      const data = parsedData.data
      // 解析基础信息
      industryInfo.value = {
        code: data.code,
        market: data.market,
        name: data.name,
        decimal: data.decimal,
        dktotal: data.dktotal,
        preKPrice: data.preKPrice
      }
      // 解析K线数据
      if (data.klines && Array.isArray(data.klines)) {
        console.log('K线数据:', data.klines)
        klineData.value = parseKlineData(data.klines)
        console.log('解析后的K线数据:', klineData.value)
        // 检查解析后的数据结构
        if (klineData.value.length > 0) {
          console.log('解析后的数据示例:', klineData.value[0])
          console.log('数据字段检查 - time:', klineData.value[0].time)
          console.log('数据字段检查 - open:', klineData.value[0].open)
          console.log('数据字段检查 - close:', klineData.value[0].close)
          console.log('数据字段检查 - high:', klineData.value[0].high)
          console.log('数据字段检查 - low:', klineData.value[0].low)
          console.log('数据字段检查 - volume:', klineData.value[0].volume)
          console.log('数据字段检查 - amount:', klineData.value[0].amount)
          console.log('数据字段检查 - amplitude:', klineData.value[0].amplitude)
          console.log('数据字段检查 - changePercent:', klineData.value[0].changePercent)
          console.log('数据字段检查 - changeAmount:', klineData.value[0].changeAmount)
          console.log('数据字段检查 - turnover:', klineData.value[0].turnover)
        }
        // 存储原始数据
        originalKlineData.value = [...klineData.value]
        // 初始化图表
        console.log('准备初始化图表...')
        // 使用 nextTick 确保 DOM 更新后再初始化图表
        nextTick(() => {
          console.log('DOM 更新完成，开始初始化图表')
          initCharts()
        })
      } else {
        error.value = '获取数据失败：K线数据格式不正确'
        console.error('K线数据格式不正确:', data.klines)
      }
    } else {
      error.value = `获取数据失败：${parsedData?.rc || '未知错误'}`
      console.error('数据格式不正确:', parsedData)
    }
  } catch (err) {
    console.error('获取行业板块K线数据失败:', err)
    error.value = '获取数据失败：网络错误'
  } finally {
    loading.value = false
  }
}

// 初始化图表
const initCharts = () => {
  console.log('开始初始化图表，K线数据长度:', klineData.value.length)
  if (klineData.value.length === 0) {
    console.log('K线数据为空，跳过图表初始化')
    return
  }
  
  // 初始化各个图表
  console.log('开始初始化各个图表...')
  initOpenChart()
  initCloseChart()
  initHighChart()
  initLowChart()
  initVolumeChart()
  initAmountChart()
  initAmplitudeChart()
  initChangePercentChart()
  initChangeAmountChart()
  initTurnoverChart()
  console.log('所有图表初始化完成')
}

// 初始化单个折线图的通用函数
const initSingleChart = (chartRef: any, title: string, data: number[], color: string) => {
  console.log(`初始化${title}图表，数据长度:`, data.length)
  console.log(`${title}数据:`, data)
  
  if (!chartRef.value) {
    console.error(`${title}图表DOM元素不存在:`, chartRef)
    return null
  }
  
  console.log(`${title}图表DOM元素:`, chartRef.value)
  
  try {
    const chart = echarts.init(chartRef.value)
    console.log(`${title}图表实例创建成功:`, chart)
    
    const times = klineData.value.map(item => item.time)
    console.log(`${title}图表时间数据:`, times)
    
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
    
    console.log(`${title}图表配置:`, option)
    chart.setOption(option)
    console.log(`${title}图表配置设置成功`)
    return chart
  } catch (error) {
    console.error(`${title}图表初始化失败:`, error)
    return null
  }
}

// 初始化开盘价图表
const initOpenChart = () => {
  console.log('开始初始化开盘价图表')
  const openPrices = klineData.value.map(item => item.open)
  console.log('开盘价数据:', openPrices)
  openChart = initSingleChart(openChartRef, '开盘价', openPrices, '#409eff')
  console.log('开盘价图表初始化完成:', openChart)
}

// 初始化收盘价图表
const initCloseChart = () => {
  console.log('开始初始化收盘价图表')
  const closePrices = klineData.value.map(item => item.close)
  console.log('收盘价数据:', closePrices)
  closeChart = initSingleChart(closeChartRef, '收盘价', closePrices, '#f56c6c')
  console.log('收盘价图表初始化完成:', closeChart)
}

// 初始化最高价图表
const initHighChart = () => {
  console.log('开始初始化最高价图表')
  const highPrices = klineData.value.map(item => item.high)
  console.log('最高价数据:', highPrices)
  highChart = initSingleChart(highChartRef, '最高价', highPrices, '#67c23a')
  console.log('最高价图表初始化完成:', highChart)
}

// 初始化最低价图表
const initLowChart = () => {
  console.log('开始初始化最低价图表')
  const lowPrices = klineData.value.map(item => item.low)
  console.log('最低价数据:', lowPrices)
  lowChart = initSingleChart(lowChartRef, '最低价', lowPrices, '#e6a23c')
  console.log('最低价图表初始化完成:', lowChart)
}

// 初始化成交量图表
const initVolumeChart = () => {
  console.log('开始初始化成交量图表')
  const volumes = klineData.value.map(item => item.volume)
  console.log('成交量数据:', volumes)
  volumeChart = initSingleChart(volumeChartRef, '成交量', volumes, '#909399')
  console.log('成交量图表初始化完成:', volumeChart)
}

// 初始化成交额图表
const initAmountChart = () => {
  console.log('开始初始化成交额图表')
  const amounts = klineData.value.map(item => item.amount)
  console.log('成交额数据:', amounts)
  amountChart = initSingleChart(amountChartRef, '成交额', amounts, '#409eff')
  console.log('成交额图表初始化完成:', amountChart)
}

// 初始化振幅图表
const initAmplitudeChart = () => {
  console.log('开始初始化振幅图表')
  const amplitudes = klineData.value.map(item => item.amplitude)
  console.log('振幅数据:', amplitudes)
  amplitudeChart = initSingleChart(amplitudeChartRef, '振幅', amplitudes, '#f56c6c')
  console.log('振幅图表初始化完成:', amplitudeChart)
}

// 初始化涨跌幅图表
const initChangePercentChart = () => {
  console.log('开始初始化涨跌幅图表')
  const changePercents = klineData.value.map(item => item.changePercent)
  console.log('涨跌幅数据:', changePercents)
  changePercentChart = initSingleChart(changePercentChartRef, '涨跌幅', changePercents, '#67c23a')
  console.log('涨跌幅图表初始化完成:', changePercentChart)
}

// 初始化涨跌额图表
const initChangeAmountChart = () => {
  console.log('开始初始化涨跌额图表')
  const changeAmounts = klineData.value.map(item => item.changeAmount)
  console.log('涨跌额数据:', changeAmounts)
  changeAmountChart = initSingleChart(changeAmountChartRef, '涨跌额', changeAmounts, '#e6a23c')
  console.log('涨跌额图表初始化完成:', changeAmountChart)
}

// 初始化换手率图表
const initTurnoverChart = () => {
  console.log('开始初始化换手率图表')
  const turnovers = klineData.value.map(item => item.turnover)
  console.log('换手率数据:', turnovers)
  turnoverChart = initSingleChart(turnoverChartRef, '换手率', turnovers, '#909399')
  console.log('换手率图表初始化完成:', turnoverChart)
}

// 监听窗口大小变化，调整图表大小
const handleResize = () => {
  openChart?.resize()
  closeChart?.resize()
  highChart?.resize()
  lowChart?.resize()
  volumeChart?.resize()
  amountChart?.resize()
  amplitudeChart?.resize()
  changePercentChart?.resize()
  changeAmountChart?.resize()
  turnoverChart?.resize()
}

// 监听路由变化，重新获取数据
watch(
  () => route.query.industryCode,
  () => {
    fetchIndustryKlineData()
  }
)

onMounted(() => {
  fetchIndustryKlineData()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  openChart?.dispose()
  closeChart?.dispose()
  highChart?.dispose()
  lowChart?.dispose()
  volumeChart?.dispose()
  amountChart?.dispose()
  amplitudeChart?.dispose()
  changePercentChart?.dispose()
  changeAmountChart?.dispose()
  turnoverChart?.dispose()
})
</script>

<style scoped>
.industry-kline-container {
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
  min-height: 600px;
}

.error-alert {
  margin-bottom: 20px;
}

.time-filter-form {
  margin-bottom: 20px;
}

.info-card {
  margin-bottom: 20px;
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
