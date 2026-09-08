<template>
  <div class="curd-search">
    <el-form
      ref="searchFormRef"
      :model="searchParams"
      :inline="true"
      :label-width="labelWidth"
      label-position="left"
      class="curd-search__form"
      :show-message="false"
    >
      <div class="search-container" :class="[`layout-${layout}`]">
        <div class="search-left">
          <template v-for="(item, index) in searchConfig" :key="item.prop">
            <!-- Always show if item.alwaysShow is true or if not collapsed or if index is small enough -->
            <el-form-item
              v-show="shouldShow(item, index)"
              :label="item.label"
              :prop="item.prop"
              class="curd-search__item"
            >
              <!-- Slot support -->
              <slot v-if="item.slotName" :name="item.slotName" :row="item" :form="searchParams" />

              <!-- Select -->
              <el-select
                v-else-if="item.type === 'select'"
                v-model="searchParams[item.prop]"
                :placeholder="item.placeholder || '请选择'"
                clearable
                :style="{ width: item.width || '180px' }"
                v-bind="item.opetion"
                @change="handleSearch"
              >
                <el-option
                  v-for="opt in normalizeOptions(item.options)"
                  :key="opt.key"
                  :label="opt.label"
                  :value="opt.key"
                />
              </el-select>

              <!-- Cascader -->
              <el-cascader
                v-else-if="item.type === 'cascader'"
                v-model="searchParams[item.prop]"
                :options="resolveRawOptions(item.options)"
                :popper-options="{ modifiers: [{ name: 'flip', enabled: false }] }"
                :placeholder="item.placeholder || '请选择'"
                :show-all-levels="resolveCascaderShowAllLevels(item)"
                :props="
                  item.props || {
                    value: 'value',
                    label: 'label',
                    children: 'children'
                  }
                "
                :clearable="item.clearable !== false"
                :style="{ width: item.width || '180px' }"
                v-bind="item.opetion"
              />

              <!-- Date Picker -->
              <el-date-picker
                v-else-if="item.type === 'date'"
                v-model="searchParams[item.prop]"
                type="date"
                :placeholder="item.placeholder || '选择日期'"
                value-format="YYYY-MM-DD"
                :style="{ width: item.width || '180px' }"
              />

              <!-- Date Range Picker -->
              <el-date-picker
                v-else-if="item.type === 'daterange'"
                v-model="searchParams[item.prop]"
                type="daterange"
                range-separator="~"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                value-format="YYYY-MM-DD"
                :style="{ width: item.width || '240px' }"
              >
                <template #range-separator>
                  <SvgIcon name="toIcon" />
                </template>
              </el-date-picker>
              <!-- Input (Default) -->
              <el-input
                v-else
                v-model="searchParams[item.prop]"
                :placeholder="item.placeholder || '请输入'"
                clearable
                :style="{ width: item.width || '180px' }"
                @keyup.enter="handleSearch"
              />
            </el-form-item>
          </template>

          <el-form-item v-if="showCollapseButton" class="curd-search__item">
            <el-button type="primary" link @click="toggleCollapse">
              {{ collapsed ? '展开' : '收起' }}
              <el-icon class="el-icon--right">
                <ArrowDown v-if="collapsed" />
                <ArrowUp v-else />
              </el-icon>
            </el-button>
          </el-form-item>

          <el-form-item v-if="!searchFlex" class="curd-search__item ml-16">
            <el-button class="operate-btn" round @click="handleSearch" plain type="primary">查询</el-button>
            <el-button class="operate-btn" v-if="props.showReset" round @click="handleReset">重置</el-button>
            <!-- Extra slots for search bar -->
            <slot name="search-action" />
          </el-form-item>
        </div>

        <!-- Operation Buttons -->
        <div v-if="searchFlex" class="search-right">
          <el-form-item class="curd-search__item">
            <el-button type="primary" round :icon="Search" @click="handleSearch">查询</el-button>
            <el-button v-if="props.showReset" round @click="handleReset">重置</el-button>
            <!-- Extra slots for search bar -->
            <slot name="search-action" />
          </el-form-item>
        </div>

        <!-- Right Filler Slot -->
        <div class="search-filler" v-if="$slots['search-filler']">
          <slot name="search-filler" />
        </div>
      </div>
    </el-form>
  </div>
</template>

