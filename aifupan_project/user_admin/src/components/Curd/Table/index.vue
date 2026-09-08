<template>
  <div class="curd-table">
    <!-- Toolbar -->
    <!-- <CurdToolbar
      v-if="showToolbarComputed"
      :show-add="showAdd"
      :show-refresh="showToolbarRight"
      :show-option="showToolbarRight"
      :columns="columns"
      :visible-columns="currentVisibleColumns"
      @add="emit('add')"
      @refresh="emit('refresh')"
      @update:visibleColumns="handleColumnChange"
    >
      <template #toolbar-left>
        <slot name="toolbar-left" />
      </template>
      <template #toolbar-right>
        <slot name="toolbar-right" />
      </template>
    </CurdToolbar> -->

    <!-- Table -->
    <el-table
      v-loading="loading"
      :data="data"
      style="width: 100%; border-radius: 6px"
      :border="border"
      @selection-change="handleSelectionChange"
      v-bind="$attrs"
    >
      <el-table-column v-if="selection" type="selection" width="55" />
      <el-table-column v-if="index" type="index" label="序号" width="60" />

      <template v-for="col in columns" :key="col.prop">
        <el-table-column
          v-if="currentVisibleColumns.includes(col.prop)"
          :prop="col.prop"
          :label="col.label"
          :width="col.width"
          :min-width="col.minWidth"
          :fixed="col.fixed"
          :align="col.align"
          :header-align="col.headerAlign || col.align"
          :show-overflow-tooltip="col.showOverflowTooltip"
        >
          <template #default="scope">
            <!-- 1. Slot -->
            <slot v-if="col.slotName" :name="col.slotName" :row="scope.row" :index="scope.$index" />

            <!-- 3. Render Function -->
            <RenderColumn v-else-if="col.render" :row="scope.row" :index="scope.$index" :render="col.render" />

            <!-- 4. Tag Render -->
            <span v-else-if="col.type === 'tag' || col.tag">
              <el-tag
                v-if="scope.row[col.prop] !== undefined && scope.row[col.prop] !== null"
                v-bind="getTagProps(scope.row, col)"
              >
                {{ getEnumLabel(scope.row, col) }}
              </el-tag>
            </span>

            <!-- 5. Enum/Options Render -->
            <span v-else-if="col.options">
              {{ getEnumLabel(scope.row, col) }}
            </span>

            <!-- 6. Formatter -->
            <span v-else-if="col.formatter">
              {{ col.formatter(scope.row, col) }}
            </span>

            <!-- 7. Default -->
            <span v-else>{{ scope.row[col.prop] }}</span>
          </template>
        </el-table-column>
      </template>

      <!-- Operation Column -->
      <el-table-column
        v-if="showOperation"
        label="操作"
        :width="operationWidth"
        fixed="right"
        align="center"
        header-align="center"
      >
        <template #default="scope">
          <TableAction
            :row="scope.row"
            :view-text="viewText"
            :edit-text="editText"
            :del-text="delText"
            :actions="actionConfig"
            :custom-actions="customActions"
            :action-button-size="actionButtonSize"
            :list-permission-code="listPermissionCodeComputed"
            @command="handleActionCommand"
          />
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
  /**
   * @file Table Component for Curd Module
   * @description Provides a configurable table with dynamic columns and operations.
   */
  import { ref, watch, onMounted, useSlots, computed } from 'vue'
  import { useRoute } from 'vue-router'
  import TableAction from './TableAction.vue'
  import RenderColumn from './RenderColumn.js'
  import CurdToolbar from '../components/CurdToolbar.vue'
  import { normalizeKeyLabelOptions } from '@/utils/options'

  const slots = useSlots()
  const route = useRoute()

  const props = defineProps({
    columns: {
      type: Array,
      default: () => []
    },
    data: {
      type: Array,
      default: () => []
    },
    loading: {
      type: Boolean,
      default: false
    },
    selection: {
      type: Boolean,
      default: false
    },
    index: {
      type: Boolean,
      default: false
    },
    border: {
      type: Boolean,
      default: true
    },
    showOperation: {
      type: Boolean,
      default: true
    },
    showToolbarRight: {
      type: Boolean,
      default: true
    },
    operationWidth: {
      type: [String, Number],
      default: '200'
    },
    actionConfig: {
      type: Object, // { view, edit, del }
      default: () => ({ view: true, edit: true, del: true })
    },
    customActions: {
      type: Array,
      default: () => []
    },
    /**
     * Controlled visible columns
     */
    visibleColumns: {
      type: Array,
      default: undefined
    },
    /**
     * Whether to show the internal toolbar
     */
    showToolbar: {
      type: Boolean,
      default: true
    },
    /**
     * Unique key for caching column settings
     */
    cacheKey: {
      type: String,
      default: ''
    },
    viewText: {
      type: String,
      default: '查看'
    },
    editText: {
      type: String,
      default: '编辑'
    },
    delText: {
      type: String,
      default: '删除'
    },
    actionButtonSize: {
      type: String,
      default: 'default'
    },
    listPermissionCode: {
      type: String,
      default: ''
    }
  })

  const emit = defineEmits(['add', 'refresh', 'selection-change', 'action', 'update:visibleColumns'])

  const listPermissionCodeComputed = computed(() => {
    const direct = String(props.listPermissionCode || '').trim()
    if (direct) return direct
    return String(route.meta?.permissionCode || '').trim()
  })

  // Compute if the entire toolbar should be visible
  const showToolbarComputed = computed(() => {
    if (!props.showToolbar) return false
    const hasLeftContent = props.showAdd || !!slots['toolbar-left']
    const hasRightContent = props.showToolbarRight // If toolbar right is enabled, it has default buttons
    return hasLeftContent || hasRightContent
  })

  // Column Visibility Management
  const internalVisibleColumns = ref([])

  const currentVisibleColumns = computed({
    get: () => (props.visibleColumns !== undefined ? props.visibleColumns : internalVisibleColumns.value),
    set: (val) => {
      if (props.visibleColumns !== undefined) {
        emit('update:visibleColumns', val)
      } else {
        internalVisibleColumns.value = val
      }
    }
  })

  const initColumns = () => {
    // If controlled, do nothing (parent handles it)
    if (props.visibleColumns !== undefined) return

    // Load from cache if available
    if (props.cacheKey) {
      const cached = localStorage.getItem(`curd_table_cols_${props.cacheKey}`)
      if (cached) {
        try {
          internalVisibleColumns.value = JSON.parse(cached)
          // Verify columns still exist (in case config changed)
          const allProps = props.columns.map((c) => c.prop)
          internalVisibleColumns.value = internalVisibleColumns.value.filter((p) => allProps.includes(p))
          return
        } catch (e) {
          console.error('Failed to parse cached columns', e)
        }
      }
    }
    // Default all visible
    internalVisibleColumns.value = props.columns.map((col) => col.prop)
  }

  const saveColumns = () => {
    if (props.cacheKey && props.visibleColumns === undefined) {
      localStorage.setItem(`curd_table_cols_${props.cacheKey}`, JSON.stringify(internalVisibleColumns.value))
    }
  }

  const resetColumns = () => {
    const all = props.columns.map((col) => col.prop)
    if (props.visibleColumns !== undefined) {
      emit('update:visibleColumns', all)
    } else {
      internalVisibleColumns.value = all
      saveColumns()
    }
  }

  watch(
    internalVisibleColumns,
    () => {
      saveColumns()
    },
    { deep: true }
  )

  // Initialize on mount
  onMounted(() => {
    initColumns()
  })

  const handleSelectionChange = (val) => {
    emit('selection-change', val)
  }

  const handleActionCommand = ({ type, row }) => {
    emit('action', { type, row })
  }

  const handleColumnChange = (val) => {
    if (props.visibleColumns !== undefined) {
      emit('update:visibleColumns', val)
    } else {
      internalVisibleColumns.value = val
    }
  }

  // Helpers for Enum and Tag Rendering
  const getEnumLabel = (row, col) => {
    const val = row[col.prop]
    if (!col.options) return val

    const option = normalizeKeyLabelOptions(col.options).find((opt) => String(opt.key) === String(val))
    return option ? option.label : val
  }

  const getTagProps = (row, col) => {
    const val = row[col.prop]
    // 1. Check col.tagProps (function or object)
    if (col.tagProps) {
      if (typeof col.tagProps === 'function') {
        return col.tagProps(row, val)
      }
      return col.tagProps
    }

    // 2. Check option.type or option.tagType
    if (col.options) {
      const option = normalizeKeyLabelOptions(col.options).find((opt) => String(opt.key) === String(val))
      if (option) {
        // Allow 'type' or 'tagType' in option to define color
        if (option.type) return { type: option.type }
        if (option.tagType) return { type: option.tagType }
      }
    }

    // Default fallback
    return {}
  }
</script>

<style scoped>
  .curd-table {
    background: #fff;
    border-radius: 4px;
  }

  /* 设置表头背景色为 #fbfbfb */
  :deep(.el-table th.el-table__cell) {
    background-color: #fbfbfb;
  }
</style>
