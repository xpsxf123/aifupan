/**
 * @file src/components/Schedule/utils.js
 * @description 工具函数：时间与像素位置转换、碰撞检测
 */

// 一天总分钟数
export const TOTAL_MINUTES = 24 * 60
const DAY_MS = TOTAL_MINUTES * 60 * 1000

/**
 * 获取时间对应的时间戳（当天0点为基准）
 * @param {Date|number} date
 * @returns {number} 当天已过去的毫秒数
 */
export const getDayMilliseconds = (date) => {
  const d = new Date(date)
  return (d.getHours() * 60 * 60 + d.getMinutes() * 60 + d.getSeconds()) * 1000 + d.getMilliseconds()
}

/**
 * 将时间转换为百分比位置
 * @param {number} startTime timestamp or ms from 00:00
 * @param {number} endTime timestamp or ms from 00:00
 * @param {number} [baseTime] 可选：基准时间戳 (用于计算相对偏移)
 * @returns {Object} { left: string, width: string }
 */
export const timeToPosition = (startTime, endTime, baseTime = null) => {
  let relStart = startTime
  let relEnd = endTime

  // 如果提供了基准时间，强制使用相对时间计算
  if (baseTime !== null) {
    relStart = startTime - baseTime
    relEnd = endTime - baseTime
  } else {
    // 原有逻辑：尝试推断
    // 阈值：大约 115 天。绝对时间戳通常远大于此 (e.g. 1.7e12)。
    // 拖拽时的相对时间通常在 -24h 到 48h 之间，远小于此。
    const TIMESTAMP_THRESHOLD = 1e10

    const isStartTimestamp = startTime > TIMESTAMP_THRESHOLD
    const isEndTimestamp = endTime > TIMESTAMP_THRESHOLD

    if (isStartTimestamp) relStart = getDayMilliseconds(startTime)
    if (isEndTimestamp) relEnd = getDayMilliseconds(endTime)

    // 处理时间戳跨天情况：
    // 如果原始输入是时间戳（即 > 阈值），且 end > start（时间流向正确），
    // 但转换后的 relEnd < relStart（例如 01:00 < 23:00），
    // 说明 relEnd 实际上是次日的时间，需要加 24h。
    if (isStartTimestamp && isEndTimestamp && endTime > startTime && relEnd < relStart) {
      relEnd += DAY_MS
    }
  }

  // 计算 Left (相对于当天 00:00 的百分比，允许 < 0 或 > 100)
  const left = (relStart / DAY_MS) * 100

  // 计算 Width
  // 用户需求：当跨天时，"在 left 变大的过程中缩小 width"。
  // 这意味着我们应该只计算在当天（0~24h）范围内的宽度。
  // 如果 relEnd 超过 24h，宽度应该被截断。

  // 1. 取当天的结束边界 DAY_MS
  // 2. 计算有效结束点：min(relEnd, DAY_MS)
  // 3. 宽度 = 有效结束点 - 开始点
  // 4. 如果开始点也超过了 DAY_MS，宽度会变成负数，需要置 0

  const effectiveEnd = Math.min(relEnd, DAY_MS)
  let widthVal = effectiveEnd - relStart

  if (widthVal < 0) widthVal = 0

  const width = (widthVal / DAY_MS) * 100

  return {
    left: `${left.toFixed(4)}%`,
    width: `${width.toFixed(4)}%`
  }
}

/**
 * 像素位置转换为时间
 * @param {number} x 鼠标在容器内的X坐标
 * @param {number} totalWidth 容器总宽度
 * @returns {number} 当天已过去的毫秒数 (可以是负数或 > 24h，用于跨天判断)
 */
export const positionToTime = (x, totalWidth) => {
  const ratio = x / totalWidth
  return Math.round(ratio * DAY_MS)
}

/**
 * 格式化时间 HH:mm
 * @param {number} ms 当天已过去的毫秒数 或 timestamp
 */
export const formatTime = (ms) => {
  const d = new Date(ms)

  if (ms < DAY_MS) {
    // 相对时间
    let normalizedMs = ms
    // 处理负数时间 (e.g. -60min -> 23:00)
    while (normalizedMs < 0) normalizedMs += DAY_MS
    while (normalizedMs >= DAY_MS) normalizedMs -= DAY_MS

    // 绝对时间戳判断
    if (ms > 365 * 24 * 3600 * 1000) {
      const h = d.getHours().toString().padStart(2, '0')
      const m = d.getMinutes().toString().padStart(2, '0')
      return `${h}:${m}`
    }

    const hours = Math.floor(normalizedMs / (1000 * 60 * 60))
    const minutes = Math.floor((normalizedMs % (1000 * 60 * 60)) / (1000 * 60))
    return `${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}`
  }

  const h = d.getHours().toString().padStart(2, '0')
  const m = d.getMinutes().toString().padStart(2, '0')
  return `${h}:${m}`
}

