<template>
  <div class="personal-schedule-detail">
    <!-- 顶部个人信息卡片 -->
    <UserProfileCard :user-info="userInfo" />

    <!-- 排班控制栏 -->
    <div class="schedule-controls">
      <div class="title">个人排班 ({{ dateRangeText }})</div>
      <div class="actions">
        <el-button-group>
          <el-button :type="activeRange === 'thisWeek' ? 'primary' : 'default'"  @click="setThisWeek"
            >本周</el-button
          >
          <el-button :type="activeRange === 'nextWeek' ? 'primary' : 'default'"  @click="setNextWeek"
            >下周</el-button
          >
          <el-button :type="activeRange === 'thisMonth' ? 'primary' : 'default'"  @click="setThisMonth"
            >本月</el-button
          >
          <el-button :type="activeRange === 'nextMonth' ? 'primary' : 'default'"  @click="setNextMonth"
            >下月</el-button
          >
        </el-button-group>
        <el-date-picker
          v-model="currentDate"
          type="date"
          placeholder="请选择日期"
          value-format="YYYY-MM-DD"
          style="width: 140px"
        />
      </div>
    </div>

    <!-- 排班组件 (只读) -->
    <div class="schedule-wrapper">
      <Schedule :data="scheduleData" readonly />
    </div>
  </div>
</template>

