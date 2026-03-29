<template>
  <div class="page-shell stock-analysis-page">
    <section class="page-hero">
      <div>
        <p class="page-kicker">实时选股</p>
        <h2 class="page-title">第三阶段趋势分析</h2>
        <p class="page-subtitle">
          在原有机会评分上，加入盘口结构分和日线技术分，让已抓到的 `stock-real`、`stock-kline`
          真正参与候选排序。
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
          <span class="hero-badge-label">第三阶段覆盖</span>
          <strong>{{ stage3CoverageLabel }}</strong>
        </div>
        <el-button type="primary" @click="fetchAnalysisData">刷新分析</el-button>
      </div>
    </section>

    <section v-if="summary" class="metric-grid">
      <article v-for="item in analysisMetrics" :key="item.label" class="metric-card">
        <p class="metric-label">{{ item.label }}</p>
        <p :class="['metric-value', item.tone, item.compact ? 'metric-value--compact' : '']">
          {{ item.value }}
        </p>
        <p class="metric-note">{{ item.note }}</p>
      </article>
    </section>

    <div v-if="warnings.length > 0" class="warning-stack">
      <el-alert
        v-for="warning in warnings"
        :key="warning"
        :title="warning"
        type="warning"
        show-icon
        :closable="false"
      />
    </div>

    <el-card class="view-card">
      <template #header>
        <div class="section-header">
          <div>
            <p class="section-kicker">市场环境</p>
            <h3 class="section-title">大盘与板块概览</h3>
          </div>
          <p class="section-note">分析优先读本地股票池、行业快照和指数缓存。</p>
        </div>
      </template>

      <div v-loading="loading" element-loading-text="正在生成分析..." class="loading-container">
        <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" class="error-alert" />

        <template v-if="!error && summary">
          <div class="snapshot-grid">
            <div class="snapshot-item market-subcard">
              <p class="snapshot-label">分析生成</p>
              <strong>{{ formatFetchedAt(summary.analysisGeneratedAt) }}</strong>
            </div>
            <div class="snapshot-item market-subcard">
              <p class="snapshot-label">股票池快照</p>
              <strong>{{ stockPoolSnapshotLabel }}</strong>
            </div>
            <div class="snapshot-item market-subcard">
              <p class="snapshot-label">行业快照</p>
              <strong>{{ formatFetchedAt(summary.industryBaseFetchedAt) }}</strong>
            </div>
          </div>

          <div class="index-grid">
            <article v-for="indexItem in indices" :key="indexItem.code" class="index-card market-subcard">
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

          <div class="panel-grid">
            <el-card class="market-subcard" shadow="never">
              <template #header>
                <div class="card-title-row">
                  <span>热点行业</span>
                  <small>按涨幅和净流入排序</small>
                </div>
              </template>

              <div class="industry-list">
                <div v-for="industry in topIndustries" :key="industry.industryCode" class="industry-item">
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

            <el-card class="market-subcard" shadow="never">
              <template #header>
                <div class="card-title-row">
                  <span>市场宽度</span>
                  <small>看当前整体强弱</small>
                </div>
              </template>

              <div class="breadth-grid">
                <div class="breadth-item">
                  <span>上涨家数</span>
                  <strong class="rise">{{ summary.advanceCount ?? 0 }}</strong>
                </div>
                <div class="breadth-item">
                  <span>下跌家数</span>
                  <strong class="fall">{{ summary.declineCount ?? 0 }}</strong>
                </div>
                <div class="breadth-item">
                  <span>涨停数量</span>
                  <strong class="rise">{{ summary.limitUpCount ?? 0 }}</strong>
                </div>
                <div class="breadth-item">
                  <span>跌停数量</span>
                  <strong class="fall">{{ summary.limitDownCount ?? 0 }}</strong>
                </div>
              </div>
            </el-card>
          </div>
        </template>
      </div>
    </el-card>

    <el-card class="view-card">
      <template #header>
        <div class="section-header">
          <div>
            <p class="section-kicker">分时观察</p>
            <h3 class="section-title">当前候选分时状态</h3>
          </div>
          <p class="section-note">默认展示当前榜首股票，可在下表切换。</p>
        </div>
      </template>

      <div v-loading="intradayLoading" element-loading-text="正在加载分时..." class="loading-container">
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
              <p class="industry-name">{{ selectedCandidate.stockName }} {{ selectedCandidate.stockCode }}</p>
              <p class="industry-meta">{{ selectedCandidate.industryName || '未分类行业' }} / {{ intradaySummary.sessionTone }}</p>
            </div>
            <el-button @click="goToStockReal(selectedCandidate)">看盘口</el-button>
          </div>

          <section class="metric-grid intraday-metric-grid">
            <article v-for="item in intradayMetrics" :key="item.label" class="metric-card">
              <p class="metric-label">{{ item.label }}</p>
              <p :class="['metric-value', item.tone, item.compact ? 'metric-value--compact' : '']">
                {{ item.value }}
              </p>
              <p class="metric-note">{{ item.note }}</p>
            </article>
          </section>

          <div class="signal-grid">
            <el-card class="market-subcard" shadow="never">
              <template #header>
                <div class="card-title-row">
                  <span>积极信号</span>
                  <small>分时维度</small>
                </div>
              </template>
              <div class="tag-list">
                <el-tag v-for="signal in intradaySignals" :key="signal" type="success" effect="plain">{{ signal }}</el-tag>
                <span v-if="intradaySignals.length === 0" class="muted-text">暂无明显积极信号</span>
              </div>
            </el-card>

            <el-card class="market-subcard" shadow="never">
              <template #header>
                <div class="card-title-row">
                  <span>风险提醒</span>
                  <small>候选风险 + 分时风险</small>
                </div>
              </template>
              <div class="tag-list">
                <el-tag v-for="tag in mergedRiskTags" :key="tag" type="danger" effect="plain">{{ tag }}</el-tag>
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

        <el-empty v-if="!intradayLoading && !intradayError && !selectedCandidate" description="暂无可展示的分时分析" />
      </div>
    </el-card>

    <el-card class="view-card">
      <template #header>
        <div class="section-header">
          <div>
            <p class="section-kicker">候选股票</p>
            <h3 class="section-title">综合评分前列</h3>
          </div>
          <p class="section-note">第三阶段新增盘口分、技术分和对应抓取时间。</p>
        </div>
      </template>

      <div v-loading="loading" element-loading-text="正在加载候选..." class="loading-container">
        <el-table v-if="!error && candidates.length > 0" :data="candidates" border class="market-table" height="560" row-key="stockCode">
          <el-table-column label="操作" width="150" fixed="left">
            <template #default="{ row }">
              <div class="action-group">
                <el-button size="small" type="primary" @click="goToStockReal(row)">盘口</el-button>
                <el-button size="small" @click="handleSelectCandidate(row)">分时</el-button>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="stockCode" label="代码" width="96" />
          <el-table-column prop="stockName" label="名称" width="120" />
          <el-table-column prop="industryName" label="行业" width="140" />
          <el-table-column label="机会分" width="90">
            <template #default="{ row }"><span :class="['score-chip', scoreToneClass(row.score)]">{{ formatScore(row.score) }}</span></template>
          </el-table-column>
          <el-table-column label="质量分" width="90">
            <template #default="{ row }"><span :class="['score-chip', qualityToneClass(row.qualityScore)]">{{ formatScore(row.qualityScore) }}</span></template>
          </el-table-column>
          <el-table-column label="盘口分" width="90">
            <template #default="{ row }"><span :class="['score-chip', scoreToneClass(row.scoreDetail.quote)]">{{ formatScore(row.scoreDetail.quote) }}</span></template>
          </el-table-column>
          <el-table-column label="技术分" width="90">
            <template #default="{ row }"><span :class="['score-chip', scoreToneClass(row.scoreDetail.technical)]">{{ formatScore(row.scoreDetail.technical) }}</span></template>
          </el-table-column>
          <el-table-column prop="attentionLevel" label="关注级别" width="110" />
          <el-table-column prop="riskLevel" label="风险级别" width="96" />
          <el-table-column label="涨跌幅" width="100">
            <template #default="{ row }"><span :class="row.changePct >= 0 ? 'rise' : 'fall'">{{ formatSignedPercent(row.changePct) }}</span></template>
          </el-table-column>
          <el-table-column label="主力净流入" width="150">
            <template #default="{ row }"><span :class="row.netInflow >= 0 ? 'rise' : 'fall'">{{ formatAmount(row.netInflow) }}</span></template>
          </el-table-column>
          <el-table-column label="换手率" width="90">
            <template #default="{ row }">{{ formatPercent(row.turnoverRatePct) }}</template>
          </el-table-column>
          <el-table-column label="量比" width="80">
            <template #default="{ row }">{{ formatNumber(row.volumeRatio) }}</template>
          </el-table-column>
          <el-table-column label="盘口抓取" width="170">
            <template #default="{ row }">{{ formatFetchedAt(row.quoteFetchedAt) }}</template>
          </el-table-column>
          <el-table-column label="日线抓取" width="170">
            <template #default="{ row }">{{ formatFetchedAt(row.klineFetchedAt) }}</template>
          </el-table-column>
          <el-table-column label="评分拆解" min-width="220">
            <template #default="{ row }">
              <div class="score-breakdown">
                <span>趋势 {{ row.scoreDetail.trend }}</span>
                <span>资金 {{ row.scoreDetail.capital }}</span>
                <span>行业 {{ row.scoreDetail.sector }}</span>
                <span>流动 {{ row.scoreDetail.liquidity }}</span>
                <span>质量 {{ row.scoreDetail.quality }}</span>
                <span>盘口 {{ row.scoreDetail.quote }}</span>
                <span>技术 {{ row.scoreDetail.technical }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="风险标签" min-width="220">
            <template #default="{ row }">
              <div class="tag-list">
                <el-tag v-for="tag in row.riskTags" :key="tag" type="danger" effect="plain" size="small">{{ tag }}</el-tag>
                <span v-if="row.riskTags.length === 0" class="muted-text">低风险</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="入选理由" min-width="260">
            <template #default="{ row }">
              <div class="tag-list">
                <el-tag v-for="reason in row.reasons" :key="reason" effect="plain" size="small">{{ reason }}</el-tag>
              </div>
            </template>
          </el-table-column>
        </el-table>

        <el-empty v-if="!loading && !error && candidates.length === 0" description="暂无候选分析结果" />
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
  stockPoolOldestFetchedAt?: string
  stockPoolLatestFetchedAt?: string
  industryBaseFetchedAt?: string
  advanceCount?: number
  declineCount?: number
  flatCount?: number
  limitUpCount?: number
  limitDownCount?: number
  stage3PreselectCount?: number
  stage3QuoteCoverageCount?: number
  stage3KlineCoverageCount?: number
  stage3QuoteCoverageRatio?: number
  stage3KlineCoverageRatio?: number
}

