<template>
  <div class="page-shell industry-kline-container">
    <section class="page-hero">
      <div>
        <p class="page-kicker">行业趋势</p>
        <h2 class="page-title">{{ industryInfo?.name || '行业K线' }}</h2>
        <p class="page-subtitle">
          查看当前行业板块的价格结构、换手表现和历史区间，辅助判断趋势延续性。
        </p>
      </div>

      <div class="page-actions">
        <div class="hero-badge">
          <span class="hero-badge-label">行业代码</span>
          <strong>{{ industryInfo?.code || getIndustryCode() }}</strong>
        </div>
        <div class="hero-badge">
          <span class="hero-badge-label">观察区间</span>
          <strong>{{ activeDateLabel }}</strong>
        </div>
        <div class="hero-badge">
          <span class="hero-badge-label">数据库抓取</span>
          <strong>{{ fetchedAtLabel }}</strong>
        </div>
        <el-button type="primary" @click="fetchIndustryKlineData">刷新数据</el-button>
        <el-button type="default" @click="handleBack">返回</el-button>
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
            <p class="section-kicker">K线工作区</p>
            <h3 class="section-title">行业历史走势</h3>
          </div>
          <p class="section-note">
            页面会展示数据库抓取时间，方便判断这份行业历史数据的新鲜度。
          </p>
        </div>
      </template>

      <div v-loading="loading" element-loading-text="加载行业历史数据中..." class="loading-container">
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
              <el-form-item label="开始时间">
                <el-date-picker
                  v-model="dateRange[0]"
                  type="date"
                  placeholder="选择开始日期"
                  format="YYYY-MM-DD"
                  value-format="YYYY-MM-DD"
                  @change="handleTimeFilter"
                />
              </el-form-item>
              <el-form-item label="结束时间">
                <el-date-picker
                  v-model="dateRange[1]"
                  type="date"
                  placeholder="选择结束日期"
                  format="YYYY-MM-DD"
                  value-format="YYYY-MM-DD"
                  @change="handleTimeFilter"
                />
              </el-form-item>
            </el-form>
          </div>

          <el-card class="info-card market-subcard" :body-style="{ padding: '12px 14px' }">
            <el-descriptions :column="6" border>
              <el-descriptions-item label="代码">{{ industryInfo.code }}</el-descriptions-item>
              <el-descriptions-item label="市场">{{ industryInfo.market }}</el-descriptions-item>
              <el-descriptions-item label="名称">{{ industryInfo.name }}</el-descriptions-item>
              <el-descriptions-item label="小数位">{{ industryInfo.decimal }}</el-descriptions-item>
              <el-descriptions-item label="总数">{{ industryInfo.dktotal }}</el-descriptions-item>
              <el-descriptions-item label="前值">{{ formatNumber(industryInfo.preKPrice) }}</el-descriptions-item>
            </el-descriptions>
          </el-card>

          <div class="charts-container">
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
                      <small>交易量</small>
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
                      <small>交易额</small>
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
                      <small>百分比变化</small>
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
                      <small>点位变化</small>
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
                      <small>换手表现</small>
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
          description="暂无行业历史数据"
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
  if (startDate && endDate) return `${startDate} 至 ${endDate}`
  if (startDate) return `${startDate} 至最新`
  if (endDate) return `截至 ${endDate}`
  return '全量历史'
})

const klineStats = computed(() => {
  const latest = latestKline.value
  if (!latest || klineData.value.length === 0) return []

  const highs = klineData.value.map((item) => item.high)
  const lows = klineData.value.map((item) => item.low)
  return [
    {
      label: '最新收盘',
      value: formatNumber(latest.close),
      note: latest.time,
      tone: latest.changePercent >= 0 ? 'rise' : 'fall'
    },
    {
      label: '涨跌幅',
      value: formatPercent(latest.changePercent),
      note: `涨跌额 ${formatNumber(latest.changeAmount)}`,
      tone: latest.changePercent >= 0 ? 'rise' : 'fall'
    },
    {
      label: '区间最高',
      value: formatNumber(Math.max(...highs)),
      note: '当前筛选区间内最高点',
      tone: ''
    },
    {
      label: '区间最低',
      value: formatNumber(Math.min(...lows)),
      note: '当前筛选区间内最低点',
      tone: ''
    },
    {
      label: '最新成交额',
      value: formatCompact(latest.amount),
      note: `成交量 ${formatCompact(latest.volume)}`,
      tone: ''
    },
    {
      label: '换手率',
      value: formatPercent(latest.turnover),
      note: `振幅 ${formatPercent(latest.amplitude)}`,
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
      { name: '开盘价', type: 'line', smooth: true, showSymbol: false, data: klineData.value.map((item) => item.open), lineStyle: { color: '#2563eb', width: 2 } },
      { name: '收盘价', type: 'line', smooth: true, showSymbol: false, data: klineData.value.map((item) => item.close), lineStyle: { color: '#dc2626', width: 2 } },
      { name: '最高价', type: 'line', smooth: true, showSymbol: false, data: klineData.value.map((item) => item.high), lineStyle: { color: '#16a34a', width: 2 } },
      { name: '最低价', type: 'line', smooth: true, showSymbol: false, data: klineData.value.map((item) => item.low), lineStyle: { color: '#f59e0b', width: 2 } }
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
  renderSingleSeriesChart(volumeChartRef.value, '成交量', klineData.value.map((item) => item.volume), '#64748b')
  renderSingleSeriesChart(amountChartRef.value, '成交额', klineData.value.map((item) => item.amount), '#2563eb')
  renderSingleSeriesChart(amplitudeChartRef.value, '振幅', klineData.value.map((item) => item.amplitude), '#dc2626')
  renderSingleSeriesChart(changePercentChartRef.value, '涨跌幅', klineData.value.map((item) => item.changePercent), '#16a34a')
  renderSingleSeriesChart(changeAmountChartRef.value, '涨跌额', klineData.value.map((item) => item.changeAmount), '#f59e0b')
  renderSingleSeriesChart(turnoverChartRef.value, '换手率', klineData.value.map((item) => item.turnover), '#7c3aed')
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
      throw new Error(parsedData?.message || '加载行业历史数据失败')
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
    error.value = err?.message || '加载行业历史数据失败'
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
