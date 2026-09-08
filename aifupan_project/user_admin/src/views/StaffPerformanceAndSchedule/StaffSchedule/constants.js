/**
 * @file constants.js
 * @description 个人排班列表页常量配置
 */

// 表格列配置
export const tableColumns = [
  { label: '人员', prop: 'staff', minWidth: 160, slotName: 'staff' },
  { label: '岗位', prop: 'positionName', width: 120, align: 'center' },
  { label: '直播间', prop: 'roomText', minWidth: 220, slotName: 'room', align: 'center' },
  { label: '今日排班', prop: 'todaySchedules', minWidth: 260, slotName: 'todaySchedules' },
  { label: '明日排班', prop: 'tomorrowSchedules', minWidth: 260, slotName: 'tomorrowSchedules' },
  { label: '操作', prop: 'action', width: 100, fixed: 'right', slotName: 'action', align: 'center' }
]
