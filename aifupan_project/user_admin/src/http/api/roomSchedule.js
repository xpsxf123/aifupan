/**
 * @file roomSchedule.js
 * @description 直播间排班 API
 */
import http from '../httpConfig'

export default {
  /**
   * @description 新增直播间排班
   * @param {Object} data - { liveRoomId, workDay, sessions, batchConfig }
   * @returns {Promise}
   */
  add: (data, options = {}) => http.post('/governance/room-schedule/add', data, options),

  /**
   * @description 新增直播间排班（原始 JSON 字符串，兼容 int64 大整数精度）
   * @param {string} json - JSON 字符串
   * @param {Object} options - axios options
   * @returns {Promise}
   */
  addJson: (json, options = {}) =>
    http.post('/governance/room-schedule/add', json, {
      ...options,
      headers: { 'Content-Type': 'application/json', ...(options.headers || {}) }
    }),

  /**
   * @description 分页查询排班数据
   * @param {Object} data - { page, limit, liveRoomId, startDate, endDate, employeeId, positionId }
   * @returns {Promise}
   */
  page: (data) => http.post('/governance/room-schedule/list', data),

  /**
   * @description 查询指定时间范围排班数据
   * @param {Object} data - { liveRoomId, startDate, endDate, employeeId, positionId }
   * @returns {Promise}
   */
  listRange: (data) => http.post('/governance/room-schedule/list-range', data),

  /**
   * @description 查询指定时间范围排班数据 - 拆分每天
   * @param {Object} data - { liveRoomId, startDate, endDate, employeeId, positionId }
   * @returns {Promise}
   */
  listRangeSpitDay: (data) => http.post('/governance/room-schedule/list-range-spit-day', data),

  /**
   * @description 添加排班人员
   * @param {Object} data - { scheduleId, liveRoomId, employeeId, positionId }
   * @returns {Promise}
   */
  addEmployee: (data) => http.post('/governance/room-schedule/add-employee', data),

  /**
   * @description 移除排班人员
   * @param {Object} data - { scheduleId, liveRoomId, employeeId }
   * @returns {Promise}
   */
  removeEmployee: (data) => http.post('/governance/room-schedule/remove-employee', data),

  /**
   * @description 修改排班时间
   * @param {Object} data - { id, liveRoomId, startWork, endWork, scheduleDuration, restDuration }
   * @returns {Promise}
   */
  update: (data) => http.post('/governance/room-schedule/update', data),

  /**
   * @description 查询指定排班详情
   * @param {Object} params - { roomScheduleId }
   * @param {Object} options - axios options
   * @returns {Promise}
   */
  detail: (params, options = {}) => http.get('/governance/room-schedule/detail', { params, ...options }),

  /**
   * @description 删除排班
   * @param {Object} data - { id, liveRoomId }
   * @returns {Promise}
   */
  del: (data) => http.post('/governance/room-schedule/delete', data),

  /**
   * @description 获取直播间排班配置
   * @param {Object} params - { id }
   * @returns {Promise}
   */
  getAttribute: (params) => http.get('/governance/live-room/schedule-attribute', { params }),

  /**
   * @description 设置直播间排班配置
   * @param {Object} data - { id, startPlan, endPlan, shiftOptions, restOptions, positionOptions }
   * @returns {Promise}
   */
  setAttribute: (data) => http.post('/governance/live-room/schedule-attribute', data),

  /**
   * @description 下载直播间排班导入模板（.xlsx）
   * @param {Object} params - { liveRoomId }
   * @returns {Promise<Blob>}
   */
  downloadTemplate: (params) => http.get('/governance/room-schedule/import-template', { params, responseType: 'blob' }),

  /**
   * @description 批量导入直播间排班（前端解析后的结构）
   * @param {Object} data - { liveRoomId, schedules: [{ workDay, startWork, endWork, scheduleDuration, restDuration, employees: [{ employeeId, positionId }] }] }
   * @returns {Promise}
   */
  importSchedule: (data) => http.post('/governance/room-schedule/import', data)
}
