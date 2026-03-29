<template>
  <div class="page-shell stock-real-container">
    <section class="page-hero">
      <div>
        <p class="page-kicker">实时盘口</p>
        <h2 class="page-title">{{ stockInfo?.f58 || '个股实盘看板' }}</h2>
        <p class="page-subtitle">
          {{ stockInfo ? `${stockInfo.f57} / ${stockInfo.f128 || '未识别板块'}` : '输入股票代码，先看盘口，再看 K 线与成交结构。' }}
        </p>
      </div>

      <div class="page-actions page-actions--stack">
        <div class="search-bar">
          <el-input
            v-model="stockCodeInput"
            placeholder="输入股票代码，例如 600960"
            clearable
            @keyup.enter="handleSearch"
          />
          <el-button type="primary" @click="handleSearch">查询</el-button>
        </div>

        <div class="hero-badge-row">
          <div class="hero-badge">
            <span class="hero-badge-label">股票代码</span>
            <strong>{{ stockInfo?.f57 || stockCodeInput || '--' }}</strong>
          </div>
          <div class="hero-badge">
            <span class="hero-badge-label">所属板块</span>
            <strong>{{ stockInfo?.f128 || '等待查询' }}</strong>
          </div>
          <div class="hero-badge">
            <span class="hero-badge-label">盘口抓取时间</span>
            <strong>{{ quoteFetchedAt }}</strong>
          </div>
          <div class="hero-badge">
            <span class="hero-badge-label">K线抓取时间</span>
            <strong>{{ klineFetchedAt }}</strong>
          </div>
        </div>

        <el-button type="default" @click="handleBack">返回股票池</el-button>
      </div>
    </section>

    <section v-if="!error && stockInfo" class="metric-grid">
      <article
        v-for="item in quoteSummary"
        :key="item.label"
        class="metric-card"
      >
        <p class="metric-label">{{ item.label }}</p>
        <p :class="['metric-value', item.tone]">{{ item.value }}</p>
        <p class="metric-note">{{ item.note }}</p>
      </article>
    </section>

    <el-card class="view-card data-card">
      <template #header>
        <div class="section-header">
          <div>
            <p class="section-kicker">盘口工作区</p>
            <h3 class="section-title">盘口与趋势联动</h3>
          </div>
          <p class="section-note">盘口先展示，K 线和成交结构随后补齐。</p>
        </div>
      </template>

      <div v-loading="loading" element-loading-text="加载盘口中..." class="loading-container">
        <el-alert
          v-if="error"
          :title="error"
          type="error"
          show-icon
          :closable="false"
          class="error-alert"
        />

        <template v-if="!error && stockInfo">
          <el-card class="market-subcard snapshot-card">
            <template #header>
              <div class="chart-card-header">
                <span>盘口速览</span>
                <small>盘口快照</small>
              </div>
            </template>
            <el-table :data="stockDataList" border class="market-table snapshot-table">
              <el-table-column label="参数" width="150" prop="label" />
              <el-table-column label="值" prop="value" />
            </el-table>
          </el-card>

          <div class="filter-bar market-subcard">
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
          </div>

          <div
            v-loading="klineLoading"
            element-loading-text="加载 K 线中..."
            class="charts-container"
          >
            <el-row :gutter="18">
              <el-col :span="24">
                <el-card class="chart-card view-card">
                  <template #header>
                    <div class="chart-card-header">
                      <span>价格走势</span>
                      <small>开盘 / 收盘 / 最高 / 最低</small>
                    </div>
                  </template>
                  <div ref="priceChartRef" class="chart"></div>
                </el-card>
              </el-col>

              <el-col :xs="24" :xl="12">
                <el-card class="chart-card view-card">
                  <template #header>
                    <div class="chart-card-header">
                      <span>成交量</span>
                      <small>成交量</small>
                    </div>
                  </template>
                  <div ref="volumeChartRef" class="chart"></div>
                </el-card>
              </el-col>

              <el-col :xs="24" :xl="12">
                <el-card class="chart-card view-card">
                  <template #header>
                    <div class="chart-card-header">
                      <span>成交额</span>
                      <small>成交额</small>
                    </div>
                  </template>
                  <div ref="amountChartRef" class="chart"></div>
                </el-card>
              </el-col>

              <el-col :xs="24" :xl="12">
                <el-card class="chart-card view-card">
                  <template #header>
                    <div class="chart-card-header">
                      <span>振幅</span>
                      <small>波动率</small>
                    </div>
                  </template>
                  <div ref="amplitudeChartRef" class="chart"></div>
                </el-card>
              </el-col>

              <el-col :xs="24" :xl="12">
                <el-card class="chart-card view-card">
                  <template #header>
                    <div class="chart-card-header">
                      <span>涨跌幅</span>
                      <small>涨跌幅</small>
                    </div>
                  </template>
                  <div ref="changePercentChartRef" class="chart"></div>
                </el-card>
              </el-col>

              <el-col :xs="24" :xl="12">
                <el-card class="chart-card view-card">
                  <template #header>
                    <div class="chart-card-header">
                      <span>涨跌额</span>
                      <small>涨跌额</small>
                    </div>
                  </template>
                  <div ref="changeAmountChartRef" class="chart"></div>
                </el-card>
              </el-col>

              <el-col :xs="24" :xl="12">
                <el-card class="chart-card view-card">
                  <template #header>
                    <div class="chart-card-header">
                      <span>换手率</span>
                      <small>换手率</small>
                    </div>
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
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import axios from 'axios'
import * as echarts from 'echarts'
import { formatFetchedAt, getFetchedAt, parseApiPayload } from '../utils/responseMeta'

