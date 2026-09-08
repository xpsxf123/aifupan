<template>
  <div class="schedule-container">
    <!-- 时间轴头部 -->
    <div class="schedule-header mb-16">
      <div class="header-placeholder">
        <div v-if="showWeekNav" class="week-nav-col">
          <div class="week-nav-btn" :class="{ disabled: !canPrevWeek }" @click="canPrevWeek && handlePrevWeek()">
            <el-icon><ArrowUp /></el-icon>
          </div>
        </div>
      </div>
      <!-- 对应日期列 -->
      <div class="header-role-placeholder ml-16"></div>
      <!-- 对应岗位列 -->
      <div ref="timeAxisRef" :class="['time-axis', timeAxisClass]">
        <div v-for="t in timeAxisTicks" :key="t" class="time-label" :style="{ left: `${getTickLeft(t)}%` }">
          {{ formatTickLabel(t) }}
        </div>
      </div>
    </div>

    <!-- 排班内容主体 -->
    <div class="schedule-body">
      <ScheduleDay
        v-for="(day, index) in visibleScheduleData"
        :key="`${day.dateTime}_${index}`"
        :day-data="day"
        :preview-data="previewState"
        :is-copy-mode="copyState.active"
        :is-source-day="copyState.active && copyState.sourceDate === day.dateTime"
        :readonly="readonly"
        :add-mode="addMode"
        :block-style-config="blockStyleConfig"
        @update-data="handleUpdateData"
        @update-block-data="handleUpdateBlockData"
        @remove-block-data="handleRemoveBlockData"
        @cross-day-drag="(payload) => handleCrossDayDrag(getGlobalDayIndex(index), payload)"
        @drag-preview="handleDragPreview"
        @copy-day="handleCopyDay"
        @paste-day="handlePasteDay"
        @exit-copy="handleExitCopy"
        @fixed-add="handleFixedAdd"
        @edit-block="handleEditBlock"
        @view-detail="openDetailDialog"
      />

      <div v-if="showWeekNav" class="week-nav-row">
        <div class="week-nav-btn" :class="{ disabled: !canNextWeek }" @click="canNextWeek && handleNextWeek()">
          <el-icon><ArrowDown /></el-icon>
        </div>
      </div>
    </div>
  </div>

  <ScheduleShiftDrawer
    ref="shiftDrawerRef"
    v-model="editDialogVisible"
    :title="dialogTitle"
    :confirm-text="dialogSubmitText"
    size="860px"
    :mode="dialogContext?.mode || 'add'"
    :initial="shiftFormInitial"
    :duration-options="durationOptionList"
    :rest-options="restOptionList"
    :disable-pick-member="memberPickDisabled"
    @pick-member="handlePickMember"
    @confirm="submitDialog"
  />

  <ScheduleMemberDrawer
    v-model="memberDrawerVisible"
    size="920px"
    :selected-id="memberSelectedId"
    v-model:keyword="memberQuery.keyword"
    v-model:deptId="memberDeptIdModel"
    v-model:teamId="memberTeamIdModel"
    v-model:page="memberQuery.page"
    v-model:limit="memberQuery.limit"
    :dept-options="memberDeptOptions"
    :team-options="memberTeamOptions"
    :list="memberList"
    :total="memberTotal"
    :loading="memberLoading"
    @search="fetchMemberList(true)"
    @page-change="fetchMemberList()"
    @confirm="confirmMember"
    @cancel="memberDrawerVisible = false"
  />

  <el-dialog v-model="detailDialogVisible" title="排班详情" width="520px" class="schedule-detail-dialog common-dialog">
    <el-form label-width="90px">
      <el-form-item label="日期">
        <el-input :model-value="detailForm.dayDate" disabled />
      </el-form-item>
      <el-form-item label="岗位">
        <el-input :model-value="detailForm.roleName" disabled />
      </el-form-item>
      <el-form-item label="标题">
        <el-input :model-value="detailForm.name" disabled />
      </el-form-item>
      <el-form-item label="时间段">
        <el-input :model-value="detailForm.timeRange" disabled />
      </el-form-item>
      <el-form-item label="时长">
        <el-input :model-value="detailForm.durationText" disabled />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button type="primary" @click="detailDialogVisible = false">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
  /**
   * @file src/components/Schedule/index.vue
   * @description 排班表主组件 (支持跨天拖拽处理)
   */
  import { computed, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
  import dayjs from 'dayjs'
  import { ElMessage, ElMessageBox } from 'element-plus'
  import { ArrowDown, ArrowUp } from '@element-plus/icons-vue'
  import ScheduleDay from './components/ScheduleDay.vue'
  import ScheduleShiftDrawer from '@/components/ScheduleShiftDrawer/index.vue'
  import ScheduleMemberDrawer from '@/components/ScheduleMemberDrawer/index.vue'
  import { formatScheduleData } from './formatter'
  import apiModule from '@/http/api'
  import { normalizeKeyLabelOptions } from '@/utils/options'

  // 模拟数据 (User provided structure)
  const today = new Date()
  const todayBase = new Date(today.setHours(0, 0, 0, 0)).getTime()
  // 辅助：解析日期字符串获取基准时间戳
  const getBaseTime = (dateStr) => {
    // 假设 'MM/DD'，年份取当前年
    const [m, d] = dateStr.split('/')
    const date = new Date()
    date.setMonth(parseInt(m) - 1)
    date.setDate(parseInt(d))
    date.setHours(0, 0, 0, 0)
    return date.getTime()
  }
  // 动态生成日期字符串 MM/DD
  const props = defineProps({
    readonly: {
      type: Boolean,
      default: false
    },
    data: {
      type: Array,
      default: () => []
    },
    addMode: {
      type: String,
      default: 'fixed'
    },
    weekMode: {
      type: Boolean,
      default: true
    },
    timeAxisUnit: {
      type: Number,
      default: 0
    },
    durationOptions: {
      type: Array,
      default: () => []
    },
    restOptions: {
      type: Array,
      default: () => []
    },
    blockStyleConfig: {
      type: Object,
      default: () => ({})
    },
    roleColors: {
      type: Array,
      default: () => ['#409eff', '#67c23a', '#b37feb']
    },
    submitMode: {
      type: String,
      default: 'local'
    },
    liveRoomId: {
      type: [String, Number],
      default: ''
    },
    companyId: {
      type: [String, Number],
      default: ''
    },
    deptId: {
      type: [String, Number],
      default: ''
    },
    teamId: {
      type: [String, Number],
      default: ''
    },
    anchorPositionId: {
      type: [String, Number],
      default: ''
    },
    defaultStartTime: {
      type: String,
      default: '08:00'
    }
  })

  const emit = defineEmits(['saved', 'remove-block-data', 'open-config'])

  const timeAxisRef = ref(null)
  const timeAxisWidth = ref(0)

  const normalizeOptions = (raw, fallback) => {
    const list = (Array.isArray(raw) ? raw : []).map((x) => Number(x)).filter((x) => Number.isFinite(x) && x >= 0)
    const uniq = Array.from(new Set(list))
    uniq.sort((a, b) => a - b)
    return uniq.length ? uniq : fallback
  }

  const durationOptionList = computed(() => normalizeOptions(props.durationOptions, [60, 120, 180]))
  const restOptionList = computed(() => normalizeOptions(props.restOptions, [0, 15, 30, 60, 120]))

  const blockStyleConfig = computed(() => props.blockStyleConfig || {})
  const roleColors = computed(() => {
    const list = Array.isArray(props.roleColors) ? props.roleColors : []
    const cleaned = list.map((c) => String(c || '').trim()).filter(Boolean)
    return cleaned.length ? cleaned : ['#409eff', '#67c23a', '#b37feb']
  })

  const resolvedTimeAxisUnit = computed(() => {
    const u = Number(props.timeAxisUnit) || 0
    if ([30, 60, 120].includes(u)) return u
    return 0
  })

  /**
   * @description 计算时间轴刻度步长（根据当前宽度自适应）
   * @returns {number}
   */
  const timeAxisStep = computed(() => {
    if (resolvedTimeAxisUnit.value) return 2
    const w = Number(timeAxisWidth.value) || 0
    if (w <= 0) return 2

    const minPxPerTick = 48
    const candidates = [1, 2, 3, 4, 6, 8, 12]

    for (const step of candidates) {
      const tickCount = Math.ceil(24 / step)
      if (tickCount * minPxPerTick <= w) return step
    }
    return 12
  })

  /**
   * @description 生成时间轴刻度数组（0-23 点）
   * @returns {number[]}
   */
  const timeAxisTicks = computed(() => {
    if (resolvedTimeAxisUnit.value) {
      const u = resolvedTimeAxisUnit.value
      const ticks = []
      for (let m = 0; m < 24 * 60; m += u) ticks.push(m)
      return ticks
    }
    const step = Number(timeAxisStep.value) || 2
    const ticks = []
    for (let h = 0; h < 24; h += step) ticks.push(h)
    return ticks
  })

  const timeAxisClass = computed(() => {
    if (resolvedTimeAxisUnit.value) return `time-axis-unit-${resolvedTimeAxisUnit.value}`
    return `time-axis-step-${timeAxisStep.value}`
  })

  const getTickLeft = (t) => {
    if (resolvedTimeAxisUnit.value) return (Number(t) / (24 * 60)) * 100
    return (Number(t) / 24) * 100
  }

  const formatTickLabel = (t) => {
    if (!resolvedTimeAxisUnit.value) return `${String(t).padStart(2, '0')}:00`
    const m = Number(t) || 0
    const hh = Math.floor(m / 60)
    const mm = m % 60
    return `${String(hh).padStart(2, '0')}:${String(mm).padStart(2, '0')}`
  }

  const detailDialogVisible = ref(false)
  const detailPayload = ref(null)
  const detailSchedule = ref(null)

  const formatDurationText = (minutes) => {
    const m = Number(minutes) || 0
    const h = Math.floor(m / 60)
    const mm = m % 60
    if (h <= 0) return `${mm}分钟`
    if (mm <= 0) return `${h}小时`
    return `${h}小时${mm}分钟`
  }

  const normalizeDetailSchedule = (raw) => {
    const base = raw || {}
    const sessions = Array.isArray(base.sessions) ? base.sessions : []
    const firstSession = sessions[0] || {}
    return {
      ...base,
      startWork: base.startWork || firstSession.startWork,
      endWork: base.endWork || firstSession.endWork,
      scheduleDuration: base.scheduleDuration || firstSession.scheduleDuration,
      restDuration: base.restDuration || firstSession.restDuration,
      employees: Array.isArray(base.employees) && base.employees.length ? base.employees : firstSession.employees
    }
  }

  const detailForm = computed(() => {
    const payload = detailPayload.value || {}
    const block = payload?.block || {}
    const schedule = normalizeDetailSchedule(detailSchedule.value || {})
    const dayDate = String(schedule?.workDay || schedule?.date || payload?.dayDate || '')
    const roleName = String(schedule?.positionName || schedule?.roleName || payload?.roleName || '')
    const start = pickHhmm(schedule?.startWork, '')
    let end = pickHhmm(schedule?.endWork, '')
    const durationMin = Number(schedule?.scheduleDuration) || 0
    if (!end && dayDate && start && durationMin > 0) {
      end = dayjs(`${dayDate} ${start}`).add(durationMin, 'minute').format('HH:mm')
    }
    const timeRange = start && end ? `${start} - ${end}` : ''
    const { memberName } = pickFirstEmployee(schedule)
    const title = String(schedule?.name || schedule?.employeeName || memberName || block?.name || '')
    return {
      dayDate,
      roleName,
      name: title,
      timeRange,
      durationText: formatDurationText(durationMin || block?.timeCount)
    }
  })

  const openDetailDialog = async (payload) => {
    const nextPayload = payload || null
    const block = nextPayload?.block || {}
    const scheduleId = block?.scheduleId || block?.id
    if (!scheduleId) {
      ElMessage.error('缺少排班ID')
      return
    }
    detailPayload.value = nextPayload
    detailSchedule.value = null
    detailDialogVisible.value = true
    try {
      const res = await apiModule.roomSchedule.detail(
        { roomScheduleId: scheduleId },
        { showLoading: true, loadingText: '加载排班详情...' }
      )
      detailSchedule.value = res?.data || {}
    } catch (e) {
      detailDialogVisible.value = false
      detailPayload.value = null
      detailSchedule.value = null
    }
  }

  let timeAxisObserver = null
  onMounted(() => {
    if (!timeAxisRef.value) return
    const updateWidth = () => {
      const el = timeAxisRef.value
      timeAxisWidth.value = el?.clientWidth || 0
    }
    updateWidth()
    if (typeof ResizeObserver !== 'undefined') {
      timeAxisObserver = new ResizeObserver(() => updateWidth())
      timeAxisObserver.observe(timeAxisRef.value)
    }
  })

  onUnmounted(() => {
    if (timeAxisObserver) timeAxisObserver.disconnect()
    timeAxisObserver = null
  })
  const getFormatDate = (offset = 0) => {
    const d = new Date(todayBase + offset * 24 * 60 * 60 * 1000)
    return `${(d.getMonth() + 1).toString().padStart(2, '0')}/${d.getDate().toString().padStart(2, '0')}`
  }

  const mockData = [
    {
      dateTime: getFormatDate(0), // 今日
      label: '今日',
      data: [
        {
          name: '主播',
          type: 1,
          data: [
            {
              name: '主播1',
              id: 1,
              memberId: 101, // 有效人员
              timeCount: 120,
              startTime: todayBase,
              times: [todayBase, todayBase + 1000 * 60 * 120] // 00:00 - 02:00
            },
            {
              name: '待定人员',
              id: 2,
              memberId: null, // 无效人员 -> 灰色
              timeCount: 60,
              startTime: todayBase + 1000 * 60 * 180, // 03:00
              times: [todayBase + 1000 * 60 * 180, todayBase + 1000 * 60 * 240] // 03:00 - 04:00
            },
            {
              name: '未添加主播',
              id: 3,
              memberId: 102,
              isRecord: true, // 上播记录 -> 绿色
              timeCount: 120,
              startTime: todayBase + 1000 * 60 * 420, // 07:00
              times: [todayBase + 1000 * 60 * 420, todayBase + 1000 * 60 * 540]
            }
          ]
        }
      ]
    },
    {
      dateTime: getFormatDate(1), // 明日
      label: '明日',
      data: [{ name: '主播', type: 1, data: [] }]
    }
  ]

  const scheduleData = ref([])
  const weekStartIndex = ref(0)

  const showWeekNav = computed(() => props.weekMode)
  const canPrevWeek = computed(() => props.weekMode && weekStartIndex.value > 0)
  const canNextWeek = computed(() => props.weekMode && weekStartIndex.value + 7 < scheduleData.value.length)

  const visibleScheduleData = computed(() => {
    if (!props.weekMode) return scheduleData.value
    return scheduleData.value.slice(weekStartIndex.value, weekStartIndex.value + 7)
  })

  const getGlobalDayIndex = (localIndex) => {
    if (!props.weekMode) return localIndex
    return weekStartIndex.value + localIndex
  }

  const handlePrevWeek = () => {
    weekStartIndex.value = Math.max(0, weekStartIndex.value - 7)
  }

  const handleNextWeek = () => {
    weekStartIndex.value = Math.min(Math.max(0, scheduleData.value.length - 7), weekStartIndex.value + 7)
  }

  watch(
    () => props.data,
    (newVal) => {
      if (newVal && newVal.length) {
        scheduleData.value = formatScheduleData(newVal, { roleColors: roleColors.value })
      } else {
        scheduleData.value = formatScheduleData(mockData, { roleColors: roleColors.value })
      }

      const todayKey = getFormatDate(0)
      const idx = scheduleData.value.findIndex((d) => d.dateTime === todayKey)
      const base = scheduleData.value[idx >= 0 ? idx : 0]?.dateTime
      if (!base) {
        weekStartIndex.value = 0
        return
      }
      const baseTs = getBaseTime(base)
      const day = new Date(baseTs).getDay()
      const offsetToMonday = (day + 6) % 7
      const mondayTs = baseTs - offsetToMonday * 24 * 60 * 60 * 1000
      const mondayKey = `${String(new Date(mondayTs).getMonth() + 1).padStart(2, '0')}/${String(
        new Date(mondayTs).getDate()
      ).padStart(2, '0')}`
      const mondayIdx = scheduleData.value.findIndex((d) => d.dateTime === mondayKey)
      weekStartIndex.value = Math.max(0, mondayIdx >= 0 ? mondayIdx : (idx >= 0 ? idx : 0) - offsetToMonday)
    },
    { immediate: true, deep: true }
  )
  const previewState = ref(null) // { targetDate, roleType, block, range }
  const copyState = ref({
    active: false,
    sourceDate: null,
    sourceData: null
  })

  const handleCopyDay = (dayData) => {
    copyState.value = {
      active: true,
      sourceDate: dayData.dateTime,
      sourceData: JSON.parse(JSON.stringify(dayData.data))
    }
  }

  const handleExitCopy = () => {
    copyState.value = {
      active: false,
      sourceDate: null,
      sourceData: null
    }
  }

  const handlePasteDay = async (targetDayData) => {
    if (!copyState.value.active || !copyState.value.sourceData) return

    const targetBaseTime = getBaseTime(targetDayData.dateTime)
    const sourceBaseTime = getBaseTime(copyState.value.sourceDate)
    const timeOffset = targetBaseTime - sourceBaseTime

    const targetDay = scheduleData.value.find((d) => d.dateTime === targetDayData.dateTime)
    if (!targetDay) return

    const insertedIdSet = new Set()
    const sessions = []
    const workDay = toDateStr(targetDayData.dateTime)
    const liveRoomId = props.liveRoomId !== undefined && props.liveRoomId !== null ? String(props.liveRoomId) : ''

    copyState.value.sourceData.forEach((sourceRole) => {
      const targetRole = targetDay.data.find((r) => r.type === sourceRole.type)
      if (targetRole) {
        const positionId = String(targetRole.positionId || '') || String(props.anchorPositionId || '')
        // 生成新块并追加
        const newBlocks = sourceRole.data.map((block, index) => {
          // 生成唯一ID: timestamp + random + index
          const newId = Date.now() + Math.floor(Math.random() * 10000) + index
          const newStartTime = block.startTime + timeOffset
          const newTimes = block.times.map((t) => t + timeOffset)

          return {
            ...block,
            id: newId,
            startTime: newStartTime,
            times: newTimes
          }
        })

        // 简单去重策略：如果追加可能会导致严重重叠，但这是用户行为。
        // 这里直接追加。
        targetRole.data.push(...newBlocks)
        targetRole.data.sort((a, b) => a.times[0] - b.times[0])

        newBlocks.forEach((b) => insertedIdSet.add(b.id))
        if (String(props.submitMode) === 'liveRoom' && liveRoomId && positionId) {
          newBlocks.forEach((b) => {
            const start = dayjs(Number(b?.times?.[0]))
            const end = dayjs(Number(b?.times?.[1]))
            if (!start.isValid() || !end.isValid()) return
            const durationMin = Math.max(0, Math.floor(end.diff(start, 'minute', true)))
            const employeeId = b?.memberId !== undefined && b?.memberId !== null ? String(b.memberId) : ''
            if (!employeeId) return
            sessions.push({
              positionId,
              startWork: start.format('HH:mm:ss'),
              endWork: end.format('HH:mm:ss'),
              scheduleDuration: durationMin,
              restDuration: Number(b?.restDuration) || 0,
              employees: [{ employeeId, positionId }]
            })
          })
        }
      }
    })

    if (String(props.submitMode) === 'liveRoom') {
      if (!liveRoomId) {
        ElMessage.error('缺少直播间ID，无法保存粘贴排班')
        return
      }
      if (!sessions.length) {
        ElMessage.warning('无可保存的排班数据')
        return
      }
      try {
        await apiModule.roomSchedule.add({ liveRoomId, workDay, sessions })
        ElMessage.success('粘贴成功')
        emit('saved', { mode: 'paste', workDay })
      } catch (e) {
        targetDay.data.forEach((row) => {
          if (!Array.isArray(row?.data) || !row.data.length) return
          row.data = row.data.filter((b) => !insertedIdSet.has(b?.id))
        })
        console.error(e)
      }
    }
  }

  const handleUpdateData = ({ dayDate, roleType, newBlock }) => {
    /**
     * 更新数据处理函数
     * @param {Object} params - 参数对象
     * @param {string} params.dayDate - 日期字符串
     * @param {number} params.roleType - 角色类型
     * @param {Object} params.newBlock - 新的排班块数据
     */
    // 查找对应天和角色，插入数据
    const day = scheduleData.value.find((d) => d.dateTime === dayDate)
    if (day) {
      const rowData = day.data.find((r) => r.type === roleType)
      if (rowData) {
        rowData.data.push(newBlock)
        // 简单排序，保证渲染顺序（虽然绝对定位不影响，但为了逻辑清晰）
        rowData.data.sort((a, b) => a.times[0] - b.times[0])
      }
    }
  }

  const handleUpdateBlockData = ({ dayDate, roleType, newBlock }) => {
    /**
     * 更新已有排班块数据
     * @param {Object} params - 参数对象
     * @param {string} params.dayDate - 日期字符串
     * @param {number} params.roleType - 角色类型
     * @param {Object} params.newBlock - 更新后的排班块数据
     */
    const day = scheduleData.value.find((d) => d.dateTime === dayDate)
    if (day) {
      const rowData = day.data.find((r) => r.type === roleType)
      if (rowData) {
        // 找到并更新
        const index = rowData.data.findIndex((b) => b.id === newBlock.id)
        if (index !== -1) {
          // 更新属性
          const durationMinutes = (newBlock.times[1] - newBlock.times[0]) / (1000 * 60)

          rowData.data[index] = {
            ...rowData.data[index],
            ...newBlock,
            startTime: newBlock.times[0],
            timeCount: durationMinutes
          }

          // 重新排序
          rowData.data.sort((a, b) => a.times[0] - b.times[0])
        }
      }
    }
  }

  const handleRemoveBlockData = ({ dayDate, roleType, block }) => {
    /**
     * 删除排班块数据
     * @param {Object} params - 参数对象
     * @param {string} params.dayDate - 日期字符串
     * @param {number} params.roleType - 角色类型
     * @param {Object} params.block - 要删除的排班块
     */
    if (String(props.submitMode) === 'liveRoom' && (block?.scheduleId || block?.id)) {
      emit('remove-block-data', { dayDate, roleType, block })
      return
    }

    const day = scheduleData.value.find((d) => d.dateTime === dayDate)
    if (day) {
      const rowData = day.data.find((r) => r.type === roleType)
      if (rowData) {
        const index = rowData.data.findIndex((b) => b.id === block.id)
        if (index !== -1) {
          rowData.data.splice(index, 1)
        }
      }
    }
  }

  const handleDragPreview = ({ sourceDate, roleType, payload }) => {
    /**
     * 处理拖拽预览
     * @param {Object} params - 参数对象
     * @param {string} params.sourceDate - 源日期
     * @param {number} params.roleType - 角色类型
     * @param {Object} params.payload - 拖拽负载信息
     */
    if (!payload) {
      previewState.value = null
      return
    }

    const { block, range } = payload
    const DAY_MS = 24 * 60 * 60 * 1000

    // 计算目标日期
    let targetDate = null
    let targetRange = null

    const dayIndex = scheduleData.value.findIndex((d) => d.dateTime === sourceDate)
    if (dayIndex === -1) return

    if (range.start < 0) {
      // Prev Day
      if (dayIndex > 0) {
        targetDate = scheduleData.value[dayIndex - 1].dateTime
        targetRange = {
          start: DAY_MS + range.start,
          end: DAY_MS + range.end
        }
      }
    } else if (range.end > DAY_MS) {
      // Next Day
      if (dayIndex < scheduleData.value.length - 1) {
        targetDate = scheduleData.value[dayIndex + 1].dateTime
        targetRange = {
          start: range.start - DAY_MS,
          end: range.end - DAY_MS
        }
      }
    }

    if (targetDate) {
      previewState.value = {
        targetDate,
        roleType,
        block,
        range: targetRange
      }
    } else {
      previewState.value = null
    }
  }

  const handleCrossDayDrag = (dayIndex, { roleType, originalBlock, newRange }) => {
    /**
     * 处理跨天拖拽逻辑
     * @param {number} dayIndex - 当前拖拽发生的日期索引
     * @param {Object} payload - 拖拽数据包含 roleType, originalBlock, newRange
     * @description 判断id是否重复，如果重复则修改原有id而不是添加新的数据。
     * 如果时跨天之后，前一天和后一天如果没有同样id的时间段则新添加时间段。
     * 如果从跨天时间在拖拽回当天的时候则检查当天的时间段是否还存在，如果还有id相同的数据则修改当前数据，而不是新添加数据。
     */
    // Clear preview
    previewState.value = null

    const DAY_MS = 24 * 60 * 60 * 1000
    const currentDayData = scheduleData.value[dayIndex]

    // 计算绝对时间范围
    const currentBaseTime = getBaseTime(currentDayData.dateTime)
    const absStartTime = currentBaseTime + newRange.start
    const absEndTime = currentBaseTime + newRange.end

    // 计算新的总时长
    const newTotalTimeCount = (absEndTime - absStartTime) / (1000 * 60)

    // 判断是否真正跨天
    const startDate = new Date(absStartTime)
    const endDate = new Date(absEndTime - 1)
    const isActuallyCrossDay = startDate.getDate() !== endDate.getDate() || startDate.getMonth() !== endDate.getMonth()

    // 需要检查的日期索引范围：前一天、当前天、后一天
    const checkIndices = [dayIndex - 1, dayIndex, dayIndex + 1]

    checkIndices.forEach((idx) => {
      // 索引越界检查
      if (idx < 0 || idx >= scheduleData.value.length) return

      const targetDay = scheduleData.value[idx]
      const targetBaseTime = getBaseTime(targetDay.dateTime)
      const targetNextBaseTime = targetBaseTime + DAY_MS

      // 计算该天内的有效时间段 (取交集)
      const start = Math.max(absStartTime, targetBaseTime)
      const end = Math.min(absEndTime, targetNextBaseTime)

      const targetRowData = targetDay.data.find((r) => r.type === roleType)
      if (!targetRowData) return

      // 查找是否存在相同 ID 的 block
      const existingBlockIndex = targetRowData.data.findIndex((b) => b.id === originalBlock.id)

      if (start < end) {
        // 该天有排班时间段
        const offset = start - absStartTime // 计算相对于整体开始时间的偏移量 (ms)

        const newBlockData = {
          ...originalBlock,
          times: [start, end],
          startTime: start,
          timeCount: (end - start) / (1000 * 60), // 当前片段时长
          originalTimeCount: newTotalTimeCount, // 更新为新的总时长
          crossDayOffset: isActuallyCrossDay ? offset : undefined, // 记录偏移量
          isCrossDay: isActuallyCrossDay // 标记为跨天数据
        }

        if (existingBlockIndex !== -1) {
          // 存在则更新
          targetRowData.data[existingBlockIndex] = {
            ...targetRowData.data[existingBlockIndex],
            ...newBlockData
          }
        } else {
          // 不存在则添加
          targetRowData.data.push(newBlockData)
        }
        // 排序
        targetRowData.data.sort((a, b) => a.times[0] - b.times[0])
      } else {
        // 该天没有排班时间段（或者时间段无效）
        if (existingBlockIndex !== -1) {
          // 如果之前有，现在没了，则删除
          targetRowData.data.splice(existingBlockIndex, 1)
        }
      }
    })
  }

  const editDialogVisible = ref(false)
  const dialogContext = ref(null)
  const shiftDrawerRef = ref(null)
  const shiftFormInitial = ref({})

  const memberPickDisabled = computed(() => {
    return String(props.submitMode) === 'liveRoom' && dialogContext.value?.mode === 'edit'
  })

  const companyOptions = ref([])
  const memberDrawerVisible = ref(false)
  const memberDeptOptions = ref([])
  const lastMemberDeptOptionsCompanyId = ref('')
  const memberTeamOptions = ref([])
  const lastMemberTeamOptionsDeptId = ref('')
  const memberList = ref([])
  const memberTotal = ref(0)
  const memberLoading = ref(false)
  const memberPickIndex = ref(-1)
  const memberSelectedId = ref('')
  const isSyncingMemberOrg = ref(false)
  const memberOrgAutoFill = ref(true)

  const memberQuery = reactive({
    keyword: '',
    companyId: '',
    deptId: '',
    teamId: '',
    page: 1,
    limit: 10
  })

  const memberDeptIdModel = computed({
    get() {
      return memberQuery.deptId
    },
    set(v) {
      memberOrgAutoFill.value = false
      memberQuery.deptId = v
    }
  })

  const memberTeamIdModel = computed({
    get() {
      return memberQuery.teamId
    },
    set(v) {
      memberOrgAutoFill.value = false
      memberQuery.teamId = v
    }
  })

  watch(
    () => memberQuery.companyId,
    async (v, ov) => {
      if (isSyncingMemberOrg.value) return
      if (String(v || '') === String(ov || '')) return
      memberQuery.deptId = ''
      memberQuery.teamId = ''
      memberTeamOptions.value = []
      await fetchMemberDeptOptions()
      applyLiveRoomOrgToMemberQuery()
    }
  )

  watch(
    () => memberQuery.deptId,
    async (v, ov) => {
      if (isSyncingMemberOrg.value) return
      if (String(v || '') === String(ov || '')) return
      memberQuery.teamId = ''
      await fetchMemberTeamOptions()
      applyLiveRoomOrgToMemberQuery()
    }
  )

  watch(
    () => [props.submitMode, props.liveRoomId, props.companyId, props.deptId, props.teamId],
    () => Promise.resolve().then(() => fetchLiveRoomOrg()),
    { immediate: true }
  )

  const dialogTitle = computed(() => {
    if (dialogContext.value?.mode === 'edit') return '修改排班'
    return '添加排班'
  })

  const dialogSubmitText = computed(() => {
    if (dialogContext.value?.mode === 'edit') return '确认修改'
    return '确定'
  })

  const toDateStr = (dateTime) => dayjs(getBaseTime(dateTime)).format('YYYY-MM-DD')
  const toDateTimeKey = (dateStr) => {
    const d = dayjs(dateStr)
    return `${d.format('MM')}/${d.format('DD')}`
  }

  const normalizeDuration = (durationMin) => {
    const n = Number(durationMin) || 0
    const candidates = durationOptionList.value
    let best = candidates[0]
    let minDiff = Math.abs(n - best)
    candidates.forEach((c) => {
      const diff = Math.abs(n - c)
      if (diff < minDiff) {
        minDiff = diff
        best = c
      }
    })
    return best
  }

  const parseNonNegativeNumber = (raw) => {
    const n = raw === null || raw === undefined ? NaN : Number(raw)
    if (!Number.isFinite(n)) return null
    if (n < 0) return null
    return n
  }

  const getRestMinValue = (source) => {
    const raw = source?.restDuration
    const n = parseNonNegativeNumber(raw)
    if (n !== null) return n
    const fallback = parseNonNegativeNumber(restOptionList.value?.[0])
    return fallback !== null ? fallback : 0
  }

  const getTargetPositionId = (ctx) => {
    const anchorPositionId =
      props.anchorPositionId !== undefined && props.anchorPositionId !== null ? String(props.anchorPositionId) : ''
    const targetPositionId = String(ctx?.positionId || '') || anchorPositionId
    return targetPositionId
  }

  const checkScheduleConfig = async (ctx) => {
    if (String(props.submitMode) !== 'liveRoom') return true
    const liveRoomId = props.liveRoomId !== undefined && props.liveRoomId !== null ? String(props.liveRoomId) : ''
    const positionId = getTargetPositionId(ctx)
    if (!liveRoomId || !positionId) {
      const shouldGo = await ElMessageBox.confirm('请先完成排班配置后再进行排班操作', '提示', {
        type: 'warning',
        confirmButtonText: '前往排班配置',
        cancelButtonText: '取消',
        closeOnClickModal: false,
        closeOnPressEscape: false,
        distinguishCancelAndClose: true
      })
        .then(() => true)
        .catch(() => false)

      if (shouldGo) emit('open-config')
      return false
    }
    return true
  }

  const openDialog = async (ctx) => {
    if (!(await checkScheduleConfig(ctx))) return
    dialogContext.value = ctx
    const date = toDateStr(ctx.dateTime)
    shiftFormInitial.value = {
      date,
      shifts: [
        {
          memberId: '',
          memberName: '',
          startTime: '',
          durationMin: null,
          restMin: null
        }
      ],
      batch: {
        weekdays: [],
        endDate: ''
      }
    }
    editDialogVisible.value = true
  }

  const openAdd = async (dateStr) => {
    const date = typeof dateStr === 'string' ? dateStr : dayjs().format('YYYY-MM-DD')
    if (String(props.submitMode) === 'liveRoom') {
      if (!(await checkScheduleConfig({ positionId: '' }))) return
    }
    shiftFormInitial.value = {
      date,
      shifts: [
        {
          memberId: '',
          memberName: '',
          startTime: '',
          durationMin: null,
          restMin: null
        }
      ],
      batch: { weekdays: [], endDate: '' }
    }
    dialogContext.value = { mode: 'add', dateTime: toDateTimeKey(date) }
    editDialogVisible.value = true
  }

  const pickHhmm = (raw, fallback = '') => {
    const value = raw === undefined || raw === null ? '' : String(raw)
    return value.match(/(\d{1,2}:\d{2})/)?.[1] || fallback
  }

  const pickFirstEmployee = (schedule) => {
    if (!schedule) return { memberId: '', memberName: '' }
    const positions = Array.isArray(schedule.positions) ? schedule.positions : []
    for (let i = 0; i < positions.length; i++) {
      const p = positions[i]
      const emps = Array.isArray(p?.employees) ? p.employees : []
      const hit = emps.find((e) => e?.employeeId !== undefined && e?.employeeId !== null && e?.employeeId !== '')
      if (!hit) continue
      return { memberId: String(hit.employeeId), memberName: String(hit.employeeName || '') }
    }
    const employees = Array.isArray(schedule.employees) ? schedule.employees : []
    const hit = employees.find((e) => e?.employeeId !== undefined && e?.employeeId !== null && e?.employeeId !== '')
    if (hit) return { memberId: String(hit.employeeId), memberName: String(hit.employeeName || '') }
    if (schedule.employeeId !== undefined && schedule.employeeId !== null && schedule.employeeId !== '') {
      return { memberId: String(schedule.employeeId), memberName: String(schedule.employeeName || schedule.name || '') }
    }
    return { memberId: '', memberName: '' }
  }

  const buildEditInitialByScheduleDetail = (schedule) => {
    const base = schedule || {}
    const sessions = Array.isArray(base.sessions) ? base.sessions : []
    const firstSession = sessions[0] || {}
    const normalized = {
      ...base,
      startWork: base.startWork || firstSession.startWork,
      scheduleDuration: base.scheduleDuration || firstSession.scheduleDuration,
      restDuration: base.restDuration || firstSession.restDuration,
      employees: Array.isArray(base.employees) && base.employees.length ? base.employees : firstSession.employees
    }

    const date = String(normalized?.workDay || normalized?.date || '')
    const startTime = pickHhmm(normalized?.startWork, String(props.defaultStartTime || '08:00'))
    const durationMinRaw = Number(normalized?.scheduleDuration)
    const durationMin =
      Number.isFinite(durationMinRaw) && durationMinRaw > 0
        ? normalizeDuration(durationMinRaw)
        : Number(durationOptionList.value?.[0] || 60)
    const { memberId, memberName } = pickFirstEmployee(normalized)
    return {
      date,
      shifts: [
        {
          memberId,
          memberName,
          startTime,
          durationMin,
          restMin: getRestMinValue(normalized)
        }
      ],
      batch: { weekdays: [], endDate: '' }
    }
  }

  const openEditByRow = async (row) => {
    if (!row) return
    const scheduleId = row?.id
    if (!scheduleId) {
      ElMessage.error('缺少排班ID')
      return
    }
    const res = await apiModule.roomSchedule.detail(
      { roomScheduleId: scheduleId },
      { showLoading: true, loadingText: '加载排班详情...' }
    )
    const schedule = res?.data || {}
    const initial = buildEditInitialByScheduleDetail(schedule)
    shiftFormInitial.value = initial
    dialogContext.value = {
      mode: 'edit',
      dateTime: toDateTimeKey(initial.date),
      block: { scheduleId: schedule.id || scheduleId }
    }
    editDialogVisible.value = true
  }

  defineExpose({ openAdd, openEditByRow })

  const handleFixedAdd = async (payload) => {
    const range = payload?.range
    if (!range) return
    if (!(await checkScheduleConfig(payload))) return
    await openDialog({ mode: 'add', ...payload })
  }

  const handleEditBlock = async (payload) => {
    const block = payload?.block
    if (!block) return
    const scheduleId = block?.scheduleId || block?.id
    if (!scheduleId) {
      ElMessage.error('缺少排班ID')
      return
    }
    const res = await apiModule.roomSchedule.detail(
      { roomScheduleId: scheduleId },
      { showLoading: true, loadingText: '加载排班详情...' }
    )
    const schedule = res?.data || {}
    const initial = buildEditInitialByScheduleDetail(schedule)
    shiftFormInitial.value = initial
    dialogContext.value = {
      mode: 'edit',
      ...payload,
      dateTime: toDateTimeKey(initial.date),
      block: { ...(block || {}), scheduleId }
    }
    editDialogVisible.value = true
  }

  const removeAllBlocksById = (id, roleType) => {
    scheduleData.value.forEach((day) => {
      const row = (day.data || []).find((r) => r.type === roleType)
      if (!row) return
      row.data = (row.data || []).filter((b) => b.id !== id)
    })
  }

  const submitDialog = async () => {
    const ctx = dialogContext.value
    if (!ctx) return
    const payload = shiftDrawerRef.value?.getSubmitData?.()
    if (!payload?.date) {
      ElMessage.error('请选择班次日期')
      return
    }
    if (!Array.isArray(payload.shifts) || !payload.shifts.length) {
      ElMessage.error('请至少添加一场排班')
      return
    }
    const invalidDuration = (payload.shifts || []).find((s) => (Number(s?.durationMin) || 0) <= 0)
    if (invalidDuration) {
      ElMessage.error('请选择班次时长')
      return
    }

    if (String(props.submitMode) === 'liveRoom') {
      const liveRoomId = props.liveRoomId !== undefined && props.liveRoomId !== null ? String(props.liveRoomId) : ''
      if (!liveRoomId) {
        ElMessage.error('缺少直播间ID')
        return
      }

      const baseDate = payload.date
      if (ctx.mode === 'edit') {
        const shift = payload.shifts[0]
        const scheduleId = ctx?.block?.scheduleId || ctx?.block?.id
        if (!scheduleId) {
          ElMessage.error('缺少排班ID')
          return
        }
        const durationMin = Number(shift.durationMin) || 0
        const startDt = dayjs(`${baseDate} ${shift.startTime}`)
        const endDt = startDt.add(durationMin, 'minute')
        await apiModule.roomSchedule.update({
          id: scheduleId,
          liveRoomId,
          startWork: startDt.format('HH:mm:ss'),
          endWork: endDt.format('HH:mm:ss'),
          scheduleDuration: durationMin,
          restDuration: Number(shift.restMin) || 0
        })
        ElMessage.success('修改成功')
        editDialogVisible.value = false
        emit('saved', { mode: 'edit', id: scheduleId })
        return
      }

      const anchorPositionId =
        props.anchorPositionId !== undefined && props.anchorPositionId !== null ? String(props.anchorPositionId) : ''
      const targetPositionId = String(ctx?.positionId || '') || anchorPositionId
      if (!targetPositionId) {
        ElMessage.error('缺少岗位ID')
        return
      }
      const invalidShift = (payload.shifts || []).find((s) => !s?.memberId)
      if (invalidShift) {
        ElMessage.error('请选择成员')
        return
      }
      const sessions = payload.shifts.map((s) => {
        const durationMin = Number(s.durationMin) || 0
        const startDt = dayjs(`${baseDate} ${s.startTime}`)
        const endDt = startDt.add(durationMin, 'minute')
        return {
          positionId: targetPositionId,
          startWork: startDt.format('HH:mm:ss'),
          endWork: endDt.format('HH:mm:ss'),
          scheduleDuration: durationMin,
          restDuration: Number(s.restMin) || 0,
          employees: [{ employeeId: String(s.memberId), positionId: targetPositionId }]
        }
      })
      const req = { liveRoomId, workDay: baseDate, sessions }
      const weekdays = Array.isArray(payload.batch?.weekdays) ? payload.batch.weekdays : []
      const endDate = payload.batch?.endDate
      const cycleDays = weekdays
        .map((d) => (Number(d) === 0 ? 7 : Number(d)))
        .filter((d) => Number.isFinite(d) && d >= 1 && d <= 7)
      if (cycleDays.length && endDate) req.batchConfig = { cycleDays, endDate }
      await apiModule.roomSchedule.add(req)
      ElMessage.success('新增成功')
      editDialogVisible.value = false
      emit('saved', { mode: 'add', workDay: baseDate })
      return
    }

    const baseDate = payload.date
    const targetDates = [baseDate]
    if (
      ctx.mode === 'add' &&
      payload.batch?.endDate &&
      Array.isArray(payload.batch?.weekdays) &&
      payload.batch.weekdays.length
    ) {
      const end = dayjs(payload.batch.endDate)
      const start = dayjs(baseDate)
      if (!end.isValid() || end.isBefore(start, 'day')) {
        ElMessage.error('请选择有效的排班结束日期')
        return
      }
      const weekdays = new Set(payload.batch.weekdays.map((x) => Number(x)))
      const diffDays = Math.min(366, end.diff(start, 'day'))
      for (let i = 1; i <= diffDays; i++) {
        const d = start.add(i, 'day')
        if (weekdays.has(d.day())) targetDates.push(d.format('YYYY-MM-DD'))
      }
    }

    const addBlocksForDate = (dateStr, shifts) => {
      const startKey = toDateTimeKey(dateStr)
      const dayIndex = scheduleData.value.findIndex((d) => d.dateTime === startKey)
      if (dayIndex === -1) return { ok: false, msg: `${dateStr} 不在当前排班数据范围内` }

      const baseTime = getBaseTime(startKey)
      const isToday = dayjs(dateStr).isSame(dayjs(), 'day')
      const color = isToday ? '#3d5af1' : '#e5e7eb'
      const textColor = isToday ? '#ffffff' : '#303133'

      shifts.forEach((s, idx) => {
        const startStr = `${dateStr} ${s.startTime}`
        const startTs = dayjs(startStr).valueOf()
        const durationMin = Number(s.durationMin) || 0
        const endTs = dayjs(startStr).add(durationMin, 'minute').valueOf()
        if (!dayjs(startStr).isValid() || !dayjs(endTs).isValid() || durationMin <= 0) return

        const newRange = { start: startTs - baseTime, end: endTs - baseTime }
        const blockName = (s.memberName || '').trim() || `未添加${ctx.rowName || ''}`.trim() || '未添加'
        const newBlock = {
          name: blockName,
          id: `${Date.now()}_${Math.floor(Math.random() * 10000)}_${idx}`,
          memberId: s.memberId ? String(s.memberId) : null,
          timeCount: durationMin,
          originalTimeCount: durationMin,
          startTime: startTs,
          times: [startTs, endTs],
          restMin: Number(s.restMin) || 0,
          color,
          textColor,
          isTemp: true
        }
        handleCrossDayDrag(dayIndex, { block: newBlock, newRange, roleType: ctx.roleType })
      })
      return { ok: true }
    }

    if (ctx.mode === 'edit') {
      const original = ctx.block
      if (original?.id) removeAllBlocksById(original.id, ctx.roleType)
      const oneShift = payload.shifts[0]
      const startStr = `${baseDate} ${oneShift.startTime}`
      const startTs = dayjs(startStr).valueOf()
      const durationMin = Number(oneShift.durationMin) || 0
      const endTs = dayjs(startStr).add(durationMin, 'minute').valueOf()
      if (!dayjs(startStr).isValid() || durationMin <= 0) {
        ElMessage.error('请选择有效的班次时间')
        return
      }
      const startKey = toDateTimeKey(baseDate)
      const dayIndex = scheduleData.value.findIndex((d) => d.dateTime === startKey)
      if (dayIndex === -1) {
        ElMessage.error('选择的日期不在当前排班数据范围内')
        return
      }
      const baseTime = getBaseTime(startKey)
      const newRange = { start: startTs - baseTime, end: endTs - baseTime }
      const isToday = dayjs(baseDate).isSame(dayjs(), 'day')
      const color = isToday ? '#3d5af1' : '#e5e7eb'
      const textColor = isToday ? '#ffffff' : '#303133'
      const blockName =
        (oneShift.memberName || '').trim() ||
        (original?.name || '').trim() ||
        `未添加${ctx.rowName || ''}`.trim() ||
        '未添加'
      const newBlock = {
        ...original,
        name: blockName,
        memberId: oneShift.memberId ? String(oneShift.memberId) : original?.memberId || null,
        timeCount: durationMin,
        originalTimeCount: durationMin,
        startTime: startTs,
        times: [startTs, endTs],
        restMin: Number(oneShift.restMin) || 0,
        color,
        textColor,
        isCrossDay: false
      }
      handleCrossDayDrag(dayIndex, { block: newBlock, newRange, roleType: ctx.roleType })
      editDialogVisible.value = false
      return
    }

    const errors = []
    targetDates.forEach((d) => {
      const res = addBlocksForDate(d, payload.shifts)
      if (!res.ok && res.msg) errors.push(res.msg)
    })
    if (errors.length) {
      ElMessage.warning(errors[0])
    }
    editDialogVisible.value = false
  }

  const normalizeOrgId = (id) => {
    if (id === undefined || id === null || id === '') return undefined
    return String(id)
  }

  const liveRoomOrg = reactive({ companyId: '', deptId: '', teamId: '' })
  const liveRoomOrgLoading = ref(false)

  const fetchLiveRoomOrg = async () => {
    if (String(props.submitMode) !== 'liveRoom') return
    const liveRoomId = normalizeOrgId(props.liveRoomId)
    if (!liveRoomId) return
    if (normalizeOrgId(props.deptId) && normalizeOrgId(props.teamId)) return
    if (normalizeOrgId(liveRoomOrg.deptId) && normalizeOrgId(liveRoomOrg.teamId)) return
    if (liveRoomOrgLoading.value) return
    liveRoomOrgLoading.value = true
    try {
      const res = await apiModule.liveRoom.detail({ id: liveRoomId })
      const data = res?.data || {}
      liveRoomOrg.companyId =
        data.companyId !== undefined && data.companyId !== null ? String(data.companyId) : liveRoomOrg.companyId
      liveRoomOrg.deptId = data.deptId !== undefined && data.deptId !== null ? String(data.deptId) : liveRoomOrg.deptId
      liveRoomOrg.teamId = data.teamId !== undefined && data.teamId !== null ? String(data.teamId) : liveRoomOrg.teamId
    } catch (e) {
      void e
    } finally {
      liveRoomOrgLoading.value = false
    }
  }

  const applyLiveRoomOrgToMemberQuery = () => {
    if (String(props.submitMode) !== 'liveRoom') return
    const nextCompanyId = normalizeOrgId(props.companyId) || normalizeOrgId(liveRoomOrg.companyId)
    const nextDeptId = normalizeOrgId(props.deptId) || normalizeOrgId(liveRoomOrg.deptId)
    const nextTeamId = normalizeOrgId(props.teamId) || normalizeOrgId(liveRoomOrg.teamId)
    if (!nextCompanyId && !nextDeptId && !nextTeamId) return

    isSyncingMemberOrg.value = true
    try {
      const currentCompanyId = normalizeOrgId(memberQuery.companyId)
      const companyChanged = currentCompanyId && nextCompanyId && currentCompanyId !== nextCompanyId
      if (companyChanged) {
        memberQuery.companyId = nextCompanyId
        memberQuery.deptId = ''
        memberQuery.teamId = ''
      } else if (!currentCompanyId && nextCompanyId) {
        memberQuery.companyId = nextCompanyId
      }
      if (memberOrgAutoFill.value && !normalizeOrgId(memberQuery.deptId) && nextDeptId) memberQuery.deptId = nextDeptId
      if (memberOrgAutoFill.value && !normalizeOrgId(memberQuery.teamId) && nextTeamId) memberQuery.teamId = nextTeamId
    } finally {
      isSyncingMemberOrg.value = false
    }
  }

  const normalizeEmployeeList = (raw) => {
    const list = Array.isArray(raw) ? raw : []
    return list.map((item) => ({
      ...item,
      id: item.id !== undefined && item.id !== null ? String(item.id) : '',
      avatar: item.userAvatar || item.avatar || '',
      companyName: item.companyName || item.company || '',
      deptName: item.deptName || item.department || '',
      teamName: item.teamName || item.group || '',
      positionName: item.positionName || item.positionDesc || '',
      orgName:
        `${item.deptName || item.department || ''}${item.teamName || item.group ? `-${item.teamName || item.group}` : ''}` ||
        '-'
    }))
  }

  const fetchCompanyOptions = async () => {
    const res = await apiModule.subCompany.options({ limit: 200 })
    companyOptions.value = normalizeKeyLabelOptions(res.data || []).map((i) => ({
      ...i,
      key: String(i.key),
      label: i.label
    }))
  }

  const fetchMemberDeptOptions = async () => {
    const companyId = normalizeOrgId(memberQuery.companyId)
    if (!companyId) {
      memberDeptOptions.value = []
      lastMemberDeptOptionsCompanyId.value = ''
      return
    }
    memberDeptOptions.value = []
    lastMemberDeptOptionsCompanyId.value = companyId
  }

  const fetchMemberTeamOptions = async () => {
    const deptId = normalizeOrgId(memberQuery.deptId)
    if (!deptId) {
      memberTeamOptions.value = []
      lastMemberTeamOptionsDeptId.value = ''
      return
    }
    memberTeamOptions.value = []
    lastMemberTeamOptionsDeptId.value = deptId
  }

  const fetchMemberList = async (reset = false) => {
    if (reset) memberQuery.page = 1
    applyLiveRoomOrgToMemberQuery()
    memberLoading.value = true
    try {
      const query = {
        page: memberQuery.page,
        limit: memberQuery.limit,
        name: memberQuery.keyword || undefined,
        openMain: true
      }

      // const companyId = normalizeOrgId(memberQuery.companyId)
      // const deptId = normalizeOrgId(memberQuery.deptId)
      // const teamId = normalizeOrgId(memberQuery.teamId)
      const positionId =
        dialogContext.value?.positionId !== undefined && dialogContext.value?.positionId !== null
          ? String(dialogContext.value.positionId)
          : ''
      // if (teamId) {
      //   query.teamIds = [teamId]
      // } else if (deptId) {
      //   query.deptIds = [deptId]
      // } else if (companyId) {
      //   query.companyIds = [companyId]
      // }
      if (positionId) query.positionIds = [positionId]

      const res = await apiModule.employee.specialPage(query)
      memberList.value = normalizeEmployeeList(res.data?.list || [])
      memberTotal.value = res.data?.totalCount || res.data?.total || 0
    } finally {
      memberLoading.value = false
    }
  }

  const confirmMember = (row) => {
    if (!row) return
    shiftDrawerRef.value?.setMember?.(memberPickIndex.value, { id: row.id, name: row.name })
    memberSelectedId.value = row?.id !== undefined && row?.id !== null ? String(row.id) : ''
  }

  const handlePickMember = async ({ index }) => {
    memberPickIndex.value = Number(index)
    memberSelectedId.value = ''
    const current = shiftDrawerRef.value?.getSubmitData?.()
    const shifts = Array.isArray(current?.shifts) ? current.shifts : []
    const picked = shifts[memberPickIndex.value]
    if (picked?.memberId !== undefined && picked?.memberId !== null && String(picked.memberId)) {
      memberSelectedId.value = String(picked.memberId)
    }
    memberQuery.keyword = ''
    memberQuery.page = 1
    memberQuery.limit = 10
    memberOrgAutoFill.value = true
    await fetchLiveRoomOrg()
    applyLiveRoomOrgToMemberQuery()
    memberDrawerVisible.value = true
    if (!(companyOptions.value || []).length) await fetchCompanyOptions()
    await fetchMemberList(true)
  }
</script>

<style scoped lang="scss">
  .schedule-container {
    width: 100%;
    border-radius: 4px;
    background: #fff;
    overflow-x: auto; /* 支持横向滚动如果太窄 */

    .schedule-header {
      display: flex;
      position: sticky;
      top: 0;
      z-index: 300;

      .header-placeholder {
        width: 100px;
        flex-shrink: 0;
        display: flex;
        align-items: center;
        justify-content: center;
      }

      .header-role-placeholder {
        width: 70px;
        flex-shrink: 0;
        // border-right: 1px solid #dcdfe6;
      }

      .time-axis {
        flex: 1;
        position: relative;

        .time-label {
          position: absolute;
          top: 10px;
          // transform: translateX(-50%);
          font-size: 12px;
          color: #909399;

          // &::after {
          //   content: '';
          //   position: absolute;
          //   top: 20px;
          //   left: 50%;
          //   width: 1px;
          //   height: 10px; // 小刻度
          //   background-color: #dcdfe6;
          // }
        }
      }
    }
  }

  :deep(.schedule-member-drawer) {
    .member-filters {
      display: flex;
      align-items: center;
      gap: 12px;
      flex-wrap: wrap;
      margin-bottom: 12px;
    }

    .member-cell {
      display: flex;
      align-items: center;
      gap: 8px;

      .name {
        max-width: 120px;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }
    }

    .member-pagination {
      display: flex;
      justify-content: flex-end;
      padding: 12px 0;
    }

    .drawer-footer {
      display: flex;
      justify-content: flex-end;
      gap: 12px;
    }
  }

  .time-axis-unit-30,
  .time-axis-unit-60,
  .time-axis-unit-120 {
    .time-label {
      font-size: 11px;
    }
  }

  .time-axis-unit-30 {
    .time-label {
      font-size: 10px;
    }
  }

  .time-axis-step-1 {
    .time-label {
      font-size: 12px;
    }
  }

  .time-axis-step-2,
  .time-axis-step-3,
  .time-axis-step-4,
  .time-axis-step-6,
  .time-axis-step-8,
  .time-axis-step-12 {
    .time-label {
      font-size: 12px;
    }
  }

  .week-nav-col {
    display: flex;
    flex-direction: column;
    gap: 6px;
    width: 100%;
  }

  .week-nav-row {
    width: 100px;
    margin-top: 8px;
  }

  .week-nav-btn {
    width: 100%;
    height: 30px;
    flex-shrink: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #909399;
    background: #f7fbff;
    border-radius: 8px;
    cursor: pointer;
  }

  .week-nav-btn.disabled {
    opacity: 0.4;
    cursor: not-allowed;
  }
</style>
