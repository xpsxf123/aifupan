<template>
  <div class="group-compass-page__actions">
    <el-button-group>
      <el-button type="primary" plain>本周</el-button>
      <el-button plain>下周</el-button>
      <el-button plain>本月</el-button>
      <el-button plain>下月</el-button>
    </el-button-group>
    <el-date-picker v-model="currentDate" type="date" placeholder="请选择日期" style="width: 140px" />
  </div>
</template>

<script setup>
  import { ref } from 'vue'

  const currentDate = ref(new Date('2026-01-24'))
</script>

<style scoped>
  .group-compass-page__actions {
    display: flex;
    gap: 16px;
    align-items: center;
  }
</style>
