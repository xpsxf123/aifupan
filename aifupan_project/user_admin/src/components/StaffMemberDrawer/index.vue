<template>
  <el-drawer
    header-class="common-drawer-header-style"
    body-class="common-drawer-body-style"
    footer-class="common-drawer-footer-style"
    :model-value="modelValue"
    :show-close="false"
    :size="902"
    destroy-on-close
    @close="handleClose"
  >
    <template #header>
      <div class="staff-member-dialog__header">
        <CloseSvg @click="handleClose" />
        <div class="staff-member-dialog__title">{{ dialogTitle }}</div>
      </div>
    </template>

    <el-form ref="formRef" :model="form" :rules="rules" class="staff-member-dialog__form" label-position="right">
      <p class="staff-member-dialog__section-title">基础信息</p>
      <div class="staff-member-dialog__section staff-member-dialog__section--card">
        <div class="staff-member-dialog__section-body">
          <el-row :gutter="20">
            <el-col :span="20">
              <el-row :gutter="20">
                <el-col :span="12">
                  <el-form-item label="姓名" prop="name" required label-width="70px">
                    <el-input v-model="form.name" placeholder="请输入" class="rounded" />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="手机号" prop="mobile" required label-width="70px">
                    <el-input v-model="form.mobile" placeholder="请输入" class="rounded" />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="工号" prop="staffNumber" required label-width="70px" style="margin-bottom: 0">
                    <el-input v-model="form.staffNumber" placeholder="请输入" class="rounded" />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="邮箱" prop="email" label-width="70px" style="margin-bottom: 0">
                    <el-input v-model="form.email" placeholder="请输入" class="rounded" />
                  </el-form-item>
                </el-col>
              </el-row>
            </el-col>
            <el-col :span="4">
              <el-col>
                <el-form-item
                  label="员工头像"
                  prop="avatar"
                  label-width="80px"
                  label-position="top"
                  style="margin-bottom: 0"
                  class="avatar-form-item"
                >
                  <div class="staff-avatar-box">
                    <input
                      ref="avatarInputRef"
                      class="staff-avatar-input"
                      type="file"
                      accept="image/jpeg,image/png,image/webp"
                      @change="handleAvatarChange"
                    />
                    <div
                      class="staff-avatar-uploader"
                      :class="{ 'is-loading': avatarUploading }"
                      @click="handleAvatarTrigger"
                    >
                      <img v-if="form.avatar" class="staff-avatar-img" :src="form.avatar" alt="avatar" />
                      <div v-else class="staff-avatar-plus">
                        <el-icon><Plus /></el-icon>
                      </div>
                    </div>
                  </div>
                </el-form-item>
              </el-col>
            </el-col>
          </el-row>
        </div>
      </div>

      <p class="staff-member-dialog__section-title">职位信息</p>
      <div class="staff-member-dialog__section staff-member-dialog__section--card">
        <div class="staff-member-dialog__section-body">
          <el-row :gutter="50">
            <el-col :span="12">
              <el-form-item label="所属公司" prop="companyId" required label-width="80px">
                <div class="staff-member-dialog__select-row">
                  <el-select
                    v-model="form.companyId"
                    placeholder="请选择"
                    clearable
                    class="rounded"
                    style="width: 100%"
                    :loading="orgTreeLoading"
                  >
                    <el-option v-for="opt in companyTreeOptions" :key="opt.key" :label="opt.label" :value="opt.key" />
                  </el-select>
                  <el-button
                    v-if="!companyTreeOptions.length"
                    class="staff-member-dialog__empty-action"
                    type="primary"
                    link
                    @click="goTo('/department-staff/subsidiary')"
                  >
                    去添加子公司
                  </el-button>
                </div>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="所属部门" prop="deptId" label-width="80px">
                <div class="staff-member-dialog__select-row">
                  <el-select
                    v-model="form.deptId"
                    placeholder="请选择"
                    clearable
                    class="rounded"
                    style="width: 100%"
                    :disabled="!form.companyId"
                    :loading="deptLoading"
                  >
                    <el-option v-for="opt in localDeptOptions" :key="opt.key" :label="opt.label" :value="opt.key" />
                  </el-select>
                  <el-button
                    v-if="form.companyId && !localDeptOptions.length"
                    class="staff-member-dialog__empty-action"
                    type="primary"
                    link
                    @click="goTo('/department-staff/department')"
                  >
                    去添加部门
                  </el-button>
                </div>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="所属小组" prop="teamId" label-width="75px">
                <div class="staff-member-dialog__select-row">
                  <el-select
                    v-model="form.teamId"
                    placeholder="请选择"
                    clearable
                    class="rounded"
                    style="width: 100%"
                    :disabled="!form.deptId"
                    :loading="teamLoading"
                  >
                    <el-option v-for="opt in localTeamOptions" :key="opt.key" :label="opt.label" :value="opt.key" />
                  </el-select>
                  <el-button
                    v-if="form.deptId && !localTeamOptions.length"
                    class="staff-member-dialog__empty-action"
                    type="primary"
                    link
                    @click="goTo('/department-staff/team')"
                  >
                    去添加小组
                  </el-button>
                </div>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="所属岗位" prop="positionId" label-width="80px">
                <el-select v-model="form.positionId" placeholder="请选择" clearable class="rounded" style="width: 100%">
                  <el-option v-for="opt in positionOptions" :key="opt.key" :label="opt.label" :value="opt.key" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="在职状态" prop="jobType" label-width="75px" style="margin-bottom: 0">
                <el-select v-model="form.jobType" placeholder="请选择" clearable class="rounded" style="width: 100%">
                  <el-option :value="1" label="全职" />
                  <el-option :value="2" label="兼职" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
        </div>
      </div>

      <p class="staff-member-dialog__section-title">管理权限</p>
      <div class="staff-member-dialog__section staff-member-dialog__section--card">
        <div class="staff-member-dialog__section-body">
          <el-row :gutter="50">
            <el-col :span="12">
              <el-form-item label="角色" prop="roleId" required label-width="55px" style="margin-bottom: 0">
                <el-select v-model="form.roleId" placeholder="请选择" clearable class="rounded" style="width: 100%">
                  <el-option v-for="opt in roleOptions" :key="opt.key" :label="opt.label" :value="opt.key" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
        </div>
      </div>

      <p class="staff-member-dialog__section-title">录制权限</p>
      <div class="staff-member-dialog__section staff-member-dialog__section--card">
        <div class="staff-member-dialog__section-body">
          <el-row :gutter="50">
            <el-col :span="24">
              <el-form-item label="录制权限（客户端子账号权限）" prop="onRec" label-width="220px" style="margin-bottom: 0">
                <el-radio-group v-model="form.onRec" class="rounded">
                  <el-radio :label="false">不开启</el-radio>
                  <el-radio :label="true">开启</el-radio>
                </el-radio-group>
              </el-form-item>
            </el-col>
          </el-row>
        </div>
      </div>
    </el-form>

    <template #footer>
      <el-button
        v-auth="props.mode === 'edit' ? permissionUpdateCode : permissionAddCode"
        class="custom-btn status-btn"
        type="primary"
        :loading="submitting"
        @click="handleSubmit"
      >
        确定
      </el-button>
      <el-button class="custom-btn status-btn" @click="handleClose">取消</el-button>
    </template>
  </el-drawer>
