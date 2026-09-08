<template>
  <div class="login-form">
    <el-tabs v-model="loginType" class="login-form__tabs" stretch>
      <el-tab-pane label="验证码登录" name="mobile">
        <el-form ref="mobileLoginFormRef" :model="mobileLoginForm" :rules="mobileLoginRules" size="large">
          <el-form-item prop="mobile">
            <el-input v-model="mobileLoginForm.mobile" placeholder="请输入手机号" prefix-icon="Iphone" clearable />
          </el-form-item>
          <el-form-item prop="code">
            <div class="login-form__code-group">
              <el-input v-model="mobileLoginForm.code" placeholder="请输入验证码" prefix-icon="Key" clearable />
              <el-button :disabled="isCounting" @click="sendCode('login')" class="login-form__send-code">
                {{ codeBtnText }}
              </el-button>
            </div>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" class="login-form__submit" :loading="loading" @click="submitLogin('mobile')">
              登录
            </el-button>
          </el-form-item>
        </el-form>
      </el-tab-pane>
      <el-tab-pane label="账号密码登录" name="account">
        <el-form ref="accountLoginFormRef" :model="accountLoginForm" :rules="accountLoginRules" size="large">
          <el-form-item prop="mobile">
            <el-input v-model="accountLoginForm.mobile" placeholder="请输入手机号" prefix-icon="User" clearable />
          </el-form-item>
          <el-form-item prop="password">
            <el-input
              v-model="accountLoginForm.password"
              type="password"
              placeholder="请输入密码"
              show-password
              prefix-icon="Lock"
              clearable
            />
          </el-form-item>
          <el-form-item>
            <div class="login-form__footer">
              <el-checkbox v-model="accountLoginForm.rememberMe" label="记住密码" />
            </div>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" class="login-form__submit" :loading="loading" @click="submitLogin('account')">
              登录
            </el-button>
          </el-form-item>
        </el-form>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
  /**
   * @file views/Login/components/LoginForm.vue
   * @description 登录表单组件
   */
  import { ref, reactive, onMounted, watch, nextTick } from 'vue'
  import { useCaptcha } from '@/hooks/useCaptcha'
  import { useLoginRules } from '../hooks/useLoginRules'
  import { useLogin } from '../hooks/useLogin'

  // 状态
  const loginType = ref('mobile')
  const mobileLoginFormRef = ref(null)
  const accountLoginFormRef = ref(null)

  // 表单数据
  const mobileLoginForm = reactive({
    mobile: '',
    code: ''
  })

  const accountLoginForm = reactive({
    mobile: '',
    password: '',
    rememberMe: false
  })

  const rememberAccountKey = 'remember_account_v1'
  const rememberMobileKey = 'remember_mobile'

  const decodePassword = (value) => {
    const raw = String(value ?? '')
    if (!raw) return ''
    try {
      return decodeURIComponent(atob(raw))
    } catch (e) {
      return raw
    }
  }

  // Hooks
  const { isCounting, codeBtnText, handleSendCode } = useCaptcha()
  const { mobileLoginRules, accountLoginRules } = useLoginRules({}) // 登录表单不需要复杂的跨字段校验，传空对象即可
  const { loading, handleLogin } = useLogin()

  // 方法
  const sendCode = (type) => {
    handleSendCode(mobileLoginForm, mobileLoginFormRef.value, type)
  }

  const submitLogin = (type) => {
    if (type === 'mobile') {
      handleLogin(mobileLoginFormRef.value, mobileLoginForm, type)
    } else {
      handleLogin(accountLoginFormRef.value, accountLoginForm, type)
    }
  }

  watch(
    loginType,
    async () => {
      await nextTick()
      mobileLoginFormRef.value?.clearValidate?.()
      accountLoginFormRef.value?.clearValidate?.()
    },
    { flush: 'post' }
  )

  // 初始化
  onMounted(() => {
    const raw = localStorage.getItem(rememberAccountKey)
    if (raw) {
      try {
        const payload = JSON.parse(raw) || {}
        const mobile = String(payload.mobile || '')
        const password = decodePassword(payload.password)
        if (mobile) accountLoginForm.mobile = mobile
        if (password) accountLoginForm.password = password
        accountLoginForm.rememberMe = Boolean(mobile || password)
      } catch (e) {
        void e
      }
    }

    if (!accountLoginForm.rememberMe) {
      const mobile = localStorage.getItem(rememberMobileKey)
      if (mobile) {
        accountLoginForm.mobile = mobile
        accountLoginForm.rememberMe = true
      }
    }

    if (accountLoginForm.rememberMe) {
      loginType.value = 'mobile'
    }
  })

  watch(
    () => accountLoginForm.rememberMe,
    (val) => {
      if (val) return
      localStorage.removeItem(rememberAccountKey)
      localStorage.removeItem(rememberMobileKey)
    }
  )
</script>

<style scoped lang="scss">
  .login-form {
    .login-form__tabs {
      margin-top: 10px;

      :deep(.el-tabs__header) {
        margin: 0 0 16px;
      }

      :deep(.el-tabs__nav-wrap::after) {
        height: 0;
      }

      :deep(.el-tabs__item) {
        font-size: 14px;
        color: #86909c;
        height: 40px;
        line-height: 40px;
      }

      :deep(.el-tabs__item.is-active) {
        color: #5e81f4;
        font-weight: 600;
      }

      :deep(.el-tabs__active-bar) {
        background-color: #5e81f4;
        height: 3px;
        border-radius: 3px;
      }
    }

    :deep(.el-input__wrapper) {
      border-radius: 18px;
      background: #f7f8fa;
      /* 你的阴影 */
      box-shadow: none;

      transition:
        background-color 0.16s ease,
        box-shadow 0.16s ease,
        border-color 0.16s ease;
    }

    :deep(.el-input__wrapper.is-focus) {
      box-shadow: 0 0 0 1px #444dff inset;
    }

    :deep(.el-input__inner) {
      height: 40px;
    }

    :deep(.el-form-item) {
      margin-bottom: 16px;
    }

    .login-form__code-group {
      display: flex;
      width: 100%;
      gap: 10px;

      .login-form__send-code {
        width: 112px;
        flex-shrink: 0;
        border-radius: 18px;
        height: 40px;
      }
    }

    .login-form__submit {
      width: 100%;
      margin-top: 10px;
      height: 44px;
      border-radius: 22px;
      border: none;
      background: linear-gradient(90deg, #3e5bf4 0%, #5e81f4 100%);

      &:hover {
        opacity: 0.92;
      }

      &:active {
        transform: translateY(1px);
      }
    }

    .login-form__footer {
      width: 100%;
      display: flex;
      justify-content: flex-start;
      align-items: center;
    }
  }
</style>
