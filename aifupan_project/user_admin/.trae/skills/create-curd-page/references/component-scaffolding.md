# CURD Component Scaffolding

This document contains the source code for the CURD component library.

## 1. Core Container (`src/components/Curd/index.vue`)

```vue
<template>
  <div class="curd-container">
    <!-- Search -->
    <CurdSearch
      v-if="searchConfig && searchConfig.length"
      v-model="searchParams"
      :search-config="searchConfig"
      @search="handleSearch"
      @reset="handleReset"
    >
      <template #search-action>
        <slot name="search-action" />
      </template>
      <!-- Forward slots -->
      <template v-for="slot in Object.keys($slots)" #[slot]="scope">
        <slot :name="slot" v-bind="scope" />
      </template>
    </CurdSearch>

    <!-- Table -->
    <CurdTable
      :columns="mergedTableColumns"
      :data="tableData"
      :loading="loading"
      :selection="selection"
      :index="index"
      :show-add="showAdd"
      :show-operation="showOperation"
      :operation-width="operationWidth"
      :action-config="actionConfig"
      :custom-actions="customActions"
      :cache-key="tableCacheKey"
      @add="handleAdd"
      @refresh="getData"
      @action="handleAction"
      @selection-change="handleSelectionChange"
    >
      <template #toolbar-left>
        <slot name="toolbar-left" />
      </template>
      <template #toolbar-right>
        <slot name="toolbar-right" />
      </template>

      <!-- Forward column slots -->
      <template v-for="col in tableColumns" #[col.slotName]="scope">
        <slot :name="col.slotName" v-bind="scope" />
      </template>
    </CurdTable>

    <!-- Pagination -->
    <CurdPagination
      v-if="pagination"
      :total="total"
      v-model:page="paginationParams.page"
      v-model:limit="paginationParams.pageSize"
      @pagination="getData"
    />

    <!-- Form Dialog -->
    <CurdForm
      v-model:visible="dialogVisible"
      :mode="dialogMode"
      :form-config="formConfig"
      v-model="formData"
      :loading="formLoading"
      :width="dialogWidth"
      @submit="handleFormSubmit"
    >
      <!-- Forward form slots -->
      <template v-for="item in flatFormConfig" #[item.slotName]="scope">
        <slot :name="item.slotName" v-bind="scope" />
      </template>
    </CurdForm>
  </div>
</template>

<script setup>
  /**
   * @file Core Curd Component
   * @description Integrates Search, Table, Pagination, and Form components with CRUD logic.
   */
  import { ref, reactive, computed, onMounted, watch } from 'vue'
  import { ElMessage, ElMessageBox } from 'element-plus'
  import CurdSearch from './Search/index.vue'
  import CurdTable from './Table/index.vue'
  import CurdPagination from './Pagination/index.vue'
  import CurdForm from './Form/index.vue'

  const props = defineProps({
    // configs
    searchConfig: { type: Array, default: () => [] },
    tableColumns: { type: Array, default: () => [] },
    formConfig: { type: [Array, Object], default: () => [] },

    // API methods: { list, add, edit, del, get }
    api: { type: Object, required: true },

    // Behavior config
    autoLoad: { type: Boolean, default: true },
    queryCacheKey: { type: String, default: '' }, // For caching search params & page
    tableCacheKey: { type: String, default: '' }, // For caching column visibility

    // Pagination
    pagination: { type: Boolean, default: true },

    // Table features
    selection: { type: Boolean, default: false },
    index: { type: Boolean, default: false },
    showAdd: { type: Boolean, default: true },
    showOperation: { type: Boolean, default: true },
    operationWidth: { type: [String, Number], default: 200 },
    actionConfig: { type: Object, default: () => ({ view: true, edit: true, del: true }) },
    customActions: { type: Array, default: () => [] },

    // Dialog
    dialogWidth: { type: [String, Number], default: '500px' },

    // Hooks / Mappers
    beforeRequest: { type: Function }, // (params) => modifiedParams
    afterRequest: { type: Function }, // (data) => { list, total }
    dataMap: { type: Object, default: () => ({ list: 'list', total: 'total' }) } // Map backend response fields
  })

  const emit = defineEmits(['load', 'selection-change'])

  // State
  const loading = ref(false)
  const tableData = ref([])
  const total = ref(0)
  const searchParams = ref({})
  const paginationParams = reactive({
    page: 1,
    pageSize: 10
  })

  // Dialog State
  const dialogVisible = ref(false)
  const dialogMode = ref('add') // add, edit, view
  const formData = ref({})
  const formLoading = ref(false)

  // Helper to flatten form config for slot forwarding
  const flatFormConfig = computed(() => {
    if (Array.isArray(props.formConfig)) return props.formConfig
    return [...(props.formConfig.add || []), ...(props.formConfig.edit || []), ...(props.formConfig.view || [])]
  })

  // Merge table columns with search options
  const mergedTableColumns = computed(() => {
    return props.tableColumns.map((col) => {
      // If column already has options, use them
      if (col.options) return col

      // Try to find matching prop in searchConfig
      const searchItem = props.searchConfig.find((item) => item.prop === col.prop)
      if (searchItem && searchItem.options) {
        return {
          ...col,
          options: searchItem.options
        }
      }

      return col
    })
  })

  // --- Caching Logic ---
  const initCache = () => {
    if (props.queryCacheKey) {
      const cached = localStorage.getItem(`curd_query_${props.queryCacheKey}`)
      if (cached) {
        try {
          const { search, page, pageSize } = JSON.parse(cached)
          if (search) searchParams.value = search
          if (page) paginationParams.page = page
          if (pageSize) paginationParams.pageSize = pageSize
          return true
        } catch (e) {
          console.error('Failed to load query cache', e)
        }
      }
    }
    return false
  }

  const saveCache = () => {
    if (props.queryCacheKey) {
      const cacheData = {
        search: searchParams.value,
        page: paginationParams.page,
        pageSize: paginationParams.pageSize
      }
      localStorage.setItem(`curd_query_${props.queryCacheKey}`, JSON.stringify(cacheData))
    }
  }

  // --- Data Loading ---
  const getData = async () => {
    if (!props.api || !props.api.list) {
      console.warn('API list method is missing')
      return
    }

    loading.value = true
    try {
      // 1. Prepare Params
      let params = { ...searchParams.value }
      if (props.pagination) {
        params.page = paginationParams.page
        params.pageSize = paginationParams.pageSize
      }

      // Hook: beforeRequest
      if (props.beforeRequest) {
        params = props.beforeRequest(params)
      }

      // Save to cache
      saveCache()

      // 2. Call API
      const res = await props.api.list(params)

      // 3. Process Response
      let list = []
      let totalCount = 0

      if (props.afterRequest) {
        const result = props.afterRequest(res)
        list = result.list
        totalCount = result.total
      } else {
        // Helper to get nested property
        const getProp = (obj, path) => path.split('.').reduce((o, i) => (o ? o[i] : undefined), obj)

        list = getProp(res, props.dataMap.list) || []
        totalCount = getProp(res, props.dataMap.total) || 0
      }

      tableData.value = list
      total.value = totalCount
      emit('load', tableData.value)
    } catch (error) {
      console.error('Load data failed', error)
    } finally {
      loading.value = false
    }
  }

  const handleSearch = () => {
    paginationParams.page = 1
    getData()
  }

  const handleReset = () => {
    paginationParams.page = 1
    // searchParams is already reset by child component
    getData()
  }

  // --- Action Handling ---
  const handleAdd = () => {
    dialogMode.value = 'add'
    formData.value = {}
    dialogVisible.value = true
  }

  const handleAction = ({ type, row }) => {
    if (type === 'view') {
      dialogMode.value = 'view'
      formData.value = JSON.parse(JSON.stringify(row))
      dialogVisible.value = true
    } else if (type === 'edit') {
      dialogMode.value = 'edit'
      formData.value = JSON.parse(JSON.stringify(row))
      dialogVisible.value = true
    } else if (type === 'del') {
      handleDelete(row)
    } else {
      // Custom action logic if needed, but usually handled by parent via event
    }
  }

  const handleDelete = (row) => {
    ElMessageBox.confirm('请确认您是否需要删除此条数据？', '确认删除', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
      .then(async () => {
        if (props.api.del) {
          try {
            await props.api.del(row)
            ElMessage.success('删除成功')
            getData()
          } catch (error) {
            console.error(error)
          }
        }
      })
      .catch(() => {})
  }

  // --- Form Submission ---
  const handleFormSubmit = async (data) => {
    formLoading.value = true
    try {
      if (dialogMode.value === 'add' && props.api.add) {
        await props.api.add(data)
        ElMessage.success('新增成功')
      } else if (dialogMode.value === 'edit' && props.api.edit) {
        await props.api.edit(data)
        ElMessage.success('修改成功')
      }
      dialogVisible.value = false
      getData()
    } catch (error) {
      console.error(error)
    } finally {
      formLoading.value = false
    }
  }

  const handleSelectionChange = (val) => {
    emit('selection-change', val)
  }

  // Lifecycle
  onMounted(() => {
    initCache()
    if (props.autoLoad) {
      getData()
    }
  })

  // Expose getData for external refresh
  defineExpose({
    getData,
    searchParams
  })
</script>

<style scoped>
  .curd-container {
    padding: 10px;
  }
</style>
```

