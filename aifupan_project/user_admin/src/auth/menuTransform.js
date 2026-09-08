const normalizeUrl = (url) => {
  const clean = String(url || '').trim()
  if (!clean) return ''
  if (clean.startsWith('/')) return clean
  return `/${clean}`
}

const normalizeMenuTreeInput = (value) => {
  if (Array.isArray(value)) return value
  if (value && Array.isArray(value.value)) return value.value
  return []
}

const normalizePageLookupUrl = (url) => {
  const clean = normalizeUrl(url)
  if (!clean) return ''
  if (clean.endsWith('/index')) return clean.slice(0, -'/index'.length)
  return clean
}

const joinPath = (parentPath, childPath) => {
  const parent = String(parentPath || '')
  const child = String(childPath || '')

  if (!child) return parent
  if (child.startsWith('/')) return child

  const normalizedParent = parent.endsWith('/') ? parent.slice(0, -1) : parent
  return `${normalizedParent}/${child}`
}

const sortNodes = (list) => {
  if (!Array.isArray(list)) return []
  return [...list].sort((a, b) => Number(a?.sort || 0) - Number(b?.sort || 0))
}

const cloneRouteConfig = (route) => {
  if (!route || typeof route !== 'object') return null

  const tmp = { ...route }
  if (tmp.meta && typeof tmp.meta === 'object') {
    tmp.meta = { ...tmp.meta }
  }
  if (Array.isArray(tmp.children)) {
    tmp.children = tmp.children.map(cloneRouteConfig).filter(Boolean)
  }
  return tmp
}

const mergeChildren = ({ baseChildren, extraChildren }) => {
  const base = Array.isArray(baseChildren) ? baseChildren : []
  const extra = Array.isArray(extraChildren) ? extraChildren : []

  if (!extra.length) return base

  const exists = new Set(base.map((c) => String(c?.path || '')))
  const merged = [...base]

  extra.forEach((c) => {
    const p = String(c?.path || '')
    if (!p) return
    if (exists.has(p)) return
    exists.add(p)
    merged.push(c)
  })

  return merged
}

const toRouteName = (url) => {
  const clean = normalizeUrl(url)
  if (!clean) return ''
  const parts = clean
    .split('/')
    .filter(Boolean)
    .map((x) => x.replace(/[^a-zA-Z0-9]/g, ''))
    .filter(Boolean)

  if (!parts.length) return ''
  const cap = (s) => (s ? s.charAt(0).toUpperCase() + s.slice(1) : '')
  return parts.map((p) => cap(p.toLowerCase())).join('')
}

const buildConfigIndex = (routes) => {
  const byFullPath = new Map()
  const metaByFullPath = new Map()
  const navByPermissionCode = new Map()

  const walk = (list, parentPath = '') => {
    if (!Array.isArray(list)) return
    list.forEach((r) => {
      if (!r) return
      const currentPath = joinPath(parentPath, r.path)
      if (currentPath) {
        byFullPath.set(currentPath, r)
        const meta = r.meta || {}
        metaByFullPath.set(currentPath, meta)

        const permissionCode = String(meta?.permissionCode || '').trim()
        if (permissionCode && meta?.isNav === true && !navByPermissionCode.has(permissionCode)) {
          navByPermissionCode.set(permissionCode, { fullPath: currentPath, route: r, meta })
        }
      }
      const children = Array.isArray(r.children) ? r.children : []
      if (children.length) walk(children, currentPath)
    })
  }

  walk(routes, '')

  return {
    byFullPath,
    metaByFullPath,
    navByPermissionCode
  }
}

const resolveConfigPathByPermissionCode = ({ configIndex, permissionCode, fallbackUrl }) => {
  const code = String(permissionCode || '').trim()
  const fb = normalizeUrl(fallbackUrl)
  if (!code) return fb

  const hit = configIndex?.navByPermissionCode?.get(code)
  if (hit?.fullPath) return hit.fullPath

  if (fb.endsWith('/index')) {
    const parent = fb.slice(0, -'/index'.length)
    if (configIndex?.byFullPath?.has(parent)) return parent
  }

  return fb
}

const getFirstLeafPath = (routes) => {
  if (!Array.isArray(routes)) return ''

  const invalidPaths = new Set(['/', '/login'])

  for (const r of routes) {
    if (!r) continue
    const children = Array.isArray(r.children) ? r.children : []
    if (children.length) {
      const child = getFirstLeafPath(children)
      if (child) return child
    }

    const p = String(r.path || '')
    if (p && !invalidPaths.has(p)) return p
  }

  return ''
}

