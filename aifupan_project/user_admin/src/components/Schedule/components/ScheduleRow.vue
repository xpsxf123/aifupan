<template>
  <div
    class="schedule-row"
    ref="rowRef"
    @mousemove="handleMouseMove"
    @mouseleave="handleMouseLeave"
    @click="handleClick"
  >
    <div
      v-if="showFixedAddButton"
      class="fixed-add-btn"
      :style="{ left: fixedAddButtonLeft }"
      @click.stop="handleFixedAddClick"
    >
      <el-icon class="fixed-add-icon"><CirclePlus /></el-icon>
      <span class="fixed-add-text">添加</span>
    </div>

    <!-- 排班块列表 -->
    <template v-for="(block, index) in blocks" :key="index">
      <!-- 如果是拖拽中的原始块，根据 draggingState 决定是否显示 -->
      <ScheduleBlock
        v-if="!isDragging || draggingState.block.id !== block.id"
        :data="block"
        :base-time="baseTime"
        :readonly="readonly"
        :day-date="dayDate"
        :role-name="roleName"
        :style-config="blockStyleConfig"
        :draggable="dragEnabled"
        :resizable="resizeEnabled"
        :clickable="addMode === 'fixed' && !readonly"
        @drag-start="handleDragStart"
        @resize-start="handleResizeStart"
        @delete="handleDeleteBlock"
        @click="handleBlockClick"
        @view-detail="handleViewDetail"
      />
    </template>

    <!-- 正在拖拽/调整大小的块 (Shadow) -->
    <!-- 注意：为了支持跨天视觉溢出，我们可以允许这个 Shadow 超出父容器 -->
    <!-- 但 ScheduleRow 可能是 overflow:hidden 的（为了截断普通块）。 -->
    <!-- 如果要实现跨天预览，最好是由父组件在顶层渲染一个全局的 DragLayer，或者在这里 emit 事件让父组件在相邻天渲染 Shadow。 -->
    <!-- 用户的需求是：超出部分在后一天同步出现块元素。这意味着我们需要实时通知父组件。 -->

    <ScheduleBlock
      v-if="isDragging && draggingState.currentRange"
      :data="draggingBlockDisplayData"
      class="dragging-block"
      :is-dragging="true"
      :base-time="baseTime"
      :readonly="true"
      :draggable="false"
      :resizable="false"
    />

    <!-- 跟随鼠标的虚线框 (Ghost) - 只有非拖拽状态才显示 -->
    <ScheduleGhost :visible="ghostVisible && !isDragging && addMode !== 'fixed'" :range="ghostRange" />
  </div>
</template>

