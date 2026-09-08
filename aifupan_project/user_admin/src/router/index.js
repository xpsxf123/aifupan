/**
 * @file router/index.js
 * @description 路由配置入口，包含路由守卫和动态路由生成
 */

import { createRouter, createWebHistory } from 'vue-router'

// 静态路由
const constantRoutes = [
  {
    path: '/login',
    component: () => import('@/views/Login/index.vue'),
    hidden: true
  },
  {
    path: '/404',
    component: () => import('@/views/404.vue'),
    hidden: true
  },
  {
     path: '/403',
     component: () => import('@/views/403.vue'),
     hidden: true
  },
  {
    path: '/',
    redirect: '/login'
  },
  {
    path: '/:pathMatch(.*)*',
    component: () => import('@/views/404.vue'),
    hidden: true
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes: constantRoutes,
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) return savedPosition
    return { left: 0, top: 0 }
  }
})

export default router
