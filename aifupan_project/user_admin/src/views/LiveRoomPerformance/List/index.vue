<template>
  <div class="page-container">
    <LiveRoomList
      :list="liveRoomCardList"
      :total="liveRoomTotal"
      :loading="liveRoomLoading"
      :stats-config="liveRoomStatsConfig"
      :unit-config="unitConfig"
      height="calc(100vh - 240px)"
      @search="handleLiveRoomSearch"
      @reset="handleLiveRoomReset"
      @select="handleGoDetail"
      @history="handleGoHistory"
      @edit="handleGoEdit"
    />

    <div class="list-page" v-if="liveRoomTotal > 0">
      <el-pagination
        v-model:current-page="liveRoomPage"
        v-model:page-size="liveRoomPageSize"
        :total="liveRoomTotal"
        background
        layout="total, prev, pager, next, sizes"
        :page-sizes="[10, 20, 50, 100]"
        @current-change="fetchLiveRoomList"
        @size-change="handleLiveRoomSizeChange"
      />
    </div>

    <!-- 直播间编辑弹窗 -->
    <CurdForm
      v-model:visible="editDialogVisible"
      mode="edit"
      :form-config="formConfig"
      v-model="editFormData"
      :loading="editFormLoading"
      labelPosition="top"
      width="512px"
      type="dialog"
      custom-class="common-dialog"
      @submit="handleEditSubmit"
    >
      <template #managerSelect="{ form }">
        <ManagerSelect v-model="form.managerUserIds" v-model:users="form.managerUserInfos" />
      </template>
    </CurdForm>
  </div>
</template>