## 2. Table Component (`src/components/Curd/Table/index.vue`)

```vue
<template>
  <div class="curd-table">
    <!-- Toolbar -->
    <div class="curd-table__toolbar">
      <div class="toolbar-left">
        <slot name="toolbar-left">
          <el-button v-if="showAdd" type="primary" :icon="Plus" @click="emit('add')"> 新增 </el-button>
        </slot>
      </div>
      <div class="toolbar-right">
        <slot name="toolbar-right" />
        <el-tooltip content="刷新" placement="top">
          <el-button circle :icon="Refresh" @click="emit('refresh')" />
        </el-tooltip>
        <el-tooltip content="列设置" placement="top">
          <el-popover placement="bottom-end" :width="200" trigger="click">
            <template #reference>
              <el-button circle :icon="Setting" />
            </template>
            <div class="column-setting">
              <div class="column-setting-title">列展示</div>
              <el-checkbox-group v-model="visibleColumns">
                <div v-for="col in columns" :key="col.prop" class="column-setting-item">
                  <el-checkbox :label="col.prop">{{ col.label }}</el-checkbox>
                </div>
              </el-checkbox-group>
              <div class="column-setting-footer">
                <el-button size="small" link type="primary" @click="resetColumns">重置</el-button>
              </div>
            </div>
          </el-popover>
        </el-tooltip>
      </div>
    </div>

    <!-- Table -->
    <el-table v-loading="loading" :data="data" style="width: 100%" border @selection-change="handleSelectionChange">
      <el-table-column v-if="selection" type="selection" width="55" />
      <el-table-column v-if="index" type="index" label="序号" width="60" />

      <template v-for="col in columns" :key="col.prop">
        <el-table-column
          v-if="visibleColumns.includes(col.prop)"
          :prop="col.prop"
          :label="col.label"
          :width="col.width"
          :min-width="col.minWidth"
          :fixed="col.fixed"
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
      <el-table-column v-if="showOperation" label="操作" :width="operationWidth" fixed="right">
        <template #default="scope">
          <TableAction
            :row="scope.row"
            :actions="actionConfig"
            :custom-actions="customActions"
            @command="handleActionCommand"
          />
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
  import { ref, watch, onMounted } from 'vue'
  import { Plus, Refresh, Setting } from '@element-plus/icons-vue'
  import TableAction from './TableAction.vue'
  import RenderColumn from './RenderColumn.js'

  const props = defineProps({
    columns: { type: Array, default: () => [] },
    data: { type: Array, default: () => [] },
    loading: { type: Boolean, default: false },
    selection: { type: Boolean, default: false },
    index: { type: Boolean, default: false },
    showAdd: { type: Boolean, default: true },
    showOperation: { type: Boolean, default: true },
    operationWidth: { type: [String, Number], default: '200' },
    actionConfig: { type: Object, default: () => ({ view: true, edit: true, del: true }) },
    customActions: { type: Array, default: () => [] },
    cacheKey: { type: String, default: '' }
  })

  const emit = defineEmits(['add', 'refresh', 'selection-change', 'action'])

  // Column Visibility Management
  const visibleColumns = ref([])

  const initColumns = () => {
    if (props.cacheKey) {
      const cached = localStorage.getItem(`curd_table_cols_${props.cacheKey}`)
      if (cached) {
        try {
          visibleColumns.value = JSON.parse(cached)
          const allProps = props.columns.map((c) => c.prop)
          visibleColumns.value = visibleColumns.value.filter((p) => allProps.includes(p))
          return
        } catch (e) {
          console.error('Failed to parse cached columns', e)
        }
      }
    }
    visibleColumns.value = props.columns.map((col) => col.prop)
  }

  const saveColumns = () => {
    if (props.cacheKey) {
      localStorage.setItem(`curd_table_cols_${props.cacheKey}`, JSON.stringify(visibleColumns.value))
    }
  }

  const resetColumns = () => {
    visibleColumns.value = props.columns.map((col) => col.prop)
    saveColumns()
  }

  watch(
    visibleColumns,
    () => {
      saveColumns()
    },
    { deep: true }
  )

  onMounted(() => {
    initColumns()
  })

  const handleSelectionChange = (val) => {
    emit('selection-change', val)
  }

  const handleActionCommand = ({ type, row }) => {
    emit('action', { type, row })
  }

  const getEnumLabel = (row, col) => {
    const val = row[col.prop]
    if (!col.options) return val
    const option = col.options.find((opt) => opt.value === val)
    return option ? option.label : val
  }

  const getTagProps = (row, col) => {
    const val = row[col.prop]
    if (col.tagProps) {
      if (typeof col.tagProps === 'function') return col.tagProps(row, val)
      return col.tagProps
    }
    if (col.options) {
      const option = col.options.find((opt) => opt.value === val)
      if (option) {
        if (option.type) return { type: option.type }
        if (option.tagType) return { type: option.tagType }
      }
    }
    return {}
  }
</script>

<style scoped>
  .curd-table {
    background: #fff;
    border-radius: 4px;
  }
  .curd-table__toolbar {
    display: flex;
    justify-content: space-between;
    margin-bottom: 16px;
  }
  .toolbar-right {
    display: flex;
    gap: 8px;
  }
  .column-setting-title {
    font-weight: bold;
    margin-bottom: 8px;
    padding-bottom: 8px;
    border-bottom: 1px solid #eee;
  }
  .column-setting-item {
    margin-bottom: 4px;
  }
  .column-setting-footer {
    margin-top: 8px;
    padding-top: 8px;
    border-top: 1px solid #eee;
    text-align: right;
  }
</style>
```

