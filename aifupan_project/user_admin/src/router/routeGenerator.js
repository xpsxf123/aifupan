import { h } from 'vue'

const viewModules = import.meta.glob('../views/**/*.vue')

const resolveViewComponent = (component) => {
  if (component === 'Layout') {
    return () => import('@/layout/index.vue')
  }

  if (typeof component !== 'string') {
    return component
  }

  const key = `../${component}.vue`
  if (viewModules[key]) return viewModules[key]

  const indexKey = `../${component}/index.vue`
  if (viewModules[indexKey]) return viewModules[indexKey]

  console.warn(`Component not found: ${key} or ${indexKey}`)

  return {
    render() {
      return h('div', { style: 'color:red;padding:20px;' }, `Component not found: ${component}`)
    }
  }
}

const shouldWrapLayout = (route, isRoot) => {
  if (!isRoot) return false
  if (route.path === '/login') return false
  if (route.children && route.children.length) return false
  return Boolean(route.meta?.isNav) && route.component !== 'Layout'
}

export const generateRoutes = (routes, isRoot = false) => {
  const res = []

  routes.forEach((route) => {
    const tmp = { ...route }

    if (!tmp.component) {
      if (tmp.meta?.useTemplate !== false && !tmp.meta?.notLayout) {
        tmp.component = 'Layout'
      }
    }

    if (typeof tmp.component === 'string') {
      if (shouldWrapLayout(tmp, isRoot)) {
        const pageComponent = resolveViewComponent(tmp.component)
        tmp.component = resolveViewComponent('Layout')
        tmp.children = [
          {
            path: '',
            name: tmp.name ? `${tmp.name}Index` : undefined,
            component: pageComponent,
            meta: tmp.meta
          }
        ]
      } else {
        tmp.component = resolveViewComponent(tmp.component)
      }
    }

    if (tmp.children) {
      tmp.children = generateRoutes(tmp.children, false)
    }

    res.push(tmp)
  })

  return res
}
