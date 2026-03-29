<template>
  <div class="page-shell industry-kline-container">
    <section class="page-hero">
      <div>
        <p class="page-kicker">Industry Trend</p>
        <h2 class="page-title">{{ industryInfo?.name || 'Industry K-Line' }}</h2>
        <p class="page-subtitle">
          Review price structure, turnover, and historical range for the selected industry board.
        </p>
      </div>

      <div class="page-actions">
        <div class="hero-badge">
          <span class="hero-badge-label">Industry Code</span>
          <strong>{{ industryInfo?.code || getIndustryCode() }}</strong>
        </div>
        <div class="hero-badge">
          <span class="hero-badge-label">Date Range</span>
          <strong>{{ activeDateLabel }}</strong>
        </div>
        <div class="hero-badge">
          <span class="hero-badge-label">DB Fetched</span>
          <strong>{{ fetchedAtLabel }}</strong>
        </div>
        <el-button type="primary" @click="fetchIndustryKlineData">Refresh</el-button>
        <el-button type="default" @click="handleBack">Back</el-button>
      </div>
    </section>

    <section v-if="!error && klineStats.length > 0" class="metric-grid">
      <article
        v-for="item in klineStats"
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
            <p class="section-kicker">K-Line Workspace</p>
            <h3 class="section-title">Industry History</h3>
          </div>
          <p class="section-note">
            The page now shows the database fetched time so you can judge how fresh the history is.
          </p>
        </div>
      </template>

      <div v-loading="loading" element-loading-text="Loading industry history..." class="loading-container">
        <el-alert
          v-if="error"
          :title="error"
          type="error"
          show-icon
          :closable="false"
          class="error-alert"
        />

        <template v-if="!error && industryInfo">
          <div class="filter-bar market-subcard">
            <el-form :inline="true" class="time-filter-form" @submit.prevent="handleTimeFilter">
              <el-form-item label="Start">
                <el-date-picker
                  v-model="dateRange[0]"
                  type="date"
                  placeholder="Start date"
                  format="YYYY-MM-DD"
                  value-format="YYYY-MM-DD"
                  @change="handleTimeFilter"
                />
              </el-form-item>
              <el-form-item label="End">
                <el-date-picker
                  v-model="dateRange[1]"
                  type="date"
                  placeholder="End date"
                  format="YYYY-MM-DD"
                  value-format="YYYY-MM-DD"
                  @change="handleTimeFilter"
                />
              </el-form-item>
            </el-form>
          </div>

          <el-card class="info-card market-subcard" :body-style="{ padding: '12px 14px' }">
            <el-descriptions :column="6" border>
              <el-descriptions-item label="Code">{{ industryInfo.code }}</el-descriptions-item>
              <el-descriptions-item label="Market">{{ industryInfo.market }}</el-descriptions-item>
              <el-descriptions-item label="Name">{{ industryInfo.name }}</el-descriptions-item>
              <el-descriptions-item label="Decimal">{{ industryInfo.decimal }}</el-descriptions-item>
              <el-descriptions-item label="Total">{{ industryInfo.dktotal }}</el-descriptions-item>
              <el-descriptions-item label="Pre K Price">{{ formatNumber(industryInfo.preKPrice) }}</el-descriptions-item>
            </el-descriptions>
          </el-card>

          <div class="charts-container">
            <el-row :gutter="18">
              <el-col :span="24">
                <el-card class="chart-card view-card">
                  <template #header>
                    <div class="chart-card-header">
                      <span>Price Trend</span>
                      <small>Open / Close / High / Low</small>
                    </div>
                  </template>
                  <div ref="priceChartRef" class="chart"></div>
                </el-card>
              </el-col>

              <el-col :xs="24" :xl="12">
                <el-card class="chart-card view-card">
                  <template #header>
                    <div class="chart-card-header">
                      <span>Volume</span>
                      <small>Trading Volume</small>
                    </div>
                  </template>
                  <div ref="volumeChartRef" class="chart"></div>
                </el-card>
              </el-col>

              <el-col :xs="24" :xl="12">
                <el-card class="chart-card view-card">
                  <template #header>
                    <div class="chart-card-header">
                      <span>Amount</span>
                      <small>Trading Amount</small>
                    </div>
                  </template>
                  <div ref="amountChartRef" class="chart"></div>
                </el-card>
              </el-col>

              <el-col :xs="24" :xl="12">
                <el-card class="chart-card view-card">
                  <template #header>
                    <div class="chart-card-header">
                      <span>Amplitude</span>
                      <small>Volatility</small>
                    </div>
                  </template>
                  <div ref="amplitudeChartRef" class="chart"></div>
                </el-card>
              </el-col>

              <el-col :xs="24" :xl="12">
                <el-card class="chart-card view-card">
                  <template #header>
                    <div class="chart-card-header">
                      <span>Change %</span>
                      <small>Percent Change</small>
                    </div>
                  </template>
                  <div ref="changePercentChartRef" class="chart"></div>
                </el-card>
              </el-col>

              <el-col :xs="24" :xl="12">
                <el-card class="chart-card view-card">
                  <template #header>
                    <div class="chart-card-header">
                      <span>Change Value</span>
                      <small>Point Change</small>
                    </div>
                  </template>
                  <div ref="changeAmountChartRef" class="chart"></div>
                </el-card>
              </el-col>

              <el-col :xs="24" :xl="12">
                <el-card class="chart-card view-card">
                  <template #header>
                    <div class="chart-card-header">
                      <span>Turnover</span>
                      <small>Turnover Rate</small>
                    </div>
                  </template>
                  <div ref="turnoverChartRef" class="chart"></div>
                </el-card>
              </el-col>
            </el-row>
          </div>
        </template>

        <el-empty
          v-if="!loading && !error && !industryInfo"
          description="No industry history available"
        />
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

