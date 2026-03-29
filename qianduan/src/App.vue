<template>
  <div class="app-shell">
    <header class="terminal-hero">
      <div class="terminal-copy">
        <p class="page-kicker">东方终端</p>
        <h1 class="page-title">东方财富数据看板</h1>
        <p class="page-subtitle">
          把行业热度、股票池、盘口强弱和实时选股分析放到同一个工作台里，先看市场环境，再下钻到具体股票。
        </p>
      </div>

      <div class="terminal-summary">
        <div class="hero-badge">
          <span class="hero-badge-label">当前模块</span>
          <strong>{{ activeMeta.label }}</strong>
        </div>
        <div class="hero-badge">
          <span class="hero-badge-label">关注重点</span>
          <strong>{{ activeMeta.focus }}</strong>
        </div>
        <div class="hero-badge">
          <span class="hero-badge-label">使用方式</span>
          <strong>先扫市场，再挑股票</strong>
        </div>
      </div>
    </header>

    <section class="workspace-shell">
      <div class="workspace-header">
        <div>
          <p class="section-kicker">行情工作区</p>
          <h2 class="workspace-title">{{ activeMeta.label }}</h2>
          <p class="workspace-note">{{ activeMeta.description }}</p>
        </div>
      </div>

      <el-tabs
        v-model="activeTab"
        class="market-tabs"
        @tab-click="handleTabClick"
      >
        <el-tab-pane label="行业板块基础数据" name="industry-base">
          <router-view v-if="activeTab === 'industry-base'" />
        </el-tab-pane>
        <el-tab-pane label="行业板块 K 线数据" name="industry-kline">
          <router-view v-if="activeTab === 'industry-kline'" />
        </el-tab-pane>
        <el-tab-pane label="个股实盘数据" name="stock-real">
          <router-view v-if="activeTab === 'stock-real'" />
        </el-tab-pane>
        <el-tab-pane label="股票池" name="stock-pool">
          <router-view v-if="activeTab === 'stock-pool'" />
        </el-tab-pane>
        <el-tab-pane label="实时选股分析" name="stock-analysis">
          <router-view v-if="activeTab === 'stock-analysis'" />
        </el-tab-pane>
      </el-tabs>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

type TabMeta = {
  label: string
  focus: string
  description: string
}

const route = useRoute()
const router = useRouter()
const defaultTabKey = 'industry-base'
const activeTab = ref(route.path.slice(1) || defaultTabKey)

const tabMeta: Record<string, TabMeta> = {
  'industry-base': {
    label: '行业板块基础数据',
    focus: '板块强弱、资金方向、行业热度',
    description: '先看全市场行业分布，找到当天最强、最弱和主力关注最多的方向。'
  },
  'industry-kline': {
    label: '行业板块 K 线数据',
    focus: '历史趋势、波动结构、成交变化',
    description: '把短线行业热度放回更长周期里，帮助判断趋势是否可持续。'
  },
  'stock-real': {
    label: '个股实盘数据',
    focus: '盘口强弱、委比委差、即时成交',
    description: '适合盯单只股票的实时状态，观察买卖盘力量和交易活跃度。'
  },
  'stock-pool': {
    label: '股票池',
    focus: '全市场列表、行业归类、快速联动',
    description: '把全市场股票铺开，先筛行业和概念，再跳到个股盘口和 K 线。'
  },
  'stock-analysis': {
    label: '实时选股分析',
    focus: '市场环境、热点行业、候选评分',
    description: '把大盘环境、板块强弱和股票池评分放到一起，快速找出当前值得重点跟踪的标的。'
  }
}

const activeMeta = computed<TabMeta>(() => tabMeta[activeTab.value] ?? tabMeta[defaultTabKey]!)

const handleTabClick = (tab: { props: { name: string } }) => {
  router.push(`/${tab.props.name}`)
}

watch(
  () => route.path,
  (newPath) => {
    activeTab.value = newPath.slice(1) || defaultTabKey
  },
  { immediate: true }
)
</script>

<style scoped>
.app-shell {
  display: flex;
  flex-direction: column;
  gap: 20px;
  min-height: 100vh;
  padding: 24px;
}

.terminal-hero {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 20px;
  padding: 26px 28px;
  border-radius: 34px;
  background:
    radial-gradient(circle at top right, rgba(184, 138, 69, 0.18), transparent 28%),
    linear-gradient(135deg, rgba(16, 24, 39, 0.98), rgba(29, 45, 72, 0.92));
  box-shadow: 0 26px 58px rgba(22, 32, 51, 0.18);
}

.terminal-summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  min-width: min(480px, 100%);
}

.workspace-shell {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-height: 0;
  padding: 20px;
  border: 1px solid rgba(24, 34, 53, 0.08);
  border-radius: 34px;
  background: rgba(255, 252, 247, 0.58);
  box-shadow: 0 18px 40px rgba(22, 32, 51, 0.08);
  backdrop-filter: blur(14px);
}

.workspace-header {
  margin-bottom: 18px;
}

.workspace-title {
  font-size: 28px;
  color: var(--text-primary);
}

.workspace-note {
  margin: 8px 0 0;
  color: var(--text-secondary);
  font-size: 15px;
}

.market-tabs {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-height: 0;
}

:deep(.market-tabs .el-tabs__header) {
  margin: 0 0 18px;
}

:deep(.market-tabs .el-tabs__nav-wrap::after) {
  display: none;
}

:deep(.market-tabs .el-tabs__nav-scroll) {
  padding: 8px;
  border-radius: 24px;
  background: rgba(24, 34, 53, 0.05);
}

:deep(.market-tabs .el-tabs__nav) {
  border: none;
  gap: 10px;
}

:deep(.market-tabs .el-tabs__item) {
  height: auto;
  padding: 12px 18px !important;
  border: none !important;
  border-radius: 16px;
  color: var(--text-secondary);
  font-weight: 700;
  transition: all 0.2s ease;
}

:deep(.market-tabs .el-tabs__item:hover) {
  color: var(--text-primary);
}

:deep(.market-tabs .el-tabs__item.is-active) {
  color: #ffffff;
  background: linear-gradient(135deg, #20314f, #101827);
  box-shadow: 0 14px 24px rgba(22, 32, 51, 0.18);
}

:deep(.market-tabs .el-tabs__content) {
  flex: 1;
  min-height: 0;
}

:deep(.market-tabs .el-tab-pane) {
  height: 100%;
}

@media (max-width: 1100px) {
  .terminal-hero {
    flex-direction: column;
  }

  .terminal-summary {
    width: 100%;
    min-width: 0;
  }
}

@media (max-width: 768px) {
  .app-shell {
    padding: 16px;
  }

  .terminal-hero,
  .workspace-shell {
    padding: 18px;
    border-radius: 26px;
  }

  .terminal-summary {
    grid-template-columns: 1fr;
  }

  .workspace-title {
    font-size: 24px;
  }
}
</style>
