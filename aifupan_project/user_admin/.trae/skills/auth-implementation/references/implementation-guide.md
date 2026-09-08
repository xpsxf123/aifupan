# Auth Implementation Guide

This guide provides the core code for the RBAC permission system.

## Core Logic (`src/auth/core.js`)

```javascript
export class PermissionConverter {
  convert(config, prefix = '') {
    let permissions = []
    if (Array.isArray(config)) {
      config.forEach((item) => permissions.push(prefix ? `${prefix}:${item}` : item))
      return permissions
    }
    if (typeof config === 'object' && config !== null) {
      for (const key in config) {
        const value = config[key]
        const currentCode = prefix ? `${prefix}:${key}` : key
        if (value === true) {
          permissions.push(currentCode)
        } else if (typeof value === 'string') {
          permissions.push(prefix ? `${prefix}:${value}` : value)
        } else if (Array.isArray(value)) {
          value.forEach((item) => permissions.push(`${currentCode}:${item}`))
        } else if (typeof value === 'object') {
          if (key === 'LABEL') continue
          permissions = permissions.concat(this.convert(value, currentCode))
        }
      }
    }
    return permissions
  }

  convertConfig(config) {
    const platform = config.platform || ''
    let permissions = []
    for (const key in config) {
      if (key === 'platform') continue
      permissions = permissions.concat(this.convert(config[key], platform ? `${platform}:${key}` : key))
    }
    return permissions
  }
}

export class PermissionValidator {
  check(userPermissions, requiredPermissions) {
    if (!userPermissions || !Array.isArray(userPermissions)) return false
    if (userPermissions.includes('*') || userPermissions.some((p) => p.endsWith(':*'))) {
      // Super admin logic handled in match usually, but explicit check is faster
    }
    const needed = Array.isArray(requiredPermissions) ? requiredPermissions : [requiredPermissions]
    return needed.some((need) => userPermissions.some((have) => this.match(have, need)))
  }

  match(have, need) {
    if (have === need || have === '*') return true
    const haveParts = have.split(':')
    const needParts = need.split(':')
    if (haveParts.length === 1 && haveParts[0] === '*') return true

    for (let i = 0; i < haveParts.length; i++) {
      const h = haveParts[i]
      if (h === '*') {
        if (i === haveParts.length - 1) return true
        if (i >= needParts.length) return false
        continue
      }
      if (i >= needParts.length || h !== needParts[i]) return false
    }
    return haveParts.length >= needParts.length
  }
}

export const permissionConverter = new PermissionConverter()
export const permissionValidator = new PermissionValidator()
```

## Store (`src/auth/store.js`)

```javascript
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { permissionValidator, permissionConverter } from './core'
import { permissionConfig, roleConfig } from './config'

export const usePermissionStore = defineStore('permission', () => {
  const permissions = ref([])

  const setPermissions = (perms) => {
    permissions.value = perms
  }

  const generatePermissionsByRoles = (roles) => {
    let perms = []
    if (roles.includes('super_admin')) {
      perms = ['*']
    } else {
      roles.forEach((role) => {
        if (roleConfig[role]) perms = [...perms, ...roleConfig[role]]
      })
    }
    permissions.value = [...new Set(perms)]
    return permissions.value
  }

  const hasPermission = (code) => permissionValidator.check(permissions.value, code)

  return { permissions, setPermissions, generatePermissionsByRoles, hasPermission }
})
```

## Directives (`src/auth/directives.js`)

```javascript
import { usePermissionStore } from './store'

export const auth = {
  mounted(el, binding) {
    const { value } = binding
    const permissionStore = usePermissionStore()
    if (value && !permissionStore.hasPermission(value)) {
      el.parentNode && el.parentNode.removeChild(el)
    }
  }
}
```

## Hooks (`src/auth/hooks.js`)

```javascript
import { usePermissionStore } from './store'

export function usePermission() {
  const permissionStore = usePermissionStore()
  const hasPermission = (value) => permissionStore.hasPermission(value)
  const canWrite = (codes = []) => hasPermission(codes)
  const canRead = (codes = []) => hasPermission(codes)

  return { hasPermission, canWrite, canRead, permissions: permissionStore.permissions }
}
```

## Router Guard (`src/auth/guard.js`)

```javascript
import router from '@/router'
import { usePermissionStore } from './store'
import { useUserStore } from '@/store/user' // Assuming you have one

router.beforeEach(async (to, from, next) => {
  const permissionStore = usePermissionStore()
  const userStore = useUserStore()

  if (userStore.token) {
    if (permissionStore.permissions.length === 0) {
      await userStore.getUserInfo() // Should trigger permission generation
      next({ ...to, replace: true })
    } else {
      if (to.meta.permission && !permissionStore.hasPermission(to.meta.permission)) {
        next(from.path)
      } else {
        next()
      }
    }
  } else {
    // Handle login redirect
    next()
  }
})
```

## Config Example (`src/auth/config.js`)

```javascript
export const permissionConfig = {
  platform: 'admin',
  dashboard: {
    view: true,
    MENU: true
  },
  user: {
    list: true,
    add: true,
    edit: true,
    MENU: true
  }
}

export const roleConfig = {
  super_admin: ['*'],
  visitor: ['admin:dashboard:view']
}
```

## Entry (`src/auth/index.js`)

```javascript
export * from './config'
export * from './core'
export * from './store'
export * from './hooks'
export * from './directives'
import './guard'
```
