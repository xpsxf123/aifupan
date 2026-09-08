<template>
  <div class="live-room-schedule-detail">
    <LiveRoomInfoCard :live-room-info="liveRoomInfo" />
    <div class="schedule-controls">
      <div class="title">
        直播间排班 ({{ dateRangeText }})
        <el-button
          v-auth="'room:schedule:update'"
          type="primary"
          link
          @click="openAttributeDialog"
          style="margin-left: 12px"
          >修改排班配置</el-button
        >
        <el-button
          v-auth="'room:schedule:list'"
          type="primary"
          link
          @click="downloadScheduleTemplate"
          style="margin-left: 12px"
          >下载排班模板</el-button
        >
        <el-button
          v-auth="'room:schedule:add'"
          type="primary"
          link
          @click="openImportDialog"
          style="margin-left: 12px"
          >导入排班</el-button
        >
      </div>
      <div class="actions">
        <el-button-group>
          <el-button :type="activeRange === 'thisWeek' ? 'primary' : 'default'" @click="setThisWeek">本周</el-button>
          <el-button :type="activeRange === 'nextWeek' ? 'primary' : 'default'" @click="setNextWeek">下周</el-button>
          <el-button :type="activeRange === 'thisMonth' ? 'primary' : 'default'" @click="setThisMonth">本月</el-button>
          <el-button :type="activeRange === 'nextMonth' ? 'primary' : 'default'" @click="setNextMonth">下月</el-button>
        </el-button-group>
        <!--  <el-button v-auth="'room:schedule:add'" type="primary" @click="openAddDialog">新增排班</el-button> -->
        <el-date-picker
          v-model="currentDate"
          type="date"
          placeholder="请选择日期"
          value-format="YYYY-MM-DD"
          style="width: 140px"
        />
      </div>
    </div>

    <div class="schedule-wrapper">
      <Schedule
        ref="scheduleRef"
        :data="scheduleData"
        :readonly="false"
        submit-mode="liveRoom"
        :live-room-id="liveRoomId"
        :company-id="liveRoomInfo.companyId"
        :dept-id="liveRoomInfo.deptId"
        :team-id="liveRoomInfo.teamId"
        :anchor-position-id="anchorPositionId"
        :default-start-time="attributeForm.planRange?.[0] || '08:00'"
        :time-axis-unit="timeAxisUnit"
        :duration-options="attributeForm.shiftOptions"
        :rest-options="attributeForm.restOptions"
        @saved="handleScheduleSaved"
        @open-config="openAttributeDialog"
        @remove-block-data="handleRemoveScheduleBlock"
      />
    </div>
    <div class="schedule-table-wrapper" v-empty="emptyConfig">
      <div class="table-header">
        <div class="table-title">排班列表</div>
      </div>
      <Curd
        ref="curdRef"
        :table-columns="scheduleTableColumns"
        :api="tableApi"
        :auto-load="false"
        :show-search="false"
        :show-add="false"
        :show-operation="false"
        :show-toolbar-right="false"
        :action-config="{ view: false, edit: false, del: false }"
        @load="handleLoad"
      >
        <template #positions="{ row }">
          <div class="positions-cell">
            <div v-for="p in row.positions || []" :key="p.positionId" class="position-block">
              <div class="position-name">{{ p.positionName }}</div>
              <div class="position-emps">
                <div v-for="e in p.employees || []" :key="e.employeeId" class="emp-item">
                  <el-tag size="small">{{ e.employeeName }}</el-tag>
                  <el-popconfirm width="200" title="确认移除该人员？" @confirm="handleRemoveEmployee(row, e)">
                    <template #reference>
                      <el-button v-auth="'room:schedule:update'" type="danger" link>移除</el-button>
                    </template>
                  </el-popconfirm>
                </div>
              </div>
            </div>
          </div>
        </template>

        <template #action="{ row }">
          <el-button v-auth="'room:schedule:update'" type="primary" link @click="scheduleRef?.openEditByRow(row)"
            >调整时间</el-button
          >
          <el-button v-auth="'room:schedule:update'" type="primary" link @click="openAddEmployeeDialog(row)"
            >添加人员</el-button
          >
          <el-popconfirm width="200" title="确认删除该排班？" @confirm="handleDelete(row)">
            <template #reference>
              <el-button v-auth="'room:schedule:delete'" type="danger" link>删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </Curd>
    </div>

    <ScheduleAttributeDrawer
      v-model="attributeDialogVisible"
      title="排班配置"
      confirm-text="确定"
      :confirm-permission-code="'room:schedule:update'"
      :room-name="liveRoomInfo.name"
      v-model:planRange="attributeForm.planRange"
      :shift-options="attributeForm.shiftOptions"
      :rest-options="attributeForm.restOptions"
      v-model:shiftOptionInput="shiftOptionInput"
      v-model:restOptionInput="restOptionInput"
      v-model:timeAxisUnit="timeAxisUnit"
      :selected-position-tags="selectedPositionTags"
      :copy-source-name="copySourceName"
      @open-copy="openCopyAttributeDialog"
      @clear-copy="clearCopySource"
      @add-shift-option="addShiftOption"
      @remove-shift-option="removeShiftOption"
      @add-rest-option="addRestOption"
      @remove-rest-option="removeRestOption"
      @open-position="openPositionDialog"
      @remove-position="removeSelectedPosition"
      @confirm="submitAttribute"
      @cancel="attributeDialogVisible = false"
    />
    <ScheduleCopyAttributeDrawer
      v-model="copyAttributeDialogVisible"
      :confirm-permission-code="'room:schedule:update'"
      :selected-id="copySourceDraftId"
      v-model:planRange="copyQuery.planRange"
      v-model:keyword="copyQuery.keyword"
      v-model:page="copyQuery.page"
      v-model:limit="copyQuery.limit"
      :total="copyTotal"
      :list="copyRoomList"
      @search="fetchCopyRoomList(true)"
      @page-change="fetchCopyRoomList()"
      @selection-change="handleCopySelected"
      @confirm="confirmCopyAttribute"
    />

    <SchedulePositionDialog
      v-model="positionDialogVisible"
      v-model:selected-ids="positionDialogValue"
      :options="attributePreset.positionOptions"
      :max="3"
      :confirm-permission-code="'room:schedule:update'"
      dialog-class="schedule-position-dialog"
      @confirm="confirmPositionDialog"
      @cancel="positionDialogVisible = false"
    />

    <el-dialog v-model="addEmployeeDialogVisible" title="添加排班人员" width="520px" class="common-dialog">
      <el-form :model="addEmployeeForm" label-width="60px">
        <el-form-item label="岗位">
          <el-select v-model="addEmployeeForm.positionId" style="width: 100%" class="rounded">
            <el-option v-for="p in liveRoomPositionOptions" :key="p.id" :label="p.name" :value="String(p.id)" />
          </el-select>
        </el-form-item>
        <el-form-item label="人员">
          <el-select
            v-model="addEmployeeForm.employeeId"
            filterable
            clearable
            :disabled="!addEmployeeForm.positionId"
            placeholder="请选择人员"
            class="rounded"
            style="width: 100%"
          >
            <el-option v-for="o in employeeOptions" :key="o.key" :label="o.label" :value="o.key" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button class="operate-btn" @click="addEmployeeDialogVisible = false">取消</el-button>
        <el-button class="operate-btn" v-auth="'room:schedule:update'" type="primary" @click="submitAddEmployee"
          >保存</el-button
        >
      </template>
    </el-dialog>

    <ImportScheduleDialog
      v-model="importDialogVisible"
      :live-room-id="liveRoomId"
      :position-options="selectedPositionTags"
      :rest-options="attributeForm.restOptions"
      @saved="handleScheduleSaved"
    />
  </div>