<script setup>
  /**
   * @file Search Component for Curd Module
   * @description Provides a configurable search form with collapse functionality.
   */
  import { ref, computed, unref } from 'vue'
  import { Search, ArrowDown, ArrowUp } from '@element-plus/icons-vue'
  import { normalizeKeyLabelOptions } from '@/utils/options'

  const props = defineProps({
    /**
     * Search configuration array
     * @type {Array<{label: string, prop: string, type?: string, placeholder?: string, options?: Array, alwaysShow?: boolean, slotName?: string}>}
     */
    searchConfig: {
      type: Array,
      default: () => []
    },
    /**
     * Label width for form items
     */
    labelWidth: {
      type: [String, Number],
      default: ''
    },
    /**
     * Search parameters object (v-model)
     */
    modelValue: {
      type: Object,
      default: () => ({})
    },
    showReset: {
      type: Boolean,
      default: true
    },
    /**
     * Whether to collapse by default
     */
    defaultCollapsed: {
      type: Boolean,
      default: true
    },
    /**
     * Number of items to show when collapsed (excluding alwaysShow items which are always shown)
     */
    collapseCount: {
      type: Number,
      default: 3
    },
    /**
     * Layout mode: 'default' (space-between) or 'inline' (flex-start)
     */
    layout: {
      type: String,
      default: 'default',
      validator: (value) => ['default', 'inline'].includes(value)
    },

    searchFlex: {
      type: Boolean,
      default: false
    }
  })

  const emits = defineEmits(['update:modelValue', 'search', 'reset', 'collapse-change'])

  const searchParams = computed({
    get: () => props.modelValue,
    set: (val) => emits('update:modelValue', val)
  })

  const collapsed = ref(props.defaultCollapsed)

  // Calculate if we need the collapse button
  const showCollapseButton = computed(() => {
    return props.searchConfig.length > props.collapseCount
  })

  /**
   * Determine if a field should be shown based on collapse state
   * @param {Object} item - Config item
   * @param {Number} index - Index of the item
   */
  const shouldShow = (item, index) => {
    if (item.alwaysShow) return true
    if (!collapsed.value) return true
    return index < props.collapseCount
  }

  /**
   * Toggle collapse state
   */
  const toggleCollapse = () => {
    collapsed.value = !collapsed.value
    emits('collapse-change', collapsed.value)
  }

  /**
   * Handle search action
   */
  const handleSearch = () => {
    emits('search', searchParams.value)
  }

  const normalizeOptions = (options) => normalizeKeyLabelOptions(options)
  const resolveRawOptions = (options) => {
    const resolved = unref(options)
    return Array.isArray(resolved) ? resolved : []
  }

  const resolveCascaderShowAllLevels = (item) => {
    const cfg = item?.opetion || {}
    if (cfg.showAllLevels !== undefined) return cfg.showAllLevels
    if (cfg['show-all-levels'] !== undefined) return cfg['show-all-levels']
    if (item?.showAllLevels !== undefined) return item.showAllLevels
    if (item?.props?.showPrefix !== undefined) return item.props.showPrefix
    return true
  }

  /**
   * Handle reset action
   */
  const handleReset = () => {
    // Reset all fields in searchParams to undefined or empty string
    // We don't want to break the object reference, so we iterate keys
    // Assuming the parent handles the actual "reset" logic usually, but here we can clear values
    // Better to emit reset and let parent handle, or clear local model if it's a copy
    // Since we use v-model, we can just emit reset.
    // But standard behavior is to clear the form.

    // Create a clean object based on config
    const cleanParams = { ...searchParams.value }
    props.searchConfig.forEach((item) => {
      if (item.type === 'daterange' || item.type === 'cascader' || item.type === 'checkbox') {
        cleanParams[item.prop] = []
        return
      }
      cleanParams[item.prop] = undefined
    })

    emits('update:modelValue', cleanParams)
    emits('reset')
    // Automatically search after reset
    // emits('search', cleanParams) // Optional: decided by parent usually
  }
</script>

<style scoped>
  .operate-btn {
    padding: 0 22px;
  }
  .curd-search {
    background-color: #fff;
    border-radius: 4px;
  }
  .curd-search-card {
    margin: 10px 0;
  }

  .curd-search__item {
    text-align: right;
    margin: 0;
    margin-right: 10px; /* Added spacing between search items */
    margin-bottom: 10px; /* Added vertical spacing for wrapped items */
  }

  .curd-search__item:last-child {
    margin-right: 0;
  }

  .search-container {
    display: flex;
    /* justify-content: space-between; */
    align-items: flex-start;
    flex-wrap: wrap;
  }

  .layout-inline {
    justify-content: flex-start;

    .search-left {
      flex: initial;
      margin-right: 16px;
    }

    .search-right {
      margin-left: 0;
    }

    .search-filler {
      margin-left: auto; /* Push filler to the right in inline mode */
    }
  }

  .search-left {
    display: flex;
    flex-wrap: wrap;
    flex: 1;
    margin-bottom: -10px;
    /* Ensure it doesn't overlap with right side if very full, but flex:1 handles it */
  }

  .search-right {
    margin-left: 20px;
    flex-shrink: 0;
  }

  .search-filler {
    flex-shrink: 0;
    display: flex;
    align-items: center;
  }

  :deep(.el-input__wrapper) {
    border-radius: 99px;
  }
  :deep(.el-select__wrapper) {
    border-radius: 99px;
  }
  :deep(.el-range-editor.el-input__wrapper) {
    border-radius: 99px;
  }
</style>
