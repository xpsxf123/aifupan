/**
 * @file dept.js
 * @description 部门管理 API
 */
import http from '../httpConfig'

export default {
  /**
   * @description 分页查询部门
   * @param {Object} data - { page, limit, companyId, name }
   * @returns {Promise}
   */
  list: (data) => http.post('/governance/dept/page', data),

  /**
   * @description 新增部门
   * @param {Object} data - { companyId, name, sort, managerUserIds }
   * @returns {Promise}
   */
  add: (data) => http.post('/governance/dept/add', data),

  /**
   * @description 修改部门
   * @param {Object} data - { id, companyId, name, sort, managerUserIds }
   * @returns {Promise}
   */
  edit: (data) => http.post('/governance/dept/update', data),

  /**
   * @description 删除部门
   * @param {Object} data - { id }
   * @returns {Promise}
   */
  del: (data) => http.post('/governance/dept/delete', data),

  /**
   * @description 部门下拉选择
   * @param {Object} params - { limit, companyId, keyword }
   * @returns {Promise}
   */
  options: (params) => http.get('/governance/dept/options', { params })
}
