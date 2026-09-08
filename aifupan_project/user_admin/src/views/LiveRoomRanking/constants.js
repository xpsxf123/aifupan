/**
 * @file constants.js
 * @description 直播间排班页面常量配置
 */

// 表格列配置
export const tableColumns = [
  { label: '直播间', prop: 'name', minWidth: 200, slotName: 'liveRoom', align: 'center' },
  { label: '平台', prop: 'platform', minWidth: 100, slotName: 'platform', align: 'center' },
  { label: '小组', prop: 'group', minWidth: 120, align: 'center' },
  { label: '今日排班', prop: 'todaySchedule', minWidth: 200, slotName: 'todaySchedule', align: 'center' },
  { label: '明日排班', prop: 'tomorrowSchedule', minWidth: 200, slotName: 'tomorrowSchedule', align: 'center' },
  {
    label: '本周排班',
    prop: 'weekSchedule',
    minWidth: 100,
    slotName: 'thisWeekSchedule',
    align: 'center',
    fixed: 'right'
  },
  {
    label: '下周排班',
    prop: 'nextWeekSchedule',
    minWidth: 100,
    slotName: 'nextWeekSchedule',
    align: 'center',
    fixed: 'right'
  },
  {
    label: '本月排班',
    prop: 'monthSchedule',
    minWidth: 100,
    slotName: 'thisMonthSchedule',
    align: 'center',
    fixed: 'right'
  }
]

// 表单配置
export const formConfig = {
  type: 'drawer',
  width: 600,
  add: [{ slotRow: true, slotName: 'scheduleForm' }]
}
