<template>
  <el-breadcrumb class="app-breadcrumb" separator="/">
    <transition-group name="breadcrumb">
      <el-breadcrumb-item v-for="(item, index) in levelList" :key="item.path">
        <span v-if="item.redirect === 'noRedirect' || index === levelList.length - 1" class="no-redirect">
          {{ item.meta.title }}
        </span>
        <a v-else @click.prevent="handleLink(item)">{{ item.meta.title }}</a>
      </el-breadcrumb-item>
    </transition-group>
  </el-breadcrumb>
</template>

<script setup>
  /**
   * @file src/components/Breadcrumb/index.vue
   * @description 面包屑导航组件
   */
  import { ref, watch } from 'vue'
  import { useRoute, useRouter } from 'vue-router'

  const route = useRoute()
  const router = useRouter()
  const levelList = ref([])

  const getBreadcrumb = () => {
    // 只显示有 meta.title 的路由
    let matched = route.matched.filter((item) => item.meta && item.meta.title)

    // 过滤掉 layout
    matched = matched.filter((item) => item.meta && item.meta.title && item.meta.breadcrumb !== false)

    levelList.value = matched
  }

  const handleLink = (item) => {
    const { redirect, path } = item
    if (redirect) {
      router.push(redirect)
      return
    }
    router.push(path)
  }

  watch(
    () => route.path,
    () => {
      getBreadcrumb()
    },
    { immediate: true }
  )
</script>

<style lang="scss" scoped>
  .app-breadcrumb.el-breadcrumb {
    display: inline-block;
    font-size: 14px;
    line-height: 50px;
    margin-left: 8px;

    .no-redirect {
      color: #97a8be;
      cursor: text;
    }
  }
</style>