<script setup>
  /**
   * @file src/components/Schedule/components/ScheduleRow.vue
   * @description 岗位排班行组件 (支持拖拽、调整大小、跨天检测)
   */
  import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
  import { CirclePlus } from '@element-plus/icons-vue'
  import ScheduleBlock from './ScheduleBlock.vue'
  import ScheduleGhost from './ScheduleGhost.vue'
  import {
    positionToTime,
    getCenteredAvailableRange,
    getDayMilliseconds,
    getResizeConstraints,
    checkCollision,
    TOTAL_MINUTES
  } from '../utils'

  const props = defineProps({
    blocks: {
      type: Array,
      default: () => []
    },
    baseTime: {
      type: Number,
      default: null
    },
    readonly: {
      type: Boolean,
      default: false
    },
    addMode: {
      type: String,
      default: 'follow'
    },
    disableAdd: {
      type: Boolean,
      default: false
    },
    dayDate: {
      type: String,
      default: ''
    },
    roleName: {
      type: String,
      default: ''
    },
    blockStyleConfig: {
      type: Object,
      default: () => ({})
    }
  })

  const emit = defineEmits([
    'add-block',
    'update-block',
    'cross-day-drag',
    'remove-block',
    'drag-preview',
    'fixed-add',
    'block-click',
    'view-detail'
  ])

  const rowRef = ref(null)
  const rowWidth = ref(0)
  const ghostVisible = ref(false)
  const ghostRange = ref({ start: 0, end: 0 })

  // 拖拽状态
  const isDragging = ref(false)
  const draggingState = ref({
    mode: 'move', // 'move' | 'resize-left' | 'resize-right'
    block: null, // 原始数据
    startX: 0, // 拖拽开始时的鼠标 X
    startRange: null, // 拖拽开始时的 { start, end } ms
    currentRange: null // 当前计算出的 { start, end } ms
  })

  // 将 props.blocks 转换为 { start, end } 格式
  const existingRanges = computed(() => {
    const base = Number(props.baseTime) || 0
    const DAY_MS = TOTAL_MINUTES * 60 * 1000
    const TIMESTAMP_THRESHOLD = 1e10

    const toRel = (raw) => {
      const n = raw === null || raw === undefined ? NaN : Number(raw)
      if (!Number.isFinite(n)) return 0
      if (base) {
        if (n > TIMESTAMP_THRESHOLD) return n - base
        return n
      }
      if (n > TIMESTAMP_THRESHOLD) return getDayMilliseconds(n)
      return n
    }

    return (props.blocks || [])
      .map((b) => {
        const times = Array.isArray(b?.times) ? b.times : []
        let start = toRel(times[0])
        let end = toRel(times[1])

        if (!(end > start)) end += DAY_MS

        if (props.addMode === 'fixed') {
          start = Math.max(0, start)
          end = Math.min(DAY_MS, end)
        }

        return { start, end, id: b?.id }
      })
      .filter((r) => r.end > r.start)
  })

  const dragEnabled = computed(() => !props.readonly && props.addMode !== 'fixed')
  const resizeEnabled = computed(() => !props.readonly && props.addMode !== 'fixed')
  const nowOffsetMs = computed(() => {
    const base = Number(props.baseTime) || 0
    if (!base) return 0
    // 修改为: 屏蔽当前时间 + 1小时 以内的区域
    const off = Date.now() + 3600 * 1000 - base
    if (off <= 0) return 0
    const DAY_MS = TOTAL_MINUTES * 60 * 1000
    return Math.min(off, DAY_MS)
  })

  const fixedNowOffsetMs = computed(() => {
    const base = Number(props.baseTime) || 0
    if (!base) return 0
    const off = Date.now() - base
    if (off <= 0) return 0
    const DAY_MS = TOTAL_MINUTES * 60 * 1000
    return Math.min(off, DAY_MS)
  })

  const showFixedAddButton = computed(() => {
    if (props.readonly) return false
    if (props.disableAdd) return false
    if (props.addMode !== 'fixed') return false
    return Boolean(fixedAddRange.value)
  })

  const fixedAddButtonMinDurationMs = computed(() => {
    const DAY_MS = TOTAL_MINUTES * 60 * 1000
    const BUTTON_WIDTH_PX = 60
    const GAP_PADDING_PX = 20
    const width = Number(rowWidth.value) || 0

    if (width <= 0) return 0
    return Math.ceil(((BUTTON_WIDTH_PX + GAP_PADDING_PX) / width) * DAY_MS)
  })

  const fixedAddRange = computed(() => {
    if (props.addMode !== 'fixed') return null
    const DAY_MS = TOTAL_MINUTES * 60 * 1000
    const DEFAULT_DURATION_MS = 2 * 60 * 60 * 1000
    const MIN_DURATION_MS = 60 * 1000
    const anchorStart = 23 * 60 * 60 * 1000
    const sortedRanges = [...existingRanges.value].sort((a, b) => a.start - b.start)
    const visualMinDurationMs = Math.max(MIN_DURATION_MS, fixedAddButtonMinDurationMs.value)
    let cursor = Math.min(fixedNowOffsetMs.value, anchorStart)

    for (const range of sortedRanges) {
      if (range.end <= cursor) continue

      if (cursor < range.start) {
        const gapEnd = Math.min(range.start, DAY_MS)
        if (gapEnd - cursor >= visualMinDurationMs) {
          return {
            start: cursor,
            end: Math.min(cursor + DEFAULT_DURATION_MS, gapEnd),
            dayMs: DAY_MS
          }
        }
      }

      if (range.start <= cursor && cursor < range.end) {
        cursor = range.end
      } else {
        cursor = Math.max(cursor, range.end)
      }

      if (!(cursor < DAY_MS)) return null
    }

    if (DAY_MS - cursor < visualMinDurationMs) return null

    return {
      start: cursor,
      end: Math.min(cursor + DEFAULT_DURATION_MS, DAY_MS),
      dayMs: DAY_MS
    }
  })

  const fixedAddButtonLeft = computed(() => {
    if (!fixedAddRange.value) return '0%'
    const DAY_MS = fixedAddRange.value.dayMs
    const center = (fixedAddRange.value.start + fixedAddRange.value.end) / 2
    return `${((center / DAY_MS) * 100).toFixed(4)}%`
  })

  // 构造用于显示的临时 Block 数据
  const draggingBlockDisplayData = computed(() => {
    if (!draggingState.value.block || !draggingState.value.currentRange) return null

    // 获取基准日期
    const baseDate = new Date(draggingState.value.block.times[0])
    baseDate.setHours(0, 0, 0, 0)
    const baseMs = baseDate.getTime()

    const newStart = baseMs + draggingState.value.currentRange.start
    const newEnd = baseMs + draggingState.value.currentRange.end

    return {
      ...draggingState.value.block,
      times: [newStart, newEnd]
    }
  })

  // 监听拖拽范围变化，实时通知父组件进行跨天预览
  watch(
    () => draggingState.value.currentRange,
    (newRange) => {
      if (isDragging.value && newRange) {
        const DAY_MS = 24 * 60 * 60 * 1000
        // 如果 start < 0 或 end > 24h，触发预览
        if (newRange.start < 0 || newRange.end > DAY_MS) {
          emit('drag-preview', {
            block: draggingState.value.block,
            range: newRange // 包含负数或超过24h的相对时间
          })
        } else {
          // 清除预览 (如果之前有)
          emit('drag-preview', null)
        }
      }
    },
    { deep: true }
  )

  const handleMouseMove = (e) => {
    if (!rowRef.value || props.readonly) return
    if (props.addMode === 'fixed') return

    const rect = rowRef.value.getBoundingClientRect()
    const x = e.clientX - rect.left
    const currentTimeMs = positionToTime(x, rect.width)

    // --- 拖拽/调整逻辑 ---
    if (isDragging.value) {
      const state = draggingState.value
      const rectWidth = rect.width

      // 计算鼠标相对于起始点的位移时间
      const diffX = e.clientX - state.startX
      const DAY_MS = 24 * 60 * 60 * 1000
      const diffMs = Math.round((diffX / rectWidth) * DAY_MS)

      // 排除自己
      const otherRanges = existingRanges.value.filter((r) => r.id !== state.block.id)

      // 获取基于原始位置的严格边界
      const { minStart, maxEnd } = getResizeConstraints(state.startRange, otherRanges, true)

      let newStart = state.startRange.start
      let newEnd = state.startRange.end

      if (state.mode === 'move') {
        newStart += diffMs
        newEnd += diffMs

        const duration = newEnd - newStart

        // Move 模式下的限制：整体移动，不能越界
        // 优先满足左边界
        if (newStart < minStart) {
          newStart = minStart
          newEnd = newStart + duration
        }
        // 再满足右边界
        if (newEnd > maxEnd) {
          newEnd = maxEnd
          newStart = newEnd - duration
        }
        // 如果 gap < duration，再次检查左边界 (防止右边界挤压导致左越界)
        if (newStart < minStart) {
          newStart = minStart
        }
      } else if (state.mode === 'resize-left') {
        newStart += diffMs
        if (newStart < minStart) newStart = minStart
        if (newEnd - newStart < 60 * 1000) newStart = newEnd - 60 * 1000
      } else if (state.mode === 'resize-right') {
        newEnd += diffMs
        if (newEnd > maxEnd) newEnd = maxEnd
        if (newEnd - newStart < 60 * 1000) newEnd = newStart + 60 * 1000
      }

      // 严格碰撞检测 (Double Check)
      if (checkCollision({ start: newStart, end: newEnd }, otherRanges)) {
        return
      }

      state.currentRange = { start: newStart, end: newEnd }

      return
    }

    // --- Ghost 逻辑 ---
    if (props.disableAdd) {
      ghostVisible.value = false
      return
    }
    if (currentTimeMs < nowOffsetMs.value) {
      ghostVisible.value = false
      return
    }
    const defaultDurationMs = 2 * 60 * 60 * 1000
    const range = getCenteredAvailableRange(currentTimeMs, defaultDurationMs, existingRanges.value)

    if (range) {
      ghostVisible.value = true
      ghostRange.value = range
    } else {
      ghostVisible.value = false
    }
  }

  const handleMouseLeave = () => {
    if (!isDragging.value) {
      ghostVisible.value = false
    }
  }

  const handleClick = () => {
    if (props.addMode === 'fixed') return
    if (props.disableAdd) return
    if (ghostRange.value.start < nowOffsetMs.value) return
    if (!isDragging.value && ghostVisible.value && ghostRange.value.start !== ghostRange.value.end) {
      emit('add-block', { ...ghostRange.value })
      ghostVisible.value = false
    }
  }

  const handleFixedAddClick = () => {
    if (!fixedAddRange.value) return
    emit('fixed-add', { range: { start: fixedAddRange.value.start, end: fixedAddRange.value.end } })
  }

  const handleBlockClick = (block) => {
    if (props.addMode !== 'fixed') return
    emit('block-click', block)
  }

  const handleDeleteBlock = (block) => {
    emit('remove-block', block)
  }

  const handleViewDetail = (payload) => {
    emit('view-detail', payload)
  }

  // --- Drag & Resize Events ---

  const handleDragStart = ({ originalEvent, block }) => {
    if (!dragEnabled.value) return
    startDrag(originalEvent, block, 'move')
  }

  const handleResizeStart = ({ originalEvent, block, direction }) => {
    if (!resizeEnabled.value) return
    startDrag(originalEvent, block, `resize-${direction}`)
  }

  const startDrag = (e, block, mode) => {
    isDragging.value = true
    draggingState.value.mode = mode
    draggingState.value.block = block
    draggingState.value.startX = e.clientX

    // 重构完整时间段逻辑
    let s, e_time

    // 如果是跨天数据且有原始时长信息，则恢复完整时间段作为拖拽基准
    if (block.isCrossDay && block.originalTimeCount && block.crossDayOffset !== undefined) {
      // 恢复原始开始时间 = 当前片段开始时间 - 偏移量
      // 注意：block.times[0] 是绝对时间戳
      const currentSegmentStart = block.times[0]
      const originalStart = currentSegmentStart - block.crossDayOffset

      // 恢复原始结束时间 = 原始开始时间 + 原始总时长
      const originalEnd = originalStart + block.originalTimeCount * 60 * 1000

      // 转换为相对于当天的毫秒数 (用于 ScheduleRow 内部计算)
      // ScheduleRow 的 getDayMilliseconds 对于 > 24h 的值会取模
      // 但是我们需要保持相对值，以便计算 diff
      // 这里的逻辑有点 tricky。draggingState.startRange 应该存什么？
      // 存绝对时间戳比较安全，或者存相对于“当前天”的线性偏移值。

      // 我们使用相对于当前天 00:00 的偏移值。
      // 例如 Day 2 的 01:00 (offset=23h, dur=2h)。
      // currentSegmentStart = Day2 00:00.
      // originalStart = Day1 23:00.
      // relative to Day 2 00:00 -> -1h.

      const currentDayBase = new Date(block.times[0])
      currentDayBase.setHours(0, 0, 0, 0)
      const baseTime = currentDayBase.getTime()

      s = originalStart - baseTime
      e_time = originalEnd - baseTime
    } else {
      s = getDayMilliseconds(block.times[0])
      e_time = getDayMilliseconds(block.times[1])
    }

    draggingState.value.startRange = { start: s, end: e_time }
    draggingState.value.currentRange = { start: s, end: e_time }

    window.addEventListener('mousemove', handleWindowMouseMove)
    window.addEventListener('mouseup', handleWindowMouseUp)
  }

  const handleWindowMouseMove = (e) => {
    handleMouseMove(e)
  }

  const handleWindowMouseUp = () => {
    if (isDragging.value) {
      const newBlock = draggingBlockDisplayData.value

      // 拖拽结束：通知父组件清理预览
      emit('drag-preview', null)

      // 检查跨天
      const DAY_MS = 24 * 60 * 60 * 1000
      const start = draggingState.value.currentRange.start
      const end = draggingState.value.currentRange.end

      if (start < 0 || end > DAY_MS || draggingState.value.block.isCrossDay) {
        // 触发跨天逻辑 (提交数据)
        emit('cross-day-drag', {
          roleType: draggingState.value.block.roleType,
          originalBlock: draggingState.value.block,
          newRange: { start, end }
        })
      } else {
        // 正常更新
        const oldS = getDayMilliseconds(draggingState.value.block.times[0])
        const oldE = getDayMilliseconds(draggingState.value.block.times[1])

        if (newBlock && (start !== oldS || end !== oldE)) {
          emit('update-block', newBlock)
        }
      }

      isDragging.value = false
      draggingState.value = { mode: 'move', block: null, startX: 0, startRange: null, currentRange: null }
    }

    window.removeEventListener('mousemove', handleWindowMouseMove)
    window.removeEventListener('mouseup', handleWindowMouseUp)
  }

  onUnmounted(() => {
    window.removeEventListener('mousemove', handleWindowMouseMove)
    window.removeEventListener('mouseup', handleWindowMouseUp)
  })

  let rowResizeObserver = null

  onMounted(() => {
    const updateRowWidth = () => {
      rowWidth.value = rowRef.value?.clientWidth || 0
    }

    updateRowWidth()

    if (typeof ResizeObserver !== 'undefined' && rowRef.value) {
      rowResizeObserver = new ResizeObserver(() => updateRowWidth())
      rowResizeObserver.observe(rowRef.value)
    }
  })

  onUnmounted(() => {
    if (rowResizeObserver) {
      rowResizeObserver.disconnect()
      rowResizeObserver = null
    }
  })
</script>

<style scoped lang="scss">
  .schedule-row {
    position: relative;
    height: 72px; /* 每行高度，兼容较长岗位名称两行展示 */
    user-select: none; /* 防止拖拽时选中文字 */
    overflow: hidden; /* 确保拖拽或跨天时，超出部分被隐藏，实现"变短"效果 */
  }

  .fixed-add-btn {
    position: absolute;
    top: 50%;
    transform: translate(-50%, -50%);
    display: inline-flex;
    align-items: center;
    gap: 4px;
    padding: 2px 8px;
    /* padding: 2px 8px; */
    border-radius: 999px;
    background: rgba(103, 194, 58, 0.12);
    color: var(--color-success);
    font-size: 12px;
    white-space: nowrap;
    cursor: pointer;
    z-index: 12;
  }

  .fixed-add-icon {
    font-size: 14px;
  }

  .dragging-block {
    z-index: 100; /* 确保在最上层 */
  }
</style>
