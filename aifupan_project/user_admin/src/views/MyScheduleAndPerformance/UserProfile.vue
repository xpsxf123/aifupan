<template>
  <div class="user-profile">
    <div class="user-profile__header">
      <div class="header-title text-main">头像</div>
      <div class="user-profile__avatar-box">
        <div class="avtar-container">
          <el-avatar :size="100" :src="avatarUrl || defaultImg" class="user-profile__avatar" />
        </div>
        <el-button link type="primary" :loading="avatarUploading" @click="handleAvatarTrigger">修改头像</el-button>
        <input
          ref="avatarInputRef"
          class="user-profile__avatar-input"
          type="file"
          accept=".jpg,.jpeg,.png,.webp"
          @change="handleAvatarChange"
        />
      </div>
    </div>

    <div class="user-profile__content">
      <SchemaForm
        ref="formRef"
        :model-value="formData"
        :schema="schema"
        :state="pageState"
        :options="{
          labelPosition: 'top',
          gutter: 62
        }"
        @update:modelValue="(val) => Object.assign(formData, val || {})"
        @action-click="handleActionClick"
      />
    </div>

    <CommonDialog v-model="changePhoneVisible" title="重新绑定" width="440px">
      <el-form
        class="change-phone-form"
        ref="changePhoneFormRef"
        :model="changePhoneForm"
        :rules="changePhoneRules"
        label-position="top"
      >
        <el-form-item>
          <el-input v-model="formData.phone" placeholder="原手机号" disabled />
        </el-form-item>
        <el-form-item prop="phone">
          <el-input v-model="changePhoneForm.phone" placeholder="请输入绑定手机号" />
        </el-form-item>
        <el-form-item prop="code">
          <SmsCodeInput
            v-model="changePhoneForm.code"
            :phone="changePhoneForm.phone"
            :api="sendPhoneCode"
            sms-key="change-phone"
          />
        </el-form-item>
        <el-form-item class="confirm-btn-container">
          <el-button type="primary" @click="handleChangePhoneSubmit" class="confirm-btn"> 确定换绑 </el-button>
        </el-form-item>
      </el-form>
    </CommonDialog>

    <CommonDialog v-model="resetPasswordVisible" title="重置密码" width="440px">
      <el-form
        ref="resetPasswordFormRef"
        :model="resetPasswordForm"
        :rules="resetPasswordRules"
        class="user-profile__password-form"
      >
        <el-form-item prop="phone">
          <el-input v-model="formData.phone" disabled placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item prop="code">
          <SmsCodeInput
            v-model="resetPasswordForm.code"
            :phone="formData.phone"
            :api="sendPasswordCode"
            sms-key="reset-password"
            placeholder="短信验证码"
          />
        </el-form-item>
        <el-form-item prop="newPassword">
          <el-input
            v-model="resetPasswordForm.newPassword"
            type="password"
            placeholder="请输入至少6位的密码"
            show-password
          />
        </el-form-item>
        <el-form-item prop="confirmPassword">
          <el-input
            v-model="resetPasswordForm.confirmPassword"
            type="password"
            placeholder="再次输入新密码确认"
            show-password
          />
        </el-form-item>
        <el-form-item class="update-confirm-btn">
          <el-button type="primary" @click="handleResetPasswordSubmit" class="confirm-btn"> 确定修改 </el-button>
        </el-form-item>
      </el-form>
    </CommonDialog>
  </div>
</template>