</template>

<script setup>
  /**
   * @file Detail.vue
   * @description 直播间排班详情页面
   */
  import { computed, onMounted, reactive, ref, watch, watchEffect } from 'vue'
  import dayjs from 'dayjs'
  import { useRoute, useRouter } from 'vue-router'
  import { ElMessage, ElMessageBox } from 'element-plus'
  import Curd from '@/components/Curd/index.vue'
  import Schedule from '@/components/Schedule/index.vue'
  import ScheduleAttributeDrawer from '@/components/ScheduleAttributeDrawer/index.vue'
  import ScheduleCopyAttributeDrawer from '@/components/ScheduleCopyAttributeDrawer/index.vue'
  import SchedulePositionDialog from '@/components/SchedulePositionDialog/index.vue'
  import LiveRoomInfoCard from './components/LiveRoomInfoCard.vue'
  import ImportScheduleDialog from './components/ImportScheduleDialog.vue'
  import apiModule from '@/http/api'
  import { shouldShowGuideEmpty } from '@/utils/empty'
  import { usePermissionStore } from '@/auth/store'

  const route = useRoute()
  const router = useRouter()
  const permissionStore = usePermissionStore()
  const scheduleRef = ref(null)

  const liveRoomId = computed(() => (route.query.id ? String(route.query.id) : ''))
  const liveRoomInfo = reactive({
    id: route.query.id || '-',
    name: route.query.name || '-',
    image: '',
    companyId: '',
    deptId: '',
    teamId: '',
    organization: '-',
    manager: '-'
  })

  const curdRef = ref(null)
  const firstLoaded = ref(false)
  const emptyConfig = reactive({
    visible: false,
    title: '暂无排班',
    description: '请先新增排班后，再查看排班列表。',
    blur: 2,
    buttons: [
      {
        text: '新增排班',
        type: 'primary',
        round: true,
        click: () => openAddDialog()
      },
      {
        text: '返回直播间排班',
        type: 'primary',
        plain: true,
        round: true,
        click: () => router.push('/live-room-ranking/index')
      }
    ]
  })

  const emptyButtonsSeed = [...emptyConfig.buttons]

  watchEffect(() => {
    const canAdd = permissionStore.hasPermission('room:schedule:add')
    emptyConfig.buttons = [...(canAdd ? [emptyButtonsSeed[0]] : []), emptyButtonsSeed[1]].filter(Boolean)
  })

  const handleLoad = (list) => {
    if (!firstLoaded.value) firstLoaded.value = true
    emptyConfig.visible = shouldShowGuideEmpty({
      firstLoaded: firstLoaded.value,
      list,
      params: {
        activeRange: activeRange.value,
        dateRange: dateRange.value
      },
      defaults: {
        activeRange: 'thisWeek',
        dateRange: getWeekRange(currentDate.value, 0)
      }
    })
  }
  const currentDate = ref('')
  const activeRange = ref('thisWeek')
  const dateRange = ref([
    dayjs().startOf('week').add(1, 'day').format('YYYY-MM-DD'),
    dayjs().startOf('week').add(7, 'day').format('YYYY-MM-DD')
  ])
  const scheduleData = ref([])
  const rawRangeList = ref([])

  const employeeOptions = ref([])
  const attributePreset = reactive({ shiftOptions: [], restOptions: [], positionOptions: [] })
  const allPositionLoaded = ref(false)

  const attributeDialogVisible = ref(false)
  const importDialogVisible = ref(false)
  const attributeForm = reactive({
    startPlan: '',
    endPlan: '',
    planRange: ['', ''],
    shiftOptions: [],
    restOptions: [],
    positionIds: []
  })

  const timeAxisUnit = ref(60)
  const shiftOptionInput = ref(120)
  const restOptionInput = ref(0)

  const copyAttributeDialogVisible = ref(false)
  const copySourceId = ref('')
  const copySourceName = ref('')
  const copySourceDraftId = ref('')
  const copySourceDraftName = ref('')
  const copyQuery = reactive({ planRange: ['', ''], keyword: '', page: 1, limit: 10 })
  const copyRoomList = ref([])
  const copyTotal = ref(0)

  const positionDialogVisible = ref(false)
  const positionDialogValue = ref([])

  const pendingOpen = ref(null)
  const isInitializing = ref(true)

  const addEmployeeDialogVisible = ref(false)
  const addEmployeeForm = reactive({
    scheduleId: undefined,
    employeeId: undefined,
    positionId: undefined,
    positionLocked: false
  })

  const dateRangeText = computed(() => {
    const [start, end] = dateRange.value || []
    if (!start || !end) return '-'
    const s = dayjs(start).format('YYYY/MM/DD')
    const e = dayjs(end).format('MM/DD')
    return `${s}-${e}`
  })

  const normalizeDateStr = (str) => {
    if (!str) return ''
    const s = String(str)
    if (s.includes('/')) return s.replaceAll('/', '-')
    return s
  }

  const parseDateTime = (workDay, timeStr) => {
    const w = normalizeDateStr(workDay)
    const t = String(timeStr || '')
    const hasDate = /(\d{4})[-/](\d{1,2})[-/](\d{1,2})/.test(t)
    const raw = hasDate ? normalizeDateStr(t) : `${w} ${t}`
    const d = dayjs(raw)
    if (d.isValid()) return d.valueOf()
    const fallback = dayjs(`${w} 00:00`)
    return fallback.isValid() ? fallback.valueOf() : Date.now()
  }

  const hasDatePart = (value) => {
    const v = value === undefined || value === null ? '' : String(value)
    return /(\d{4})[-/](\d{1,2})[-/](\d{1,2})/.test(v)
  }

  const buildScheduleData = (list, range) => {
    const [start, end] = range || []
    const startDay = dayjs(start)
    const endDay = dayjs(end)
    if (!startDay.isValid() || !endDay.isValid()) return []

    const positionNameMap = new Map(
      (attributePreset.positionOptions || [])
        .map((p) => [p?.id !== undefined && p?.id !== null ? String(p.id) : '', String(p?.name || '')])
        .filter(([id, name]) => id && name)
    )

    const roleDefs = []
    const selected = selectedPositionTags.value || []
    if (selected.length) {
      selected.forEach((p, idx) => {
        roleDefs.push({ type: idx + 1, id: String(p.id), name: p.name })
      })
    } else {
      const roleKeySet = new Set()
      ;(list || []).forEach((item) => {
        ;(item?.positions || []).forEach((p) => {
          const id = p?.positionId !== undefined && p?.positionId !== null ? String(p.positionId) : ''
          const name = String(p?.positionName || '') || positionNameMap.get(id) || '岗位'
          const key = id ? `id:${id}` : `name:${name}`
          if (!key || roleKeySet.has(key)) return
          roleKeySet.add(key)
          roleDefs.push({
            type: roleDefs.length + 1,
            id,
            name
          })
        })
      })
    }
    if (roleDefs.length === 0) roleDefs.push({ type: 1, id: '', name: '排班' })
    const roleIdMap = new Map(roleDefs.filter((r) => r.id).map((r) => [String(r.id), r.type]))

    const days = []
    const dayMap = new Map()
    const weekMap = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
    let cursor = startDay
    while (cursor.isSameOrBefore(endDay, 'day')) {
      const key = cursor.format('YYYY-MM-DD')
      const dateTime = cursor.format('MM/DD')
      const label = cursor.isSame(dayjs(), 'day') ? '今日' : weekMap[cursor.day()]
      const day = {
        dateTime,
        label,
        data: roleDefs.map((r) => ({ name: r.name, type: r.type, positionId: r.id, data: [] }))
      }
      days.push(day)
      dayMap.set(key, day)
      cursor = cursor.add(1, 'day')
    }

    ;(list || []).forEach((item) => {
      const dayKey = dayjs(normalizeDateStr(item?.workDay)).format('YYYY-MM-DD')
      const day = dayMap.get(dayKey)
      if (!day) return

      const DAY_MS = 24 * 60 * 60 * 1000
      const startMs = parseDateTime(item?.workDay, item?.startWork)
      let endMs = parseDateTime(item?.workDay, item?.endWork)
      if (endMs <= startMs && !hasDatePart(item?.endWork)) endMs += DAY_MS
      const startDayBase = dayjs(startMs).startOf('day').valueOf()
      const startDayEnd = startDayBase + DAY_MS
      if (!hasDatePart(item?.endWork) && endMs > startDayEnd && endMs - startDayEnd <= 1000) endMs = startDayEnd
      const endText = String(item?.endWork || '').match(/(\d{1,2}:\d{2})/)?.[1] || ''
      const endIsMidnight = !hasDatePart(item?.endWork) && endText === '00:00' && endMs === startDayEnd

      ;(item?.positions || []).forEach((p) => {
        const employees = Array.isArray(p?.employees) ? p.employees : []
        const hasEmployee = employees.some(
          (e) => e?.employeeId !== undefined && e?.employeeId !== null && e?.employeeId !== ''
        )
        if (!hasEmployee) return
        const pid = p?.positionId !== undefined && p?.positionId !== null ? String(p.positionId) : ''
        const roleType =
          roleIdMap.get(pid) ||
          roleDefs.find((r) => r.name === (String(p?.positionName || '') || positionNameMap.get(pid) || ''))?.type
        if (!roleType) return

        const id = `${item?.id || dayKey}_${pid || p?.positionName || 'p'}`
        const scheduleId = item?.id !== undefined && item?.id !== null ? String(item.id) : ''
        const name = `${employees
          .map((e) => e?.employeeName)
          .filter(Boolean)
          .join(',')}`
        const memberId = employees[0]?.employeeId || ''
        const scheduleDuration = Number(item?.scheduleDuration)
        const originalTimeCount = endIsMidnight
          ? Math.max(0, Math.round((startDayEnd - startMs) / (1000 * 60)))
          : Number.isFinite(scheduleDuration) && scheduleDuration > 0
            ? scheduleDuration
            : Math.max(0, Math.round((endMs - startMs) / (1000 * 60)))
        const isCrossDay = endMs - startMs > DAY_MS

        const segStartDay = dayjs(startMs).startOf('day')
        const segEndDay = dayjs(endMs - 1).startOf('day')
        let segCursor = segStartDay

        while (segCursor.isSameOrBefore(segEndDay, 'day')) {
          const segDayKey = segCursor.format('YYYY-MM-DD')
          const segDay = dayMap.get(segDayKey)
          if (segDay) {
            const segRow = segDay.data.find((r) => r.type === roleType)
            if (segRow) {
              const segBase = segCursor.valueOf()
              const segStart = Math.max(startMs, segBase)
              const segEnd = Math.min(endMs, segBase + DAY_MS)
              if (segStart < segEnd) {
                let renderEnd = segEnd
                if (endIsMidnight && segEnd === startDayEnd) {
                  renderEnd = segEnd - 1000
                  if (renderEnd <= segStart) renderEnd = segEnd
                }

                segRow.data.push({
                  id,
                  scheduleId,
                  name,
                  memberId,
                  restDuration: item?.restDuration,
                  startTime: segStart,
                  times: [segStart, renderEnd],
                  displayTimes: [segStart, segEnd],
                  timeCount: Math.max(0, Math.round((segEnd - segStart) / (1000 * 60))),
                  displayTimeCount: endIsMidnight ? originalTimeCount : undefined,
                  isCrossDay,
                  originalTimeCount,
                  crossDayOffset: isCrossDay ? segStart - startMs : undefined,
                  roleType
                })
              }
            }
          }
          segCursor = segCursor.add(1, 'day')
        }
      })
    })
    return days
  }

  const getWeekRange = (baseDateStr, weekOffset = 0) => {
    const raw = baseDateStr ? dayjs(baseDateStr) : dayjs()
    const base = raw.isValid() ? raw : dayjs()
    const day = base.day()
    const diffToMonday = (day + 6) % 7
    const monday = base.subtract(diffToMonday, 'day').add(weekOffset * 7, 'day')
    const sunday = monday.add(6, 'day')
    return [monday.format('YYYY-MM-DD'), sunday.format('YYYY-MM-DD')]
  }

  const getMonthRange = (baseDateStr, monthOffset = 0) => {
    const raw = baseDateStr ? dayjs(baseDateStr) : dayjs()
    const base = raw.isValid() ? raw.add(monthOffset, 'month') : dayjs().add(monthOffset, 'month')
    const start = base.startOf('month')
    const end = base.endOf('month')
    return [start.format('YYYY-MM-DD'), end.format('YYYY-MM-DD')]
  }

  const setThisWeek = () => {
    activeRange.value = 'thisWeek'
    dateRange.value = getWeekRange(currentDate.value, 0)
  }

  const setNextWeek = () => {
    activeRange.value = 'nextWeek'
    dateRange.value = getWeekRange(currentDate.value, 1)
  }

  const setThisMonth = () => {
    activeRange.value = 'thisMonth'
    dateRange.value = getMonthRange(currentDate.value, 0)
  }

  const setNextMonth = () => {
    activeRange.value = 'nextMonth'
    dateRange.value = getMonthRange(currentDate.value, 1)
  }

  const fetchEmployeeOptions = async (keyword) => {
    const kw = String(keyword || '')
    const positionId =
      addEmployeeForm.positionId !== undefined &&
      addEmployeeForm.positionId !== null &&
      String(addEmployeeForm.positionId)
        ? String(addEmployeeForm.positionId)
        : ''
    if (!positionId) {
      employeeOptions.value = []
      return
    }
    const res = await apiModule.employee.options({
      keyword: kw,
      limit: 200,
      positionIds: positionId ? [positionId] : undefined
    })
    employeeOptions.value = res.data || []
  }

  watch(
    () => [addEmployeeDialogVisible.value, addEmployeeForm.positionId],
    ([visible]) => {
      if (!visible) return
      addEmployeeForm.employeeId = undefined
      fetchEmployeeOptions('')
    }
  )

  const normalizeNumberList = (raw, min = 0) => {
    const list = (Array.isArray(raw) ? raw : []).map((x) => Number(x)).filter((x) => Number.isFinite(x) && x >= min)
    const uniq = Array.from(new Set(list))
    uniq.sort((a, b) => a - b)
    return uniq
  }

  const normalizeIdList = (raw) => {
    const list = (Array.isArray(raw) ? raw : [])
      .map((x) => (x === undefined || x === null ? '' : String(x)))
      .filter((x) => x)
    return Array.from(new Set(list))
  }

  const getTimeAxisKey = (id) => `live_room_schedule_time_axis_unit_${id}`
  const getCopySourceKey = (id) => `live_room_schedule_copy_source_${id}`

  const loadTimeAxisUnit = () => {
    const id = liveRoomId.value
    if (!id) return
    try {
      const v = Number(window?.localStorage?.getItem(getTimeAxisKey(id)) || 0)
      if ([30, 60, 120].includes(v)) timeAxisUnit.value = v
    } catch (e) {
      void e
    }
  }

  const saveTimeAxisUnit = () => {
    const id = liveRoomId.value
    if (!id) return
    try {
      window?.localStorage?.setItem(getTimeAxisKey(id), String(timeAxisUnit.value))
    } catch (e) {
      void e
    }
  }

  const loadCopySource = () => {
    const id = liveRoomId.value
    if (!id) return
    try {
      const raw = window?.localStorage?.getItem(getCopySourceKey(id)) || ''
      const parsed = raw ? JSON.parse(raw) : null
      copySourceId.value = parsed?.id !== undefined && parsed?.id !== null ? String(parsed.id) : ''
      copySourceName.value = parsed?.name ? String(parsed.name) : ''
      copySourceDraftId.value = copySourceId.value
      copySourceDraftName.value = copySourceName.value
    } catch (e) {
      void e
      copySourceId.value = ''
      copySourceName.value = ''
      copySourceDraftId.value = ''
      copySourceDraftName.value = ''
    }
  }

  const saveCopySource = (payload) => {
    const id = liveRoomId.value
    if (!id) return
    const sourceId = payload?.id !== undefined && payload?.id !== null ? String(payload.id) : ''
    const sourceName = payload?.name ? String(payload.name) : ''
    try {
      if (!sourceId && !sourceName) {
        window?.localStorage?.removeItem(getCopySourceKey(id))
        return
      }
      window?.localStorage?.setItem(getCopySourceKey(id), JSON.stringify({ id: sourceId, name: sourceName }))
    } catch (e) {
      void e
    }
  }

  const selectedPositionTags = computed(() => {
    const idSet = new Set(
      (Array.isArray(attributeForm.positionIds) ? attributeForm.positionIds : []).map((x) => String(x))
    )
    return (attributePreset.positionOptions || []).filter((p) => idSet.has(String(p.id)))
  })

  const liveRoomPositionOptions = computed(() => {
    return Array.isArray(selectedPositionTags.value) ? selectedPositionTags.value : []
  })

  const mapOptions = (data) => {
    if (!Array.isArray(data)) return []
    return data
      .filter((item) => item !== null && item !== undefined)
      .map((item) => {
        const key = item.key !== undefined ? item.key : item.value !== undefined ? item.value : item.id
        const label = item.label !== undefined ? item.label : item.name
        return { ...item, key, label }
      })
      .filter((item) => item.key !== undefined && item.key !== null)
  }

  const fetchAllPositionOptions = async () => {
    if (allPositionLoaded.value) return

    const normalizePositionList = (raw) => {
      const list = (Array.isArray(raw) ? raw : [])
        .map((x) => ({
          id: x?.id !== undefined && x?.id !== null ? String(x.id) : '',
          name: String(x?.name || '')
        }))
        .filter((x) => x.id && x.name)
      const idMap = new Map()
      list.forEach((i) => {
        if (!idMap.has(i.id)) idMap.set(i.id, i.name)
      })
      return Array.from(idMap.entries()).map(([id, name]) => ({ id, name }))
    }

    let positions = []
    try {
      const res = await apiModule.position.options({ limit: 2000 })
      const opts = mapOptions(res.data || [])
      positions = normalizePositionList(opts.map((i) => ({ id: i.key, name: i.label })))
    } catch (e) {
      positions = []
    }

    if (positions.length < 10) {
      const merged = []
      let page = 1
      const limit = 200
      let total = 0
      for (let i = 0; i < 20; i++) {
        const res = await apiModule.position.list({ page, limit })
        const list = res.data?.list || []
        total = Number(res.data?.totalCount || 0) || total
        merged.push(...list)
        if (!Array.isArray(list) || list.length === 0) break
        if (total > 0 && merged.length >= total) break
        page += 1
      }
      positions = normalizePositionList(merged)
    }

    attributePreset.positionOptions = positions
    allPositionLoaded.value = true
  }

  const reconcilePositionIds = (rawPositionOptions, currentIds) => {
    const options = Array.isArray(attributePreset.positionOptions) ? attributePreset.positionOptions : []
    if (!options.length) return normalizeIdList(currentIds || []).slice(0, 3)

    const normalizeName = (s) => String(s || '').trim()
    const idByName = new Map()
    options.forEach((p) => {
      const name = normalizeName(p?.name)
      const id = p?.id !== undefined && p?.id !== null ? String(p.id) : ''
      if (!name || !id) return
      if (!idByName.has(name)) idByName.set(name, id)
    })

    const optionIdSet = new Set(options.map((p) => String(p?.id)))
    const keepIds = normalizeIdList(currentIds || []).filter((id) => optionIdSet.has(id))

    const namesFromRes = (Array.isArray(rawPositionOptions) ? rawPositionOptions : [])
      .map((i) => {
        if (i && typeof i === 'object') return i.name !== undefined ? i.name : i.label
        return ''
      })
      .map((s) => normalizeName(s))
      .filter((s) => s)

    const idsFromNames = namesFromRes.map((n) => idByName.get(n)).filter((x) => x)
    return normalizeIdList([...keepIds, ...idsFromNames]).slice(0, 3)
  }

  const applyAttributeData = async (data) => {
    const startPlan = data?.startPlan || ''
    const endPlan = data?.endPlan || ''
    attributeForm.startPlan = startPlan
    attributeForm.endPlan = endPlan
    attributeForm.planRange = [startPlan, endPlan]
    attributeForm.shiftOptions = normalizeNumberList(data?.shiftOptions || [], 1)
    attributeForm.restOptions = normalizeNumberList(data?.restOptions || [], 0)
    const rawPositionOptions = Array.isArray(data?.positionOptions) ? data.positionOptions : []
    const idsFromRes = normalizeIdList(
      rawPositionOptions.map((i) => {
        if (i && typeof i === 'object')
          return i.id !== undefined
            ? i.id
            : i.positionId !== undefined
              ? i.positionId
              : i.key !== undefined
                ? i.key
                : i.value
        return i
      })
    )
    attributeForm.positionIds = idsFromRes.slice(0, 3)
    await fetchAllPositionOptions()
    attributeForm.positionIds = reconcilePositionIds(rawPositionOptions, attributeForm.positionIds)

    attributePreset.shiftOptions = attributeForm.shiftOptions
    attributePreset.restOptions = attributeForm.restOptions
  }

  const loadAttribute = async () => {
    const res = await apiModule.roomSchedule.getAttribute({ id: liveRoomId.value })
    await applyAttributeData(res.data || {})
    loadTimeAxisUnit()
  }

  const removeShiftOption = (val) => {
    const n = Number(val)
    attributeForm.shiftOptions = normalizeNumberList(
      (attributeForm.shiftOptions || []).filter((x) => Number(x) !== n),
      1
    )
  }

  const addShiftOption = () => {
    const n = Number(shiftOptionInput.value)
    if (!Number.isFinite(n) || n <= 0) return
    attributeForm.shiftOptions = normalizeNumberList([...(attributeForm.shiftOptions || []), n], 1)
  }

  const removeRestOption = (val) => {
    const n = Number(val)
    attributeForm.restOptions = normalizeNumberList(
      (attributeForm.restOptions || []).filter((x) => Number(x) !== n),
      0
    )
  }

  const addRestOption = () => {
    const n = Number(restOptionInput.value)
    if (!Number.isFinite(n) || n < 0) return
    attributeForm.restOptions = normalizeNumberList([...(attributeForm.restOptions || []), n], 0)
  }

  const openPositionDialog = () => {
    fetchAllPositionOptions()
    positionDialogValue.value = [...(attributeForm.positionIds || [])]
    positionDialogVisible.value = true
  }

  const confirmPositionDialog = () => {
    attributeForm.positionIds = normalizeIdList(positionDialogValue.value || []).slice(0, 3)
    positionDialogVisible.value = false
  }

  const removeSelectedPosition = (id) => {
    const key = String(id)
    attributeForm.positionIds = normalizeIdList(
      (attributeForm.positionIds || []).filter((x) => String(x) !== key)
    ).slice(0, 3)
  }

  const hasBaseAttribute = () => {
    const [s, e] = attributeForm.planRange || []
    return Boolean(
      s &&
      e &&
      (attributeForm.shiftOptions || []).length &&
      (attributeForm.restOptions || []).length &&
      (attributeForm.positionIds || []).length
    )
  }

  const openAttributeDialog = (next) => {
    fetchAllPositionOptions()
    loadCopySource()
    pendingOpen.value = next || null
    attributeDialogVisible.value = true
  }

  const openCopyAttributeDialog = async () => {
    copyAttributeDialogVisible.value = true
    copySourceDraftId.value = copySourceId.value
    copySourceDraftName.value = copySourceName.value
    await fetchCopyRoomList(true)
  }

  const clearCopySource = () => {
    copySourceId.value = ''
    copySourceName.value = ''
    copySourceDraftId.value = ''
    copySourceDraftName.value = ''
    saveCopySource({ id: '', name: '' })
  }

  const fetchCopyRoomList = async (reset = false) => {
    if (reset) copyQuery.page = 1
    const res = await apiModule.liveRoom.list({
      page: copyQuery.page,
      limit: copyQuery.limit,
      anchorName: copyQuery.keyword || undefined
    })
    const rawList = res.data?.list || []
    copyTotal.value = res.data?.totalCount || 0

    const enriched = await Promise.all(
      rawList.map(async (r) => {
        let attr = {}
        try {
          const ar = await apiModule.roomSchedule.getAttribute({ id: r.id })
          attr = ar.data || {}
        } catch (e) {
          void e
        }
        const planText = attr.startPlan && attr.endPlan ? `${attr.startPlan}~${attr.endPlan}` : '-'
        const shiftText =
          Array.isArray(attr.shiftOptions) && attr.shiftOptions.length ? attr.shiftOptions.join('、') : '-'
        const restText = Array.isArray(attr.restOptions) && attr.restOptions.length ? attr.restOptions.join('、') : '-'
        return {
          id: r.id,
          name: r.anchorName,
          image: r.anchorAvatar,
          planText,
          shiftText,
          restText,
          _attr: attr
        }
      })
    )

    const [ps, pe] = copyQuery.planRange || []
    copyRoomList.value = enriched.filter((x) => {
      if (!ps || !pe) return true
      return x?._attr?.startPlan === ps && x?._attr?.endPlan === pe
    })
  }

  const handleCopySelected = (row) => {
    copySourceDraftId.value = row?.id || ''
    copySourceDraftName.value = row?.name || ''
  }

  const confirmCopyAttribute = (row) => {
    const picked = row || (copyRoomList.value || []).find((x) => String(x.id) === String(copySourceDraftId.value))
    if (!picked) return
    copySourceId.value = picked?.id !== undefined && picked?.id !== null ? String(picked.id) : ''
    copySourceName.value = picked?.name || ''
    copySourceDraftId.value = copySourceId.value
    copySourceDraftName.value = copySourceName.value
    saveCopySource({ id: copySourceId.value, name: copySourceName.value })
    const data = picked?._attr || {}
    applyAttributeData(data)
    const copyUnit = Number(window?.localStorage?.getItem(getTimeAxisKey(picked?.id)) || 0)
    if ([30, 60, 120].includes(copyUnit)) timeAxisUnit.value = copyUnit
  }

  const submitAttribute = async () => {
    attributeForm.planRange = Array.isArray(attributeForm.planRange) ? attributeForm.planRange : ['', '']
    attributeForm.startPlan = attributeForm.planRange?.[0] || ''
    attributeForm.endPlan = attributeForm.planRange?.[1] || ''
    attributeForm.shiftOptions = normalizeNumberList(attributeForm.shiftOptions || [], 1)
    attributeForm.restOptions = normalizeNumberList(attributeForm.restOptions || [], 0)
    attributeForm.positionIds = normalizeIdList(attributeForm.positionIds || []).slice(0, 3)

    await apiModule.roomSchedule.setAttribute({
      id: liveRoomId.value,
      startPlan: attributeForm.startPlan,
      endPlan: attributeForm.endPlan,
      shiftOptions: attributeForm.shiftOptions || [],
      restOptions: attributeForm.restOptions || [],
      positionOptions: normalizeIdList(attributeForm.positionIds || [])
    })
    saveTimeAxisUnit()
    ElMessage.success('保存成功')
    attributeDialogVisible.value = false
    await loadAttribute()
    await refreshAll()

    const next = pendingOpen.value
    pendingOpen.value = null
    if (next?.type === 'add') openAddDialog(next.date)
  }

  const downloadScheduleTemplate = async () => {
    await loadAttribute()
    if (!hasBaseAttribute()) {
      openAttributeDialog()
      ElMessageBox.alert('请先完成排班配置后再进行排班操作', '提示', {
        type: 'warning',
        confirmButtonText: '知道了'
      })
      return
    }
    try {
      const blob = await apiModule.roomSchedule.downloadTemplate({ liveRoomId: liveRoomId.value })
      const url = window.URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = '直播间排班导入模板.xlsx'
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      window.URL.revokeObjectURL(url)
    } catch (e) {
      ElMessage.error('下载模板失败，请确认已完排班配置')
    }
  }

  const openImportDialog = async () => {
    await loadAttribute()
    if (!hasBaseAttribute()) {
      openAttributeDialog()
      ElMessageBox.alert('请先完成排班配置后再进行排班操作', '提示', {
        type: 'warning',
        confirmButtonText: '知道了'
      })
      return
    }
    importDialogVisible.value = true
  }

  const getAnchorPositionId = () => {
    const selected = selectedPositionTags.value || []
    const hit = selected.find((p) => String(p.name || '').includes('主播'))
    return String(hit?.id || selected[0]?.id || attributeForm.positionIds?.[0] || '')
  }

  const anchorPositionId = computed(() => getAnchorPositionId())

  const handleScheduleSaved = async () => {
    await refreshAll()
  }

  const handleRemoveScheduleBlock = async ({ block }) => {
    const scheduleId = block?.scheduleId || block?.id
    if (!scheduleId) {
      ElMessage.error('缺少排班ID')
      return
    }
    await apiModule.roomSchedule.del({ id: scheduleId, liveRoomId: liveRoomId.value })
    ElMessage.success('删除成功')
    await refreshAll()
  }

  const resolveQuickAddDate = () => {
    if (route.query.day) return String(route.query.day)
    if (route.query.dayType === 'tomorrow') return dayjs().add(1, 'day').format('YYYY-MM-DD')
    return dayjs().format('YYYY-MM-DD')
  }

  const openAddDialog = async (dateStr) => {
    const d = typeof dateStr === 'string' && dateStr ? dateStr : resolveQuickAddDate()
    await loadAttribute()
    if (!hasBaseAttribute()) {
      openAttributeDialog({ type: 'add', date: d })
      return
    }
    scheduleRef.value?.openAdd?.(d)
  }

  const openAddEmployeeDialog = (row) => {
    const positions = Array.isArray(row?.positions) ? row.positions : []
    const directPositionId =
      row?.positionId !== undefined && row?.positionId !== null && String(row.positionId) ? String(row.positionId) : ''
    const singlePositionId =
      positions.length === 1 &&
      positions[0]?.positionId !== undefined &&
      positions[0]?.positionId !== null &&
      String(positions[0].positionId)
        ? String(positions[0].positionId)
        : ''
    const pickedPositionId = directPositionId || singlePositionId
    addEmployeeForm.scheduleId = row.id
    addEmployeeForm.employeeId = undefined
    addEmployeeForm.positionId = pickedPositionId || undefined
    addEmployeeForm.positionLocked = !!pickedPositionId
    addEmployeeDialogVisible.value = true
  }

  const submitAddEmployee = async () => {
    await apiModule.roomSchedule.addEmployee({
      scheduleId: addEmployeeForm.scheduleId,
      liveRoomId: liveRoomId.value,
      employeeId: addEmployeeForm.employeeId,
      positionId: addEmployeeForm.positionId
    })
    ElMessage.success('添加成功')
    addEmployeeDialogVisible.value = false
    await refreshAll()
  }

  const handleRemoveEmployee = async (scheduleRow, employee) => {
    await apiModule.roomSchedule.removeEmployee({
      scheduleId: scheduleRow.id,
      liveRoomId: liveRoomId.value,
      employeeId: employee.employeeId
    })
    ElMessage.success('移除成功')
    await refreshAll()
  }

  const handleDelete = async (row) => {
    await apiModule.roomSchedule.del({ id: row.id, liveRoomId: liveRoomId.value })
    ElMessage.success('删除成功')
    await refreshAll()
  }

  const scheduleTableColumns = [
    { label: '日期', prop: 'workDay', width: 120, align: 'center' },
    { label: '上播', prop: 'startWork', width: 120, align: 'center' },
    { label: '下播', prop: 'endWork', width: 120, align: 'center' },
    { label: '班次时长', prop: 'scheduleDuration', width: 120, align: 'center' },
    { label: '休息时长', prop: 'restDuration', width: 120, align: 'center' },
    { label: '岗位/人员', prop: 'positions', minWidth: 260, slotName: 'positions', align: 'center' },
    { label: '操作', prop: 'action', width: 220, fixed: 'right', slotName: 'action', align: 'center' }
  ]

  const tableApi = {
    list: async (params) => {
      const [startDate, endDate] = dateRange.value || []
      const res = await apiModule.roomSchedule.page({
        liveRoomId: liveRoomId.value,
        startDate,
        endDate,
        page: params?.page || 1,
        limit: params?.pageSize || 10
      })
      return { list: res.data?.list || [], total: res.data?.totalCount || 0 }
    }
  }

  const loadLiveRoomInfo = async () => {
    const res = await apiModule.liveRoom.detail({ id: liveRoomId.value })
    const data = res.data || {}
    liveRoomInfo.id = data.id || liveRoomInfo.id
    liveRoomInfo.name = data.anchorName || liveRoomInfo.name
    liveRoomInfo.image = data.anchorAvatar || ''
    liveRoomInfo.companyId =
      data.companyId !== undefined && data.companyId !== null ? String(data.companyId) : liveRoomInfo.companyId
    liveRoomInfo.deptId = data.deptId !== undefined && data.deptId !== null ? String(data.deptId) : liveRoomInfo.deptId
    liveRoomInfo.teamId = data.teamId !== undefined && data.teamId !== null ? String(data.teamId) : liveRoomInfo.teamId
    liveRoomInfo.organization = [data.companyName, data.deptName, data.teamName].filter(Boolean).join('-') || '-'
    liveRoomInfo.manager =
      (data.managerUserInfos || [])
        .map((i) => i.name)
        .filter(Boolean)
        .join('、') || '-'
  }

  const loadRangeSchedule = async () => {
    const [startDate, endDate] = dateRange.value || []
    const res = await apiModule.roomSchedule.listRangeSpitDay({ liveRoomId: liveRoomId.value, startDate, endDate })

    rawRangeList.value = res.data || []
    scheduleData.value = buildScheduleData(rawRangeList.value, dateRange.value)
  }

  const refreshAll = async () => {
    await loadRangeSchedule()
    curdRef.value?.getData()
  }

  watch(currentDate, () => {
    if (!currentDate.value) {
      setThisWeek()
      return
    }
    activeRange.value = ''
    dateRange.value = [currentDate.value, currentDate.value]
  })

  watch(
    () => dateRange.value,
    () => {
      if (isInitializing.value) return
      refreshAll()
    },
    { deep: true }
  )

  onMounted(async () => {
    isInitializing.value = true
    await loadLiveRoomInfo()
    await fetchAllPositionOptions()
    await loadAttribute()

    const range = route.query.activeRange
    if (range === 'nextWeek') {
      setNextWeek()
    } else if (range === 'thisMonth') {
      setThisMonth()
    } else if (range === 'nextMonth') {
      setNextMonth()
    } else {
      setThisWeek()
    }

    await refreshAll()
    isInitializing.value = false
    if (route.query.openAdd) {
      const d = resolveQuickAddDate()
      if (!hasBaseAttribute()) openAttributeDialog({ type: 'add', date: d })
    } else if (route.query.openConfig) {
      openAttributeDialog()
    }
  })