const router = useRouter()
const route = useRoute()

const stockCode = ref('600960')
const stockCodeInput = ref('600960')
const stockMarket = ref<number | undefined>(undefined)
const loading = ref(false)
const klineLoading = ref(false)
const error = ref('')
const stockInfo = ref<any>(null)
const klineData = ref<any[]>([])
const originalKlineData = ref<any[]>([])
const dateRange = ref<[string | undefined, string | undefined]>([undefined, undefined])
const quoteFetchedAt = ref('--')
const klineFetchedAt = ref('--')

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

const handleBack = () => {
  router.push('/stock-pool')
}

const normalizeStockCode = (value: string | undefined): string => {
  return (value || '').trim()
}

const parseMarket = (value: unknown): number | undefined => {
  if (value === undefined || value === null || value === '') return undefined
  const market = Number(value)
  return Number.isNaN(market) ? undefined : market
}

const handleSearch = () => {
  const code = normalizeStockCode(stockCodeInput.value)
  if (!code) {
    error.value = '请输入股票代码'
    return
  }

  error.value = ''
  router.push({
    path: '/stock-real',
    query: {
      stockCode: code
    }
  })
}

const formatNumber = (value: number | string | undefined | null): string => {
  if (value === undefined || value === null || value === '') return '--'
  const num = Number(value)
  if (Number.isNaN(num)) return '--'
  return num.toFixed(2)
}

const formatScaledNumber = (value: number | string | undefined | null, scale = 100): string => {
  if (value === undefined || value === null || value === '') return '--'
  const num = Number(value)
  if (Number.isNaN(num)) return '--'
  return formatNumber(num / scale)
}

const formatScaledPercent = (value: number | string | undefined | null, scale = 100): string => {
  if (value === undefined || value === null || value === '') return '--'
  const num = Number(value)
  if (Number.isNaN(num)) return '--'
  return `${formatNumber(num / scale)}%`
}

const formatVolume = (value: number | string | undefined | null): string => {
  if (value === undefined || value === null || value === '') return '--'
  const num = Number(value)
  if (Number.isNaN(num)) return '--'
  if (num >= 100000000) return `${(num / 100000000).toFixed(2)}亿`
  if (num >= 10000) return `${(num / 10000).toFixed(2)}万`
  return num.toString()
}

