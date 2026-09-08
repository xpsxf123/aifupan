/**
 * @file views/Login/hooks/useLoginRules.js
 * @description 登录相关表单验证规则
 */
import { reactive } from 'vue'

export function useLoginRules() {
  const commonRules = {
    mobile: [
      { required: true, message: '请输入手机号', trigger: 'blur' },
      { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
    ],
    code: [
      { required: true, message: '请输入验证码', trigger: 'blur' },
      { len: 6, message: '验证码长度为6位', trigger: 'blur' }
    ]
  }

  const mobileLoginRules = reactive({
    ...commonRules
  })

  const accountLoginRules = reactive({
    mobile: [{ required: true, message: '请输入手机号', trigger: 'blur' }],
    password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
  })

  return {
    mobileLoginRules,
    accountLoginRules
  }
}
