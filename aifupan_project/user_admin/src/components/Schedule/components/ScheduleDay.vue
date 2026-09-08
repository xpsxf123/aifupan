<template>
  <div class="schedule-day">
    <!-- 左侧日期信息 -->
    <div class="day-info" :class="[{ 'is-source-day': isSourceDay }, dayStatusClass]">
      <DayActionPopover :disabled="isCopyMode || readonly" @copy="handleCopy">
        <div class="day-date">{{ dayData.dateTime }}</div>
        <div class="day-label">{{ dayData.label }}</div>
      </DayActionPopover>

      <!-- 复制模式下的遮罩层 (仅非源日期显示) -->
      <div v-if="isCopyMode && !isSourceDay" class="copy-overlay">
        <el-button type="primary" link size="small" @click="handlePaste">粘贴</el-button>
        <el-button type="danger" size="small" link @click="handleExitCopy">退出</el-button>
      </div>
    </div>
    <!-- 右侧排班内容 -->
    <div class="day-content br-2 ml-16">
      <div v-for="(rowData, index) in dayData.data" :key="index" class="role-row-container">
        <div class="role-name">
          <span class="role-name-text">{{ rowData.name }}</span>
        </div>
        <div class="role-schedule">
          <ScheduleRow
            :blocks="rowData.data"
            :base-time="currentDayBase"
            :readonly="readonly"
            :add-mode="addMode"
            :disable-add="disableAdd"
            :day-date="dayData.dateTime"
            :role-name="rowData.name"
            :block-style-config="blockStyleConfig"
            @add-block="(range) => handleAddBlock(rowData, range)"
            @update-block="(newBlock) => handleUpdateBlock(rowData, newBlock)"
            @remove-block="(block) => handleRemoveBlock(rowData, block)"
            @cross-day-drag="(payload) => handleCrossDayDrag(rowData, payload)"
            @drag-preview="(payload) => handleDragPreview(rowData, payload)"
            @fixed-add="(payload) => handleFixedAdd(rowData, payload)"
            @block-click="(block) => handleEditBlock(rowData, block)"
            @view-detail="handleViewDetail"
          />
          <!-- 跨天预览块 (Shadow Block from another day) -->
          <template v-if="previewBlock && previewBlock.roleType === rowData.type">
            <ScheduleBlock
              v-if="shouldShowPreview(rowData)"
              :data="previewBlockDisplayData"
              :base-time="currentDayBase"
              :readonly="true"
              :draggable="false"
              :resizable="false"
              class="preview-block"
            />
          </template>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
  /**
   * @file src/components/Schedule/components/ScheduleDay.vue
   * @description 每日排班组件
   */
  import { computed } from 'vue'
  import dayjs from 'dayjs'
  import ScheduleRow from './ScheduleRow.vue'
  import ScheduleBlock from './ScheduleBlock.vue'
  import DayActionPopover from './DayActionPopover.vue'

  const props = defineProps({
    dayData: {
      type: Object,
      required: true
    },
    previewData: {
      type: Object,
      default: null // { dayDate, roleType, block, range } from parent
    },
    isCopyMode: {
      type: Boolean,
      default: false
    },
    isSourceDay: {
      type: Boolean,
      default: false
    },
    readonly: {
      type: Boolean,
      default: false
    },
    addMode: {
      type: String,
      default: 'follow'
    },
    blockStyleConfig: {
      type: Object,
      default: () => ({})
    }
  })

  const emit = defineEmits([
    'update-data',
    'update-block-data',
    'cross-day-drag',
    'remove-block-data',
    'drag-preview',
    'copy-day',
    'paste-day',
    'exit-copy',
    'fixed-add',
    'edit-block',
    'view-detail'
  ])

  // 复制相关处理
  const handleCopy = () => {
    emit('copy-day', props.dayData)
  }

  const handlePaste = () => {
    emit('paste-day', props.dayData)
  }

  const handleExitCopy = () => {
    emit('exit-copy')
  }

  // 计算日期状态样式
  const dayStatusClass = computed(() => {
    // 1. 检查特殊异常/休息日 (预制参数 isRestDay)
    if (props.dayData.isRestDay) {
      return 'status-exception'
    }

    // 获取今日 00:00:00 时间戳
    const today = new Date()
    today.setHours(0, 0, 0, 0)
    const todayTs = today.getTime()

    // 获取当前行日期时间戳
    const currentTs = currentDayBase.value

    // 2. 比较日期
    if (currentTs === todayTs) {
      return 'status-today'
    } else if (currentTs > todayTs) {
      return 'status-future'
    } else {
      return 'status-past'
    }
  })

  const disableAdd = computed(() => {
    const base = currentDayBase.value
    if (!base) return false
    const dayEnd = base + 24 * 60 * 60 * 1000
    return dayEnd <= Date.now()
  })

  // 解析当前天基准时间
  const currentDayBase = computed(() => {
    const raw = String(props.dayData?.dateTime || '')
    if (!raw) return 0

    const mmdd = raw.match(/^(\d{1,2})\/(\d{1,2})$/)
    if (mmdd) {
      const year = dayjs().year()
      const m = String(mmdd[1]).padStart(2, '0')
      const d = String(mmdd[2]).padStart(2, '0')
      const ts = dayjs(`${year}-${m}-${d}`).startOf('day').valueOf()
      return Number.isFinite(ts) ? ts : 0
    }

    const normalized = raw.replace(/\//g, '-')
    const ts = dayjs(normalized).startOf('day').valueOf()
    return Number.isFinite(ts) ? ts : 0
  })

  // 计算预览块数据
  const previewBlock = computed(() => props.previewData)

  const previewBlockDisplayData = computed(() => {
    if (!previewBlock.value) return null

    const { block, range } = previewBlock.value

    let start = range.start
    let end = range.end
    const DAY_MS = 24 * 60 * 60 * 1000

    // Clamp start to 0 (确保跨天部分从当天 0 点开始)
    if (start < 0) start = 0

    // Clamp end to 24h (确保不超出当天范围)
    if (end > DAY_MS) end = DAY_MS

    // 如果无效则不显示
    if (start >= end) return null

    return {
      ...block,
      times: [currentDayBase.value + start, currentDayBase.value + end]
    }
  })

  const shouldShowPreview = (rowData) => {
    if (!previewBlock.value) return false
    // 必须是同一岗位类型
    if (previewBlock.value.roleType !== rowData.type) return false
    // 必须是当前天
    if (previewBlock.value.targetDate !== props.dayData.dateTime) return false

    return true
  }

  const handleAddBlock = (rowData, range) => {
    const baseTime = currentDayBase.value
    const startTime = baseTime + range.start
    const endTime = baseTime + range.end

    const newBlock = {
      name: `未添加${rowData.name}`,
      id: Date.now(),
      timeCount: (range.end - range.start) / 1000 / 60,
      originalTimeCount: (range.end - range.start) / 1000 / 60, // 记录原始时长
      startTime: startTime,
      times: [startTime, endTime],
      isTemp: true
    }

    emit('update-data', {
      dayDate: props.dayData.dateTime,
      roleType: rowData.type,
      newBlock
    })
  }

  const handleUpdateBlock = (rowData, newBlock) => {
    emit('update-block-data', {
      dayDate: props.dayData.dateTime,
      roleType: rowData.type,
      newBlock
    })
  }

  const handleRemoveBlock = (rowData, block) => {
    emit('remove-block-data', {
      dayDate: props.dayData.dateTime,
      roleType: rowData.type,
      block
    })
  }

  const handleCrossDayDrag = (rowData, payload) => {
    emit('cross-day-drag', {
      ...payload,
      roleType: rowData.type
    })
  }

  const handleDragPreview = (rowData, payload) => {
    // payload: { block, range } or null
    // 这里的 range 是相对于当前 ScheduleRow (即 source day) 的
    emit('drag-preview', {
      sourceDate: props.dayData.dateTime,
      roleType: rowData.type,
      payload
    })
  }

  const handleFixedAdd = (rowData, payload) => {
    emit('fixed-add', {
      dateTime: props.dayData.dateTime,
      roleType: rowData.type,
      rowName: rowData.name,
      positionId: rowData.positionId || rowData.id || '',
      range: payload?.range
    })
  }

  const handleEditBlock = (rowData, block) => {
    emit('edit-block', {
      dateTime: props.dayData.dateTime,
      roleType: rowData.type,
      rowName: rowData.name,
      positionId: rowData.positionId || rowData.id || '',
      block
    })
  }

  const handleViewDetail = (payload) => {
    emit('view-detail', payload)
  }
</script>

<style scoped lang="scss">
  .schedule-day {
    display: flex;
    // border-bottom: 1px solid #dcdfe6;
    margin-bottom: 16px;
    .day-info {
      width: 100px;
      flex-shrink: 0;
      display: flex;
      flex-direction: column;
      justify-content: center;
      align-items: center;
      // background-color: #f5f7fa;
      position: relative;
      z-index: 200;

      .day-date {
        font-size: 18px;
        font-weight: bold;
      }

      .day-label {
        font-size: 12px;
        color: #7a7c7f;
      }

      &.is-source-day {
        background-color: #ecf5ff; // 选中源的样式，例如浅蓝色背景
      }

      /* 日期状态样式 */
      &.status-today {
        .day-date {
          color: var(--color-primary);
        }
        .day-label {
          color: var(--color-primary);
        }
      }

      &.status-future {
        // 未来: 蓝色系 (默认风格保持或微调)
        // background-color: #f5f7fa;
        // 保持默认，或者可以加个蓝色边框
        // border-right: 4px solid #409EFF;

        .day-date {
          color: #7a7c7f;
        }
      }

      &.status-past {
        .day-date {
          color: #7a7c7f;
        }
        .day-label {
          color: #7a7c7f;
        }
      }

      &.status-exception {
        // 异常/休息: 红色系
        background-color: #fef0f0;
        border-right: 4px solid #f56c6c;

        .day-date {
          color: #f56c6c;
        }
        .day-label {
          color: #f56c6c;
        }
      }

      .copy-overlay {
        position: absolute;
        top: 0;
        left: 0;
        width: 100%;
        height: 100%;
        background-color: rgba(0, 0, 0, 0.7);
        display: flex;
        align-items: center;
        justify-content: center;
        gap: 5px;
        z-index: 10;
      }
    }

    .day-content {
      flex: 1;
      background-color: var(--bg-color-main);

      .role-row-container {
        display: flex;

        &:last-child {
          border-bottom: none;
        }

        .role-name {
          width: 70px;
          flex-shrink: 0;
          display: flex; /* 外层保持 flex */
          align-items: center;
          padding: 0 6px;
          position: relative;
          z-index: 200;
          box-sizing: border-box;
        }

        .role-name-text {
          display: -webkit-box; /* 内层负责文本截断 */
          -webkit-box-orient: vertical;
          -webkit-line-clamp: 2;
          overflow: hidden;
          word-break: break-all;
          font-size: 12px;
          color: var(--text-color-1);
          line-height: 16px;
        }

        .role-schedule {
          flex: 1;
          position: relative;
        }
      }
    }
  }

  .preview-block {
    opacity: 0.6;
    width: 120px;
    pointer-events: none;
    z-index: 100;
    background: #000;
    border: 1px dashed #000;
  }
</style>
