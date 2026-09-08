/**
 * @file http/httpConfig/index.js
 * @description HTTP 请求封装：统一 Token 注入、分页参数适配、全局 Loading、错误提示与 401 跳转登录
 */

import axios from 'axios'
import { ElLoading, ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'
import router from '@/router'

/**
 * @description 规范化 baseURL：保证以 /api 结尾
 * @param {string} raw - 原始 baseURL
 * @returns {string}
 */
const normalizeBaseURL = (raw) => {
  if (!raw) return '/api'
  const base = String(raw).trim().replace(/\/+$/, '')
  if (!base) return '/api'
  if (base.endsWith('/api')) return base
  if (base === '/api') return base
  return `${base}/api`
}

/**
 * @description 规范化分页参数：对常见的分页 POST 接口补齐 page/limit
 * @param {Object} config - Axios 请求配置
 * @returns {Object}
 */
const normalizePageParams = (config) => {
  const method = (config.method || 'get').toLowerCase()
  if (method !== 'post') return config
  const data = config.data
  if (!data || typeof data !== 'object') return config
  if (data instanceof FormData) return config

  const url = config.url || ''
  const isPaged = /\/(list|page)$/.test(url)
  if (!isPaged && data.page === undefined && data.limit === undefined && data.pageSize === undefined) return config

  if (data.page === undefined) data.page = 1
  if (data.limit === undefined) data.limit = data.pageSize !== undefined ? data.pageSize : 10
  if (data.pageSize !== undefined && data.limit === undefined) data.limit = data.pageSize
  return config
}

let loadingCount = 0
let loadingInstance = null

/**
 * @description 判断是否为普通对象
 * @param {*} value - 任意值
 * @returns {boolean}
 */
const isPlainObject = (value) => {
  return Object.prototype.toString.call(value) === '[object Object]'
}

const requestConfigKeys = [
  'params',
  'data',
  'headers',
  'timeout',
  'baseURL',
  'responseType',
  'signal',
  'showLoading',
  'loadingText'
]

/**
 * @description 判断对象是否为“请求配置对象”（而不是业务 params/data）
 * @param {*} value - 任意值
 * @returns {boolean}
 */
const isRequestConfig = (value) => {
  if (!isPlainObject(value)) return false
  return requestConfigKeys.some((key) => Object.prototype.hasOwnProperty.call(value, key))
}

/**
 * @description 统一规范化额外请求选项
 * @param {Object} [options={}] - 额外选项
 * @returns {Object}
 */
const normalizeRequestOptions = (options = {}) => {
  const config = isPlainObject(options) ? { ...options } : {}
  config.showLoading = config.showLoading === true
  return config
}

/**
 * @description 开启全局 Loading（支持并发计数）
 * @param {string} text - Loading 文案
 */
const startLoading = (text) => {
  if (loadingCount === 0 && !loadingInstance) {
    loadingInstance = ElLoading.service({
      fullscreen: true,
      lock: true,
      text: text || '加载中...'
    })
  }
  loadingCount += 1
}

/**
 * @description 关闭全局 Loading（支持并发计数）
 */
const endLoading = () => {
  loadingCount -= 1
  if (loadingCount <= 0) {
    loadingCount = 0
    if (loadingInstance) {
      loadingInstance.close()
      loadingInstance = null
    }
  }
}

/**
 * @description 创建请求实例：封装拦截器与 get/post/put/delete 调用方式
 * @param {string} baseURL - 接口地址
 * @returns {import('axios').AxiosInstance}
 */
const createService = (baseURL) => {
  const service = axios.create({
    baseURL: normalizeBaseURL(baseURL),
    timeout: 5000 // 请求超时时间
  })

  // 请求拦截器
  service.interceptors.request.use(
    (config) => {
      config = normalizePageParams(config)
      const userStore = useUserStore()
      if (userStore.token) {
        config.headers['Token'] = userStore.token
      }
      const shouldShowLoading = config.showLoading === true
      if (shouldShowLoading) {
        config.__showLoading = true
        startLoading(config.loadingText)
      }
      return config
    },
    (error) => {
      return Promise.reject(error)
    }
  )

  // 响应拦截器
  service.interceptors.response.use(
    (response) => {
      if (response.config && response.config.__showLoading) {
        endLoading()
      }
      const res = response.data
      const hasCode = res && Object.prototype.hasOwnProperty.call(res, 'code')
      const successCode = res?.code === 0 || res?.code === 200
      if (hasCode && !successCode) {
        const message = res.msg || res.message || '请求失败'
        const isSilent = Boolean(response?.config?.silent)
        const isAuthCode = res.code === 401 || res.code === 4001
        const isPermissionDenied = String(message || '').includes('权限不足')

        if (!isSilent) {
          ElMessage({
            message,
            type: 'error',
            duration: 5 * 1000
          })
        }

        if (isAuthCode && !isPermissionDenied) {
          const userStore = useUserStore()
          userStore.logout().then(() => {
            router.push({ path: '/login', query: { redirect: router.currentRoute.value.fullPath } })
          })
        }

        const bizError = new Error(message)
        bizError.code = res.code
        bizError.response = {
          status: response?.status,
          data: res,
          config: response?.config,
          headers: response?.headers
        }
        return Promise.reject(bizError)
      }
      return res
    },
    (error) => {
      if (error && error.config && error.config.__showLoading) {
        endLoading()
      }

      const message = error?.response?.data?.msg || error?.response?.data?.message || error?.message || '请求失败'
      const isSilent = Boolean(error?.config?.silent)
      const httpStatus = Number(error?.response?.status)
      const bizCode = Number(error?.response?.data?.code)
      const isAuthCode = httpStatus === 401 || bizCode === 401 || bizCode === 4001
      const isPermissionDenied = String(message || '').includes('权限不足')

      if (!isSilent) {
        ElMessage({
          message,
          type: 'error',
          duration: 5 * 1000
        })
      }

      if (isAuthCode && !isPermissionDenied) {
        const userStore = useUserStore()
        userStore.logout().then(() => {
          router.push({ path: '/login', query: { redirect: router.currentRoute.value.fullPath } })
        })
      }

      return Promise.reject(error)
    }
  )

  const rawGet = service.get.bind(service)
  const rawPost = service.post.bind(service)
  const rawPut = service.put.bind(service)
  const rawDelete = service.delete.bind(service)

  service.get = (url, paramsOrConfig = {}, options = {}) => {
    const requestOptions = normalizeRequestOptions(options)
    if (isRequestConfig(paramsOrConfig)) {
      return rawGet(url, { ...paramsOrConfig, ...requestOptions })
    }
    return rawGet(url, { params: paramsOrConfig, ...requestOptions })
  }

  service.post = (url, data = {}, options = {}) => {
    return rawPost(url, data, normalizeRequestOptions(options))
  }

  service.put = (url, data = {}, options = {}) => {
    return rawPut(url, data, normalizeRequestOptions(options))
  }

  service.delete = (url, dataOrConfig = {}, options = {}) => {
    const requestOptions = normalizeRequestOptions(options)
    if (isRequestConfig(dataOrConfig)) {
      return rawDelete(url, { ...dataOrConfig, ...requestOptions })
    }
    return rawDelete(url, { data: dataOrConfig, ...requestOptions })
  }

  return service
}

/**
 * @description 服务端接口请求实例（企业管理后台）
 */
const httpBack = createService(import.meta.env.VITE_APP_BASE_API || '/api')

export { httpBack }
export default httpBack