const formatAmount = (value: number | string | undefined | null): string => {
  if (value === undefined || value === null || value === '') return '--'
  const num = Number(value)
  if (Number.isNaN(num)) return '--'
  if (num >= 100000000) return `${(num / 100000000).toFixed(2)}亿`
  if (num >= 10000) return `${(num / 10000).toFixed(2)}万`
  return num.toString()
}

const quoteSummary = computed(() => {
  if (!stockInfo.value) return []

  const changePercent = Number(stockInfo.value.f170 || 0) / 100

  return [
    {
      label: '现价',
      value: formatScaledNumber(stockInfo.value.f43),
      note: `${changePercent >= 0 ? '+' : ''}${formatScaledPercent(stockInfo.value.f170)} / ${
        Number(stockInfo.value.f169 || 0) >= 0 ? '+' : ''
      }${formatScaledNumber(stockInfo.value.f169)}`,
      tone: changePercent >= 0 ? 'rise' : 'fall'
    },
    {
      label: '委差 / 委比',
      value: `${stockInfo.value.f192 ?? '--'} / ${formatScaledPercent(stockInfo.value.f191)}`,
      note: '观察买卖盘当前强弱',
      tone: Number(stockInfo.value.f191 || 0) >= 0 ? 'rise' : 'fall'
    },
    {
      label: '成交额',
      value: formatAmount(stockInfo.value.f48),
      note: `量比 ${formatScaledNumber(stockInfo.value.f50)}`,
      tone: ''
    },
    {
      label: '换手率',
      value: formatScaledPercent(stockInfo.value.f168),
      note: `均价 ${formatScaledNumber(stockInfo.value.f71)}`,
      tone: ''
    }
  ]
})

const stockDataList = computed(() => {
  if (!stockInfo.value) return []

  return [
    { label: '委差', value: stockInfo.value.f192 ?? '--' },
    { label: '委比', value: formatScaledPercent(stockInfo.value.f191) },
    { label: '卖5', value: formatScaledNumber(stockInfo.value.f32) },
    { label: '卖4', value: formatScaledNumber(stockInfo.value.f34) },
    { label: '卖3', value: formatScaledNumber(stockInfo.value.f36) },
    { label: '卖2', value: formatScaledNumber(stockInfo.value.f38) },
    { label: '卖1', value: formatScaledNumber(stockInfo.value.f40) },
    { label: '买1', value: formatScaledNumber(stockInfo.value.f20) },
    { label: '买2', value: formatScaledNumber(stockInfo.value.f18) },
    { label: '买3', value: formatScaledNumber(stockInfo.value.f16) },
    { label: '买4', value: formatScaledNumber(stockInfo.value.f14) },
    { label: '买5', value: formatScaledNumber(stockInfo.value.f12) },
    { label: '内盘', value: formatVolume(stockInfo.value.f161) },
    { label: '外盘', value: formatVolume(stockInfo.value.f49) },
    { label: '成交额', value: formatAmount(stockInfo.value.f48) },
    { label: '换手率', value: formatScaledPercent(stockInfo.value.f168) },
    { label: '量比', value: formatScaledNumber(stockInfo.value.f50) },
    { label: '均价', value: formatScaledNumber(stockInfo.value.f71) },
    { label: '股票代码', value: stockInfo.value.f57 ?? '--' },
    { label: '股票名称', value: stockInfo.value.f58 ?? '--' },
    { label: '所属板块', value: stockInfo.value.f128 ?? '--' }
  ]
})