/**
 * 碰撞检测
 * @param {Object} range { start: number, end: number } 待检测的时间段 (ms from 00:00)
 * @param {Array} existingRanges [{ start: number, end: number }] 已存在的时间段
 * @returns {boolean} 是否碰撞
 */
export const checkCollision = (range, existingRanges) => {
  return existingRanges.some((item) => {
    return range.start < item.end && item.start < range.end
  })
}

/**
 * 获取居中的可用时间块（用于Ghost跟随鼠标）
 * @param {number} centerMs 鼠标位置对应的时间中心点
 * @param {number} durationMs 目标时长毫秒数
 * @param {Array} existingRanges 已存在的时间段
 * @returns {Object|null} { start, end } or null if invalid
 */
export const getCenteredAvailableRange = (centerMs, durationMs, existingRanges) => {
  const sortedRanges = [...existingRanges].sort((a, b) => a.start - b.start)

  // 检查 centerMs 是否落在某个区间内
  const inRange = sortedRanges.some((r) => centerMs >= r.start && centerMs < r.end)
  if (inRange) return null

  let gapStart = 0
  let gapEnd = DAY_MS

  const prevRange = [...sortedRanges].reverse().find((r) => r.end <= centerMs)
  if (prevRange) gapStart = prevRange.end

  const nextRange = sortedRanges.find((r) => r.start >= centerMs)
  if (nextRange) gapEnd = nextRange.start

  if (gapEnd - gapStart < 60 * 1000) return null

  const halfDuration = durationMs / 2

  let start = centerMs - halfDuration
  let end = centerMs + halfDuration

  const maxDuration = gapEnd - gapStart
  const actualDuration = Math.min(durationMs, maxDuration)

  if (start < gapStart) {
    start = gapStart
    end = start + actualDuration
  } else if (end > gapEnd) {
    end = gapEnd
    start = end - actualDuration
  }

  if (actualDuration < durationMs) {
    start = gapStart
    end = gapEnd
  } else {
    start = centerMs - actualDuration / 2
    end = centerMs + actualDuration / 2

    if (start < gapStart) {
      start = gapStart
      end = start + actualDuration
    } else if (end > gapEnd) {
      end = gapEnd
      start = end - actualDuration
    }
  }

  return { start, end }
}

export const getFirstAvailableRange = (existingRanges, minDurationMs) => {
  const sortedRanges = [...existingRanges].sort((a, b) => a.start - b.start)
  let cursor = 0
  for (const r of sortedRanges) {
    if (r.start - cursor >= minDurationMs) return { start: cursor, end: r.start }
    cursor = Math.max(cursor, r.end)
  }
  if (DAY_MS - cursor >= minDurationMs) return { start: cursor, end: DAY_MS }
  return null
}

/**
 * 获取调整大小的严格限制 (防止跳过 Block)
 * @param {Object} originalRange { start, end } 调整前的原始区间
 * @param {Array} existingRanges 其他已存在的时间段
 * @param {boolean} allowCrossDay 是否允许跨天
 * @returns {Object} { minStart, maxEnd } 允许的调整范围边界
 */
export const getResizeConstraints = (originalRange, existingRanges, allowCrossDay = true) => {
  const sortedRanges = [...existingRanges].sort((a, b) => a.start - b.start)

  // 找到原始区间左边最近的 block
  const prev = [...sortedRanges].reverse().find((r) => r.end <= originalRange.start)
  const minStart = prev ? prev.end : allowCrossDay ? -Infinity : 0

  // 找到原始区间右边最近的 block
  const next = sortedRanges.find((r) => r.start >= originalRange.end)
  const maxEnd = next ? next.start : allowCrossDay ? Infinity : DAY_MS

  return { minStart, maxEnd }
}

/**
 * 获取拖拽/调整大小后的有效区间
 * @param {number} newStartMs 期望的新开始时间
 * @param {number} newEndMs 期望的新结束时间
 * @param {Object} originalRange 原始区间 (用于确定拓扑关系)
 * @param {Array} existingRanges 其他已存在的时间段（排除自己）
 * @param {boolean} allowCrossDay 是否允许跨天
 * @returns {Object} { start, end } 修正后的有效区间
 */