const normalizePermissionSetInput = (value) => {
  if (value instanceof Set) return value
  if (value && value.value instanceof Set) return value.value
  return new Set()
}

export const buildRouteConfigFromPermissionSet = ({ menuConfig, allowedPermissionSet }) => {
  const allowedSet = normalizePermissionSetInput(allowedPermissionSet)

  const joinPathLocal = (parentPath, childPath) => {
    const parent = String(parentPath || '')
    const child = String(childPath || '')
    if (!child) return parent
    if (child.startsWith('/')) return child
    const normalizedParent = parent.endsWith('/') ? parent.slice(0, -1) : parent
    return `${normalizedParent}/${child}`
  }

  const cloneAll = (node) => cloneRouteConfig(node)

  const allowedNavPathSet = (() => {
    const set = new Set()
    const traverse = (routes, parentPath = '') => {
      if (!Array.isArray(routes)) return
      routes.forEach((r) => {
        if (!r) return
        const meta = r.meta || {}
        const permissionCode = String(meta.permissionCode || '').trim()
        const isNav = meta.isNav === true
        const currentPath = joinPathLocal(parentPath, r.path)
        if (isNav && permissionCode && allowedSet.has(permissionCode) && currentPath) {
          set.add(currentPath)
        }
        if (Array.isArray(r.children) && r.children.length) {
          traverse(r.children, currentPath)
        }
      })
    }
    traverse(menuConfig, '')
    return set
  })()

  const walk = (routes, parentPath = '', forceIncludeSubtree = false) => {
    const res = []
    if (!Array.isArray(routes)) return res

    routes.forEach((r) => {
      if (!r) return
      const meta = r.meta || {}
      const permissionCode = String(meta.permissionCode || '').trim()
      const isNav = meta.isNav === true
      const currentPath = joinPathLocal(parentPath, r.path)

      if (forceIncludeSubtree) {
        res.push(cloneAll(r))
        return
      }

      const isAllowedNav = isNav && permissionCode && allowedSet.has(permissionCode)
      if (isAllowedNav) {
        res.push(cloneAll(r))
        return
      }

      const activeMenu = String(meta.activeMenu || '').trim()
      const activeMenuMatch = activeMenu && allowedNavPathSet.has(activeMenu)
      if (activeMenuMatch) {
        res.push(cloneAll(r))
        return
      }

      const children = walk(r.children, currentPath, isAllowedNav)
      if (children.length) {
        const tmp = cloneRouteConfig(r)
        tmp.children = children
        res.push(tmp)
      }
    })

    return res
  }

  return walk(menuConfig, '')
}

const createFallbackPageRoute = ({ parentUrl, node }) => {
  const fullUrl = normalizeUrl(node?.url)
  const parent = normalizeUrl(parentUrl)
  const relative = parent && fullUrl.startsWith(`${parent}/`) ? fullUrl.slice(parent.length + 1) : fullUrl

  return {
    path: relative || '',
    name: toRouteName(fullUrl) || undefined,
    component: 'views/404',
    meta: {
      title: node?.name || '未配置页面',
      isNav: true,
      permissionCode: node?.permissionCode || ''
    }
  }
}

const toChildPath = ({ parentUrl, fullUrl }) => {
  const parent = normalizeUrl(parentUrl)
  const full = normalizeUrl(fullUrl)
  if (!full) return ''

  if (parent && full.startsWith(`${parent}/`)) {
    return full.slice(parent.length + 1)
  }

  return full
}

