<template>
  <div class="session-content">
    <Curd
      ref="sessionCurdRef"
      :table-columns="sessionTableColumns"
      :api="scheduleApi"
      :search-config="{ items: sessionSearchConfig, showReset: false }"
      title="数据详情"
      :layout="dataViewLayout"
      :show-add="false"
      :show-toolbar-right="false"
      list-permission-code="room:performance:list"
      :action-config="{ view: false, edit: true, del: true }"
      :editAction="openEditSchedule"
      :operationWidth="130"
      @load="handleSessionLoad"
    >
      <template #optionBefore>
        <el-button v-auth="'room:performance:add'" round plain type="primary" @click="openAddSchedule"
          >录入数据
        </el-button>
        <ExportDataButton :data="sessionExportList" :columns="sessionTableColumns" file-name="人员业绩_按班次明细" />
      </template>

      <!-- 直播时间段自定义渲染 -->
      <template #timeRange="{ row }">
        <div class="time-range-cell">
          <div class="range">{{ formatTimeRange(row.startTime, row.endTime) }}</div>
          <div class="duration">{{ row.durationStr }}</div>
        </div>
      </template>
    </Curd>

    <SchedulePerformanceDialog
      v-model="scheduleDialogVisible"
      :live-room-id="activeLiveRoomId"
      :live-room-name="activeLiveRoomName"
      :sec-uid="activeLiveRoomSecUid"
      :platform-type="activeLiveRoomPlatformType"
      :performance-id="editingPerformanceId"
      @success="handleScheduleSaved"
    />
  </div>
</template>

<script setup>
  /**
   * @file LiveSessionTab.vue
   * @description 直播场次 Tab 组件（人员业绩统计：按班次维度）
   */
  import { defineExpose, ref, watchEffect } from 'vue'
  import dayjs from 'dayjs'
  import { ElMessage } from 'element-plus'
  import Curd from '@/components/Curd/index.vue'
  import ExportDataButton from '@/components/ExportDataButton/index.vue'
  import SchedulePerformanceDialog from '@/components/SchedulePerformanceDialog/index.vue'
  import apiModule from '@/http/api'
  import { sessionSearchConfig, sessionTableColumns } from './constants'
  import { dataViewLayout } from '@/config/curdConfig'

  const props = defineProps({
    employeeId: { type: [Number, String], required: true },
    dateRange: { type: Array, default: () => [] }
  })

  const sessionCurdRef = ref(null)
  const sessionExportList = ref([])

  const scheduleDialogVisible = ref(false)
  const activeLiveRoomId = ref('')
  const activeLiveRoomName = ref('')
  const activeLiveRoomSecUid = ref('')
  const activeLiveRoomPlatformType = ref('')
  const editingPerformanceId = ref('')

  // 记录最后一条列表数据的直播间，用于“录入数据”时的默认直播间（也可根据需求调整）
  const lastLiveRoomInfo = ref({ id: '', name: '', secUid: '', platformType: '' })

  const handleSessionLoad = (list) => {
    const data = Array.isArray(list) ? list : []
    sessionExportList.value = data
    if (data.length > 0) {
      const first = data[0]
      lastLiveRoomInfo.value = {
        id: first.liveRoomId || first.roomId || '',
        name: first.liveRoomName || first.anchorName || '',
        secUid: first.secUid || '',
        platformType: first.platformType ?? first.platform ?? first.source ?? ''
      }
    }
  }

  /**
   * @description 打开录入弹窗
   */
  const openAddSchedule = () => {
    const id = lastLiveRoomInfo.value.id
    if (!id) {
      ElMessage.warning('暂无直播间信息，无法录入场次业绩')
      return
    }
    activeLiveRoomId.value = id
    activeLiveRoomName.value = lastLiveRoomInfo.value.name || ''
    activeLiveRoomSecUid.value = lastLiveRoomInfo.value.secUid || ''
    activeLiveRoomPlatformType.value = lastLiveRoomInfo.value.platformType ?? ''
    editingPerformanceId.value = ''
    scheduleDialogVisible.value = true
  }

  /**
   * @description 打开编辑弹窗
   */
  const openEditSchedule = (row) => {
    const id = row?.id
    const liveRoomId = row?.liveRoomId || row?.roomId || ''
    if (!id || !liveRoomId) {
      ElMessage.warning('缺少必要信息，无法编辑场次业绩')
      return
    }
    activeLiveRoomId.value = liveRoomId
    activeLiveRoomName.value = row?.liveRoomName || row?.anchorName || ''
    activeLiveRoomSecUid.value = row?.secUid || ''
    activeLiveRoomPlatformType.value = row?.platformType ?? row?.platform ?? row?.source ?? ''
    editingPerformanceId.value = id
    scheduleDialogVisible.value = true
  }

  /**
   * @description 弹窗保存成功回调
   */
  const handleScheduleSaved = () => {
    sessionCurdRef.value?.getData()
  }

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
   * @description 格式化时间段展示
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
   * @description 班次明细（Curd list 约定：返回 { list, total }）
   */
  const scheduleApi = {
    list: async (params) => {
      const { page, pageSize, liveRoomName, dateRange } = params || {}
      const [startDate, endDate] = dateRange || []
      const res = await apiModule.employeePerformance.schedulePage({
        page: page || 1,
        limit: pageSize || 10,
        employeeId: props.employeeId || undefined,
        liveRoomName: liveRoomName || undefined,
        startDate: startDate || undefined,
        endDate: endDate || undefined,
        sortField: 'startTime',
        sortOrder: 'DESC'
      })
      const resolveMetricValue = (metric) => {
        if (metric && typeof metric === 'object') {
          if (metric.value === 0) return 0
          return metric.value ?? ''
        }
        if (metric === 0) return 0
        return metric ?? ''
      }
      const list = (res?.data?.list || []).map((row) => ({
        ...row,
        viewCount: resolveMetricValue(row?.performanceData?.viewCount),
        salesRevenue: resolveMetricValue(row?.performanceData?.salesRevenue),
        refund: resolveMetricValue(row?.performanceData?.refund),
        netSales: resolveMetricValue(row?.performanceData?.netSales),
        investment: resolveMetricValue(row?.performanceData?.investment),
        roi: resolveMetricValue(row?.performanceData?.roi),
        date: row?.startTime ? String(row.startTime).slice(0, 10) : '',
        durationStr: buildDurationStr(row?.startTime, row?.endTime),
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

  /**
   * @description 设置日期筛选并刷新列表
   * @param {Array} dateRange - ['YYYY-MM-DD','YYYY-MM-DD']
   */
  const setDateRange = (dateRange) => {
    if (!sessionCurdRef.value) return
    sessionCurdRef.value.searchParams.dateRange = dateRange
    sessionCurdRef.value.getData()
  }

  watchEffect(() => {
    const curdRef = sessionCurdRef.value
    void curdRef
    if (!Array.isArray(props.dateRange) || props.dateRange.length !== 2) return
    const [startDate, endDate] = props.dateRange
    if (!startDate || !endDate) return
    setDateRange(props.dateRange)
  })

  defineExpose({ setDateRange })
</script>

<style scoped>
  /* Session Content */
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
</style>
