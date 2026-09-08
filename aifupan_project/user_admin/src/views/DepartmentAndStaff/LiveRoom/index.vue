<template>
  <div class="page-container">
    <div class="curd-wrapper" v-empty="emptyConfig">
      <Curd
        ref="curdRef"
        :search-config="searchConfig"
        :table-columns="tableColumns"
        :layoutConfig="{ search: { isCard: true } }"
        :api="api"
        title="直播间"
        totalUnit="个"
        show-total
        :layout="layout"
        :border="false"
        :option-config="{ showThreshold: 0 }"
        :action-config="{ view: false, edit: true, del: true }"
        :del-action="handleDelete"
        :editAction="openEditLiveRoom"
        @load="handleLoad"
      >
        <template #syncLiveRoom>
          <el-button
            v-auth="'room:mgmt:update'"
            type="primary"
            style="margin-left: 0"
            plain
            round
            @click="handleSyncLiveRoom"
          >
            同步直播间
          </el-button>
        </template>

        <template #add>
          <el-button v-auth="'room:mgmt:add'" type="primary" round @click="handleAdd">添加直播间</el-button>
        </template>

        <!-- 直播间列自定义 -->
        <template #liveRoom="{ row }">
          <div class="live-room-cell">
            <div class="avtar-container">
              <el-avatar :size="40" :src="row.anchorAvatar || defaultImg" />
            </div>
            <div class="info">
              <div class="name">{{ row.anchorName }}</div>
              <div class="account">
                <el-icon><User /></el-icon>
                账号: {{ row.anchorNumber }}
              </div>
            </div>
          </div>
        </template>

        <template #industry="{ row }">
          <span>{{ tradeLabelMap[row.tradeId] || '-' }}</span>
        </template>

        <!-- 所属组织列自定义 -->
        <template #org="{ row }">
          <div class="org-cell">
            <div class="company">{{ row.companyName }}</div>
            <div class="dept-group">{{ row.deptName }}{{ row.teamName ? `-${row.teamName}` : '' }}</div>
          </div>
        </template>

        <!-- 管理者列自定义 -->
        <template #manager="{ row }">
          <div class="manager-cell" v-if="row.managerUserInfos && row.managerUserInfos.length">
            <div v-for="user in row.managerUserInfos" :key="user.id"> {{ user.name }} ({{ user.mobile }}) </div>
          </div>
        </template>
      </Curd>
    </div>

    <LiveRoomDrawer
      v-model="drawerVisible"
      :mode="drawerMode"
      :row="drawerRow"
      :org-tree-options="orgTreeOptions"
      :trade-tree-options="tradeTreeOptions"
      :cascader-props="cascaderProps"
      @success="handleSaved"
    />
  </div>
</template>

