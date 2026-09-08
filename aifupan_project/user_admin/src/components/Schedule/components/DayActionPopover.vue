<template>
  <el-popover placement="right" :width="60" trigger="hover" :disabled="disabled" popper-class="day-action-popover">
    <template #reference>
      <div class="day-action-trigger bg-color-main br-2 overflow-hidden">
        <slot></slot>
      </div>
    </template>
    <div class="day-action-content">
      <div>
        <el-button style="width: 100%" size="small" type="primary" @click="$emit('copy')">复制当日排班</el-button>
      </div>
      <div>
        <!-- 预留按钮位 -->
        <!-- <el-button link size="small" @click="$emit('other')">其他</el-button> -->
      </div>
    </div>
  </el-popover>
</template>

<script setup>
  /**
   * @file src/components/Schedule/components/DayActionPopover.vue
   * @description 日期操作气泡组件
   */
  defineProps({
    disabled: {
      type: Boolean,
      default: false
    }
  })

  defineEmits(['copy', 'other'])
</script>

<style scoped lang="scss">
  .day-action-trigger {
    width: 100%;
    height: 100%;
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;
    cursor: pointer;
  }

  .day-action-content {
  }
</style>
