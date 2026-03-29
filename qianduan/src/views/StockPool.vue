<template>
  <div class="page-shell stock-pool-container">
    <section class="page-hero">
      <div>
        <p class="page-kicker">Stock Universe</p>
        <h2 class="page-title">全市场股票池</h2>
        <p class="page-subtitle">
          用一张交易工作表把股票池铺开，适合先筛行业、看概念分布，再跳到单只股票的盘口和 K 线做深看。
        </p>
      </div>

      <div class="page-actions">
        <div class="hero-badge">
          <span class="hero-badge-label">股票总量</span>
          <strong>{{ total || 0 }} 只</strong>
        </div>
        <div class="hero-badge">
          <span class="hero-badge-label">当前页</span>
          <strong>{{ currentPage }} / {{ totalPages }}</strong>
        </div>
        <div class="hero-badge">
          <span class="hero-badge-label">最近刷新</span>
          <strong>{{ lastUpdated }}</strong>
        </div>
        <el-button type="primary" @click="fetchStockPoolData(currentPage)">刷新数据</el-button>
      </div>
    </section>

    <section class="metric-grid">
      <article
        v-for="item in poolStats"
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

    <el-card class="view-card data-card">
      <template #header>
        <div class="section-header">
          <div>
            <p class="section-kicker">Market Tape</p>
            <h3 class="section-title">股票池明细</h3>
          </div>
          <p class="section-note">点击“实盘数据”可继续查看盘口和长周期 K 线。</p>
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
          <el-table
            :data="stockPoolData"
            border
            class="market-table"
            height="calc(100vh - 370px)"
            max-height="calc(100vh - 370px)"
            :row-key="getRowKey"
          >
            <el-table-column label="操作" width="110" fixed="left">
              <template #default="scope">
                <el-button size="small" type="primary" @click="handleRealDataClick(scope.row)">
                  实盘数据
                </el-button>
              </template>
            </el-table-column>
            <el-table-column prop="f12" label="股票代码" width="110" />
            <el-table-column prop="f14" label="名称" width="130" />
            <el-table-column prop="f2" label="最新价" width="110">
              <template #default="scope">
                {{ formatNumber(Number(scope.row.f2) / 100) }}
              </template>
            </el-table-column>
            <el-table-column prop="f3" label="涨跌幅" width="110">
              <template #default="scope">
                <span :class="scope.row.f3 >= 0 ? 'rise' : 'fall'">
                  {{ formatPercent(scope.row.f3) }}
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="f4" label="涨跌额" width="110">
              <template #default="scope">
                <span :class="Number(scope.row.f4) >= 0 ? 'rise' : 'fall'">
                  {{ formatNumber(Number(scope.row.f4) / 100) }}
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="f5" label="成交量(手)" width="130">
              <template #default="scope">
                {{ formatVolume(scope.row.f5) }}
              </template>
            </el-table-column>
            <el-table-column prop="f6" label="成交额" width="130">
              <template #default="scope">
                {{ formatAmount(scope.row.f6) }}
              </template>
            </el-table-column>
            <el-table-column prop="f7" label="振幅" width="110">
              <template #default="scope">
                {{ formatPercent(scope.row.f7) }}
              </template>
            </el-table-column>
            <el-table-column prop="f8" label="换手率" width="110">
              <template #default="scope">
                {{ formatPercent(scope.row.f8) }}
              </template>
            </el-table-column>
            <el-table-column prop="f9" label="市盈率(动)" width="130">
              <template #default="scope">
                {{ formatNumber(scope.row.f9) }}
              </template>
            </el-table-column>
            <el-table-column prop="f10" label="量比" width="110">
              <template #default="scope">
                {{ formatNumber(scope.row.f10) }}
              </template>
            </el-table-column>
            <el-table-column prop="f15" label="最高" width="110">
              <template #default="scope">
                {{ formatNumber(Number(scope.row.f15) / 100) }}
              </template>
            </el-table-column>
            <el-table-column prop="f16" label="最低" width="110">
              <template #default="scope">
                {{ formatNumber(Number(scope.row.f16) / 100) }}
              </template>
            </el-table-column>
            <el-table-column prop="f17" label="今开" width="110">
              <template #default="scope">
                {{ formatNumber(Number(scope.row.f17) / 100) }}
              </template>
            </el-table-column>
            <el-table-column prop="f18" label="昨收" width="110">
              <template #default="scope">
                {{ formatNumber(Number(scope.row.f18) / 100) }}
              </template>
            </el-table-column>
            <el-table-column prop="f20" label="总市值" width="130">
              <template #default="scope">
                {{ formatAmount(scope.row.f20) }}
              </template>
            </el-table-column>
            <el-table-column prop="f21" label="流动市值" width="130">
              <template #default="scope">
                {{ formatAmount(scope.row.f21) }}
              </template>
            </el-table-column>
            <el-table-column prop="f22" label="涨速" width="110">
              <template #default="scope">
                {{ formatPercent(scope.row.f22) }}
              </template>
            </el-table-column>
            <el-table-column prop="f23" label="市净率" width="110">
              <template #default="scope">
                {{ formatNumber(scope.row.f23) }}
              </template>
            </el-table-column>
            <el-table-column prop="f24" label="60日涨跌幅" width="130">
              <template #default="scope">
                <span :class="scope.row.f24 >= 0 ? 'rise' : 'fall'">
                  {{ formatPercent(scope.row.f24) }}
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="f25" label="今年涨跌幅" width="140">
              <template #default="scope">
                <span :class="scope.row.f25 >= 0 ? 'rise' : 'fall'">
                  {{ formatPercent(scope.row.f25) }}
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="f62" label="主力净流入额" width="150">
              <template #default="scope">
                <span :class="scope.row.f62 >= 0 ? 'rise' : 'fall'">
                  {{ formatAmount(scope.row.f62) }}
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="f115" label="市盈率TTM" width="130">
              <template #default="scope">
                {{ formatNumber(scope.row.f115) }}
              </template>
            </el-table-column>
            <el-table-column prop="f100" label="行业板块" width="130" />
            <el-table-column prop="f102" label="地区板块" width="130" />
            <el-table-column prop="f103" label="概念板块" width="300" show-overflow-tooltip />
          </el-table>

          <div class="pagination-container">
            <el-pagination
              v-model:current-page="currentPage"
              v-model:page-size="pageSize"
              :page-sizes="[100]"
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
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'
import { formatFetchedAt, getFetchedAt, parseApiPayload } from '../utils/responseMeta'

