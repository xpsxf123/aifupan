<template>
  <div class="live-room-info">
    <div class="avtar-container">
      <el-avatar :size="40" :src="row.anchorAvatar || defaultImg" />
    </div>
    <div class="info-content">
      <div class="name">{{ row.name }}</div>
      <div v-if="row.organization" class="subtitle">{{ row.organization }}</div>
    </div>
  </div>
</template>

<script setup>
import defaultImg from '@/assets/images/funnel/defaultAvatar.png'

  defineProps({
    row: {
      type: Object,
      required: true
    }
  })
</script>

<style scoped>
  .avtar-container {
    display: flex;
    align-items: center;
  }

  .live-room-info {
    display: flex;
    align-items: center;
  }

  .info-content {
    margin-left: 12px;
    text-align: left;
  }

  .subtitle {
    font-size: 12px;
    color: #909399;
    margin-top: 4px;
    line-height: 1.2;
  }
</style>
