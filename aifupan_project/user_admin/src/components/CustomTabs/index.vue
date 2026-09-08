<template>
  <div class="custom-tabs-container">
    <!-- 左侧 Tab 区域 -->
    <div class="tabs-wrapper">
      <div
        class="tab-item"
        v-for="(item, index) in options"
        :key="item.value"
        :class="{ 'is-active': modelValue === item.value }"
        @click="handleTabClick(item.value)"
      >
        {{ item.label }}
        <!-- 分割线，除了最后一个元素 -->
        <div v-if="index < options.length - 1" class="divider"></div>
      </div>
    </div>

    <!-- 右侧操作区域 -->
    <div class="tools-wrapper">
      <slot name="tools"></slot>
    </div>
  </div>
</template>

<script setup>
  import { defineProps, defineEmits } from 'vue'

  const props = defineProps({
    // v-model 绑定值
    modelValue: {
      type: [String, Number],
      required: true
    },
    // Tab 选项 [{ label: '数据概览', value: 'overview' }, ...]
    options: {
      type: Array,
      default: () => []
    }
  })

  const emit = defineEmits(['update:modelValue', 'change'])

  const handleTabClick = (value) => {
    if (value !== props.modelValue) {
      emit('update:modelValue', value)
      emit('change', value)
    }
  }
</script>

<style scoped lang="scss">
  .custom-tabs-container {
    display: flex;
    align-items: center;
    /* 如果不需要 justify-content: space-between，因为右侧有 margin-left */
    width: 100%;
  }

  .tabs-wrapper {
    display: flex;
    align-items: center;
    background: #fff;
    border-radius: 4px;
    padding: 12px 24px;
    flex-shrink: 0; /* 防止压缩 */
  }

  .tab-item {
    font-size: 14px;
    color: #606266;
    cursor: pointer;
    position: relative;
    transition: color 0.3s;
    display: flex;
    align-items: center;

    /* 移除之前的 margin/padding 实现，改用 flex gap 或者手动控制 */
    padding: 0 16px;

    &:first-child {
      padding-left: 0;
    }

    &:last-child {
      padding-right: 0;
    }

    &:hover {
      color: var(--el-color-primary);
    }

    &.is-active {
      color: var(--el-color-primary);
      font-weight: 600;
    }

    .divider {
      position: absolute;
      right: 0;
      top: 50%;
      transform: translateY(-50%);
      width: 1px;
      height: 14px;
      background-color: #dcdfe6;
    }
  }

  .tools-wrapper {
    flex: 1;
    margin-left: 16px; /* 16px 间隔 */
    background: #fff;
    border-radius: 4px;
    padding: 12px 24px;
    display: flex;
    align-items: center;
    justify-content: flex-end; /* 右侧内容靠右 */
  }
</style>
