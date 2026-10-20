import type { RouteRecordRaw } from 'vue-router'

const NotFound = () => import('@/pages/NotFoundPage.vue')

export const routes: RouteRecordRaw[] = [
  { path: '/', component: () => import('@/pages/HomePage.vue') },
  { path: '/privacy-policy', component: () => import('@/pages/PrivacyPolicyPage.vue') },
  { path: '/open-source-licenses', component: () => import('@/pages/LicensesPage.vue') },
  { path: '/404', component: NotFound },
  { path: '/:pathMatch(.*)*', component: NotFound },
]
