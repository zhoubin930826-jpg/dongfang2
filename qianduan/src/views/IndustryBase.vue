<template>
  <div class="page-shell industry-base-container">
    <section class="page-hero">
      <div>
        <p class="page-kicker">行业监测</p>
        <h2 class="page-title">行业热度全景</h2>
        <p class="page-subtitle">
          先看板块强弱，再判断资金偏好。把行业涨跌、主力净流入和上涨家数放到一个屏幕里，更适合日内扫盘。
        </p>
      </div>

      <div class="page-actions">
        <div class="hero-badge">
          <span class="hero-badge-label">行业数量</span>
          <strong>{{ industryData.length || 0 }} 个</strong>
        </div>
        <div class="hero-badge">
          <span class="hero-badge-label">最近刷新</span>
          <strong>{{ lastUpdated }}</strong>
        </div>
        <el-button type="primary" @click="fetchIndustryBaseData">刷新数据</el-button>
      </div>
    </section>

    <section class="metric-grid">
      <article
        v-for="item in sectorStats"
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
            <p class="section-kicker">行业行情</p>
            <h3 class="section-title">板块行情列表</h3>
          </div>
          <p class="section-note">点击“历史详细”进入行业 K 线视图。</p>
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

        <div class="table-wrapper">
          <el-table
            v-if="!error && industryData.length > 0"
            :data="industryData"
            border
            class="market-table"
          >
            <el-table-column label="操作" width="120" fixed="left">
              <template #default="scope">
                <el-button size="small" type="primary" @click="handleHistoryDetail(scope.row.f12)">
                  历史详细
                </el-button>
              </template>
            </el-table-column>
            <el-table-column prop="f12" label="行业代码" width="110" />
            <el-table-column prop="f14" label="行业名称" width="130" />
            <el-table-column prop="f2" label="收盘价" width="110">
              <template #default="scope">
                {{ formatNumber(scope.row.f2) }}
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
                <span :class="scope.row.f4 >= 0 ? 'rise' : 'fall'">
                  {{ formatNumber(scope.row.f4) }}
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="f5" label="成交量" width="130">
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
            <el-table-column prop="f15" label="最高价" width="110">
              <template #default="scope">
                {{ formatNumber(scope.row.f15) }}
              </template>
            </el-table-column>
            <el-table-column prop="f16" label="最低价" width="110">
              <template #default="scope">
                {{ formatNumber(scope.row.f16) }}
              </template>
            </el-table-column>
            <el-table-column prop="f17" label="今开" width="110">
              <template #default="scope">
                {{ formatNumber(scope.row.f17) }}
              </template>
            </el-table-column>
            <el-table-column prop="f18" label="昨收" width="110">
              <template #default="scope">
                {{ formatNumber(scope.row.f18) }}
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
            <el-table-column prop="f24" label="60日涨幅" width="120">
              <template #default="scope">
                <span :class="scope.row.f24 >= 0 ? 'rise' : 'fall'">
                  {{ formatPercent(scope.row.f24) }}
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="f25" label="今年涨跌幅" width="130">
              <template #default="scope">
                <span :class="scope.row.f25 >= 0 ? 'rise' : 'fall'">
                  {{ formatPercent(scope.row.f25) }}
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="f62" label="主力净流入额" width="140">
              <template #default="scope">
                <span :class="scope.row.f62 >= 0 ? 'rise' : 'fall'">
                  {{ formatAmount(scope.row.f62) }}
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="f104" label="涨家数" width="100" />
            <el-table-column prop="f105" label="跌家数" width="100" />
            <el-table-column prop="f106" label="平家数" width="100" />
            <el-table-column prop="f115" label="市盈率TTM" width="130">
              <template #default="scope">
                {{ formatNumber(scope.row.f115) }}
              </template>
            </el-table-column>
            <el-table-column prop="f128" label="领涨股" width="140" />
            <el-table-column prop="f140" label="领涨股代码" width="130" />
          </el-table>
        </div>

        <el-empty v-if="!loading && !error && industryData.length === 0" description="暂无数据" />
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
const industryData = ref<any[]>([])
const lastUpdated = ref('--:--:--')

const formatNumber = (value: number | string): string => {
  if (value === undefined || value === null || value === '-') return '-'
  const num = Number(value)
  return isNaN(num) ? '-' : num.toFixed(2)
}

const formatPercent = (value: number | string): string => {
  if (value === undefined || value === null || value === '-') return '-'
  const num = Number(value)
  return isNaN(num) ? '-' : num.toFixed(2) + '%'
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

const sectorStats = computed(() => {
  const rows = industryData.value
  const riseCount = rows.filter((item) => Number(item.f3) > 0).length
  const fallCount = rows.filter((item) => Number(item.f3) < 0).length
  const strongest = rows.reduce<any | null>((best, row) => {
    if (!best) return row
    return Number(row.f3) > Number(best.f3) ? row : best
  }, null)
  const netFlow = rows.reduce((sum, row) => sum + Number(row.f62 || 0), 0)

  return [
    {
      label: '覆盖行业',
      value: String(rows.length),
      note: '当前抓取到的行业板块快照',
      tone: '',
      compact: false
    },
    {
      label: '上涨板块',
      value: String(riseCount),
      note: `下跌 ${fallCount} 个行业`,
      tone: riseCount >= fallCount ? 'rise' : 'fall',
      compact: false
    },
    {
      label: '最强板块',
      value: strongest?.f14 ?? '--',
      note: strongest ? `${formatPercent(strongest.f3)} 涨幅` : '暂无数据',
      tone: Number(strongest?.f3 ?? 0) >= 0 ? 'rise' : 'fall',
      compact: true
    },
    {
      label: '主力净流入',
      value: formatAmount(netFlow),
      note: '当前板块快照合计值',
      tone: netFlow >= 0 ? 'rise' : 'fall',
      compact: false
    }
  ]
})

const handleHistoryDetail = (industryCode: string) => {
  router.push({
    path: '/industry-kline',
    query: { industryCode }
  })
}

const fetchIndustryBaseData = async () => {
  loading.value = true
  error.value = ''

  try {
    const response = await axios.get('http://localhost:8080/api/industry/base')
    const parsedData = parseApiPayload(response.data)

    if (parsedData && parsedData.data && parsedData.data.diff) {
      industryData.value = parsedData.data.diff
      lastUpdated.value = formatFetchedAt(getFetchedAt(parsedData))
    } else {
      error.value = '获取数据失败：数据格式不正确'
    }
  } catch (err) {
    console.error('获取行业板块基础数据失败:', err)
    error.value = '获取数据失败：网络错误'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchIndustryBaseData()
})
</script>

<style scoped>
.industry-base-container {
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
  min-height: 360px;
  overflow: auto;
}

.table-wrapper {
  overflow-x: auto;
}

.market-table {
  min-width: 1880px;
}

.error-alert {
  margin-bottom: 18px;
}
</style>
