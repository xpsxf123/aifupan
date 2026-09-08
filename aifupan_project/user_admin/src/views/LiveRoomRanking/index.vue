<template>
  <div class="page-container">
    <div class="curd-wrapper" v-empty="emptyConfig">
      <Curd
        ref="curdRef"
        :table-columns="tableColumns"
        :form-config="formConfig"
        :api="api"
        :search-config="searchConfig"
        title="直播间"
        show-total
        totalUnit="个"
        :layoutConfig="{ search: { isCard: true } }"
        :show-add="false"
        :show-operation="false"
        :show-toolbar-right="false"
        :action-config="{ view: false, edit: false, del: false }"
        @load="handleLoad"
      >
        <template #liveRoom="{ row }">
          <div class="live-room-cell">
            <div class="avtar-container">
              <el-avatar :size="32" :src="row.image || defaultImg" />
            </div>
            <span class="name">{{ row.name }}</span>
          </div>
        </template>

        <template #platform="{ row }">
          <div class="platform-tag" :class="getPlatformClass(row.platform)"> {{ row.platform }} </div>
        </template>
        <template #todaySchedule="{ row }">
          <div v-if="row.todayItems?.length" class="schedule-list">
            <div v-for="it in row.todayItems" :key="it.key" class="schedule-item">
              <span class="dot" style="background: #52c41a"></span>
              <span class="time">{{ it.timeRange }}</span>
              <span class="duration">{{ it.durationText }}</span>
            </div>
          </div>
          <el-button v-else v-auth="'room:schedule:add'" type="primary" link @click="handleAddSchedule('today', row)">
            添加排班
          </el-button>
        </template>

        <template #tomorrowSchedule="{ row }">
          <div v-if="row.tomorrowItems?.length" class="schedule-list">
            <div v-for="it in row.tomorrowItems" :key="it.key" class="schedule-item">
              <span class="dot" style="background: #1890ff"></span>
              <span class="time">{{ it.timeRange }}</span>
              <span class="duration">{{ it.durationText }}</span>
            </div>
          </div>
          <el-button
            v-else
            v-auth="'room:schedule:add'"
            type="primary"
            link
            @click="handleAddSchedule('tomorrow', row)"
          >
            添加排班
          </el-button>
        </template>

        <template #thisWeekSchedule="{ row }">
          <el-button class="custom-view" type="primary" link @click="handleViewSchedule(row, 'thisWeek')"
            >查看排班</el-button
          >
        </template>

        <template #nextWeekSchedule="{ row }">
          <el-button class="custom-view" type="primary" link @click="handleViewSchedule(row, 'nextWeek')"
            >查看排班</el-button
          >
        </template>

        <template #thisMonthSchedule="{ row }">
          <el-button class="custom-view" type="primary" link @click="handleViewSchedule(row, 'thisMonth')"
            >查看排班</el-button
          >
        </template>

        <template #scheduleForm="{ form }">
          <div class="section-title"> <span class="required-mark">*</span>班次信息 </div>
          <div class="form-section">
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="班次日期" prop="workDay">
                  <el-input v-model="form.workDay" disabled />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="直播间" prop="liveRoomId">
                  <el-select v-model="form.liveRoomId" disabled style="width: 100%">
                    <el-option :label="form.liveRoomName" :value="form.liveRoomId" />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>

            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="班次时间" prop="timeRange">
                  <el-time-picker
                    v-model="form.timeRange"
                    is-range
                    range-separator="~"
                    start-placeholder="上播"
                    end-placeholder="下播"
                    format="HH:mm"
                    value-format="HH:mm"
                    style="width: 100%"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="备注" prop="remark">
                  <el-input v-model="form.remark" placeholder="选填" />
                </el-form-item>
              </el-col>
            </el-row>

            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="班次时长" prop="scheduleDuration">
                  <el-select v-model="form.scheduleDuration" placeholder="请选择" style="width: 100%">
                    <el-option v-for="i in scheduleAttribute.shiftOptions" :key="i" :label="`${i} 分钟`" :value="i" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="休息时长" prop="restDuration">
                  <el-select v-model="form.restDuration" placeholder="请选择" style="width: 100%">
                    <el-option v-for="i in scheduleAttribute.restOptions" :key="i" :label="`${i} 分钟`" :value="i" />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>
          </div>

          <div class="section-title" style="margin-top: 20px"> 排班人员 </div>
          <div class="form-section">
            <div v-for="(emp, idx) in form.employees" :key="idx" class="employee-row">
              <el-row :gutter="12">
                <el-col :span="10">
                  <el-form-item label="岗位" :prop="`employees.${idx}.positionId`">
                    <el-select v-model="emp.positionId" placeholder="请选择" style="width: 100%">
                      <el-option
                        v-for="p in scheduleAttribute.positionOptions"
                        :key="p.id"
                        :label="p.name"
                        :value="p.id"
                      />
                    </el-select>
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="人员" :prop="`employees.${idx}.employeeId`">
                    <el-select
                      v-model="emp.employeeId"
                      filterable
                      remote
                      clearable
                      :remote-method="fetchEmployeeOptions"
                      placeholder="输入姓名/手机搜索"
                      style="width: 100%"
                    >
                      <el-option v-for="o in employeeOptions" :key="o.key" :label="o.label" :value="o.key" />
                    </el-select>
                  </el-form-item>
                </el-col>
                <el-col :span="2" style="display: flex; align-items: center; justify-content: flex-end">
                  <el-button v-auth="'room:schedule:update'" type="danger" link @click="form.employees.splice(idx, 1)"
                    >移除</el-button
                  >
                </el-col>
              </el-row>
            </div>
            <el-button v-auth="'room:schedule:update'" type="primary" plain @click="handleAddEmployeeRow(form)"
              >添加人员</el-button
            >
          </div>

          <div class="section-title" style="margin-top: 20px"> 批量排班 </div>
          <div class="form-section">
            <el-form-item label="班次循环" prop="cycleDays">
              <el-checkbox-group v-model="form.cycleDays">
                <el-checkbox :label="1">周一</el-checkbox>
                <el-checkbox :label="2">周二</el-checkbox>
                <el-checkbox :label="3">周三</el-checkbox>
                <el-checkbox :label="4">周四</el-checkbox>
                <el-checkbox :label="5">周五</el-checkbox>
                <el-checkbox :label="6">周六</el-checkbox>
                <el-checkbox :label="7">周日</el-checkbox>
              </el-checkbox-group>
            </el-form-item>
            <el-form-item label="结束日期" prop="endDate">
              <el-date-picker v-model="form.endDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </div>
        </template>
      </Curd>
    </div>

    <ScheduleAttributeDrawer
      v-model="attributeDrawerVisible"
      title="排班配置"
      confirm-text="确定"
      :confirm-permission-code="'room:schedule:update'"
      :room-name="activeRoom?.name || '-'"
      v-model:planRange="attributeForm.planRange"
      :shift-options="attributeForm.shiftOptions"
      :rest-options="attributeForm.restOptions"
      v-model:shiftOptionInput="shiftOptionInput"
      v-model:restOptionInput="restOptionInput"
      v-model:timeAxisUnit="timeAxisUnit"
      :selected-position-tags="selectedPositionTags"
      :copy-source-name="copySourceName"
      :copy-button-icon="Plus"
      :label-width="70"
      @open-copy="openCopyAttributeDialog"
      @clear-copy="clearCopySource"
      @add-shift-option="addShiftOption"
      @remove-shift-option="removeShiftOption"
      @add-rest-option="addRestOption"
      @remove-rest-option="removeRestOption"
      @open-position="openPositionDialog"
      @remove-position="removeSelectedPosition"
      @confirm="submitAttribute"
      @cancel="attributeDrawerVisible = false"
    />

    <ScheduleShiftDrawer
      ref="addShiftDrawerRef"
      v-model="addDrawerVisible"
      title="添加排班"
      confirm-text="确定"
      :confirm-permission-code="'room:schedule:add'"
      mode="add"
      :initial="addShiftInitial"
      :duration-options="attributeForm.shiftOptions"
      :rest-options="attributeForm.restOptions"
      @pick-member="handlePickMember"
      @confirm="submitAdd"
    />

    <ScheduleCopyAttributeDrawer
      v-model="copyAttributeDialogVisible"
      :confirm-permission-code="'room:schedule:update'"
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

    <ScheduleMemberDrawer
      v-model="memberDrawerVisible"
      :selected-id="memberSelectedId"
      v-model:keyword="memberQuery.keyword"
      v-model:deptId="memberQuery.deptId"
      v-model:page="memberQuery.page"
      v-model:limit="memberQuery.limit"
      :dept-options="memberDeptOptions"
      :list="memberList"
      :total="memberTotal"
      :loading="memberLoading"
      :confirm-permission-code="'room:schedule:update'"
      @search="fetchMemberList(true)"
      @page-change="fetchMemberList()"
      @confirm="confirmMember"
    />

    <SchedulePositionDialog
      v-model="positionDialogVisible"
      v-model:selected-ids="positionDialogValue"
      :options="attributePreset.positionOptions"
      :max="3"
      :confirm-permission-code="'room:schedule:update'"
      @confirm="confirmPositionDialog"
      @cancel="positionDialogVisible = false"
    />
  </div>
