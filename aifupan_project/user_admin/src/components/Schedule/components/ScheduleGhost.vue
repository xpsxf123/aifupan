<template>
  <div v-show="visible" class="schedule-ghost" :style="style" @click.stop="handleClick">
    <div class="ghost-inner">
      <div class="ghost-content">
        <span>点击添加</span>
      </div>
      <div class="ghost-time">{{ timeRange }}</div>
    </div>
  </div>
</template>

<script setup>
  /**
   * @file src/components/Schedule/components/ScheduleGhost.vue
   * @description 跟随鼠标的虚线框组件
   */
  import { computed } from 'vue'
  import { timeToPosition, formatTime } from '../utils'

  const props = defineProps({
    visible: {
      type: Boolean,
      default: false
    },
    range: {
      type: Object, // { start: number, end: number } ms
      default: () => ({ start: 0, end: 0 })
    }
  })

  const emit = defineEmits(['add'])

  const style = computed(() => {
    if (!props.range.start && !props.range.end) return {}
    const pos = timeToPosition(props.range.start, props.range.end)
    return {
      left: pos.left,
      width: pos.width
    }
  })

  const timeRange = computed(() => {
    return `${formatTime(props.range.start)} - ${formatTime(props.range.end)}`
  })

  const handleClick = () => {
    emit('add', props.range)
  }
</script>

<style scoped lang="scss">
  .schedule-ghost {
    position: absolute;
    box-sizing: border-box;
    top: 5px;
    bottom: 5px;
    border: 2px dashed #909399;
    background-color: rgba(144, 147, 153, 0.1);
    border-radius: 4px;
    color: #606266;
    font-size: 12px;
    cursor: pointer;
    z-index: 5;
    pointer-events: none;
    overflow: hidden; /* 隐藏超出部分 */

    /* 使用 flex 居中，但为了 ellipsis，我们需要一个 inner 容器 */
    display: flex;
    align-items: center;
    justify-content: center;

    .ghost-inner {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      width: 100%;
      padding: 0 4px;
      box-sizing: border-box;
      overflow: hidden; /* 再次确保溢出隐藏 */
    }

    .ghost-content {
      display: flex;
      align-items: center;
      justify-content: center;
      font-weight: bold;
      margin-bottom: 2px;
      width: 100%;
      /* 文本溢出处理 */
      white-space: nowrap;
      text-overflow: ellipsis;
    }

    .plus-icon {
      font-size: 14px;
      margin-right: 4px;
      flex-shrink: 0; /* 图标不压缩 */
    }

    .ghost-time {
      font-size: 10px;
      color: #909399;
      width: 100%;
      text-align: center;

      /* 文本溢出处理 */
      white-space: nowrap;
      text-overflow: ellipsis;
    }
  }
</style>
