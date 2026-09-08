<template>
  <div class="data-overview-tab">
    <!-- Top Cards -->

    <!-- Main Content Grid -->
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
                  <ExportDataButton :data="exportList" :columns="detailTableColumns" file-name="分公司详情_数据详情" />
                </div>
                <Curd
                  ref="curdRef"
                  :table-columns="detailTableColumns"
                  :api="detailApi"
                  :show-search="false"
                  :show-page="true"
                  :show-add="false"
                  :show-toolbar-right="false"
                  :show-operation="false"
                  :border="false"
                  :option-config="{ show: false }"
                  :action-config="{ view: false, edit: false, del: false }"
                  @load="handleLoad"
                />
              </div>
            </div>
          </div>
        </el-col>
        <el-col :span="5">
          <div class="right-column">
            <DataPieChart title="部门占比" :data="pieChartData" class="mb-20" />
            <DataRankingList title="TOP部门" :list="topDepartments" icon-type="chart" class="ranking-card" />
            <DataRankingList title="TOP小组" :list="topGroups" icon-type="chat" class="ranking-card mt-20" />
            <DataRankingList title="TOP直播间" :list="topLiveRooms" icon-type="video" class="ranking-card mt-20" />
          </div>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script setup>
  /**
   * @file index.vue
   * @description 分公司业绩详情-数据概览模块
   */
  import { ref, reactive, watch, onMounted } from 'vue'
  import dayjs from 'dayjs'
  import { useRouter } from 'vue-router'
  import DataTrendChart from '@/components/DataTrendChart/index.vue'
  import DataRankingList from '@/components/DataRankingList/index.vue'
  import Curd from '@/components/Curd/index.vue'
  import DataPieChart from '@/components/DataPieChart/index.vue'
  import ExportDataButton from '@/components/ExportDataButton/index.vue'
  import apiModule from '@/http/api'

  const router = useRouter()
  const props = defineProps({
    sourceId: { type: Number, required: true },
    sourceType: { type: String, required: true },
    dateRange: { type: Array, default: () => [] }
  })

  const curdRef = ref(null)
  const firstLoaded = ref(false)
  const exportList = ref([])
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
    exportList.value = Array.isArray(list) ? list : []
    if (!firstLoaded.value) firstLoaded.value = true
    const isEmpty = Array.isArray(list) && list.length === 0
    emptyConfig.visible = firstLoaded.value && isEmpty
  }

  const trendType = ref('views')
  const trendChartData = reactive({ xData: [], yData: [], unit: '' })
  const trendRaw = ref([])

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

  const calcRoi = (salesRevenue, investment) => {
    const s = Number(salesRevenue)
    const i = Number(investment)
    if (Number.isNaN(s) || Number.isNaN(i) || i <= 0) return '-'
    return (s / i).toFixed(2)
  }

  const detailApi = {
    list: async (params) => {
      const [startDate, endDate] = getDateRange()
      const res = await apiModule.performanceSummary.dailyPage({
        sourceId: props.sourceId,
        sourceType: props.sourceType,
        startDate,
        endDate,
        page: params?.page || 1,
        limit: params?.pageSize || 10
      })
      const list = (res.data?.list || []).map((row) => ({ ...row, roi: calcRoi(row.salesRevenue, row.investment) }))
      return { list, total: res.data?.totalCount || 0 }
    }
  }

  const pieChartData = ref([])
  const topDepartments = ref([])
  const topGroups = ref([])
  const topLiveRooms = ref([])

  const formatW = (num) => {
    const n = Number(num)
    if (Number.isNaN(n)) return '-'
    if (n >= 10000) return (n / 10000).toFixed(1) + 'w'
    return String(n)
  }

  const refreshTrendChart = () => {
    const metricMap = {
      views: 'viewCount',
      sales: 'salesRevenue',
      refund: 'refund',
      netSales: 'netSales',
      adCost: 'investment'
    }
    const key = metricMap[trendType.value] || 'viewCount'
    trendChartData.xData = trendRaw.value.map((i) => i.date)
    trendChartData.yData = trendRaw.value.map((i) => Number(i[key] || 0))
    trendChartData.unit = trendType.value === 'views' ? '' : 'w'
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

  const getSalesRevenueSummary = async (dimensionType) => {
    const [startDate, endDate] = getDateRange()
    const res = await apiModule.performanceSummary.salesRevenueSummary({
      dimensionType,
      companyId: props.sourceType === 'subCompany' ? props.sourceId : undefined,
      startDate,
      endDate
    })
    return res.data || []
  }

  const refreshRightSide = async () => {
    const deptList = await getSalesRevenueSummary('dept')
    pieChartData.value = deptList.map((i) => ({ name: i.name, value: Number(i.salesRevenue || 0) }))
    topDepartments.value = deptList.slice(0, 8).map((i) => ({ id: i.id, name: i.name, value: formatW(i.salesRevenue) }))

    const teamList = await getSalesRevenueSummary('team')
    topGroups.value = teamList.slice(0, 8).map((i) => ({ id: i.id, name: i.name, value: formatW(i.salesRevenue) }))

    const roomList = await getSalesRevenueSummary('liveRoom')
    topLiveRooms.value = roomList.slice(0, 8).map((i) => ({ id: i.id, name: i.name, value: formatW(i.salesRevenue) }))
  }

  onMounted(async () => {
    await Promise.all([getTrend(), refreshRightSide()])
    refreshTrendChart()
  })

  watch(
    () => props.dateRange,
    async () => {
      await Promise.all([getTrend(), refreshRightSide()])
      refreshTrendChart()
      curdRef.value?.getData()
    },
    { deep: true }
  )

  watch(trendType, () => {
    refreshTrendChart()
  })
</script>

<style scoped lang="scss">
  .left-column {
    padding: 20px 20px 0 20px;
    border-radius: 10px;
    background-color: #fff;
  }

  .curd-container {
    padding: 0;
  }

  :deep(.table-row) {
    padding: 0;
  }

  .data-overview-tab {
    display: flex;
    flex-direction: column;
    gap: 20px;
  }

  .card-container {
    background-color: #fff;
    border-radius: 10px;
  }

  .card-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 20px;

    .title {
      font-size: 16px;
      font-weight: bold;
      color: #303133;
    }
  }

  .mb-20 {
    margin-bottom: 20px;
  }

  .mt-20 {
    margin-top: 20px;
  }

  .ranking-card {
    box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
  }
</style>
