/**
 * @file constants.js
 * @description 直播间业绩页面常量配置
 */

export const unitConfig = [
  { value: 10000, label: 'w' },
  { value: 100000000, label: '亿' }
]

export const liveRoomStatsConfig = [
  {
    label: '场次',
    prop: 'session',
    unit: '',
    yesterday: 'sessionYesterday',
    week: 'sessionWeek',
    month: 'sessionMonth'
  },
  { label: '场观', prop: 'views', unit: 'auto', yesterday: 'viewsYesterday', week: 'viewsWeek', month: 'viewsMonth' },
  { label: '销售', prop: 'sales', unit: 'auto', yesterday: 'salesYesterday', week: 'salesWeek', month: 'salesMonth' },
  {
    label: '退款',
    prop: 'refund',
    unit: 'auto',
    yesterday: 'refundYesterday',
    week: 'refundWeek',
    month: 'refundMonth'
  },
  {
    label: '净销售',
    prop: 'netSales',
    unit: 'auto',
    yesterday: 'netSalesYesterday',
    week: 'netSalesWeek',
    month: 'netSalesMonth'
  },
  {
    label: '投放',
    prop: 'adSpend',
    unit: 'auto',
    yesterday: 'adSpendYesterday',
    week: 'adSpendWeek',
    month: 'adSpendMonth'
  }
]

// Mock Trend Data (Initially Reactive, but exported as initial state)
export const initialTrendChartData = {
  xData: ['1/13', '1/14', '1/15', '1/16', '1/17', '1/18', '1/19', '1/20', '1/21', '1/22'],
  yData: [1000, 2000, 1500, 3000, 2500, 4000, 3500, 2000, 1000, 5000]
}

// Session Tab Config
export const sessionSearchConfig = [
  { label: '主播', prop: 'anchorName', type: 'input', placeholder: '请输入主播名' },
  { label: '直播日期', prop: 'dateRange', type: 'daterange' }
]

export const sessionTableColumns = [
  { label: '日期', prop: 'date', width: 120 },
  {
    label: '直播时间段',
    prop: 'timeRange',
    width: 160,
    slotName: 'timeRange',
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
  {
    label: '人员',
    prop: 'staffList',
    minWidth: 200,
    slotName: 'staff',
    formatter: (row) => {
      const list = row?.staffList
      if (Array.isArray(list) && list.length) {
        return list
          .map((x) => {
            const positionName = x?.positionName || ''
            const employeeName = x?.employeeName || ''
            const prefix = positionName ? `${positionName}:` : ''
            const text = `${prefix}${employeeName}`
            return text === ':' ? '' : text
          })
          .filter(Boolean)
          .join('、')
      }
      if (list && typeof list === 'object') {
        const positionName = list?.positionName || ''
        const employeeName = list?.employeeName || ''
        const prefix = positionName ? `${positionName}:` : ''
        const text = `${prefix}${employeeName}`
        return text === ':' ? '' : text
      }
      return ''
    }
  },
  { label: '场观', prop: 'viewCount', width: 100, align: 'left', slotName: 'viewCount' },
  { label: '销售额', prop: 'salesRevenue', width: 120, align: 'left', slotName: 'salesRevenue' },
  { label: '退款', prop: 'refund', width: 100, align: 'left', slotName: 'refund' },
  { label: '净销售额', prop: 'netSales', width: 120, align: 'left', slotName: 'netSales' },
  { label: '投放', prop: 'investment', width: 100, align: 'left', slotName: 'investment' },
  { label: 'ROI', prop: 'roi', width: 90, align: 'left', slotName: 'roi' },
  { label: '数据来源', prop: 'source', width: 100, align: 'center' }
]

// Shared / Summary Tab Config
export const searchConfig = [
  { label: '主播', prop: 'anchorName', type: 'input', placeholder: '请输入主播名' },
  { label: '直播日期', prop: 'dateRange', type: 'daterange' }
]

export const performanceSearchConfig = searchConfig

export const tableColumns = [
  { label: '日期', prop: 'statsDate', width: 120 },
  {
    label: '场次',
    prop: 'scheduleCount',
    width: 140,
    formatter: (row) => `${row.scheduleCount || 0} (${row.liveDurationMinutesStr || '-'})`
  },
  {
    label: '人员',
    prop: 'staffList',
    minWidth: 200,
    slotName: 'staff',
    formatter: (row) => {
      const list = row?.staffList
      if (Array.isArray(list) && list.length) {
        return list
          .map((x) => {
            const positionName = x?.positionName || ''
            const employeeName = x?.employeeName || ''
            const scheduleCount = Number(x?.scheduleCount)
            const countText = Number.isFinite(scheduleCount) && scheduleCount > 0 ? `(${scheduleCount}场)` : ''
            const prefix = positionName ? `${positionName}:` : ''
            const text = `${prefix}${employeeName}${countText}`
            return text === ':' ? '' : text
          })
          .filter(Boolean)
          .join('、')
      }
      if (list && typeof list === 'object') {
        const positionName = list?.positionName || ''
        const employeeName = list?.employeeName || ''
        const scheduleCount = Number(list?.scheduleCount)
        const countText = Number.isFinite(scheduleCount) && scheduleCount > 0 ? `(${scheduleCount}场)` : ''
        const prefix = positionName ? `${positionName}:` : ''
        const text = `${prefix}${employeeName}${countText}`
        return text === ':' ? '' : text
      }
      return ''
    }
  },
  { label: '场观', prop: 'viewCount', width: 120, align: 'left' },
  { label: '销售额', prop: 'salesRevenue', width: 150, align: 'left' },
  { label: '退款', prop: 'refund', width: 120, align: 'left' },
  { label: '净销售额', prop: 'netSales', width: 120, align: 'left' },
  { label: '投放', prop: 'investment', width: 120, align: 'left' },
  { label: 'ROI', prop: 'roi', width: 100, align: 'left' }
]

export const performanceTableColumns = tableColumns

export const customActions = [
  {
    label: '查看场次明细',
    type: 'primary',
    link: true,
    command: 'viewDetail',
    size: 'default'
  }
]
