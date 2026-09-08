/**
 * @file trade.js
 * @description 行业管理 API
 */
import http from '../httpConfig'

export default {
  /**
   * @description 获取行业树
   * @param {Object} params - 查询参数
   * @returns {Promise}
   */
  tree: (params) => http.get('/governance/trade/tree', { params })
}
