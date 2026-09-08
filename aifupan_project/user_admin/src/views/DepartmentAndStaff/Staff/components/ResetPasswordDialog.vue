<template>
  <el-dialog
    :model-value="visible"
    width="440px"
    class="reset-password-dialog common-dialog"
    title="重置密码"
    :close-on-click-modal="false"
    @update:model-value="handleVisibleChange"
    @closed="handleClosed"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="reset-password">
      <el-form-item prop="phone" label="">
        <el-input v-model="form.phone" disabled class="rounded" />
      </el-form-item>

      <!--      <el-form-item prop="code" label="">
        <SmsCodeInput
          v-model="form.code"
          :phone="form.phone"
          :api="sendCode"
          :smsKey="smsKey"
          class="rounded"
          placeholder="短信验证码"
        />
      </el-form-item>-->

      <el-form-item prop="newPassword" label="">
        <el-input
          v-model="form.newPassword"
          type="password"
          show-password
          placeholder="请输入至少6位的密码"
          class="rounded"
        />
      </el-form-item>

      <el-form-item prop="confirmPassword" label="" class="repeat-password">
        <el-input
          v-model="form.confirmPassword"
          type="password"
          show-password
          placeholder="再次输入新密码确认"
          class="rounded"
        />
      </el-form-item>

      <el-form-item>
        <el-button
          v-auth="'sys:employee:manage:update'"
          class="submit-btn"
          type="primary"
          :loading="loading"
          @click="handleSubmit"
        >
          确定修改
        </el-button>
      </el-form-item>
    </el-form>
  </el-dialog>
</template>

<script setup>
  /**
   * @file ResetPasswordDialog.vue
   * @description 人员管理-重置密码弹窗（短信验证码 + 新密码校验）
   */
  import { computed, nextTick, ref, watch } from 'vue'
  import { ElMessage } from 'element-plus'
  import SmsCodeInput from '@/components/SmsCodeInput/index.vue'
  import apiModule from '@/http/api'

  const props = defineProps({
    visible: { type: Boolean, default: false },
    row: { type: Object, default: () => ({}) }
  })

  const emit = defineEmits(['update:visible', 'success'])

  const formRef = ref(null)
  const loading = ref(false)
  const form = ref({
    phone: '',
    code: '',
    newPassword: '',
    confirmPassword: '',
    employeeId: ''
  })

  const smsKey = computed(() => {
    const phone = form.value.phone || 'unknown'
    return `reset_password_${phone}`
  })

  const initForm = () => {
    const phone = props.row?.mobile
    const employeeId = props.row?.id
    form.value = {
      phone,
      code: '',
      employeeId,
      newPassword: '',
      confirmPassword: ''
    }
  }

  const validateConfirmPassword = (rule, value, callback) => {
    if (!value) {
      callback(new Error('请再次输入新密码'))
      return
    }
    if (value !== form.value.newPassword) {
      callback(new Error('两次输入的密码不一致'))
      return
    }
    callback()
  }

  const rules = computed(() => {
    return {
      code: [{ required: true, message: '请输入短信验证码', trigger: 'blur' }],
      newPassword: [
        { required: true, message: '请输入新密码', trigger: 'blur' },
        { min: 6, message: '密码至少6位', trigger: 'blur' }
      ],
      confirmPassword: [{ validator: validateConfirmPassword, trigger: 'blur' }]
    }
  })

  const handleVisibleChange = (val) => {
    emit('update:visible', val)
  }

  const handleClosed = () => {
    if (formRef.value) {
      formRef.value.resetFields()
    }
  }

  const sendCode = async (phone) => {
    if (!phone) return
    await apiModule.employeeProfile.sendPasswordCode({ newMobile: phone })
    ElMessage.success('验证码已发送')
  }

  const handleSubmit = async () => {
    if (!formRef.value) return
    await formRef.value.validate(async (valid) => {
      if (!valid) return
      loading.value = true
      try {
        await apiModule.employeeProfile.updatePersonPassword({
          // code: form.value.code,
          employeeId: form.value.employeeId,
          newPassword: form.value.newPassword
        })
        ElMessage.success('密码重置成功')
        emit('success')
        handleVisibleChange(false)
      } catch (e) {
        console.error(e)
      } finally {
        loading.value = false
      }
    })
  }

  watch(
    () => props.visible,
    (val) => {
      if (!val) return
      initForm()
      nextTick(() => {
        formRef.value?.clearValidate()
      })
    }
  )
</script>

<style scoped>
  .reset-password-dialog :deep(.el-form-item) {
    margin-bottom: 14px;
  }

  .reset-password-dialog :deep(.el-form-item__label) {
    padding: 0;
  }

  .submit-btn {
    width: 100%;
    border-radius: 18px;
    height: 36px;
  }

  .reset-password {
    width: 280px;
  }
  .el-form-item {
    margin-bottom: 24px;
  }

  .repeat-password {
    margin-bottom: 30px;
  }
</style>

<style>
  .reset-password-dialog .el-dialog__body {
    display: flex;
    justify-content: center;
  }
</style>
