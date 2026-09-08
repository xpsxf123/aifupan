<template>
  <div class="sidebar-container">
    <!-- Logo 区域 -->
    <div class="sidebar-logo-container">
      <!-- 这里的 logo 使用临时占位图，实际应使用项目 logo -->
      <img src="@/assets/images/icon/logo.png" class="sidebar-logo" />
      <h1 class="sidebar-title">爱复盘企业管理后台</h1>
    </div>

    <el-menu :default-active="activeMenu" class="el-menu-vertical" router unique-opened>
      <SidebarItem
        v-for="item in menuList"
        :key="item.path"
        :item="item"
        :base-path="item.path"
        :show-tooltip="item.meta && item.meta.showTooltip"
      />
    </el-menu>
  </div>
</template>

<script setup>
  /**
   * @file layout/components/Sidebar.vue
   * @description 侧边栏导航组件
   */
  import { computed, onMounted } from 'vue'
  import { useRoute } from 'vue-router'
  import { menuConfig } from '@/config/menu'
  import { usePermissionStore } from '@/auth/store'
  import { buildSidebarMenuTree } from '@/auth/menuTransform'
  import SidebarItem from './SidebarItem.vue'

  const route = useRoute()
  const permissionStore = usePermissionStore()

  onMounted(() => {
    permissionStore.ensureMenuTree({ maxAgeMs: 5 * 60 * 1000, minIntervalMs: 3000 }).catch(() => {})
  })

  /**
   * @description 计算菜单列表，过滤掉不展示的路由
   * @returns {Array} 过滤后的菜单列表
   */
  const menuList = computed(() => {
    return buildSidebarMenuTree({
      menuTree: permissionStore.menuTree,
      menuConfig,
      permissionStore
    })
  })

  const navPathList = computed(() => {
    const joinPath = (parentPath, childPath) => {
      const parent = String(parentPath || '')
      const child = String(childPath || '')

      if (!child) return parent
      if (child.startsWith('/')) return child

      const normalizedParent = parent.endsWith('/') ? parent.slice(0, -1) : parent
      return `${normalizedParent}/${child}`
    }

    const flattenPaths = (routes, parentPath = '') => {
      const res = []
      routes.forEach((route) => {
        const currentPath = joinPath(parentPath, route.path)
        if (currentPath) {
          res.push(currentPath)
        }
        if (route.children && route.children.length) {
          res.push(...flattenPaths(route.children, currentPath))
        }
      })
      return res
    }

    return flattenPaths(menuList.value)
  })

  /**
   * @description 当前激活的菜单
   * @returns {String} 当前路由路径或配置的高亮路径
   */
  const activeMenu = computed(() => {
    const { meta, path } = route
    if (meta.activeMenu) {
      return meta.activeMenu
    }

    const currentPath = String(path || '')
    const candidates = navPathList.value
      .filter((menuPath) => currentPath === menuPath || currentPath.startsWith(`${menuPath}/`))
      .sort((a, b) => b.length - a.length)

    return candidates[0] || currentPath
  })
</script>

<style scoped lang="scss">
  .sidebar-container {
    height: 100%;
    display: flex;
    flex-direction: column;
    border-right: 1px solid rgba(227, 227, 229, 0.6);
    .sidebar-logo-container {
      display: flex;
      align-items: center;
      gap: 3px;
      height: 56px;
      padding-left: 36px;
    }
  }

  .sidebar-logo {
    width: 27px;
    vertical-align: middle;
  }

  .sidebar-title {
    display: inline-block;
    margin: 0;
    color: #333;
    font-weight: 600;
    font-size: 20px;
    font-family:
      PingFang SC,
      PingFang SC;
    vertical-align: middle;
  }

  .el-menu-vertical {
    border-right: none;
    flex: 1; /* 占据剩余空间 */
    overflow-y: auto; /* 内容过多可滚动 */
    scrollbar-width: thin;
    scrollbar-color: rgba(81, 92, 115, 0.22) transparent;

    &::-webkit-scrollbar {
      width: 6px;
      height: 6px;
    }

    &::-webkit-scrollbar-track {
      background: transparent;
    }

    &::-webkit-scrollbar-thumb {
      background: rgba(81, 92, 115, 0.22);
      border-radius: 6px;
    }

    &::-webkit-scrollbar-thumb:hover {
      background: rgba(81, 92, 115, 0.35);
    }

    &::-webkit-scrollbar-corner {
      background: transparent;
    }
  }
</style>
