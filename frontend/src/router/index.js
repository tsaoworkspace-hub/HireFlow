import { createRouter, createWebHistory } from 'vue-router'

import { useAuthStore } from '@/stores/auth'
import DashboardView from '@/views/DashboardView.vue'
import ForbiddenView from '@/views/ForbiddenView.vue'
import LoginView from '@/views/LoginView.vue'
import ApplicationBoardView from '@/views/ApplicationBoardView.vue'
import CandidateListView from '@/views/CandidateListView.vue'
import JobOpeningListView from '@/views/JobOpeningListView.vue'
import InterviewListView from '@/views/InterviewListView.vue'
import DecisionListView from '@/views/DecisionListView.vue'
import AuditLogView from '@/views/AuditLogView.vue'
import RequisitionDetailView from '@/views/RequisitionDetailView.vue'
import RequisitionFormView from '@/views/RequisitionFormView.vue'
import RequisitionListView from '@/views/RequisitionListView.vue'
import CareersView from '@/views/CareersView.vue'
import PublicJobView from '@/views/PublicJobView.vue'
import CandidatePortalView from '@/views/CandidatePortalView.vue'
import CompanyCalendarView from '@/views/CompanyCalendarView.vue'
import IntegrationView from '@/views/IntegrationView.vue'
import OfferListView from '@/views/OfferListView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', name: 'login', component: LoginView, meta: { publicLayout: true } },
    { path: '/careers', name: 'careers', component: CareersView, meta: { publicLayout: true } },
    { path: '/careers/:slug', name: 'public-job', component: PublicJobView, meta: { publicLayout: true } },
    { path: '/candidate/applications/:publicId', name: 'candidate-portal', component: CandidatePortalView, meta: { publicLayout: true } },
    { path: '/', name: 'dashboard', component: DashboardView, meta: { requiresAuth: true } },
    {
      path: '/requisitions',
      name: 'requisitions',
      component: RequisitionListView,
      meta: { requiresAuth: true, roles: ['HR', 'HIRING_MANAGER'] },
    },
    {
      path: '/requisitions/new',
      name: 'requisition-create',
      component: RequisitionFormView,
      meta: { requiresAuth: true, roles: ['HR', 'HIRING_MANAGER'] },
    },
    {
      path: '/requisitions/:id/edit',
      name: 'requisition-edit',
      component: RequisitionFormView,
      meta: { requiresAuth: true, roles: ['HR', 'HIRING_MANAGER'] },
    },
    {
      path: '/requisitions/:id',
      name: 'requisition-detail',
      component: RequisitionDetailView,
      meta: { requiresAuth: true, roles: ['HR', 'HIRING_MANAGER'] },
    },
    {
      path: '/candidates',
      name: 'candidates',
      component: CandidateListView,
      meta: { requiresAuth: true, roles: ['HR', 'HIRING_MANAGER'] },
    },
    { path: '/job-openings', name: 'job-openings', component: JobOpeningListView, meta: { requiresAuth: true, roles: ['HR', 'HIRING_MANAGER'] } },
    { path: '/applications', name: 'applications', component: ApplicationBoardView, meta: { requiresAuth: true, roles: ['HR', 'HIRING_MANAGER'] } },
    {
      path: '/interviews',
      name: 'interviews',
      component: InterviewListView,
      meta: { requiresAuth: true, roles: ['HR', 'HIRING_MANAGER', 'INTERVIEWER'] },
    },
    { path: '/calendar', name: 'calendar', component: CompanyCalendarView, meta: { requiresAuth: true, roles: ['HR', 'HIRING_MANAGER', 'INTERVIEWER'] } },
    { path: '/integrations', name: 'integrations', component: IntegrationView, meta: { requiresAuth: true, roles: ['HR', 'HIRING_MANAGER', 'INTERVIEWER'] } },
    {
      path: '/decisions',
      name: 'decisions',
      component: DecisionListView,
      meta: { requiresAuth: true, roles: ['HR', 'HIRING_MANAGER'] },
    },
    { path: '/offers', name: 'offers', component: OfferListView, meta: { requiresAuth: true, roles: ['HR', 'HIRING_MANAGER'] } },
    {
      path: '/admin/audit-logs',
      name: 'audit-logs',
      component: AuditLogView,
      meta: { requiresAuth: true, roles: ['ADMIN'] },
    },
    { path: '/forbidden', name: 'forbidden', component: ForbiddenView, meta: { requiresAuth: true } },
    { path: '/:pathMatch(.*)*', redirect: '/' },
  ],
})

router.beforeEach(async (to) => {
  const authStore = useAuthStore()
  await authStore.restoreSession()

  if (to.name === 'login' && authStore.isAuthenticated) {
    return { name: 'dashboard' }
  }
  if (to.meta.requiresAuth && !authStore.isAuthenticated) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (to.meta.roles && !authStore.hasAnyRole(to.meta.roles)) {
    return { name: 'forbidden' }
  }
  return true
})

export default router

