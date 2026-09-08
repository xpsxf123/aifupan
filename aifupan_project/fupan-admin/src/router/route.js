// 页面路由
const pageRoutes = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/pages/login/index.vue'),
    meta: { title: '登录' },
  },
  {
    path: '/analysis',
    name: 'analysis',
    component: () =>
      import(
        '@/views/userInfo/anchorUrl/transcribe/components/audioAnalysisOnline.vue'
      ),
    meta: { title: '分析内容' },
  },
  // {
  //     path: '/aiAnalysis',
  //     name: 'aiAnalysis',
  //     component: () => import('@/views/modules/userInfo/anchorUrl/AiAnalysisDialog.vue'),
  //     meta: {title: 'AI分析'}
  // },
  {
    path: '/contrast',
    name: 'contrast',
    component: () =>
      import('@/views/userInfo/anchorUrl/recod/analysis-contrast-index.vue'),
    meta: { title: '对比分析' },
  },
]

// 模块路由
const moduleRoute = [
  {
    path: '/',
    redirect: { name: 'login' },
    name: 'main',
    component: () => import('@/layout/index.vue'),
    meta: {
      title: '主入口',
    },
  },
]
// 任意路由
const anyRoute = [
  {
    path: '/:pathMatch(.*)*',
    type: 3,
    name: 'NotFound',
    component: () => import('@/views/pages/404/index.vue'),
  },
]

export { pageRoutes, moduleRoute, anyRoute }
