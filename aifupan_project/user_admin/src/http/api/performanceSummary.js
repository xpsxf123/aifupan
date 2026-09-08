/**
 * @file performanceSummary.js
 * @description 业绩汇总相关接口
 */
import http from '../httpConfig'

export default {
  /**
   * @description 获取组织数量统计
   * @param {Object} data - { sourceId, sourceType }
   * @returns {Promise}
   */
  orgCount: (data) => http.post('/governance/performance/summary/org/count', data),

  /**
   * @description 各分公司业绩数据分页查询
   * @param {Object} data - { name, startDate, endDate, page, limit }
   * @returns {Promise}
   */
  subCompanyPage: (data) => http.post('/governance/performance/summary/sub-company/page', data),

  /**
   * @description 各部门业绩数据分页查询
   * @param {Object} data - { name, companyId, startDate, endDate, page, limit }
   * @returns {Promise}
   */
  deptPage: (data) => http.post('/governance/performance/summary/dept/page', data),

  /**
   * @description 各小组业绩数据分页查询
   * @param {Object} data - { name, companyId, deptId, startDate, endDate, page, limit }
   * @returns {Promise}
   */
  teamPage: (data) => http.post('/governance/performance/summary/team/page', data),

  /**
   * @description 各直播间业绩数据分页查询
   * @param {Object} data - { name, companyId, deptId, teamId, startDate, endDate, page, limit }
   * @returns {Promise}
   */
  liveRoomPage: (data) => http.post('/governance/performance/summary/live-room/page', data),

  /**
   * @description 获取业绩时段统计
   * @param {Object} data - { sourceId, sourceType }
   * @returns {Promise}
   */
  periodStats: (data) => http.post('/governance/performance/summary/period-stats', data),

  /**
   * @description 获取数据趋势（柱形图）
   * @param {Object} data - { sourceId, sourceType, startDate, endDate }
   * @returns {Promise}
   */
  trend: (data) => http.post('/governance/performance/summary/trend', data),

  /**
   * @description 获取数据详情分页列表
   * @param {Object} data - { sourceId, sourceType, startDate, endDate, page, limit }
   * @returns {Promise}
   */
  dailyPage: (data) => http.post('/governance/performance/summary/daily/page', data),

  /**
   * @description 获取销售额汇总
   * @param {Object} data - { dimensionType, companyId, deptId, teamId, startDate, endDate }
   * @returns {Promise}
   */
  salesRevenueSummary: (data) => http.post('/governance/performance/summary/sales-revenue/summary', data)
}
