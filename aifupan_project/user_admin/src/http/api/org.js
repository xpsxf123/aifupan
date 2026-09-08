/**
 * @file org.js
 * @description 组织架构相关接口
 */
import http from '../httpConfig'

export default {
  /**
   * @description 获取组织架构树
   * @returns {Promise}
   */
  tree(data) {
    return http.get('/governance/org/tree', { params: data })
  }
}
