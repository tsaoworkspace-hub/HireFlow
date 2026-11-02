import { createRouter, createWebHistory } from 'vue-router'

import DashboardView from '@/views/DashboardView.vue'
import PlaceholderView from '@/views/PlaceholderView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'dashboard', component: DashboardView },
    {
      path: '/requisitions',
      name: 'requisitions',
      component: PlaceholderView,
      props: { title: '人力需求', nextStep: 'Day 3：建立、送出與 HR 核准流程' },
    },
    {
      path: '/candidates',
      name: 'candidates',
      component: PlaceholderView,
      props: { title: '應徵者', nextStep: 'Day 4：Candidate 與 Application 分離建模' },
    },
    {
      path: '/interviews',
      name: 'interviews',
      component: PlaceholderView,
      props: { title: '面試', nextStep: 'Day 5：排程、衝突檢查與評分表' },
    },
    { path: '/:pathMatch(.*)*', redirect: '/' },
  ],
})

export default router