<script setup>
  /**
   * @file Detail/index.vue
   * @description 个人排班详情页面 (StaffScheduleDetail)
   */
  import { ref, reactive, computed, onMounted, watch } from 'vue'
  import dayjs from 'dayjs'
  import { useRoute } from 'vue-router'
  import UserProfileCard from '@/components/UserProfileCard/index.vue'
  import Schedule from '@/components/Schedule/index.vue'
  import apiModule from '@/http/api'
  import { normalizeKeyLabelOptions } from '@/utils/options'

  const route = useRoute()
  const employeeId = ref(null)

  // --- 用户信息 ---
  const userInfo = reactive({
    userAvatar: '',
    name: '-',
    liveRoom: '-',
    organization: '-',
    position: '-',
    phone: '-',
    email: '-'
  })

  const fetchUserInfo = async () => {
    if (!employeeId.value) return
    try {
      const res = await apiModule.employeeProfile.base({ employeeId: employeeId.value, loadRoom: true })
      const data = res.data || {}
      userInfo.userAvatar = data.userAvatar || ''
      userInfo.name = data.name || '-'
      userInfo.liveRoom = (data.roomInfos || []).map((r) => r.anchorName).join('、') || '-'
      const orgPath = []
      if (data.companyName) orgPath.push(data.companyName)
      if (data.deptName) orgPath.push(data.deptName)
      if (data.teamName) orgPath.push(data.teamName)
      userInfo.organization = orgPath.join('-') || '-'

      userInfo.position = data.positionName || '-'
      userInfo.phone = data.mobile || '-'
      userInfo.email = data.email || '-'
    } catch (e) {
      void e
    }
  }

  const currentDate = ref('')
  const activeRange = ref('thisWeek')
  const liveRoomId = ref(undefined)
  const liveRoomOptions = ref([])
  const dateRange = ref([
    dayjs().startOf('week').add(1, 'day').format('YYYY-MM-DD'),
    dayjs().startOf('week').add(7, 'day').format('YYYY-MM-DD')
  ])

  const getBaseDateStr = () => {
    return currentDate.value || dayjs().format('YYYY-MM-DD')
  }

  const dateRangeText = computed(() => {
    const [start, end] = dateRange.value || []
    if (!start || !end) return '-'
    const s = dayjs(start).format('YYYY/MM/DD')
    const e = dayjs(end).format('MM/DD')
    return `${s}-${e}`
  })

  const getWeekRange = (baseDateStr, weekOffset = 0) => {
    const base = dayjs(baseDateStr)
    const day = base.day()
    const diffToMonday = (day + 6) % 7
    const monday = base.subtract(diffToMonday, 'day').add(weekOffset * 7, 'day')
    const sunday = monday.add(6, 'day')
    return [monday.format('YYYY-MM-DD'), sunday.format('YYYY-MM-DD')]
  }

  const getMonthRange = (baseDateStr, monthOffset = 0) => {
    const base = dayjs(baseDateStr).add(monthOffset, 'month')
    const start = base.startOf('month')
    const end = base.endOf('month')
    return [start.format('YYYY-MM-DD'), end.format('YYYY-MM-DD')]
  }

  const setThisWeek = () => {
    currentDate.value = ''
    activeRange.value = 'thisWeek'
    dateRange.value = getWeekRange(getBaseDateStr(), 0)
  }

  const setNextWeek = () => {
    currentDate.value = ''
    activeRange.value = 'nextWeek'
    dateRange.value = getWeekRange(getBaseDateStr(), 1)
  }

  const setThisMonth = () => {
    currentDate.value = ''
    activeRange.value = 'thisMonth'
    dateRange.value = getMonthRange(getBaseDateStr(), 0)
  }

  const setNextMonth = () => {
    currentDate.value = ''
    activeRange.value = 'nextMonth'
    dateRange.value = getMonthRange(getBaseDateStr(), 1)
  }

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

    const roleDefs = []
    const roleKeySet = new Set()
    const roleKeyToType = new Map()

    ;(list || []).forEach((item) => {
      ;(item?.positions || []).forEach((p) => {
        const key = String(p?.positionId ?? p?.positionName ?? '').trim()
        if (!key || roleKeySet.has(key)) return
        roleKeySet.add(key)
        roleKeyToType.set(key, key)
        roleDefs.push({
          type: key,
          name: p?.positionName || '岗位'
        })
      })
    })

    if (roleDefs.length === 0) {
      roleDefs.push({ type: 'default', name: '排班' })
    }

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
        data: roleDefs.map((r) => ({ name: r.name, type: r.type, data: [] }))
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
        const roleKey = String(p?.positionId ?? p?.positionName ?? '').trim()
        const roleType = roleKeyToType.get(roleKey)
        if (!roleType) return

        const emp = (p?.employees || [])[0] || {}
        const id = item.id
        const name = item?.liveRoomName || '直播间'
        const memberId = emp.employeeId || null
        const originalTimeCount = Math.max(0, Math.round((endMs - startMs) / (1000 * 60)))
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
                  memberId,
                  name,
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

  const scheduleData = ref([])

  const fetchLiveRoomOptions = async () => {
    const res = await apiModule.liveRoom.options({ limit: 200 })
    liveRoomOptions.value = normalizeKeyLabelOptions(res.data || [])
  }

  const fetchSchedule = async () => {
    const [startDate, endDate] = dateRange.value || []
    if (!startDate || !endDate || !employeeId.value) return
    const res = await apiModule.employeeSchedule.listSpitDay({
      employeeId: employeeId.value,
      liveRoomId: liveRoomId.value || undefined,
      startDate,
      endDate
    })
    scheduleData.value = buildScheduleData(res.data || [], dateRange.value)
  }

  watch(
    [dateRange, liveRoomId, employeeId],
    () => {
      fetchSchedule()
    },
    { flush: 'post' }
  )

  watch(currentDate, (val) => {
    if (val) {
      activeRange.value = ''
      dateRange.value = [String(val), String(val)]
      return
    }

    if (!activeRange.value) {
      setThisWeek()
    }
  })

  onMounted(() => {
    if (route.query.id) {
      employeeId.value = route.query.id
    }
    if (route.query.name) {
      userInfo.name = route.query.name
    }
    setThisWeek()

    if (route.query.liveRoomId) {
      liveRoomId.value = Number(route.query.liveRoomId)
    }

    fetchLiveRoomOptions()
    fetchUserInfo()
  })
</script>

<style lang="scss" scoped>
  .personal-schedule-detail {
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
  }
</style>
