<template>
  <div class="global-search">
    <el-select
      v-model="searchValue"
      filterable
      remote
      placeholder="请输入搜索关键词"
      :remote-method="querySearch"
      :loading="loading"
      clearable
      class="search-input rounded"
      @change="handleSelect"
    >
      <template #prefix>
        <el-icon class="search-icon"><Search /></el-icon>
      </template>
      <el-option v-for="item in options" :key="item.key" :label="item.label" :value="item.key" />
    </el-select>
  </div>
</template>

<script setup>
  /**
   * @file src/components/GlobalSearch/index.vue
   * @description 全局搜索组件
   */
  import { ref } from 'vue'
  import { Search } from '@element-plus/icons-vue'
  import { useRouter } from 'vue-router'
  import { menuConfig } from '@/config/menu'
  import { usePermissionStore } from '@/auth/store'
  import { buildSidebarMenuTree } from '@/auth/menuTransform'

  const router = useRouter()
  const permissionStore = usePermissionStore()
  const searchValue = ref('')
  const loading = ref(false)
  const options = ref([])

  const flattenMenuItems = (items) => {
    const list = []
    const walk = (arr) => {
      if (!Array.isArray(arr)) return
      arr.forEach((it) => {
        if (!it) return
        const path = String(it.path || '')
        const title = String(it.meta?.title || '')
        if (path && title) {
          list.push({ key: path, label: title })
        }
        if (Array.isArray(it.children) && it.children.length) {
          walk(it.children)
        }
      })
    }
    walk(items)
    return list
  }

  /**
   * @description 搜索查询方法 (预留)
   * @param {String} query 搜索关键词
   */
  const querySearch = (query) => {
    const keyword = String(query || '').trim()
    if (!keyword) {
      options.value = []
      return
    }
    loading.value = true
    const sidebarTree = buildSidebarMenuTree({
      menuTree: permissionStore.menuTree,
      menuConfig,
      permissionStore
    })
    const all = flattenMenuItems(sidebarTree)
    options.value = all.filter((item) => item.label.includes(keyword)).slice(0, 20)
    loading.value = false
  }

  /**
   * @description 选中项处理
   * @param {String} value 选中的值 (路由路径)
   */
  const handleSelect = (value) => {
    if (value) {
      router.push(value)
      searchValue.value = '' // 清空搜索
    }
  }
</script>

<style scoped lang="scss">
  .global-search {
    display: flex;
    align-items: center;
    margin-left: 46px;
    :deep(.el-select__wrapper) {
      height: 34px;
      box-shadow: none;
      background-color: #f0f3f8;
    }
    .search-input {
      width: 200px;
      transition: width 0.3s;

      &:focus-within {
        width: 300px;
      }

      :deep(.el-input__wrapper) {
        border-radius: 16px; // 圆角风格
        background-color: #f5f7fa;
        box-shadow: none;

        &.is-focus {
          background-color: #fff;
          box-shadow: 0 0 0 1px var(--el-color-primary) inset;
        }
      }
    }
  }
</style>