export const getValidRange = (newStartMs, newEndMs, originalRange, existingRanges, allowCrossDay = true) => {
  // 获取基于原始位置的严格边界
  const { minStart, maxEnd } = getResizeConstraints(originalRange, existingRanges, allowCrossDay)

  let start = newStartMs
  let end = newEndMs
  const duration = end - start

  // 添加一个缓冲时间，例如 1 分钟，或者在像素转换时已经处理了
  // 如果手柄有宽度，视觉上可能重叠，但逻辑上不能。
  // 我们不需要在这里减去手柄宽度，因为 start/end 是时间概念。
  // 只要时间不重叠，就不会逻辑重叠。

  // 1. 限制 start
  if (start < minStart) {
    start = minStart
    // 如果是移动操作，end 也要跟着变
    // 但我们不知道是移动还是调整大小。
    // ScheduleRow 传递的是已经计算好的 newStart/newEnd。
    // 如果是 move，newEnd = newStart + duration
    // 如果我们这里改变了 start，必须同时改变 end 以保持 duration (假设调用者希望保持 duration)

    // 这里的逻辑有点模糊。如果不区分 move/resize，我们只能做 clamp。
    // 但是 clamp start 时如果不 clamp end，duration 就会变。
    // 如果是 move，duration 必须不变。
    // 如果是 resize-left，duration 会变，end 不变。

    // 鉴于此函数无法区分操作类型，我们修改 API，让调用者处理 clamp 后的联动？
    // 或者传入 mode？

    // 更好的做法：ScheduleRow 负责 move 的联动，这里只负责 clamp 绝对边界。
    // 但是 ScheduleRow 里的逻辑是先算位移，再调这个函数。
    // 如果这个函数 clamp 了 start，ScheduleRow 不知道 duration 变了没有。

    // 我们在 ScheduleRow 里已经分别处理了 move/resize 的联动逻辑。
    // 这里只需要负责返回合法的 start/end。
    // 但是，对于 Move，如果 start < minStart，start = minStart。那 end 呢？
    // 如果只 clamp start，end 还是原来的值，duration 变小了？这不对。Move 不能改变 duration。

    // 所以我们还是需要在 ScheduleRow 里做更细致的处理。
    // 或者这里假设如果是 Move，传入的 end - start 已经是固定的。
    // 如果是 Resize，只有一端变了。

    // 我们简化 getValidRange，只做单纯的 clamp。
    // 具体的联动逻辑由 ScheduleRow 处理。
  }

  if (start < minStart) start = minStart
  if (end > maxEnd) end = maxEnd

  // 再次检查 start > end 的情况 (防止过度 clamp)
  if (start > end) {
    // 如果因为 clamp 导致 start > end，说明空间不足以容纳当前 duration
    // 或者操作越界。
    // 我们优先保留哪一端？
    // 通常不应该发生，因为 minStart <= maxEnd 是前提。
    // 除非 duration > (maxEnd - minStart)

    // 如果 duration 过大，放不下，我们强制压缩
    end = start // 坍缩
  }

  return { start, end, minStart, maxEnd }
}

/**
 * 检查排班时间重叠
 * @param {Array} blocks 排班块列表 (需包含 times: [start, end])
 * @returns {Object} { hasOverlap: boolean, overlaps: Array }
 */
export const checkOverlaps = (blocks) => {
  const overlaps = new Set()
  if (!Array.isArray(blocks) || blocks.length < 2) {
    return { hasOverlap: false, overlaps: [] }
  }

  // 按开始时间排序副本
  const sorted = [...blocks].sort((a, b) => a.times[0] - b.times[0])

  for (let i = 0; i < sorted.length; i++) {
    const current = sorted[i]
    for (let j = i + 1; j < sorted.length; j++) {
      const next = sorted[j]
      // 如果下一个块的开始时间 >= 当前块的结束时间，说明不再重叠（因为已排序）
      if (next.times[0] >= current.times[1]) break

      // 发现重叠
      overlaps.add(current)
      overlaps.add(next)
    }
  }

  return {
    hasOverlap: overlaps.size > 0,
    overlaps: Array.from(overlaps)
  }
}

/**
 * 通过 ID 在排班数据中查找目标块引用
 * @param {Array} scheduleData 整个排班数据结构
 * @param {string|number} id 目标 Block ID
 * @returns {Object|null} Block 对象引用
 */
export const findBlockById = (scheduleData, id) => {
  if (!Array.isArray(scheduleData)) return null

  for (const day of scheduleData) {
    if (!day.data) continue
    for (const roleRow of day.data) {
      if (!roleRow.data) continue
      const block = roleRow.data.find((b) => b.id === id)
      if (block) return block
    }
  }
  return null
}

/**
 * 获取当日指定ID的所有排班数据
 * @param {Object} dayData 当日排班数据
 * @param {string|number} id 目标ID (人员ID)
 * @returns {Array} Block数组
 */
export const getDayBlocksById = (dayData, id) => {
  const blocks = []
  if (!dayData || !dayData.data) return blocks

  dayData.data.forEach((roleRow) => {
    if (Array.isArray(roleRow.data)) {
      roleRow.data.forEach((block) => {
        if (block.id === id) {
          blocks.push(block)
        }
      })
    }
  })
  return blocks
}

/**
 * 判断同一个id在当日下多个排班是否存在排班重叠
 * @param {Object} dayData 当日排班数据
 * @param {string|number} id 目标ID
 * @returns {boolean} 是否重叠
 */
export const checkDayOverlapById = (dayData, id) => {
  const blocks = getDayBlocksById(dayData, id)
  const { hasOverlap } = checkOverlaps(blocks)
  return hasOverlap
}