<script setup>
  /**
   * @file index.vue
   * @description 直播间业绩列表页（筛选区 + 卡片列表）
   */
  import { onMounted, ref } from 'vue'
  import { useRouter } from 'vue-router'
  import { ElMessage } from 'element-plus'
  import apiModule from '@/http/api'
  import LiveRoomList from '../components/LiveRoomList/index.vue'
  import { liveRoomStatsConfig, unitConfig } from '../constants'
  import CurdForm from '@/components/Curd/Form/index.vue'
  import ManagerSelect from '@/components/ManagerSelect/index.vue'

  const router = useRouter()

  const liveRoomLoading = ref(false)
  const liveRoomCardList = ref([])
  const liveRoomTotal = ref(0)
  const liveRoomPage = ref(1)
  const liveRoomPageSize = ref(10)
  const liveRoomQuery = ref({})

  /**
   * @description 获取直播间ID（兼容不同字段命名）
   * @param {Object} item - 列表项数据
   * @returns {string}
   */
  const getLiveRoomId = (item) => {
    const raw = item?.raw || item || {}
    return String(raw.id || raw.liveRoomId || raw.roomId || item?.id || item?.liveRoomId || item?.roomId || '')
  }

  /**
   * @description 分钟转时长字符串
   * @param {number} minutes - 分钟数
   * @returns {string}
   */
  const formatMinutes = (minutes) => {
    const m = Number(minutes) || 0
    const h = Math.floor(m / 60)
    const mm = m % 60
    if (h <= 0) return `${mm}分`
    if (mm <= 0) return `${h}小时`
    return `${h}小时${mm}分`
  }

  /**
   * @description 班次统计格式化
   * @param {Object} stats - SessionStats
   * @returns {string}
   */
  const formatSessionStats = (stats) => {
    const count = Number(stats?.count) || 0
    const duration = formatMinutes(stats?.duration)
    return `${count} (${duration})`
  }

  /**
   * @description 将直播间业绩响应映射为卡片数据结构
   * @param {Object} row - LiveRoomPerformanceResponse
   * @returns {Object}
   */
  const mapToCard = (row) => {
    const ps = row?.performanceStatistics || {}
    const ss = ps?.sessionStats || {}
    const vc = ps?.viewCount || {}
    const sales = ps?.salesRevenue || {}
    const refund = ps?.refund || {}
    const netSales = ps?.netSales || {}
    const inv = ps?.investment || {}

    const managers = (row?.managerUserInfos || [])
      .map((x) => `${x?.name || ''}${x?.mobile ? `(${x.mobile})` : ''}`)
      .filter((s) => s && s !== '()')
      .join('，')

    return {
      id: row?.id,
      raw: row,
      header: {
        name: row?.anchorName,
        avatar: row?.anchorAvatar,
        tags: [],
        org: [row?.companyName, row?.deptName, row?.teamName].filter(Boolean).join('-'),
        managers
      },
      stats: {
        session: formatSessionStats(ss?.today),
        sessionYesterday: formatSessionStats(ss?.yesterday),
        sessionWeek: formatSessionStats(ss?.thisWeek),
        sessionMonth: formatSessionStats(ss?.thisMonth),

        views: vc?.today,
        viewsYesterday: vc?.yesterday,
        viewsWeek: vc?.thisWeek,
        viewsMonth: vc?.thisMonth,

        sales: sales?.today,
        salesYesterday: sales?.yesterday,
        salesWeek: sales?.thisWeek,
        salesMonth: sales?.thisMonth,

        refund: refund?.today,
        refundYesterday: refund?.yesterday,
        refundWeek: refund?.thisWeek,
        refundMonth: refund?.thisMonth,

        netSales: netSales?.today,
        netSalesYesterday: netSales?.yesterday,
        netSalesWeek: netSales?.thisWeek,
        netSalesMonth: netSales?.thisMonth,

        adSpend: inv?.today,
        adSpendYesterday: inv?.yesterday,
        adSpendWeek: inv?.thisWeek,
        adSpendMonth: inv?.thisMonth
      }
    }
  }

  /**
   * @description 拉取直播间业绩列表（卡片）
   */
  const fetchLiveRoomList = async () => {
    liveRoomLoading.value = true
    try {
      const q = liveRoomQuery.value || {}
      const res = await apiModule.liveRoomPerformance.pageLiveRoomPerformance({
        page: liveRoomPage.value,
        limit: liveRoomPageSize.value,
        platform: q.platform === '' ? undefined : q.platform,
        companyId: q.companyId || undefined,
        deptId: q.deptId || undefined,
        teamId: q.teamId || undefined,
        anchorName: q.anchorName || undefined
      })
      const list = res?.data?.list || []
      liveRoomCardList.value = list.map(mapToCard)
      liveRoomTotal.value = res?.data?.totalCount || 0
    } finally {
      liveRoomLoading.value = false
    }
  }

  /**
   * @description 直播间列表查询
   * @param {Object} params - 列表筛选参数
   */
  const handleLiveRoomSearch = (params) => {
    liveRoomQuery.value = params || {}
    liveRoomPage.value = 1
    fetchLiveRoomList()
  }

  /**
   * @description 直播间列表重置
   */
  const handleLiveRoomReset = () => {
    liveRoomQuery.value = {}
    liveRoomPage.value = 1
    fetchLiveRoomList()
  }

  /**
   * @description 直播间列表分页大小变更
   */
  const handleLiveRoomSizeChange = () => {
    liveRoomPage.value = 1
    fetchLiveRoomList()
  }

  /**
   * @description 跳转直播间业绩详情
   * @param {Object} item - LiveRoomCard 数据
   */
  const handleGoDetail = (item) => {
    const id = getLiveRoomId(item)
    if (!id) {
      ElMessage.warning('缺少直播间ID，无法进入详情')
      return
    }
    router.push(`/live-room-performance/detail/${id}`)
  }

  /**
   * @description 跳转直播间业绩历史页
   * @param {Object} item - LiveRoomCard 数据
   */
  const handleGoHistory = (item) => {
    const id = getLiveRoomId(item)
    if (!id) {
      ElMessage.warning('缺少直播间ID，无法进入历史页')
      return
    }
    router.push(`/live-room-performance/history/${id}`)
  }

  // ===== 直播间编辑相关 =====
  const editDialogVisible = ref(false)
  const editFormLoading = ref(false)
  const editFormData = ref({})

  const companyOptions = ref([])
  const tradeTreeOptions = ref([])

  const getCompanyOptions = async (keyword = '') => {
    try {
      const res = await apiModule.subCompany.options({ keyword, limit: 50 })
      companyOptions.value = res.data?.map((i) => ({ ...i, key: i.key, label: i.label })) || []
    } catch (e) {
      void e
    }
  }

  const getTradeTreeOptions = async () => {
    try {
      const res = await apiModule.trade.tree()
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
    } catch (e) {
      void e
    }
  }

  const formConfig = ref([
    {
      label: '所属平台',
      prop: 'platform',
      type: 'select',
      rules: [{ required: true, message: '请选择平台', trigger: 'change' }],
      options: [
        { label: '抖音', key: 0 },
        { label: '快手', key: 1 },
        { label: '视频号', key: 2 }
      ],
      width: '392px'
    },
    {
      label: '账号ID',
      prop: 'anchorNumber',
      type: 'input',
      placeholder: '请输入',
      rules: [{ required: true, message: '请输入账号ID', trigger: 'blur' }],
      width: '392px'
    },
    {
      label: '行业选择',
      prop: 'tradeId',
      type: 'cascader',
      options: tradeTreeOptions,
      props: {
        value: 'value',
        label: 'label',
        children: 'children',
        emitPath: false,
        checkStrictly: true
      },
      placeholder: '请选择行业',
      width: '392px'
    },
    {
      label: '首播日期',
      prop: 'debutDate',
      type: 'date',
      placeholder: '选择时间',
      width: '392px'
    },
    {
      label: '所属组织',
      prop: 'companyId',
      type: 'select',
      rules: [{ required: true, message: '请选择所属组织', trigger: 'change' }],
      options: companyOptions,
      opetion: {
        filterable: true,
        remote: true,
        remoteMethod: getCompanyOptions
      },
      placeholder: '请选择子公司',
      width: '392px'
    },
    {
      label: '管理员',
      prop: 'managerUserIds',
      slotName: 'managerSelect',
      width: '392px',
      class: 'manager-user-ids'
    }
  ])

  /**
   * @description 唤起编辑弹窗
   * @param {Object} item - LiveRoomCard 数据
   */
  const handleGoEdit = async (item) => {
    const id = getLiveRoomId(item)
    if (!id) return
    try {
      const res = await apiModule.liveRoom.detail({ id })
      const rawData = res?.data || item.raw
      editFormData.value = {
        ...rawData,
        managerUserIds: rawData.managerUserInfos?.map((u) => u.id) || []
      }
      editDialogVisible.value = true
    } catch (e) {
      void e
    }
  }

  const handleEditSubmit = async (data) => {
    editFormLoading.value = true
    try {
      const payload = {
        id: data.id,
        tradeId: data.tradeId,
        companyId: data.companyId,
        managerUserIds: data.managerUserIds || [],
        platform: data.platform,
        debutDate: data.debutDate,
        anchorNumber: data.anchorNumber
      }
      await apiModule.liveRoom.edit(payload)
      ElMessage.success('修改成功')
      editDialogVisible.value = false
      fetchLiveRoomList()
    } catch (e) {
      void e
    } finally {
      editFormLoading.value = false
    }
  }

  onMounted(() => {
    fetchLiveRoomList()
    getCompanyOptions()
    getTradeTreeOptions()
  })
</script>

<style scoped>
  .list-page {
    background: #fff;
    border-radius: 8px;
    padding: 12px 16px;
    display: flex;
    justify-content: center;
    margin-top: 12px;
  }

  :deep(.form-container) {
    width: 392px;
  }

  :deep(.el-dialog__body) {
    display: flex;
    justify-content: center;
  }

  :deep(.manager-user-ids) {
    display: flex;
    gap: 30px;
  }

  :deep(.manager-user-ids .el-form-item__label) {
    display: flex;
    align-items: center;
    margin-bottom: 0;
  }

  :deep(.manager-user-ids .el-form-item__label) {
    align-items: flex-start;
  }

  :deep(.manager-user-ids .manager-select-trigger) {
    display: flex;
    flex-direction: column;

    .el-button {
      align-items: flex-start;
      justify-content: flex-start;
      line-height: 1.3;
    }
  }

  :deep(.common-dialog .el-dialog__footer) {
    display: flex;
    justify-content: center;
    border-top: none;
    padding: 0 20px 40px 20px;

    .dialog-footer {
      display: inline-block;
      width: 392px;

      .el-button {
        border-radius: 50px;
        width: 84px;
        height: 34px;
      }
    }
  }
</style>