type MarketIndex = { label: string; code: string; latestPrice: number; changePct: number; open: number; high: number; low: number; amount: number }
type IndustrySnapshot = { industryCode: string; industryName: string; changePct: number; netInflow: number; advanceRatio: number; leaderName: string }
type Candidate = {
  stockCode: string
  market?: number
  stockName: string
  industryName: string
  score: number
  qualityScore: number
  changePct: number
  turnoverRatePct: number
  volumeRatio: number
  netInflow: number
  quoteFetchedAt?: string
  klineFetchedAt?: string
  riskLevel: string
  riskTags: string[]
  reasons: string[]
  attentionLevel: string
  scoreDetail: { trend: number; capital: number; sector: number; liquidity: number; quality: number; quote: number; technical: number }
}
type AnalysisResponse = { summary?: Summary; indices?: MarketIndex[]; topIndustries?: IndustrySnapshot[]; candidates?: Candidate[]; warnings?: string[] }
type IntradaySummary = { fetchedAt?: string; dayChangePct?: number; fromOpenPct?: number; vwapPremiumPct?: number; averagePrice?: number; last30MinChangePct?: number; last60MinChangePct?: number; closeStrengthPct?: number; highPullbackPct?: number; sessionTone?: string }
type IntradayPoint = { time: string; closePrice: number; averagePrice: number }
type IntradayResponse = { summary?: IntradaySummary; positiveSignals?: string[]; riskTags?: string[]; points?: IntradayPoint[] }

