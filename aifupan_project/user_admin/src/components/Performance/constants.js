/**
 * @file constants.js
 * @description 业绩页面常量配置 (Shared)
 */
// 1. 数据趋势图表默认数据
export const initialTrendChartData = {
  xData: [],
  yData: []
}

// 2. 按天维度（数据详情）配置
export const performanceSearchConfig = [
  { label: '直播间', prop: 'liveRoomName', type: 'input', placeholder: '请输入直播间名称' },
  { label: '直播日期', prop: 'dateRange', type: 'daterange' }
]

export const performanceTableColumns = [
  { label: '日期', prop: 'statsDate', width: 120 },
  {
    label: '场次',
    prop: 'scheduleCount',
    width: 140,
    formatter: (row) => `${row.scheduleCount || 0} (${row.duration || '-'})`
  },
  {
    label: '直播间',
    prop: 'liveRoomList',
    minWidth: 220,
    slotName: 'liveRooms',
    align: 'center',
    formatter: (row) => {
      const liveRoomList = row?.liveRoomList
      if (Array.isArray(liveRoomList) && liveRoomList.length) {
        return liveRoomList
          .map((x) => x?.liveRoomName || x?.anchorName)
          .filter(Boolean)
          .join('、')
      }
      if (liveRoomList && typeof liveRoomList === 'object') {
        return liveRoomList?.liveRoomName || liveRoomList?.anchorName || ''
      }
      return ''
    }
  },
  { label: '场观', prop: 'viewCount', width: 120 },
  { label: '销售额', prop: 'salesRevenue', width: 140 },
  { label: '退款', prop: 'refund', width: 120 },
  { label: '净销售额', prop: 'netSales', width: 140 },
  { label: '投放', prop: 'investment', width: 120 },
  { label: 'ROI', prop: 'roi', width: 100 }
]

export const customActions = [
  {
    label: '查看场次明细',
    type: 'primary',
    link: true,
    command: 'viewDetail'
  }
]

// 3. 按班次维度（直播场次）配置
export const sessionSearchConfig = [
  { label: '直播间', prop: 'liveRoomName', type: 'input', placeholder: '请输入直播间名称' },
  { label: '直播日期', prop: 'dateRange', type: 'daterange' }
]

export const sessionTableColumns = [
  { label: '日期', prop: 'date', width: 120 },
  {
    label: '直播时间段',
    prop: 'timeRange',
    width: 160,
    slotName: 'timeRange',
    align: 'center',
    formatter: (row) => {
      const startTime = row?.startTime
      const endTime = row?.endTime
      if (!startTime || !endTime) return ''
      const s = String(startTime).slice(11, 16)
      const e = String(endTime).slice(11, 16)
      const range = `${s}-${e}`
      const durationStr = row?.durationStr
      if (durationStr && durationStr !== '-') return `${range} ${durationStr}`
      return range
    }
  },
  { label: '直播间', prop: 'liveRoomName', minWidth: 180, showOverflowTooltip: true, align: 'center' },
  { label: '场观', prop: 'viewCount', width: 120 },
  { label: '销售额', prop: 'salesRevenue', width: 140 },
  { label: '退款', prop: 'refund', width: 120 },
  { label: '净销售额', prop: 'netSales', width: 140 },
  { label: '投放', prop: 'investment', width: 120 },
  { label: 'ROI', prop: 'roi', width: 100 },
  { label: '数据来源', prop: 'source', width: 100, align: 'center' }
]
