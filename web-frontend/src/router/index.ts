import { createRouter, createWebHistory } from 'vue-router'

import HomeView from '@/views/HomeView.vue'
import OrderView from '@/views/OrderView.vue'

function resolvePublicBase(): string {
  const envBase = import.meta.env.VITE_PUBLIC_BASE
  if (typeof envBase === 'string' && envBase.trim() !== '') {
    const raw = envBase.trim()
    if (!raw || raw === '/') return '/'
    const base = raw.startsWith('/') ? raw : `/${raw}`
    return base.endsWith('/') ? base : `${base}/`
  }
  try {
    const injected = (window as unknown as { __DISHES_PUBLIC_BASE__?: string }).__DISHES_PUBLIC_BASE__
    const raw = (injected ?? '/dishes').trim()
    if (!raw) return '/dishes/'
    const base = raw.startsWith('/') ? raw : `/${raw}`
    return base.endsWith('/') ? base : `${base}/`
  } catch {
    return '/dishes/'
  }
}

const router = createRouter({
  history: createWebHistory(resolvePublicBase()),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView,
      meta: { title: '首页' },
    },
    {
      path: '/order',
      name: 'order',
      component: OrderView,
      meta: { title: '点菜' },
    },
    { path: '/dishes/manage', redirect: '/' },
  ],
  scrollBehavior() {
    return { top: 0 }
  },
})

export default router
