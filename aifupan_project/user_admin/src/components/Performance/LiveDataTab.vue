<template>
  <div class="performance-content">
    <!-- 1. 数据概览 -->
    <DataCardGroup :cards="cards">
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
    <!-- 2. 数据趋势与漏斗图 -->
    <el-row class="charts-section" :gutter="20">
      <el-col :xs="24" :sm="24" :md="10" :lg="8" :xl="7">
        <!-- 左侧转化漏斗 -->
        <div class="funnel-chart-container card-box">
          <FunnelChart :data="funnelData" />
        </div>
      </el-col>

      <el-col :xs="24" :sm="24" :md="14" :lg="16" :xl="17">
        <!-- 右侧数据趋势 -->
        <div class="trend-chart-container card-box">
          <DataTrendChart :chart-data="trendChartData">
            <template #describe>
              <div class="describe-container">
                <el-radio-group v-model="trendMetric" @change="fetchTrend">
                  <el-radio-button label="viewCount">场观</el-radio-button>
                  <el-radio-button label="salesRevenue">销售额</el-radio-button>
                  <el-radio-button label="refund">退款</el-radio-button>
                  <el-radio-button label="netSales">净销售额</el-radio-button>
                  <el-radio-button label="investment">投放</el-radio-button>
                </el-radio-group>
                <div class="trend-filter">
                  <DateQuickPicker
                    v-model="trendDateRange"
                    :quickOptions="quickOptions"
                    format="YYYY/MM/DD"
                    @change="fetchTrend"
                  ></DateQuickPicker>
                </div>
              </div>
            </template>
          </DataTrendChart>
        </div>
      </el-col>
    </el-row>
    <!-- 3. 数据详情 -->
    <Curd
      ref="performanceCurdRef"
      :table-columns="performanceTableColumns"
      :api="dailyApi"
      title="数据详情"
      :layout="dataViewLayout"
      :show-add="false"
      :show-toolbar-right="false"
      :operationWidth="130"
      :action-config="{ view: false, edit: false, del: false }"
      :search-config="{ items: sessionSearchConfig, showReset: false }"
      :custom-actions="customActions"
      @action="handlePerformanceAction"
      @load="handlePerformanceLoad"
    >
      <template #optionBefore>
        <ExportDataButton
          :data="performanceExportList"
          :columns="performanceTableColumns"
          file-name="人员业绩_按天明细"
        />
      </template>
      <template #liveRooms="{ row }">
        <el-tooltip :content="formatLiveRooms(row.liveRoomList)" placement="top" :show-after="500">
          <div class="live-room-cell">{{ formatLiveRooms(row.liveRoomList) }}</div>
        </el-tooltip>
      </template>
    </Curd>
  </div>
</template>

