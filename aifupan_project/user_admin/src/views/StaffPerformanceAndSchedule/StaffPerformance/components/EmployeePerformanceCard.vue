<template>
  <div class="live-room-card">
    <div class="live-room-header">
      <div class="header-left">
        <div class="avatar-info">
          <div class="avtar-container">
            <el-avatar :size="40" :src="avatar || defaultImg" />
          </div>
          <div class="room-name">
            <span class="name">{{ data.employeeName || '-' }}</span>
          </div>
        </div>
        <div class="room-info">
          <div class="room-meta">
            <div class="meta-item">
              <span class="label">岗位：</span>
              <span class="value" :style="{ color: getRoleColor(data.positionName) }">{{ data.positionName }}</span>
            </div>
          </div>
          <div class="room-meta">
            <div class="meta-item">
              <span class="label">所属组织：</span>
              <span class="value">{{ orgText }}</span>
            </div>
          </div>
          <div class="room-meta">
            <div class="meta-item">
              <span class="label">所属直播间：</span>
              <span class="value">{{ formatLiveRoomList }}</span>
            </div>
          </div>
        </div>
      </div>
      <div class="header-right">
        <el-button type="primary" plain @click.stop="$emit('detail')" style="border-radius: 39px; height: 28px"
          >查看历史业绩</el-button
        >
      </div>
    </div>

    <div class="live-room-stats">
      <div v-for="(item, index) in statsConfig" :key="index" class="stat-item">
        <div class="stat-label">{{ item.label }}</div>
        <div class="stat-value" :style="{ color: item.color }">{{ item.today }}</div>
        <div class="stat-compare">
          <div class="compare-row">
            <div class="compare-label">
              <span class="dot yesterday"></span>
              <span class="label">昨日</span>
            </div>
            <span class="value">{{ item.yesterday }}</span>
          </div>
          <div class="compare-row">
            <div class="compare-label">
              <span class="dot this-week"></span>
              <span class="label">本周</span>
            </div>
            <span class="value">{{ item.week }}</span>
          </div>
          <div class="compare-row">
            <div class="compare-label">
              <span class="dot this-month"></span>
              <span class="label">本月</span>
            </div>
            <span class="value">{{ item.month }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
  /**
   * @file EmployeePerformanceCard.vue
   * @description 人员业绩列表卡片（六时段指标展示）
   */
  import { computed } from 'vue'
  import useGetRoleColor from '@/hooks/roleColor.js'
  import defaultImg from '@/assets/images/funnel/defaultAvatar.png'

  const { getRoleColor } = useGetRoleColor()
  const props = defineProps({
    data: { type: Object, required: true, default: () => ({}) }
  })

  defineEmits(['detail'])

  const period = computed(() => props.data?.performanceStatistics || {})

  const avatar = computed(() => props.data?.avatar || props.data?.userAvatar || '')

  const orgText = computed(() => {
    const companyName = props.data?.companyName
    const deptName = props.data?.deptName
    const teamName = props.data?.teamName
    return [companyName, deptName, teamName].filter(Boolean).join('-') || '-'
  })

  const statsConfig = computed(() => {
    return [
      {
        label: '场次',
        color: '#151719',
        today: formatSession(period.value.sessionStats?.today),
        yesterday: formatSession(period.value.sessionStats?.yesterday),
        week: formatSession(period.value.sessionStats?.thisWeek),
        month: formatSession(period.value.sessionStats?.thisMonth)
      },
      {
        label: '场观',
        color: '#151719',
        today: formatAuto(period.value.viewCount?.today),
        yesterday: formatAuto(period.value.viewCount?.yesterday),
        week: formatAuto(period.value.viewCount?.thisWeek),
        month: formatAuto(period.value.viewCount?.thisMonth)
      },
      {
        label: '销售',
        color: '#151719',
        today: formatAuto(period.value.salesRevenue?.today),
        yesterday: formatAuto(period.value.salesRevenue?.yesterday),
        week: formatAuto(period.value.salesRevenue?.thisWeek),
        month: formatAuto(period.value.salesRevenue?.thisMonth)
      },
      {
        label: '退款',
        color: '#151719',
        today: formatAuto(period.value.refund?.today),
        yesterday: formatAuto(period.value.refund?.yesterday),
        week: formatAuto(period.value.refund?.thisWeek),
        month: formatAuto(period.value.refund?.thisMonth)
      },
      {
        label: '净销售',
        color: '#151719',
        today: formatAuto(period.value.netSales?.today),
        yesterday: formatAuto(period.value.netSales?.yesterday),
        week: formatAuto(period.value.netSales?.thisWeek),
        month: formatAuto(period.value.netSales?.thisMonth)
      },
      {
        label: '投放',
        color: '#151719',
        today: formatAuto(period.value.investment?.today),
        yesterday: formatAuto(period.value.investment?.yesterday),
        week: formatAuto(period.value.investment?.thisWeek),
        month: formatAuto(period.value.investment?.thisMonth)
      }
    ]
  })

  const formatLiveRoomList = computed(() => {
    const liveRoomList = props.data?.liveRoomList
    console.log('liveRoomList', liveRoomList)
    if (Array.isArray(liveRoomList)) {
      return liveRoomList.map((item) => item.name).join(',')
    }
    return '-'
  })

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
   * @description 场次统计格式化
   * @param {Object} stats - SessionStats
   * @returns {string}
   */
  const formatSession = (stats) => {
    if (!stats) return '-'
    const count = Number(stats?.count) || 0
    const duration = formatMinutes(stats?.duration)
    return `${count}场 (${duration})`
  }

  /**
   * @description 自动单位格式化（w/亿）
   * @param {number} val - 数值
   * @returns {string}
   */
  const formatAuto = (val) => {
    const n = Number(val)
    if (!Number.isFinite(n)) return '-'
    const abs = Math.abs(n)
    if (abs >= 100000000) return `${(n / 100000000).toFixed(1)}亿`
    if (abs >= 10000) return `${(n / 10000).toFixed(1)}w`
    return n.toLocaleString('zh-CN', { maximumFractionDigits: 2 })
  }
</script>

<style scoped lang="scss">
  .avtar-container {
    display: flex;
    align-items: center;
  }

  .live-room-card {
    position: relative;
    border: 1px solid #dfeaf6;
    border-radius: 10px;
    padding: 16px 20px;
  }

  .live-room-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding-bottom: 12px;
  }

  .header-left {
    display: flex;
    align-items: center;
    gap: 12px;
    min-width: 0;
  }

  .avatar-info {
    display: flex;
    align-items: center;
    column-gap: 8px;

    .name {
      font-weight: 600;
      color: #303133;
      font-size: 14px;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }

  .room-info {
    display: flex;
    column-gap: 40px;
    font-size: 14px;
    min-width: 0;
  }

  .room-name {
    display: flex;
    align-items: center;
    gap: 30px;
    min-width: 0;
  }

  .tags {
    display: flex;
    gap: 4px;
    flex-shrink: 0;
  }

  .room-meta {
    color: #909399;
    display: flex;
    align-items: center;
    gap: 30px;
    min-width: 0;
  }

  .room-meta .label {
    color: #909399;
  }

  .room-meta .value {
    color: #606266;
    max-width: 520px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .header-right {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-shrink: 0;
  }

  .live-room-stats {
    display: flex;
    justify-content: space-evenly;
    padding: 22px 40px;
    border-radius: 4px;
    background-color: #f4f9ff;
  }

  .stat-item {
    display: flex;
    align-items: center;
    flex-direction: column;
    justify-content: space-between;
    text-align: center;
    flex: 1;
    min-width: 0;
  }

  .stat-label {
    font-size: 16px;
    font-weight: 500;
    color: #484a4c;
  }

  .stat-value {
    font-size: 18px;
    font-weight: 500;
    color: #151719;
    margin: 14px 0 18px 0;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    max-width: 100%;
  }

  .stat-compare {
    display: flex;
    flex-direction: column;
    gap: 12px;
    font-size: 14px;
  }

  .compare-row {
    display: flex;
    align-items: center;
    column-gap: 16px;
    line-height: 1;
    min-width: 0;
  }

  .compare-label {
    display: flex;
    align-items: center;
    column-gap: 8px;
    flex-shrink: 0;
  }

  .compare-label .dot {
    display: block;
    width: 6px;
    height: 6px;
    border-radius: 50%;
  }

  .compare-label .yesterday {
    background-color: #1890ff;
  }

  .compare-label .this-week {
    background-color: #13c2c2;
  }

  .compare-label .this-month {
    background-color: #faad14;
  }

  .compare-row .label {
    color: #909399;
  }

  .compare-row .value {
    color: #606266;
    white-space: nowrap;
    overflow: hidden;
    max-width: 120px;
  }
</style>
