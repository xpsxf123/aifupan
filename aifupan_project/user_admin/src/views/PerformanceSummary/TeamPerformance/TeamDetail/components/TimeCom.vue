<template>
  <div class="group-compass-page__actions">
    <el-radio-group v-model="dateType" size="default" class="custom-radio-group">
      <el-radio-button label="yesterday">昨日</el-radio-button>
      <el-radio-button label="7days">近7天</el-radio-button>
      <el-radio-button label="15days">近15天</el-radio-button>
      <el-radio-button label="30days">近30天</el-radio-button>
      <el-radio-button label="90days">近90天</el-radio-button>
    </el-radio-group>
    <el-date-picker
      v-model="dateRange"
      type="daterange"
      size="default"
      start-placeholder="开始日期"
      end-placeholder="结束日期"
      class="custom-date-picker"
    />
  </div>
</template>

<script setup>
  import { ref } from 'vue'

  const dateType = ref('90days')
  const dateRange = ref([])
</script>

<style scoped>
  .group-compass-page__actions {
    display: flex;
    gap: 16px;
    align-items: center;
  }

  .custom-radio-group {
    margin-right: 0;
  }

  .custom-date-picker {
    width: 240px !important;
  }
</style>
