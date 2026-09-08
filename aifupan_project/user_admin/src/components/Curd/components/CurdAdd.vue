<template>
  <el-button v-if="show" type="primary" round :icon="Plus" @click="$emit('click')">
    <slot>新增</slot>
  </el-button>
</template>

<script setup>
  import { Plus } from '@element-plus/icons-vue'

  defineProps({
    show: { type: Boolean, default: true }
  })

  defineEmits(['click'])
</script>