const router = useRouter()
const loading = ref(false)
const error = ref('')
const stockPoolData = ref<any[]>([])
const currentPage = ref(1)
const pageSize = ref(100)
const total = ref(0)
const lastUpdated = ref('--:--:--')

const formatNumber = (value: number | string): string => {
  if (value === undefined || value === null || value === '-') return '-'
  const num = Number(value)
  return isNaN(num) ? '-' : num.toFixed(2)
}

const formatPercent = (value: number | string): string => {
  if (value === undefined || value === null || value === '-') return '-'
  const num = Number(value)
  return isNaN(num) ? '-' : (num / 100).toFixed(2) + '%'
}

const formatVolume = (value: number | string): string => {
  if (value === undefined || value === null || value === '-') return '-'
  const num = Number(value)
  if (isNaN(num)) return '-'
  if (num >= 100000000) return (num / 100000000).toFixed(2) + '亿'
  if (num >= 10000) return (num / 10000).toFixed(2) + '万'
  return num.toString()
}

const formatAmount = (value: number | string): string => {
  if (value === undefined || value === null || value === '-') return '-'
  const num = Number(value)
  if (isNaN(num)) return '-'
  if (Math.abs(num) >= 100000000) return (num / 100000000).toFixed(2) + '亿'
  if (Math.abs(num) >= 10000) return (num / 10000).toFixed(2) + '万'
  return num.toString()
}

const totalPages = computed(() => {
  if (!total.value || !pageSize.value) return 1
  return Math.max(1, Math.ceil(total.value / pageSize.value))
})

const poolStats = computed(() => {
  const rows = stockPoolData.value
  const riseCount = rows.filter((item) => Number(item.f3) > 0).length
  const fallCount = rows.filter((item) => Number(item.f3) < 0).length
  const strongest = rows.reduce<any | null>((best, row) => {
    if (!best) return row
    return Number(row.f3) > Number(best.f3) ? row : best
  }, null)
  const netFlow = rows.reduce((sum, row) => sum + Number(row.f62 || 0), 0)

  return [
    {
      label: '本页股票',
      value: String(rows.length),
      note: `当前第 ${currentPage.value} 页展示数量`,
      tone: '',
      compact: false
    },
    {
      label: '上涨 / 下跌',
      value: `${riseCount} / ${fallCount}`,
      note: '便于快速判断池子整体强弱',
      tone: riseCount >= fallCount ? 'rise' : 'fall',
      compact: true
    },
    {
      label: '最强个股',
      value: strongest?.f14 ?? '--',
      note: strongest ? `${formatPercent(strongest.f3)} 涨幅` : '暂无数据',
      tone: Number(strongest?.f3 ?? 0) >= 0 ? 'rise' : 'fall',
      compact: true
    },
    {
      label: '主力净流入',
      value: formatAmount(netFlow),
      note: '当前页合计主力资金',
      tone: netFlow >= 0 ? 'rise' : 'fall',
      compact: false
    }
  ]
})

const handleRealDataClick = (row: any) => {
  if (row && row.f12) {
    goToRealData(row.f12, row.f13)
  } else {
    console.error('当前行数据为空或股票代码不存在')
  }
}

const getRowKey = (row: Record<string, unknown>) => String(row.f12 ?? '')

const goToRealData = (stockCode: any, market?: any) => {
  try {
    if (!stockCode) return
    const code = String(stockCode)
    if (!code) return

    const query: Record<string, string> = { stockCode: code }
    if (market !== undefined && market !== null && market !== '') {
      query.market = String(market)
    }

    router.push({
      path: '/stock-real',
      query
    })
  } catch (jumpError) {
    console.error('跳转失败:', jumpError)
  }
}

const fetchStockPoolData = async (page: number = 1) => {
  loading.value = true
  error.value = ''

  try {
    const response = await axios.get(`/api/stock/pool?pn=${page}`)
    const parsedData = parseApiPayload(response.data)

    if (parsedData && parsedData.data && parsedData.data.diff) {
      const diff = parsedData.data.diff
      if (Array.isArray(diff)) {
        stockPoolData.value = diff
      } else if (typeof diff === 'object') {
        stockPoolData.value = Object.values(diff)
      } else {
        stockPoolData.value = []
      }

      total.value = Number(parsedData.data.total ?? stockPoolData.value.length)
      lastUpdated.value = formatFetchedAt(getFetchedAt(parsedData))
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

const handleCurrentChange = (val: number) => {
  currentPage.value = val
  fetchStockPoolData(val)
}

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
  min-height: 100%;
}

.data-card {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-height: 0;
}

.loading-container {
  flex: 1;
  min-height: 400px;
  overflow: auto;
}

.table-container {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.market-table {
  min-width: 2180px;
}

.pagination-container {
  display: flex;
  justify-content: flex-end;
}

.error-alert {
  margin-bottom: 18px;
}
</style>
