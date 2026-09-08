/**
 * @file auth/guard.js
 * @description 路由权限控制
 */
import router from '@/router'
import { useUserStore } from '@/store/user'
import { usePermissionStore } from '@/auth/store'
import { menuConfig } from '@/config/menu'
import { ensureDynamicRoutes, resetDynamicRoutes, shouldRematchAfterDynamic } from '@/router/dynamicRoutes'
import { getFirstAccessiblePathByMenu } from '@/auth/menuTransform'

const whiteList = ['/login', '/404', '/403']

const rematchOnce = new Set()
const clientAuthOnce = new Set()

const getQueryToken = (route) => {
  const raw = route?.query?.token
  if (typeof raw !== 'string') return ''
  return raw.trim()
}

const removeTokenQuery = (route) => {
  const query = { ...(route?.query || {}) }
  delete query.token
  return query
}

const isAuthInvalidError = (error) => {
  const httpStatus = Number(error?.response?.status)
  const rawCode = error?.response?.data?.code ?? error?.code
  const code = Number(rawCode)
  const message = String(error?.response?.data?.message || error?.response?.data?.msg || error?.message || '')
  const isAuthCode = httpStatus === 401 || code === 401 || code === 4001
  const isPermissionDenied = message.includes('权限不足')
  return isAuthCode && !isPermissionDenied
}

const hasPathRoute = (routerInstance, path) => {
  const p = String(path || '').trim()
  if (!p) return false
  const routes = routerInstance?.getRoutes ? routerInstance.getRoutes() : []
  return routes.some((r) => String(r?.path || '') === p)
}

router.beforeEach(async (to, from, next) => {
  // 扩展功能：isWhite 白名单配置
  if (to.meta && to.meta.isWhite) {
    next()
    return
  }

  const userStore = useUserStore()
  const permissionStore = usePermissionStore()
  const queryToken = getQueryToken(to)
  const hasClientAuthToken = Boolean(queryToken)
  const token = userStore.token

  if (hasClientAuthToken) {
    const key = `${to.path}|${queryToken}`
    if (!clientAuthOnce.has(key)) {
      clientAuthOnce.add(key)
      const redirectPath = router.resolve({ path: to.path, query: removeTokenQuery(to), params: to.params }).fullPath
      try {
        permissionStore.resetMenu()
        resetDynamicRoutes(router)
        await userStore.logout()
        await userStore.clientAuthLogin({ token: queryToken })

        if (to.path === '/login') {
          try {
            await permissionStore.ensureMenuTree({ maxAgeMs: 0, minIntervalMs: 0 })
          } catch (e) {
            void e
          }

          try {
            await ensureDynamicRoutes({
              router,
              permissionStore,
              menuConfig
            })
          } catch (e) {
            void e
          }

          const rawRedirect = to.query?.redirect
          const redirect = typeof rawRedirect === 'string' ? rawRedirect.trim() : ''
          if (redirect) {
            next({ path: redirect, replace: true })
            return
          }

          const fallback =
            getFirstAccessiblePathByMenu({
              menuTree: permissionStore.menuTree,
              menuConfig,
              permissionStore
            }) || ''

          if (!fallback || fallback === '/404' || fallback === '/login' || fallback === '/') {
            next({ path: '/404', replace: true })
            return
          }

          if (!hasPathRoute(router, fallback)) {
            next({ path: '/404', replace: true })
            return
          }

          next({ path: fallback, replace: true })
          return
        }

        next({ path: to.path, query: removeTokenQuery(to), params: to.params, replace: true })
        return
      } catch (e) {
        clientAuthOnce.delete(key)
        if (isAuthInvalidError(e)) {
          next({ path: '/login', query: { redirect: redirectPath } })
          return
        }
        next({ path: '/login', query: { redirect: redirectPath } })
        return
      }
    }
  }

  if (token) {
    if (to.path === '/login') {
      permissionStore.resetMenu()
      resetDynamicRoutes(router)
      await userStore.logout()
      next()
      return
    } else {
      if (to.path === '/404' || to.path === '/403') {
        next()
        return
      }

      try {
        await permissionStore.ensureMenuTree({ maxAgeMs: 5 * 60 * 1000, minIntervalMs: 3000 })
      } catch (e) {
        void e
      }

      try {
        await ensureDynamicRoutes({
          router,
          permissionStore,
          menuConfig
        })
      } catch (e) {
        void e
      }

      if (shouldRematchAfterDynamic(to)) {
        const key = String(to.fullPath || '')
        if (key && !rematchOnce.has(key)) {
          rematchOnce.add(key)
          next({
            path: to.path,
            query: to.query, // 保留查询参数
            params: to.params, // 保留路径参数
            replace: true
          })
          return
        }

        if (key) rematchOnce.delete(key)

        const fallback =
          getFirstAccessiblePathByMenu({
            menuTree: permissionStore.menuTree,
            menuConfig,
            permissionStore
          }) || ''

        if (!fallback || fallback === '/404' || fallback === '/login' || fallback === '/') {
          next({ path: '/404', replace: true })
          return
        }

        if (!hasPathRoute(router, fallback)) {
          next({ path: '/404', replace: true })
          return
        }

        next({ path: fallback, replace: true })
        return
      }

      const meta = to.meta || {}
      const hasPermissionCode = Object.prototype.hasOwnProperty.call(meta, 'permissionCode')
      const requiredCode = hasPermissionCode ? String(meta.permissionCode || '').trim() : ''

      if (requiredCode && !permissionStore.hasMenuAccess(requiredCode, to.path)) {
        next({ path: '/403', query: { from: to.fullPath }, replace: true })
        return
      }

      next()
    }
  } else {
    if (whiteList.includes(to.path)) {
      next()
    } else {
      next({ path: '/login', query: { redirect: to.fullPath } })
    }
  }
})
