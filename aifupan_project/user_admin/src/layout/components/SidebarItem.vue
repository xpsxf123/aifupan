<template>
  <template v-if="!item.hidden">
    <template
      v-if="
        shouldRenderMenuItem(item.children, item) &&
        (!onlyOneChild.children || onlyOneChild.noShowingChildren) &&
        !item.alwaysShow
      "
    >
      <el-menu-item
        :index="resolvePath(onlyOneChild.path)"
        :class="{ 'submenu-title-noDropdown': !isNest }"
        v-bind="$attrs"
      >
        <div class="svg-container">
          <SvgIcon
            :icon-style="{
              width: '25px',
              height: '25px'
            }"
            v-if="!isNest && typeof menuItemIcon === 'string'"
            :name="menuItemIcon"
            class="sidebar-item__icon"
            :icon-color="getIconColor(resolvePath(onlyOneChild.path), false)"
          />
        </div>
        <template #title>
          <el-tooltip
            v-if="showTooltip"
            :content="onlyOneChild.meta ? onlyOneChild.meta.title : onlyOneChild.name"
            placement="right"
            :disabled="!showTooltip"
          >
            <span
              :class="['menu-item-title', { 'menu-item-title--dot': isNest }]"
              :style="{ color: getIconColor(resolvePath(onlyOneChild.path), false) }"
              >{{ onlyOneChild.meta ? onlyOneChild.meta.title : onlyOneChild.name }}</span
            >
          </el-tooltip>
          <span
            v-else
            :class="['menu-item-title', { 'menu-item-title--dot': isNest }]"
            :style="{ color: getIconColor(resolvePath(onlyOneChild.path), false) }"
            >{{ onlyOneChild.meta ? onlyOneChild.meta.title : onlyOneChild.name }}</span
          >
        </template>
      </el-menu-item>
    </template>

    <el-sub-menu v-else :index="basePath" popper-append-to-body v-bind="$attrs">
      <template #title>
        <div class="submenu-title-content">
          <div class="svg-container">
            <SvgIcon
              v-if="!isNest && typeof subMenuIcon === 'string'"
              :name="subMenuIcon"
              :icon-color="getIconColor(basePath, true)"
            />
          </div>
          <el-tooltip
            v-if="showTooltip"
            :content="item.meta ? item.meta.title : item.name"
            placement="right"
            :disabled="!showTooltip"
          >
            <span
              :class="['menu-item-title', { 'menu-item-title--dot': isNest }]"
              :style="{ color: getIconColor(basePath, true) }"
              >{{ item.meta ? item.meta.title : item.name }}</span
            >
          </el-tooltip>
          <span
            v-else
            :class="['menu-item-title', { 'menu-item-title--dot': isNest }]"
            :style="{ color: getIconColor(basePath, true) }"
            >{{ item.meta ? item.meta.title : item.name }}</span
          >
        </div>
      </template>

      <SidebarItem
        v-for="child in item.children"
        :key="child.path"
        :is-nest="true"
        :item="child"
        :base-path="resolvePath(child.path)"
        :show-tooltip="showTooltip"
        class="nest-menu"
      />
    </el-sub-menu>
  </template>
</template>

<script setup>
  import { computed, ref } from 'vue'
  import { useRoute } from 'vue-router'

  defineOptions({
    inheritAttrs: false
  })

  const route = useRoute()

  const props = defineProps({
    item: {
      type: Object,
      required: true
    },
    isNest: {
      type: Boolean,
      default: false
    },
    basePath: {
      type: String,
      default: ''
    },
    showTooltip: {
      type: Boolean,
      default: false
    }
  })

  const onlyOneChild = ref(null)
  const inactiveIconColor = '#515c73'
  const activeIconColor = '#444dff'
  const activeMenuPath = computed(() => String(route.meta?.activeMenu || route.path || ''))

  const menuItemIcon = computed(() => {
    const icon =
      (onlyOneChild.value && onlyOneChild.value.meta && onlyOneChild.value.meta.icon) ||
      (props.item && props.item.meta && props.item.meta.icon)
    if (icon) return icon
    if (props.isNest) return ''
    return 'mySchedulePerformance'
  })

  const subMenuIcon = computed(() => {
    const icon = props.item && props.item.meta && props.item.meta.icon
    if (icon) return icon
    if (props.isNest) return ''
    return 'mySchedulePerformance'
  })

  const shouldRenderMenuItem = (children = [], parent) => {
    const showingChildren = children.filter((item) => {
      if (item.hidden) {
        return false
      } else {
        // Temp set(will be used if only 1 showing child)
        onlyOneChild.value = item
        return true
      }
    })

    // Show parent if there are no child router to display
    if (showingChildren.length === 0) {
      onlyOneChild.value = { ...parent, path: '', noShowingChildren: true }
      return true
    }

    return false
  }

  const resolvePath = (routePath) => {
    // 如果是外部链接，直接返回
    if (routePath && (routePath.startsWith('http://') || routePath.startsWith('https://'))) {
      return routePath
    }
    // 如果是绝对路径，直接返回
    if (routePath && routePath.startsWith('/')) {
      return routePath
    }

    // 如果 routePath 为空，则返回 basePath
    if (!routePath) {
      return props.basePath
    }

    // 拼接 basePath 和 routePath
    const base = props.basePath.endsWith('/') ? props.basePath : props.basePath + '/'
    const path = routePath.startsWith('/') ? routePath.slice(1) : routePath
    return base + path
  }

  const isMenuActive = (path) => {
    const currentPath = String(activeMenuPath.value || '')
    const targetPath = String(path || '')

    if (!currentPath || !targetPath) {
      return false
    }

    if (currentPath === targetPath) {
      return true
    }

    return currentPath.startsWith(`${targetPath}/`)
  }

  const getIconColor = (path) => {
    return isMenuActive(path) ? activeIconColor : inactiveIconColor
  }
</script>

<style scoped lang="scss">
  :deep(.el-sub-menu__title) {
    padding-left: 36px !important;
  }
  .submenu-title-noDropdown {
    padding-left: 36px !important;
  }
  .svg-container {
    display: flex;
    align-items: center;
    justify-content: center;
    margin-right: 13px;
  }

  .submenu-title-content {
    display: flex;
    align-items: center;
    width: calc(100% - 24px);
  }

  .menu-item-title {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    display: inline-block;
    width: 100%;
    vertical-align: middle;
    font-size: 16px;
    color: #515c73;
  }

  :deep(.el-menu-item.is-active .menu-item-title) {
    color: #444dff;
  }

  :deep(.el-sub-menu.is-active > .el-sub-menu__title .menu-item-title) {
    color: #444dff;
  }

  .menu-item-title--dot::before {
    content: '';
    display: inline-block;
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background: currentColor;
    margin-right: 17px;
    vertical-align: middle;
  }
</style>

<style lang="scss">
  .el-sub-menu.is-active {
    .el-sub-menu__icon-arrow {
      color: #444dff !important;
    }
  }
</style>
