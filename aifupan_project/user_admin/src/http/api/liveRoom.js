/**
 * @file liveRoom.js
 * @description 直播间管理 API
 */
import http from '../httpConfig'

export default {
  /**
   * @description 分页查询直播间
   * @param {Object} data - { page, limit, platform, anchorNumber, anchorName, accountStatus, companyId, deptId, teamId, managerUserId }
   * @returns {Promise}
   */
  list: (data) => http.post('/governance/live-room/page', data),

  /**
   * @description 新增直播间
   * @param {Object} data - { platform, anchorNumber, tradeId, companyId, deptId, teamId, managerUserIds }
   * @returns {Promise}
   */
  add: (data) => http.post('/governance/live-room/add', data),

  /**
   * @description 修改直播间
   * @param {Object} data - { id, tradeId, companyId, deptId, teamId, managerUserIds }
   * @returns {Promise}
   */
  edit: (data) => http.post('/governance/live-room/update', data),

  /**
   * @description 删除直播间
   * @param {Object} data - { id }
   * @returns {Promise}
   */
  del: (data) => http.post('/governance/live-room/delete', data),

  /**
   * @description 获取指定直播间的详细信息
   * @param {Object} params - { id }
   * @returns {Promise}
   */
  detail: (params) => http.get('/governance/live-room/detail', { params }),

  /**
   * @description 启用直播间
   * @param {Object} data - { id }
   * @returns {Promise}
   */
  enable: (data) => http.post('/governance/live-room/enable', data),

  /**
   * @description 停用直播间
   * @param {Object} data - { id }
   * @returns {Promise}
   */
  disable: (data) => http.post('/governance/live-room/disable', data),

  /**
   * @description 获取直播间下拉选项
   * @param {Object} params - { limit, platform, keyword, accountStatus, companyId, deptId, teamId }
   * @returns {Promise}
   */
  options: (params) => http.get('/governance/live-room/options', { params }),

  /**
   * @description 同步租户主播到直播间
   * @param {Object} data - { platform, tradeId, managerUserIds }
   * @returns {Promise}
   */
  syncTenantAnchors: (data) => http.post('/governance/live-room/sync-tenant-anchors', data)
}
