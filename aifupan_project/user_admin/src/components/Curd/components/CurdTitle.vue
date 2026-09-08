<template>
  <div class="curd-title-wrap">
    <slot name="header-left">
      <span v-if="title" class="title-text">{{ title }}</span>
      <span v-if="showTotal" class="total-text">
        <slot name="total" :total="total">（{{ total }}{{ unit }}）</slot>
      </span>
    </slot>
  </div>
</template>

<script setup>
  defineProps({
    title: { type: String, default: '' },
    showTotal: { type: Boolean, default: false },
    total: { type: Number, default: 0 },
    unit: { type: String, default: '' }
  })
</script>

<style scoped>
  .curd-title-wrap {
    display: flex;
    align-items: center;
  }
  .title-text {
    font-size: 16px;
    font-weight: bold;
  }
  .total-text {
    font-weight: bold;
  }
</style>
