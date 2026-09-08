<template>
  <div class="page-container">
    <div class="header-info">
      <div class="header-left">
        <div class="avtar-container">
          <el-avatar :size="64" :src="activeLiveRoomAvatar || defaultImg" />
        </div>
        <div class="info-content">
          <div class="name">{{ activeLiveRoomName || '-' }}</div>
          <div class="douyin">账号：{{ activeLiveRoomNumber || '-' }}</div>
        </div>
      </div>
      <div class="header-right">
        <div class="info-item">
          <div class="label">所属组织</div>
          <div class="value">{{ activeLiveRoomOrg || '-' }}</div>
        </div>
        <div class="info-item">
          <div class="label">管理者</div>
          <div class="value">{{ activeLiveRoomManagers || '-' }}</div>
        </div>
      </div>
    </div>

    <PageTabs v-model="activeTab" :tabs="tabs" @tab-click="handleTabClick">
      <template #performance>
        <div class="table-container" v-empty="performanceEmptyConfig">
          <Curd
            ref="performanceCurdRef"
            :table-columns="performanceTableColumns"
            :api="dailyApi"
            :search-config="performanceSearchConfig"
            list-permission-code="room:performance:list"
            :layout="performanceLayout"
            title="数据详情"
            show-total
            :show-add="false"
            :show-toolbar-right="false"
            :action-config="{ view: false, edit: false, del: false }"
            :custom-actions="customActions"
            @action="handleDailyAction"
            @load="handlePerformanceLoad"
          >
            <template #export>
              <ExportDataButton
                :data="performanceExportList"
                :columns="performanceTableColumns"
                file-name="直播间历史_按天明细"
              />
            </template>
            <template #staff="{ row }">
              <StaffList :row="row" />
            </template>
          </Curd>
        </div>
      </template>

      <template #session>
        <div class="table-container" v-empty="sessionEmptyConfig">
          <Curd
            ref="sessionCurdRef"
            :table-columns="sessionTableColumns"
            :api="sessionApi"
            :search-config="sessionSearchConfig"
            list-permission-code="room:performance:list"
            :layout="sessionLayout"
            title="数据详情"
            show-total
            :show-add="false"
            :show-toolbar-right="false"
            :action-config="{ view: false, edit: true, del: true }"
            :editAction="openEditSchedule"
            @load="handleSessionLoad"
          >
            <template #sessionActions>
              <el-button v-auth="'room:performance:add'" type="primary" plain round @click="openAddSchedule"
                >录入数据</el-button
              >
              <ExportDataButton
                :data="sessionExportList"
                :columns="sessionTableColumns"
                file-name="直播间历史_按场次明细"
              />
            </template>

            <template #timeRange="{ row }">
              <div class="time-range-cell">
                <div class="range">{{ formatTimeRange(row.startTime, row.endTime) }}</div>
                <div class="duration">{{ row.durationStr }}</div>
              </div>
            </template>

            <template #staff="{ row }">
              <StaffList :row="row" />
            </template>
          </Curd>
        </div>
      </template>
    </PageTabs>

    <SchedulePerformanceDialog
      v-model="scheduleDialogVisible"
      :live-room-id="activeLiveRoomId"
      :live-room-name="activeLiveRoomName"
      :sec-uid="activeLiveRoom?.secUid"
      :platform-type="activeLiveRoom?.platform"
      :performance-id="editingPerformanceId"
      @success="handleScheduleSaved"
    />
  </div>
</template>