<script setup>
  /**
   * @file UserProfile.vue
   * @description 用户个人资料页面，包含头像展示和分组表单
   */
  import { ref, reactive, onMounted } from 'vue'
  import { ElMessage } from 'element-plus'
  import SchemaForm from '@/components/SchemaForm/index.vue'
  import CommonDialog from '@/components/CommonDialog/index.vue'
  import SmsCodeInput from '@/components/SmsCodeInput/index.vue'
  import { useUserStore } from '@/store/user'
  import api from '@/http/api/index.js'
  import defaultImg from '@/assets/images/funnel/defaultAvatar.png'

  const userStore = useUserStore()

  const pageState = ref('view')
  const avatarUrl = ref('')
  const avatarUploading = ref(false)
  const profileSubmitting = ref(false)
  const avatarInputRef = ref(null)
  const formRef = ref(null)

  const formData = reactive({
    name: '',
    phone: '',
    email: '',
    password: '******',
    organization: '',
    role: '',
    recordingPermission: '未开启',
    positionName: ''
  })

  const fetchProfileInfo = async () => {
    try {
      const res = await api.employeeProfile.info({ loadRoom: false })
      if (res && res.code === 0 && res.data) {
        const data = res.data
        formData.name = data.name || ''
        formData.phone = data.mobile || ''
        formData.email = data.email || ''
        const orgParts = []
        if (data.companyName) orgParts.push(data.companyName)
        if (data.deptName) orgParts.push(data.deptName)
        if (data.teamName) orgParts.push(data.teamName)
        formData.organization = orgParts.join('-') || '-'
        formData.role = data.roleName || '-'
        formData.recordingPermission = data.onRec ? '已开启' : '未开启'
        formData.positionName = data.positionName || '-'
        if (data.userAvatar) {
          avatarUrl.value = data.userAvatar
        }

        userStore.userInfo.name = formData.name
        userStore.userInfo.userAvatar = data.userAvatar || ''
        userStore.userInfo.phone = formData.phone
        userStore.userInfo.email = formData.email
      }
    } catch (error) {
      console.error('获取个人资料失败:', error)
    }
  }

  onMounted(() => {
    fetchProfileInfo()
  })

  const changePhoneVisible = ref(false)
  const resetPasswordVisible = ref(false)

  const changePhoneForm = reactive({
    phone: '',
    code: ''
  })

  const resetPasswordForm = reactive({
    code: '',
    newPassword: '',
    confirmPassword: ''
  })

  const changePhoneRules = {
    phone: [{ required: true, pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }],
    code: [{ required: true, message: '请输入验证码', trigger: 'blur' }]
  }

  const resetPasswordRules = {
    code: [{ required: true, message: '请输入验证码', trigger: 'blur' }],
    newPassword: [
      { required: true, message: '请输入新密码', trigger: 'blur' },
      { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
    ],
    confirmPassword: [
      { required: true, message: '请再次输入密码', trigger: 'blur' },
      {
        validator: (rule, value, callback) => {
          if (value !== resetPasswordForm.newPassword) {
            callback(new Error('两次输入密码不一致'))
          } else {
            callback()
          }
        },
        trigger: 'blur'
      }
    ]
  }

  const changePhoneFormRef = ref(null)
  const resetPasswordFormRef = ref(null)

  const buildProfilePayload = (extraData = {}) => {
    const payload = {
      name: String(formData.name || '').trim(),
      userAvatar: avatarUrl.value,
      ...extraData
    }

    const email = String(formData.email || '').trim()
    if (email) {
      payload.email = email
    }
    return payload
  }

  const submitProfile = async (payload, successMessage = '保存成功') => {
    if (!payload.name) {
      ElMessage.error('请输入姓名')
      return false
    }
    if (profileSubmitting.value) {
      return false
    }
    profileSubmitting.value = true
    try {
      const res = await api.employeeProfile.updateInfo(payload)
      if (res && res.code === 0) {
        ElMessage.success(successMessage)
        await fetchProfileInfo()
        return true
      }
      return false
    } catch (error) {
      console.error('保存个人资料失败:', error)
      return false
    } finally {
      profileSubmitting.value = false
    }
  }

  const validateProfileField = async (prop) => {
    const innerFormRef = formRef.value?.formRef
    if (!innerFormRef) {
      return true
    }
    try {
      await innerFormRef.validateField(prop)
      return true
    } catch {
      return false
    }
  }

  const handleProfileSave = async ({ prop, actions }) => {
    const valid = await validateProfileField(prop)
    if (!valid) {
      return
    }
    const isSuccess = await submitProfile(buildProfilePayload())
    if (isSuccess) {
      actions.view()
    }
  }

  const handleAvatarTrigger = () => {
    avatarInputRef.value?.click()
  }

  const handleAvatarChange = async (event) => {
    const file = event.target?.files?.[0]
    if (!file) {
      return
    }
    if (!/^image\/(jpeg|png|webp)$/.test(file.type)) {
      ElMessage.error('仅支持 JPG、PNG、WEBP 格式图片')
      event.target.value = ''
      return
    }
    if (file.size > 5 * 1024 * 1024) {
      ElMessage.error('图片大小不能超过 5MB')
      event.target.value = ''
      return
    }
    avatarUploading.value = true
    try {
      const suffix = file.name.includes('.') ? file.name.slice(file.name.lastIndexOf('.')) : '.png'
      const presignedRes = await api.employeeProfile.getImagePresignedUpload({ suffix })
      const uploadInfo = presignedRes?.data || {}
      if (!presignedRes || presignedRes.code !== 0 || !uploadInfo.uploadUrl || !uploadInfo.ossKey) {
        ElMessage.error(presignedRes?.msg || '获取上传地址失败')
        return
      }

      const avatarAccessUrl = String(uploadInfo.uploadUrl || '').split('?')[0]
      if (!avatarAccessUrl) {
        ElMessage.error('获取头像地址失败')
        return
      }

      const parseOssError = (rawText) => {
        const codeMatch = rawText.match(/<Code>([^<]+)<\/Code>/)
        const messageMatch = rawText.match(/<Message>([^<]+)<\/Message>/)
        const requestIdMatch = rawText.match(/<RequestId>([^<]+)<\/RequestId>/)
        return {
          code: codeMatch?.[1] || '',
          message: messageMatch?.[1] || '',
          requestId: requestIdMatch?.[1] || ''
        }
      }

      const uploadResponse = await fetch(uploadInfo.uploadUrl, {
        method: 'PUT',
        body: await file.arrayBuffer()
      })
      if (!uploadResponse.ok) {
        let text = ''
        try {
          text = await uploadResponse.text()
        } catch {
          text = ''
        }
        const ossError = parseOssError(text || '')
        const msgParts = ['头像上传失败', String(uploadResponse.status)]
        if (ossError.code) msgParts.push(ossError.code)
        if (ossError.requestId) msgParts.push(`requestId=${ossError.requestId}`)
        ElMessage.error(msgParts.join(' '))
        return
      }

      const saved = await submitProfile(buildProfilePayload({ userAvatar: avatarAccessUrl }), '头像保存成功')
      if (saved) {
        avatarUrl.value = avatarAccessUrl
      }
    } catch (error) {
      console.error('上传头像失败:', error)
      if (error?.response || error?.config) return
      ElMessage.error(error?.message || '上传头像失败')
    } finally {
      avatarUploading.value = false
      event.target.value = ''
    }
  }

  const handleActionClick = ({ prop, button, actions }) => {
    if (button.text === '修改') {
      actions.edit()
      return
    }

    if (button.text === '换绑') {
      changePhoneVisible.value = true
      return
    }

    if (button.text === '重置密码') {
      resetPasswordForm.code = ''
      resetPasswordForm.newPassword = ''
      resetPasswordForm.confirmPassword = ''
      resetPasswordVisible.value = true
      return
    }

    if (button.text === '保存') {
      if (prop === 'name' || prop === 'email') {
        handleProfileSave({ prop, actions })
      }
      return
    }
  }

  // 表单配置 Schema
  const schema = [
    {
      title: '基本信息',
      children: [
        {
          prop: 'name',
          label: '姓名',
          placeholder: '请输入姓名',
          required: true,
          maxlength: 20,
          span: 12,
          readonly: false,
          disabled: ['view'],
          actionButton: [
            {
              show: ['view'],
              icon: 'Edit',
              text: '修改',
              action: 'nameEdit',
              type: 'primary'
            },
            {
              show: ['edit'],
              icon: 'Check',
              text: '保存',
              action: 'nameSave',
              type: 'success',
              confirm: {
                title: '确认修改姓名吗？',
                confirmButtonText: '确定',
                cancelButtonText: '取消'
              }
            }
          ]
        },
        {
          prop: 'phone',
          label: '电话',
          placeholder: '请输入电话号码',
          required: true,
          span: 12,
          readonly: false,
          disabled: ['view'],
          rules: [{ pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码', trigger: 'blur' }],
          actionButton: [
            {
              icon: 'Refresh',
              text: '换绑',
              type: 'primary'
            }
          ]
        },
        {
          prop: 'email',
          label: '电子邮箱',
          placeholder: '-',
          maxlength: 100,
          span: 12,
          readonly: false,
          disabled: ['view'],
          rules: [{ type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }],
          actionButton: [
            {
              show: ['view'],
              icon: 'Edit',
              text: '修改',
              action: 'emailEdit',
              type: 'primary'
            },
            {
              show: ['edit'],
              icon: 'Check',
              text: '保存',
              action: 'emailSave',
              type: 'success',
              confirm: {
                title: '确认修改邮箱吗？',
                confirmButtonText: '确定',
                cancelButtonText: '取消'
              }
            }
          ]
        },
        {
          prop: 'password',
          label: '密码',
          placeholder: '******',
          span: 12,
          readonly: false,
          disabled: true,
          inputType: 'password',
          actionButton: [
            {
              text: '重置密码',
              type: 'primary'
            }
          ]
        }
      ]
    },
    {
      title: '所属组织',
      children: [
        {
          prop: 'organization',
          label: '所属组织',
          readonly: false,
          disabled: true,
          span: 12
        },
        {
          prop: 'positionName',
          label: '岗位',
          readonly: false,
          disabled: true,
          span: 12
        }
      ]
    },
    {
      title: '权限管理',
      children: [
        {
          prop: 'role',
          label: '所属角色',
          readonly: false,
          disabled: true,
          span: 12
        },
        {
          prop: 'recordingPermission',
          label: '录制权限',
          readonly: false,
          disabled: true,
          span: 12
        }
      ]
    }
  ]

  const handleEditToggle = () => {
    pageState.value = 'edit_basic'
  }

  const handleSave = () => {
    ElMessage.success('保存成功')
    pageState.value = 'view'
  }

  const handleCancel = () => {
    pageState.value = 'view'
  }

  const sendPhoneCode = async (phone) => {
    try {
      const res = await api.employeeProfile.sendBindCode({ newMobile: phone })
      if (res && res.code === 0) {
        ElMessage.success(`验证码已发送至 ${phone}`)
        return true
      }
      return false
    } catch (error) {
      console.error('发送验证码失败:', error)
      return false
    }
  }

  const handleChangePhoneSubmit = async () => {
    if (!changePhoneFormRef.value) return
    await changePhoneFormRef.value.validate(async (valid) => {
      if (valid) {
        try {
          const res = await api.employeeProfile.updateMobile({
            code: changePhoneForm.code,
            newMobile: changePhoneForm.phone
          })
          if (res && res.code === 0) {
            ElMessage.success('手机号更换成功')
            formData.phone = changePhoneForm.phone
            changePhoneVisible.value = false
            changePhoneForm.phone = ''
            changePhoneForm.code = ''
          }
        } catch (error) {
          console.error('更换手机号失败:', error)
        }
      }
    })
  }

  const sendPasswordCode = async () => {
    try {
      const res = await api.employeeProfile.sendPasswordCode({
        newMobile: formData.phone
      })
      if (res && res.code === 0) {
        ElMessage.success(`验证码已发送至 ${formData.phone}`)
        return true
      }
      return false
    } catch (error) {
      console.error('发送验证码失败:', error)
      return false
    }
  }

  const handleResetPasswordSubmit = async () => {
    if (!resetPasswordFormRef.value) return
    await resetPasswordFormRef.value.validate(async (valid) => {
      if (valid) {
        try {
          const res = await api.employeeProfile.updatePassword({
            code: resetPasswordForm.code,
            newPassword: resetPasswordForm.newPassword
          })
          if (res && res.code === 0) {
            ElMessage.success('密码重置成功')
            resetPasswordVisible.value = false
            resetPasswordForm.code = ''
            resetPasswordForm.newPassword = ''
            resetPasswordForm.confirmPassword = ''
          }
        } catch (error) {
          console.error('密码重置失败:', error)
        }
      }
    })
  }
</script>

<style lang="scss" scoped>
  .avtar-container {
    display: flex;
    align-items: center;
  }

  .user-profile {
    padding: 30px 40px 0 40px;
    border-radius: 10px;
    background-color: #fff;
    box-shadow: 0 90px 25px 0 rgba(213, 214, 221, 0);
    min-width: 300px;
    flex: 1;
    &__header {
      margin-bottom: 40px;

      .header-title {
        font-size: 14px;
        margin-bottom: 12px;
      }
    }

    &__avatar-box {
      display: inline-flex;
      flex-direction: column;
      gap: 12px;
      align-items: center;
    }

    &__avatar-input {
      display: none;
    }

    &__footer {
      padding-top: 20px;
      display: flex;
      gap: 16px;

      .action-btn {
        min-width: 80px;
        border-radius: 20px; // 按钮圆角
      }
    }
  }

  :deep(.el-form-item) {
    margin-bottom: 24px;
  }

  :deep(.el-input__wrapper) {
    border-radius: 999px;
    background-color: #fff;
    box-shadow: 0 0 0 1px #dcdfe6 inset;
    padding: 1px 15px;

    &.is-focus {
      box-shadow: 0 0 0 1px var(--el-color-primary) inset;
    }
  }

  :deep(.is-disabled .el-input__wrapper) {
    box-shadow: 0 0 0 1px #dcdfe6 inset !important;
  }

  :deep(.is-disabled .el-input__wrapper) {
    background-color: var(--bg-color-disabled);
    box-shadow: none;
    color: var(--text-color-main);

    .el-input__inner {
      color: var(--text-color-main);
      -webkit-text-fill-color: var(--text-color-main);
    }
  }

  :deep(.schema-form__group-title) {
    font-size: 16px;
    font-weight: 500;
    color: #303133;
    margin-bottom: 24px;
    padding-left: 0;
    border-left: none; // 去除左侧竖线
  }

  :deep(.schema-form__group) {
    &:last-child {
      border-bottom: none;
    }
  }

  .user-profile__password-form {
    margin: 30px 0;
    :deep(.el-form-item) {
      margin-bottom: 24px;
    }
    .update-confirm-btn {
      margin: 30px 0 0 0;
    }
  }

  .user-profile__password-footer {
    display: flex;
    justify-content: center;
    width: 100%;
  }

  .user-profile__content {
    max-width: 917px;
  }

  .confirm-btn-container {
    margin: 60px 0 0;
  }

  .change-phone-form {
    padding: 40px 0 48px;
    width: 280px;
  }

  .confirm-btn {
    width: 100%;
    border-radius: 56px;
  }
</style>
