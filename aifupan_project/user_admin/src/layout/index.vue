<template>
  <div v-if="isNotLayout" class="app-main-full">
    <router-view />
  </div>
  <el-container v-else class="layout-container">
    <el-aside style="width: 264px">
      <Sidebar />
    </el-aside>
    <el-container>
      <el-header class="layout-header">
        <Header></Header>
      </el-header>
      <el-main ref="mainRef">
        <!-- 面包屑导航 -->
        <!-- <div class="breadcrumb-container" v-if="!route.meta.hideBreadcrumb">
             <Breadcrumb />
           </div> -->
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
  /**
   * @file layout/index.vue
   * @description 全局Layout组件，包含侧边栏导航和头部
   */
  import { computed, nextTick, ref, watch } from 'vue'
  import { useRoute } from 'vue-router'
  import Sidebar from './components/Sidebar.vue'
  import Header from './components/Header.vue'
  // import Breadcrumb from '@/components/Breadcrumb/index.vue'

  const route = useRoute()
  const mainRef = ref(null)

  /**
   * @description 是否不使用Layout布局
   * @returns {Boolean}
   */
  const isNotLayout = computed(() => {
    return route.meta && route.meta.notLayout === true
  })

  watch(
    () => route.fullPath,
    async () => {
      await nextTick()
      const el = mainRef.value?.$el || mainRef.value
      if (el && typeof el.scrollTo === 'function') el.scrollTo({ top: 0, left: 0 })
      else if (el) el.scrollTop = 0

      if (typeof window !== 'undefined' && typeof window.scrollTo === 'function') window.scrollTo(0, 0)
    },
    { flush: 'post' }
  )
</script>

<style scoped>
  .layout-container {
    height: 100vh;
  }
  .layout-header {
    background-color: #fff;
    border-bottom: 1px solid #dcdfe6;
    display: flex;
    align-items: center;
    padding: 0 20px;
    height: 56px;
  }
  .app-main-full {
    height: 100vh;
    width: 100%;
  }
  .el-main {
    padding: 20px;
    background: #f4f9ff;
    display: flex;
    flex-direction: column;
  }
  .breadcrumb-container {
    background: #fff;
    padding: 0 20px;
    border-bottom: 1px solid #dcdfe6;
    height: 50px;
    display: flex;
    align-items: center;
  }
</style>