type KlinePoint = {
  time: string
  open: number
  close: number
  high: number
  low: number
  volume: number
  amount: number
  amplitude: number
  changePercent: number
  changeAmount: number
  turnover: number
}

type IndustryInfo = {
  code: string
  market: number | string
  name: string
  decimal: number
  dktotal: number
  preKPrice: number
}

const router = useRouter()
const route = useRoute()

const loading = ref(false)
const error = ref('')
const industryInfo = ref<IndustryInfo | null>(null)
const allKlineData = ref<KlinePoint[]>([])
const klineData = ref<KlinePoint[]>([])
const dateRange = ref<[string | undefined, string | undefined]>([undefined, undefined])
const fetchedAtLabel = ref('--')

const priceChartRef = ref<HTMLElement>()
const volumeChartRef = ref<HTMLElement>()
const amountChartRef = ref<HTMLElement>()
const amplitudeChartRef = ref<HTMLElement>()
const changePercentChartRef = ref<HTMLElement>()
const changeAmountChartRef = ref<HTMLElement>()
const turnoverChartRef = ref<HTMLElement>()

const getIndustryCode = (): string => {
  const value = route.query.industryCode
  return typeof value === 'string' && value.trim() ? value.trim() : 'BK0428'
}

const handleBack = () => {
  router.push('/industry-base')
}

const formatNumber = (value: number | undefined | null): string => {
  if (value === undefined || value === null || Number.isNaN(value)) return '--'
  return Number(value).toFixed(2)
}

const formatPercent = (value: number | undefined | null): string => {
  if (value === undefined || value === null || Number.isNaN(value)) return '--'
  return `${Number(value).toFixed(2)}%`
}

const formatCompact = (value: number | undefined | null): string => {
  if (value === undefined || value === null || Number.isNaN(value)) return '--'
  const num = Number(value)
  if (Math.abs(num) >= 100000000) return `${(num / 100000000).toFixed(2)}B`
  if (Math.abs(num) >= 10000) return `${(num / 10000).toFixed(2)}W`
  return num.toFixed(2)
}

const latestKline = computed(() => {
  return klineData.value.length > 0 ? klineData.value[klineData.value.length - 1] : null
})

const activeDateLabel = computed(() => {
  const [startDate, endDate] = dateRange.value
  if (startDate && endDate) return `${startDate} -> ${endDate}`
  if (startDate) return `${startDate} -> latest`
  if (endDate) return `up to ${endDate}`
  return 'full history'
})