const router = useRouter()
const loading = ref(false)
const error = ref('')
const intradayLoading = ref(false)
const intradayError = ref('')
const summary = ref<Summary | null>(null)
const indices = ref<MarketIndex[]>([])
const topIndustries = ref<IndustrySnapshot[]>([])
const candidates = ref<Candidate[]>([])
const warnings = ref<string[]>([])
const selectedCandidate = ref<Candidate | null>(null)
const intradaySummary = ref<IntradaySummary | null>(null)
const intradaySignals = ref<string[]>([])
const intradayRiskTags = ref<string[]>([])
const intradayPoints = ref<IntradayPoint[]>([])
const intradayChartRef = ref<HTMLElement>()

const stockPoolSnapshotLabel = computed(() => {
  const start = formatFetchedAt(summary.value?.stockPoolOldestFetchedAt)
  const end = formatFetchedAt(summary.value?.stockPoolLatestFetchedAt)
  return start === '--' && end === '--' ? '--' : start === end ? start : `${start} ~ ${end}`
})

const stage3CoverageLabel = computed(() => {
  const current = summary.value
  if (!current?.stage3PreselectCount) return '--'
  return `${current.stage3QuoteCoverageCount ?? 0}/${current.stage3PreselectCount} 盘口`
})

const analysisMetrics = computed(() => {
  const current = summary.value
  const first = candidates.value[0]
  if (!current) return []
  return [
    { label: '市场情绪', value: `${formatScore(current.marketSentimentScore)} / 100`, note: `当前市场：${current.marketTone || '--'}`, tone: scoreToneClass(current.marketSentimentScore ?? 0), compact: true },
    { label: '上涨 / 下跌', value: `${current.advanceCount ?? 0} / ${current.declineCount ?? 0}`, note: `平盘 ${current.flatCount ?? 0} 家`, tone: (current.advanceCount ?? 0) >= (current.declineCount ?? 0) ? 'rise' : 'fall', compact: true },
    { label: '涨停 / 跌停', value: `${current.limitUpCount ?? 0} / ${current.limitDownCount ?? 0}`, note: `板块快照 ${formatFetchedAt(current.industryBaseFetchedAt)}`, tone: (current.limitUpCount ?? 0) >= (current.limitDownCount ?? 0) ? 'rise' : 'fall', compact: true },
    { label: '盘口覆盖率', value: formatPercent(current.stage3QuoteCoverageRatio), note: `日线覆盖率 ${formatPercent(current.stage3KlineCoverageRatio)}`, tone: (current.stage3QuoteCoverageRatio ?? 0) >= 70 ? 'rise' : '', compact: true },
    { label: '榜首候选', value: first?.stockName || '--', note: first ? `机会分 ${formatScore(first.score)}` : '暂无候选股票', tone: first && first.score >= 80 ? 'rise' : '', compact: true }
  ]
})

