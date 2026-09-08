/**
 * @file http/api/user.js
 * @description 用户与登录相关接口（登录/验证码/登出）
 */
import http from '../httpConfig'

export default {
  /**
   * @description 企业端-账号密码登录
   * @param {Object} params - 登录参数
   * @returns {Promise}
   */
  enterpriseLogin: (params) => http.post('/governance/oauth/password', params, { showLoading: false }),

  /**
   * @description 获取登录验证码
   * @param {Object} params - { mobile: string }
   * @returns {Promise}
   */
  getLoginCode: (params) => http.post('/governance/oauth/code', params, { showLoading: false }),

  /**
   * @description 手机号 + 验证码登录
   * @param {Object} params - { mobile: string, code: string }
   * @returns {Promise}
   */
  codeLogin: (params) => http.post('/governance/oauth/code-login', params, { showLoading: false }),

  clientAuthLogin: (params = {}) => {
    const token = String(params?.token || '').trim()
    const headers = token ? { Token: token } : {}
    return http.post('/governance/oauth/client-auth', {}, { showLoading: false, headers })
  },

  /**
   * @description 登出
   * @returns {Promise}
   */
  logout: () => http.get('/governance/oauth/logout', {}, { showLoading: false })
}
