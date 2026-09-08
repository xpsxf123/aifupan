<template>
  <el-tooltip v-if="show" content="刷新" placement="top">
    <el-button circle :icon="Refresh" @click="$emit('click')" />
  </el-tooltip>
</template>

<script setup>
  import { Refresh } from '@element-plus/icons-vue'

  defineProps({
    show: { type: Boolean, default: true }
  })

  defineEmits(['click'])
</script>
