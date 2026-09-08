/**
 * @file hooks/useCaptcha.js
 * @description 验证码发送与倒计时Hook
 */
import { ref, computed, onUnmounted, onMounted } from 'vue'
import { useUserStore } from '@/store/user'
import { ElMessage } from 'element-plus'

const CAPTCHA_END_TIME_KEY = 'app_captcha_endtime'

export function useCaptcha() {
  const userStore = useUserStore()
  const isCounting = ref(false)
  const count = ref(60)
  const codeBtnText = computed(() => (isCounting.value ? `${count.value}s后重发` : '获取验证码'))
  let timer = null

  /**
   * @description 清除定时器和状态
   */
  const clearTimer = () => {
    if (timer) {
      clearInterval(timer)
      timer = null
    }
    isCounting.value = false
    localStorage.removeItem(CAPTCHA_END_TIME_KEY)
  }

  /**
   * @description 执行倒计时逻辑
   */
  const runTimer = () => {
    timer = setInterval(() => {
      if (count.value > 0) {
        count.value--
      } else {
        clearTimer()
      }
    }, 1000)
  }

  /**
   * @description 开始倒计时
   * @param {number} [initialCount=60] 初始秒数
   */
  const startCountDown = (initialCount = 60) => {
    isCounting.value = true
    count.value = initialCount

    // 记录结束时间戳
    const endTime = Date.now() + initialCount * 1000
    localStorage.setItem(CAPTCHA_END_TIME_KEY, endTime.toString())

    runTimer()
  }

  /**
   * @description 检查缓存中的倒计时
   */
  const checkCache = () => {
    const endTimeStr = localStorage.getItem(CAPTCHA_END_TIME_KEY)
    if (endTimeStr) {
      const endTime = parseInt(endTimeStr, 10)
      const now = Date.now()
      if (endTime > now) {
        const remaining = Math.ceil((endTime - now) / 1000)
        isCounting.value = true
        count.value = remaining
        runTimer()
      } else {
        localStorage.removeItem(CAPTCHA_END_TIME_KEY)
      }
    }
  }

  /**
   * @description 发送验证码
   * @param {Object} formModel 表单数据对象
   * @param {Object} formRef 表单引用
   * @param {string} type 业务类型 'login' | 'register' | 'reset'
   */
  const handleSendCode = async (formModel, formRef, type) => {
    // 校验手机号
    try {
      await formRef.validateField('mobile')
    } catch (error) {
      return
    }

    if (isCounting.value) return

    try {
      await userStore.sendCaptcha({ phone: formModel.mobile, type })
      ElMessage.success('验证码发送成功')
      startCountDown()
    } catch (e) {
      void e
    }
  }

  onMounted(() => {
    checkCache()
  })

  onUnmounted(() => {
    if (timer) {
      clearInterval(timer)
      // 注意：这里只清除定时器，不清除缓存和状态，以便恢复
    }
  })

  return {
    isCounting,
    codeBtnText,
    handleSendCode
  }
}