const intradayMetrics = computed(() => {
  const current = intradaySummary.value
  if (!current) return []
  return [
    { label: '日内涨跌幅', value: formatSignedPercent(current.dayChangePct), note: `开盘后 ${formatSignedPercent(current.fromOpenPct)}`, tone: (current.dayChangePct ?? 0) >= 0 ? 'rise' : 'fall', compact: true },
    { label: '均价偏离', value: formatSignedPercent(current.vwapPremiumPct), note: `当前均价 ${formatNumber(current.averagePrice)}`, tone: (current.vwapPremiumPct ?? 0) >= 0 ? 'rise' : 'fall', compact: true },
    { label: '尾盘变化', value: formatSignedPercent(current.last30MinChangePct), note: `近60分钟 ${formatSignedPercent(current.last60MinChangePct)}`, tone: (current.last30MinChangePct ?? 0) >= 0 ? 'rise' : 'fall', compact: true },
    { label: '收盘强度', value: formatPercent(current.closeStrengthPct), note: `冲高回落 ${formatSignedPercent(current.highPullbackPct)}`, tone: (current.closeStrengthPct ?? 0) >= 60 ? 'rise' : '', compact: true }
  ]
})

const mergedRiskTags = computed(() => Array.from(new Set([...(selectedCandidate.value?.riskTags || []), ...intradayRiskTags.value])))