</template>

<script setup>
  /**
   * @file index.vue
   * @description 直播间排班页面
   */
  import { computed, onMounted, reactive, ref, watch } from 'vue'
  import dayjs from 'dayjs'
  import { useRouter } from 'vue-router'
  import { ElMessage } from 'element-plus'
  import Curd from '@/components/Curd/index.vue'
  import ScheduleShiftDrawer from '@/components/ScheduleShiftDrawer/index.vue'
  import ScheduleAttributeDrawer from '@/components/ScheduleAttributeDrawer/index.vue'
  import ScheduleCopyAttributeDrawer from '@/components/ScheduleCopyAttributeDrawer/index.vue'
  import ScheduleMemberDrawer from '@/components/ScheduleMemberDrawer/index.vue'
  import SchedulePositionDialog from '@/components/SchedulePositionDialog/index.vue'
  import apiModule from '@/http/api'
  import { Plus } from '@element-plus/icons-vue'
  import { tableColumns, formConfig } from './constants'
  import defaultImg from '@/assets/images/funnel/defaultAvatar.png'

  const router = useRouter()
  const curdRef = ref(null)
  const firstLoaded = ref(false)
  const employeeOptions = ref([])
  const scheduleAttribute = reactive({
    startPlan: '',
    endPlan: '',
    shiftOptions: [],
    restOptions: [],
    positionOptions: []
  })
  const activeRoom = ref(null)

  const attributeDrawerVisible = ref(false)
  const addDrawerVisible = ref(false)
  const addShiftDrawerRef = ref(null)
  const addShiftInitial = ref({})

  const attributePreset = reactive({ shiftOptions: [], restOptions: [], positionOptions: [] })
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

  const pendingAddDate = ref('')
  const allPositionLoaded = ref(false)

  const copyAttributeDialogVisible = ref(false)
  const copySourceId = ref('')
  const copySourceName = ref('')
  const copyQuery = reactive({ planRange: ['', ''], keyword: '', page: 1, limit: 10 })
  const copyRoomList = ref([])
  const copyTotal = ref(0)

  const positionDialogVisible = ref(false)
  const positionDialogValue = ref([])
  const companyOptions = ref([])
  const deptOptions = ref([])
  const teamOptions = ref([])
  const memberDrawerVisible = ref(false)
  const memberDeptOptions = ref([])
  const memberTeamOptions = ref([])
  const memberList = ref([])
  const memberTotal = ref(0)
  const memberLoading = ref(false)
  const memberPickIndex = ref(-1)
  const memberSelectedId = ref('')

  const memberQuery = reactive({
    keyword: '',
    companyId: '',
    deptId: '',
    teamId: '',
    page: 1,
    limit: 10
  })
  const hasCompany = computed(() => (companyOptions.value || []).length > 0)

  const emptyConfig = reactive({
    visible: false,
    title: '',
    description: '',
    blur: 2,
    buttons: []
  })

  const refreshEmptyConfig = () => {
    if (hasCompany.value) {
      emptyConfig.title = '暂无直播间'
      emptyConfig.description = '请先创建直播间后，再为直播间配置排班。'
      emptyConfig.buttons = [
        {
          text: '去直播间管理',
          type: 'primary',
          round: true,
          click: () => router.push('/department-staff/live-room')
        },
        {
          text: '去看直播间业绩',
          type: 'primary',
          plain: true,
          round: true,
          click: () => router.push('/live-room-performance/index')
        }
      ]
      return
    }

    emptyConfig.title = '暂无子公司'
    emptyConfig.description = '创建直播间前，请先添加子公司。'
    emptyConfig.buttons = [
      {
        text: '去子公司管理',
        type: 'primary',
        round: true,
        click: () => router.push('/department-staff/subsidiary')
      },
      {
        text: '去直播间管理',
        type: 'primary',
        plain: true,
        round: true,
        disabled: true,
        click: () => router.push('/department-staff/live-room')
      }
    ]
  }

  const isDefaultSearch = () => {
    const params = curdRef.value?.searchParams || {}
    return Object.keys(params).every((k) => {
      const v = params[k]
      if (Array.isArray(v)) return v.length === 0
      return v === undefined || v === null || v === ''
    })
  }

  const handleLoad = (list) => {
    if (!firstLoaded.value) firstLoaded.value = true
    const isEmpty = Array.isArray(list) && list.length === 0
    emptyConfig.visible = firstLoaded.value && isEmpty && isDefaultSearch()
  }

  const platformLabelMap = {
    0: '抖音',
    1: '快手',
    2: '视频号'
  }

  const getPlatformLabel = (val) => {
    if (val === undefined || val === null || val === '') return '-'
    return platformLabelMap[val] || String(val)
  }

  const getPlatformClass = (platform) => {
    if (platform === '抖音') return 'platform-douyin'
    if (platform === '视频号') return 'platform-sph'
    if (platform === '快手') return 'platform-kuaishou'
    return ''
  }

  const searchConfig = computed(() => [
    { label: '直播间名称', prop: 'name', type: 'input', placeholder: '请输入' },
    {
      label: '所属平台',
      prop: 'platform',
      type: 'select',
      options: [
        { label: '全部', key: '' },
        { label: '抖音', key: 0 },
        { label: '快手', key: 1 },
        { label: '视频号', key: 2 }
      ]
    },
    {
      label: '所属公司',
      prop: 'companyId',
      type: 'select',
      options: [{ label: '全部', key: '' }, ...companyOptions.value]
    },
    {
      label: '所属部门',
      prop: 'deptId',
      type: 'select',
      options: [{ label: '全部', key: '' }, ...deptOptions.value]
    },
    {
      label: '所属小组',
      prop: 'teamId',
      type: 'select',
      options: [{ label: '全部', key: '' }, ...teamOptions.value]
    }
  ])

  const normalizeTimeToHHmmss = (raw) => {
    const s = String(raw || '')
    const m = s.match(/(\d{1,2}):(\d{2})(?::(\d{2}))?/)
    if (!m) return ''
    const hh = String(m[1]).padStart(2, '0')
    const mm = String(m[2]).padStart(2, '0')
    const ss = String(m[3] || '00').padStart(2, '0')
    return `${hh}:${mm}:${ss}`
  }

  const extractTime = (raw) => {
    const s = String(raw || '').trim()
    if (!s) return ''
    if (s.includes('T')) return s.split('T')[1]?.slice(0, 5) || s
    if (s.includes(' ')) return s.split(' ')[1]?.slice(0, 5) || s
    return s.slice(0, 5)
  }

  const formatDurationMin = (rawMin) => {
    const n = Number(rawMin)
    if (!Number.isFinite(n) || n <= 0) return '-'
    const total = Math.floor(n)
    const h = Math.floor(total / 60)
    const m = total % 60
    if (h > 0 && m > 0) return `${h}小时${m}分钟`
    if (h > 0) return `${h}小时`
    return `${m}分钟`
  }

  const normalizeScheduleList = (raw) => {
    if (Array.isArray(raw)) return raw
    if (raw && typeof raw === 'object') {
      if (Array.isArray(raw.list)) return raw.list
      if (Array.isArray(raw.schedules)) return raw.schedules
      if (Array.isArray(raw.items)) return raw.items
    }
    return []
  }

  const buildScheduleItems = (raw) => {
    const list = normalizeScheduleList(raw).filter((i) => i && typeof i === 'object')
    const items = list
      .map((s) => {
        const start = extractTime(s?.startWork || s?.startTime || s?.start || '')
        const end = extractTime(s?.endWork || s?.endTime || s?.end || '')
        const timeRange = start && end ? `${start}-${end}` : '-'
        const durationText = formatDurationMin(s?.scheduleDuration ?? s?.durationMin ?? s?.duration)
        const key = String(s?.roomSchedulesId || s?.id || `${s?.workDay || ''}_${start}_${end}_${durationText}`)
        return { key, timeRange, durationText }
      })
      .filter((x) => x.timeRange !== '-')

    items.sort((a, b) => String(a.timeRange).localeCompare(String(b.timeRange)))
    return items
  }

  const api = {
    list: async (params) => {
      const page = params?.page || 1
      const pageSize = params?.pageSize || 10
      const res = await apiModule.liveRoom.list({
        page,
        limit: pageSize,
        platform: params?.platform === '' ? undefined : params?.platform,
        anchorName: params?.name || undefined,
        companyId: params?.companyId || undefined,
        deptId: params?.deptId || undefined,
        teamId: params?.teamId || undefined,
        loadSchedule: true
      })

      const rawList = res.data?.list || []
      const list = rawList.map((row) => ({
        id: row.id,
        name: row.anchorName,
        image: row.anchorAvatar,
        platform: getPlatformLabel(row.platform),
        companyId: row.companyId !== undefined && row.companyId !== null ? String(row.companyId) : '',
        companyName: row.companyName,
        group: row.teamName || '-',
        todayItems: buildScheduleItems(
          row?.todaySchedules ||
            row?.todayScheduleList ||
            row?.todaySchedule ||
            row?.thatDaySchedules ||
            row?.thatDayScheduleList ||
            row?.thatDaySchedule
        ),
        tomorrowItems: buildScheduleItems(
          row?.tomorrowSchedules || row?.tomorrowScheduleList || row?.tomorrowSchedule || row?.nextDaySchedules
        )
      }))

      return { list, total: res.data?.totalCount || 0 }
    },
    add: async (form) => {
      const workDay = form.workDay
      const timeRange = form.timeRange || []
      const start = timeRange[0]
      const end = timeRange[1]
      const startWork = normalizeTimeToHHmmss(start) || String(start || '')
      const endWork = normalizeTimeToHHmmss(end) || String(end || '')

      const payload = {
        liveRoomId: form.liveRoomId,
        workDay,
        sessions: [
          {
            startWork,
            endWork,
            scheduleDuration: Number(form.scheduleDuration || 0),
            restDuration: Number(form.restDuration || 0),
            employees: (form.employees || [])
              .filter((i) => i && i.employeeId && i.positionId)
              .map((i) => ({ employeeId: i.employeeId, positionId: i.positionId }))
          }
        ]
      }

      if (Array.isArray(form.cycleDays) && form.cycleDays.length > 0 && form.endDate) {
        payload.batchConfig = { cycleDays: form.cycleDays, endDate: form.endDate }
      }

      await apiModule.roomSchedule.add(payload)
    }
  }

  const loadScheduleAttribute = async (liveRoomId) => {
    if (!liveRoomId) return
    const res = await apiModule.roomSchedule.getAttribute({ id: liveRoomId })
    const data = res.data || {}
    scheduleAttribute.startPlan = data.startPlan || ''
    scheduleAttribute.endPlan = data.endPlan || ''
    scheduleAttribute.shiftOptions = data.shiftOptions || []
    scheduleAttribute.restOptions = data.restOptions || []

    scheduleAttribute.positionOptions = (data.positionOptions || []).map((i) => ({ id: i.id, name: i.name }))
  }

  const fetchEmployeeOptions = async (keyword) => {
    const res = await apiModule.employee.options({ keyword, limit: 50 })
    employeeOptions.value = res.data || []
  }

  const handleAddEmployeeRow = (form) => {
    if (!Array.isArray(form.employees)) form.employees = []
    form.employees.push({ employeeId: undefined, positionId: undefined })
  }

  const handleViewSchedule = (row, rangeType) => {
    router.push({
      name: 'LiveRoomScheduleDetail',
      query: {
        id: row.id,
        name: row.name,
        activeRange: rangeType
      }
    })
  }

  const getQuickAddDate = (dateKey) => {
    return dateKey === 'tomorrow' ? dayjs().add(1, 'day') : dayjs()
  }

  const getQuickAddRangeType = (targetDay) => {
    const startOfThisWeek = dayjs().startOf('week').add(1, 'day')
    const endOfThisWeek = dayjs().startOf('week').add(7, 'day')
    return targetDay.isAfter(endOfThisWeek, 'day') ? 'nextWeek' : 'thisWeek'
  }

  const normalizeOrgId = (id) => {
    if (id === undefined || id === null || id === '') return undefined
    return String(id)
  }

  const deptOptionPromiseCache = new Map()
  const deptOptionResultCache = new Map()

  const normalizeDeptOptionList = (data) => {
    return (data || []).map((i) => ({ ...i, key: String(i.key), label: i.label }))
  }

  const loadDeptOptions = async (companyId) => {
    const normalizedCompanyId = normalizeOrgId(companyId)
    const cacheKey = normalizedCompanyId || '__ALL__'

    const cached = deptOptionResultCache.get(cacheKey)
    if (cached) return cached

    const pending = deptOptionPromiseCache.get(cacheKey)
    if (pending) return pending

    const p = apiModule.dept
      .options({ limit: 200, ...(normalizedCompanyId ? { companyId: normalizedCompanyId } : {}) })
      .then((res) => {
        const list = normalizeDeptOptionList(res.data || [])
        deptOptionResultCache.set(cacheKey, list)
        return list
      })
      .catch((e) => {
        deptOptionResultCache.delete(cacheKey)
        throw e
      })
      .finally(() => {
        deptOptionPromiseCache.delete(cacheKey)
      })

    deptOptionPromiseCache.set(cacheKey, p)
    return p
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

  const fetchMemberDeptOptions = async () => {
    const companyId = normalizeOrgId(memberQuery.companyId)
    if (!companyId) {
      memberDeptOptions.value = []
      return
    }
    memberDeptOptions.value = await loadDeptOptions(companyId)
  }

  const fetchMemberTeamOptions = async () => {
    const deptId = normalizeOrgId(memberQuery.deptId)
    if (!deptId) {
      memberTeamOptions.value = []
      return
    }
    const res = await apiModule.team.options({ limit: 200, deptId })
    memberTeamOptions.value = (res.data || []).map((i) => ({ ...i, key: String(i.key), label: i.label }))
  }

  const fetchMemberList = async (reset = false) => {
    if (reset) memberQuery.page = 1
    memberLoading.value = true
    try {
      const query = {
        page: memberQuery.page,
        limit: memberQuery.limit,
        name: memberQuery.keyword || undefined,
        openMain: true
      }
      const companyId = normalizeOrgId(memberQuery.companyId)
      const deptId = normalizeOrgId(memberQuery.deptId)
      const teamId = normalizeOrgId(memberQuery.teamId)
      if (companyId) query.companyIds = [companyId]
      if (deptId) query.deptIds = [deptId]
      if (teamId) query.teamIds = [teamId]

      const res = await apiModule.employee.specialPage(query)
      memberList.value = normalizeEmployeeList(res.data?.list || [])
      memberTotal.value = res.data?.totalCount || res.data?.total || 0
    } finally {
      memberLoading.value = false
    }
  }

  const confirmMember = (row) => {
    if (!row) return
    addShiftDrawerRef.value?.setMember?.(memberPickIndex.value, { id: row.id, name: row.name })
    memberSelectedId.value = row?.id !== undefined && row?.id !== null ? String(row.id) : ''
  }

  const handlePickMember = async ({ index }) => {
    memberPickIndex.value = Number(index)
    memberSelectedId.value = ''
    const current = addShiftDrawerRef.value?.getSubmitData?.()
    const shifts = Array.isArray(current?.shifts) ? current.shifts : []
    const picked = shifts[memberPickIndex.value]
    if (picked?.memberId !== undefined && picked?.memberId !== null && String(picked.memberId)) {
      memberSelectedId.value = String(picked.memberId)
    }
    memberQuery.keyword = ''
    memberQuery.page = 1
    memberQuery.limit = 10
    memberQuery.companyId =
      activeRoom.value?.companyId !== undefined && activeRoom.value?.companyId !== null
        ? String(activeRoom.value.companyId)
        : ''
    memberQuery.deptId = ''
    memberQuery.teamId = ''
    memberDrawerVisible.value = true
    await fetchMemberDeptOptions()
    await fetchMemberTeamOptions()
    await fetchMemberList(true)
  }

  const normalizeNumberList = (raw, min = 0) => {
    const list = (Array.isArray(raw) ? raw : []).map((x) => Number(x)).filter((x) => Number.isFinite(x) && x >= min)
    const uniq = Array.from(new Set(list))
    uniq.sort((a, b) => a - b)
    return uniq
  }

  const getTimeAxisKey = (id) => `live_room_schedule_time_axis_unit_${id}`

  const loadTimeAxisUnit = (id) => {
    if (!id) return
    try {
      const v = Number(window?.localStorage?.getItem(getTimeAxisKey(id)) || 0)
      if ([30, 60, 120].includes(v)) timeAxisUnit.value = v
    } catch (e) {
      void e
    }
  }

  const saveTimeAxisUnit = (id) => {
    if (!id) return
    try {
      window?.localStorage?.setItem(getTimeAxisKey(id), String(timeAxisUnit.value))
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

  const normalizeIdList = (raw) => {
    const list = (Array.isArray(raw) ? raw : [])
      .map((x) => (x === undefined || x === null ? '' : String(x)))
      .filter((x) => x)
    return Array.from(new Set(list))
  }

  const applyAttributeData = (data) => {
    const startPlan = data?.startPlan || ''
    const endPlan = data?.endPlan || ''
    attributeForm.startPlan = startPlan
    attributeForm.endPlan = endPlan
    attributeForm.planRange = [startPlan, endPlan]
    attributeForm.shiftOptions = normalizeNumberList(data?.shiftOptions || [], 1)
    attributeForm.restOptions = normalizeNumberList(data?.restOptions || [], 0)

    attributePreset.shiftOptions = attributeForm.shiftOptions
    attributePreset.restOptions = attributeForm.restOptions

    const idsFromRes = normalizeIdList((data?.positionOptions || []).map((i) => i?.id))
    attributeForm.positionIds = idsFromRes.slice(0, 3)
  }

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

  const openCopyAttributeDialog = async () => {
    copyAttributeDialogVisible.value = true
    copySourceId.value = ''
    copySourceName.value = ''
    await fetchCopyRoomList(true)
  }

  const clearCopySource = () => {
    copySourceId.value = ''
    copySourceName.value = ''
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
        return { id: r.id, name: r.anchorName, image: r.anchorAvatar, planText, shiftText, restText, _attr: attr }
      })
    )
    const [ps, pe] = copyQuery.planRange || []
    copyRoomList.value = enriched.filter((x) => {
      if (!ps || !pe) return true
      return x?._attr?.startPlan === ps && x?._attr?.endPlan === pe
    })
  }

  const handleCopySelected = (row) => {
    copySourceId.value = row?.id || ''
    copySourceName.value = row?.name || ''
  }

  const confirmCopyAttribute = (row) => {
    const picked = row || (copyRoomList.value || []).find((x) => String(x.id) === String(copySourceId.value))
    if (!picked) return
    const data = picked?._attr || {}
    applyAttributeData(data)
    loadTimeAxisUnit(picked?.id)
  }

  const submitAttribute = async () => {
    if (!activeRoom.value?.id) return
    attributeForm.planRange = Array.isArray(attributeForm.planRange) ? attributeForm.planRange : ['', '']
    attributeForm.startPlan = attributeForm.planRange?.[0] || ''
    attributeForm.endPlan = attributeForm.planRange?.[1] || ''
    attributeForm.shiftOptions = normalizeNumberList(attributeForm.shiftOptions || [], 1)
    attributeForm.restOptions = normalizeNumberList(attributeForm.restOptions || [], 0)
    attributeForm.positionIds = normalizeIdList(attributeForm.positionIds || []).slice(0, 3)
    await apiModule.roomSchedule.setAttribute({
      id: activeRoom.value.id,
      startPlan: attributeForm.startPlan,
      endPlan: attributeForm.endPlan,
      shiftOptions: attributeForm.shiftOptions || [],
      restOptions: attributeForm.restOptions || [],
      positionOptions: normalizeIdList(attributeForm.positionIds || [])
        .map((id) => normalizeOrgId(id))
        .filter((x) => x !== undefined)
    })
    saveTimeAxisUnit(activeRoom.value.id)
    ElMessage.success('保存成功')
    attributeDrawerVisible.value = false

    const dateStr = pendingAddDate.value
    pendingAddDate.value = ''
    if (dateStr) openAddDrawer(dateStr)
  }

  const getDefaultDuration = () => normalizeNumberList(attributeForm.shiftOptions || [], 1)[0] || 120
  const getDefaultRest = () => {
    const list = normalizeNumberList(attributeForm.restOptions || [], 0)
    return list.includes(0) ? 0 : list[0] || 0
  }

  const getAnchorPositionId = () => {
    const selected = selectedPositionTags.value || []
    const hit = selected.find((p) => String(p.name || '').includes('主播'))
    return String(hit?.id || selected[0]?.id || attributeForm.positionIds?.[0] || '')
  }

  const openAddDrawer = (dateStr) => {
    addShiftInitial.value = {
      date: dateStr,
      shifts: [
        {
          memberId: '',
          memberName: '',
          startTime: attributeForm.planRange?.[0] || '08:00',
          durationMin: getDefaultDuration(),
          restMin: getDefaultRest()
        }
      ],
      batch: { weekdays: [], endDate: '' }
    }
    addDrawerVisible.value = true
  }

  const submitAdd = async () => {
    const payload = addShiftDrawerRef.value?.getSubmitData?.()
    if (!activeRoom.value?.id) return
    if (!payload?.date) {
      ElMessage.error('请选择班次日期')
      return
    }
    if (!Array.isArray(payload.shifts) || !payload.shifts.length) {
      ElMessage.error('请至少添加一场排班')
      return
    }

    const workDay = payload.date
    const anchorPositionId = getAnchorPositionId()
    const sessions = payload.shifts.map((s) => {
      const startTime = s.startTime
      const durationMin = Number(s.durationMin) || 0
      const startDt = dayjs(`${workDay} ${startTime}`)
      const endDt = startDt.add(durationMin, 'minute')
      const employeeId = normalizeOrgId(s.memberId)
      const positionId = normalizeOrgId(anchorPositionId)
      return {
        startWork: startDt.format('HH:mm:ss'),
        endWork: endDt.format('HH:mm:ss'),
        scheduleDuration: durationMin,
        restDuration: Number(s.restMin) || 0,
        employees:
          employeeId && positionId
            ? [
                {
                  employeeId,
                  positionId
                }
              ]
            : []
      }
    })

    const liveRoomId = normalizeOrgId(activeRoom.value.id)
    if (!liveRoomId) {
      ElMessage.error('直播间ID异常')
      return
    }
    const req = { liveRoomId, workDay, sessions }
    const weekdays = Array.isArray(payload.batch?.weekdays) ? payload.batch.weekdays : []
    const endDate = payload.batch?.endDate
    const cycleDays = weekdays
      .map((d) => (Number(d) === 0 ? 7 : Number(d)))
      .filter((d) => Number.isFinite(d) && d >= 1 && d <= 7)
    if (cycleDays.length && endDate) req.batchConfig = { cycleDays, endDate }

    await apiModule.roomSchedule.add(req)
    ElMessage.success('新增成功')
    addDrawerVisible.value = false
    curdRef.value?.getData?.()
  }

  const loadRoomAttributeFor = async (roomId) => {
    if (!roomId) return
    await fetchAllPositionOptions()
    const res = await apiModule.roomSchedule.getAttribute({ id: roomId })
    applyAttributeData(res.data || {})
    loadTimeAxisUnit(roomId)
  }

  const handleAddSchedule = async (dateKey, row) => {
    const targetDay = getQuickAddDate(dateKey)
    router.push({
      name: 'LiveRoomScheduleDetail',
      query: {
        id: row.id,
        name: row.name,
        activeRange: getQuickAddRangeType(targetDay),
        openAdd: '1',
        day: targetDay.format('YYYY-MM-DD'),
        dayType: dateKey
      }
    })
  }

  const fetchCompanyOptions = async () => {
    const res = await apiModule.subCompany.options({ limit: 200 })
    companyOptions.value = (res.data || []).map((i) => ({ ...i, key: String(i.key), label: i.label }))
  }

  const fetchDeptOptions = async (companyId) => {
    deptOptions.value = await loadDeptOptions(companyId)
  }

  const fetchTeamOptions = async (deptId) => {
    const normalizedDeptId = normalizeOrgId(deptId)
    if (!normalizedDeptId) {
      teamOptions.value = []
      return
    }
    const res = await apiModule.team.options({ limit: 200, deptId: normalizedDeptId })
    teamOptions.value = (res.data || []).map((i) => ({ ...i, key: String(i.key), label: i.label }))
  }

  onMounted(async () => {
    fetchAllPositionOptions()
    await fetchCompanyOptions()
    refreshEmptyConfig()
    await fetchDeptOptions()
    await fetchTeamOptions()
  })

  watch(hasCompany, () => {
    refreshEmptyConfig()
  })

  watch(
    () => curdRef.value?.searchParams?.companyId,
    (val, oldVal) => {
      if (oldVal === undefined) return
      if (val === oldVal) return
      fetchDeptOptions(val)
      if (curdRef.value?.searchParams && curdRef.value.searchParams.deptId) curdRef.value.searchParams.deptId = ''
      if (curdRef.value?.searchParams && curdRef.value.searchParams.teamId) curdRef.value.searchParams.teamId = ''
      if (teamOptions.value.length) teamOptions.value = []
    }
  )

  watch(
    () => curdRef.value?.searchParams?.deptId,
    (val, oldVal) => {
      if (oldVal === undefined) return
      if (val === oldVal) return
      fetchTeamOptions(val)
      if (curdRef.value?.searchParams && curdRef.value.searchParams.teamId) curdRef.value.searchParams.teamId = ''
    }
  )

  watch(
    () => memberQuery.companyId,
    async () => {
      memberQuery.deptId = ''
      memberQuery.teamId = ''
      memberTeamOptions.value = []
      await fetchMemberDeptOptions()
      await fetchMemberList(true)
    }
  )

  watch(
    () => memberQuery.deptId,
    async () => {
      memberQuery.teamId = ''
      await fetchMemberTeamOptions()
      await fetchMemberList(true)
    }
  )

  watch(
    () => memberQuery.teamId,
    () => {
      fetchMemberList(true)
    }
  )
