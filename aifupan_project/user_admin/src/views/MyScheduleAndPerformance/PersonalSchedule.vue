<template>
  <div class="personal-schedule">
    <UserProfileCard :user-info="userInfo" />

    <div class="schedule-controls">
      <div class="title">个人排班 ({{ dateRangeText }})</div>
      <div class="actions">
        <el-button-group>
          <el-button :type="rangeType === 'thisWeek' ? 'primary' : 'default'" @click="setRangeType('thisWeek')">
            本周
          </el-button>
          <el-button :type="rangeType === 'nextWeek' ? 'primary' : 'default'" @click="setRangeType('nextWeek')">
            下周
          </el-button>
          <el-button :type="rangeType === 'thisMonth' ? 'primary' : 'default'" @click="setRangeType('thisMonth')">
            本月
          </el-button>
          <el-button :type="rangeType === 'nextMonth' ? 'primary' : 'default'" @click="setRangeType('nextMonth')">
            下月
          </el-button>
        </el-button-group>
        <el-date-picker
          v-model="currentDate"
          type="date"
          value-format="YYYY-MM-DD"
          format="YYYY/MM/DD"
          placeholder="请选择日期"
          style="width: 140px"
          @change="handleDateChange"
        />
      </div>
    </div>

    <div class="schedule-wrapper">
      <el-skeleton v-if="loading" :rows="6" animated />
      <Schedule v-else :data="scheduleData" readonly />
    </div>
  </div>
</template>

<script setup>
  /**
   * @file PersonalSchedule.vue
   * @description 个人排班页面
   */
  import { computed, onMounted, ref, watch } from 'vue'
  import dayjs from 'dayjs'
  import UserProfileCard from '@/components/UserProfileCard/index.vue'
  import Schedule from '@/components/Schedule/index.vue'
  import { useUserStore } from '@/store/user'
  import apiModule from '@/http/api'

  const userStore = useUserStore()
  const userInfo = computed(() => userStore.userInfo)

  const loading = ref(false)
  const scheduleData = ref([])

  const currentDate = ref('')
  const rangeType = ref('thisWeek')
  const dateRange = ref([dayjs().format('YYYY-MM-DD'), dayjs().format('YYYY-MM-DD')])

  const dateRangeText = computed(() => {
    const [startDate, endDate] = dateRange.value || []
    if (!startDate || !endDate) return '-'
    if (startDate === endDate) return dayjs(startDate).format('YYYY/MM/DD')
    const s = dayjs(startDate).format('YYYY/MM/DD')
    const e = dayjs(endDate).format('MM/DD')
    return `${s}-${e}`
  })

  const getWeekRange = (baseDate, addWeeks = 0) => {
    const d = dayjs(baseDate).add(addWeeks, 'week')
    const diff = (d.day() + 6) % 7
    const start = d.subtract(diff, 'day').format('YYYY-MM-DD')
    const end = dayjs(start).add(6, 'day').format('YYYY-MM-DD')
    return [start, end]
  }

  const getMonthRange = (baseDate, addMonths = 0) => {
    const d = dayjs(baseDate).add(addMonths, 'month')
    const start = d.startOf('month').format('YYYY-MM-DD')
    const end = d.endOf('month').format('YYYY-MM-DD')
    return [start, end]
  }

  const setRangeType = (type) => {
    rangeType.value = type
    currentDate.value = '' // 点击快捷按钮，清空独立日期

    const baseDate = dayjs().format('YYYY-MM-DD')
    if (type === 'thisWeek') dateRange.value = getWeekRange(baseDate, 0)
    if (type === 'nextWeek') dateRange.value = getWeekRange(baseDate, 1)
    if (type === 'thisMonth') dateRange.value = getMonthRange(baseDate, 0)
    if (type === 'nextMonth') dateRange.value = getMonthRange(baseDate, 1)
  }

  const handleDateChange = () => {
    if (!currentDate.value) {
      // 清空独立日期，默认回到本周
      setRangeType('thisWeek')
    } else {
      // 选择独立日期，取消快捷按钮高亮，只展示选中的这一天
      rangeType.value = ''
      dateRange.value = [currentDate.value, currentDate.value]
    }
  }

  const formatDurationShort = (startTs, endTs) => {
    const diffMin = Math.max(0, Math.floor((endTs - startTs) / 60000))
    const h = Math.floor(diffMin / 60)
    const m = diffMin % 60
    if (h > 0 && m > 0) return `${h}h${m}m`
    if (h > 0) return `${h}h`
    return `${m}m`
  }

  const parseDateTime = (workDay, hhmmss) => {
    if (!workDay) return null
    if (!hhmmss) return null
    const time = String(hhmmss).length >= 8 ? String(hhmmss).slice(0, 8) : `${hhmmss}:00`
    return dayjs(`${workDay} ${time}`).valueOf()
  }

  const buildScheduleData = (list, startDate, endDate) => {
    const start = dayjs(startDate)
    const end = dayjs(endDate)
    const days = []
    const todayStr = dayjs().format('YYYY-MM-DD')
    const weekMap = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']

    for (let i = 0; i <= end.diff(start, 'day'); i++) {
      const dayStr = start.add(i, 'day').format('YYYY-MM-DD')
      const dateTimeKey = start.add(i, 'day').format('MM/DD')
      const isToday = dayStr === todayStr
      const label = isToday ? '今日' : weekMap[start.add(i, 'day').day()]

      const daySchedules = (list || []).filter((x) => String(x?.workDay || '') === dayStr)
      const blocks = daySchedules
        .map((item) => {
          const startTs = parseDateTime(item?.workDay, item?.startWork)
          const endTs = parseDateTime(item?.workDay, item?.endWork)
          if (!startTs || !endTs) return null
          // const durationStr = formatDurationShort(startTs, endTs)
          const title = item?.liveRoomName ? `直播间：${item.liveRoomName}` : '直播间'
          return {
            id: String(item?.id || `${dayStr}_${item?.liveRoomId || ''}_${item?.startWork || ''}`),
            memberId: String(userStore.userInfo?.employeeId || 'me'),
            name: `${title}`,
            startTime: startTs,
            times: [startTs, endTs],
            timeCount: (endTs - startTs) / 60000,
            color: isToday ? '#3d5af1' : '#e5e7eb',
            textColor: isToday ? '#ffffff' : '#303133'
          }
        })
        .filter(Boolean)

      days.push({
        dateTime: dateTimeKey,
        label,
        isToday,
        data: [
          {
            name: '主播',
            type: 1,
            data: blocks
          }
        ]
      })
    }
    return days
  }

  const fetchSchedule = async () => {
    const [startDate, endDate] = dateRange.value || []
    if (!startDate || !endDate) return
    loading.value = true
    try {
      if (!userStore.userInfo?.employeeId) {
        await userStore.getUserInfo()
      }
      const res = await apiModule.employeeSchedule.listSpitDay({
        startDate,
        endDate
      })
      const list = res?.data || []
      scheduleData.value = buildScheduleData(list, startDate, endDate)
    } finally {
      loading.value = false
    }
  }

  watch(
    () => dateRange.value,
    () => {
      fetchSchedule()
    },
    { deep: true }
  )

  onMounted(() => {
    setRangeType('thisWeek')
  })
</script>

<style lang="scss" scoped>
  .personal-schedule {
    min-height: 100%;
    display: flex;
    flex-direction: column;
    gap: 26px;

    .schedule-controls {
      display: flex;
      justify-content: space-between;
      align-items: center;

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
    }
  }

  :deep(.schedule-block) {
    border-radius: 6px;
  }
</style>
