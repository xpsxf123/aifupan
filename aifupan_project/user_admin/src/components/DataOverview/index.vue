<template>
  <div class="data-overview-container">
    <DataTrendChart :chart-data="chartData" :unit="unit" @tab-change="handleChartTabChange" />

    <div class="detail-table-card">
      <div class="card-header">
        <div class="title">数据详情</div>
        <div class="header-right">
          <DateQuickPicker
            v-if="showDatePicker"
            v-model="dateRange"
            @change="handleDateChange"
            style="margin-right: 12px"
          />
          <ExportDataButton
            v-if="showExport"
            :data="exportList"
            :columns="tableColumns"
            file-name="数据概览_数据详情"
            @export="handleExport"
          />
        </div>
      </div>

      <Curd
        ref="curdRef"
        :api="api"
        :table-columns="tableColumns"
        :show-action="false"
        :show-add="false"
        :show-search="false"
        :show-tools="false"
        :page-size="10"
        :extra-params="queryParams"
        @load="handleLoad"
      />
    </div>
  </div>
</template>

<script setup>
  /**
   * @file components/DataOverview/index.vue
   */
  import { ref, computed, defineProps, defineEmits } from 'vue'
  import DataTrendChart from '@/components/DataTrendChart/index.vue'
  import Curd from '@/components/Curd/index.vue'
  import DateQuickPicker from '@/components/DateQuickPicker/index.vue'
  import ExportDataButton from '@/components/ExportDataButton/index.vue'

  const emit = defineEmits(['tab-change', 'export', 'date-change'])

  const props = defineProps({
    /**
     * @description 列表接口（Curd 约定：list）
     */
    api: {
      type: Object,
      required: true
    },
    /**
     * @description 表格列配置
     */
    tableColumns: {
      type: Array,
      default: () => []
    },
    /**
     * @description 趋势图数据
     */
    chartData: {
      type: Object,
      default: () => ({ xData: [], yData: [] })
    },
    /**
     * @description 趋势图单位
     */
    unit: {
      type: String,
      default: 'w'
    },
    // 外部传入的额外查询参数
    extraParams: {
      type: Object,
      default: () => ({})
    },
    /**
     * @description 是否显示导出按钮
     */
    showExport: {
      type: Boolean,
      default: true
    },
    /**
     * @description 是否显示日期筛选
     */
    showDatePicker: {
      type: Boolean,
      default: true
    }
  })

  // --- 日期选择相关 ---
  const dateRange = ref([])
  const exportList = ref([])

  // --- 列表查询参数 ---
  const queryParams = computed(() => {
    const params = { ...props.extraParams }
    if (dateRange.value && dateRange.value.length === 2) {
      params.startDate = dateRange.value[0]
      params.endDate = dateRange.value[1]
    }
    return params
  })

  const curdRef = ref(null)

  const handleDateChange = (val) => {
    emit('date-change', val)
    curdRef.value?.refresh()
  }

  const handleChartTabChange = (val) => {
    emit('tab-change', val)
  }

  const handleLoad = (list) => {
    exportList.value = Array.isArray(list) ? list : []
  }

  const handleExport = (payload) => {
    emit('export', { ...queryParams.value, ...(payload || {}) })
  }
</script>

<style scoped lang="scss">
  .data-overview-container {
    display: flex;
    flex-direction: column;
    gap: 20px;
  }

  .detail-table-card {
    background: #fff;
    border-radius: 8px;
    padding: 20px;
  }

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;

    .title {
      font-size: 16px;
      font-weight: 600;
      color: #333;
    }

    .header-right {
      display: flex;
      align-items: center;
    }
  }
</style>
