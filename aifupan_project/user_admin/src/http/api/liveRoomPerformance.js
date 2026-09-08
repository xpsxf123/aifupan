/**
 * @file liveRoomPerformance.js
 * @description 直播间业绩与排班业绩相关接口
 */
import http from '../httpConfig'

export default {
  /**
   * @description 直播间的业绩分页列表
   * @param {Object} data - LiveRoomQueryRequest
   * @returns {Promise}
   */
  pageLiveRoomPerformance: (data) => http.post('/governance/performance/live-room/pageLiveRoomPerformance', data),

  /**
   * @description 按天维度统计排班业绩
   * @param {Object} data - DailyPerformanceStatsRequest
   * @returns {Promise}
   */
  dailyStats: (data) => http.post('/governance/performance/live-room/dailyStats', data),

  /**
   * @description 详细的排班业绩分页查询
   * @param {Object} data - SchedulePerformancePageRequest
   * @returns {Promise}
   */
  pageQuerySchedule: (data) => http.post('/governance/performance/live-room/pageQuerySchedule', data),

  /**
   * @description 单个班次业绩详情查询
   * @param {number|string} id - 班次业绩ID
   * @returns {Promise}
   */
  schedulePerformanceDetail: (id) => http.get(`/governance/performance/live-room/schedulePerformance/${id}`),

  /**
   * @description 单个班次业绩保存/修改
   * @param {Object} data - SchedulePerformanceSaveRequest
   * @returns {Promise}
   */
  schedulePerformanceSave: (data) => http.post('/governance/performance/live-room/schedulePerformanceSave', data),

  /**
   * @description 删除班次业绩
   * @param {Object} data - IdRequest
   * @returns {Promise}
   */
  deleteSchedulePerformance: (data) => http.post('/governance/performance/live-room/deleteSchedulePerformance', data),

  /**
   * @description 排班列表查询
   * @param {Object} data - ScheduleListRequest
   * @returns {Promise}
   */
  scheduleList: (data) => http.post('/governance/performance/live-room/scheduleList', data)
}
