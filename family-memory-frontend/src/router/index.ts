import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    name: 'main',
    component: () => import('@/views/MainLayout.vue'),
    meta: { title: '时光·家' }
  },
  {
    path: '/memory/:id',
    name: 'memory-detail',
    component: () => import('@/views/MemoryDetailView.vue'),
    meta: { title: '记忆详情' }
  },
  {
    path: '/today-in-history',
    name: 'today-in-history',
    component: () => import('@/views/TodayInHistoryView.vue'),
    meta: { title: '那年今日' }
  },
  {
    path: '/memories',
    name: 'all-memories',
    component: () => import('@/views/AllMemoriesView.vue'),
    meta: { title: '全部动态' }
  }
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes
})

router.beforeEach((to, _from, next) => {
  document.title = `${to.meta.title || '时光·家'} - 家庭记忆`
  next()
})

export default router
