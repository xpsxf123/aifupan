<template>
  <div class="page-container">
    <div class="curd-wrapper" v-empty="emptyConfig">
      <Curd
        ref="curdRef"
        :api="api"
        :table-columns="tableColumns"
        :search-config="searchConfig"
        title="部门"
        totalUnit="个"
        show-total
        :show-add="false"
        :action-config="{ edit: true, del: true }"
        :edit-action="handleEdit"
        :border="false"
        @load="handleLoad"
      >
        <template #optionBefore>
          <el-button v-auth="'org:dept:manage:add'" type="primary" round @click="handleAdd">新建部门</el-button>
        </template>
      </Curd>
    </div>

    <DepartmentDialog
      v-model="dialogVisible"
      :mode="dialogMode"
      :row="dialogRow"
      :company-options="companyOptions"
      :api="{ add: api.add, edit: api.edit }"
      @success="handleSaved"
    />
  </div>
</template>

<script setup>
  /**
   * @file index.vue
   * @description 部门管理页面
   */
  import { reactive, ref, onMounted } from 'vue'
  import { useRouter } from 'vue-router'
  import Curd from '@/components/Curd/index.vue'
  import apiModule from '@/http/api'
  import DepartmentDialog from './components/DepartmentDialog.vue'
  import { shouldShowGuideEmpty } from '@/utils/empty'
  import { usePermissionStore } from '@/auth/store'

  const curdRef = ref(null)
  const router = useRouter()
  const permissionStore = usePermissionStore()
  const firstLoaded = ref(false)
  const emptyConfig = reactive({
    visible: false,
    title: '暂无部门',
    description: '',
    blur: 2,
    buttons: []
  })

  const companyOptions = ref([])
  const dialogVisible = ref(false)
  const dialogMode = ref('add')
  const dialogRow = ref({})

  const getCompanyOptions = async (keyword = '') => {
    try {
      const res = await apiModule.subCompany.options({ keyword, limit: 50 })
      companyOptions.value = res.data?.map((i) => ({ ...i, label: i.label, value: i.key })) || []
    } catch (e) {
      console.error(e)
    }
  }

  onMounted(() => {
    getCompanyOptions()
  })

  const searchConfig = [
    {
      label: '所属公司',
      prop: 'companyId',
      type: 'select',
      options: companyOptions,
      opetion: { filterable: true }
    },
    { label: '部门名称', prop: 'name', type: 'input', placeholder: '请输入部门名称' },
    { label: '管理者', prop: 'managerKeyword', type: 'input', placeholder: '请输入管理者' }
  ]

  const tableColumns = [
    { label: '部门名称', prop: 'name', minWidth: 160, align: 'center' },
    { label: '所属公司', prop: 'companyName', minWidth: 160, align: 'center' },
    { label: '归属直播间', prop: 'roomCount', width: 120, align: 'center' },
    { label: '排序', prop: 'sort', width: 100, align: 'center' },
    { label: '管理者', prop: 'manager', minWidth: 240 }
  ]

  const api = {
    list: async (params) => {
      const { managerKeyword, pageSize, ...rest } = params || {}
      const query = {
        ...rest,
        limit: pageSize,
        page: rest.page
      }
      delete query.pageSize

      const res = await apiModule.dept.list(query)
      let list = res.data?.list || []

      list = list.map((item) => ({
        ...item,
        manager: item.managerUserInfos?.map((u) => `${u.name} (${u.mobile})`).join(' , ') || '',
        managerUserIds: item.managerUserInfos?.map((u) => u.id) || []
      }))

      if (managerKeyword) {
        const keyword = String(managerKeyword).trim()
        if (keyword) {
          list = list.filter((item) => String(item.manager || '').includes(keyword))
        }
      }

      return {
        list,
        total: res.data?.totalCount || 0
      }
    },
    add: async (data) => {
      return await apiModule.dept.add({
        companyId: data.companyId,
        name: data.name,
        sort: data.sort || 0,
        managerUserIds: data.managerUserIds || []
      })
    },
    edit: async (data) => {
      return await apiModule.dept.edit({
        id: data.id,
        companyId: data.companyId,
        name: data.name,
        sort: data.sort || 0,
        managerUserIds: data.managerUserIds || []
      })
    },
    del: async (row) => {
      return await apiModule.dept.del({ id: row.id })
    }
  }

  const handleAdd = () => {
    dialogMode.value = 'add'
    dialogRow.value = {}
    dialogVisible.value = true
  }

  const handleEdit = (row) => {
    dialogMode.value = 'edit'
    dialogRow.value = row || {}
    dialogVisible.value = true
  }

  const handleSaved = () => {
    curdRef.value?.getData()
  }

  /**
   * @description 检查是否存在子公司数据（部门前置条件）
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
   * @description Curd 首次加载后空数据引导（部门依赖子公司）
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
    emptyConfig.title = '暂无部门'
    emptyConfig.description = companyOk
      ? '当前没有部门数据。你可以直接新增部门。'
      : '当前没有部门数据。添加部门前需要先添加子公司。'
    const canAdd = permissionStore.hasPermission('org:dept:manage:add')

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
              text: '新增部门',
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