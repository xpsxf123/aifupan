/**
 * @file src/components/Schedule/formatter.js
 * @description 统一的数据格式化处理工具
 */

import { getDayMilliseconds } from './utils'

/**
 * 格式化排班数据
 * @param {Array} rawData 原始后端数据或Mock数据
 * @param {Object} options 配置项
 * @returns {Array} 格式化后的数据，保证UI渲染所需的字段完整
 */
export const formatScheduleData = (rawData, options = {}) => {
  if (!Array.isArray(rawData)) return []

  const roleColors =
    Array.isArray(options.roleColors) && options.roleColors.length
      ? options.roleColors
      : ['#409eff', '#67c23a', '#b37feb']

  const formattedData = rawData.map((day) => {
    // 确保 data 字段存在
    const dayData = Array.isArray(day.data) ? day.data : []

    // 解析日期基准时间
    let baseTime = 0
    if (day.dateTime) {
      const today = new Date()
      // 假设 dateTime 格式为 "MM/DD"
      const parts = day.dateTime.split('/')
      if (parts.length === 2) {
        today.setMonth(parseInt(parts[0]) - 1)
        today.setDate(parseInt(parts[1]))
        today.setHours(0, 0, 0, 0)
        baseTime = today.getTime()
      }
    }

    const formattedDayData = dayData.map((role, roleIndex) => {
      const roleColor = roleColors[roleIndex % roleColors.length]
      // 确保角色下的排班数据存在
      const blocks = Array.isArray(role.data) ? role.data : []

      const formattedBlocks = blocks.map((block, index) => {
        // 1. 确保 ID 存在
        const id = block.id || `${Date.now()}_${Math.random().toString(36).substr(2, 9)}_${index}`

        // 2. 补全时间信息
        let times = block.times || []
        let startTime = block.startTime
        let timeCount = block.timeCount

        // 情况A: 只有 startTime 和 timeCount -> 计算 times
        if ((!times || times.length !== 2) && startTime !== undefined && timeCount !== undefined) {
          const endTime = startTime + timeCount * 60 * 1000
          times = [startTime, endTime]
        }

        // 情况B: 只有 times -> 计算 startTime 和 timeCount
        if (times && times.length === 2) {
          if (startTime === undefined) startTime = times[0]
          if (timeCount === undefined) {
            timeCount = (times[1] - times[0]) / (1000 * 60)
          }
        }

        // 情况C: 都没有 (异常数据) -> 默认值
        if (times.length !== 2) {
          // 默认为当天 09:00 - 10:00
          const defaultStart = baseTime + 9 * 60 * 60 * 1000
          const defaultEnd = baseTime + 10 * 60 * 60 * 1000
          times = [defaultStart, defaultEnd]
          startTime = defaultStart
          timeCount = 60
        }

        // 3. 补全其他 UI 必要字段
        const name = block.name || role.name || '未命名'
        // memberId 用于判断是否已分配人员，如果不存在则视为无效排班（灰色显示）
        const memberId = block.memberId || block.userId || null

        // 4. 跨天相关字段初始化
        // 如果是跨天切割后的片段，通常会带有 originalTimeCount 等字段，这里仅做基本校验
        const isCrossDay = block.isCrossDay || false
        // 上播记录标识
        const isRecord = block.isRecord || false

        return {
          ...block,
          id,
          name,
          memberId,
          color: block.color || roleColor,
          textColor: block.textColor || '#fff',
          startTime,
          timeCount,
          times,
          isCrossDay,
          isRecord,
          // 确保 roleType 存在于 block 中，方便拖拽时回溯
          roleType: block.roleType || role.type
        }
      })

      // 排序：按开始时间
      formattedBlocks.sort((a, b) => a.times[0] - b.times[0])

      return {
        ...role,
        data: formattedBlocks
      }
    })

    return {
      ...day,
      data: formattedDayData
    }
  })

  return formattedData
}
