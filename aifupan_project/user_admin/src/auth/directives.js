/**
 * @file auth/directives.js
 * @description 权限指令
 */
import { usePermissionStore } from './store'
import { watch, effectScope } from 'vue'

/**
 * v-auth 指令
 * 用法: v-auth="'admin:user:add'" 或 v-auth="['admin:user:add', 'admin:user:edit']"
 */
export const auth = {
  mounted(el, binding) {
    const { value } = binding
    const permissionStore = usePermissionStore()

    if (!value) return

    if (typeof value === 'string' || Array.isArray(value)) {
      const scope = effectScope()
      el.__authScope = scope
      scope.run(() => {
        watch(
          () => permissionStore.hasPermission(value),
          (hasPerm) => {
            el.style.display = hasPerm ? '' : 'none'
          },
          { immediate: true }
        )
      })
    }
  },
  unmounted(el) {
    if (el.__authScope) {
      el.__authScope.stop()
      el.__authScope = null
    }
  }
}

// 可以扩展 v-write, v-read 等，逻辑类似，只是可能语义不同
export const write = {
  mounted(el, binding) {
    const { value } = binding
    const permissionStore = usePermissionStore()
    // 假设 v-write 也接受权限码
    if (!value) return
    if (value) {
      const scope = effectScope()
      el.__writeScope = scope
      scope.run(() => {
        watch(
          () => permissionStore.hasPermission(value),
          (hasPerm) => {
            el.style.display = hasPerm ? '' : 'none'
          },
          { immediate: true }
        )
      })
    }
  },
  unmounted(el) {
    if (el.__writeScope) {
      el.__writeScope.stop()
      el.__writeScope = null
    }
  }
}

export const read = {
  mounted(el, binding) {
    const { value } = binding
    const permissionStore = usePermissionStore()
    if (!value) return
    if (value) {
      const scope = effectScope()
      el.__readScope = scope
      scope.run(() => {
        watch(
          () => permissionStore.hasPermission(value),
          (hasPerm) => {
            el.style.display = hasPerm ? '' : 'none'
          },
          { immediate: true }
        )
      })
    }
  },
  unmounted(el) {
    if (el.__readScope) {
      el.__readScope.stop()
      el.__readScope = null
    }
  }
}
