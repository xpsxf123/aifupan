<template>
  <img v-if="imageUrl" :src="imageUrl" :alt="name" class="icon-image" :style="{ width: width, height: height }" />
</template>

<script setup>
  import { computed, defineProps } from 'vue'

  /**
   * @file IconImage/index.vue
   * @description 图标图片组件，动态加载 src/assets/images/icon 下的图片
   */

  const props = defineProps({
    // 图标名称 (不含后缀)
    name: {
      type: String,
      required: true
    },
    // 图片后缀，默认为 png
    ext: {
      type: String,
      default: 'png'
    },
    width: {
      type: String,
      default: 'auto'
    },
    height: {
      type: String,
      default: 'auto'
    }
  })

  // 使用 Vite 的 import.meta.glob 动态引入图片
  // 这种方式可以确保图片被正确打包
  // 注意：import.meta.glob 的 key 必须是字面量或者以特定的方式引用
  // 这里使用相对路径或者别名
  const icons = import.meta.glob('@/assets/images/icon/*.*', { eager: true })

  const imageUrl = computed(() => {
    if (!props.name) return ''
    // 构造文件路径 key，必须匹配 glob 的 key 格式
    // 假设 @ 别名被解析为 /src，所以 glob 的 key 是 /src/assets/images/icon/...
    // 或者是相对路径，取决于 glob 的写法
    // import.meta.glob('@/assets/images/icon/*.*') 的 key 通常是 /src/assets/images/icon/xxx.png

    const path = `/src/assets/images/icon/${props.name}.${props.ext}`
    const module = icons[path]
    return module?.default || ''
  })
</script>

<style scoped lang="scss">
  .icon-image {
    display: inline-block;
    vertical-align: middle;
    object-fit: contain;
  }
</style>
