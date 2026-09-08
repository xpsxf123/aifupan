<template>
  <div class="data-overview-tab">
    <DataCardGroup :cards="performanceCards">
      <template #icon-0
        ><el-icon> <VideoCamera /> </el-icon
      ></template>
      <template #icon-1
        ><el-icon> <View /> </el-icon
      ></template>
      <template #icon-2
        ><el-icon> <Money /> </el-icon
      ></template>
      <template #icon-3
        ><el-icon> <Wallet /> </el-icon
      ></template>
      <template #icon-4
        ><el-icon> <Coin /> </el-icon
      ></template>
      <template #icon-5
        ><el-icon> <Promotion /> </el-icon
      ></template>
    </DataCardGroup>

    <div class="data-overview-tab__section">
      <div class="data-overview-tab__section-header">
        <div class="data-overview-tab__section-title">数据概览</div>
        <div class="data-overview-tab__section-actions">
          <slot name="header-actions" />
        </div>
      </div>

      <div class="main-grid">
        <el-row :gutter="20">
          <el-col :span="19">
            <div class="left-column">
              <div class="left-top card-container" v-empty="emptyConfig">
                <DataTrendChart :chart-data="trendChartData" class="mb-20">
                  <template #subtitle>
                    <el-radio-group v-model="trendType">
                      <el-radio-button label="views">场观</el-radio-button>
                      <el-radio-button label="sales">销售额</el-radio-button>
                      <el-radio-button label="refund">退款</el-radio-button>
                      <el-radio-button label="netSales">净销售额</el-radio-button>
                      <el-radio-button label="adCost">投放</el-radio-button>
                    </el-radio-group>
                  </template>
                </DataTrendChart>
                <div class="detail-table-card">
                  <div class="card-header">
                    <div class="title">数据详情</div>
                    <ExportDataButton
                      :data="detailExportList"
                      :columns="detailTableColumns"
                      file-name="集团罗盘_数据详情"
                    />
                  </div>
                  <Curd
                    ref="detailCurdRef"
                    :curdContainerStyle="{ padding: '0px' }"
                    :table-columns="detailTableColumns"
                    :api="detailApi"
                    :border="false"
                    :show-search="false"
                    :show-page="true"
                    :show-add="false"
                    :show-operation="false"
                    :show-toolbar-right="false"
                    :option-config="{ show: false }"
                    :action-config="{ view: false, edit: false, del: false }"
                    @load="handleLoad"
                  />
                </div>
              </div>
              <div class="left-bottom card-container">
                <div class="detail-table-card">
                  <div class="card-header">
                    <div class="title">各分公司</div>
                    <ExportDataButton
                      :data="branchExportList"
                      :columns="branchTableColumns"
                      file-name="集团罗盘_各分公司"
                    />
                  </div>
                  <Curd
                    ref="branchCurdRef"
                    :table-columns="branchTableColumns"
                    :curdContainerStyle="{ padding: '0px' }"
                    :api="branchApi"
                    :border="false"
                    :show-search="false"
                    :show-page="false"
                    :show-add="false"
                    :show-operation="false"
                    :show-toolbar-right="false"
                    :option-config="{ show: false }"
                    :action-config="{ view: false, edit: false, del: false }"
                    @load="handleBranchLoad"
                  >
                    <template #rank="{ row }">
                      <div class="rank-cell">
                        <img v-if="row.rank === 1" src="@/assets/images/icon/top1.png" alt="1" class="rank-icon" />
                        <img v-else-if="row.rank === 2" src="@/assets/images/icon/top2.png" alt="2" class="rank-icon" />
                        <img v-else-if="row.rank === 3" src="@/assets/images/icon/top3.png" alt="3" class="rank-icon" />
                        <span v-else class="rank-text">{{ row.rank }}</span>
                      </div>
                    </template>
                    <!--                    <template #branch="{ row }">
                      <div class="info-cell">
                        <div class="avtar-container" style="margin-right: 8px">
                          <el-avatar :size="24" :src="row.avatar" />
                        </div>
                        <div class="name">{{ row.name }}</div>
                      </div>
                    </template>-->
                    <template #action="{ row }">
                      <el-button type="primary" link @click="goBranchDetail(row)">查看详情</el-button>
                    </template>
                  </Curd>
                </div>
              </div>
            </div>
          </el-col>
          <el-col :span="5">
            <div class="right-column">
              <DataPieChart title="分公司占比" :data="pieChartData" class="mb-20" />
              <DataRankingList title="TOP部门" :list="topDepartments" icon-type="chart" class="ranking-card" />
              <DataRankingList title="TOP小组" :list="topGroups" icon-type="chat" class="ranking-card mt-20" />
              <DataRankingList title="TOP直播间" :list="topLiveRooms" icon-type="video" class="ranking-card mt-20" />
            </div>
          </el-col>
        </el-row>
      </div>
    </div>
  </div>
