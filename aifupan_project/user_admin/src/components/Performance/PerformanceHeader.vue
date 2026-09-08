<template>
  <PageHeaderInfo
    :title="userInfo.name"
    :description="userInfo.description || `直播间：${userInfo.liveRoom}`"
    :avatar="userInfo.avatar"
  >
    <template #stats>
      <div class="info-item" v-if="userInfo.organization">
        <div class="label">所属组织</div>
        <div class="value">{{ userInfo.organization }}</div>
      </div>
      <div class="info-item" v-if="userInfo.position">
        <div class="label">岗位</div>
        <div class="value tag">{{ userInfo.position }}</div>
      </div>
      <div class="info-item" v-if="userInfo.phone">
        <div class="label">电话</div>
        <div class="value">{{ userInfo.phone }}</div>
      </div>
      <div class="info-item" v-if="userInfo.email">
        <div class="label">邮箱</div>
        <div class="value">{{ userInfo.email }}</div>
      </div>
    </template>
  </PageHeaderInfo>
</template>

<script setup>
  /**
   * @file PerformanceHeader.vue
   * @description 业绩页面 - 头部信息组件 (Shared)
   */
  import PageHeaderInfo from '@/components/PageHeaderInfo/index.vue'

  defineProps({
    userInfo: {
      type: Object,
      default: () => ({})
    }
  })
</script>

<style scoped>
  .info-item {
    display: flex;
    flex-direction: column;
  }

  .info-item .label {
    margin-bottom: 6px;
    font-size: var(--font-size-14);
    color: var(--text-color-regular);
    font-weight: 500;
  }

  .info-item .value {
    font-size: var(--font-size-14);
    color: var(--text-color-regular);
    font-weight: 400;
  }

  .info-item .tag {
    margin-left: 4px;
    color: var(--color-primary);
  }
</style>
