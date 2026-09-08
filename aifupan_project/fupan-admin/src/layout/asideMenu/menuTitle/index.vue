<script setup>
import { computed } from 'vue'
import { useSystemInfoStore } from '@/store'
import { storeToRefs } from 'pinia'

const {collapse} = storeToRefs(useSystemInfoStore())

const rootClasses = computed(() => ({
  'menu-title': true,
  'menu-title--collapsed': Boolean(collapse.value)
}))
</script>

<template>
  <div :class="rootClasses" aria-label="爱复盘管理系统">
    <i aria-hidden="true" class="menu-title__logo">
      <img alt="" src="@/assets/images/icon.png"/>
    </i>
    <p class="menu-title__text">爱复盘管理系统</p>
  </div>
</template>

<style lang="scss" scoped>
.menu-title {
  display: flex;
  align-items: center;
  height: 56px;
  padding: 0 12px;

  &__logo {
    flex: 0 0 28px;
    width: 28px;
    height: 28px;

    img {
      width: 100%;
      height: 100%;
      object-fit: contain;
    }
  }

  &__text {
    margin-left: 10px;
    font-size: 16px;
    font-weight: 600;
    color: #fff;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    max-width: 140px;
    opacity: 1;
  }

  &--collapsed {
    justify-content: center; /* 收起时让 Logo 居中 */

    .menu-title__text {
      max-width: 0;
      opacity: 0;
      margin-left: 0;
    }
  }
}
</style>
