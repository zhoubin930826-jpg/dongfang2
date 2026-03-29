import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    redirect: '/industry-base'
  },
  {
    path: '/industry-base',
    name: 'IndustryBase',
    component: () => import('../views/IndustryBase.vue'),
    meta: { title: '行业板块基础数据' }
  },
  {
    path: '/industry-kline',
    name: 'IndustryKline',
    component: () => import('../views/IndustryKline.vue'),
    meta: { title: '行业板块 K 线数据' }
  },
  {
    path: '/stock-real',
    name: 'StockReal',
    component: () => import('../views/StockReal.vue'),
    meta: { title: '个股实盘数据' }
  },
  {
    path: '/stock-pool',
    name: 'StockPool',
    component: () => import('../views/StockPool.vue'),
    meta: { title: '股票池' }
  },
  {
    path: '/stock-analysis',
    name: 'StockAnalysis',
    component: () => import('../views/StockAnalysis.vue'),
    meta: { title: '实时选股分析' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
