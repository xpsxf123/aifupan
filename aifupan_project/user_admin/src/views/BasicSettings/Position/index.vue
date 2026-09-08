<template>
  <div class="page-container bg-white br-2">
    <div class="curd-wrapper" v-empty="emptyConfig">
      <Curd
        ref="curdRef"
        :table-columns="tableColumns"
        :api="api"
        :before-request="beforeRequest"
        :data-map="dataMap"
        :action-before="actionBefore"
        title="岗位"
        totalUnit="个"
        show-total
        :show-add="false"
        :show-toolbar-right="false"
        :border="false"
        :action-config="{ edit: true, del: true }"
        :edit-action="handleEdit"
        @load="handleLoad"
      >
        <template #optionBefore>
          <el-button v-auth="'org:position:add'" type="primary" round @click="handleAdd">新建岗位</el-button>
        </template>
      </Curd>
    </div>

    <PositionDialog
      v-model="dialogVisible"
      :mode="dialogMode"
      :row="dialogRow"
      :api="{ add: api.add, edit: api.edit }"
      @success="handleSaved"
    />
  </div>
</template>

<script setup>
  /**
   * @file index.vue
   * @description 岗位管理页面
   */
  import { reactive, ref, watchEffect } from 'vue'
  import { ElMessage } from 'element-plus'
  import Curd from '@/components/Curd/index.vue'
  import positionApi from '@/http/api/position'
  import PositionDialog from './components/PositionDialog.vue'
  import { shouldShowGuideEmpty } from '@/utils/empty'
  import { usePermissionStore } from '@/auth/store'

  const curdRef = ref(null)
  const firstLoaded = ref(false)
  const dialogVisible = ref(false)
  const dialogMode = ref('add')
  const dialogRow = ref({})
  const permissionStore = usePermissionStore()
  const emptyConfig = reactive({
    visible: false,
    title: '暂无岗位',
    description: '当前没有岗位数据。你可以先新增岗位，再去人员管理中为成员选择岗位。',
    blur: 2,
    buttons: [
      {
        text: '新建岗位',
        type: 'primary',
        round: true,
        click: () => handleAdd()
      }
    ]
  })

  const emptyButtonsSeed = [...emptyConfig.buttons]

  watchEffect(() => {
    emptyConfig.buttons = permissionStore.hasPermission('org:position:add') ? emptyButtonsSeed : []
  })

  /**
   * @description 表格列配置
   */
  const tableColumns = [
    { label: '岗位名称', prop: 'name', minWidth: 120, align: 'center' },
    {
      label: '来源',
      prop: 'isDefault',
      minWidth: 100,
      align: 'center',
      formatter: (row) => (row.isDefault ? '系统' : '自定义')
    },
    { label: '更新时间', prop: 'updateTime', minWidth: 160, formatter: (row) => row.updateTime || '-', align: 'center' }
  ]

  /**
   * @description 搜索表单配置
   */
  const searchConfig = [
    {
      label: '岗位名称',
      prop: 'name',
      type: 'input',
      placeholder: '请输入岗位名称'
    }
  ]

  /**
   * @description 页面接口（Curd 约定：list/add/edit/del）
   */
  const api = {
    /**
     * @description 获取岗位列表
     * @param {Object} params - 查询参数
     * @returns {Promise}
     */
    list: async (params) => {
      return positionApi.list(params)
    },
    /**
     * @description 新增岗位
     * @param {Object} data - 表单数据
     * @returns {Promise}
     */
    add: async (data) => {
      return positionApi.add({
        name: data.name,
        sort: Number(data.sort)
      })
    },
    /**
     * @description 编辑岗位
     * @param {Object} data - 表单数据
     * @returns {Promise}
     */
    edit: async (data) => {
      return positionApi.edit({
        id: data.id,
        name: data.name,
        sort: Number(data.sort)
      })
    },
    /**
     * @description 删除岗位
     * @param {Object} row - 行数据
     * @returns {Promise}
     */
    del: async (row) => {
      return positionApi.del({ id: row.id })
    }
  }

  /**
   * @description 将 Curd 的分页参数转换为后端分页参数格式（page/limit）
   * @param {Object} params - Curd 透传的请求参数
   * @returns {Object}
   */
  const beforeRequest = (params) => {
    const { page, pageSize, ...rest } = params || {}
    return {
      ...rest,
      page,
      limit: pageSize
    }
  }

  /**
   * @description Curd 列表数据映射（ApiResponse<PageData<T>>）
   */
  const dataMap = {
    list: 'data.list',
    total: 'data.totalCount'
  }

  /**
   * @description 删除拦截：默认岗位不可删；被人员引用不可删
   * @param {string} type - 操作类型
   * @param {Object} row - 行数据
   * @returns {boolean}
   */
  const actionBefore = (type, row) => {
    if (type !== 'del') return true
    if (row && row.isDefault) {
      ElMessage.warning('默认岗位不可删除')
      return false
    }
    if (row && Number(row.employeeCount) > 0) {
      ElMessage.warning('该岗位已被人员引用，无法删除')
      return false
    }
    return true
  }

  /**
   * @description 打开新增岗位弹窗
   */
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
   * @description Curd 首次加载后空数据引导
   * @param {Array} list - 表格数据
   */
  const handleLoad = (list) => {
    if (!firstLoaded.value) firstLoaded.value = true
    emptyConfig.visible = shouldShowGuideEmpty({
      firstLoaded: firstLoaded.value,
      list,
      params: curdRef.value?.searchParams || {}
    })
  }
</script>

