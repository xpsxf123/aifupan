<template>
  <div class="department-card" :class="{ 'is-active': active }">
    <!-- 左侧信息 -->
    <div class="card-header">
      <div class="icon-wrapper">
        <slot name="icon">
          <el-icon v-if="icon" :size="24"><component :is="icon" /></el-icon>
          <svg v-else class="default-icon" viewBox="0 0 1024 1024" width="24" height="24">
            <path
              d="M832 320H672V192c0-35.3-28.7-64-64-64H416c-35.3 0-64 28.7-64 64v128H192c-35.3 0-64 28.7-64 64v320c0 35.3 28.7 64 64 64h640c35.3 0 64-28.7 64-64V384c0-35.3-28.7-64-64-64zM416 192h192v128H416V192zm-224 512V384h160v128c0 17.7 14.3 32 32 32h256c17.7 0 32-14.3 32-32V384h160v320H192zm320-320h128v128H512V384z"
              fill="currentColor"
            />
          </svg>
        </slot>
      </div>
      <div class="title-wrapper">
        <span class="title">{{ title }}</span>
        <slot name="extra"></slot>
      </div>
    </div>

    <!-- 分割线 -->
    <div class="divider"></div>

    <!-- 右侧详细信息列表 -->
    <div class="info-list">
      <div v-for="(item, index) in config" :key="index" class="info-item" :style="{ width: item.width || 'auto' }">
        <div class="label">{{ item.label }}</div>
        <div class="value">
          <slot :name="`item-${item.prop}`" :row="item" :value="data[item.prop]">
            {{ data[item.prop] || '-' }}
          </slot>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
  import { defineProps } from 'vue'

  const props = defineProps({
    // 卡片标题
    title: {
      type: String,
      required: true
    },
    // 图标组件名称 (可选)
    icon: {
      type: String,
      default: ''
    },
    // 数据对象
    data: {
      type: Object,
      default: () => ({})
    },
    // 配置项列表
    // [{ label: '所属组织', prop: 'org', width: '200px' }, ...]
    config: {
      type: Array,
      default: () => []
    },
    // 是否激活/选中状态
    active: {
      type: Boolean,
      default: false
    }
  })
</script>

<style scoped lang="scss">
  .department-card {
    display: flex;
    align-items: center;
    padding: 16px 24px;
    background: #fff;
    border: 1px solid #ebeef5;
    border-radius: 4px;
    transition: all 0.3s;

    &:hover {
      box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
    }

    &.is-active {
      border-color: var(--el-color-success);
      background-color: var(--el-color-success-light-9);
    }
  }

  .card-header {
    display: flex;
    align-items: center;
    flex-shrink: 0;
    min-width: 200px; /* 标题区域最小宽度，可根据实际调整 */
  }

  .icon-wrapper {
    width: 48px;
    height: 48px;
    border-radius: 4px;
    background-color: #e1f3d8; /* 浅绿色背景 */
    color: #67c23a; /* 绿色图标 */
    display: flex;
    justify-content: center;
    align-items: center;
    margin-right: 12px;

    .default-icon {
      fill: currentColor;
    }
  }

  .title-wrapper {
    display: flex;
    flex-direction: column;

    .title {
      font-size: 16px;
      font-weight: 600;
      color: #303133;
    }
  }

  .divider {
    width: 1px;
    height: 40px;
    background-color: #ebeef5;
    margin: 0 32px;
  }

  .info-list {
    display: flex;
    flex: 1;
    gap: 40px; /* 间距 */
    overflow-x: auto;

    &::-webkit-scrollbar {
      display: none; /* 隐藏滚动条 */
    }
  }

  .info-item {
    display: flex;
    flex-direction: column;
    justify-content: center;

    .label {
      margin-bottom: 4px;
    }

    .value {
      font-weight: 500;
    }
  }
</style>