<script setup>
  /**
   * @file index.vue
   * @description 直播间管理页面
   */
  import { reactive, ref, onMounted } from 'vue'
  import { useRouter } from 'vue-router'
  import Curd from '@/components/Curd/index.vue'
  import { ElMessage } from 'element-plus'
  import { User } from '@element-plus/icons-vue'
  import apiModule from '@/http/api'
  import LiveRoomDrawer from '@/components/LiveRoomDrawer/index.vue'
  import { shouldShowGuideEmpty } from '@/utils/empty'
  import { usePermissionStore } from '@/auth/store'
  import defaultImg from '@/assets/images/funnel/defaultAvatar.png'

  const curdRef = ref(null)
  const router = useRouter()
  const permissionStore = usePermissionStore()
  const firstLoaded = ref(false)
  const emptyConfig = reactive({
    visible: false,
    title: '暂无直播间',
    description: '',
    blur: 2,
    buttons: []
  })
  const layout = [['search'], [['title'], ['add', 'syncLiveRoom', 'option']], ['table'], ['page']]
  const orgTreeOptions = ref([])
  const tradeTreeOptions = ref([])
  const tradeLabelMap = ref({})

  const drawerVisible = ref(false)
  const drawerMode = ref('add')
  const drawerRow = ref({})

  const cascaderProps = {
    value: 'value',
    label: 'label',
    children: 'children',
    emitPath: true,
    checkStrictly: true,
    showPrefix: false
  }

  const buildTradeLabelMap = (nodes, map = {}) => {
    if (!Array.isArray(nodes)) return map
    nodes.forEach((node) => {
      const value = node?.value
      const label = node?.label
      if (value !== undefined && value !== null) map[value] = label
      if (Array.isArray(node?.children) && node.children.length) buildTradeLabelMap(node.children, map)
    })
    return map
  }

  const getTradeTreeOptions = async () => {
    try {
      const res = await apiModule.trade.tree({
        level: 4
      })
      const normalize = (nodes) => {
        if (!Array.isArray(nodes)) return undefined
        return nodes
          .map((node) => {
            const value = node?.key ?? node?.id ?? node?.value
            const label = node?.label ?? node?.name
            if (value === undefined || value === null) return null
            return {
              value,
              label,
              children: node?.children && node.children.length ? normalize(node.children) : undefined
            }
          })
          .filter(Boolean)
      }
      tradeTreeOptions.value = normalize(res.data) || []
      tradeLabelMap.value = buildTradeLabelMap(tradeTreeOptions.value, {})
    } catch (e) {
      console.error(e)
    }
  }
  const getOrgTreeOptions = async () => {
    try {
      const res = await apiModule.org.tree({
        level: 4
      })
      const mapTree = (nodes, level = 0) => {
        if (!nodes) return undefined
        return nodes.map((node) => ({
          value: node.key,
          label: node.label,
          children: node.children && node.children.length ? mapTree(node.children, level + 1) : undefined
        }))
      }
      orgTreeOptions.value = mapTree(res.data) || []
    } catch (e) {
      console.error(e)
    }
  }

  onMounted(() => {
    getOrgTreeOptions()
    getTradeTreeOptions()
  })

  /**
   * @description 检查是否存在子公司数据（直播间前置条件）
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
   * @description Curd 首次加载后空数据引导（直播间依赖子公司）
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
    emptyConfig.title = '暂无直播间'
    emptyConfig.description = companyOk
      ? '当前没有直播间数据。你可以直接新增直播间。'
      : '当前没有直播间数据。添加直播间前需要先添加子公司。'
    const canAdd = permissionStore.hasPermission('room:mgmt:add')

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
              text: '新增直播间',
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

  // 搜索配置
  const searchConfig = [
    {
      label: '所属平台',
      prop: 'platform',
      type: 'select',
      placeholder: '全部',
      options: [
        { label: '全部', key: '' },
        { label: '抖音', key: 0 },
        { label: '快手', key: 1 },
        { label: '视频号', key: 2 }
      ]
    },
    {
      label: '直播间名称',
      prop: 'anchorName',
      type: 'input',
      placeholder: '请输入'
    },
    {
      label: '所属组织',
      prop: 'orgId',
      type: 'cascader',
      options: orgTreeOptions,
      props: {
        value: 'value',
        label: 'label',
        children: 'children',
        showPrefix: true
        // emitPath: true,
        // checkStrictly: true
      },
      opetion: { filterable: true },
      placeholder: '请选择组织'
    }
  ]

  // 表格列配置
  const tableColumns = [
    {
      label: '直播间',
      prop: 'liveRoom',
      minWidth: 200,
      slotName: 'liveRoom'
    },
    {
      label: '行业',
      prop: 'tradeId',
      minWidth: 150,
      slotName: 'industry',
      align: 'center'
    },
    {
      label: '所属组织',
      prop: 'org',
      minWidth: 200,
      slotName: 'org',
      align: 'center'
    },
    {
      label: '管理者',
      prop: 'manager',
      minWidth: 150,
      slotName: 'manager',
      align: 'center'
    }
  ]

  const getOrgPath = (row = {}) => {
    const path = []
    if (row.companyId) path.push(row.companyId)
    if (row.deptId) path.push(row.deptId)
    if (row.teamId) path.push(row.teamId)
    return path
  }

  const mapFormPayload = (data = {}) => {
    const orgPath = Array.isArray(data.orgPath) ? data.orgPath : []
    return {
      id: data.id,
      platform: data.platform,
      anchorNumber: data.anchorNumber,
      tradeId: data.tradeId,
      debutDate: data.debutDate,
      companyId: orgPath[0] || '',
      deptId: orgPath[1] || '',
      teamId: orgPath[2] || '',
      managerUserIds: data.managerUserIds || []
    }
  }

  const api = {
    list: async (params) => {
      const { orgId, pageSize, ...rest } = params || {}
      const query = {
        ...rest,
        limit: pageSize,
        page: rest.page
      }

      // 处理组织搜索级联 (orgId 为数组)
      if (orgId && orgId.length > 0) {
        if (orgId[0]) query.companyId = orgId[0]
        if (orgId[1]) query.deptId = orgId[1]
        if (orgId[2]) query.teamId = orgId[2]
      }

      delete query.pageSize

      const res = await apiModule.liveRoom.list(query)
      return {
        list:
          (res.data?.list || []).map((row) => ({
            ...row,
            orgPath: getOrgPath(row),
            managerUserIds: row?.managerUserInfos?.map((u) => u.id) || [],
            managerUserInfos: row?.managerUserInfos ? [...row.managerUserInfos] : []
          })) || [],
        total: res.data?.totalCount || 0
      }
    },
    del: async (row) => {
      return await apiModule.liveRoom.del({ id: row.id })
    },
    add: async (data) => {
      return await apiModule.liveRoom.add(mapFormPayload(data))
    },
    edit: async (data) => {
      return await apiModule.liveRoom.edit(mapFormPayload(data))
    }
  }

  const handleAdd = () => {
    drawerMode.value = 'add'
    drawerRow.value = {
      platform: '',
      anchorNumber: '',
      tradeId: '',
      debutDate: '',
      orgPath: [],
      managerUserIds: [],
      managerUserInfos: []
    }
    drawerVisible.value = true
  }

  const openEditLiveRoom = (row) => {
    drawerMode.value = 'edit'
    drawerRow.value = row || {}
    drawerVisible.value = true
  }

  const handleSaved = () => {
    curdRef.value?.getData()
  }

  const handleDelete = async (row) => {
    try {
      await api.del(row)
      ElMessage.success('删除成功')
      curdRef.value?.getData()
    } catch (e) {
      console.error(e)
    }
  }

  const handleSyncLiveRoom = async () => {
    try {
      const params = curdRef.value?.searchParams || {}
      const payload = {}
      if (params.platform !== '' && params.platform !== undefined && params.platform !== null) {
        payload.platform = Number(params.platform)
      }
      const res = await apiModule.liveRoom.syncTenantAnchors(payload)
      const result = res?.data || {}
      const total = result?.total ?? 0
      const created = result?.created ?? 0
      const updated = result?.updated ?? 0
      ElMessage.success(`同步成功：共${total}，新增${created}，更新${updated}`)
      curdRef.value?.getData()
    } catch (e) {
      console.error(e)
    }
  }
</script>

<style scoped lang="scss">
  .avtar-container {
    display: flex;
    align-items: center;
  }

  .live-room-cell {
    display: flex;
    align-items: center;
    gap: 12px;

    .info {
      display: flex;
      flex-direction: column;
      align-items: flex-start;

      .name {
        font-weight: 600;
        color: #303133;
        font-size: 14px;
      }

      .account {
        font-size: 12px;
        color: #909399;
        display: flex;
        align-items: center;
        gap: 4px;
        margin-top: 4px;
      }
    }
  }

  .auth-cell {
    display: flex;
    flex-direction: column;
    gap: 4px;

    .auth-item {
      display: flex;
      align-items: center;
      gap: 6px;
      font-size: 12px;

      .dot {
        width: 6px;
        height: 6px;
        border-radius: 50%;

        &.success {
          background-color: #67c23a;
        }
        &.error {
          background-color: #f56c6c;
        } // Or gray for 'unauthorized' if preferred, but red for 'expired' matches typical UI
      }

      .label {
        &.success {
          color: #67c23a;
        }
        &.error {
          color: #f56c6c;
        }
      }
    }
  }

  .org-cell {
    display: flex;
    flex-direction: column;
    align-items: center;
    line-height: 1.4;

    .company {
      color: #303133;
    }

    .dept-group {
      font-size: 12px;
      color: #909399;
    }
  }
</style>