## 3. Table Action (`src/components/Curd/Table/TableAction.vue`)

```vue
<template>
  <div class="table-action">
    <!-- Default Buttons -->
    <el-button v-if="actions.view" link type="primary" size="small" @click="handleAction('view')"> 查看 </el-button>
    <el-button v-if="actions.edit" link type="primary" size="small" @click="handleAction('edit')"> 编辑 </el-button>

    <!-- Delete with Popconfirm -->
    <el-popconfirm v-if="actions.del" :title="delPopconfirmTitle" @confirm="handleAction('del')">
      <template #reference>
        <el-button link type="danger" size="small"> 删除 </el-button>
      </template>
    </el-popconfirm>

    <!-- Custom Buttons -->
    <template v-for="btn in customActions" :key="btn.label">
      <template v-if="!btn.hidden">
        <!-- With Popconfirm -->
        <el-popconfirm
          v-if="btn.popconfirm"
          :title="typeof btn.popconfirm === 'string' ? btn.popconfirm : '确认执行此操作？'"
          @confirm="handleCustomAction(btn)"
        >
          <template #reference>
            <el-button link :type="btn.type || 'primary'" size="small">
              {{ btn.label }}
            </el-button>
          </template>
        </el-popconfirm>

        <!-- Without Popconfirm -->
        <el-button v-else link :type="btn.type || 'primary'" size="small" @click="handleCustomAction(btn)">
          {{ btn.label }}
        </el-button>
      </template>
    </template>
  </div>
</template>

<script setup>
  import { computed } from 'vue'

  const props = defineProps({
    row: { type: Object, required: true },
    actions: { type: Object, default: () => ({ view: true, edit: true, del: true }) },
    customActions: { type: Array, default: () => [] }
  })

  const emit = defineEmits(['command'])

  const delPopconfirmTitle = computed(() => {
    if (typeof props.actions.del === 'object' && props.actions.del.popconfirm) {
      return props.actions.del.popconfirm
    }
    return '确认删除该数据？'
  })

  const handleAction = (type) => {
    emit('command', { type, row: props.row })
  }

  const handleCustomAction = (btn) => {
    if (btn.handler) {
      btn.handler(props.row)
    } else {
      emit('command', { type: btn.command, row: props.row })
    }
  }
</script>
```