const parseKlineData = (klines: string[]): any[] => {
  return klines.map((line) => {
    const [
      f51 = '',
      f52 = '0',
      f53 = '0',
      f54 = '0',
      f55 = '0',
      f56 = '0',
      f57 = '0',
      f58 = '0',
      f59 = '0',
      f60 = '0',
      f61 = '0'
    ] = line.split(',')

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

const fetchStockRealData = async (code: string, market?: number): Promise<any> => {
  const params = new URLSearchParams({ stockCode: code })
  if (market !== undefined) {
    params.set('market', String(market))
  }

  const response = await axios.get(`/api/stock/real?${params.toString()}`)
  const parsedData = parseApiPayload(response.data)
  quoteFetchedAt.value = formatFetchedAt(getFetchedAt(parsedData))

  if (parsedData && parsedData.data) {
    return parsedData.data
  }

  throw new Error('获取股票数据失败')
}

const fetchStockKlineData = async (code: string, market?: number): Promise<any[]> => {
  try {
    const params = new URLSearchParams({ stockCode: code })
    if (market !== undefined) {
      params.set('market', String(market))
    }

    const response = await axios.get(`/api/stock/kline?${params.toString()}`)
    const parsedData = parseApiPayload(response.data)
    klineFetchedAt.value = formatFetchedAt(getFetchedAt(parsedData))

    if (parsedData && parsedData.rc === 0 && parsedData.data?.klines) {
      return parseKlineData(parsedData.data.klines)
    }
  } catch (err) {
    console.error('获取股票 K 线失败:', err)
  }

  return []
}

const applyDefaultDateRange = () => {
  if (originalKlineData.value.length === 0) {
    dateRange.value = [undefined, undefined]
    klineData.value = []
    return
  }

  const maxTimeStr = originalKlineData.value.reduce((max, item) => {
    return item.time > max ? item.time : max
  }, originalKlineData.value[0].time)

  const endDate = new Date(maxTimeStr)
  const startDate = new Date(endDate)
  startDate.setMonth(startDate.getMonth() - 2)

  const formatDate = (date: Date): string => {
    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    return `${year}-${month}-${day}`
  }

  dateRange.value = [formatDate(startDate), formatDate(endDate)]
  handleTimeFilter()
}

const handleTimeFilter = () => {
  const [startDate, endDate] = dateRange.value

  if (originalKlineData.value.length === 0) {
    klineData.value = []
    return
  }

  if (startDate || endDate) {
    klineData.value = originalKlineData.value.filter((item) => {
      const itemDate = item.time
      const matchStart = startDate ? itemDate >= startDate : true
      const matchEnd = endDate ? itemDate <= endDate : true
      return matchStart && matchEnd
    })
  } else {
    klineData.value = [...originalKlineData.value]
  }

  nextTick(() => {
    initCharts()
  })
}

const fetchStockData = async () => {
  if (!stockCode.value) {
    stockCode.value = '600960'
  }

  loading.value = true
  klineLoading.value = false
  error.value = ''
  stockInfo.value = null
  klineData.value = []
  originalKlineData.value = []
  dateRange.value = [undefined, undefined]
  quoteFetchedAt.value = '--'
  klineFetchedAt.value = '--'
  disposeCharts()

  try {
    const quoteData = await fetchStockRealData(stockCode.value, stockMarket.value)
    stockInfo.value = quoteData
  } catch (err: any) {
    error.value = err?.message || '获取数据失败'
    loading.value = false
    return
  }

  loading.value = false
  klineLoading.value = true

  try {
    const klineResult = await fetchStockKlineData(stockCode.value, stockMarket.value)
    originalKlineData.value = [...klineResult]
    applyDefaultDateRange()
  } finally {
    klineLoading.value = false
  }
}

const initSingleChart = (
  chartRef: { value?: HTMLElement },
  title: string,
  data: number[],
  color: string
) => {
  if (!chartRef.value) return null

  const chart = echarts.init(chartRef.value)
  chart.setOption({
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
      data: klineData.value.map((item) => item.time),
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
        data,
        smooth: true,
        lineStyle: {
          width: 2,
          color
        }
      }
    ]
  })

  return chart
}

const initPriceChart = () => {
  if (!priceChartRef.value) return

  priceChart = echarts.init(priceChartRef.value)
  priceChart.setOption({
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
      data: klineData.value.map((item) => item.time),
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
        data: klineData.value.map((item) => item.open),
        smooth: true,
        lineStyle: { width: 2, color: '#315fce' }
      },
      {
        name: '收盘价',
        type: 'line',
        data: klineData.value.map((item) => item.close),
        smooth: true,
        lineStyle: { width: 2, color: '#d64b43' }
      },
      {
        name: '最高价',
        type: 'line',
        data: klineData.value.map((item) => item.high),
        smooth: true,
        lineStyle: { width: 2, color: '#0f9968' }
      },
      {
        name: '最低价',
        type: 'line',
        data: klineData.value.map((item) => item.low),
        smooth: true,
        lineStyle: { width: 2, color: '#b88a45' }
      }
    ]
  })

  priceChart.resize()
}

