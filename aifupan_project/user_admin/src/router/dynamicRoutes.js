import { generateRoutes } from './routeGenerator'
import { buildRouteConfigFromPermissionSet, buildRouteConfigFromMenuTree } from '@/auth/menuTransform'

let dynamicAdded = false
let dynamicRouteNames = []

const isPathMatchRoute = (route) => {
  const path = String(route?.path || '')
  return path.includes(':pathMatch')
}

const mergeRouteConfig = (left, right) => {
  if (!left) return right
  if (!right) return left
  const leftChildren = Array.isArray(left.children) ? left.children : []
  const rightChildren = Array.isArray(right.children) ? right.children : []
  if (!leftChildren.length && !rightChildren.length) return left
  return {
    ...left,
    children: mergeRouteConfigs(leftChildren, rightChildren)
  }
}

const getRouteConfigKey = (route) => {
  const name = route?.name
  if (name) return `name:${String(name)}`
  return `path:${String(route?.path || '')}`
}

const mergeRouteConfigs = (base, extra) => {
  const map = new Map()
  const put = (r) => {
    if (!r) return
    const key = getRouteConfigKey(r)
    const existed = map.get(key)
    map.set(key, existed ? mergeRouteConfig(existed, r) : r)
  }
  ;(Array.isArray(base) ? base : []).forEach(put)
  ;(Array.isArray(extra) ? extra : []).forEach(put)
  return Array.from(map.values())
}

export const resetDynamicRoutes = (router) => {
  if (!router) return
  dynamicRouteNames.forEach((name) => {
    try {
      if (router.hasRoute(name)) {
        router.removeRoute(name)
      }
    } catch (e) {
      void e
    }
  })
  dynamicRouteNames = []
  dynamicAdded = false
}

export const ensureDynamicRoutes = async ({ router, permissionStore, menuConfig }) => {
  if (!router) return { added: false, reMatch: false }
  if (dynamicAdded) return { added: false, reMatch: false }

  const menuTree = permissionStore?.menuTree
  const hasMenuTree = Array.isArray(menuTree?.value)
    ? menuTree.value.length > 0
    : Array.isArray(menuTree) && menuTree.length > 0

  const configByPermissionSet = buildRouteConfigFromPermissionSet({
    menuConfig,
    allowedPermissionSet: permissionStore?.pagePermissionSet
  })

  const configByMenuTree = hasMenuTree
    ? buildRouteConfigFromMenuTree({
        menuTree,
        menuConfig
      })
    : []

  const config = mergeRouteConfigs(configByPermissionSet, configByMenuTree)
  const routes = generateRoutes(config, true)

  if (!routes.length) {
    return { added: false, reMatch: false }
  }

  const names = []
  routes.forEach((r) => {
    if (!r) return
    if (r.name && !router.hasRoute(r.name)) {
      router.addRoute(r)
      names.push(r.name)
    } else if (!r.name) {
      router.addRoute(r)
    }
  })

  dynamicRouteNames = names
  dynamicAdded = true

  return { added: true, reMatch: true }
}

export const shouldRematchAfterDynamic = (to) => {
  const matched = Array.isArray(to?.matched) ? to.matched : []
  if (matched.length !== 1) return false
  return isPathMatchRoute(matched[0])
}