## 4. Render Helper (`src/components/Curd/Table/RenderColumn.js`)

```javascript
import { h } from 'vue'

export default {
  props: {
    row: Object,
    index: Number,
    render: Function
  },
  setup(props) {
    return () => {
      return props.render ? props.render(props.row, props.index) : null
    }
  }
}
```

## 5. Search Component (`src/components/Curd/Search/index.vue`)

```vue
<template>
  <div class="curd-search">
    <el-form
      ref="searchFormRef"
      :model="searchParams"
      :inline="true"
      label-width="80px"
      label-position="left"
      class="curd-search__form"
      :show-message="false"
    >
      <div class="search-container">
        <div class="search-left">
          <template v-for="(item, index) in searchConfig" :key="item.prop">
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
                @change="handleSearch"
              >
                <el-option v-for="opt in item.options" :key="opt.value" :label="opt.label" :value="opt.value" />
              </el-select>

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
                range-separator="至"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                value-format="YYYY-MM-DD"
                :style="{ width: item.width || '240px' }"
              />

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
        </div>

        <!-- Operation Buttons -->
        <div class="search-right">
          <el-form-item class="curd-search__item">
            <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
            <el-button :icon="Refresh" @click="handleReset">重置</el-button>
            <!-- Extra slots for search bar -->
            <slot name="search-action" />
          </el-form-item>
        </div>
      </div>
    </el-form>
  </div>
</template>

<script setup>
  import { ref, computed, watch } from 'vue'
  import { Search, Refresh, ArrowDown, ArrowUp } from '@element-plus/icons-vue'

  const props = defineProps({
    searchConfig: { type: Array, default: () => [] },
    modelValue: { type: Object, default: () => ({}) },
    defaultCollapsed: { type: Boolean, default: true },
    collapseCount: { type: Number, default: 3 }
  })

  const emits = defineEmits(['update:modelValue', 'search', 'reset', 'collapse-change'])

  const searchParams = computed({
    get: () => props.modelValue,
    set: (val) => emits('update:modelValue', val)
  })

  const collapsed = ref(props.defaultCollapsed)

  const showCollapseButton = computed(() => {
    return props.searchConfig.length > props.collapseCount
  })

  const shouldShow = (item, index) => {
    if (item.alwaysShow) return true
    if (!collapsed.value) return true
    return index < props.collapseCount
  }

  const toggleCollapse = () => {
    collapsed.value = !collapsed.value
    emits('collapse-change', collapsed.value)
  }

  const handleSearch = () => {
    emits('search', searchParams.value)
  }

  const handleReset = () => {
    const cleanParams = { ...searchParams.value }
    props.searchConfig.forEach((item) => {
      cleanParams[item.prop] = undefined
    })

    emits('update:modelValue', cleanParams)
    emits('reset')
  }
</script>

<style scoped>
  .curd-search {
    background-color: #fff;
    margin-bottom: 0px;
    border-radius: 4px;
    padding-bottom: 16px;
  }
  .curd-search__item {
    text-align: right;
    margin: 0;
  }
  .search-container {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
  }
  .search-left {
    display: flex;
    flex-wrap: wrap;
    flex: 1;
  }
  .search-right {
    margin-left: 20px;
    flex-shrink: 0;
  }
</style>
```