const initCharts = () => {
  if (klineData.value.length === 0) return

  disposeCharts()
  initPriceChart()
  volumeChart = initSingleChart(volumeChartRef, '成交量', klineData.value.map((item) => item.volume), '#6f7f95')
  amountChart = initSingleChart(amountChartRef, '成交额', klineData.value.map((item) => item.amount), '#315fce')
  amplitudeChart = initSingleChart(amplitudeChartRef, '振幅', klineData.value.map((item) => item.amplitude), '#d64b43')
  changePercentChart = initSingleChart(changePercentChartRef, '涨跌幅', klineData.value.map((item) => item.changePercent), '#0f9968')
  changeAmountChart = initSingleChart(changeAmountChartRef, '涨跌额', klineData.value.map((item) => item.changeAmount), '#b88a45')
  turnoverChart = initSingleChart(turnoverChartRef, '换手率', klineData.value.map((item) => item.turnover), '#162033')
}

const disposeCharts = () => {
  priceChart?.dispose()
  volumeChart?.dispose()
  amountChart?.dispose()
  amplitudeChart?.dispose()
  changePercentChart?.dispose()
  changeAmountChart?.dispose()
  turnoverChart?.dispose()

  priceChart = null
  volumeChart = null
  amountChart = null
  amplitudeChart = null
  changePercentChart = null
  changeAmountChart = null
  turnoverChart = null
}

const handleResize = () => {
  priceChart?.resize()
  volumeChart?.resize()
  amountChart?.resize()
  amplitudeChart?.resize()
  changePercentChart?.resize()
  changeAmountChart?.resize()
  turnoverChart?.resize()
}

watch(
  () => [route.query.stockCode, route.query.market],
  ([newStockCode, newMarket]) => {
    const nextCode = normalizeStockCode(newStockCode as string | undefined) || '600960'
    stockCode.value = nextCode
    stockCodeInput.value = nextCode
    stockMarket.value = parseMarket(newMarket)
    fetchStockData()
  },
  { immediate: true }
)

watch(
  () => klineData.value.length,
  (length) => {
    if (stockInfo.value && length > 0) {
      nextTick(() => {
        initCharts()
      })
    }
  }
)

onMounted(() => {
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  disposeCharts()
})
</script>

<style scoped>
.stock-real-container {
  min-height: 100%;
}

.data-card {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-height: 0;
}

.search-bar {
  display: flex;
  gap: 12px;
  width: 100%;
}

.hero-badge-row {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  width: 100%;
}

.time-filter-form {
  margin-bottom: 0;
}

.loading-container {
  flex: 1;
  min-height: 600px;
  overflow: auto;
}

.error-alert {
  margin-bottom: 18px;
}

.snapshot-card {
  margin-bottom: 18px;
}

.snapshot-table {
  width: 100%;
}

.filter-bar {
  margin-bottom: 16px;
  padding: 16px 18px 0;
}

.charts-container {
  margin-top: 6px;
}

.chart-card {
  margin-bottom: 18px;
}

.chart-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
  color: var(--text-primary);
  font-weight: 700;
}

.chart-card-header small {
  color: var(--text-secondary);
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.chart {
  width: 100%;
  height: 360px;
}

@media (max-width: 768px) {
  .search-bar {
    flex-direction: column;
  }

  .hero-badge-row {
    grid-template-columns: 1fr;
  }

  .filter-bar {
    padding: 14px 14px 0;
  }

  .chart {
    height: 320px;
  }
}
</style>
