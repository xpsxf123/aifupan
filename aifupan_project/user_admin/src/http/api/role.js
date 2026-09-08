/**
 * @file http/api/role.js
 * @description 角色与菜单相关接口
 */
import http from '../httpConfig'

export default {
  /**
   * @description 分页查询角色
   * @param {Object} params - RolePageQueryRequest
   * @returns {Promise}
   */
  list: (params) => http.post('/governance/role/list', params),

  /**
   * @description 新增角色
   * @param {Object} params - RoleAddRequest
   * @returns {Promise}
   */
  add: (params) => http.post('/governance/role/add', params),

  /**
   * @description 修改角色
   * @param {Object} params - RoleUpdateRequest
   * @returns {Promise}
   */
  edit: (params) => http.post('/governance/role/update', params),

  /**
   * @description 删除角色
   * @param {Object} params - IdRequest
   * @returns {Promise}
   */
  del: (params) => http.post('/governance/role/delete', params),

  /**
   * @description 获取角色的菜单列表
   * @param {Object} params - { id: number }
   * @returns {Promise}
   */
  detail: (params) => http.get('/governance/role/detail', { params }),

  /**
   * @description 给角色分配菜单
   * @param {Object} params - RoleAssignMenuRequest
   * @returns {Promise}
   */
  assignMenus: (params) => http.post('/governance/role/assign-menus', params),

  /**
   * @description 获取当前登录用户菜单树
   * @param {Object} [options] - 请求选项（如 { silent: true }）
   * @returns {Promise}
   */
  userMenuTree: (options = {}) => http.get('/governance/user-menu/tree', {}, options),

  /**
   * @description 角色下拉选择
   * @param {Object} params - { keyword, limit }
   * @returns {Promise}
   */
  options: (params) => http.get('/governance/role/options', { params })
}