## 6. Pagination Component (`src/components/Curd/Pagination/index.vue`)

```vue
<template>
  <div :class="{ hidden: hidden }" class="curd-pagination-container">
    <div v-if="$slots.left" class="pagination-left">
      <slot name="left" />
    </div>

    <div class="pagination-wrapper" :class="paginationClass">
      <el-pagination
        :background="background"
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :layout="layout"
        :page-sizes="pageSizes"
        :total="total"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>

    <div v-if="$slots.right" class="pagination-right">
      <slot name="right" />
    </div>
  </div>
</template>

<script setup>
  import { computed, useSlots } from 'vue'

  const props = defineProps({
    total: { required: true, type: Number },
    page: { type: Number, default: 1 },
    limit: { type: Number, default: 10 },
    pageSizes: { type: Array, default: () => [10, 20, 30, 50] },
    layout: { type: String, default: 'total, sizes, prev, pager, next, jumper' },
    background: { type: Boolean, default: true },
    hidden: { type: Boolean, default: false }
  })

  const emit = defineEmits(['update:page', 'update:limit', 'pagination'])
  const slots = useSlots()

  const currentPage = computed({
    get() {
      return props.page
    },
    set(val) {
      emit('update:page', val)
    }
  })

  const pageSize = computed({
    get() {
      return props.limit
    },
    set(val) {
      emit('update:limit', val)
    }
  })

  const paginationClass = computed(() => {
    const hasLeft = !!slots.left
    const hasRight = !!slots.right
    if (hasLeft && hasRight) return 'is-center'
    else if (hasLeft || hasRight) return ''
    else return 'is-center'
  })

  const handleSizeChange = (val) => {
    emit('pagination', { page: currentPage.value, limit: val })
  }

  const handleCurrentChange = (val) => {
    emit('pagination', { page: val, limit: pageSize.value })
  }
</script>

<style scoped lang="scss">
  .curd-pagination-container {
    background: #fff;
    padding: 32px 16px;
    display: flex;
    align-items: center;
    justify-content: space-between;
  }
  .curd-pagination-container.hidden {
    display: none;
  }
  .pagination-wrapper {
    display: flex;
  }
  .pagination-wrapper.is-center {
    flex: 1;
    justify-content: center;
  }
</style>
```

