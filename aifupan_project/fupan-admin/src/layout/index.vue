<script setup>
import AsideMenu from '@/layout/asideMenu/index.vue'
import HeaderNav from '@/layout/headerNav/index.vue'
import MainContent from '@/layout/mainContent/index.vue'
import { useSystemInfoStore } from '@/store'
import { storeToRefs } from 'pinia'
import { ref, computed } from 'vue'
import emitter from '@/utils/emitter.js'

const {collapse, isMobile} = storeToRefs(useSystemInfoStore())
const refreshFlag = ref(false)
const changeRefreshFlag = () => {
  refreshFlag.value = !refreshFlag.value
  setTimeout(() => {
    refreshFlag.value = !refreshFlag.value
  }, 100)
}

const collapseClass = computed(() => {
  if (!collapse.value) {
    return ''
  } else {
    return isMobile.value ? 'el-aside--mobile-collapsed' : 'el-aside--pc-collapsed'
  }
})

emitter.on('changeRefreshFlag', changeRefreshFlag)
</script>

<template>
  <div class="layout-container">
    <el-container>
      <!--左侧菜单栏-->
      <el-aside :class="collapseClass">
        <AsideMenu/>
      </el-aside>
      <!--右侧主内容-->
      <el-container>
        <!--导航栏-->
        <el-header>
          <HeaderNav class="header-nav"/>
        </el-header>
        <!--内容区-->
        <el-main>
          <MainContent v-if="!refreshFlag"/>
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<style lang="scss" scoped>
.layout-container {
  height: 100vh;

  .el-aside {
    width: 230px;
    transition: width 0.28s;
    background-color: #001428;

    &--pc-collapsed {
      width: 63px;
    }

    &--mobile-collapsed {
      width: 63px;
    }
  }

  .el-container {
    height: 100%;

    .el-header {
      border-bottom: 1px solid transparent;
      box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
      padding: 0;

      .header-nav {
        position: sticky;
        z-index: 999;
        top: 0;
        right: 0;
        padding: 0 20px;
        border-bottom: 1px solid transparent;
        box-shadow: 0 1px 1px rgba(0, 0, 0, 0.05);
      }
    }

    .el-main {
      background-color: #f1f4f6;
    }
  }
}
</style>
