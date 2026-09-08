/**
 * @file team.js
 * @description 小组管理 API
 */
import http from '../httpConfig'

export default {
  /**
   * @description 分页查询小组
   * @param {Object} data - { page, limit, deptId, name }
   * @returns {Promise}
   */
  list: (data) => http.post('/governance/team/page', data),

  /**
   * @description 新增小组
   * @param {Object} data - { deptId, name, sort, managerUserIds }
   * @returns {Promise}
   */
  add: (data) => http.post('/governance/team/add', data),

  /**
   * @description 修改小组
   * @param {Object} data - { id, deptId, name, sort, managerUserIds }
   * @returns {Promise}
   */
  edit: (data) => http.post('/governance/team/update', data),

  /**
   * @description 删除小组
   * @param {Object} data - { id }
   * @returns {Promise}
   */
  del: (data) => http.post('/governance/team/delete', data),

  /**
   * @description 小组下拉选择
   * @param {Object} params - { limit, deptId, keyword }
   * @returns {Promise}
   */
  options: (params) => http.get('/governance/team/options', { params })
}