export const buildRouteConfigFromMenuTree = ({ menuTree, menuConfig }) => {
  const tree = sortNodes(normalizeMenuTreeInput(menuTree))
  const configIndex = buildConfigIndex(menuConfig)

  const buildNode = ({ node, parentUrl = '' }) => {
    if (!node) return null

    const url = normalizeUrl(node.url)
    const type = Number(node.type)
    const children = sortNodes(node.children)

    if (type === 1) return null

    if (type === 0) {
      const normalizedLookupUrl = normalizePageLookupUrl(url)
      const configNode = configIndex.byFullPath.get(url) || configIndex.byFullPath.get(normalizedLookupUrl)
      if (configNode) {
        const tmp = cloneRouteConfig(configNode)
        tmp.path = toChildPath({ parentUrl, fullUrl: url })
        if (normalizedLookupUrl !== url && tmp.name) {
          tmp.name = `${tmp.name}Index`
        }
        tmp.meta = { ...(tmp.meta || {}) }
        tmp.meta.title = node.name || tmp.meta.title
        tmp.meta.isNav = true
        if (node.permissionCode) tmp.meta.permissionCode = node.permissionCode

        const configChildren = Array.isArray(configNode.children)
          ? configNode.children.map(cloneRouteConfig).filter(Boolean)
          : []
        if (configChildren.length) {
          tmp.children = mergeChildren({
            baseChildren: Array.isArray(tmp.children) ? tmp.children : [],
            extraChildren: configChildren
          })
        }

        return tmp
      }

      return createFallbackPageRoute({ parentUrl, node })
    }

    const configNode = configIndex.byFullPath.get(url)
    const tmp = configNode ? cloneRouteConfig(configNode) : null
    const group = tmp || {
      path: url,
      name: toRouteName(url) || undefined,
      component: 'Layout',
      meta: {
        title: node.name || '',
        isNav: true
      },
      children: []
    }

    group.path = url
    group.meta = { ...(group.meta || {}) }
    group.meta.title = node.name || group.meta.title
    group.meta.isNav = true
    if (node.permissionCode) group.meta.permissionCode = node.permissionCode

    if (Array.isArray(children) && children.length) {
      const rawComponent = group.component
      const isLayout = rawComponent === 'Layout' || rawComponent === 'layout' || rawComponent === '@/layout/index.vue'
      if (!isLayout) {
        group.component = 'Layout'
      }
    }

    if (
      configNode &&
      Array.isArray(configNode.children) &&
      configNode.children.length &&
      group.children &&
      group.children.length
    ) {
      const extraChildren = configNode.children.map(cloneRouteConfig).filter(Boolean)
      group.children = mergeChildren({ baseChildren: group.children, extraChildren })
    }

    const pageChildren = children.map((c) => buildNode({ node: c, parentUrl: url })).filter(Boolean)

    group.children = pageChildren

    if (pageChildren.length) {
      const rawName = String(group.name || '')
      if (rawName && !rawName.endsWith('Parent')) {
        group.name = `${rawName}Parent`
      }
    }

    if (!group.redirect && pageChildren.length) {
      const firstChild = pageChildren[0]
      const childPath = String(firstChild.path || '')
      const redirectUrl = childPath.startsWith('/') ? childPath : `${url}/${childPath}`
      group.redirect = redirectUrl
    }

    return group
  }

  return tree.map((node) => buildNode({ node, parentUrl: '' })).filter(Boolean)
}

export const buildSidebarMenuTree = ({ menuTree, menuConfig, permissionStore }) => {
  const tree = sortNodes(normalizeMenuTreeInput(menuTree))
  const configIndex = buildConfigIndex(menuConfig)

  const pickIcon = (url) => {
    const meta = configIndex.metaByFullPath.get(url)
    return meta?.icon || ''
  }

  const buildNode = (node) => {
    if (!node) return null
    const type = Number(node.type)
    if (type === 1) return null

    const url = normalizeUrl(node.url)
    const permissionCode = String(node.permissionCode || '').trim()
    const allowed = !permissionCode || permissionStore.hasMenuAccess(permissionCode, url)
    if (!allowed) return null

    const resolvedPath = resolveConfigPathByPermissionCode({
      configIndex,
      permissionCode,
      fallbackUrl: url
    })

    const children = sortNodes(node.children).map(buildNode).filter(Boolean)

    if (type !== 0 && children.length === 0) return null

    return {
      path: resolvedPath,
      meta: {
        title: node.name || '',
        icon: pickIcon(resolvedPath || url),
        isNav: true,
        permissionCode
      },
      children
    }
  }

  return tree.map(buildNode).filter(Boolean)
}

export const getFirstAccessibleUrlFromMenuTree = ({ menuTree, permissionStore, menuConfig }) => {
  const configIndex = menuConfig ? buildConfigIndex(menuConfig) : null
  const walk = (list) => {
    if (!Array.isArray(list)) return ''

    for (const node of sortNodes(list)) {
      if (!node) continue
      const type = Number(node.type)
      if (type === 1) continue

      const url = normalizeUrl(node.url)
      const permissionCode = String(node.permissionCode || '').trim()
      if (permissionCode && !permissionStore.hasMenuAccess(permissionCode, url)) continue

      const children = Array.isArray(node.children) ? node.children : []
      const child = walk(children)
      if (child) return child

      if (type === 0 && url) {
        if (configIndex) {
          return resolveConfigPathByPermissionCode({
            configIndex,
            permissionCode,
            fallbackUrl: url
          })
        }
        return url
      }
    }

    return ''
  }

  return walk(normalizeMenuTreeInput(menuTree))
}

export const getFirstAccessiblePathByMenu = ({ menuTree, menuConfig, permissionStore }) => {
  const sidebarTree = buildSidebarMenuTree({ menuTree, menuConfig, permissionStore })
  return getFirstLeafPath(sidebarTree)
}
