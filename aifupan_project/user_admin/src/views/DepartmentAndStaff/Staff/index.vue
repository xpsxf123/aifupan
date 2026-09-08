<template>
  <div class="page-container">
    <div class="curd-wrapper" v-empty="emptyConfig">
      <Curd
        ref="curdRef"
        :table-columns="tableColumns"
        :api="api"
        :search-config="searchConfig"
        title="成员人数"
        totalUnit="个"
        show-total
        :operation-width="260"
        :layoutConfig="{ search: { isCard: true } }"
        :custom-actions="customActions"
        :action-config="{ edit: true, del: false }"
        :border="false"
        :editAction="openEditMember"
        @action="handleAction"
        @load="handleLoad"
      >
        <template #add>
          <el-button v-auth="'sys:employee:manage:add'" type="primary" round @click="handleAdd">新建成员</el-button>
        </template>
        <!-- 头像 -->
        <template #avatar="{ row }">
          <div class="avtar-container">
            <el-avatar :size="40" :src="row.avatar || defaultImg" />
          </div>
        </template>

        <!-- 状态 -->
        <template #status="{ row }">
          <span :style="{ color: row.status === 0 ? '#E43B32' : '' }">{{ row.statusStr }}</span>
        </template>

        <!-- 在职 -->
        <template #jobStatus="{ row }">
          <el-tag :type="row.jobType === 1 ? 'warning' : 'success'" effect="plain">{{ row.jobTypeStr }}</el-tag>
        </template>

        <!-- 手机/邮箱账号 -->
        <template #account="{ row }">
          <div class="account-info">
            <div>{{ row.mobile || row.phone }}</div>
            <div class="email">{{ row.email }}</div>
          </div>
        </template>

        <!-- 所属组织 -->
        <template #org="{ row }">
          <div class="org-info">
            <div class="company">{{ row.company }}</div>
            <div class="dept">{{ row.department }} - {{ row.group }}</div>
          </div>
        </template>
      </Curd>
    </div>

    <ResetPasswordDialog v-model:visible="resetPwdVisible" :row="resetPwdRow" @success="handleResetPwdSuccess" />
    <StaffMemberDrawer
      v-model="memberDialogVisible"
      :mode="memberDialogMode"
      :row="memberDialogRow"
      :company-options="companyOptions"
      :dept-options="deptOptions"
      :team-options="teamOptions"
      :position-options="positionOptions"
      :role-options="roleOptions"
      :api="{ add: api.add, edit: api.edit }"
      @success="handleMemberSaved"
    />
  </div>
</template>

