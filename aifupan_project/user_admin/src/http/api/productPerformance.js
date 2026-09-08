/**
 * @file productPerformance.js
 * @description 商品业绩相关接口（商品排行、关联直播、来源组织）
 */
import http from '../httpConfig'

export default {
  /**
   * @description 商品排行分页查询
   * @param {Object} data - ProductRankingRequest
   * @returns {Promise}
   */
  ranking: (data) => http.post('/governance/performance/product/ranking', data),

  /**
   * @description 商品关联直播场次分页查询
   * @param {Object} data - ProductSessionRequest
   * @returns {Promise}
   */
  sessions: (data) => http.post('/governance/performance/product/sessions', data),

  /**
   * @description 商品关联分公司列表查询
   * @param {Object} data - ProductCompanyRequest
   * @returns {Promise}
   */
  companies: (data) => http.post('/governance/performance/product/companies', data)
}