const formatNumber = (value: number | string | undefined | null) => value === undefined || value === null || value === '' ? '--' : Number(value).toFixed(2)
const formatScore = (value: number | string | undefined | null) => value === undefined || value === null || value === '' ? '--' : Number(value).toFixed(1)
const formatPercent = (value: number | string | undefined | null) => value === undefined || value === null || value === '' ? '--' : `${Number(value).toFixed(2)}%`
const formatSignedPercent = (value: number | string | undefined | null) => value === undefined || value === null || value === '' ? '--' : `${Number(value) >= 0 ? '+' : ''}${Number(value).toFixed(2)}%`
const formatAmount = (value: number | string | undefined | null) => {
  if (value === undefined || value === null || value === '') return '--'
  const num = Number(value)
  if (Math.abs(num) >= 100000000) return `${(num / 100000000).toFixed(2)} 亿`
  if (Math.abs(num) >= 10000) return `${(num / 10000).toFixed(2)} 万`
  return num.toFixed(0)
}
const scoreToneClass = (score: number) => (score >= 80 ? 'rise' : score < 60 ? 'fall' : '')
const qualityToneClass = (score: number) => (score >= 75 ? 'rise' : score < 55 ? 'fall' : '')

const ensureIntradayChart = () => intradayChartRef.value ? echarts.getInstanceByDom(intradayChartRef.value) || echarts.init(intradayChartRef.value) : null
const disposeIntradayChart = () => intradayChartRef.value && echarts.getInstanceByDom(intradayChartRef.value)?.dispose()
const resizeIntradayChart = () => intradayChartRef.value && echarts.getInstanceByDom(intradayChartRef.value)?.resize()

const renderIntradayChart = async () => {
  await nextTick()
  const chart = ensureIntradayChart()
  if (!chart) return
  chart.setOption({
    animation: false,
    tooltip: { trigger: 'axis' },
    legend: { top: 0, textStyle: { color: '#475569' } },
    grid: { left: '4%', right: '3%', top: 38, bottom: 30, containLabel: true },
    xAxis: { type: 'category', boundaryGap: false, data: intradayPoints.value.map((item) => item.time), axisLabel: { color: '#64748b' } },
    yAxis: { type: 'value', axisLabel: { color: '#64748b' }, splitLine: { lineStyle: { color: 'rgba(148, 163, 184, 0.18)' } } },
    series: [
      { name: '价格', type: 'line', smooth: true, showSymbol: false, data: intradayPoints.value.map((item) => item.closePrice), lineStyle: { color: '#d64b43', width: 2 } },
      { name: '均价', type: 'line', smooth: true, showSymbol: false, data: intradayPoints.value.map((item) => item.averagePrice), lineStyle: { color: '#315fce', width: 2 } }
    ]
  })
}

const goToStockReal = (row: Candidate) => router.push({ path: '/stock-real', query: { stockCode: row.stockCode, ...(row.market !== undefined ? { market: String(row.market) } : {}) } })