</template>

<script setup>
  /**
   * @file index.vue
   * @description 集团业绩罗盘-数据概览模块
   */
  import { ref, reactive, computed, watch, onMounted } from 'vue'
  import { useRouter } from 'vue-router'
  import dayjs from 'dayjs'
  import { VideoCamera, View, Money, Wallet, Coin, Promotion } from '@element-plus/icons-vue'
  import DataCardGroup from '@/components/DataCardGroup/index.vue'
  import DataTrendChart from '@/components/DataTrendChart/index.vue'
  import DataRankingList from '@/components/DataRankingList/index.vue'
  import DataPieChart from '@/components/DataPieChart/index.vue'
  import Curd from '@/components/Curd/index.vue'
  import ExportDataButton from '@/components/ExportDataButton/index.vue'
  import apiModule from '@/http/api'

  const props = defineProps({
    sourceId: {
      type: Number,
      required: true
    },
    sourceType: {
      type: String,
      required: true
    },
    dateRange: {
      type: Array,
      default: () => []
    }
  })

  const trendType = ref('views')
  const trendChartData = reactive({ xData: [], yData: [], unit: '' })
  const router = useRouter()
  const detailCurdRef = ref(null)
  const branchCurdRef = ref(null)
  const firstLoaded = ref(false)
  const detailExportList = ref([])
  const branchExportList = ref([])
  const emptyConfig = reactive({
    visible: false,
    title: '暂无业绩数据',
    description: '请先产生直播间场次并录入业绩数据后，再查看业绩汇总。',
    blur: 2,
    buttons: [
      {
        text: '去看直播间业绩',
        type: 'primary',
        round: true,
        click: () => router.push('/live-room-performance/index')
      },
      {
        text: '去直播间排班',
        type: 'primary',
        plain: true,
        round: true,
        click: () => router.push('/live-room-ranking/index')
      }
    ]
  })

  const handleLoad = (list) => {
    detailExportList.value = Array.isArray(list) ? list : []
    if (!firstLoaded.value) firstLoaded.value = true
    const isEmpty = Array.isArray(list) && list.length === 0
    emptyConfig.visible = firstLoaded.value && isEmpty
  }

  const handleBranchLoad = (list) => {
    branchExportList.value = Array.isArray(list) ? list : []
  }

  const detailTableColumns = [
    { label: '日期', prop: 'date', width: 120 },
    { label: '场观', prop: 'viewCount', sortable: true },
    { label: '销售额', prop: 'salesRevenue', sortable: true },
    { label: '退款', prop: 'refund', sortable: true },
    { label: '净销售额', prop: 'netSales', sortable: true },
    { label: '投放', prop: 'investment', sortable: true },
    { label: 'ROI', prop: 'roi', sortable: true }
  ]

  const getDateRange = () => {
    if (props.dateRange && props.dateRange.length === 2) return props.dateRange
    return [dayjs().subtract(30, 'day').format('YYYY-MM-DD'), dayjs().subtract(1, 'day').format('YYYY-MM-DD')]
  }

  const formatW = (num) => {
    const n = Number(num)
    if (Number.isNaN(n)) return '-'
    if (n >= 10000) return (n / 10000).toFixed(1) + 'w'
    return String(n)
  }

  const calcRoi = (salesRevenue, investment) => {
    const s = Number(salesRevenue)
    const i = Number(investment)
    if (Number.isNaN(s) || Number.isNaN(i) || i <= 0) return '-'
    return (s / i).toFixed(2)
  }

  const detailApi = {
    list: async (params) => {
      const [startDate, endDate] = getDateRange()
      const query = {
        sourceId: props.sourceId,
        sourceType: props.sourceType,
        startDate,
        endDate,
        page: params?.page || 1,
        limit: params?.pageSize || 10
      }
      const res = await apiModule.performanceSummary.dailyPage(query)
      const list = (res.data?.list || []).map((row) => ({
        ...row,
        roi: calcRoi(row.salesRevenue, row.investment)
      }))
      return { list, total: res.data?.totalCount || 0 }
    }
  }

  const branchTableColumns = [
    { label: '排行', prop: 'rank', width: 80, slotName: 'rank', align: 'center' },
    { label: '分公司', prop: 'name', minWidth: 200 },
    { label: '场观', prop: 'viewCount', sortable: true },
    { label: '销售额', prop: 'salesRevenue', sortable: true },
    { label: '退款', prop: 'refund', sortable: true },
    { label: '净销售额', prop: 'netSales', sortable: true },
    { label: '投放', prop: 'investment', sortable: true },
    { label: 'ROI', prop: 'roi', sortable: true },
    { label: '操作', width: 100, slotName: 'action', align: 'center' }
  ]

  const branchApi = {
    list: async () => {
      const [startDate, endDate] = getDateRange()
      const res = await apiModule.performanceSummary.subCompanyPage({
        startDate,
        endDate,
        page: 1,
        limit: 50
      })
      const list = (res.data?.list || []).map((row, index) => ({
        ...row,
        rank: index + 1,
        avatar: ''
      }))
      return { list, total: res.data?.totalCount || 0 }
    }
  }

  const pieChartData = ref([])
  const topDepartments = ref([])
  const topGroups = ref([])
  const topLiveRooms = ref([])

  const periodStats = ref(null)
  const trendRaw = ref([])

  const performanceCards = computed(() => {
    const stats = periodStats.value || {}

    const sessionStats = stats.sessionStats || {}
    const viewCount = stats.viewCount || {}
    const salesRevenue = stats.salesRevenue || {}
    const refund = stats.refund || {}
    const netSales = stats.netSales || {}
    const investment = stats.investment || {}

    const formatMinutes = (raw) => {
      const minutes = Math.max(0, Math.floor(Number(raw) || 0))
      const h = Math.floor(minutes / 60)
      const m = minutes % 60
      if (h <= 0) return `${m}分钟`
      if (m <= 0) return `${h}小时`
      return `${h}小时${m}分钟`
    }

    const formatSession = (key) => {
      const info = sessionStats?.[key] || {}
      const count = info?.count || 0
      const minutes = info?.duration || 0
      const durationText = formatMinutes(minutes)
      return `${count} (${durationText})`
    }

    return [
      {
        title: '直播场次',
        data: {
          today: formatSession('today'),
          yesterday: formatSession('yesterday'),
          thisWeek: formatSession('thisWeek'),
          lastWeek: formatSession('lastWeek'),
          thisMonth: formatSession('thisMonth'),
          lastMonth: formatSession('lastMonth')
        }
      },
      { title: '场观', data: viewCount },
      {
        title: '销售额',
        data: salesRevenue,
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
        data: investment,
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

  const getPeriodStats = async () => {
    const res = await apiModule.performanceSummary.periodStats({
      sourceId: props.sourceId,
      sourceType: props.sourceType
    })
    periodStats.value = res.data || null
  }

  const getTrend = async () => {
    const [startDate, endDate] = getDateRange()
    const res = await apiModule.performanceSummary.trend({
      sourceId: props.sourceId,
      sourceType: props.sourceType,
      startDate,
      endDate
    })
    trendRaw.value = res.data || []
  }

  const refreshTrendChart = () => {
    const type = trendType.value
    const metricMap = {
      views: 'viewCount',
      sales: 'salesRevenue',
      refund: 'refund',
      netSales: 'netSales',
      adCost: 'investment'
    }
    const key = metricMap[type] || 'viewCount'
    trendChartData.xData = trendRaw.value.map((i) => i.date)
    trendChartData.yData = trendRaw.value.map((i) => Number(i[key] || 0))
    trendChartData.unit = type === 'views' ? '' : 'w'
  }

  const getSalesRevenueSummary = async (dimensionType) => {
    const [startDate, endDate] = getDateRange()
    const res = await apiModule.performanceSummary.salesRevenueSummary({
      dimensionType,
      startDate,
      endDate
    })
    return res.data || []
  }

  const refreshRightSide = async () => {
    const list = await getSalesRevenueSummary('subCompany')
    pieChartData.value = list.map((i) => ({ name: i.name, value: Number(i.salesRevenue || 0) }))

    const deptList = await getSalesRevenueSummary('dept')
    topDepartments.value = deptList.slice(0, 8).map((i) => ({ id: i.id, name: i.name, value: formatW(i.salesRevenue) }))

    const teamList = await getSalesRevenueSummary('team')
    topGroups.value = teamList.slice(0, 8).map((i) => ({ id: i.id, name: i.name, value: formatW(i.salesRevenue) }))

    const roomList = await getSalesRevenueSummary('liveRoom')
    topLiveRooms.value = roomList.slice(0, 8).map((i) => ({ id: i.id, name: i.name, value: formatW(i.salesRevenue) }))
  }

  const goBranchDetail = (row) => {
    router.push({ name: 'BranchDetail', params: { id: row.id }, query: { name: row.name } })
  }

  onMounted(async () => {
    await Promise.all([getPeriodStats(), getTrend(), refreshRightSide()])
    refreshTrendChart()
  })

  watch(
    () => props.dateRange,
    async () => {
      await Promise.all([getTrend(), refreshRightSide()])
      refreshTrendChart()
      detailCurdRef.value?.getData()
      branchCurdRef.value?.getData()
    },
    { deep: true }
  )

  watch(trendType, () => {
    refreshTrendChart()
  })
</script>

<style scoped>
  .avtar-container {
    display: flex;
    align-items: center;
  }

  .rank-cell {
    display: flex;
    justify-content: center;
    align-items: center;
  }
  .data-overview-tab {
    display: flex;
    flex-direction: column;
    gap: 20px;
  }

  .data-overview-tab__section {
    border-radius: 8px;
  }

  .data-overview-tab__section-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 20px;
    margin-bottom: 16px;
    background-color: #fff;
    border-radius: 10px;
  }

  .data-overview-tab__section-title {
    font-size: 16px;
    font-weight: 500;
    color: #444dff;
  }

  .data-overview-tab__section-actions {
    display: flex;
    align-items: center;
  }

  .card-container {
    background-color: #fff;
    border-radius: 10px;
    padding: 20px 20px 0 20px;
  }

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
  }

  .card-header .title {
    font-size: 16px;
    font-weight: bold;
    color: #303133;
  }

  .mb-20 {
    margin-bottom: 20px;
  }

  .left-top {
    margin-bottom: 20px;
  }

  .mt-20 {
    margin-top: 20px;
  }

  .ranking-card {
    box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
  }

  .rank-icon {
    width: 24px;
    height: 24px;
  }

  .rank-text {
    font-weight: bold;
    color: #606266;
  }

  .info-cell {
    display: flex;
    align-items: center;
  }

  .info-cell .name {
    font-weight: 500;
  }

  :deep(.table-row) {
    padding: 0;
  }
</style>
