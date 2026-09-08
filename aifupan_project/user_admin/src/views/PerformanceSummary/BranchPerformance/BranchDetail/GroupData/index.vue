<template>
  <div class="group-data-tab">
    <!-- Table -->
    <div class="table-container" v-empty="emptyConfig">
      <Curd
        ref="curdRef"
        :table-columns="tableColumns"
        :api="api"
        :search-config="searchConfig"
        :layout="customLayout"
        :show-add="false"
        :show-toolbar-right="false"
        :show-operation="false"
        :border="false"
        :action-config="{ view: false, edit: false, del: false }"
        @load="handleLoad"
      >
        <template #search>
          <RoundSearch v-model="searchKeyword" placeholder="请输入名称" @search="handleSearch" />
        </template>

        <template #date>
          <div class="header-filters">
            <ExportDataButton
              class="export-btn"
              :data="exportList"
              :columns="tableColumns"
              file-name="分公司详情_小组列表"
            />
          </div>
        </template>

        <template #rank="{ row }">
          <div class="rank-cell">
            <img v-if="row.rank === 1" src="@/assets/images/icon/top1.png" alt="1" class="rank-icon" />
            <img v-else-if="row.rank === 2" src="@/assets/images/icon/top2.png" alt="2" class="rank-icon" />
            <img v-else-if="row.rank === 3" src="@/assets/images/icon/top3.png" alt="3" class="rank-icon" />
            <span v-else class="rank-text">{{ row.rank }}</span>
          </div>
        </template>

        <template #group="{ row }">
          <div class="info-cell">
            <div class="name">{{ row.name }}</div>
            <div class="sub">-</div>
          </div>
        </template>

        <template #action="{ row }">
          <el-button type="primary" link @click="handleViewDetail(row)">查看详情</el-button>
        </template>
      </Curd>
    </div>
  </div>
</template>

<script setup>
  import { reactive, ref, watch } from 'vue'
  import dayjs from 'dayjs'
  import { useRouter } from 'vue-router'
  import Curd from '@/components/Curd/index.vue'
  import RoundSearch from '@/components/RoundSearch/index.vue'
  import ExportDataButton from '@/components/ExportDataButton/index.vue'
  import apiModule from '@/http/api'

  const props = defineProps({
    companyId: { type: Number, required: true },
    dateRange: { type: Array, default: () => [] }
  })

  const router = useRouter()
  const curdRef = ref(null)
  const searchKeyword = ref('')
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

  const customLayout = [[['search'], ['date']], ['table'], ['page']]

  const searchConfig = {
    formItems: [{ type: 'input', prop: 'name', placeholder: '请输入名称', width: '240px' }]
  }

  const tableColumns = [
    { label: '排行', prop: 'rank', width: 80, slotName: 'rank', align: 'center' },
    { label: '小组', prop: 'name', minWidth: 200, slotName: 'group' },
    { label: '场观', prop: 'viewCount', sortable: true },
    { label: '销售额', prop: 'salesRevenue', sortable: true },
    { label: '退款', prop: 'refund', sortable: true },
    { label: '净销售额', prop: 'netSales', sortable: true },
    { label: '投放', prop: 'investment', sortable: true },
    { label: 'ROI', prop: 'roi', sortable: true },
    {
      label: '操作',
      prop: 'action',
      width: 100,
      fixed: 'right',
      slotName: 'action',
      align: 'center'
    }
  ]

  const getDateRange = () => {
    if (props.dateRange && props.dateRange.length === 2) return props.dateRange
    return [dayjs().subtract(30, 'day').format('YYYY-MM-DD'), dayjs().subtract(1, 'day').format('YYYY-MM-DD')]
  }

  const api = {
    list: async (params) => {
      const { page, pageSize, name } = params || {}
      const [startDate, endDate] = getDateRange()
      const res = await apiModule.performanceSummary.teamPage({
        name,
        companyId: props.companyId,
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

  const handleSearch = () => {
    if (!curdRef.value) return
    curdRef.value.searchParams.name = searchKeyword.value
    curdRef.value.getData()
  }

  const handleLoad = (list) => {
    exportList.value = Array.isArray(list) ? list : []
    if (!firstLoaded.value) firstLoaded.value = true
    const isEmpty = Array.isArray(list) && list.length === 0
    emptyConfig.visible = firstLoaded.value && isEmpty && !searchKeyword.value
  }

  const handleViewDetail = (row) => {
    router.push({
      name: 'TeamDetail',
      params: { id: row.id },
      query: { name: row.name }
    })
  }

  watch(
    () => props.dateRange,
    () => {
      curdRef.value?.getData()
    },
    { deep: true }
  )
</script>

<style scoped>
  .rank-cell {
    display: flex;
    justify-content: center;
    align-items: center;
  }
  .mt-20 {
    margin-top: 20px;
  }
  .header-filters {
    display: flex;
    align-items: center;
  }
  .rank-icon {
    width: 24px;
    height: 24px;
  }
  .rank-text {
    font-weight: bold;
    color: #606266;
  }
  .info-cell .name {
    font-weight: 500;
  }
  .info-cell .sub {
    font-size: 12px;
    color: #909399;
  }
  .table-container {
    background: #fff;
    border-radius: 10px;
  }
</style>
