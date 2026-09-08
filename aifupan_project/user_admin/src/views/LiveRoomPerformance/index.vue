<template>
  <div class="page-container">
    <div class="header-info">
      <img :src="mapPlatform[activeLiveRoom?.platform]" alt="" class="platform-icon" />
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
      <!--      <template #attached-content>
        <el-button type="primary" link @click="handleGoHistory">查看历史数据</el-button>
      </template>-->

      <template #performance>
        <div class="performance-content">
          <DataCardGroup :cards="performanceCards">
            <template #icon-0
              ><el-icon><VideoCamera /></el-icon
            ></template>
            <template #icon-1
              ><el-icon><View /></el-icon
            ></template>
            <template #icon-2
              ><el-icon><Money /></el-icon
            ></template>
            <template #icon-3
              ><el-icon><Wallet /></el-icon
            ></template>
            <template #icon-4
              ><el-icon><Coin /></el-icon
            ></template>
            <template #icon-5
              ><el-icon><Promotion /></el-icon
            ></template>
          </DataCardGroup>

          <div class="trendChart-data-class">
            <DataTrendChart :chart-data="trendChartData">
              <template #describe>
                <div class="subtitle-container">
                  <el-radio-group v-model="trendMetric">
                    <el-radio-button label="view">场观</el-radio-button>
                    <el-radio-button label="sales">销售额</el-radio-button>
                    <el-radio-button label="refund">退款</el-radio-button>
                    <el-radio-button label="netSales">净销售</el-radio-button>
                    <el-radio-button label="ads">投放</el-radio-button>
                  </el-radio-group>
                  <div class="trend-tools">
                    <el-radio-group v-model="trendQuick" @change="handleTrendQuickChange">
                      <el-radio-button label="yesterday">昨日</el-radio-button>
                      <el-radio-button label="7">近7日</el-radio-button>
                      <el-radio-button label="30">近30日</el-radio-button>
                    </el-radio-group>
                    <el-date-picker
                      v-model="trendDateRange"
                      type="daterange"
                      value-format="YYYY-MM-DD"
                      format="YYYY-MM-DD"
                      range-separator="-"
                      start-placeholder="开始日期"
                      end-placeholder="结束日期"
                      class="trend-date"
                      style="width: 200px"
                      @change="handleTrendDateChange"
                    />
                  </div>
                </div>
              </template>
            </DataTrendChart>
          </div>

          <div class="table-container" v-empty="performanceEmptyConfig">
            <Curd
              ref="performanceCurdRef"
              :table-columns="performanceTableColumns"
              :api="dailyApi"
              :search-config="performanceSearchConfig"
              list-permission-code="room:performance:list"
              :layout="performanceLayout"
              title="数据详情"
              :show-add="false"
              :show-toolbar-right="false"
              :operationWidth="150"
              :action-config="{ view: false, edit: false, del: false }"
              :custom-actions="customActions"
              @action="handleDailyAction"
              @load="handlePerformanceLoad"
            >
              <template #export>
                <ExportDataButton
                  :data="performanceExportList"
                  :columns="performanceTableColumns"
                  file-name="直播间业绩_按天明细"
                />
              </template>
              <template #staff="{ row }">
                <StaffList :staffList="row.staffList" />
              </template>
            </Curd>
          </div>
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
            :show-add="false"
            :show-toolbar-right="false"
            :operationWidth="150"
            :table-config="{ actionButtonSize: 'default' }"
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
                file-name="直播间业绩_按场次明细"
              />
            </template>

            <template #timeRange="{ row }">
              <div class="time-range-cell">
                <div class="range">{{ formatTimeRange(row.startTime, row.endTime) }}</div>
                <div class="duration">{{ row.durationStr }}</div>
              </div>
            </template>

            <template #staff="{ row }">
              <StaffList :staffList="row.staffList" />
            </template>

            <template #viewCount="{ row }">
              <span :class="['metric-cell', row.viewCountModified && 'metric-cell--modified']">{{
                row.viewCount === 0 ? 0 : row.viewCount || ''
              }}</span>
            </template>

            <template #salesRevenue="{ row }">
              <span :class="['metric-cell', row.salesRevenueModified && 'metric-cell--modified']">{{
                row.salesRevenue === 0 ? 0 : row.salesRevenue || ''
              }}</span>
            </template>

            <template #refund="{ row }">
              <span :class="['metric-cell', row.refundModified && 'metric-cell--modified']">{{
                row.refund === 0 ? 0 : row.refund || ''
              }}</span>
            </template>

            <template #netSales="{ row }">
              <span :class="['metric-cell', row.netSalesModified && 'metric-cell--modified']">{{
                row.netSales === 0 ? 0 : row.netSales || ''
              }}</span>
            </template>

            <template #investment="{ row }">
              <span :class="['metric-cell', row.investmentModified && 'metric-cell--modified']">{{
                row.investment === 0 ? 0 : row.investment || ''
              }}</span>
            </template>

            <template #roi="{ row }">
              <span :class="['metric-cell', row.roiModified && 'metric-cell--modified']">{{
                row.roi === 0 ? 0 : row.roi || ''
              }}</span>
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
   * @file index.vue
   * @description 直播间业绩详情页（概览 + 趋势 + 按天/按场次明细）
   */
  import { computed, nextTick, reactive, ref, watch, watchEffect } from 'vue'
  import dayjs from 'dayjs'
  import { useRoute, useRouter } from 'vue-router'
  import PageTabs from '@/components/PageTabs/index.vue'
  import Curd from '@/components/Curd/index.vue'
  import DataCardGroup from '@/components/DataCardGroup/index.vue'
  import DataTrendChart from '@/components/DataTrendChart/index.vue'
  import ExportDataButton from '@/components/ExportDataButton/index.vue'
  import StaffList from './components/StaffList.vue'
  import SchedulePerformanceDialog from '@/components/SchedulePerformanceDialog/index.vue'
  import { VideoCamera, View, Money, Wallet, Coin, Promotion } from '@element-plus/icons-vue'
  import apiModule from '@/http/api'
  import { usePermissionStore } from '@/auth/store'
  import {
    sessionSearchConfig,
    sessionTableColumns,
    performanceSearchConfig,
    performanceTableColumns,
    customActions
  } from './constants'
  import dy from '@/assets/images/icon/dy20.png'
  import ks from '@/assets/images/icon/ks20.png'
  import sph from '@/assets/images/icon/sph20.png'
  import defaultImg from '@/assets/images/funnel/defaultAvatar.png'

  const route = useRoute()
  const router = useRouter()

  const sessionDateRange = computed(() => {
    const normalize = (value) => (Array.isArray(value) ? value[0] : value)
    const startTime = normalize(route.query?.startTime)
    const endTime = normalize(route.query?.endTime)
    if (!startTime || !endTime) return []
    return [String(startTime).slice(0, 10), String(endTime).slice(0, 10)]
  })

  const permissionStore = usePermissionStore()
  const mapPlatform = {
    0: dy,
    1: ks,
    2: sph
  }
  const liveRoomId = computed(() => (route.params.liveRoomId ? String(route.params.liveRoomId) : ''))
  console.log('route.params.liveRoomId', liveRoomId)

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
    title: '暂无业绩数据',
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

  const sessionEmptyConfig = reactive({
    visible: false,
    title: '暂无场次数据',
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
        text: '返回列表',
        type: 'primary',
        plain: true,
        round: true,
        click: () => handleBackList()
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

  const periodStats = ref(null)

  /**
   * @description 分钟转时长字符串
   * @param {number} minutes - 分钟数
   * @returns {string}
   */
  const formatMinutes = (minutes) => {
    const m = Number(minutes) || 0
    const h = Math.floor(m / 60)
    const mm = m % 60
    if (h <= 0) return `${mm}分钟`
    if (mm <= 0) return `${h}小时`
    return `${h}小时${mm}分钟`
  }

  /**
   * @description 班次统计格式化
   * @param {Object} stats - SessionStats
   * @returns {string}
   */
  const formatSessionStats = (stats) => {
    const count = Number(stats?.count) || 0
    const minutes = Number(stats?.duration) || 0
    const durationText = formatMinutes(minutes)
    return `${count} (${durationText})`
  }

  /**
   * @description 生成概览卡片数据
   */
  const performanceCards = computed(() => {
    const ps = periodStats.value || {}
    const ss = ps?.sessionStats || {}
    const vc = ps?.viewCount || {}
    const sales = ps?.salesRevenue || {}
    const refund = ps?.refund || {}
    const netSales = ps?.netSales || {}
    const inv = ps?.investment || {}

    return [
      {
        title: '直播场次',
        icon: 'VideoCamera',
        iconColor: '#409eff',
        iconBgColor: '#ecf5ff',
        data: {
          today: formatSessionStats(ss?.today),
          yesterday: formatSessionStats(ss?.yesterday),
          thisWeek: formatSessionStats(ss?.thisWeek),
          lastWeek: formatSessionStats(ss?.lastWeek),
          thisMonth: formatSessionStats(ss?.thisMonth),
          lastMonth: formatSessionStats(ss?.lastMonth)
        }
      },
      {
        title: '场观',
        icon: 'View',
        iconColor: '#e6a23c',
        iconBgColor: '#fdf6ec',
        data: vc
      },
      {
        title: '销售额',
        icon: 'Money',
        iconColor: '#f56c6c',
        iconBgColor: '#fef0f0',
        data: sales,
        config: [
          { key: 'today', label: '今日', unit: 'w' },
          { key: 'yesterday', label: '昨日', unit: 'w' },
          { key: 'thisWeek', label: '本周', unit: 'w' },
          { key: 'lastWeek', label: '上周', unit: 'w' },
          { key: 'thisMonth', label: '本月', unit: 'w' },
          { key: 'lastMonth', label: '上月', unit: 'w' }
        ]
      },
      {
        title: '退款',
        icon: 'Wallet',
        iconColor: '#67c23a',
        iconBgColor: '#f0f9eb',
        data: refund,
        config: [
          { key: 'today', label: '今日', unit: 'w' },
          { key: 'yesterday', label: '昨日', unit: 'w' },
          { key: 'thisWeek', label: '本周', unit: 'w' },
          { key: 'lastWeek', label: '上周', unit: 'w' },
          { key: 'thisMonth', label: '本月', unit: 'w' },
          { key: 'lastMonth', label: '上月', unit: 'w' }
        ]
      },
      {
        title: '净销售额',
        icon: 'Coin',
        iconColor: '#909399',
        iconBgColor: '#f4f4f5',
        data: netSales,
        config: [
          { key: 'today', label: '今日', unit: 'w' },
          { key: 'yesterday', label: '昨日', unit: 'w' },
          { key: 'thisWeek', label: '本周', unit: 'w' },
          { key: 'lastWeek', label: '上周', unit: 'w' },
          { key: 'thisMonth', label: '本月', unit: 'w' },
          { key: 'lastMonth', label: '上月', unit: 'w' }
        ]
      },
      {
        title: '投放',
        icon: 'Promotion',
        iconColor: '#ff90bc',
        iconBgColor: '#fcecf2',
        data: inv,
        config: [
          { key: 'today', label: '今日', unit: 'w' },
          { key: 'yesterday', label: '昨日', unit: 'w' },
          { key: 'thisWeek', label: '本周', unit: 'w' },
          { key: 'lastWeek', label: '上周', unit: 'w' },
          { key: 'thisMonth', label: '本月', unit: 'w' },
          { key: 'lastMonth', label: '上月', unit: 'w' }
        ]
      }
    ]
  })

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
   * @description 生成默认查询日期范围（近30天）
   * @returns {Array}
   */
  const getDefaultDateRange = () => {
    const end = dayjs().subtract(1, 'day')
    const start = end.subtract(29, 'day')
    return [start.format('YYYY-MM-DD'), end.format('YYYY-MM-DD')]
  }

  /**
   * @description 拉取直播间业绩（用于头部信息与统计）
   */
  const fetchLiveRoomPerformance = async () => {
    if (!liveRoomId.value) return
    const res = await apiModule.liveRoom.detail({ id: liveRoomId.value })
    activeLiveRoom.value = res?.data || null
  }

  const fetchPeriodStats = async () => {
    if (!liveRoomId.value) return
    const res = await apiModule.performanceSummary.periodStats({
      sourceId: liveRoomId.value,
      sourceType: 'liveRoom'
    })
    periodStats.value = res?.data || null
  }

  const trendMetric = ref('view')
  const trendQuick = ref('30')
  const trendDateRange = ref(getDefaultDateRange())
  const trendChartData = reactive({ xData: [], yData: [] })
  const trendRaw = ref([])

  const refreshTrendChart = () => {
    const metricMap = {
      view: 'viewCount',
      sales: 'salesRevenue',
      refund: 'refund',
      netSales: 'netSales',
      ads: 'investment'
    }
    const field = metricMap[trendMetric.value] || 'viewCount'

    trendChartData.xData = (trendRaw.value || []).map((x) => {
      const d = String(x?.date || '')
      if (!d) return '-'
      return `${Number(d.slice(5, 7))}/${Number(d.slice(8, 10))}`
    })
    trendChartData.yData = (trendRaw.value || []).map((x) => Number(x?.[field]) || 0)
  }

  /**
   * @description 拉取数据趋势（柱形图）
   */
  const fetchTrend = async () => {
    if (!liveRoomId.value) return
    const range = Array.isArray(trendDateRange.value) && trendDateRange.value.length === 2 ? trendDateRange.value : []
    const [startDate, endDate] = range.length === 2 ? range : getDefaultDateRange()

    const res = await apiModule.performanceSummary.trend({
      sourceId: liveRoomId.value,
      sourceType: 'liveRoom',
      startDate,
      endDate
    })
    const raw = Array.isArray(res?.data) ? res.data : []

    const sortedRaw = [...raw].sort((a, b) => {
      const da = String(a?.date || '')
      const db = String(b?.date || '')
      return da.localeCompare(db)
    })
    trendRaw.value = sortedRaw
    refreshTrendChart()
  }

  /**
   * @description 快捷切换趋势日期
   */
  const handleTrendQuickChange = () => {
    const end = dayjs().subtract(1, 'day')
    if (trendQuick.value === 'yesterday') {
      const d = end.format('YYYY-MM-DD')
      trendDateRange.value = [d, d]
      fetchTrend()
      return
    }
    const days = Number(trendQuick.value) || 7
    const start = end.subtract(days - 1, 'day')
    trendDateRange.value = [start.format('YYYY-MM-DD'), end.format('YYYY-MM-DD')]
    fetchTrend()
  }

  /**
   * @description 手动选择趋势日期
   */
  const handleTrendDateChange = () => {
    trendQuick.value = ''
    fetchTrend()
  }

  watch(trendMetric, () => {
    refreshTrendChart()
  })

  const resolveQueryTimeRange = (dateRange) => {
    const range = Array.isArray(dateRange) ? dateRange : []
    const start = range[0]
    const end = range[1]
    if (!start || !end) return {}
    return toStartEndTime([start, end])
  }

  const getFieldValue = (raw) => {
    if (!raw) return raw
    if (typeof raw === 'object') return raw.value
    return raw
  }

  const getFieldModified = (raw) => {
    if (!raw) return false
    if (typeof raw === 'object') return raw.modified === true
    return false
  }

  /**
   * @description 日维度统计（Curd list）
   */
  const dailyApi = {
    list: async (params) => {
      const { page, pageSize, anchorName, dateRange } = params || {}
      const timeRange = resolveQueryTimeRange(dateRange)
      const res = await apiModule.liveRoomPerformance.dailyStats({
        page: page || 1,
        limit: pageSize || 10,
        ...timeRange,
        anchorName: anchorName || undefined,
        liveRoomId: liveRoomId.value || undefined,
        sortField: 'statsDate',
        sortOrder: 'DESC'
      })

      const list = (res?.data?.list || []).map((row) => ({
        statsDate: row?.statsDate,
        scheduleCount: row?.scheduleCount,
        liveDurationMinutes: row?.liveDurationMinutes,
        liveDurationMinutesStr:
          row?.liveDurationMinutes === 0
            ? '0分钟'
            : row?.liveDurationMinutes
              ? formatMinutes(row?.liveDurationMinutes)
              : '-',
        staffList: (row?.staffList || []).map((x) => ({
          positionName: x?.positionName,
          employeeName: x?.employeeName,
          scheduleCount: x?.scheduleCount
        })),
        viewCount: getFieldValue(row?.performanceData?.viewCount ?? row?.viewCount),
        salesRevenue: getFieldValue(row?.performanceData?.salesRevenue ?? row?.salesRevenue),
        refund: getFieldValue(row?.performanceData?.refund ?? row?.refund),
        netSales: getFieldValue(row?.performanceData?.netSales ?? row?.netSales),
        investment: getFieldValue(row?.performanceData?.investment ?? row?.investment),
        roi: getFieldValue(row?.performanceData?.roi ?? row?.roi)
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
      const timeRange = resolveQueryTimeRange(dateRange)
      const res = await apiModule.liveRoomPerformance.pageQuerySchedule({
        page: page || 1,
        limit: pageSize || 10,
        ...timeRange,
        anchorName: anchorName || undefined,
        liveRoomId: liveRoomId.value || undefined
      })

      const list = (res?.data?.list || []).map((row) => ({
        ...(row?.performanceData ? row.performanceData : {}),
        id: row?.id,
        date: row?.startTime ? String(row.startTime).slice(0, 10) : '',
        startTime: row?.startTime,
        endTime: row?.endTime,
        durationStr: buildDurationStr(row?.startTime, row?.endTime),
        staffList: (row?.staffList || []).map((x) => ({
          positionName: x?.positionName,
          employeeName: x?.employeeName
        })),
        viewCount: getFieldValue(row?.performanceData?.viewCount ?? row?.viewCount),
        viewCountModified: getFieldModified(row?.performanceData?.viewCount ?? row?.viewCount),
        salesRevenue: getFieldValue(row?.performanceData?.salesRevenue ?? row?.salesRevenue),
        salesRevenueModified: getFieldModified(row?.performanceData?.salesRevenue ?? row?.salesRevenue),
        refund: getFieldValue(row?.performanceData?.refund ?? row?.refund),
        refundModified: getFieldModified(row?.performanceData?.refund ?? row?.refund),
        netSales: getFieldValue(row?.performanceData?.netSales ?? row?.netSales),
        netSalesModified: getFieldModified(row?.performanceData?.netSales ?? row?.netSales),
        investment: getFieldValue(row?.performanceData?.investment ?? row?.investment),
        investmentModified: getFieldModified(row?.performanceData?.investment ?? row?.investment),
        roi: getFieldValue(row?.performanceData?.roi ?? row?.roi),
        roiModified: getFieldModified(row?.performanceData?.roi ?? row?.roi),
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
    fetchPeriodStats()
    fetchTrend()
  }

  /**
   * @description 日维度表格自定义动作：查看场次明细
   * @param {Object} payload - { type, row }
   */
  const handleDailyAction = ({ type, row }) => {
    if (type !== 'viewDetail') return
    activeTab.value = 'session'
    sessionCurdRef.value.searchParams.dateRange = row?.statsDate ? [row.statsDate, row.statsDate] : []
    sessionCurdRef.value.getData()
  }

  const handleTabClick = (tab) => {
    if (tab?.value === 'performance') performanceCurdRef.value?.getData()
    if (tab?.value === 'session') sessionCurdRef.value?.getData()
  }

  const handleBackList = () => {
    router.push('/live-room-performance/index')
  }

  const handleGoHistory = () => {
    if (!liveRoomId.value) return
    router.push(`/live-room-performance/history/${liveRoomId.value}`)
  }

  watch(
    () => liveRoomId.value,
    (val) => {
      if (!val) return

      fetchLiveRoomPerformance()
      fetchPeriodStats()
      fetchTrend()
      if (activeTab.value === 'session') {
        sessionCurdRef.value?.getData()
        return
      }
      performanceCurdRef.value?.getData()
    },
    { immediate: true }
  )
  watch(() => route.query, (newQuery) => {
    if (newQuery.tagType) {
      activeTab.value = newQuery.tagType === '1' ? 'session' : 'performance'
    }
  }, { immediate: true })

  watchEffect(() => {
    const curdRef = sessionCurdRef.value
    if (!curdRef) return
    if (!Array.isArray(sessionDateRange.value) || sessionDateRange.value.length !== 2) return
    curdRef.searchParams.dateRange = sessionDateRange.value
    if (activeTab.value !== 'session') return
    curdRef.getData()
  })
</script>

<style scoped>
  .avtar-container {
    display: flex;
    align-items: center;
  }

  .header-info {
    position: relative;
    background: #fff;
    padding: 28px 20px;
    border-radius: 8px;
    display: flex;
    align-items: center;
    margin-bottom: 16px;
  }

  .metric-cell {
    color: inherit;
  }

  .metric-cell--modified {
    color: #00893b;
  }
  .platform-icon {
    position: absolute;
    left: 0;
    top: 0;
    width: 20px;
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

  .attached-divider {
    color: #dcdfe6;
    margin: 0 8px;
    font-size: 12px;
  }

  .performance-content {
    display: flex;
    flex-direction: column;
    gap: 20px;
  }

  .trendChart-data-class {
    background-color: #fff;
    padding: 20px;
    border-radius: var(--border-radius-base);
    .subtitle-container {
      display: flex;
      align-items: center;
      justify-content: space-between;
    }
  }

  .trend-tools {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  .trend-date {
    width: 260px;
  }

  .table-container {
    background-color: #fff;
    border-radius: var(--border-radius-base);
  }

  .time-range-cell {
    display: flex;
    flex-direction: column;
    align-items: center;
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
</style>