const fetchIntradayDetail = async (candidate: Candidate) => {
  selectedCandidate.value = candidate
  intradayLoading.value = true
  intradayError.value = ''
  try {
    const response = await axios.get<IntradayResponse>('/api/analysis/intraday', { params: { stockCode: candidate.stockCode, ...(candidate.market !== undefined ? { market: candidate.market } : {}) } })
    intradaySummary.value = response.data.summary || null
    intradaySignals.value = response.data.positiveSignals || []
    intradayRiskTags.value = response.data.riskTags || []
    intradayPoints.value = response.data.points || []
    await renderIntradayChart()
  } catch (requestError: any) {
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

const handleSelectCandidate = (candidate: Candidate) => void fetchIntradayDetail(candidate)

const fetchAnalysisData = async () => {
  loading.value = true
  error.value = ''
  try {
    const response = await axios.get<AnalysisResponse>('/api/analysis/opportunities', { params: { limit: 30 } })
    summary.value = response.data.summary || null
    indices.value = response.data.indices || []
    topIndustries.value = response.data.topIndustries || []
    candidates.value = response.data.candidates || []
    warnings.value = response.data.warnings || []
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
.stock-analysis-page, .loading-container { min-height: 320px; }
.warning-stack, .industry-list { display: flex; flex-direction: column; gap: 10px; }
.snapshot-grid, .index-grid, .panel-grid, .signal-grid { display: grid; gap: 14px; }
.snapshot-grid { grid-template-columns: repeat(3, minmax(0, 1fr)); margin-bottom: 18px; }
.index-grid { grid-template-columns: repeat(4, minmax(0, 1fr)); margin-bottom: 18px; }
.panel-grid, .signal-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
.snapshot-item, .index-card, .intraday-topbar, .chart-shell { padding: 16px 18px; }
.snapshot-label, .index-label, .muted-text, .industry-meta { color: var(--text-secondary); }
.snapshot-label, .card-title-row small { font-size: 12px; letter-spacing: 0.08em; text-transform: uppercase; }
.index-card-header, .card-title-row, .intraday-topbar, .industry-item { display: flex; justify-content: space-between; gap: 12px; }
.index-price { font-size: 28px; line-height: 1; }
.index-change { font-size: 18px; font-weight: 700; }
.index-card-meta, .score-breakdown { display: grid; gap: 6px 10px; color: var(--text-secondary); font-size: 12px; }
.index-card-meta { grid-template-columns: repeat(2, minmax(0, 1fr)); margin-top: 14px; }
.breadth-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 14px; }
.breadth-item { padding: 14px 16px; border-radius: 18px; background: rgba(255, 255, 255, 0.68); }
.breadth-item span { display: block; margin-bottom: 10px; color: var(--text-secondary); font-size: 13px; }
.breadth-item strong { font-size: 24px; }
.industry-item { padding: 14px 0; border-bottom: 1px solid rgba(24, 34, 53, 0.08); }
.industry-item:last-child { border-bottom: none; padding-bottom: 0; }
.industry-name { margin: 0; font-weight: 700; }
.industry-meta { margin: 6px 0 0; font-size: 13px; }
.industry-stats { display: flex; flex-direction: column; align-items: flex-end; gap: 6px; }
.tag-list { display: flex; flex-wrap: wrap; gap: 8px; }
.intraday-metric-grid, .signal-grid { margin-top: 16px; }
.intraday-chart { width: 100%; height: 340px; }
.action-group { display: flex; gap: 6px; }
.score-chip { display: inline-flex; align-items: center; justify-content: center; min-width: 70px; padding: 6px 10px; border-radius: 999px; background: rgba(24, 34, 53, 0.08); font-weight: 700; }
.score-breakdown { grid-template-columns: repeat(2, minmax(0, 1fr)); }
.error-alert { margin-bottom: 18px; }
@media (max-width: 1200px) { .index-grid, .panel-grid, .signal-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
@media (max-width: 768px) {
  .snapshot-grid, .index-grid, .panel-grid, .signal-grid, .breadth-grid { grid-template-columns: 1fr; }
  .intraday-topbar, .industry-item, .action-group { flex-direction: column; }
  .industry-stats { align-items: flex-start; }
}
</style>
