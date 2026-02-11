<template>
  <div class="app-container">
    <el-tabs v-model="activeTab" @tab-click="handleTabClick" type="card" :stretch="true">
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
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()
const activeTab = ref(route.path.slice(1) || 'industry-base')

const handleTabClick = (tab: any) => {
  const tabName = tab.props.name
  router.push(`/${tabName}`)
}

watch(
  () => route.path,
  (newPath) => {
    activeTab.value = newPath.slice(1) || 'industry-base'
  }
)

onMounted(() => {
  // 初始加载时确保tab与路由同步
  activeTab.value = route.path.slice(1) || 'industry-base'
})
</script>

<style scoped>
.app-container {
  width: 100%;
  height: 100vh;
  padding: 20px;
  box-sizing: border-box;
}

.el-tabs {
  height: 100%;
}

.el-tabs__content {
  height: calc(100% - 40px);
  overflow: auto;
}
</style>
