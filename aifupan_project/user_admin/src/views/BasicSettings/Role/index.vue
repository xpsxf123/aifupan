<template>
  <div class="page-container br-2 bg-white">
    <div class="curd-wrapper" v-empty="emptyConfig">
      <Curd
        ref="curdRef"
        :table-columns="tableColumns"
        :api="api"
        :before-request="beforeRequest"
        :data-map="dataMap"
        title="角色"
        totalUnit="个"
        show-total
        :show-add="false"
        :action-config="{ edit: true, del: true }"
        :edit-action="handleEdit"
        :border="false"
        @load="handleLoad"
      >
        <template #optionBefore>
          <el-button v-auth="'sys:role:add'" type="primary" round @click="handleAdd">新建角色</el-button>
        </template>
      </Curd>
    </div>

    <RoleDrawer
      v-model="dialogVisible"
      :mode="dialogMode"
      :row="dialogRow"
      :permission-tree-data="permissionTreeData"
      :api="{ add: api.add, edit: api.edit }"
      @success="handleSaved"
    />
  </div>
</template>

<script setup>
  /**
   * @file index.vue
   * @description 角色管理页面
   */
  import { reactive, ref, computed, onMounted, watchEffect } from 'vue'
  import Curd from '@/components/Curd/index.vue'
  import roleApi from '@/http/api/role'
  import RoleDrawer from './components/RoleDrawer.vue'
  import { usePermissionStore } from '@/auth/store'

  const curdRef = ref(null)
  const firstLoaded = ref(false)
  const dialogVisible = ref(false)
  const dialogMode = ref('add')
  const dialogRow = ref({})
  const permissionStore = usePermissionStore()
  const emptyConfig = reactive({
    visible: false,
    title: '暂无角色',
    description: '创建角色后，才能给用户分配权限。',
    blur: 2,
    buttons: [
      {
        text: '新建角色',
        type: 'primary',
        round: true,
        click: () => handleAdd()
      }
    ]
  })

  const emptyButtonsSeed = [...emptyConfig.buttons]

  watchEffect(() => {
    emptyConfig.buttons = permissionStore.hasPermission('sys:role:add') ? emptyButtonsSeed : []
  })

  const isDefaultSearch = () => {
    const params = curdRef.value?.searchParams || {}
    return Object.keys(params).every((k) => {
      const v = params[k]
      if (Array.isArray(v)) return v.length === 0
      return v === undefined || v === null || v === ''
    })
  }

  const handleLoad = (list) => {
    if (!firstLoaded.value) firstLoaded.value = true
    const isEmpty = Array.isArray(list) && list.length === 0
    emptyConfig.visible = firstLoaded.value && isEmpty && isDefaultSearch()
  }

  /**
   * @description 菜单树原始数据（后端返回）
   */
  const rawMenuTree = ref([])

  /**
   * @description 将后端菜单树结构转换为 el-tree 需要的结构
   * @param {Array} nodes - 后端菜单树节点
   * @returns {Array}
   */
  const mapMenuTree = (nodes) => {
    return (nodes || []).map((node) => ({
      id: node.id,
      label: node.name,
      type: node.type,
      children: mapMenuTree(node.children || [])
    }))
  }

  /**
   * @description el-tree 数据源（id/label/children）
   */
  const permissionTreeData = computed(() => mapMenuTree(rawMenuTree.value))

  /**
   * @description 从菜单树中提取所有 id（用于回填勾选项）
   * @param {Array} nodes - 菜单树节点
   * @returns {Array<number>}
   */
  const collectIds = (nodes) => {
    const result = []
    const walk = (list) => {
      ;(list || []).forEach((item) => {
        if (item && item.id !== undefined && item.id !== null) {
          result.push(item.id)
        }
        if (item && item.children && item.children.length) {
          walk(item.children)
        }
      })
    }
    walk(nodes)
    return Array.from(new Set(result))
  }

  /**
   * @description 根据菜单名称匹配默认菜单 id（用于新建角色预选最低可用集）
   * @param {Array} nodes - 菜单树节点
   * @param {Array<string>} names - 菜单名称列表
   * @returns {Array<number>}
   */
  const pickIdsByNames = (nodes, names) => {
    const nameSet = new Set(names || [])
    const result = []
    const walk = (list) => {
      ;(list || []).forEach((item) => {
        if (item && nameSet.has(item.name)) {
          result.push(item.id)
        }
        if (item && item.children && item.children.length) {
          walk(item.children)
        }
      })
    }
    walk(nodes)
    return Array.from(new Set(result))
  }

  /**
   * @description 新建角色默认菜单权限（个人排班/个人业绩）
   */
  const defaultMenuIds = ref([])

  /**
   * @description 加载当前用户菜单树，并计算新建角色默认菜单权限
   * @returns {Promise<void>}
   */
  const loadMenuTree = async () => {
    try {
      const res = await roleApi.userMenuTree()
      rawMenuTree.value = res?.data || []
      defaultMenuIds.value = pickIdsByNames(rawMenuTree.value, ['个人排班', '个人业绩'])
    } catch (e) {
      rawMenuTree.value = []
      defaultMenuIds.value = []
    }
  }

  /**
   * @description 表格列配置
   */
  const tableColumns = [
    { label: '角色名称', prop: 'name', minWidth: 120, align: 'center' },
    { label: '用户数量', prop: 'userCount', minWidth: 100, align: 'center' },
    { label: '创建时间', prop: 'createDate', minWidth: 180, align: 'center' },
    { label: '更新时间', prop: 'updateDate', minWidth: 180, align: 'center' }
  ]

  /**
   * @description 搜索表单配置
   */
  const searchConfig = [
    {
      label: '角色名称',
      prop: 'name',
      type: 'input',
      placeholder: '请输入角色名称'
    }
  ]

  /**
   * @description 页面接口（Curd 约定：list/add/edit/del）
   */
  const api = {
    /**
     * @description 分页查询角色
     * @param {Object} params - 查询参数
     * @returns {Promise}
     */
    list: async (params) => {
      return roleApi.list(params)
    },
    /**
     * @description 新增角色
     * @param {Object} data - 表单数据
     * @returns {Promise}
     */
    add: async (data) => {
      return roleApi.add(data)
    },
    /**
     * @description 修改角色
     * @param {Object} data - 表单数据
     * @returns {Promise}
     */
    edit: async (data) => {
      return roleApi.edit(data)
    },
    /**
     * @description 删除角色
     * @param {Object} row - 行数据
     * @returns {Promise}
     */
    del: async (row) => {
      return roleApi.del({ id: row.id })
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
   * @description 编辑角色前，拉取角色详情并回填 menuIds
   * @param {Object} row - 行数据
   * @returns {Promise<void>}
   */
  const handleEdit = async (row) => {
    try {
      const res = await roleApi.detail({ id: row.id })
      const menuIds = collectIds(res?.data?.menuTree || [])
      dialogMode.value = 'edit'
      dialogRow.value = { ...row, menuIds }
      dialogVisible.value = true
    } catch (e) {
      return
    }
  }

  /**
   * @description 打开新建角色弹窗，并预设默认菜单权限
   */
  const handleAdd = () => {
    dialogMode.value = 'add'
    dialogRow.value = { menuIds: defaultMenuIds.value }
    dialogVisible.value = true
  }

  const handleSaved = () => {
    curdRef.value?.getData()
  }

  /**
   * @description 页面初始化：加载菜单树
   */
  onMounted(() => {
    loadMenuTree()
  })
</script>