<script setup>
  /**
   * @description 直播间业绩历史数据页（按天/按场次）
   */
  import { computed, nextTick, onMounted, reactive, ref, watch, watchEffect } from 'vue'
  import dayjs from 'dayjs'
  import { useRoute, useRouter } from 'vue-router'
  import PageTabs from '@/components/PageTabs/index.vue'
  import Curd from '@/components/Curd/index.vue'
  import ExportDataButton from '@/components/ExportDataButton/index.vue'
  import apiModule from '@/http/api'
  import StaffList from '../components/StaffList.vue'
  import SchedulePerformanceDialog from '@/components/SchedulePerformanceDialog/index.vue'
  import { usePermissionStore } from '@/auth/store'
  import {
    sessionSearchConfig,
    sessionTableColumns,
    performanceSearchConfig,
    performanceTableColumns,
    customActions
  } from '../constants'
  import defaultImg from '@/assets/images/funnel/defaultAvatar.png'

  const route = useRoute()
  const router = useRouter()
  const permissionStore = usePermissionStore()

  const liveRoomId = computed(() => (route.params.liveRoomId ? String(route.params.liveRoomId) : ''))
  const activeTab = ref('performance')
  const tabs = [
    { label: '直播间业绩', value: 'performance' },
    { label: '直播场次', value: 'session' }
  ]

  const performanceCurdRef = ref(null)
  const sessionCurdRef = ref(null)
  const performanceExportList = ref([])
  const sessionExportList = ref([])
  const performanceFirstLoaded = ref(false)
  const sessionFirstLoaded = ref(false)

  const performanceEmptyConfig = reactive({
    visible: false,
    title: '暂无历史业绩数据',
    description: '请先为直播间配置排班，并录入场次业绩后查看。',
    blur: 2,
    buttons: [
      {
        text: '去直播间排班',
        type: 'primary',
        round: true,
        click: () => router.push('/live-room-ranking/index')
      },
      {
        text: '录入场次业绩',
        type: 'primary',
        plain: true,
        round: true,
        click: () => {
          activeTab.value = 'session'
          nextTick(() => openAddSchedule())
        }
      }
    ]
  })

  const performanceEmptyButtonsSeed = [...performanceEmptyConfig.buttons]
  const sessionEmptyButtonsSeed = [...sessionEmptyConfig.buttons]

  watchEffect(() => {
    const canAdd = permissionStore.hasPermission('room:performance:add')

    performanceEmptyConfig.buttons = [
      performanceEmptyButtonsSeed[0],
      ...(canAdd ? [performanceEmptyButtonsSeed[1]] : [])
    ].filter(Boolean)

    sessionEmptyConfig.buttons = [...(canAdd ? [sessionEmptyButtonsSeed[0]] : []), sessionEmptyButtonsSeed[1]].filter(
      Boolean
    )
  })

  const sessionEmptyConfig = reactive({
    visible: false,
    title: '暂无历史场次数据',
    description: '请先录入直播场次业绩数据后查看。',
    blur: 2,
    buttons: [
      {
        text: '录入数据',
        type: 'primary',
        round: true,
        click: () => openAddSchedule()
      },
      {
        text: '返回详情',
        type: 'primary',
        plain: true,
        round: true,
        click: () => router.push(`/live-room-performance/detail/${liveRoomId.value}`)
      }
    ]
  })

  const isDefaultSearch = (curdRef) => {
    const params = curdRef?.value?.searchParams || {}
    return Object.keys(params).every((k) => {
      const v = params[k]
      if (Array.isArray(v)) return v.length === 0
      return v === undefined || v === null || v === ''
    })
  }

  const handlePerformanceLoad = (list) => {
    performanceExportList.value = Array.isArray(list) ? list : []
    if (!performanceFirstLoaded.value) performanceFirstLoaded.value = true
    const isEmpty = Array.isArray(list) && list.length === 0
    performanceEmptyConfig.visible = performanceFirstLoaded.value && isEmpty && isDefaultSearch(performanceCurdRef)
  }

  const handleSessionLoad = (list) => {
    sessionExportList.value = Array.isArray(list) ? list : []
    if (!sessionFirstLoaded.value) sessionFirstLoaded.value = true
    const isEmpty = Array.isArray(list) && list.length === 0
    sessionEmptyConfig.visible = sessionFirstLoaded.value && isEmpty && isDefaultSearch(sessionCurdRef)
  }
  const performanceLayout = [[['title'], ['add', 'option']], [['search'], ['export']], ['table'], ['page']]
  const sessionLayout = [[['title'], ['sessionActions']], ['search'], ['table'], ['page']]

  const activeLiveRoom = ref(null)

  const activeLiveRoomId = computed(() => activeLiveRoom.value?.id)
  const activeLiveRoomName = computed(() => activeLiveRoom.value?.anchorName)
  const activeLiveRoomNumber = computed(() => activeLiveRoom.value?.anchorNumber)
  const activeLiveRoomAvatar = computed(() => activeLiveRoom.value?.anchorAvatar)
  const activeLiveRoomOrg = computed(() => {
    const companyName = activeLiveRoom.value?.companyName
    const deptName = activeLiveRoom.value?.deptName
    const teamName = activeLiveRoom.value?.teamName
    return [companyName, deptName, teamName].filter(Boolean).join('-')
  })
  const activeLiveRoomManagers = computed(() => {
    const list = activeLiveRoom.value?.managerUserInfos || []
    return list
      .map((x) => `${x?.name || ''}${x?.mobile ? `(${x.mobile})` : ''}`)
      .filter((s) => s && s !== '()')
      .join('，')
  })

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
   * @description 日期范围转 startTime/endTime（补齐到当天起止）
   * @param {Array} dateRange - ['YYYY-MM-DD','YYYY-MM-DD']
   * @returns {Object}
   */
  const toStartEndTime = (dateRange) => {
    if (!Array.isArray(dateRange) || dateRange.length !== 2) return {}
    const [startDate, endDate] = dateRange
    if (!startDate || !endDate) return {}
    return {
      startTime: `${startDate} 00:00:00`,
      endTime: `${endDate} 23:59:59`
    }
  }

  /**
   * @description 生成默认查询日期范围（近7天）
   * @returns {Array}
   */
  const getDefaultDateRange = () => [
    dayjs().subtract(7, 'day').format('YYYY-MM-DD'),
    dayjs().subtract(1, 'day').format('YYYY-MM-DD')
  ]

  /**
   * @description 拉取直播间业绩（用于头部信息与统计）
   */
  const fetchLiveRoomPerformance = async () => {
    if (!liveRoomId.value) return
    const baseRes = await apiModule.liveRoom.detail({ id: liveRoomId.value })
    const base = baseRes?.data || {}

    const perfRes = await apiModule.liveRoomPerformance.pageLiveRoomPerformance({
      page: 1,
      limit: 1,
      platform: base?.platform,
      anchorNumber: base?.anchorNumber
    })
    const row = (perfRes?.data?.list || [])[0]

    activeLiveRoom.value = row || { ...base }
  }

  /**
   * @description 日维度统计（Curd list）
   */
  const dailyApi = {
    list: async (params) => {
      const { page, pageSize, anchorName, dateRange } = params || {}
      const { startTime, endTime } = toStartEndTime(dateRange || getDefaultDateRange())
      const res = await apiModule.liveRoomPerformance.dailyStats({
        page: page || 1,
        limit: pageSize || 10,
        startTime,
        endTime,
        anchorName: anchorName || undefined,
        liveRoomId: liveRoomId.value || undefined,
        sortField: 'statsDate',
        sortOrder: 'DESC'
      })

      const list = (res?.data?.list || []).map((row) => ({
        date: row?.statsDate,
        sessionCount: row?.scheduleCount,
        duration: formatMinutes(row?.liveDurationMinutes),
        staffList: (row?.staffList || []).map((x) => ({
          positionName: x?.positionName,
          employeeName: x?.employeeName,
          scheduleCount: x?.scheduleCount
        })),
        views: row?.viewCount,
        sales: row?.salesRevenue,
        refund: row?.refund,
        netSales: row?.netSales,
        adCost: row?.investment,
        roi: row?.roi
      }))

      return {
        list,
        total: res?.data?.totalCount || 0
      }
    }
  }

  /**
   * @description 计算时间段展示
   * @param {string} startTime - 开始时间
   * @param {string} endTime - 结束时间
   * @returns {string}
   */
  const formatTimeRange = (startTime, endTime) => {
    if (!startTime || !endTime) return '-'
    const s = String(startTime).slice(11, 16)
    const e = String(endTime).slice(11, 16)
    return `${s}-${e}`
  }

  /**
   * @description 计算时长字符串
   * @param {string} startTime - 开始时间
   * @param {string} endTime - 结束时间
   * @returns {string}
   */
  const buildDurationStr = (startTime, endTime) => {
    if (!startTime || !endTime) return '-'
    const s = dayjs(startTime)
    const e = dayjs(endTime)
    const diffMin = e.diff(s, 'minute')
    if (!Number.isFinite(diffMin) || diffMin < 0) return '-'
    return formatMinutes(diffMin)
  }

  /**
   * @description 班次明细（Curd list）
   */
  const sessionApi = {
    list: async (params) => {
      const { page, pageSize, anchorName, dateRange } = params || {}
      const { startTime, endTime } = toStartEndTime(dateRange || getDefaultDateRange())
      const res = await apiModule.liveRoomPerformance.pageQuerySchedule({
        page: page || 1,
        limit: pageSize || 10,
        startTime,
        endTime,
        anchorName: anchorName || undefined,
        liveRoomId: liveRoomId.value || undefined
      })

      const list = (res?.data?.list || []).map((row) => ({
        id: row?.id,
        date: row?.startTime ? String(row.startTime).slice(0, 10) : '',
        startTime: row?.startTime,
        endTime: row?.endTime,
        durationStr: buildDurationStr(row?.startTime, row?.endTime),
        staffList: (row?.staffList || []).map((x) => ({
          positionName: x?.positionName,
          employeeName: x?.employeeName
        })),
        views: row?.viewCount,
        sales: row?.salesRevenue,
        refund: row?.refund,
        netSales: row?.netSales,
        adCost: row?.investment,
        roi: row?.roi,
        source: row?.source === 1 ? '系统' : row?.source === 2 ? '手动' : '-'
      }))

      return {
        list,
        total: res?.data?.totalCount || 0
      }
    },
    del: async (row) => {
      return apiModule.liveRoomPerformance.deleteSchedulePerformance({ id: row?.id })
    }
  }

  const scheduleDialogVisible = ref(false)
  const editingPerformanceId = ref('')

  /**
   * @description 新增班次业绩
   */
  const openAddSchedule = () => {
    if (!liveRoomId.value) return
    editingPerformanceId.value = ''
    scheduleDialogVisible.value = true
  }

  /**
   * @description 编辑班次业绩
   * @param {Object} row - 行数据
   */
  const openEditSchedule = (row) => {
    if (!row?.id) return
    editingPerformanceId.value = row.id
    scheduleDialogVisible.value = true
  }

  /**
   * @description 保存成功后刷新列表
   */
  const handleScheduleSaved = () => {
    sessionCurdRef.value?.getData()
    performanceCurdRef.value?.getData()
  }

  /**
   * @description 日维度表格自定义动作：查看场次明细
   * @param {Object} payload - { type, row }
   */
  const handleDailyAction = ({ type, row }) => {
    if (type !== 'viewDetail') return
    activeTab.value = 'session'
    sessionCurdRef.value.searchParams.dateRange = row?.date ? [row.date, row.date] : getDefaultDateRange()
    sessionCurdRef.value.getData()
  }

  const handleTabClick = (tab) => {
    if (tab?.value === 'performance') performanceCurdRef.value?.getData()
    if (tab?.value === 'session') sessionCurdRef.value?.getData()
  }

  watch(
    () => liveRoomId.value,
    () => {
      fetchLiveRoomPerformance()
      performanceCurdRef.value?.getData()
      sessionCurdRef.value?.getData()
    },
    { immediate: true }
  )

  onMounted(() => {
    fetchLiveRoomPerformance()
  })
