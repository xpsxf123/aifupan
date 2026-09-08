/**
 * @file employeeSchedule.js
 * @description 个人排班 API
 */
import http from '../httpConfig'

export default {
  /**
   * @description 员工排班列表 (员工维度的排班详情)
   * @param {Object} data - { employeeId, startDate, endDate, ... }
   * @returns {Promise}
   */
  list: (data) => http.post('/governance/employee-schedule/list', data),

  /**
   * @description 员工排班列表 - 拆分每天 (员工维度的排班详情)
   * @param {Object} data - { employeeId, startDate, endDate, ... }
   * @returns {Promise}
   */
  listSpitDay: (data) => http.post('/governance/employee-schedule/list-spit-day', data),

  /**
   * @description 个人排班分页 (我管辖人员列表的排班)
   * @param {Object} data - { page, limit, liveRoomId, startDate, endDate, ... }
   * @returns {Promise}
   */
  page: (data) => http.post('/governance/employee-schedule/page', data)
}
