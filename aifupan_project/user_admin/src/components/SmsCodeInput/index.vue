<template>
  <div class="sms-code-input">
    <el-input v-model="internalValue" v-bind="$attrs" placeholder="短信验证码" :maxlength="6" />
    <el-button
      :loading="loading"
      :disabled="isCounting || disabled"
      @click="handleSend"
      type="primary"
      plain
      class="verificationCodeBtn"
    >
      {{ buttonText }}
    </el-button>
  </div>
</template>

<script setup>
  import { ref, computed } from 'vue'
  import { useSmsSender } from '@/hooks/useSmsSender'
  import { ElMessage } from 'element-plus'

  /**
   * @file SmsCodeInput/index.vue
   * @description Input component for SMS verification code with countdown and persistence logic.
   */

  const props = defineProps({
    modelValue: {
      type: String,
      default: ''
    },
    phone: {
      type: String,
      default: ''
    },
    /**
     * API function to send SMS. Must return a Promise.
     * Will be called with (phone).
     */
    api: {
      type: Function,
      required: true
    },
    /**
     * Unique key for localStorage persistence.
     * Use different keys for different forms (e.g. 'login', 'register').
     */
    smsKey: {
      type: String,
      default: 'default'
    },
    disabled: {
      type: Boolean,
      default: false
    }
  })

  const emit = defineEmits(['update:modelValue'])

  const internalValue = computed({
    get: () => props.modelValue,
    set: (val) => emit('update:modelValue', val)
  })

  const { isCounting, count, send } = useSmsSender(props.smsKey)
  const loading = ref(false)

  const buttonText = computed(() => {
    if (isCounting.value) {
      return `${count.value}s后重发`
    }
    return '获取验证码'
  })

  const handleSend = async () => {
    if (!props.phone) {
      ElMessage.warning('请输入手机号')
      return
    }

    // Basic phone validation
    if (!/^1[3-9]\d{9}$/.test(props.phone)) {
      ElMessage.warning('请输入正确的手机号')
      return
    }

    loading.value = true
    try {
      // Pass a wrapper to send that calls the prop api
      await send(async () => {
        await props.api(props.phone)
      })
    } catch (error) {
      // Error handling if needed
      console.error('Failed to send SMS:', error)
    } finally {
      loading.value = false
    }
  }
</script>

<style lang="scss" scoped>
  .sms-code-input {
    width: 100%;
    display: flex;
    column-gap: 16px;
  }

  .verificationCodeBtn {
    border-radius: 56px;
    width: 94px;
  }
</style>