</script>

<style scoped>
  .avtar-container {
    display: flex;
    align-items: center;
  }

  .header-info {
    background: #fff;
    padding: 28px 20px;
    border-radius: 8px;
    display: flex;
    align-items: center;
    margin-bottom: 16px;
  }

  .header-left {
    display: flex;
    align-items: center;
    padding-right: 60px;
    margin-right: 60px;
    border-right: 1px solid #e4e7ed;
  }

  .header-left .info-content {
    margin-left: 16px;
  }

  .header-left .name {
    font-size: 18px;
    font-weight: bold;
    color: #303133;
    margin-bottom: 8px;
  }

  .header-left .douyin {
    font-size: 14px;
    color: #909399;
  }

  .header-right {
    display: flex;
    gap: 60px;
  }

  .info-item .label {
    font-size: 14px;
    color: #303133;
    margin-bottom: 8px;
  }

  .info-item .value {
    font-size: 14px;
    color: #909399;
    font-weight: 500;
  }

  .table-container {
    background-color: #fff;
    border-radius: var(--border-radius-base);
  }

  .time-range-cell {
    display: flex;
    flex-direction: column;
    line-height: 1.4;
  }

  .time-range-cell .range {
    font-weight: 500;
    color: #303133;
  }

  .time-range-cell .duration {
    font-size: 12px;
    color: #909399;
  }

  :deep(.curd-container) {
    padding: 0;
  }
</style>