const klineStats = computed(() => {
  const latest = latestKline.value
  if (!latest || klineData.value.length === 0) return []

  const highs = klineData.value.map((item) => item.high)
  const lows = klineData.value.map((item) => item.low)
  return [
    {
      label: 'Last Close',
      value: formatNumber(latest.close),
      note: latest.time,
      tone: latest.changePercent >= 0 ? 'rise' : 'fall'
    },
    {
      label: 'Change %',
      value: formatPercent(latest.changePercent),
      note: `Change ${formatNumber(latest.changeAmount)}`,
      tone: latest.changePercent >= 0 ? 'rise' : 'fall'
    },
    {
      label: 'Range High',
      value: formatNumber(Math.max(...highs)),
      note: 'Highest point in filtered window',
      tone: ''
    },
    {
      label: 'Range Low',
      value: formatNumber(Math.min(...lows)),
      note: 'Lowest point in filtered window',
      tone: ''
    },
    {
      label: 'Latest Amount',
      value: formatCompact(latest.amount),
      note: `Volume ${formatCompact(latest.volume)}`,
      tone: ''
    },
    {
      label: 'Turnover',
      value: formatPercent(latest.turnover),
      note: `Amplitude ${formatPercent(latest.amplitude)}`,
      tone: ''
    }
  ]
})

const parseKlineData = (rawKlines: string[]): KlinePoint[] => {
  return rawKlines
    .map((line) => {
      const [
        time = '',
        open = '0',
        close = '0',
        high = '0',
        low = '0',
        volume = '0',
        amount = '0',
        amplitude = '0',
        changePercent = '0',
        changeAmount = '0',
        turnover = '0'
      ] = line.split(',')

      return {
        time,
        open: Number.parseFloat(open),
        close: Number.parseFloat(close),
        high: Number.parseFloat(high),
        low: Number.parseFloat(low),
        volume: Number.parseFloat(volume),
        amount: Number.parseFloat(amount),
        amplitude: Number.parseFloat(amplitude),
        changePercent: Number.parseFloat(changePercent),
        changeAmount: Number.parseFloat(changeAmount),
        turnover: Number.parseFloat(turnover)
      }
    })
    .filter((item) => item.time)
}

const ensureChart = (target?: HTMLElement | null): echarts.ECharts | null => {
  if (!target) return null
  return echarts.getInstanceByDom(target) || echarts.init(target)
}

const getAxisData = () => klineData.value.map((item) => item.time)

const renderPriceChart = () => {
  const chart = ensureChart(priceChartRef.value)
  if (!chart) return

  chart.setOption({
    animation: false,
    tooltip: { trigger: 'axis' },
    legend: { top: 0, textStyle: { color: '#475569' } },
    grid: { left: '4%', right: '3%', top: 40, bottom: 50, containLabel: true },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: getAxisData(),
      axisLabel: { rotate: 45, color: '#64748b' }
    },
    yAxis: {
      type: 'value',
      axisLabel: { color: '#64748b' },
      splitLine: { lineStyle: { color: 'rgba(148, 163, 184, 0.18)' } }
    },
    series: [
      { name: 'Open', type: 'line', smooth: true, showSymbol: false, data: klineData.value.map((item) => item.open), lineStyle: { color: '#2563eb', width: 2 } },
      { name: 'Close', type: 'line', smooth: true, showSymbol: false, data: klineData.value.map((item) => item.close), lineStyle: { color: '#dc2626', width: 2 } },
      { name: 'High', type: 'line', smooth: true, showSymbol: false, data: klineData.value.map((item) => item.high), lineStyle: { color: '#16a34a', width: 2 } },
      { name: 'Low', type: 'line', smooth: true, showSymbol: false, data: klineData.value.map((item) => item.low), lineStyle: { color: '#f59e0b', width: 2 } }
    ]
  })
}

const renderSingleSeriesChart = (
  target: HTMLElement | undefined,
  name: string,
  values: number[],
  color: string
) => {
  const chart = ensureChart(target)
  if (!chart) return

  chart.setOption({
    animation: false,
    tooltip: { trigger: 'axis' },
    grid: { left: '4%', right: '3%', top: 18, bottom: 42, containLabel: true },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: getAxisData(),
      axisLabel: { rotate: 45, color: '#64748b' }
    },
    yAxis: {
      type: 'value',
      axisLabel: { color: '#64748b' },
      splitLine: { lineStyle: { color: 'rgba(148, 163, 184, 0.18)' } }
    },
    series: [
      {
        name,
        type: 'line',
        smooth: true,
        showSymbol: false,
        data: values,
        lineStyle: { color, width: 2 },
        areaStyle: { color: `${color}22` }
      }
    ]
  })
}

