/**
 * @file employeePerformance.js
 * @description 人员业绩统计相关接口（列表/汇总/趋势/明细）
 */
import http from '../httpConfig'

export default {
  /**
   * @description 人员业绩分页查询
   * @param {Object} data - EmployeePerformancePageRequest
   * @returns {Promise}
   */
  page: (data) => http.post('/governance/performance/employee/page', data),

  /**
   * @description 人员业绩时段统计（六时段）
   * @param {Object} data - EmployeePeriodStatsRequest
   * @returns {Promise}
   */
  periodStats: (data) => http.post('/governance/performance/employee/period-stats', data),

  /**
   * @description 人员业绩汇总统计
   * @param {Object} data - EmployeePeriodStatsRequest
   * @returns {Promise}
   */
  summary: (data) => http.post('/governance/performance/employee/summary', data),

  /**
   * @description 人员业绩趋势数据
   * @param {Object} data - EmployeeTrendRequest
   * @returns {Promise}
   */
  trend: (data) => http.post('/governance/performance/employee/trend', data),

  /**
   * @description 员工业绩按天维度分页查询
   * @param {Object} data - EmployeeDailyPerformanceRequest
   * @returns {Promise}
   */
  dailyPage: (data) => http.post('/governance/performance/employee/daily/page', data),

  /**
   * @description 员工业绩按班次维度分页查询
   * @param {Object} data - EmployeeSchedulePerformanceRequest
   * @returns {Promise}
   */
  schedulePage: (data) => http.post('/governance/performance/employee/schedule/page', data)
}
