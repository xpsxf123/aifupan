<template>
  <svg :style="iconStyle" aria-hidden="true">
    <use :xlink:href="symbolId" :fill="iconColor" />
  </svg>
</template>

<script setup>
  import { computed } from 'vue'
  const props = defineProps({
    name: {
      type: String,
      required: true
    },
    prefix: {
      type: String,
      default: 'icon'
    },
    iconStyle: {
      type: Object,
      default: () => ({ width: '25px', height: '25px' })
    },
    iconColor: {
      type: String,
      default: ''
    }
  })

  const symbolId = computed(() => `#${props.prefix}-${props.name}`)
</script>
