/**
 * @file auth/store.js
 * @description 权限状态管理
 */
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { permissionValidator, permissionConverter } from './core'
import { permissionConfig, roleConfig } from './config'
import roleApi from '@/http/api/role'

export const usePermissionStore = defineStore('permission', () => {
  // 状态
  const parseJsonArray = (raw) => {
    try {
      const val = JSON.parse(raw)
      return Array.isArray(val) ? val : []
    } catch (e) {
      return []
    }
  }

  const cachedMenuUrlsRaw = localStorage.getItem('permission_menuUrls_v1')
  const cachedPermissionsRaw = localStorage.getItem('permission_permissions_v1')
  const cachedPagePermissionCodesRaw = localStorage.getItem('permission_pagePermissionCodes_v1')
  const cachedPageUrlsRaw = localStorage.getItem('permission_pageUrls_v1')
  const cachedMenuTreeRaw = localStorage.getItem('permission_menuTree_v1')
  const cachedMenuLoadedAtRaw = localStorage.getItem('permission_menuLoadedAt_v1')

  const permissions = ref(parseJsonArray(cachedPermissionsRaw)) // 当前用户的权限码列表 ['admin:user:add', ...]
  const pagePermissionCodes = ref(parseJsonArray(cachedPagePermissionCodesRaw)) // 当前用户可访问页面权限(type=0)
  const pageUrls = ref(parseJsonArray(cachedPageUrlsRaw)) // 当前用户可访问页面URL(type=0)
  const routes = ref([]) // 动态路由 (如果有)
  const menuLoaded = ref(false)
  const menuTree = ref(parseJsonArray(cachedMenuTreeRaw))
  const menuUrls = ref(parseJsonArray(cachedMenuUrlsRaw))
  const menuLoadedAt = ref(Number(cachedMenuLoadedAtRaw) || 0)

  // Getters
  const allPermissions = computed(() => permissions.value)
  const menuUrlSet = computed(() => new Set(menuUrls.value))
  const pagePermissionSet = computed(() => new Set(pagePermissionCodes.value))
  const pageUrlSet = computed(() => new Set(pageUrls.value))

  // Actions
  /**
   * 设置权限列表
   * @param {string[]} perms 权限码数组
   */
  const setPermissions = (perms) => {
    permissions.value = perms
  }

  /**
   * 根据角色生成权限 (模拟后端逻辑，或者前端静态配置)
   * @param {string[]} roles 角色列表
   */
  const generatePermissionsByRoles = (roles) => {
    let perms = []

    // 如果是超级管理员，直接给 *
    if (roles.includes('super_admin')) {
      perms = ['*']
    } else {
      // 遍历角色，合并权限
      roles.forEach((role) => {
        if (roleConfig[role]) {
          perms = [...perms, ...roleConfig[role]]
        }
      })
    }

    // 去重
    permissions.value = [...new Set(perms)]
    return permissions.value
  }

  /**
   * 检查是否拥有权限
   * @param {string|string[]} code
   */
  const hasPermission = (code) => {
    return permissionValidator.check(permissions.value, code)
  }

  /**
   * 获取所有可用权限配置 (用于展示或调试)
   */
  const getAllConfiguredPermissions = () => {
    return permissionConverter.convertConfig(permissionConfig)
  }

  const resetMenu = () => {
    menuLoaded.value = false
    menuTree.value = []
    menuUrls.value = []
    permissions.value = []
    pagePermissionCodes.value = []
    pageUrls.value = []
    menuLoadedAt.value = 0
    localStorage.removeItem('permission_menuUrls_v1')
    localStorage.removeItem('permission_permissions_v1')
    localStorage.removeItem('permission_pagePermissionCodes_v1')
    localStorage.removeItem('permission_pageUrls_v1')
    localStorage.removeItem('permission_menuTree_v1')
    localStorage.removeItem('permission_menuLoadedAt_v1')
  }

  const setMenuTree = (tree) => {
    menuTree.value = Array.isArray(tree) ? tree : []

    const result = new Set()
    const permissionResult = new Set()
    const pagePermissionResult = new Set()
    const pageUrlResult = new Set()
    const walk = (list) => {
      if (!Array.isArray(list)) return
      list.forEach((item) => {
        if (!item) return
        const url = typeof item.url === 'string' ? item.url.trim() : ''
        if (url) result.add(url)
        const permissionCode = typeof item.permissionCode === 'string' ? item.permissionCode.trim() : ''
        if (permissionCode) result.add(permissionCode)
        if (permissionCode) permissionResult.add(permissionCode)
        if (Number(item.type) === 0) {
          if (permissionCode) pagePermissionResult.add(permissionCode)
          if (url) pageUrlResult.add(url)
        }
        if (item.children) walk(item.children)
      })
    }
    walk(menuTree.value)

    menuUrls.value = Array.from(result)
    permissions.value = Array.from(permissionResult)
    pagePermissionCodes.value = Array.from(pagePermissionResult)
    pageUrls.value = Array.from(pageUrlResult)
    menuLoadedAt.value = Date.now()
    localStorage.setItem('permission_menuUrls_v1', JSON.stringify(menuUrls.value))
    localStorage.setItem('permission_permissions_v1', JSON.stringify(permissions.value))
    localStorage.setItem('permission_pagePermissionCodes_v1', JSON.stringify(pagePermissionCodes.value))
    localStorage.setItem('permission_pageUrls_v1', JSON.stringify(pageUrls.value))
    localStorage.setItem('permission_menuTree_v1', JSON.stringify(menuTree.value))
    localStorage.setItem('permission_menuLoadedAt_v1', String(menuLoadedAt.value))
    menuLoaded.value = true
  }

  const normalizeTree = (res) => {
    const data = res?.data
    if (Array.isArray(data)) return data
    if (Array.isArray(data?.data)) return data.data
    return []
  }

  const loadMenuTree = async () => {
    const res = await roleApi.userMenuTree({ silent: true })
    setMenuTree(normalizeTree(res))
    return menuTree.value
  }

  let loadingPromise = null
  let lastFetchAt = 0

  const ensureMenuTree = async (options = {}) => {
    const force = Boolean(options.force)
    const maxAgeMs = typeof options.maxAgeMs === 'number' ? options.maxAgeMs : 0
    const minIntervalMs = typeof options.minIntervalMs === 'number' ? options.minIntervalMs : 0
    const now = Date.now()
    const stale = !menuLoadedAt.value || now - menuLoadedAt.value > maxAgeMs
    const treeEmpty = !Array.isArray(menuTree.value) || menuTree.value.length === 0

    if (!menuLoaded.value) {
      if (
        menuUrls.value.length ||
        permissions.value.length ||
        pagePermissionCodes.value.length ||
        pageUrls.value.length
      ) {
        menuLoaded.value = true
      }
    }

    const shouldFetch = force || !menuLoaded.value || stale || treeEmpty
    if (shouldFetch) {
      if (!loadingPromise) {
        const tooFrequent = minIntervalMs > 0 && lastFetchAt && now - lastFetchAt < minIntervalMs
        if (!tooFrequent) {
          lastFetchAt = now
          loadingPromise = loadMenuTree().finally(() => {
            loadingPromise = null
          })
        }
      }
      if (loadingPromise) {
        await loadingPromise
      }
    }
    return menuTree.value
  }

  let autoRefreshTimer = null

  const startAutoRefresh = (options = {}) => {
    const intervalMs = typeof options.intervalMs === 'number' ? options.intervalMs : 60 * 1000
    if (autoRefreshTimer) return
    autoRefreshTimer = setInterval(async () => {
      try {
        if (typeof document !== 'undefined' && document.visibilityState && document.visibilityState !== 'visible')
          return
        await ensureMenuTree({ force: true, maxAgeMs: 0 })
      } catch (e) {
        void e
      }
    }, intervalMs)
  }

  const stopAutoRefresh = () => {
    if (!autoRefreshTimer) return
    clearInterval(autoRefreshTimer)
    autoRefreshTimer = null
  }

  const actionCodeMap = computed(() => {
    const map = new Map()
    const collectDescendants = (list, target) => {
      if (!Array.isArray(list)) return
      list.forEach((item) => {
        if (!item) return
        const code = typeof item.permissionCode === 'string' ? item.permissionCode.trim() : ''
        if (code) target.add(code)
        if (item.children) collectDescendants(item.children, target)
      })
    }
    const walk = (node) => {
      if (!node) return
      const code = typeof node.permissionCode === 'string' ? node.permissionCode.trim() : ''
      const children = Array.isArray(node.children) ? node.children : []
      if (code && children.length) {
        const set = map.get(code) || new Set()
        collectDescendants(children, set)
        map.set(code, set)
      }
      children.forEach(walk)
    }
    menuTree.value.forEach(walk)
    return map
  })

  const actionOpMap = computed(() => {
    const map = new Map()
    const ensureBuckets = (listCode) => {
      if (!map.has(listCode)) {
        map.set(listCode, {
          add: new Set(),
          update: new Set(),
          delete: new Set(),
          enable: new Set(),
          disable: new Set(),
          assignMenus: new Set(),
          scheduleAttribute: new Set(),
          addEmployee: new Set(),
          removeEmployee: new Set()
        })
      }
      return map.get(listCode)
    }

    const getOpsByName = (name) => {
      const n = String(name || '').trim()
      if (!n) return []
      const ops = []
      if (n.includes('新增') || n.includes('添加') || n === '新建') ops.push('add')
      if (n.includes('修改') || n.includes('编辑') || n.includes('调整')) ops.push('update')
      if (n.includes('删除') || n.includes('移除')) ops.push('delete')
      if (n.includes('启用')) ops.push('enable')
      if (n.includes('停用') || n.includes('禁用')) ops.push('disable')
      if (n.includes('分配菜单')) ops.push('assignMenus')
      if (n.includes('排班配置')) ops.push('scheduleAttribute')
      if (n.includes('添加人员')) ops.push('addEmployee')
      if (n.includes('移除人员')) ops.push('removeEmployee')
      return ops
    }

    const walkPageNode = (pageNode) => {
      if (!pageNode) return
      const listCode = typeof pageNode.permissionCode === 'string' ? pageNode.permissionCode.trim() : ''
      if (!listCode) return
      const children = Array.isArray(pageNode.children) ? pageNode.children : []
      if (!children.length) return

      const buckets = ensureBuckets(listCode)
      const collect = (nodes) => {
        if (!Array.isArray(nodes)) return
        nodes.forEach((node) => {
          if (!node) return
          const code = typeof node.permissionCode === 'string' ? node.permissionCode.trim() : ''
          const ops = getOpsByName(node.name)
          if (code && ops.length) {
            ops.forEach((op) => {
              if (buckets[op]) buckets[op].add(code)
            })
          }
          if (node.children) collect(node.children)
        })
      }
      collect(children)
    }

    const traverse = (node) => {
      if (!node) return
      walkPageNode(node)
      const children = Array.isArray(node.children) ? node.children : []
      children.forEach(traverse)
    }

    menuTree.value.forEach(traverse)
    return map
  })

  const getActionPermissionCode = (listPermissionCode, operation) => {
    const listCode = String(listPermissionCode || '').trim()
    const op = String(operation || '').trim()
    if (!listCode || !op) return ''

    const inferByListCode = (baseCode, opKey) => {
      const base = String(baseCode || '').trim()
      const action = String(opKey || '').trim()
      if (!base || !action) return ''
      const m = base.match(/^(.*?):(list|page)$/)
      if (!m) return ''
      return `${m[1]}:${action}`
    }

    const opMap = {
      update: ['update'],
      enable: ['update'],
      disable: ['update'],
      onRec: ['add'],
      offRec: ['add'],
      assignMenus: ['update'],
      scheduleAttribute: ['update'],
      addEmployee: ['update'],
      removeEmployee: ['update'],
      updateSchedule: ['update'],
      deleteSchedule: ['update']
    }
    const ops = opMap[op] || [op]

    const buckets = actionOpMap.value.get(listCode)
    if (!buckets) {
      const inferred = ops.map((k) => inferByListCode(listCode, k)).filter(Boolean)
      if (inferred.length === 1) return inferred[0]
      if (inferred.length) return inferred
      return ''
    }

    const result = []
    const addCode = (code) => {
      if (!code) return
      if (!result.includes(code)) result.push(code)
    }

    ops.forEach((key) => {
      const set = buckets[key]
      if (set && set.size) {
        for (const c of set) addCode(c)
      }
    })

    if (result.length === 1) return result[0]
    if (result.length) return result

    const inferred = ops.map((k) => inferByListCode(listCode, k)).filter(Boolean)
    if (inferred.length === 1) return inferred[0]
    if (inferred.length) return inferred
    return ''
  }

  const hasMenuAccess = (permissionCode, url = '') => {
    const cleanUrl = String(url || '').trim()
    const cleanCode = String(permissionCode || '').trim()
    if (cleanCode && pagePermissionSet.value.has(cleanCode)) return true
    if (cleanUrl && pageUrlSet.value.has(cleanUrl)) return true
    if (!cleanCode && !cleanUrl) return true
    if (!cleanCode) return false
    const code = String(permissionCode).trim()
    if (!code) return true
    return permissionValidator.check(permissions.value, code)
  }

  return {
    permissions,
    pagePermissionCodes,
    pageUrls,
    routes,
    menuLoaded,
    menuTree,
    menuUrls,
    menuUrlSet,
    pagePermissionSet,
    pageUrlSet,
    menuLoadedAt,
    allPermissions,
    setPermissions,
    generatePermissionsByRoles,
    hasPermission,
    getAllConfiguredPermissions,
    resetMenu,
    setMenuTree,
    loadMenuTree,
    ensureMenuTree,
    startAutoRefresh,
    stopAutoRefresh,
    actionCodeMap,
    actionOpMap,
    getActionPermissionCode,
    hasMenuAccess
  }
})
