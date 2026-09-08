/**
 * @file store/user.js
 * @description 用户相关状态管理
 */

import { defineStore } from 'pinia'
import { ref } from 'vue'
import userApi from '@/http/api/user'
import employeeProfileApi from '@/http/api/employeeProfile'

export const useUserStore = defineStore('user', () => {
  const parseStorageJson = (key, fallback) => {
    try {
      const raw = localStorage.getItem(key)
      if (!raw) return fallback
      const parsed = JSON.parse(raw)
      if (parsed === null || parsed === undefined) return fallback
      return parsed
    } catch (e) {
      return fallback
    }
  }

  const setStorageJson = (key, value) => {
    try {
      localStorage.setItem(key, JSON.stringify(value))
    } catch (e) {
      void e
    }
  }

  // 状态
  const token = ref(localStorage.getItem('token') || '')
  const roles = ref(parseStorageJson('roles', []))
  const userInfo = ref(parseStorageJson('userInfo', {}))

  // Actions
  /**
   * @description 登录
   * @param {Object} loginForm 登录表单数据
   * @returns {Promise}
   */
  const login = async (loginForm) => {
    if (loginForm.loginType === 'account') {
      const res = await userApi.enterpriseLogin({
        mobile: loginForm.mobile,
        password: loginForm.password
      })
      const accessToken = res?.data?.accessToken || ''
      token.value = accessToken
      localStorage.setItem('token', accessToken)
      userInfo.value = res?.data || {}
      setStorageJson('userInfo', userInfo.value)
      roles.value = []
      setStorageJson('roles', roles.value)
      return res?.data
    }

    const res = await userApi.codeLogin({
      mobile: loginForm.mobile,
      code: loginForm.code
    })
    const accessToken = res?.data?.accessToken || ''
    token.value = accessToken
    localStorage.setItem('token', accessToken)
    userInfo.value = res?.data || {}
    setStorageJson('userInfo', userInfo.value)
    roles.value = []
    setStorageJson('roles', roles.value)
    return res?.data
  }

  const clientAuthLogin = async (options = {}) => {
    const tokenValue = String(options?.token || '').trim()
    if (!tokenValue) return null
    const res = await userApi.clientAuthLogin({ token: tokenValue })
    const accessToken = res?.data?.accessToken || ''
    if (!accessToken) {
      throw new Error('认证登录失败')
    }
    token.value = accessToken
    localStorage.setItem('token', accessToken)
    userInfo.value = res?.data || {}
    setStorageJson('userInfo', userInfo.value)
    roles.value = []
    setStorageJson('roles', roles.value)
    return res?.data
  }

  /**
   * @description 注册
   * @returns {Promise}
   */
  const register = async () => {
    return new Promise((resolve) => {
      // 模拟注册成功
      resolve({ code: 200, message: '注册成功' })
    })
  }

  /**
   * @description 发送验证码
   * @param {Object} params { phone, type }
   * @returns {Promise}
   */
  const sendCaptcha = async (params) => {
    const mobile = params?.mobile || params?.phone || ''
    const type = params?.type || 'login'
    if (type === 'reset') {
      return employeeProfileApi.sendPasswordCode({ newMobile: mobile })
    }
    return userApi.getLoginCode({ mobile })
  }

  /**
   * @description 重置密码
   * @param {Object} resetForm 重置密码表单
   * @returns {Promise}
   */
  const resetPassword = async (resetForm) => {
    return employeeProfileApi.updatePassword({
      code: resetForm.code,
      newPassword: resetForm.newPassword
    })
  }

  /**
   * @description 获取用户信息
   */
  const getUserInfo = async () => {
    try {
      const res = await employeeProfileApi.info({ loadRoom: false })
      const data = res?.data || {}

      const org = [data.companyName, data.deptName, data.teamName].filter(Boolean).join('-') || '-'
      const jobTypeText = data.jobType === 'FULL_TIME' ? '全职' : data.jobType === 'PART_TIME' ? '兼职' : ''
      const positionText = `${data.positionName || '-'}${jobTypeText ? `（${jobTypeText}）` : ''}`
      const rooms = data.roomInfos || []
      const liveRoomText = rooms.length
        ? rooms
            .map((x) => x.anchorName || x.liveRoomName)
            .filter(Boolean)
            .join('、')
        : '-'

      userInfo.value = {
        ...data,
        employeeId: data.id ? String(data.id) : '',
        userAvatar: data.userAvatar || '',
        name: data.name || '-',
        liveRoom: liveRoomText,
        organization: org,
        position: positionText,
        phone: data.mobile || '-',
        email: data.email || '-',
        roles: ['admin']
      }
      roles.value = ['admin']
      setStorageJson('userInfo', userInfo.value)
      setStorageJson('roles', roles.value)
      return userInfo.value
    } catch (e) {
      console.error('获取个人资料失败', e)
      throw e
    }
  }

  /**
   * @description 登出
   */
  const logout = () => {
    return new Promise((resolve) => {
      token.value = ''
      roles.value = []
      userInfo.value = {}
      localStorage.removeItem('token')
      localStorage.removeItem('roles')
      localStorage.removeItem('userInfo')
      resolve()
    })
  }

  return {
    token,
    roles,
    userInfo,
    login,
    clientAuthLogin,
    register,
    sendCaptcha,
    resetPassword,
    getUserInfo,
    logout
  }
})
