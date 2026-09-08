<template>
  <div class="page-container">
    <!-- Header Info -->
    <div class="header-info">
      <div class="header-left">
        <el-icon class="company-icon" :size="64"><OfficeBuilding /></el-icon>
        <div class="info-content">
          <div class="name">{{ headerInfo.sourceName }}</div>
        </div>
      </div>
      <div class="header-right">
        <div class="info-item">
          <div class="label">所属组织</div>
          <div class="value">{{ headerInfo.organization }}</div>
        </div>
        <div class="info-item">
          <div class="label">小组</div>
          <div class="value">{{ headerInfo.teamCount }}个</div>
        </div>
        <div class="info-item">
          <div class="label">直播间</div>
          <div class="value">{{ headerInfo.liveRoomCount }}个</div>
        </div>
      </div>
    </div>
    <div class="card-container">
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
    </div>
    <!-- Tabs -->
    <div class="tab-container">
      <PageTabs v-model="activeTab" :tabs="tabs">
        <template #attached-content>
          <div class="group-compass-page__actions">
            <DateQuickPicker v-model="dateRange" />
          </div>
        </template>
        <template #overview>
          <DataOverviewTab :source-id="deptId" source-type="dept" :date-range="dateRange" />
        </template>
        <template #group>
          <GroupDataTab :dept-id="deptId" :date-range="dateRange" />
        </template>
        <template #liveRoom>
          <LiveRoomDataTab :dept-id="deptId" :date-range="dateRange" @view-live-room="handleViewLiveRoom" />
        </template>
      </PageTabs>
    </div>
  </div>
</template>

<script setup>
  import { ref, computed, onMounted, watch } from 'vue'
  import dayjs from 'dayjs'
  import { useRoute, useRouter } from 'vue-router'
  import { Coin, Money, OfficeBuilding, Promotion, VideoCamera, View, Wallet } from '@element-plus/icons-vue'
  import PageTabs from '@/components/PageTabs/index.vue'
  import DataOverviewTab from './DataOverview/index.vue'
  import GroupDataTab from './GroupData/index.vue'
  import LiveRoomDataTab from './LiveRoomData/index.vue'
  import DataCardGroup from '@/components/DataCardGroup/index.vue'
  import DateQuickPicker from '@/components/DateQuickPicker/index.vue'
  import apiModule from '@/http/api'

  const route = useRoute()
  const router = useRouter()
  const deptId = computed(() => (route.params.id ? String(route.params.id) : ''))
  const dateRange = ref([
    dayjs().subtract(30, 'day').format('YYYY-MM-DD'),
    dayjs().subtract(1, 'day').format('YYYY-MM-DD')
  ])
  const activeTab = ref('overview')

  const normalizeTab = (val) => {
    const v = String(val || '').trim()
    if (v === 'overview' || v === 'group' || v === 'liveRoom') return v
    return 'overview'
  }

  const headerInfo = ref({
    sourceName: '',
    organization: '',
    teamCount: 0,
    liveRoomCount: 0
  })

  const performanceCards = ref([])

  const applyPeriodStats = (stats) => {
    const sessionStats = stats?.sessionStats || {}
    const viewCount = stats?.viewCount || {}
    const salesRevenue = stats?.salesRevenue || {}
    const refund = stats?.refund || {}
    const netSales = stats?.netSales || {}
    const investment = stats?.investment || {}

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

    const moneyConfig = [
      { key: 'today', label: '今日', unit: 'w' },
      { key: 'yesterday', label: '昨日', unit: 'w' },
      { key: 'thisWeek', label: '本周', unit: 'w' },
      { key: 'lastWeek', label: '上周', unit: 'w' },
      { key: 'thisMonth', label: '本月', unit: 'w' },
      { key: 'lastMonth', label: '上月', unit: 'w' }
    ]

    performanceCards.value = [
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
      { title: '销售额', data: salesRevenue, config: moneyConfig },
      { title: '退款', data: refund, config: moneyConfig },
      { title: '净销售额', data: netSales, config: moneyConfig },
      { title: '投放', data: investment, config: moneyConfig }
    ]
  }

  const getHeaderData = async () => {
    const [orgRes, statsRes] = await Promise.all([
      apiModule.performanceSummary.orgCount({ sourceId: deptId.value, sourceType: 'dept' }),
      apiModule.performanceSummary.periodStats({ sourceId: deptId.value, sourceType: 'dept' })
    ])
    headerInfo.value = {
      ...headerInfo.value,
      sourceName: orgRes.data?.sourceName || '',
      organization: orgRes.data.deptName
        ? `${orgRes.data.companyName}-${orgRes.data.deptName}`
        : orgRes.data.companyName,
      teamCount: orgRes.data?.teamCount || 0,
      liveRoomCount: orgRes.data?.liveRoomCount || 0
    }
    applyPeriodStats(statsRes.data)
  }

  const handleViewLiveRoom = (row) => {
    router.push({
      name: 'LiveRoomPerformanceDetailPage',
      params: { liveRoomId: row.id },
      query: { name: row.name }
    })
  }
  const tabs = [
    { label: '数据概览', value: 'overview' },
    { label: '小组数据', value: 'group' },
    { label: '直播间数据', value: 'liveRoom' }
  ]
  onMounted(() => {
    getHeaderData()
  })

  watch(
    () => route.query?.tab,
    (v) => {
      activeTab.value = normalizeTab(v)
    },
    { immediate: true, flush: 'post' }
  )

  watch(
    deptId,
    () => {
      activeTab.value = 'overview'
      getHeaderData()
    },
    { flush: 'post' }
  )
</script>

<style scoped lang="scss">
  .header-info {
    display: flex;
    align-items: center;
    padding: 28px 20px;
    margin-bottom: 20px;
    background: #fff;
    border-radius: 8px;

    .header-left {
      display: flex;
      align-items: center;
      padding-right: 60px;
      border-right: 1px solid #dcdcdc;

      .company-icon {
        width: 48px;
        height: 48px;
        padding: 8px;
        color: #409eff;
        background: #ecf5ff;
        border-radius: 8px;
      }

      .info-content {
        margin-left: 16px;

        .name {
          font-size: 18px;
          font-weight: 500;
          color: #303133;
        }
      }
    }

    .header-right {
      display: flex;
      gap: 40px;
      padding-left: 60px;
      margin-right: 40px;

      .info-item {
        text-align: left;

        .label {
          margin-bottom: 4px;
          font-size: 14px;
          color: #303133;
        }

        .value {
          font-size: 14px;
          font-weight: 500;
          color: #484a4c;
        }
      }
    }
  }

  .card-container {
    margin-bottom: 16px;
  }
</style>
