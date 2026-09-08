import { createWebHashHistory, createRouter } from 'vue-router'
import { pageRoutes, moduleRoute } from '@/router/route.js'

const router = createRouter({
    history: createWebHashHistory(),
    routes: [...pageRoutes, ...moduleRoute]
})

export default router
