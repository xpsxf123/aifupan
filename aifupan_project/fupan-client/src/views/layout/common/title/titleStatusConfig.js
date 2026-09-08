/**
 * @description 标题栏状态角标配置。
 * 用于统一维护网络状态、第三方授权状态、平台信息和本地缓存键名。
 */
import douyinIcon from '@/assets/imgs/icon/dyLogo.png'

/**
 * @description 网络状态缓存键
 * @type {string}
 */
export const NETWORK_STATUS_STORAGE_KEY = 'title_network_status'

/**
 * @description 第三方授权状态缓存键前缀
 * @type {string}
 */
export const THIRD_PARTY_AUTH_STORAGE_PREFIX = 'title_third_party_auth_status'

/**
 * @description 网络状态映射
 * @type {Object<string, {text: string, tone: string}>}
 */
export const NETWORK_STATUS_MAP = {
    good: {
        text: '网络良好',
        tone: 'good'
    },
    exhausted: {
        text: '网络疲惫',
        tone: 'exhausted'
    },
    poor: {
        text: '网络不佳',
        tone: 'poor'
    }
}

/**
 * @description 第三方平台映射
 * @type {Object<number, {label: string, icon: string, authorizeUrl: string}>}
 */
export const THIRD_PARTY_PLATFORM_MAP = {
    0: {
        label: '抖音',
        icon: douyinIcon,
        authorizeUrl: 'https://buyin.jinritemai.com/dashboard'
    }
}

/**
 * @description 第三方授权状态映射
 * @type {Object<string, {tone: string, suffix: string, canAuthorize: boolean}>}
 */
export const THIRD_PARTY_AUTH_STATUS_MAP = {
    success: {
        tone: 'success',
        suffix: '已授权',
        canAuthorize: false
    },
    fail: {
        tone: 'fail',
        suffix: '授权失效',
        canAuthorize: true
    },
    wait: {
        tone: 'wait',
        suffix: '授权',
        canAuthorize: true
    }
}

/**
 * @description 获取第三方授权状态缓存键
 * @param {number|string} platform 平台标识
 * @returns {string}
 */
export const getThirdPartyAuthStorageKey = (platform) => {
    return `${THIRD_PARTY_AUTH_STORAGE_PREFIX}_${platform}`
}
