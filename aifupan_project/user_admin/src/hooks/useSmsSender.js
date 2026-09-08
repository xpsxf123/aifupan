/**
 * @file hooks/useSmsSender.js
 * @description Hook for SMS verification code sending and countdown logic with persistence.
 */
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'

const SMS_KEY_PREFIX = 'sms_countdown_'

/**
 * @param {string} uniqueKey - Unique key for localStorage to distinguish different SMS buttons
 * @returns {Object} { isCounting, count, send }
 */
export function useSmsSender(uniqueKey = 'default') {
  const isCounting = ref(false)
  const count = ref(60)
  const storageKey = `${SMS_KEY_PREFIX}${uniqueKey}`
  let timer = null

  /**
   * Clear timer and remove storage
   */
  const clearTimer = () => {
    if (timer) {
      clearInterval(timer)
      timer = null
    }
    isCounting.value = false
    localStorage.removeItem(storageKey)
  }

  /**
   * Start countdown timer
   * @param {number} seconds
   */
  const startTimer = (seconds) => {
    if (timer) clearInterval(timer)
    count.value = seconds
    isCounting.value = true
    timer = setInterval(() => {
      count.value--
      if (count.value <= 0) {
        clearTimer()
      }
    }, 1000)
  }

  /**
   * Check localStorage for existing countdown
   */
  const checkCache = () => {
    const targetTimeStr = localStorage.getItem(storageKey)
    if (targetTimeStr) {
      const targetTime = parseInt(targetTimeStr, 10)
      const now = Date.now()

      // Check if expired
      if (now < targetTime) {
        // Still counting down
        const remaining = Math.ceil((targetTime - now) / 1000)
        startTimer(remaining)
      } else {
        // Expired, clear it
        localStorage.removeItem(storageKey)
      }
    }
  }

  /**
   * Send SMS and start countdown
   * @param {Function} apiCall - Async function that sends the SMS
   */
  const send = async (apiCall) => {
    // Double check cache before sending (in case another tab started it or refreshed)
    const targetTimeStr = localStorage.getItem(storageKey)
    if (targetTimeStr) {
      const targetTime = parseInt(targetTimeStr, 10)
      const now = Date.now()
      if (now < targetTime) {
        ElMessage.warning('不可重复获取验证码')
        // Ensure UI reflects state
        if (!isCounting.value) {
          const remaining = Math.ceil((targetTime - now) / 1000)
          startTimer(remaining)
        }
        return
      }
    }

    if (isCounting.value) {
      ElMessage.warning('不可重复获取验证码')
      return
    }

    try {
      await apiCall()
      // Success: Start countdown
      const now = Date.now()
      const seconds = 60
      const target = now + seconds * 1000
      localStorage.setItem(storageKey, target.toString())
      startTimer(seconds)
    } catch (error) {
      console.error(error)
      // Error handling is expected to be done by the caller or apiCall if needed,
      // but we re-throw to ensure loading state is handled correctly in component
      throw error
    }
  }

  onMounted(() => {
    checkCache()
  })

  return {
    isCounting,
    count,
    send
  }
}
