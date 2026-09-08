/**
 * @file http/api/position.js
 * @description 企业端-岗位相关接口
 */
import http from '../httpConfig'

export default {
  /**
   * @description 分页查询岗位
   * @param {Object} params - PositionPageQueryRequest
   * @returns {Promise}
   */
  list: (params) => http.post('/governance/position/list', params),

  /**
   * @description 添加岗位
   * @param {Object} params - PositionAddRequest
   * @returns {Promise}
   */
  add: (params) => http.post('/governance/position/add', params),

  /**
   * @description 修改岗位
   * @param {Object} params - PositionUpdateRequest
   * @returns {Promise}
   */
  edit: (params) => http.post('/governance/position/update', params),

  /**
   * @description 删除岗位
   * @param {Object} params - IdRequest
   * @returns {Promise}
   */
  del: (params) => http.post('/governance/position/delete', params),

  /**
   * @description 岗位下拉
   * @param {Object} params - { keyword?: string, limit?: number }
   * @returns {Promise}
   */
  options: (params) => http.get('/governance/position/options', { params })
}
