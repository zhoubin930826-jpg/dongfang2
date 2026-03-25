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
    meta: { title: '实盘委托买卖数据' }
  },
  {
    path: '/stock-pool',
    name: 'StockPool',
    component: () => import('../views/StockPool.vue'),
    meta: { title: '读取股票池' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