</template>

<script setup>
  import { computed, reactive, ref, watch } from 'vue'
  import { useRouter } from 'vue-router'
  import { ElMessage } from 'element-plus'
  import { Plus } from '@element-plus/icons-vue'
  import CloseSvg from '@/components/CloseSvgIcon/index.vue'
  import apiModule from '@/http/api'
  import { normalizeKeyLabelOptions } from '@/utils/options'

  const props = defineProps({
    modelValue: { type: Boolean, default: false },
    mode: { type: String, default: 'add' },
    row: { type: Object, default: () => ({}) },
    companyOptions: { type: Array, default: () => [] },
    deptOptions: { type: Array, default: () => [] },
    teamOptions: { type: Array, default: () => [] },
    positionOptions: { type: Array, default: () => [] },
    roleOptions: { type: Array, default: () => [] },
    api: { type: Object, required: true },
    permissionAddCode: { type: String, default: 'sys:employee:manage:add' },
    permissionUpdateCode: { type: String, default: 'sys:employee:manage:update' }
  })

  const emit = defineEmits(['update:modelValue', 'success'])

  const router = useRouter()
  const formRef = ref(null)
  const submitting = ref(false)
  const avatarInputRef = ref(null)
  const avatarUploading = ref(false)
  const deptLoading = ref(false)
  const teamLoading = ref(false)
  const localDeptOptions = ref([])
  const localTeamOptions = ref([])
  const initializingCascade = ref(false)
  const orgTreeLoading = ref(false)
  const orgTreeOptions = ref([])
  const orgTreeRequestInFlight = ref(null)

  const form = reactive({
    id: '',
    name: '',
    mobile: '',
    staffNumber: '',
    email: '',
    avatar: '',
    companyId: '',
    deptId: '',
    teamId: '',
    positionId: '',
    jobType: 1,
    roleId: '',
    onRec: false
  })

  const rules = {
    name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
    mobile: [
      { required: true, message: '请输入手机号', trigger: 'blur' },
      {
        pattern: /^1[3-9]\d{9}$/,
        message: '请输入正确的手机号码',
        trigger: 'blur'
      }
    ],
    staffNumber: [{ required: true, message: '请输入工号', trigger: 'blur' }],
    companyId: [{ required: true, message: '请选择公司', trigger: 'change' }],
    positionId: [{ required: true, message: '请选择岗位', trigger: 'change' }],
    roleId: [{ required: true, message: '请选择角色', trigger: 'change' }]
  }

  const dialogTitle = computed(() => (props.mode === 'edit' ? '编辑成员' : '新建成员'))
  const companyTreeOptions = computed(() => orgTreeOptions.value)

  const ensureOrgTreeLoaded = async ({ level } = {}) => {
    if (orgTreeOptions.value.length) return orgTreeOptions.value
    if (orgTreeRequestInFlight.value) return orgTreeRequestInFlight.value

    orgTreeLoading.value = true
    deptLoading.value = true
    teamLoading.value = true

    orgTreeRequestInFlight.value = (async () => {
      try {
        const res = await apiModule.org.tree({ level: level || 3 })
        orgTreeOptions.value = normalizeKeyLabelOptions(res?.data || [])
      } catch (e) {
        void e
        orgTreeOptions.value = []
      } finally {
        orgTreeLoading.value = false
        deptLoading.value = false
        teamLoading.value = false
        orgTreeRequestInFlight.value = null
      }
      return orgTreeOptions.value
    })()

    return orgTreeRequestInFlight.value
  }

  const resolveChildrenOptions = (node) => {
    return normalizeKeyLabelOptions(node?.children || [])
  }

  /**
   * @description 按公司同步部门下拉（组织树内）
   * @param {string|number} companyId - 子公司 ID
   */
  const fetchDeptOptionsByCompany = async (companyId) => {
    await ensureOrgTreeLoaded({ level: 3 })
    if (!companyId) {
      localDeptOptions.value = []
      return
    }
    const companyNode = (orgTreeOptions.value || []).find((i) => String(i?.key) === String(companyId))
    localDeptOptions.value = resolveChildrenOptions(companyNode)
  }

  /**
   * @description 按部门同步小组下拉（组织树内）
   * @param {string|number} deptId - 部门 ID
   */
  const fetchTeamOptionsByDept = async (deptId) => {
    await ensureOrgTreeLoaded({ level: 3 })
    if (!deptId) {
      localTeamOptions.value = []
      return
    }
    let deptNode = (localDeptOptions.value || []).find((i) => String(i?.key) === String(deptId))
    if (!deptNode) {
      for (const company of orgTreeOptions.value || []) {
        const depts = resolveChildrenOptions(company)
        deptNode = depts.find((i) => String(i?.key) === String(deptId))
        if (deptNode) break
      }
    }
    localTeamOptions.value = resolveChildrenOptions(deptNode)
  }

  const resetForm = () => {
    form.id = ''
    form.name = ''
    form.mobile = ''
    form.staffNumber = ''
    form.email = ''
    form.avatar = ''
    form.companyId = ''
    form.deptId = ''
    form.teamId = ''
    form.positionId = ''
    form.jobType = 1
    form.roleId = ''
    form.onRec = false
  }

  const fillForm = (row) => {
    form.id = row?.id || ''
    form.name = row?.name || ''
    form.mobile = row?.mobile || row?.phone || ''
    form.staffNumber = row?.staffNumber || ''
    form.email = row?.email || ''
    form.avatar = row?.avatar || row?.userAvatar || ''
    form.companyId = row?.companyId || ''
    form.deptId = row?.deptId || ''
    form.teamId = row?.teamId || ''
    form.positionId = row?.positionId || ''
    form.jobType = row?.jobType || 1
    form.roleId = row?.roleId || ''
    form.onRec = row?.onRec || false
  }

  const handleClose = () => {
    emit('update:modelValue', false)
  }

  const goTo = (path) => {
    router.push(path)
  }

  const handleAvatarTrigger = () => {
    avatarInputRef.value?.click()
  }

  const handleAvatarChange = async (event) => {
    const file = event.target?.files?.[0]
    if (!file) return
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
      const presignedRes = await apiModule.employeeProfile.getImagePresignedUpload({ suffix })
      const uploadInfo = presignedRes?.data || {}
      if (!presignedRes || presignedRes.code !== 0 || !uploadInfo.uploadUrl) {
        ElMessage.error(presignedRes?.msg || '获取上传地址失败')
        return
      }
      const avatarAccessUrl = String(uploadInfo.uploadUrl || '').split('?')[0]
      if (!avatarAccessUrl) {
        ElMessage.error('获取头像地址失败')
        return
      }
      const uploadResponse = await fetch(uploadInfo.uploadUrl, {
        method: 'PUT',
        body: await file.arrayBuffer()
      })
      if (!uploadResponse.ok) {
        ElMessage.error('头像上传失败')
        return
      }
      form.avatar = avatarAccessUrl
      ElMessage.success('头像上传成功')
    } catch (e) {
      if (e?.response || e?.config) return
      void e
      ElMessage.error('头像上传失败')
    } finally {
      avatarUploading.value = false
      event.target.value = ''
    }
  }

  const handleSubmit = async () => {
    if (!formRef.value) return
    const valid = await formRef.value.validate().catch(() => false)
    if (!valid) return

    submitting.value = true
    try {
      if (props.mode === 'edit') {
        await props.api.edit({ ...form })
      } else {
        await props.api.add({ ...form })
      }
      ElMessage.success('保存成功')
      emit('success')
      handleClose()
    } catch (e) {
      void e
    } finally {
      submitting.value = false
    }
  }

  watch(
    () => props.modelValue,
    async (v) => {
      if (!v) return
      initializingCascade.value = true
      resetForm()
      localDeptOptions.value = []
      localTeamOptions.value = []
      await ensureOrgTreeLoaded({ level: 3 })
      if (props.mode === 'edit') {
        fillForm(props.row)
      }
      if (form.companyId) {
        await fetchDeptOptionsByCompany(form.companyId)
      }
      if (form.deptId) {
        await fetchTeamOptionsByDept(form.deptId)
      }
      initializingCascade.value = false
    }
  )

  watch(
    () => form.companyId,
    async (val, oldVal) => {
      if (!props.modelValue) return
      if (String(val || '') === String(oldVal || '')) return
      if (initializingCascade.value) return
      form.deptId = ''
      form.teamId = ''
      localTeamOptions.value = []
      await fetchDeptOptionsByCompany(val)
    }
  )

  watch(
    () => form.deptId,
    async (val, oldVal) => {
      if (!props.modelValue) return
      if (String(val || '') === String(oldVal || '')) return
      if (initializingCascade.value) return
      form.teamId = ''
      await fetchTeamOptionsByDept(val)
    }
  )
