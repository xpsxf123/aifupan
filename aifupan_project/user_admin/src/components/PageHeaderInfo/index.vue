<template>
  <div class="page-header-info">
    <div class="header-left">
      <div class="icon-wrapper">
        <slot name="icon">
          <el-icon v-if="icon" :size="32" class="default-icon"><component :is="icon" /></el-icon>
          <img v-else-if="avatar" :src="avatar" class="avatar-img" />
        </slot>
      </div>
      <div class="title-wrapper">
        <div class="title font-s16 text-main">{{ title }}</div>
        <div v-if="description || $slots.description" class="description font-s12 text-color-2">
          <slot name="description">{{ description }}</slot>
        </div>
      </div>
    </div>

    <div class="divider"></div>

    <div class="header-right">
      <slot name="stats">
        <div v-for="(item, index) in stats" :key="index" class="stat-item">
          <div class="label font-s14 text-regular fw-500">{{ item.label }}</div>
          <div class="value font-s14 text-color-2 fw-400">
            {{ item.value }}
            <span v-if="item.unit" class="unit">{{ item.unit }}</span>
          </div>
        </div>
      </slot>
    </div>
  </div>
</template>

<script setup>
  import { defineProps } from 'vue'

  defineProps({
    title: {
      type: String,
      default: ''
    },
    description: {
      type: String,
      default: ''
    },
    icon: {
      type: [String, Object],
      default: ''
    },
    avatar: {
      type: String,
      default: ''
    },
    stats: {
      type: Array,
      default: () => []
    }
  })
</script>

<style scoped>
  .page-header-info {
    background: #fff;
    border: 1px solid #409eff;
    border-radius: 8px;
    padding: 20px 24px;
    display: flex;
    align-items: center;
    margin-bottom: 20px;
    box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
  }

  .header-left {
    display: flex;
    align-items: center;
    padding-right: 20px;
  }

  .icon-wrapper {
    width: 56px;
    height: 56px;
    border-radius: 50%;
    overflow: hidden;
    background: #ecf5ff;
    display: flex;
    justify-content: center;
    align-items: center;
    margin-right: 16px;
    flex-shrink: 0;
  }

  .default-icon {
    color: #409eff;
  }

  .avatar-img {
    width: 100%;
    height: 100%;
  }

  .title-wrapper {
    display: flex;
    flex-direction: column;
  }

  .title {
    font-weight: 600;
    line-height: 1.4;
  }

  .description {
    margin-top: 4px;
  }

  .divider {
    width: 1px;
    height: 40px;
    background-color: #dcdfe6;
    margin: 0 60px;
  }

  .header-right {
    display: flex;
    flex: 1;
    gap: 60px;
    align-items: center;
  }

  .stat-item {
    display: flex;
    flex-direction: column;
  }

  .stat-item .label {
    margin-bottom: 6px;
  }

  .stat-item .value {
    font-weight: 500;
  }

  .stat-item .unit {
    margin-left: 2px;
  }
</style>