</script>

<style scoped lang="scss">
  .avtar-container {
    display: flex;
    align-items: center;
  }

  .role-drawer__header {
    display: flex;
    width: 100%;
    gap: 13px;
    align-items: center;
  }
  .page-container {
    background-color: #f5f7fa;
  }
  :deep(.curd-row-card-top) {
    padding: 20px 20px 0 20px;
  }

  :deep(.table-row) {
    padding: 20px 20px 0 20px;
  }

  :deep(.page-row) {
    padding: 10px 0 20px 0;
  }

  :deep(.curd-search-form) {
    padding: 20px;
    background-color: #fff;
    border-radius: 8px;
    margin-bottom: 16px;
  }

  .live-room-cell {
    display: flex;
    align-items: center;
    gap: 12px;

    .name {
      font-weight: 500;
    }
  }

  .platform-tag {
    padding: 2px 16px;
    display: inline-block;
    border-radius: 4px;
    font-size: 12px;
    border: none;
    background-color: #ddd;

    &.platform-douyin {
      color: #333a60;
      background-color: #f3f5ff;
    }
    &.platform-sph {
      color: #fa8710;
      background-color: #fff5ea;
    }
    &.platform-kuaishou {
      color: #fff2ed;
      background-color: #ff6026;
    }
  }

  .schedule-list {
    display: flex;
    flex-direction: column;
    gap: 4px;
  }

  .schedule-item {
    display: flex;
    align-items: center;
    font-size: 14px;
    line-height: 1.5;

    .dot {
      width: 6px;
      height: 6px;
      border-radius: 50%;
      margin-right: 8px;
      flex-shrink: 0;
    }
    .duration {
      margin-left: 20px;
    }
  }

  .section-title {
    font-weight: bold;
    font-size: 14px;
    margin-bottom: 16px;
    color: #303133;
  }

  .required-mark {
    color: #f56c6c;
    margin-right: 4px;
  }

  .form-section {
    background-color: #f5f7fa;
    padding: 20px;
    border-radius: 4px;
  }

  .drawer-footer {
    display: flex;
    justify-content: flex-start;
    gap: 12px;
    padding: 12px 30px 30px 26px;
    .el-button {
      height: 34px;
      border-radius: 56px;
    }
    .status-btn {
      width: 84px;
    }
  }

  .member-footer {
    display: flex;
    justify-content: space-between;
  }

  .attribute-drawer-footer {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }

  .attribute-panel {
    display: flex;
    flex-direction: column;
    gap: 36px;
    padding-right: 6px;
  }

  .attr-section {
    display: flex;
    flex-direction: column;
    gap: 10px;
  }

  .attr-title {
    font-size: 16px;
    font-weight: bold;
    color: #303133;
  }

  .required {
    color: #f56c6c;
    margin-right: 4px;
  }

  .attr-card {
    display: flex;
    flex-direction: column;
    row-gap: 24px;
    background: #f7f8fa;
    border-radius: 8px;
    padding: 24px;
  }

  .attr-row {
    display: flex;
    align-items: center;
    gap: 12px;

    &:last-child {
      margin-bottom: 0;
    }
  }

  .attr-label {
    width: 90px;
    flex-shrink: 0;
    color: #151719;
    font-size: 14px;
    text-wrap: nowrap;
    text-align: end;
  }
  .attr-input {
    width: 230px;

    :deep(.el-input__wrapper) {
      background-color: #dcdcdc;
      box-shadow: none;
    }

    :deep(.el-input__inner) {
      color: #151719 !important;
      -webkit-text-fill-color: #151719 !important;
    }

    &.is-disabled {
      :deep(.el-input__inner) {
        color: #151719 !important;
        -webkit-text-fill-color: #151719 !important;
      }
    }
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

  .position-grid {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 10px 16px;
  }

  .every-time-picker {
    width: 230px;
  }

  .member-filters {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 10px;
    margin-bottom: 12px;
  }

  .member-cell {
    display: flex;
    align-items: center;
    gap: 10px;

    .name {
      color: #303133;
      font-weight: 500;
    }
  }

  .custom-view {
    color: #15acfe;
  }

  :deep(.common-dialog .el-dialog__footer) {
    border-top: none;
    padding: 0 40px 28px 0;
  }
  .custom-btn {
    width: 84px;
    height: 34px;
    border-radius: 50px;
  }
</style>

<!--<style lang="scss">
  .attribute-drawer-header {
    padding: 11px 30px;
    margin-bottom: 0;
    border-bottom: 1px solid #dcdcdc;
    .form-section__title {
      font-size: 16px;
      background-color: transparent;
      border: none;
      padding: 0;
    }
    .header-title {
      display: flex;
      width: 100%;
      gap: 13px;
    }
  }
  .member-search {
    width: 84px;
    height: 34px;
    border-radius: 54px;
  }
</style>-->