</script>

<style scoped lang="scss">
  .staff-member-dialog__header {
    display: flex;
    width: 100%;
    gap: 13px;
    align-items: center;
  }

  .staff-member-dialog__title {
    font-size: 16px;
    color: #151719;
  }
  .staff-member-dialog__form {
    padding-right: 10px;
  }

  .staff-member-dialog__section-title {
    margin-bottom: 20px;
  }

  .staff-member-dialog__section {
    border: 1px solid #ebeef5;
    border-radius: 10px;
    overflow: hidden;
    margin-bottom: 14px;
    background: #fff;
  }

  .staff-member-dialog__section--card {
    padding: 24px;
    background-color: #f7f7f7;
    border: none;
  }

  .staff-member-dialog__section-body {
    display: flex;
    flex-direction: column;
    row-gap: 36px;
  }

  .staff-member-dialog__select-row {
    display: flex;
    align-items: center;
    gap: 10px;
    width: 100%;
  }

  .staff-member-dialog__empty-action {
    flex: 0 0 auto;
  }

  .staff-avatar-box {
    width: 100%;
    display: flex;
    gap: 16px;
    align-items: center;
    justify-content: center;
  }

  .staff-avatar-input {
    display: none;
  }

  .staff-avatar-uploader {
    width: 72px;
    height: 72px;
    border-radius: 50%;
    border: 1px dashed #a0cfff;
    background: #f4f8ff;
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    user-select: none;
    overflow: hidden;
  }

  .staff-avatar-uploader.is-loading {
    opacity: 0.7;
    pointer-events: none;
  }

  .staff-avatar-img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }

  .staff-avatar-plus {
    color: #409eff;
  }

  :deep(.el-form-item__label) {
    color: #151719;
  }
</style>

<style lang="scss">
  .el-form-item {
    margin-bottom: 24px;
  }
  .avatar-form-item.el-form-item--label-top .el-form-item__label {
    display: inline-block;
    width: 100%;
    margin-bottom: 0;
    line-height: 1;
    text-align: center;
  }
  .enter-drawer-header {
    padding: 11px 30px;
    margin-bottom: 0;
    border-bottom: 1px solid #dcdcdc;
  }

  .custom-btn {
    height: 34px;
    border-radius: 50px;
  }
  .status-btn {
    width: 84px;
  }

  .el-drawer__footer {
    display: flex;
  }
</style>
