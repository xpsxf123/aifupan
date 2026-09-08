/**
 * @file views/Login/hooks/useLogin.js
 * @description 登录逻辑Hook
 */
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/store/user'
import { usePermissionStore } from '@/auth/store'
import { menuConfig } from '@/config/menu'
import { ensureDynamicRoutes } from '@/router/dynamicRoutes'
import { getFirstAccessiblePathByMenu } from '@/auth/menuTransform'
import {ElMessage} from 'element-plus'

const rememberAccountKey = 'remember_account_v1'
const rememberMobileKey = 'remember_mobile'

export function useLogin() {
  const userStore = useUserStore()
  const permissionStore = usePermissionStore()
  const router = useRouter()
  const route = useRoute()
  const loading = ref(false)

  const encodePassword = (value) => {
    const raw = String(value ?? '')
    if (!raw) return ''
    try {
      return btoa(encodeURIComponent(raw))
    } catch (e) {
      return raw
    }
  }

  const handleLogin = async (formRef, formData, loginType) => {
    if (!formRef) return
    await formRef.validate(async (valid) => {
      if (valid) {
        loading.value = true
        try {
          const params = {
            loginType
          }
          if (loginType === 'mobile') {
            params.mobile = formData.mobile
            params.code = formData.code
          } else {
            params.mobile = formData.mobile
            params.password = formData.password
          }

          await userStore.login(params)

          try {
            await permissionStore.ensureMenuTree({ force: true, maxAgeMs: 0 })
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

          // 记住密码逻辑
          if (loginType === 'account') {
            if (formData.rememberMe) {
              const payload = {
                mobile: String(formData.mobile || ''),
                password: encodePassword(formData.password)
              }
              localStorage.setItem(rememberAccountKey, JSON.stringify(payload))
              localStorage.setItem(rememberMobileKey, payload.mobile)
            } else {
              localStorage.removeItem(rememberAccountKey)
              localStorage.removeItem(rememberMobileKey)
            }
          }

          ElMessage.success('登录成功')
          const rawRedirect = route.query.redirect
          const redirect = typeof rawRedirect === 'string' && rawRedirect.startsWith('/') ? rawRedirect : ''
          if (redirect && redirect !== '/' && redirect !== '/login') {
            router.push(redirect)
            return
          }

          const first =
            getFirstAccessiblePathByMenu({
              menuTree: permissionStore.menuTree,
              menuConfig,
              permissionStore
            }) || '/403'

          router.push(first)
        } catch (e) {
          void e
        } finally {
          loading.value = false
        }
      }
    })
  }

  return {
    loading,
    handleLogin
  }
}
