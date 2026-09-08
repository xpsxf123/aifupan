/**
 * @file http/api/employeeProfile.js
 * @description 员工个人档案相关接口
 */
import http from '../httpConfig'

export default {
  /**
   * @description 个人资料
   * @param {Object} params - { loadRoom: boolean } 是否查询所属直播间信息
   * @returns {Promise}
   */
  info: (params) => http.get('/governance/employee-profile/info', { params }),

  /**
   * @description 员工资料 (排班详情页顶部等使用)
   * @param {Object} params - { employeeId: number, loadRoom?: boolean }
   * @returns {Promise}
   */
  base: (params) => http.get('/governance/employee-profile/base', { params }),

  /**
   * @description 修改个人资料
   * @param {Object} data - { name: string, email?: string, userAvatar?: string }
   * @returns {Promise}
   */
  updateInfo: (data) => http.post('/governance/employee-profile/info', data),

  /**
   * @description 发送密码重置验证码
   * @param {Object} data - { newMobile: string } 新手机号码
   * @returns {Promise}
   */
  sendPasswordCode: (data) => http.post('/governance/employee-profile/send-password-code', data),

  /**
   * @description 修改密码
   * @param {Object} data - { code: string, newPassword: string }
   * @returns {Promise}
   */
  updatePassword: (data) => http.post('/governance/employee-profile/update-password', data),

  /**
   * @description 修改列表人员密码
   * @param {Object} data - { employeeId: string, newPassword: string }
   * @returns {Promise}
   */
  updatePersonPassword: (data) => http.post('/governance/employee/password', data),

  /**
   * @description 发送绑定手机号码验证码
   * @param {Object} data - { newMobile: string } 新手机号码
   * @returns {Promise}
   */
  sendBindCode: (data) => http.post('/governance/employee-profile/send-bind-code', data),

  /**
   * @description 获取图片预上传链接
   * @param {Object} params - { suffix: string }
   * @returns {Promise}
   */
  getImagePresignedUpload: (params) => http.get('/governance/oss/image/presigned-upload', { params }),

  /**
   * @description 更改手机号码
   * @param {Object} data - { code: string, newMobile: string }
   * @returns {Promise}
   */
  updateMobile: (data) => http.post('/governance/employee-profile/update-mobile', data)
}
