<template>
  <svg :style="iconStyle" aria-hidden="true">
    <use :xlink:href="symbolId" :fill="iconColor"/>
  </svg>
</template>

<script setup lang="ts">
import {computed} from 'vue'
import type {CSSProperties} from 'vue'

interface SvgProps {
  name: string // 图标的名称（Svg 文件名） ==> 必传
  prefix?: string // 图标的前缀 ==> 非必传（默认为"icon"）
  iconStyle?: CSSProperties // 图标的样式 ==> 非必传
  iconColor?: string
}

// 接收父组件参数并设置默认值
const props = withDefaults(defineProps<SvgProps>(), {
  prefix: 'icon',
  iconStyle: () => ({width: '25px', height: '25px'}),
})

const symbolId = computed(() => `#${props.prefix}-${props.name}`)
</script>