<script setup>
  /**
   * @file LiveDataTab.vue
   * @description 直播数据 Tab 组件（人员业绩统计：概览/趋势/按天明细）
   */
  import { computed, ref, watch } from 'vue'
  import dayjs from 'dayjs'
  import Curd from '@/components/Curd/index.vue'
  import DataCardGroup from '@/components/DataCardGroup/index.vue'
  import DataTrendChart from '@/components/DataTrendChart/index.vue'
  import FunnelChart from '@/components/FunnelChart/index.vue'
  import DateQuickPicker from '@/components/DateQuickPicker/index.vue'
  import ExportDataButton from '@/components/ExportDataButton/index.vue'
  import { VideoCamera, View, Money, Wallet, Coin, Promotion } from '@element-plus/icons-vue'
  import { dataViewLayout } from '@/config/curdConfig'
  import apiModule from '@/http/api'
  import { initialTrendChartData, performanceTableColumns, customActions, sessionSearchConfig } from './constants'

  const props = defineProps({
    employeeId: { type: [Number, String], required: true }
  })

  const emit = defineEmits(['switch-tab'])

  const periodStats = ref(null)
  const summaryStats = ref(null)
  const performanceExportList = ref([])

  const trendMetric = ref('viewCount')
  const trendDateRange = ref([
    dayjs().subtract(7, 'day').format('YYYY-MM-DD'),
    dayjs().subtract(1, 'day').format('YYYY-MM-DD')
  ])
  const trendChartData = ref({ ...initialTrendChartData })

  const quickOptions = [
    { label: '昨日', value: 1 },
    { label: '近7日', value: 7 },
    { label: '近30天', value: 30 }
  ]

  const funnelData = computed(() => {
    const totalViewCount = summaryStats.value?.totalViewCount
    const totalSalesRevenue = summaryStats.value?.totalSalesRevenue
    const wholeConversionRate = summaryStats.value?.wholeConversionRate
    return {
      label: '整体转化率',
      rate: [wholeConversionRate],
      unit: '%',
      datas: {
        top: {
          label: '总场观',
          data: totalViewCount || 0,
          unit: ''
        },
        bottom: {
          label: '总销售额',
          datas: [Number(totalSalesRevenue) || 0, Number(totalSalesRevenue) || 0],
          unit: ''
        }
      }
    }
  })

  /**
   * @description 分钟转时长字符串
   * @param {number} minutes - 分钟
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
   * @description 场次统计格式化
   * @param {Object} stats - SessionStats
   * @returns {string}
   */
  const formatSession = (stats) => {
    if (!stats) return '-'
    const count = Number(stats?.count) || 0
    const duration = formatMinutes(stats?.duration)
    return `${count} (${duration})`
  }

  const cards = computed(() => {
    const ps = periodStats.value || {}
    const sessionStats = ps?.sessionStats || {}
    return [
      {
        title: '直播场次',
        icon: 'VideoCamera',
        iconColor: '#409eff',
        iconBgColor: '#ecf5ff',
        data: {
          today: formatSession(sessionStats?.today),
          yesterday: formatSession(sessionStats?.yesterday),
          thisWeek: formatSession(sessionStats?.thisWeek),
          lastWeek: formatSession(sessionStats?.lastWeek),
          thisMonth: formatSession(sessionStats?.thisMonth),
          lastMonth: formatSession(sessionStats?.lastMonth)
        }
      },
      {
        title: '场观',
        icon: 'View',
        iconColor: '#e6a23c',
        iconBgColor: '#fdf6ec',
        data: ps?.viewCount || {},
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
        title: '销售额',
        icon: 'Money',
        iconColor: '#f56c6c',
        iconBgColor: '#fef0f0',
        data: ps?.salesRevenue || {},
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
        data: ps?.refund || {},
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
        data: ps?.netSales || {},
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
        data: ps?.investment || {},
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
   * @description 格式化直播间列表展示
   * @param {Array} liveRoomList - 直播间列表
   * @returns {string}
   */
  const formatLiveRooms = (liveRoomList) => {
    if (!Array.isArray(liveRoomList) || !liveRoomList.length) return '-'
    return (
      liveRoomList
        .map((x) => x?.liveRoomName || x?.anchorName)
        .filter(Boolean)
        .join('、') || '-'
    )
  }

  /**
   * @description 拉取六时段统计与汇总
   */
  const fetchStats = async () => {
    if (!props.employeeId) return
    const [periodRes, summaryRes] = await Promise.all([
      apiModule.employeePerformance.periodStats({ employeeId: String(props.employeeId || '') }),
      apiModule.employeePerformance.summary({ employeeId: String(props.employeeId || '') })
    ])
    periodStats.value = periodRes?.data || null
    summaryStats.value = summaryRes?.data || null
  }

  /**
   * @description 拉取趋势数据
   */
  const fetchTrend = async () => {
    if (!props.employeeId) return
    const [startDate, endDate] = trendDateRange.value || []
    if (!startDate || !endDate) return
    const res = await apiModule.employeePerformance.trend({
      employeeId: String(props.employeeId || ''),
      startDate,
      endDate
    })
    const list = res?.data || []
    trendChartData.value = {
      xData: list.map((x) => x?.date),
      yData: list.map((x) => Number(x?.[trendMetric.value]) || 0)
    }
  }

  /**
   * @description 日维度明细（Curd list 约定：返回 { list, total }）
   */
  const dailyApi = {
    list: async (params) => {
      const { page, pageSize, liveRoomName, dateRange } = params || {}
      const [startDate, endDate] = dateRange || []
      const res = await apiModule.employeePerformance.dailyPage({
        page: page || 1,
        limit: pageSize || 10,
        employeeId: String(props.employeeId || ''),
        liveRoomName: liveRoomName || undefined,
        startDate: startDate || undefined,
        endDate: endDate || undefined,
        sortField: 'statsDate',
        sortOrder: 'DESC'
      })
      const list = (res?.data?.list || []).map((row) => ({
        ...row,
        duration: formatMinutes(row?.liveDurationMinutes)
      }))
      return {
        list,
        total: res?.data?.totalCount || 0
      }
    }
  }

  const handlePerformanceLoad = (list) => {
    performanceExportList.value = Array.isArray(list) ? list : []
  }

  const handlePerformanceAction = ({ type, row }) => {
    if (type === 'viewDetail') {
      emit('switch-tab', { tab: 'session', dateRange: row?.statsDate ? [row.statsDate, row.statsDate] : undefined })
    }
  }

  watch(
    () => props.employeeId,
    () => {
      fetchStats()
      fetchTrend()
    },
    { immediate: true }
  )
</script>

<style lang="scss" scoped>
  /* Performance Content */
  .performance-content {
    display: flex;
    flex-direction: column;
    gap: 20px;
  }

  .charts-section {
    margin: 0 !important;
  }

  /* Funnel Chart Placeholder Styles */
  .funnel-chart-container {
    width: 100%;
    height: 100%;
  }

  .section-title {
    font-size: 16px;
    font-weight: 600;
    color: #303133;
    margin-bottom: 20px;
  }

  .funnel-placeholder {
    flex: 1;
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;
    padding: 20px 0;
    gap: 20px;
  }

  .funnel-step {
    width: 100%;
    height: 80px;
    background: #409eff;
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 0 30px;
    color: white;
    border-radius: 4px;
  }

  .step-1 {
    background-color: #5470c6;
    width: 90%;
  }

  .step-2 {
    background-color: #91cc75;
    width: 70%;
  }

  .funnel-arrow {
    height: 30px;
    border-left: 2px dashed #ccc;
    width: 2px;
    position: relative;
  }

  .funnel-arrow .rate {
    position: absolute;
    left: 10px;
    top: 50%;
    transform: translateY(-50%);
    color: #666;
    font-size: 12px;
    white-space: nowrap;
  }

  .trend-chart-container {
    flex: 1;
    min-width: 0;
    .describe-container {
      display: flex;
      justify-content: space-between;
    }
  }

  .trend-filter {
    ::v-deep(.el-date-editor) {
      max-width: 170px;
    }
  }

  .live-room-cell {
    width: 100%;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
</style>
