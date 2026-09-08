<template>
  <div class="page-container">
    <div class="curd-wrapper" v-empty="emptyConfig">
      <Curd
        ref="curdRef"
        :api="api"
        :table-columns="tableColumns"
        :search-config="searchConfig"
        title="小组"
        totalUnit="个"
        show-total
        :show-add="false"
        :action-config="{ edit: true, del: true }"
        :border="false"
        :edit-action="handleEdit"
        @load="handleLoad"
      >
        <template #optionBefore>
          <el-button v-auth="'org:team:manage:add'" type="primary" round @click="handleAdd">新建小组</el-button>
        </template>
      </Curd>
    </div>

    <TeamDialog
      v-model="dialogVisible"
      :mode="dialogMode"
      :row="dialogRow"
      :org-tree-options="orgTreeOptions"
      :api="{ add: api.add, edit: api.edit }"
      @success="handleSaved"
    />
  </div>
</template>

<script setup>
  /**
   * @file index.vue
   * @description 小组管理页面
   */
  import { reactive, ref, onMounted } from 'vue'
  import { useRouter } from 'vue-router'
  import Curd from '@/components/Curd/index.vue'
  import apiModule from '@/http/api'
  import TeamDialog from './components/TeamDialog.vue'
  import { shouldShowGuideEmpty } from '@/utils/empty'
  import { usePermissionStore } from '@/auth/store'

  const curdRef = ref(null)
  const router = useRouter()
  const permissionStore = usePermissionStore()
  const firstLoaded = ref(false)
  const emptyConfig = reactive({
    visible: false,
    title: '暂无小组',
    description: '',
    blur: 2,
    buttons: []
  })

  const companyOptions = ref([])
  const deptOptions = ref([])

  const orgTreeOptions = ref([])

  const dialogVisible = ref(false)
  const dialogMode = ref('add')
  const dialogRow = ref({})

  const getCompanyOptions = async (keyword = '') => {
    try {
      const res = await apiModule.subCompany.options({ keyword, limit: 50 })
      companyOptions.value =
        res.data.map((i) => {
          return { ...i, label: i.label, value: i.key }
        }) || []
    } catch (e) {
      console.error(e)
    }
  }

  const getDeptOptions = async (keyword = '', companyId) => {
    try {
      const res = await apiModule.dept.options({ keyword, limit: 50, companyId })
      deptOptions.value =
        res.data.map((i) => {
          return { ...i, label: i.label, value: i.key }
        }) || []
    } catch (e) {
      console.error(e)
    }
  }

  const getOrgTreeOptions = async () => {
    try {
      const res = await apiModule.org.tree({ level: 2 })
      const mapTree = (nodes, level = 0) => {
        if (!nodes) return undefined
        return nodes.map((node) => ({
          value: node.key,
          label: node.label,
          disabled: level !== 1,
          children: node.children && node.children.length ? mapTree(node.children, level + 1) : undefined
        }))
      }
      orgTreeOptions.value = mapTree(res.data) || []
    } catch (e) {
      console.error(e)
    }
  }

  onMounted(() => {
    getCompanyOptions()
    getDeptOptions()
    getOrgTreeOptions()
  })

  const searchConfig = [
    {
      label: '所属公司',
      prop: 'companyId',
      type: 'select',
      options: companyOptions,
      opetion: { filterable: true }
    },
    {
      label: '所属部门',
      prop: 'deptId',
      type: 'select',
      options: deptOptions,
      opetion: { filterable: true }
    },
    { label: '管理者', prop: 'managerKeyword', type: 'input', placeholder: '请输入管理者' }
  ]

  const tableColumns = [
    { label: '小组名称', prop: 'name', minWidth: 160, align: 'center' },
    { label: '所属公司', prop: 'companyName', minWidth: 160, align: 'center' },
    { label: '所属部门', prop: 'deptName', minWidth: 160, align: 'center' },
    { label: '归属直播间', prop: 'roomCount', width: 120, align: 'center' },
    { label: '排序', prop: 'sort', width: 100, align: 'center' },
    { label: '管理者', prop: 'manager', minWidth: 240 }
  ]

  const api = {
    list: async (params) => {
      const { managerKeyword, companyId, pageSize, ...rest } = params || {}
      const query = {
        ...rest,
        limit: pageSize,
        page: rest.page
      }
      if (companyId !== undefined && companyId !== '') {
        query.companyId = companyId
      }
      delete query.pageSize

      const res = await apiModule.team.list(query)
      let list = res.data?.list || []

      list = list.map((item) => ({
        ...item,
        manager: item.managerUserInfos?.map((u) => `${u.name} (${u.mobile})`).join(' , ') || '',
        managerUserIds: item.managerUserInfos?.map((u) => u.id) || [],
        companyId: item.companyId || companyId
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
      return await apiModule.team.add({
        deptId: data.deptId,
        name: data.name,
        sort: data.sort || 0,
        managerUserIds: data.managerUserIds || []
      })
    },
    edit: async (data) => {
      return await apiModule.team.edit({
        id: data.id,
        deptId: data.deptId,
        name: data.name,
        sort: data.sort || 0,
        managerUserIds: data.managerUserIds || []
      })
    },
    del: async (row) => {
      return await apiModule.team.del({ id: row.id })
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
   * @description 检查是否存在部门数据（小组前置条件）
   * @returns {Promise<boolean>}
   */
  const hasAnyDept = async () => {
    try {
      const res = await apiModule.dept.options({ keyword: '', limit: 1 })
      const list = res?.data || []
      return Array.isArray(list) && list.length > 0
    } catch (e) {
      return false
    }
  }

  /**
   * @description Curd 首次加载后空数据引导（小组依赖部门）
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

    const deptOk = await hasAnyDept()
    emptyConfig.title = '暂无小组'
    emptyConfig.description = deptOk
      ? '当前没有小组数据。你可以直接新增小组。'
      : '当前没有小组数据。添加小组前需要先添加部门。'
    const canAdd = permissionStore.hasPermission('org:team:manage:add')

    emptyConfig.buttons = [
      {
        text: '去部门管理',
        type: 'primary',
        plain: true,
        round: true,
        click: () => router.push('/department-staff/department')
      },
      ...(canAdd
        ? [
            {
              text: '新增小组',
              type: 'primary',
              round: true,
              disabled: !deptOk,
              click: () => deptOk && handleAdd()
            }
          ]
        : [])
    ]
    emptyConfig.visible = true
  }
</script>