## 7. Form Component (`src/components/Curd/Form/index.vue`)

```vue
<template>
  <el-dialog
    :title="title"
    :model-value="visible"
    :width="computedDialogWidth"
    :close-on-click-modal="false"
    @update:model-value="handleVisibleChange"
    @close="handleClose"
  >
    <el-form ref="formRef" :model="formData" :rules="rules" label-width="100px" :disabled="mode === 'view'">
      <el-row :gutter="20">
        <template v-for="item in currentConfig" :key="item.prop">
          <el-col :span="item.span || 24" v-if="shouldShow(item)">
            <el-form-item :label="item.label" :prop="item.prop">
              <!-- Custom Slot -->
              <slot v-if="item.slotName" :name="item.slotName" :row="item" :form="formData" :mode="mode" />

              <!-- Select -->
              <el-select
                v-else-if="item.type === 'select'"
                v-model="formData[item.prop]"
                :placeholder="item.placeholder"
                :style="{ width: item.width || '100%', maxWidth: item.width ? 'unset' : '350px' }"
              >
                <el-option v-for="opt in item.options" :key="opt.value" :label="opt.label" :value="opt.value" />
              </el-select>

              <!-- Radio -->
              <el-radio-group
                v-else-if="item.type === 'radio'"
                v-model="formData[item.prop]"
                :style="{ width: item.width || '100%', maxWidth: item.width ? 'unset' : '350px' }"
              >
                <el-radio v-for="opt in item.options" :key="opt.value" :label="opt.value">
                  {{ opt.label }}
                </el-radio>
              </el-radio-group>

              <!-- Checkbox -->
              <el-checkbox-group
                v-else-if="item.type === 'checkbox'"
                v-model="formData[item.prop]"
                :style="{ width: item.width || '100%', maxWidth: item.width ? 'unset' : '350px' }"
              >
                <el-checkbox v-for="opt in item.options" :key="opt.value" :label="opt.value">
                  {{ opt.label }}
                </el-checkbox>
              </el-checkbox-group>

              <!-- Date -->
              <el-date-picker
                v-else-if="item.type === 'date'"
                v-model="formData[item.prop]"
                type="date"
                value-format="YYYY-MM-DD"
                :style="{ width: item.width || '100%', maxWidth: item.width ? 'unset' : '350px' }"
              />

              <!-- Textarea -->
              <el-input
                v-else-if="item.type === 'textarea'"
                v-model="formData[item.prop]"
                type="textarea"
                :rows="item.rows || 3"
                :placeholder="item.placeholder"
                :style="{ width: item.width || '100%', maxWidth: item.width ? 'unset' : '350px' }"
              />

              <!-- Input -->
              <el-input
                v-else
                v-model="formData[item.prop]"
                :placeholder="item.placeholder"
                :type="item.inputType || 'text'"
                :style="{ width: item.width || '100%', maxWidth: item.width ? 'unset' : '350px' }"
              />
            </el-form-item>
          </el-col>
        </template>
      </el-row>
    </el-form>

    <template #footer>
      <span class="dialog-footer">
        <el-button @click="handleVisibleChange(false)">取消</el-button>
        <el-button v-if="mode !== 'view'" type="primary" @click="handleSubmit" :loading="loading"> 确认 </el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script setup>
  import { ref, computed, watch, nextTick } from 'vue'

  const props = defineProps({
    visible: { type: Boolean, default: false },
    mode: { type: String, default: 'add' },
    formConfig: { type: [Array, Object], default: () => [] },
    modelValue: { type: Object, default: () => ({}) },
    loading: { type: Boolean, default: false },
    width: { type: [String, Number], default: '500px' }
  })

  const emit = defineEmits(['update:visible', 'update:modelValue', 'submit', 'close'])

  const formRef = ref(null)

  const formData = computed({
    get: () => props.modelValue,
    set: (val) => emit('update:modelValue', val)
  })

  const title = computed(() => {
    const map = { add: '新增', edit: '编辑', view: '查看' }
    return map[props.mode] || '操作'
  })

  const currentConfig = computed(() => {
    if (Array.isArray(props.formConfig)) return props.formConfig
    return props.formConfig[props.mode] || []
  })

  const rules = computed(() => {
    const r = {}
    currentConfig.value.forEach((item) => {
      if (item.rules) r[item.prop] = item.rules
    })
    return r
  })

  const computedDialogWidth = computed(() => {
    if (typeof props.width === 'number') return `${props.width}px`
    if (!props.width) return '500px'
    return props.width
  })

  const shouldShow = (item) => {
    if (typeof item.hidden === 'function') return !item.hidden(formData.value, props.mode)
    return !item.hidden
  }

  const handleVisibleChange = (val) => {
    emit('update:visible', val)
  }

  const handleClose = () => {
    if (formRef.value) formRef.value.resetFields()
    emit('close')
  }

  const handleSubmit = async () => {
    if (!formRef.value) return
    await formRef.value.validate((valid) => {
      if (valid) emit('submit', formData.value)
    })
  }

  defineExpose({ formRef })
</script>
```
