<template>
  <div class="page-container">
    <div class="curd-wrapper" v-empty="emptyConfig">
      <Curd
        ref="curdRef"
        :table-columns="tableColumns"
        :api="api"
        title="子公司"
        totalUnit="个"
        show-total
        :show-add="false"
        :show-toolbar-right="false"
        :action-config="{ edit: true, del: true }"
        :edit-action="handleEdit"
        :border="false"
        @load="handleLoad"
      >
        <template #optionBefore>
          <el-button v-auth="'org:sub-company:manage:add'" type="primary" round @click="handleAdd"
            >新建子公司</el-button
          >
        </template>
      </Curd>
    </div>
    <SubsidiaryDialog v-model="dialogVisible" :mode="dialogMode" :row="dialogRow" @success="handleSuccess" />
  </div>
</template>

<script setup>
  /**
   * @file index.vue
   * @description 子公司管理页面
   */
  import { reactive, ref, onMounted, watchEffect } from 'vue'
  import { useRouter } from 'vue-router'
  import Curd from '@/components/Curd/index.vue'
  import SubsidiaryDialog from './components/SubsidiaryDialog.vue'
  import apiModule from '@/http/api'
  import { usePermissionStore } from '@/auth/store'

  const curdRef = ref(null)
  const router = useRouter()
  const permissionStore = usePermissionStore()
  const firstLoaded = ref(false)
  const dialogVisible = ref(false)
  const dialogMode = ref('add')
  const dialogRow = ref({})
  const emptyConfig = reactive({
    visible: false,
    title: '暂无子公司',
    description: '添加子公司后，才能继续添加部门、小组、直播间等组织数据。',
    blur: 2,
    buttons: [
      {
        text: '新建子公司',
        type: 'primary',
        round: true,
        click: () => handleAdd()
      }
    ]
  })

  const emptyButtonsSeed = [...emptyConfig.buttons]

  watchEffect(() => {
    emptyConfig.buttons = permissionStore.hasPermission('org:sub-company:manage:add') ? emptyButtonsSeed : []
  })

  const tableColumns = [
    { label: '公司名称', prop: 'name', minWidth: 150, align: 'center' },
    { label: '归属直播间', prop: 'liveRoomCount', minWidth: 80, align: 'center' },
    // { label: '排序', prop: 'sort', minWidth: 80 },
    { label: '管理者', prop: 'manager', minWidth: 300 }
  ]

  const searchConfig = [
    // { label: '公司名称', prop: 'name', type: 'input' }
  ]

  const api = {
    list: async (params) => {
      const res = await apiModule.subCompany.list(params)
      const list = (res.data?.list || []).map((item) => ({
        ...item,
        liveRoomCount: item.roomCount,
        manager: item.managerUserInfos?.map((u) => `${u.name} (${u.mobile})`).join(' , ') || '',
        managerUserIds: item.managerUserInfos?.map((u) => u.id) || []
      }))
      return {
        list,
        total: res.data?.totalCount || 0
      }
    },
    del: async (row) => {
      return await apiModule.subCompany.del({ id: row.id })
    }
  }

  const handleAdd = () => {
    dialogMode.value = 'add'
    dialogRow.value = {}
    dialogVisible.value = true
  }

  const handleEdit = (row) => {
    dialogMode.value = 'edit'
    dialogRow.value = row
    dialogVisible.value = true
  }

  const handleSuccess = () => {
    curdRef.value?.getData()
  }

  /**
   * @description 判断是否为默认搜索条件（用于首次空数据引导）
   * @returns {boolean}
   */
  const isDefaultSearch = () => {
    const params = curdRef.value?.searchParams || {}
    return Object.keys(params).every((k) => {
      const v = params[k]
      if (Array.isArray(v)) return v.length === 0
      return v === undefined || v === null || v === ''
    })
  }

  /**
   * @description Curd 首次加载后空数据引导
   * @param {Array} list - 表格数据
   */
  const handleLoad = (list) => {
    if (!firstLoaded.value) firstLoaded.value = true
    const isEmpty = Array.isArray(list) && list.length === 0
    emptyConfig.visible = firstLoaded.value && isEmpty && isDefaultSearch()
  }
</script>
