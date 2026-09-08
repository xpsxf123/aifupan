import ipConfig from '@/../ipConfig';
/**
 * 轻量级后端请求封装
 * 支持默认请求、调用时注入 baseURL、按 baseURL 生成独立请求实例。
 */
import { createRequestClient } from './request-back'

const defaultClient = createRequestClient()

/**
 * 创建自定义baseURL的请求客户端
 * @param {string} baseURL 自定义请求前缀
 * @param {Object} clientConfig axios默认配置
 * @returns {{request: Function, get: Function, post: Function, put: Function, del: Function, withBaseURL: Function}}
 */
export const createHttpBack2 = (baseURL = '', clientConfig = {}) => {
    return createRequestClient({
        ...clientConfig,
        ...(baseURL ? { baseURL } : {})
    })
}

const httpBackObject = {
    /**
     * 通用请求方法
     * @param {Object} config axios请求配置
     * @param {Object} option 扩展配置
     * @returns {Promise<any>}
     */
    request(config = {}, option = {}) {
        return defaultClient.request(config, option)
    },
    /**
     * GET请求
     * @param {string} url 请求地址
     * @param {Object} params 查询参数
     * @param {Object} option 扩展配置，可直接传入 baseURL
     * @returns {Promise<any>}
     */
    get(url, params = {}, option = {}) {
        return defaultClient.get(url, params, option)
    },
    /**
     * POST请求
     * @param {string} url 请求地址
     * @param {Object} data 请求体
     * @param {Object} option 扩展配置，可直接传入 baseURL
     * @returns {Promise<any>}
     */
    post(url, data = {}, option = {}) {
        return defaultClient.post(url, data, option)
    },
    /**
     * PUT请求
     * @param {string} url 请求地址
     * @param {Object} data 请求体
     * @param {Object} option 扩展配置，可直接传入 baseURL
     * @returns {Promise<any>}
     */
    put(url, data = {}, option = {}) {
        return defaultClient.put(url, data, option)
    },
    /**
     * DELETE请求
     * @param {string} url 请求地址
     * @param {Object} data 请求体
     * @param {Object} option 扩展配置，可直接传入 baseURL
     * @returns {Promise<any>}
     */
    del(url, data = {}, option = {}) {
        return defaultClient.del(url, data, option)
    },
    /**
     * 生成绑定baseURL的新实例
     * @param {string} baseURL 自定义请求前缀
     * @param {Object} clientConfig axios默认配置
     * @returns {{request: Function, get: Function, post: Function, put: Function, del: Function, withBaseURL: Function}}
     */
    withBaseURL(baseURL, clientConfig = {}) {
        return createHttpBack2(baseURL, clientConfig)
    },
    /**
     * 别名方法，便于按工厂方式创建实例
     * @param {string} baseURL 自定义请求前缀
     * @param {Object} clientConfig axios默认配置
     * @returns {{request: Function, get: Function, post: Function, put: Function, del: Function, withBaseURL: Function}}
     */
    create(baseURL, clientConfig = {}) {
        return createHttpBack2(baseURL, clientConfig)
    }
}

const {back2}  = ipConfig;

const customHttp = httpBackObject.withBaseURL(back2[window.SITE_CONFIG.env])

const httpBack2 = {
    ...httpBackObject,
    liveRoom: {
        /**
         * 批量获取直播间排班计划
         * @param {Array<Object>} data 请求体
         * @param {Object} option 扩展配置
         * @returns {Promise<any>}
         */
        batchPlan(data = [], option = {}) {
            return customHttp.post('/governance/client/live-room/batch-plan', data, option)
        },
        plan(data = {}, option = {}) {
            return customHttp.post('/governance/client/live-room/plan', data, option)
        },
        querySchedulePerformance(data = [], option = {}) {
            return customHttp.post('/governance/performance/video/querySchedulePerformance', data, option)
        },
        productPage(data = {}, option = {}) {
            return customHttp.post('/governance/performance/video/productPage', data, option)
        },
    }
}

export default httpBack2
