<template>
  <div class="page-shell stock-analysis-container">
    <section class="page-hero">
      <div>
        <p class="page-kicker">实时选股</p>
        <h2 class="page-title">候选优质股票分析</h2>
        <p class="page-subtitle">
          第二阶段在原有机会评分上，补上基础质量因子、风险标签和分钟级分时强弱，既能筛票，也能看当前盘中的状态。
        </p>
      </div>

      <div class="page-actions">
        <div class="hero-badge">
          <span class="hero-badge-label">市场状态</span>
          <strong>{{ summary?.marketTone || '--' }}</strong>
        </div>
        <div class="hero-badge">
          <span class="hero-badge-label">情绪分数</span>
          <strong>{{ formatScore(summary?.marketSentimentScore) }}</strong>
        </div>
        <div class="hero-badge">
          <span class="hero-badge-label">股票池快照</span>
          <strong>{{ stockPoolSnapshotLabel }}</strong>
        </div>
        <el-button type="primary" @click="fetchAnalysisData">刷新分析</el-button>
      </div>
    </section>

    <section class="metric-grid" v-if="summary">
      <article
        v-for="item in analysisMetrics"
        :key="item.label"
        class="metric-card"
      >
        <p class="metric-label">{{ item.label }}</p>
        <p :class="['metric-value', item.tone, item.compact ? 'metric-value--compact' : '']">
          {{ item.value }}
        </p>
        <p class="metric-note">{{ item.note }}</p>
      </article>
    </section>

    <div class="warning-stack" v-if="warnings.length > 0">
      <el-alert
        v-for="warning in warnings"
        :key="warning"
        :title="warning"
        type="warning"
        show-icon
        :closable="false"
      />
    </div>

    <el-card class="view-card overview-card">
      <template #header>
        <div class="section-header">
          <div>
            <p class="section-kicker">市场环境</p>
            <h3 class="section-title">大盘与热点概览</h3>
          </div>
          <p class="section-note">
            候选评分基于本地股票池、行业快照和核心指数；分时明细按需抓取，避免一次性请求过多。
          </p>
        </div>
      </template>

      <div v-loading="loading" element-loading-text="正在生成实时分析..." class="loading-container">
        <el-alert
          v-if="error"
          :title="error"
          type="error"
          show-icon
          :closable="false"
          class="error-alert"
        />

        <template v-if="!error && summary">
          <div class="snapshot-grid">
            <div class="snapshot-item market-subcard">
              <p class="snapshot-label">分析生成时间</p>
              <strong>{{ formatFetchedAt(summary.analysisGeneratedAt) }}</strong>
            </div>
            <div class="snapshot-item market-subcard">
              <p class="snapshot-label">行业快照时间</p>
              <strong>{{ formatFetchedAt(summary.industryBaseFetchedAt) }}</strong>
            </div>
            <div class="snapshot-item market-subcard">
              <p class="snapshot-label">指数抓取时间</p>
              <strong>{{ formatFetchedAt(summary.marketIndexFetchedAt) }}</strong>
            </div>
          </div>

          <div class="index-grid">
            <article
              v-for="indexItem in indices"
              :key="indexItem.code"
              class="index-card market-subcard"
            >
              <div class="index-card-header">
                <div>
                  <p class="index-label">{{ indexItem.label }}</p>
                  <strong class="index-price">{{ formatNumber(indexItem.latestPrice) }}</strong>
                </div>
                <div :class="['index-change', indexItem.changePct >= 0 ? 'rise' : 'fall']">
                  {{ formatSignedPercent(indexItem.changePct) }}
                </div>
              </div>
              <div class="index-card-meta">
                <span>开 {{ formatNumber(indexItem.open) }}</span>
                <span>高 {{ formatNumber(indexItem.high) }}</span>
                <span>低 {{ formatNumber(indexItem.low) }}</span>
                <span>额 {{ formatAmount(indexItem.amount) }}</span>
              </div>
            </article>
          </div>

          <el-row :gutter="18" class="overview-row">
            <el-col :xs="24" :xl="12">
              <el-card class="market-subcard industry-card" shadow="never">
                <template #header>
                  <div class="card-title-row">
                    <span>热点行业</span>
                    <small>按涨幅与净流入排序</small>
                  </div>
                </template>
                <div class="industry-list">
                  <div
                    v-for="industry in topIndustries"
                    :key="industry.industryCode"
                    class="industry-item"
                  >
                    <div>
                      <p class="industry-name">{{ industry.industryName }}</p>
                      <p class="industry-meta">
                        领涨股 {{ industry.leaderName || '--' }} / 上涨占比 {{ formatPercent(industry.advanceRatio) }}
                      </p>
                    </div>
                    <div class="industry-stats">
                      <strong :class="industry.changePct >= 0 ? 'rise' : 'fall'">
                        {{ formatSignedPercent(industry.changePct) }}
                      </strong>
                      <span>{{ formatAmount(industry.netInflow) }}</span>
                    </div>
                  </div>
                </div>
              </el-card>
            </el-col>

            <el-col :xs="24" :xl="12">
              <el-card class="market-subcard overview-card-inner" shadow="never">
                <template #header>
                  <div class="card-title-row">
                    <span>情绪拆解</span>
                    <small>从全市场宽度看强弱</small>
                  </div>
                </template>
                <div class="breadth-grid">
                  <div class="breadth-item">
                    <span>上涨家数</span>
                    <strong class="rise">{{ summary.advanceCount }}</strong>
                  </div>
                  <div class="breadth-item">
                    <span>下跌家数</span>
                    <strong class="fall">{{ summary.declineCount }}</strong>
                  </div>
                  <div class="breadth-item">
                    <span>涨停数量</span>
                    <strong class="rise">{{ summary.limitUpCount }}</strong>
                  </div>
                  <div class="breadth-item">
                    <span>跌停数量</span>
                    <strong class="fall">{{ summary.limitDownCount }}</strong>
                  </div>
                  <div class="breadth-item">
                    <span>平均涨跌幅</span>
                    <strong :class="(summary.averageChangePct ?? 0) >= 0 ? 'rise' : 'fall'">
                      {{ formatSignedPercent(summary.averageChangePct ?? 0) }}
                    </strong>
                  </div>
                  <div class="breadth-item">
                    <span>上涨占比</span>
                    <strong>{{ formatPercent(summary.advanceRatio) }}</strong>
                  </div>
                </div>
              </el-card>
            </el-col>
          </el-row>
        </template>
      </div>
    </el-card>

    <el-card class="view-card intraday-card">
      <template #header>
        <div class="section-header">
          <div>
            <p class="section-kicker">分时强弱</p>
            <h3 class="section-title">当前候选分时分析</h3>
          </div>
          <p class="section-note">
            默认展示当前榜首股票，也可以在候选表里切换。
          </p>
        </div>
      </template>

      <div v-loading="intradayLoading" element-loading-text="正在加载分时分析..." class="loading-container">
        <el-alert
          v-if="intradayError"
          :title="intradayError"
          type="error"
          show-icon
          :closable="false"
          class="error-alert"
        />

        <template v-if="selectedCandidate && intradaySummary && !intradayError">
          <div class="intraday-topbar market-subcard">
            <div>
              <p class="intraday-title">{{ selectedCandidate.stockName }} {{ selectedCandidate.stockCode }}</p>
              <p class="intraday-subtitle">
                {{ selectedCandidate.industryName || '未分类行业' }} / {{ intradaySummary.sessionTone }}
              </p>
            </div>
            <div class="intraday-actions">
              <el-button type="default" @click="goToStockReal(selectedCandidate)">看盘口</el-button>
            </div>
          </div>

          <section class="metric-grid intraday-metric-grid">
            <article
              v-for="item in intradayMetrics"
              :key="item.label"
              class="metric-card"
            >
              <p class="metric-label">{{ item.label }}</p>
              <p :class="['metric-value', item.tone, item.compact ? 'metric-value--compact' : '']">
                {{ item.value }}
              </p>
              <p class="metric-note">{{ item.note }}</p>
            </article>
          </section>

          <div class="signal-grid">
            <el-card class="market-subcard signal-card" shadow="never">
              <template #header>
                <div class="card-title-row">
                  <span>积极信号</span>
                  <small>分钟级判断</small>
                </div>
              </template>
              <div class="tag-list">
                <el-tag
                  v-for="signal in intradaySignals"
                  :key="signal"
                  type="success"
                  effect="plain"
                >
                  {{ signal }}
                </el-tag>
                <span v-if="intradaySignals.length === 0" class="muted-text">暂无明显积极信号</span>
              </div>
            </el-card>

            <el-card class="market-subcard signal-card" shadow="never">
              <template #header>
                <div class="card-title-row">
                  <span>风险提醒</span>
                  <small>日内状态 + 候选风险</small>
                </div>
              </template>
              <div class="tag-list">
                <el-tag
                  v-for="tag in mergedRiskTags"
                  :key="tag"
                  type="danger"
                  effect="plain"
                >
                  {{ tag }}
                </el-tag>
                <span v-if="mergedRiskTags.length === 0" class="muted-text">当前未发现明显高风险标签</span>
              </div>
            </el-card>
          </div>

          <el-card class="market-subcard chart-shell" shadow="never">
            <template #header>
              <div class="card-title-row">
                <span>分时价格与均价</span>
                <small>{{ formatFetchedAt(intradaySummary.fetchedAt) }}</small>
              </div>
            </template>
            <div ref="intradayChartRef" class="intraday-chart"></div>
          </el-card>
        </template>

        <el-empty
          v-if="!intradayLoading && !intradayError && !selectedCandidate"
          description="暂无可展示的分时分析"
        />
      </div>
    </el-card>

    <el-card class="view-card candidate-card">
      <template #header>
        <div class="section-header">
          <div>
            <p class="section-kicker">候选股票</p>
            <h3 class="section-title">综合评分前列</h3>
          </div>
          <p class="section-note">
            第二阶段增加了质量分、风险级别和分时入口，便于快速判断“能不能上”和“现在强不强”。
          </p>
        </div>
      </template>

      <div v-loading="loading" element-loading-text="正在加载候选股票..." class="loading-container">
        <div v-if="!error && candidates.length > 0" class="table-wrapper">
          <el-table
            :data="candidates"
            border
            class="market-table"
            height="calc(100vh - 520px)"
            max-height="calc(100vh - 520px)"
            row-key="stockCode"
          >
            <el-table-column label="操作" width="168" fixed="left">
              <template #default="scope">
                <div class="action-group">
                  <el-button size="small" type="primary" @click="goToStockReal(scope.row)">看盘口</el-button>
                  <el-button size="small" type="default" @click="handleSelectCandidate(scope.row)">分时</el-button>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="stockCode" label="代码" width="110" />
            <el-table-column prop="stockName" label="名称" width="130" />
            <el-table-column label="机会评分" width="116">
              <template #default="scope">
                <span :class="['score-chip', scoreToneClass(scope.row.score)]">
                  {{ formatScore(scope.row.score) }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="质量分" width="106">
              <template #default="scope">
                <span :class="['score-chip', qualityToneClass(scope.row.qualityScore)]">
                  {{ formatScore(scope.row.qualityScore) }}
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="attentionLevel" label="关注级别" width="110" />
            <el-table-column prop="riskLevel" label="风险级别" width="96" />
            <el-table-column label="涨跌幅" width="110">
              <template #default="scope">
                <span :class="scope.row.changePct >= 0 ? 'rise' : 'fall'">
                  {{ formatSignedPercent(scope.row.changePct) }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="主力净流入" width="150">
              <template #default="scope">
                <span :class="scope.row.netInflow >= 0 ? 'rise' : 'fall'">
                  {{ formatAmount(scope.row.netInflow) }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="换手率" width="100">
              <template #default="scope">{{ formatPercent(scope.row.turnoverRatePct) }}</template>
            </el-table-column>
            <el-table-column label="量比" width="96">
              <template #default="scope">{{ formatNumber(scope.row.volumeRatio) }}</template>
            </el-table-column>
            <el-table-column prop="industryName" label="行业" width="140" />
            <el-table-column label="评分拆解" width="260">
              <template #default="scope">
                <div class="score-breakdown">
                  <span>趋势 {{ scope.row.scoreDetail.trend }}</span>
                  <span>资金 {{ scope.row.scoreDetail.capital }}</span>
                  <span>行业 {{ scope.row.scoreDetail.sector }}</span>
                  <span>流动 {{ scope.row.scoreDetail.liquidity }}</span>
                  <span>质量 {{ scope.row.scoreDetail.quality }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="风险标签" min-width="220">
              <template #default="scope">
                <div class="tag-list">
                  <el-tag
                    v-for="tag in scope.row.riskTags"
                    :key="tag"
                    type="danger"
                    effect="plain"
                    size="small"
                  >
                    {{ tag }}
                  </el-tag>
                  <span v-if="scope.row.riskTags.length === 0" class="muted-text">低风险</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="入选理由" min-width="260">
              <template #default="scope">
                <div class="tag-list">
                  <el-tag
                    v-for="reason in scope.row.reasons"
                    :key="reason"
                    effect="plain"
                    size="small"
                  >
                    {{ reason }}
                  </el-tag>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <el-empty
          v-if="!loading && !error && candidates.length === 0"
          description="暂无候选股票分析结果"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'
import * as echarts from 'echarts'
import { formatFetchedAt } from '../utils/responseMeta'

type Summary = {
  analysisGeneratedAt?: string
  marketSentimentScore?: number
  marketTone?: string
  stockPoolReferenceFetchedAt?: string
  stockPoolOldestFetchedAt?: string
  stockPoolLatestFetchedAt?: string
  industryBaseFetchedAt?: string
  marketIndexFetchedAt?: string
  totalStocks?: number
  candidateCount?: number
  pageCount?: number
  missingPageCount?: number
  advanceCount?: number
  declineCount?: number
  flatCount?: number
  limitUpCount?: number
  limitDownCount?: number
  averageChangePct?: number
  advanceRatio?: number
}

type MarketIndex = {
  label: string
  code: string
  latestPrice: number
  changePct: number
  changeAmount: number
  open: number
  high: number
  low: number
  prevClose: number
  amount: number
}

type IndustrySnapshot = {
  industryCode: string
  industryName: string
  changePct: number
  netInflow: number
  advanceCount: number
  declineCount: number
  flatCount: number
  advanceRatio: number
  leaderName: string
  leaderCode: string
}

type Candidate = {
  stockCode: string
  market?: number
  stockName: string
  industryName: string
  latestPrice: number
  changePct: number
  changeAmount: number
  dealAmount: number
  turnoverRatePct: number
  volumeRatio: number
  netInflow: number
  marketCap: number
  peTtm: number
  score: number
  qualityScore: number
  riskLevel: string
  riskTags: string[]
  scoreDetail: {
    trend: number
    capital: number
    sector: number
    liquidity: number
    quality: number
    riskPenalty: number
  }
  sectorChangePct: number
  reasons: string[]
  attentionLevel: string
}

type AnalysisResponse = {
  summary?: Summary
  indices?: MarketIndex[]
  topIndustries?: IndustrySnapshot[]
  candidates?: Candidate[]
  warnings?: string[]
}

type IntradaySummary = {
  stockCode?: string
  market?: number
  name?: string
  date?: string
  fetchedAt?: string
  preClose?: number
  openPrice?: number
  latestPrice?: number
  highPrice?: number
  lowPrice?: number
  averagePrice?: number
  dayChangePct?: number
  fromOpenPct?: number
  amplitudePct?: number
  highPullbackPct?: number
  vwapPremiumPct?: number
  closeStrengthPct?: number
  last30MinChangePct?: number
  last60MinChangePct?: number
  last30MinVolumeRatio?: number
  afternoonChangePct?: number
  sessionTone?: string
}

type IntradayPoint = {
  time: string
  closePrice: number
  averagePrice: number
  volume: number
  amount: number
}

type IntradayResponse = {
  summary?: IntradaySummary
  positiveSignals?: string[]
  riskTags?: string[]
  points?: IntradayPoint[]
}

const router = useRouter()
const loading = ref(false)
const error = ref('')
const summary = ref<Summary | null>(null)
const indices = ref<MarketIndex[]>([])
const topIndustries = ref<IndustrySnapshot[]>([])
const candidates = ref<Candidate[]>([])
const warnings = ref<string[]>([])

const intradayLoading = ref(false)
const intradayError = ref('')
const selectedCandidate = ref<Candidate | null>(null)
const intradaySummary = ref<IntradaySummary | null>(null)
const intradaySignals = ref<string[]>([])
const intradayRiskTags = ref<string[]>([])
const intradayPoints = ref<IntradayPoint[]>([])
const intradayChartRef = ref<HTMLElement>()

const stockPoolSnapshotLabel = computed(() => {
  if (!summary.value?.stockPoolOldestFetchedAt && !summary.value?.stockPoolLatestFetchedAt) {
    return '--'
  }
  const start = formatFetchedAt(summary.value?.stockPoolOldestFetchedAt)
  const end = formatFetchedAt(summary.value?.stockPoolLatestFetchedAt)
  return start === end ? start : `${start} ~ ${end}`
})

const analysisMetrics = computed(() => {
  const current = summary.value
  if (!current) return []

  const strongestCandidate = candidates.value[0]
  const riseCount = current.advanceCount ?? 0
  const fallCount = current.declineCount ?? 0

  return [
    {
      label: '市场情绪',
      value: `${formatScore(current.marketSentimentScore)} / 100`,
      note: `当前市场状态：${current.marketTone || '--'}`,
      tone: scoreToneClass(current.marketSentimentScore ?? 0),
      compact: true
    },
    {
      label: '上涨 / 下跌',
      value: `${riseCount} / ${fallCount}`,
      note: `平盘 ${current.flatCount ?? 0} 家`,
      tone: riseCount >= fallCount ? 'rise' : 'fall',
      compact: true
    },
    {
      label: '涨停 / 跌停',
      value: `${current.limitUpCount ?? 0} / ${current.limitDownCount ?? 0}`,
      note: `上涨占比 ${formatPercent(current.advanceRatio)}`,
      tone: (current.limitUpCount ?? 0) >= (current.limitDownCount ?? 0) ? 'rise' : 'fall',
      compact: true
    },
    {
      label: '榜首候选',
      value: strongestCandidate?.stockName || '--',
      note: strongestCandidate ? `机会分 ${formatScore(strongestCandidate.score)}` : '暂无候选股票',
      tone: strongestCandidate && strongestCandidate.score >= 80 ? 'rise' : '',
      compact: true
    }
  ]
})

const intradayMetrics = computed(() => {
  const current = intradaySummary.value
  if (!current) return []

  return [
    {
      label: '日内涨跌幅',
      value: formatSignedPercent(current.dayChangePct),
      note: `开盘后 ${formatSignedPercent(current.fromOpenPct)}`,
      tone: (current.dayChangePct ?? 0) >= 0 ? 'rise' : 'fall',
      compact: true
    },
    {
      label: '均价偏离',
      value: formatSignedPercent(current.vwapPremiumPct),
      note: `当前均价 ${formatNumber(current.averagePrice)}`,
      tone: (current.vwapPremiumPct ?? 0) >= 0 ? 'rise' : 'fall',
      compact: true
    },
    {
      label: '尾盘变化',
      value: formatSignedPercent(current.last30MinChangePct),
      note: `近60分钟 ${formatSignedPercent(current.last60MinChangePct)}`,
      tone: (current.last30MinChangePct ?? 0) >= 0 ? 'rise' : 'fall',
      compact: true
    },
    {
      label: '收盘位置强度',
      value: formatPercent(current.closeStrengthPct),
      note: `冲高回落 ${formatSignedPercent(current.highPullbackPct)}`,
      tone: (current.closeStrengthPct ?? 0) >= 60 ? 'rise' : (current.closeStrengthPct ?? 0) <= 35 ? 'fall' : '',
      compact: true
    }
  ]
})

const mergedRiskTags = computed(() => {
  const tags = [...(selectedCandidate.value?.riskTags || []), ...intradayRiskTags.value]
  return Array.from(new Set(tags))
})

const formatNumber = (value: number | string | undefined | null): string => {
  if (value === undefined || value === null || value === '') return '--'
  const num = Number(value)
  return Number.isNaN(num) ? '--' : num.toFixed(2)
}

const formatScore = (value: number | string | undefined | null): string => {
  if (value === undefined || value === null || value === '') return '--'
  const num = Number(value)
  return Number.isNaN(num) ? '--' : num.toFixed(1)
}

const formatPercent = (value: number | string | undefined | null): string => {
  if (value === undefined || value === null || value === '') return '--'
  const num = Number(value)
  return Number.isNaN(num) ? '--' : `${num.toFixed(2)}%`
}

const formatSignedPercent = (value: number | string | undefined | null): string => {
  if (value === undefined || value === null || value === '') return '--'
  const num = Number(value)
  if (Number.isNaN(num)) return '--'
  return `${num >= 0 ? '+' : ''}${num.toFixed(2)}%`
}

const formatAmount = (value: number | string | undefined | null): string => {
  if (value === undefined || value === null || value === '') return '--'
  const num = Number(value)
  if (Number.isNaN(num)) return '--'
  if (Math.abs(num) >= 100000000) return `${(num / 100000000).toFixed(2)}亿`
  if (Math.abs(num) >= 10000) return `${(num / 10000).toFixed(2)}万`
  return num.toFixed(0)
}

const scoreToneClass = (score: number): string => {
  if (score >= 80) return 'rise'
  if (score >= 65) return ''
  return 'fall'
}

const qualityToneClass = (score: number): string => {
  if (score >= 75) return 'rise'
  if (score >= 55) return ''
  return 'fall'
}

const ensureIntradayChart = () => {
  if (!intradayChartRef.value) return null
  return echarts.getInstanceByDom(intradayChartRef.value) || echarts.init(intradayChartRef.value)
}

const renderIntradayChart = async () => {
  await nextTick()
  const chart = ensureIntradayChart()
  if (!chart) return

  chart.setOption({
    animation: false,
    tooltip: { trigger: 'axis' },
    legend: { top: 0, textStyle: { color: '#475569' } },
    grid: { left: '4%', right: '3%', top: 38, bottom: 44, containLabel: true },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: intradayPoints.value.map((item) => item.time),
      axisLabel: { color: '#64748b' }
    },
    yAxis: {
      type: 'value',
      axisLabel: { color: '#64748b' },
      splitLine: { lineStyle: { color: 'rgba(148, 163, 184, 0.18)' } }
    },
    series: [
      {
        name: '价格',
        type: 'line',
        smooth: true,
        showSymbol: false,
        data: intradayPoints.value.map((item) => item.closePrice),
        lineStyle: { color: '#d64b43', width: 2 }
      },
      {
        name: '均价',
        type: 'line',
        smooth: true,
        showSymbol: false,
        data: intradayPoints.value.map((item) => item.averagePrice),
        lineStyle: { color: '#315fce', width: 2 }
      }
    ]
  })
}

const disposeIntradayChart = () => {
  if (intradayChartRef.value) {
    echarts.getInstanceByDom(intradayChartRef.value)?.dispose()
  }
}

const resizeIntradayChart = () => {
  if (intradayChartRef.value) {
    echarts.getInstanceByDom(intradayChartRef.value)?.resize()
  }
}

const goToStockReal = (row: Candidate) => {
  router.push({
    path: '/stock-real',
    query: {
      stockCode: row.stockCode,
      ...(row.market !== undefined ? { market: String(row.market) } : {})
    }
  })
}

const fetchIntradayDetail = async (candidate: Candidate) => {
  selectedCandidate.value = candidate
  intradayLoading.value = true
  intradayError.value = ''

  try {
    const response = await axios.get<IntradayResponse>('/api/analysis/intraday', {
      params: {
        stockCode: candidate.stockCode,
        ...(candidate.market !== undefined ? { market: candidate.market } : {})
      }
    })
    intradaySummary.value = response.data.summary || null
    intradaySignals.value = response.data.positiveSignals || []
    intradayRiskTags.value = response.data.riskTags || []
    intradayPoints.value = response.data.points || []
    await renderIntradayChart()
  } catch (requestError: any) {
    console.error('获取分时分析失败:', requestError)
    intradayError.value = requestError?.response?.data?.message || '获取分时分析失败，请稍后重试'
    intradaySummary.value = null
    intradaySignals.value = []
    intradayRiskTags.value = []
    intradayPoints.value = []
    disposeIntradayChart()
  } finally {
    intradayLoading.value = false
  }
}

const handleSelectCandidate = (candidate: Candidate) => {
  void fetchIntradayDetail(candidate)
}

const fetchAnalysisData = async () => {
  loading.value = true
  error.value = ''

  try {
    const response = await axios.get<AnalysisResponse>('/api/analysis/opportunities?limit=30')
    const payload = response.data || {}
    summary.value = payload.summary || null
    indices.value = payload.indices || []
    topIndustries.value = payload.topIndustries || []
    candidates.value = payload.candidates || []
    warnings.value = payload.warnings || []

    if (candidates.value.length > 0) {
      await fetchIntradayDetail(candidates.value[0]!)
    } else {
      selectedCandidate.value = null
      intradaySummary.value = null
      intradaySignals.value = []
      intradayRiskTags.value = []
      intradayPoints.value = []
      disposeIntradayChart()
    }
  } catch (requestError: any) {
    console.error('获取实时选股分析失败:', requestError)
    error.value = requestError?.response?.data?.message || '获取实时选股分析失败，请稍后重试'
    summary.value = null
    indices.value = []
    topIndustries.value = []
    candidates.value = []
    warnings.value = []
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  void fetchAnalysisData()
  window.addEventListener('resize', resizeIntradayChart)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeIntradayChart)
  disposeIntradayChart()
})
</script>

<style scoped>
.stock-analysis-container {
  min-height: 100%;
}

.overview-card,
.intraday-card,
.candidate-card {
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.loading-container {
  min-height: 320px;
}

.warning-stack {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.error-alert {
  margin-bottom: 18px;
}

.snapshot-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
  margin-bottom: 18px;
}

.snapshot-item {
  padding: 16px 18px;
}

.snapshot-label {
  margin: 0 0 8px;
  color: var(--text-secondary);
  font-size: 12px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.snapshot-item strong {
  font-size: 16px;
  color: var(--text-primary);
}

.index-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
  margin-bottom: 18px;
}

.index-card {
  padding: 18px;
}

.index-card-header,
.card-title-row,
.intraday-topbar {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.index-label,
.muted-text {
  color: var(--text-secondary);
}

.index-label {
  margin: 0 0 8px;
  font-size: 13px;
}

.index-price {
  font-size: 28px;
  line-height: 1;
}

.index-change {
  align-self: flex-start;
  font-size: 18px;
  font-weight: 700;
}

.index-card-meta {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px 12px;
  margin-top: 14px;
  color: var(--text-secondary);
  font-size: 12px;
}

.overview-row {
  margin-top: 4px;
}

.industry-card,
.overview-card-inner {
  min-height: 100%;
}

.card-title-row {
  align-items: center;
  font-weight: 700;
  color: var(--text-primary);
}

.card-title-row small {
  color: var(--text-secondary);
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.industry-list,
.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.industry-list {
  flex-direction: column;
  gap: 12px;
}

.industry-item {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 0;
  border-bottom: 1px solid rgba(24, 34, 53, 0.08);
}

.industry-item:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.industry-name,
.intraday-title {
  margin: 0;
  font-weight: 700;
}

.industry-meta,
.intraday-subtitle {
  margin: 6px 0 0;
  color: var(--text-secondary);
  font-size: 13px;
}

.industry-stats {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 6px;
}

.breadth-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.breadth-item {
  padding: 14px 16px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.68);
}

.breadth-item span {
  display: block;
  margin-bottom: 10px;
  color: var(--text-secondary);
  font-size: 13px;
}

.breadth-item strong {
  font-size: 24px;
}

.intraday-topbar,
.chart-shell {
  padding: 16px 18px;
}

.intraday-metric-grid,
.signal-grid {
  margin-top: 16px;
}

.signal-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.signal-card {
  min-height: 100%;
}

.intraday-chart {
  width: 100%;
  height: 340px;
}

.table-wrapper {
  min-height: 0;
}

.action-group {
  display: flex;
  gap: 6px;
}

.score-chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 74px;
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(24, 34, 53, 0.08);
  font-weight: 700;
}

.score-breakdown {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 6px 10px;
  color: var(--text-secondary);
  font-size: 12px;
}

@media (max-width: 1200px) {
  .index-grid,
  .signal-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .snapshot-grid,
  .index-grid,
  .breadth-grid,
  .signal-grid {
    grid-template-columns: 1fr;
  }

  .industry-item,
  .intraday-topbar,
  .action-group {
    flex-direction: column;
  }

  .industry-stats {
    align-items: flex-start;
  }
}
</style>
