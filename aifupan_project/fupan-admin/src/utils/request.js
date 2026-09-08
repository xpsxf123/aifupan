import axios from 'axios'
import { ElLoading } from 'element-plus'

import { withSignatureHeaders } from '@/utils/withSignatureHeaders'
import { clearLoginInfo } from './index'
import { customSvg } from '@/utils/icon.js'
import { useUserInfoStore } from '@/store'
import router from '@/router/index.js'
// 全局 loading 管理
let loadingCount = 0
let loadingInstance = null
const http = axios.create({
    baseURL: import.meta.env.VITE_BASE_URL,
    timeout: 1000 * 120
})

http.interceptors.request.use(
    (config) => {
        // 处理特定URL的超时设置
        if (config.url.includes('/aianalysis/aiAnalysis')) {
            config.timeout = 1000 * 60 * 3 // 单独设置超时为3分钟
        }

        // 处理loading显示逻辑 - 只有显式设置 showLoading: true 才显示
        const shouldShowLoading = config.showLoading === true
        if (shouldShowLoading) {
            loadingCount++
            if (loadingCount === 1) {
                // 只在第一个需要loading的请求时创建
                loadingInstance = ElLoading.service({
                    lock: true,
                    background: 'rgba(38,50,56,.7)',
                    svg: customSvg
                })
            }
        }

        // 添加签名头部和token
        const userInfoStore = useUserInfoStore()
        config.headers.Token = userInfoStore.token || ''
        config = withSignatureHeaders(config)

        return config
    },
    (error) => {
        // 请求错误时也要减少计数
        const shouldShowLoading = error.config?.showLoading === true
        if (shouldShowLoading) {
            loadingCount = Math.max(0, loadingCount - 1)
            if (loadingCount === 0 && loadingInstance) {
                loadingInstance.close()
                loadingInstance = null
            }
        }
        return Promise.reject(error)
    }
)

// 响应拦截
http.interceptors.response.use(
    (res) => {
        // 关闭loading
        const shouldShowLoading = res.config.showLoading === true
        if (shouldShowLoading) {
            loadingCount = Math.max(0, loadingCount - 1)
            if (loadingCount === 0 && loadingInstance) {
                loadingInstance.close()
                loadingInstance = null
            }
        }
        if (res.status === 200 && res.data.code === 0) {
            return res.data
        } else {
            let resErr_text = ''
            if (res.status !== 200) {
                resErr_text = '与服务器网络连接断开，请稍后重试或联系管理员'
            } else {
                resErr_text = res.data.msg || '请求返回信息错误'
            }

            if (res.data.code === 4001) {
                clearLoginInfo()
                router.replace({name: 'login'})
            }
            ElMessage.error(resErr_text)
            return Promise.reject(res.data)
        }
    },
    (error) => {
        // 响应错误时也要减少计数
        const shouldShowLoading = error.config?.showLoading === true
        if (shouldShowLoading) {
            loadingCount = Math.max(0, loadingCount - 1)
            if (loadingCount === 0 && loadingInstance) {
                loadingInstance.close()
                loadingInstance = null
            }
        }
        let err_txt = ''
        if (error.message.indexOf('timeout of') !== -1) {
            err_txt = '网络较差,请再试一次'
        } else if (error.message.indexOf('Network Error') !== -1) {
            err_txt = '网络较差,请再试一次'
        }
        ElMessage.error(err_txt)
        return Promise.reject(error)
    }
)

/**
 * Get 方法，对应 GET 请求
 * @param {string} url - 请求的 URL 地址
 * @param {Record<string, any>} params - 请求时携带的参数
 * @param {Object} options - 请求配置选项
 * @param {boolean} options.showLoading - 默认false，需要显式设置为true才显示
 * @returns {Promise<any>} - 返回请求结果的 Promise
 */
export const get = (url, params, options = {}) => {
    return http.get(url, {
        params,
        showLoading: options.showLoading
    })
}
/**
 * POST 方法，对应 POST 请求
 * @param {string} url - 请求的 URL 地址
 * @param {Record<string, any>} data - 请求时携带的参数
 * @param {Object} options - 请求配置选项
 * @param {boolean} options.showLoading - 默认false，需要显式设置为true才显示
 * @returns {Promise<any>} - 返回请求结果的 Promise
 */
export const post = (url, data, options = {}) => {
    return http.post(url, data, {
        showLoading: options.showLoading
    })
}

export const put = (url, data, options = {}) => {
    return http.put(
        url,
        {},
        {
            params: data,
            showLoading: options.showLoading
        }
    )
}

export const del = (url, data, options = {}) => {
    return http.delete(url, {
        data,
        showLoading: options.showLoading
    })
}
