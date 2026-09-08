/**
 * @file employee.js
 * @description 员工管理 API
 */
import http from '../httpConfig'

export default {
  /**
   * @description 分页查询人员
   * @param {Object} data - { page, limit, companyIds, deptIds, teamIds, name, staffNumber, mobile, positionIds, onRec, jobType, accountStatus }
   * @returns {Promise}
   */
  list: (data) => http.post('/governance/employee/page', data),

  /**
   * @description 无功能权限的特殊分页（用于需要绕开菜单/功能权限控制的场景）
   * @param {Object} data - { page, limit, companyIds, deptIds, teamIds, name, staffNumber, mobile, positionIds, onRec, jobType, accountStatus, openMain }
   * @returns {Promise}
   */
  specialPage: (data) => http.post('/governance/employee/special-page', data),

  /**
   * @description 新增人员
   * @param {Object} data - { name, userAvatar, staffNumber, mobile, email, companyId, deptId, teamId, positionId, onRec, roleId }
   * @returns {Promise}
   */
  add: (data) => http.post('/governance/employee/add', data),

  /**
   * @description 修改人员
   * @param {Object} data - { id, name, userAvatar, staffNumber, mobile, email, companyId, deptId, teamId, positionId, roleId }
   * @returns {Promise}
   */
  edit: (data) => http.post('/governance/employee/update', data),

  /**
   * @description 启用人员
   * @param {Object} data - { id }
   * @returns {Promise}
   */
  enable: (data) => http.post('/governance/employee/enable', data),

  /**
   * @description 禁用人员
   * @param {Object} data - { id }
   * @returns {Promise}
   */
  disable: (data) => http.post('/governance/employee/disable', data),

  /**
   * @description 开启录制权限
   * @param {Object} data - { id }
   * @returns {Promise}
   */
  onRec: (data) => http.post('/governance/employee/on-rec', data),

  /**
   * @description 关闭录制权限
   * @param {Object} data - { id }
   * @returns {Promise}
   */
  offRec: (data) => http.post('/governance/employee/off-rec', data),

  /**
   * @description 人员详情
   * @param {Object} params - { employId }
   * @returns {Promise}
   */
  detail: (params) => http.get('/governance/employee/detail', { params }),

  /**
   * @description 搜索下拉
   * @param {Object} params - { keyword, limit }
   * @returns {Promise}
   */
  options: (params) => http.get('/governance/employee/option', { params }),

  /**
   * @description 同步子账号
   * @returns {Promise}
   */
  syncSubAccount: () => http.post('/governance/employee/sync-sub-account')
}
