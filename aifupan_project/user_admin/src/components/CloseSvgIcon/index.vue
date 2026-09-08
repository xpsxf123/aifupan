<template>
  <div class="close-container">
    <SvgIcon name="close" :iconStyle="iconStyle" />
  </div>
</template>

<script setup>
  import { computed } from 'vue'

  const iconStyle = computed(() => {
    return {
      width: '18px',
      height: '18px'
    }
  })
</script>

<style scoped lang="scss">
  .close-container {
    display: flex;
    align-items: center;
    cursor: pointer;
  }
</style>
