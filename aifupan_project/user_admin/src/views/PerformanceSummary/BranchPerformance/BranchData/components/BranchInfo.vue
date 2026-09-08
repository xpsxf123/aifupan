<template>
  <div class="branch-info">
    <div class="info-content">
      <div class="name">{{ row.name }}</div>
    </div>
  </div>
</template>

<script setup>
  defineProps({
    row: {
      type: Object,
      required: true
    }
  })
</script>

<style scoped>
  .branch-info {
    display: flex;
    align-items: center;
  }

  .info-content {
    text-align: left;
  }
</style>