</script>

<style lang="scss" scoped>
  .live-room-schedule-detail {
    display: flex;
    flex-direction: column;
    gap: 24px;

    .schedule-controls {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 8px;

      .title {
        font-size: 16px;
        font-weight: 600;
        color: #303133;
      }

      .actions {
        display: flex;
        gap: 16px;
      }
    }

    .schedule-wrapper {
      background: #fff;
      padding: 20px;
      border-radius: 8px;
      box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
    }

    .schedule-table-wrapper {
      min-height: 300px;
      background: #fff;
      padding: 20px 20px 0 20px;
      border-radius: 8px;
      box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
    }

    .table-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: 12px;
    }

    .table-title {
      font-size: 16px;
      font-weight: 600;
      color: #303133;
    }

    .positions-cell {
      display: flex;
      flex-direction: column;
      gap: 10px;
    }

    .position-block {
      display: flex;
      gap: 10px;
    }

    .position-name {
      width: 72px;
      font-weight: 500;
      color: #303133;
    }

    .position-emps {
      display: flex;
      flex-wrap: wrap;
      gap: 10px;
      flex: 1;
    }

    .operate-btn {
      width: 84px;
      border-radius: 56px;
    }

    .emp-item {
      display: flex;
      align-items: center;
      gap: 6px;
    }

    .dialog-employees {
      display: flex;
      flex-direction: column;
      gap: 12px;
      width: 100%;
    }

    .dialog-emp-row {
      display: flex;
      gap: 10px;
      align-items: center;
    }

    .dialog-batch {
      display: flex;
      gap: 12px;
      align-items: center;
      flex-wrap: wrap;
      width: 100%;
    }

    .attribute-panel {
      display: flex;
      flex-direction: column;
      gap: 16px;
    }

    .attr-section {
      display: flex;
      flex-direction: column;
      gap: 10px;
    }

    .attr-title {
      font-size: 14px;
      font-weight: 700;
      color: #303133;
    }

    .required {
      color: #f56c6c;
      margin-right: 4px;
    }

    .attr-card {
      background: #f7f8fa;
      border-radius: 8px;
      padding: 16px;
    }

    .attr-row {
      display: flex;
      align-items: center;
      gap: 12px;
      margin-bottom: 12px;

      &:last-child {
        margin-bottom: 0;
      }
    }

    .attr-label {
      width: 96px;
      flex-shrink: 0;
      color: #606266;
      font-size: 13px;
    }

    .attr-input {
      flex: 1;
    }

    .attr-option-line {
      display: flex;
      flex: 1;
      flex-wrap: wrap;
      gap: 10px;
      align-items: center;
    }

    .attr-tag {
      border-radius: 6px;
    }

    .attr-num {
      width: 120px;
    }

    .attr-hint {
      color: #909399;
      font-size: 12px;
    }

    .copy-filters {
      display: flex;
      align-items: center;
      gap: 10px;
      margin-bottom: 12px;
    }

    .copy-room-cell {
      display: flex;
      align-items: center;
      gap: 10px;

      .name {
        color: #303133;
        font-weight: 500;
      }
    }

    .copy-pagination {
      display: flex;
      justify-content: flex-end;
      margin-top: 12px;
    }

    .position-tip {
      color: #909399;
      font-size: 12px;
      margin-bottom: 12px;
    }

    .position-grid {
      display: grid;
      grid-template-columns: repeat(3, minmax(0, 1fr));
      gap: 10px 16px;
    }
  }
</style>
