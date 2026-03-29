<template>
  <div class="app-shell">
    <header class="terminal-hero">
      <div class="terminal-copy">
        <p class="page-kicker">Dongfang Terminal</p>
        <h1 class="page-title">东方财富数据看板</h1>
        <p class="page-subtitle">
          把行业热度、股票池、盘口强弱和长周期 K 线放到同一个交易界面里，先扫全局，再下钻细节。
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
          <span class="hero-badge-label">界面风格</span>
          <strong>行情终端 / 看盘工作台</strong>
        </div>
      </div>
    </header>

    <section class="workspace-shell">
      <div class="workspace-header">
        <div>
          <p class="section-kicker">Market Workspace</p>
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
        <el-tab-pane label="实盘委托买卖数据" name="stock-real">
          <router-view v-if="activeTab === 'stock-real'" />
        </el-tab-pane>
        <el-tab-pane label="读取股票池" name="stock-pool">
          <router-view v-if="activeTab === 'stock-pool'" />
        </el-tab-pane>
      </el-tabs>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()
const activeTab = ref(route.path.slice(1) || 'industry-base')
const defaultTabKey = 'industry-base'
type TabMeta = { label: string; focus: string; description: string }

const defaultTabMeta: TabMeta = {
  label: '行业板块基础数据',
  focus: '强弱分布、主力流向、板块热度',
  description: '适合先做全局扫描，快速找出当天最强、最弱和资金关注度最高的行业方向。'
}

const tabMeta: Record<string, TabMeta> = {
  [defaultTabKey]: defaultTabMeta,
  'industry-kline': {
    label: '行业板块 K 线数据',
    focus: '历史趋势、波动结构、换手变化',
    description: '从价格和成交维度下钻到单个行业，把短线热度放回更长的趋势背景里。'
  },
  'stock-real': {
    label: '实盘委托买卖数据',
    focus: '盘口强弱、委差委比、即时成交',
    description: '更像盯盘界面，适合判断一只股票当前的买卖力量和交易活跃度。'
  },
  'stock-pool': {
    label: '读取股票池',
    focus: '全市场列表、板块归类、联动查询',
    description: '把全市场股票按一张工作表铺开，便于从池子里继续跳转到实盘和 K 线。'
  }
}

const activeMeta = computed<TabMeta>(() => {
  return tabMeta[activeTab.value] || defaultTabMeta
})

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
