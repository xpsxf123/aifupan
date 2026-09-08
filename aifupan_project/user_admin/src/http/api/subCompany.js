/**
 * @file subCompany.js
 * @description 子公司管理 API
 */
import http from '../httpConfig'

export default {
  /**
   * @description 分页查询子公司
   * @param {Object} data - 查询参数 { page, limit, name }
   * @returns {Promise}
   */
  list: (data) => http.post('/governance/sub-company/page', data),

  /**
   * @description 新增子公司
   * @param {Object} data - 子公司数据 { name, sort, managerUserIds }
   * @returns {Promise}
   */
  add: (data) => http.post('/governance/sub-company/add', data),

  /**
   * @description 修改子公司
   * @param {Object} data - 子公司数据 { id, name, sort, managerUserIds }
   * @returns {Promise}
   */
  edit: (data) => http.post('/governance/sub-company/update', data),

  /**
   * @description 删除子公司
   * @param {Object} data - { id }
   * @returns {Promise}
   */
  del: (data) => http.post('/governance/sub-company/delete', data),

  /**
   * @description 子公司下拉选择
   * @param {Object} params - { limit, keyword }
   * @returns {Promise}
   */
  options: (params) => http.get('/governance/sub-company/options', { params })
}
