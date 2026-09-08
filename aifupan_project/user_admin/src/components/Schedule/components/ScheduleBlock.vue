<template>
  <div
    class="schedule-block"
    :class="{
      'is-dragging': isDragging,
      'is-record': data.isRecord,
      'is-readonly': readonly,
      'is-draggable': draggable && isClickable,
      'is-clickable': isClickable
    }"
    :style="style"
    @mousedown.stop="draggable && isClickable && handleMouseDown($event)"
    @click.stop="handleClick"
  >
    <!-- 左侧调整手柄 -->
    <div class="resize-handle left" v-if="isClickable && resizable" @mousedown.stop="handleResizeStart($event, 'left')">
      <i class="handle-icon"></i>
    </div>
    <el-popover
      placement="top"
      :width="'auto'"
      trigger="hover"
      :disabled="popoverDisabled || isDragging"
      popper-class="schedule-dark-popover"
      :offset="10"
      :show-arrow="true"
    >
      <div class="popover-content">
        <span class="mr-10">{{ displayName }}</span>
        <span class="mr-10">{{ dateTimeFormat(popoverTimeCount) }}</span>
        <span class="mr-10">{{ timeRange }}</span>
        <el-button type="primary" size="small" round @click.stop="handleViewDetail" class="detail-btn"
          >排班详情</el-button
        >
      </div>
      <template #reference>
        <div class="block-content">
          <div class="block-title">{{ displayName }}</div>
          <div class="block-time">{{ timeRange }}</div>
        </div>
      </template>
    </el-popover>

    <!-- 删除按钮 -->
    <el-popconfirm v-if="isClickable" width="220" title="确认删除该排班时间块？" @confirm="handleDelete">
      <template #reference>
        <div class="delete-btn" @mousedown.stop @click.stop> × </div>
      </template>
    </el-popconfirm>

    <!-- 右侧调整手柄 -->
    <div
      class="resize-handle right"
      v-if="isClickable && resizable"
      @mousedown.stop="handleResizeStart($event, 'right')"
    >
      <i class="handle-icon"></i>
    </div>
  </div>
</template>

