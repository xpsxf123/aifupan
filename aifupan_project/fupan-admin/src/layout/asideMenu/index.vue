<script setup>
import { useUserInfoStore } from '@/store/index.js'
import menuItem from '@/layout/asideMenu/menuItem/index.vue'
import menuTitle from '@/layout/asideMenu/menuTitle/index.vue'
import { ref } from 'vue'
import { useRoute } from 'vue-router'
import { useSystemInfoStore } from '@/store'
import { storeToRefs } from 'pinia'

const {collapse} = storeToRefs(useSystemInfoStore())

const route = useRoute()
const userInfoStore = useUserInfoStore()
const menuTreeList = ref([]) // 菜单数据
const defaultActive = ref('') // 默认激活的菜单
menuTreeList.value = userInfoStore.loginResultData?.menuTreeList || []
</script>

<template>
  <div class="aside-menu">
    <menuTitle/>
    <el-menu
        :collapse="collapse"
        :collapse-transition="false"
        :default-active="$route.name"
        class="el-menu-vertical"
        router
        unique-opened
    >
      <menuItem :collapse="collapse" :menuTreeList="menuTreeList"/>
    </el-menu>
  </div>
</template>

<style lang="scss" scoped>
.aside-menu {
  height: 100%;
  display: flex;
  flex-direction: column;
  user-select: none;
}

.el-menu-vertical {
  flex: 1;
  overflow-y: auto;
  --el-menu-bg-color: #001428; // 菜单背景色
  --el-menu-text-color: #b8c7ce; // 菜单文字颜色
  --el-menu-hover-bg-color: transparent; // 鼠标悬浮背景色
  --el-menu-active-color: #fff; // 激活菜单文字颜色
  border-right: none;

  :deep(.el-menu-item:hover, .el-sub-menu__title:hover) {
    color: #fff !important;
  }

  /* 展开的子菜单背景色 */
  :deep(.el-sub-menu .el-menu) {
    background-color: #011b35;
  }

  :deep(.el-menu-item) {
    border-radius: 5px;
  }

  /* 激活的菜单背景动画 */
  @keyframes menuItemActive {
    0% {
      background-color: rgba(45, 140, 240, 0); /* 从透明蓝色开始 */
      transform: translateX(-4px); /* 位移减小，避免突兀 */
      opacity: 0.6;
    }
    100% {
      background-color: rgba(45, 140, 240, 1); /* 平滑过渡到实心蓝色 */
      transform: translateX(0);
      opacity: 1;
    }
  }

  :deep(.el-menu-item.is-active) {
    animation: menuItemActive 0.3s ease forwards;
  }

  /* 自定义滚动条样式 */
  &::-webkit-scrollbar {
    width: 6px;
  }

  &::-webkit-scrollbar-track {
    background: #2c3e50;
  }

  &::-webkit-scrollbar-thumb {
    background: #4e73df;
    border-radius: 3px;
  }

  &::-webkit-scrollbar-thumb:hover {
    background: #5a7fd8;
  }
}
</style>