const renderAllCharts = () => {
  renderPriceChart()
  renderSingleSeriesChart(volumeChartRef.value, 'Volume', klineData.value.map((item) => item.volume), '#64748b')
  renderSingleSeriesChart(amountChartRef.value, 'Amount', klineData.value.map((item) => item.amount), '#2563eb')
  renderSingleSeriesChart(amplitudeChartRef.value, 'Amplitude', klineData.value.map((item) => item.amplitude), '#dc2626')
  renderSingleSeriesChart(changePercentChartRef.value, 'Change %', klineData.value.map((item) => item.changePercent), '#16a34a')
  renderSingleSeriesChart(changeAmountChartRef.value, 'Change Value', klineData.value.map((item) => item.changeAmount), '#f59e0b')
  renderSingleSeriesChart(turnoverChartRef.value, 'Turnover', klineData.value.map((item) => item.turnover), '#7c3aed')
}

const resizeCharts = () => {
  [
    priceChartRef.value,
    volumeChartRef.value,
    amountChartRef.value,
    amplitudeChartRef.value,
    changePercentChartRef.value,
    changeAmountChartRef.value,
    turnoverChartRef.value
  ].forEach((target) => {
    if (target) {
      echarts.getInstanceByDom(target)?.resize()
    }
  })
}

const disposeCharts = () => {
  [
    priceChartRef.value,
    volumeChartRef.value,
    amountChartRef.value,
    amplitudeChartRef.value,
    changePercentChartRef.value,
    changeAmountChartRef.value,
    turnoverChartRef.value
  ].forEach((target) => {
    if (target) {
      echarts.getInstanceByDom(target)?.dispose()
    }
  })
}

const applyDateFilter = async () => {
  const [startDate, endDate] = dateRange.value
  klineData.value = allKlineData.value.filter((item) => {
    if (startDate && item.time < startDate) return false
    if (endDate && item.time > endDate) return false
    return true
  })

  await nextTick()
  renderAllCharts()
}

const handleTimeFilter = () => {
  void applyDateFilter()
}

const fetchIndustryKlineData = async () => {
  const industryCode = getIndustryCode()
  loading.value = true
  error.value = ''

  try {
    const response = await axios.get(`http://localhost:8080/api/industry/kline?industryCode=${industryCode}`)
    const parsedData = parseApiPayload(response.data)
    fetchedAtLabel.value = formatFetchedAt(getFetchedAt(parsedData))

    if (!parsedData || parsedData.rc !== 0 || !parsedData.data) {
      throw new Error(parsedData?.message || 'Failed to load industry history')
    }

    const payload = parsedData.data
    industryInfo.value = {
      code: payload.code,
      market: payload.market,
      name: payload.name,
      decimal: payload.decimal,
      dktotal: payload.dktotal,
      preKPrice: payload.preKPrice
    }

    allKlineData.value = parseKlineData(payload.klines || [])
    await applyDateFilter()
  } catch (err: any) {
    error.value = err?.message || 'Failed to load industry history'
    industryInfo.value = null
    allKlineData.value = []
    klineData.value = []
    fetchedAtLabel.value = '--'
    disposeCharts()
  } finally {
    loading.value = false
  }
}

watch(
  () => route.query.industryCode,
  () => {
    void fetchIndustryKlineData()
  }
)

onMounted(() => {
  void fetchIndustryKlineData()
  window.addEventListener('resize', resizeCharts)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeCharts)
  disposeCharts()
})
</script>

<style scoped>
.industry-kline-container {
  min-height: 100%;
}

.data-card {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-height: 0;
}

.loading-container {
  min-height: 600px;
  overflow: auto;
}

.error-alert {
  margin-bottom: 18px;
}

.filter-bar {
  margin-bottom: 16px;
  padding: 16px 18px 0;
}

.info-card {
  margin-bottom: 18px;
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
  .filter-bar {
    padding: 14px 14px 0;
  }

  .chart {
    height: 320px;
  }
}
</style>