<script setup>
  /**
   * @file src/components/Schedule/components/ScheduleBlock.vue
   * @description 单个排班块组件 (支持拖拽和调整大小)
   */
  import { computed, ref } from 'vue'
  import { timeToPosition, formatTime } from '../utils'

  const props = defineProps({
    data: {
      type: Object,
      required: true
    },
    isDragging: {
      type: Boolean,
      default: false
    },
    baseTime: {
      type: Number,
      default: null
    },
    readonly: {
      type: Boolean,
      default: false
    },
    draggable: {
      type: Boolean,
      default: true
    },
    resizable: {
      type: Boolean,
      default: true
    },
    clickable: {
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
    styleConfig: {
      type: Object,
      default: () => ({})
    }
  })

  const emit = defineEmits(['drag-start', 'resize-start', 'delete', 'click', 'view-detail'])

  const resolvedStyleConfig = computed(() => {
    const cfg = props.styleConfig || {}
    return {
      pastBg: cfg.pastBg || '#E4E7ED',
      pastText: cfg.pastText || '#909399',
      pastBorder: cfg.pastBorder || '#DCDFE6',
      futureBg: cfg.futureBg || '#409EFF',
      futureText: cfg.futureText || '#fff',
      futureBorder: cfg.futureBorder || '#409EFF',
      futureAccent: cfg.futureAccent || '#337ecc'
    }
  })

  const displayName = computed(() => {
    const name = String(props.data?.name || '')
    if (!name) return ''
    const role = String(props.roleName || '')
    if (!role) return name
    const prefix1 = `${role}:`
    const prefix2 = `${role}：`
    if (name.startsWith(prefix1)) return name.slice(prefix1.length)
    if (name.startsWith(prefix2)) return name.slice(prefix2.length)
    return name
  })

  const resolveAbsTime = (t) => {
    const n = Number(t) || 0
    if (!n) return 0
    if (n > 1e10) return n
    const base = Number(props.baseTime) || 0
    if (!base) return 0
    return base + n
  }

  const resolveAbsRange = (times) => {
    const list = Array.isArray(times) ? times : []
    const startRaw = list[0]
    const endRaw = list[1]
    const start = resolveAbsTime(startRaw)
    let end = resolveAbsTime(endRaw)
    if (!start || !end) return { start: 0, end: 0 }
    const base = Number(props.baseTime) || 0
    if (base && end < start) {
      end = base + 24 * 60 * 60 * 1000
    }
    return { start, end }
  }

  const isPastBlock = computed(() => {
    const startRaw = Array.isArray(props.data?.times) ? props.data.times[0] : 0
    const start = resolveAbsTime(startRaw)
    if (!start) return false
    // 排班时间小于等于 当前时间+1小时 都视为过去/重叠，置灰并不可编辑
    return start <= Date.now() + 3600 * 1000
  })

  const isClickable = computed(() => {
    return !props.readonly && !isPastBlock.value
  })

  const style = computed(() => {
    const { start, end } = resolveAbsRange(props.data?.times)
    const base = Number(props.baseTime) || 0
    const DAY_MS = 24 * 60 * 60 * 1000
    let left = 0
    let width = 0

    if (base) {
      const relStart = start - base
      const relEnd = end - base
      const visibleStart = Math.max(0, relStart)
      const visibleEnd = Math.min(DAY_MS, relEnd)
      const widthVal = Math.max(0, visibleEnd - visibleStart)
      left = (visibleStart / DAY_MS) * 100
      width = (widthVal / DAY_MS) * 100
    } else {
      const [s, e] = Array.isArray(props.data?.times) ? props.data.times : []
      const pos = timeToPosition(s, e, props.baseTime)
      left = Number.parseFloat(pos.left)
      width = Number.parseFloat(pos.width)
      if (!Number.isFinite(left)) left = 0
      if (!Number.isFinite(width)) width = 0
      if (width < 0) width = 0
    }

    const cfg = resolvedStyleConfig.value
    const resolved = (() => {
      if (isPastBlock.value) {
        return {
          backgroundColor: cfg.pastBg,
          color: cfg.pastText,
          border: `1px solid ${cfg.pastBorder}`
        }
      }
      // 未来的块 (包括当天的未来和未来天的)
      return {
        backgroundColor: props.data.color || cfg.futureBg,
        color: props.data.textColor || cfg.futureText,
        border: `1px solid ${props.data.color || cfg.futureBorder}`
      }
    })()

    return {
      left: `${left.toFixed(4)}%`,
      width: `${width.toFixed(4)}%`,
      ...resolved
    }
  })

  const displayAbsRange = computed(() => {
    const base = Number(props.baseTime) || 0
    const DAY_MS = 24 * 60 * 60 * 1000

    const times = Array.isArray(props.data?.displayTimes) ? props.data.displayTimes : props.data?.times
    const { start, end } = resolveAbsRange(times)
    const dayEndAbs = base ? base + DAY_MS : 0

    if (base && dayEndAbs && dayEndAbs - end >= 0 && dayEndAbs - end <= 1000) {
      return { start, end: dayEndAbs }
    }

    return { start, end }
  })

  const popoverTimeCount = computed(() => {
    const displayRaw =
      props.data?.displayTimeCount !== undefined && props.data?.displayTimeCount !== null ? props.data.displayTimeCount : null

    if (displayRaw !== null) {
      const n = Number(displayRaw)
      if (!Number.isFinite(n)) return 0
      return Math.round(n)
    }

    const base = Number(props.baseTime) || 0
    const DAY_MS = 24 * 60 * 60 * 1000
    const dayEndAbs = base ? base + DAY_MS : 0
    const { start, end } = displayAbsRange.value

    if (base && dayEndAbs && end === dayEndAbs && start) {
      const minutes = Math.max(0, Math.round((dayEndAbs - start) / (1000 * 60)))
      return minutes
    }

    const raw = props.data?.timeCount
    const n = Number(raw)
    if (!Number.isFinite(n)) return 0
    return Math.round(n)
  })

  const timeRange = computed(() => {
    // 如果处于拖拽状态，显示在当前视野内的时间（截断后），而非原始完整时间
    // 这样可以避免用户困惑为什么时间突然变了
    if (props.isDragging && props.baseTime) {
      // 这里的计算稍微复杂，需要模拟 timeToPosition 的截断逻辑的反向
      // 简单起见，如果 start < 0，显示 00:00 - end
      // 如果 end > 24h，显示 start - 24:00
      // 但 formatTime 已经处理了 > 24h 的显示 (mod 24)。
      // 主要处理 start < 0 的情况，formatTime 会显示前一天的时间 (e.g. 22:00)
      // 用户可能更希望看到 00:00 (表示在当前天是从 0 点开始)
      // 暂时保持原样，因为 formatTime 的逻辑是准确的物理时间。
      // 如果用户看到 22:00 - 02:00，这也是真实的排班时间。
      // 只是视觉上被 overflow: hidden 截断了。
      // 如果要改文字，需要 formatTime 支持 "view time" 模式。
    }

    const { start, end } = displayAbsRange.value
    return `${formatTime(start)} - ${formatTime(end)}`
  })

  const dateTimeFormat = (time) => {
    const totalMinutes = Number(time) || 0
    const hours = Math.floor(totalMinutes / 60)
    const minutes = totalMinutes % 60
    if (hours <= 0) return `${minutes}分钟`
    if (minutes <= 0) return `${hours}小时`
    return `${hours}小时${minutes}分钟`
  }

  const popoverDisabled = ref(false)

  const handleMouseUp = () => {
    popoverDisabled.value = false
    window.removeEventListener('mouseup', handleMouseUp)
  }

  const handleMouseDown = (e) => {
    popoverDisabled.value = true
    window.addEventListener('mouseup', handleMouseUp)
    e.preventDefault()
    emit('drag-start', {
      originalEvent: e,
      block: props.data
    })
  }

  const handleResizeStart = (e, direction) => {
    popoverDisabled.value = true
    window.addEventListener('mouseup', handleMouseUp)
    e.preventDefault()
    emit('resize-start', {
      originalEvent: e,
      block: props.data,
      direction // 'left' | 'right'
    })
  }

  const handleDelete = () => {
    emit('delete', props.data)
  }

  const handleClick = () => {
    if (props.clickable && isClickable.value) {
      emit('click', props.data)
    }
  }

  const handleViewDetail = () => {
    emit('view-detail', {
      dayDate: props.dayDate,
      roleName: props.roleName,
      block: props.data
    })
  }
</script>

<style lang="scss">
  /* 全局覆盖 popover 样式以实现深色主题 */
  .schedule-dark-popover {
    background-color: #333333 !important;
    border: none !important;
    color: #ffffff !important;
    padding: 8px 12px !important;

    .el-popper__arrow::before {
      background-color: #333333 !important;
      border: none !important;
    }
  }
</style>

<style scoped lang="scss">
  .popover-content {
    font-size: 12px;

    .detail-btn {
      background-color: #4c4cd0;
      border-color: #4c4cd0;
      padding: 4px 12px;
      font-size: 12px;
      height: auto;

      &:hover {
        background-color: #6363e8;
        border-color: #6363e8;
      }
    }
  }

  .schedule-block {
    position: absolute;
    box-sizing: border-box;
    top: 5px;
    bottom: 5px;
    background-color: #409eff;
    border-radius: 4px;
    color: #fff;
    font-size: 12px;
    /* padding: 6px 8px;*/
    overflow: hidden;
    cursor: grab;
    z-index: 10;
    transition: background-color 0.2s;
    /* 移除 box-shadow transition 以避免拖拽时的闪烁 */
    display: flex;
    flex-direction: row;
    align-items: center;

    &.is-dragging {
      opacity: 0.8;
      box-shadow: 0 4px 16px rgba(0, 0, 0, 0.2);
      z-index: 100;
      cursor: grabbing;
    }

    &.is-unassigned {
      background-color: #e4e7ed !important;
      border-color: #dcdfe6 !important;
      color: #909399 !important;

      &:hover {
        background-color: #dcdfe6 !important;
      }
    }

    &.is-record {
      background-color: #67c23a; /* 绿色背景表示上播记录 */
      color: #fff;

      &:hover {
        background-color: #85ce61;
      }
    }

    &:hover {
      box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
      z-index: 11;

      .resize-handle {
        opacity: 1;
      }

      .delete-btn {
        display: flex;
      }
    }

    &:active {
      cursor: grabbing;
    }

    &.is-clickable {
      cursor: pointer;
    }

    .block-content {
      align-items: center;
      width: 100%;
      height: 100%;
      min-width: 0;
      gap: 12px;
      padding: 6px 8px;
      box-sizing: border-box;
    }

    .block-title {
      font-weight: 500;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      flex-shrink: 1;
    }

    .block-time {
      opacity: 0.9;
      font-size: 12px;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      flex-shrink: 0;
    }

    .resize-handle {
      position: absolute;
      top: 0;
      bottom: 0;
      width: 10px;
      cursor: col-resize;
      display: flex;
      align-items: center;
      justify-content: center;
      opacity: 0; /* 默认隐藏，hover显示 */
      transition: opacity 0.2s;
      background-color: rgba(0, 0, 0, 0.1);
      z-index: 20;

      &.left {
        left: 0;
      }

      &.right {
        right: 0;
      }

      .handle-icon {
        width: 2px;
        height: 12px;
        background-color: #fff;
        border-radius: 1px;
        box-shadow: 0 0 2px rgba(0, 0, 0, 0.2);

        &::before,
        &::after {
          /* 可以添加更多装饰 */
        }
      }

      &:hover {
        background-color: rgba(0, 0, 0, 0.2);
      }
    }

    .delete-btn {
      position: absolute;
      top: 0;
      right: 0;
      width: 16px;
      height: 16px;
      display: none; /* 默认隐藏，hover显示 */
      align-items: center;
      justify-content: center;
      background-color: rgba(0, 0, 0, 0.3);
      color: #fff;
      font-size: 14px;
      line-height: 1;
      cursor: pointer;
      z-index: 30;
      border-bottom-left-radius: 4px;

      &:hover {
        background-color: #f56c6c;
      }
    }
  }
  .schedule-dark-popover {
    max-width: 350px !important;
    width: auto !important;
    min-width: 200px; // 可选，设置最小宽度

    .popover-content {
      white-space: normal; // 允许内容换行
      word-break: break-word; // 长单词换行
    }
  }
</style>