<script setup>
  /**
   * @file index.vue
   * @description 人员管理页面
   */
  import { reactive, ref, onMounted } from 'vue'
  import { useRouter } from 'vue-router'
  import Curd from '@/components/Curd/index.vue'
  import { ElMessage } from 'element-plus'
  import apiModule from '@/http/api'
  import StaffMemberDrawer from '@/components/StaffMemberDrawer/index.vue'
  import ResetPasswordDialog from './components/ResetPasswordDialog.vue'
  import { shouldShowGuideEmpty } from '@/utils/empty'
  import { usePermissionStore } from '@/auth/store'
  import defaultImg from '@/assets/images/funnel/defaultAvatar.png'

  const curdRef = ref(null)
  const router = useRouter()
  const permissionStore = usePermissionStore()
  const firstLoaded = ref(false)
  const resetPwdVisible = ref(false)
  const resetPwdRow = ref({})
  const memberDialogVisible = ref(false)
  const memberDialogMode = ref('add')
  const memberDialogRow = ref({})
  const emptyConfig = reactive({
    visible: false,
    title: '暂无人员',
    description: '',
    blur: 2,
    buttons: []
  })

  const companyOptions = ref([])
  const deptOptions = ref([])
  const teamOptions = ref([])
  const searchDeptOptions = ref([])
  const searchTeamOptions = ref([])
  const positionOptions = ref([])
  const roleOptions = ref([])

  const handleAdd = () => {
    memberDialogMode.value = 'add'
    memberDialogRow.value = {}
    memberDialogVisible.value = true
  }

  const openEditMember = (row) => {
    memberDialogMode.value = 'edit'
    memberDialogRow.value = row || {}
    memberDialogVisible.value = true
  }

  const handleMemberSaved = () => {
    curdRef.value?.getData()
  }

  const getOptions = async () => {
    try {
      const [compRes, deptRes, teamRes, posRes, roleRes] = await Promise.all([
        apiModule.subCompany.options({ limit: 100 }),
        apiModule.dept.options({ limit: 100 }),
        apiModule.team.options({ limit: 100 }),
        apiModule.position.options(),
        apiModule.role.options()
      ])
      // Filter out invalid options and map them to ensure value is present
      const mapOptions = (data) => {
        if (!Array.isArray(data)) return []
        return data
          .filter((item) => item !== null && item !== undefined)
          .map((item) => {
            const key = item.key !== undefined ? item.key : item.value !== undefined ? item.value : item.id
            const label = item.label !== undefined ? item.label : item.name
            return { ...item, key, label }
          })
          .filter((item) => item.key !== undefined && item.key !== null)
      }
      companyOptions.value = mapOptions(compRes.data)
      // 搜索区域保留全量 options；成员弹窗内部改为按公司/部门级联查询。
      searchDeptOptions.value = mapOptions(deptRes.data)
      searchTeamOptions.value = mapOptions(teamRes.data)
      deptOptions.value = []
      teamOptions.value = []
      positionOptions.value = mapOptions(posRes.data)
      roleOptions.value = mapOptions(roleRes.data)
    } catch (e) {
      void e
    }
  }

  onMounted(() => {
    getOptions()
  })

  const searchConfig = [
    {
      label: '所属公司',
      prop: 'companyIds',
      type: 'select',
      options: companyOptions,
      props: { multiple: true, collapseTags: true }
    },
    {
      label: '所属部门',
      prop: 'deptIds',
      type: 'select',
      options: searchDeptOptions,
      props: { multiple: true, collapseTags: true }
    },
    {
      label: '所属小组',
      prop: 'teamIds',
      type: 'select',
      options: searchTeamOptions,
      props: { multiple: true, collapseTags: true }
    },
    {
      label: '所属岗位',
      prop: 'positionIds',
      type: 'select',
      options: positionOptions,
      props: { multiple: true, collapseTags: true }
    },
    { label: '姓名', prop: 'name', type: 'input' },
    { label: '手机号码', prop: 'mobile', type: 'input' },
    { label: '工号', prop: 'staffNumber', type: 'input' },
    {
      label: '状态',
      prop: 'accountStatus',
      type: 'select',
      options: [
        { label: '正常', key: 1 },
        { label: '停用', key: 0 }
      ]
    },
    {
      label: '在职状态',
      prop: 'jobType',
      type: 'select',
      options: [
        { label: '全职', key: 1 },
        { label: '兼职', key: 2 }
      ]
    }
  ]

  const tableColumns = [
    { label: '姓名', prop: 'name', width: 150, align: 'center' },
    { label: '头像', prop: 'avatar', width: 80, slotName: 'avatar', align: 'center' },
    { label: '工号', prop: 'staffNumber', width: 100, formatter: (row) => row.staffNumber || '-', align: 'center' },
    { label: '所属组织', prop: 'companyName', minWidth: 150, slotName: 'org', align: 'center' },
    { label: '岗位', prop: 'positionName', width: 100, align: 'center' },
    { label: '在职', prop: 'jobType', width: 80, slotName: 'jobStatus', align: 'center' },
    { label: '状态', prop: 'accountStatus', width: 100, slotName: 'status', align: 'center' },
    { label: '所属角色', prop: 'roleName', minWidth: 120, align: 'center' },
    { label: '手机/邮箱账号', prop: 'account', minWidth: 200, slotName: 'account', align: 'center' }
  ]

  const customActions = [
    {
      label: '重置密码',
      type: 'primary',
      link: true,
      command: 'resetPassword',
      permissionOp: 'update',
      show: (row) => !isHoldTenant(row)
    },
    {
      label: '同步子账号',
      type: 'primary',
      link: true,
      command: 'syncSubAccount',
      permissionOp: 'update',
      show: (row) => isHoldTenant(row)
    },
    {
      label: '停用',
      type: 'primary',
      link: true,
      command: 'stop',
      permissionOp: 'update',
      show: (row) => row.accountStatus == 1 && !isHoldTenant(row),
      confirm: {
        title: '您确认要停用该员工吗?',
        confirmButtonText: '确定',
        cancelButtonText: '取消'
      }
    },
    {
      label: '启用',
      type: 'primary',
      link: true,
      command: 'start',
      permissionOp: 'update',
      show: (row) => row.accountStatus == 0 && !isHoldTenant(row)
    }
  ]

  const isHoldTenant = (row) => {
    const v = row?.holdTenant
    if (v === true) return true
    if (v === 1) return true
    const s = String(v ?? '').toLowerCase()
    return s === '1' || s === 'true'
  }

  const api = {
    list: async (params) => {
      // Clean up search params to match API expected formats
      const queryParams = { ...params }

      // Convert arrays of single element to array of integers or omit if empty
      if (queryParams.companyIds && !Array.isArray(queryParams.companyIds)) {
        queryParams.companyIds = [queryParams.companyIds]
      }
      if (queryParams.deptIds && !Array.isArray(queryParams.deptIds)) {
        queryParams.deptIds = [queryParams.deptIds]
      }
      if (queryParams.teamIds && !Array.isArray(queryParams.teamIds)) {
        queryParams.teamIds = [queryParams.teamIds]
      }
      if (queryParams.positionIds && !Array.isArray(queryParams.positionIds)) {
        queryParams.positionIds = [queryParams.positionIds]
      }

      const res = await apiModule.employee.list(queryParams)
      const list = (res.data?.list || []).map((item) => ({
        ...item,
        avatar: item.userAvatar,
        status: item.accountStatus,
        statusStr: item.accountStatusDesc,
        jobTypeStr: item.jobTypeDesc,
        jobType: item.jobType,
        company: item.companyName,
        department: item.deptName,
        group: item.teamName,
        phone: item.mobile
      }))
      return {
        list,
        total: res.data?.totalCount || 0
      }
    },
    add: async (data) => {
      return await apiModule.employee.add({
        name: data.name,
        userAvatar: data.avatar || '',
        staffNumber: data.staffNumber,
        mobile: data.mobile,
        email: data.email || '',
        companyId: data.companyId,
        deptId: data.deptId || 0,
        teamId: data.teamId || 0,
        positionId: data.positionId || 0,
        jobType: data.jobType || 1,
        onRec: data.onRec || false,
        roleId: data.roleId
      })
    },
    edit: async (data) => {
      return await apiModule.employee.edit({
        id: data.id,
        name: data.name,
        userAvatar: data.avatar || '',
        staffNumber: data.staffNumber,
        mobile: data.mobile,
        email: data.email || '',
        companyId: data.companyId,
        deptId: data.deptId || 0,
        teamId: data.teamId || 0,
        positionId: data.positionId || 0,
        jobType: data.jobType || 1,
        onRec: data.onRec || false,
        roleId: data.roleId
      })
    },
    stopUser: async (id) => {
      return await apiModule.employee.disable({ id })
    },
    startUser: async (id) => {
      return await apiModule.employee.enable({ id })
    },
    syncSubAccount: async () => {
      return await apiModule.employee.syncSubAccount()
    }
  }

  const handleAction = ({ type, row }) => {
    if (type === 'stop') {
      api.stopUser(row.id).then(() => {
        ElMessage.success('停用成功')
        curdRef.value?.getData()
      })
    } else if (type === 'start') {
      api.startUser(row.id).then(() => {
        ElMessage.success('启用成功')
        curdRef.value?.getData()
      })
    } else if (type === 'resetPassword') {
      if (!row?.mobile && !row?.phone) {
        ElMessage.warning('该人员暂无手机号，无法重置密码')
        return
      }
      resetPwdRow.value = row
      resetPwdVisible.value = true
    } else if (type === 'syncSubAccount') {
      if (!isHoldTenant(row)) {
        ElMessage.warning('只有主账号可同步子账号')
        return
      }
      api.syncSubAccount().then(() => {
        ElMessage.success('同步子账号成功')
        curdRef.value?.getData()
      })
    }
  }

  const handleResetPwdSuccess = () => {
    curdRef.value?.getData()
  }

  /**
   * @description 检查是否存在子公司数据（人员前置条件）
   * @returns {Promise<boolean>}
   */
  const hasAnyCompany = async () => {
    try {
      const res = await apiModule.subCompany.options({ keyword: '', limit: 1 })
      const list = res?.data || []
      return Array.isArray(list) && list.length > 0
    } catch (e) {
      return false
    }
  }

  /**
   * @description Curd 首次加载后空数据引导（人员依赖子公司）
   * @param {Array} list - 表格数据
   */
  const handleLoad = async (list) => {
    if (!firstLoaded.value) firstLoaded.value = true
    const params = curdRef.value?.searchParams || {}
    if (
      !shouldShowGuideEmpty({
        firstLoaded: firstLoaded.value,
        list,
        params
      })
    ) {
      emptyConfig.visible = false
      return
    }

    const companyOk = await hasAnyCompany()
    const canAdd = permissionStore.hasPermission('sys:employee:manage:add')
    emptyConfig.title = '暂无人员'
    emptyConfig.description = companyOk
      ? '当前没有人员数据。你可以直接新增人员。'
      : '当前没有人员数据。新增人员前需要先添加子公司。'
    emptyConfig.buttons = [
      {
        text: '去子公司管理',
        type: 'primary',
        plain: true,
        round: true,
        click: () => router.push('/department-staff/subsidiary')
      },
      ...(canAdd
        ? [
            {
              text: '新增人员',
              type: 'primary',
              round: true,
              disabled: !companyOk,
              click: () => companyOk && handleAdd()
            }
          ]
        : [])
    ]
    emptyConfig.visible = true
  }
</script>

<style scoped>
  .avtar-container {
    display: flex;
    align-items: center;
  }

  .account-info {
    display: flex;
    flex-direction: column;
  }
  .account-info .email {
    color: #909399;
    font-size: 12px;
  }
  .org-info {
    display: flex;
    flex-direction: column;
  }
  .org-info .dept {
    color: #909399;
    font-size: 12px;
  }
  .staff-avatar-box {
    display: flex;
    justify-content: flex-end;
  }

  .staff-avatar-input {
    display: none;
  }

  .staff-avatar-uploader {
    width: 54px;
    height: 54px;
    border-radius: 50%;
    border: 1px dashed var(--el-color-primary);
    display: inline-flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    background: #fff;
    overflow: hidden;
  }

  .staff-avatar-uploader.is-loading {
    opacity: 0.7;
    cursor: not-allowed;
  }

  .staff-avatar-img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }

  .staff-avatar-plus {
    width: 38px;
    height: 38px;
    border-radius: 50%;
    /* border: 1px dashed var(--el-color-primary); */
    display: inline-flex;
    align-items: center;
    justify-content: center;
    color: var(--el-color-primary);
  }
</style>
