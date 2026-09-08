<template>
  <div class="branch-data-page" v-empty="emptyConfig">
    <Curd
      ref="curdRef"
      :table-columns="tableColumns"
      :api="api"
      :search-config="searchConfig"
      :layout="customLayout"
      :show-add="false"
      :show-toolbar-right="false"
      :action-config="{ view: true, edit: false, del: false }"
      :operationWidth="100"
      :border="false"
      viewText="查看详情"
      :viewAction="handleAction"
      @load="handleLoad"
    >
      <template #search>
        <RoundSearch v-model="searchKeyword" placeholder="请输入名称" @search="handleSearch" />
      </template>
      <!-- Custom Header Right for Date Filters -->
      <template #date>
        <div class="header-filters">
          <!-- <el-radio-group v-model="dateType" size="default" class="custom-radio-group">
            <el-radio-button label="yesterday">昨日</el-radio-button>
            <el-radio-button label="7days">近7天</el-radio-button>
            <el-radio-button label="30days">近30天</el-radio-button>
          </el-radio-group>
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            size="default"
            @change="handleDateRangeChange"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            class="custom-date-picker"
          /> -->
          <div class="group-compass-page__actions">
            <DateQuickPicker v-model="dateRange" @change="handleDateChange" />
          </div>
          <ExportDataButton class="export-btn" :data="exportList" :columns="tableColumns" file-name="分公司业绩列表" />
        </div>
      </template>

      <!-- Rank Column -->
      <template #rank="{ row }">
        <div class="rank-cell">
          <img v-if="row.rank === 1" src="@/assets/images/icon/top1.png" alt="1" class="rank-icon" />
          <img v-else-if="row.rank === 2" src="@/assets/images/icon/top2.png" alt="2" class="rank-icon" />
          <img v-else-if="row.rank === 3" src="@/assets/images/icon/top3.png" alt="3" class="rank-icon" />
          <span v-else class="rank-text">{{ row.rank }}</span>
        </div>
      </template>

      <!-- Branch Info Column -->
      <template #branch="{ row }">
        <BranchInfo :row="row" />
      </template>
    </Curd>
  </div>
</template>

<script setup>
  /**
   * @file index.vue
   * @description 各分公司业绩列表页
   */
  import { ref } from 'vue'
  import dayjs from 'dayjs'
  import { useRouter } from 'vue-router'
  import RoundSearch from '@/components/RoundSearch/index.vue'
  import Curd from '@/components/Curd/index.vue'
  import ExportDataButton from '@/components/ExportDataButton/index.vue'
  import BranchInfo from './components/BranchInfo.vue'
  import { tableColumns, searchConfig } from './constants'
  import DateQuickPicker from '@/components/DateQuickPicker/index.vue'
  import apiModule from '@/http/api'

  const router = useRouter()
  const curdRef = ref(null)
  const defaultDateRange = [
    dayjs().subtract(30, 'day').format('YYYY-MM-DD'),
    dayjs().subtract(1, 'day').format('YYYY-MM-DD')
  ]
  const dateRange = ref(defaultDateRange)
  const searchKeyword = ref('')
  const firstLoaded = ref(false)
  const exportList = ref([])
  const emptyConfig = ref({
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

  // Layout configuration: Search (left) and Option (right) in one row
  const customLayout = [[['search'], ['date', 'option']], ['table'], ['page']]

  const api = {
    list: async (params) => {
      const { page, pageSize, name } = params || {}
      const [startDate, endDate] = dateRange.value || []
      const res = await apiModule.performanceSummary.subCompanyPage({
        name,
        startDate,
        endDate,
        page: page || 1,
        limit: pageSize || 10
      })
      const list = (res.data?.list || []).map((row, index) => ({
        ...row,
        rank: (page ? (page - 1) * (pageSize || 10) : 0) + index + 1
      }))
      return { list, total: res.data?.totalCount || 0 }
    }
  }

  const handleViewDetail = (row) => {
    router.push({
      name: 'BranchDetail',
      params: { id: row.id },
      query: { name: row.name }
    })
  }

  const handleSearch = () => {
    if (!curdRef.value) return
    curdRef.value.searchParams.name = searchKeyword.value
    curdRef.value.getData()
  }

  const handleDateChange = () => {
    curdRef.value?.getData()
  }

  const isDefaultRange = () => {
    const v = dateRange.value || []
    return v?.[0] === defaultDateRange[0] && v?.[1] === defaultDateRange[1]
  }

  const handleLoad = (list) => {
    exportList.value = Array.isArray(list) ? list : []
    if (!firstLoaded.value) firstLoaded.value = true
    const isEmpty = Array.isArray(list) && list.length === 0
    emptyConfig.value.visible = firstLoaded.value && isEmpty && !searchKeyword.value && isDefaultRange()
  }

  const handleAction = (row) => {
    handleViewDetail(row)
  }
</script>

<style scoped>
  .rank-cell {
    display: flex;
    justify-content: center;
    align-items: center;
  }
  .branch-data-page {
    border-radius: var(--border-radius-base);
    box-sizing: border-box;
    background-color: #fff;
  }

  .header-filters {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 12px;
  }

  .custom-radio-group {
    margin-right: 0;
  }

  .custom-date-picker {
    width: 240px;
  }

  .rank-icon {
    width: 24px;
    height: 24px;
  }

  .rank-text {
    font-weight: bold;
    color: #606266;
  }

  /* Override Curd Search Form Layout */
  :deep(.curd-search-form) {
    padding: 0;
    margin-bottom: 0;
  }

  :deep(.el-form-item) {
    margin-bottom: 0;
    margin-right: 0;
  }
</style>
