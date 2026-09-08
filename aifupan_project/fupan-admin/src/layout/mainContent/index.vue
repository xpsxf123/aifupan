<template>
  <div :class="contentClass" class="main-content">
    <router-view v-slot="{ Component }">
      <transition mode="out-in" name="slide-fade">
        <component :is="Component"/>
      </transition>
    </router-view>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'

// 获取路由实例
const route = useRoute()

// 计算属性
const contentClass = computed(() => {
  return route.meta?.bgColor || 'bg-white'
})
</script>

<style lang="scss" scoped>
.main-content {
  background-color: #fff;
  overflow: hidden;
  border-radius: 5px;
}

.slide-fade-enter-active {
  transition: all 0.35s ease;
}

.slide-fade-leave-active {
  transition: all 0.25s ease;
}

.slide-fade-enter-from {
  transform: translateX(-20px);
  opacity: 0;
}

.slide-fade-leave-to {
  transform: translateX(20px);
  opacity: 0;
}
</style>
