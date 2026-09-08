<template>
  <div
    class="curd-container"
    :class="{ 'curd-container--split-search': splitSearchBoxEnabled }"
    :style="curdContainerStyle"
  >
    <template v-if="splitSearchBoxEnabled">
      <div v-if="isRowVisible(layout[0])" class="curd-box curd-box--search">
        <div
          v-show="isRowVisible(layout[0])"
          class="curd-row"
          :class="[
            {
              'curd-row--split': isSplitRow(layout[0]),
              'curd-row-card-top': cardIndexMaps[0]?.top,
              'curd-row-card-bottom': cardIndexMaps[0]?.bottom
            },
            getRowClass(layout[0]),
            isCard(layout[0], 0)
          ]"
        >
          <template v-if="isSplitRow(layout[0])">
            <div class="curd-row__left">
              <template v-for="(compName, cIndex) in layout[0][0]" :key="`l0-${cIndex}`">
                <slot :name="`${compName}Before`" />
                <slot v-if="$slots[compName]" :name="compName" />
                <component
                  v-else
                  :is="getComponent(compName)"
                  v-bind="getComponentProps(compName)"
                  v-on="getComponentEvents(compName)"
                  :class="`${compName}-box`"
                >
                  <template
                    v-for="slotItem in getComponentSlots(compName)"
                    :key="slotItem.source"
                    #[slotItem.dest]="slotProps"
                  >
                    <slot :name="slotItem.source" v-bind="slotProps" />
                  </template>
                </component>
                <slot :name="`${compName}After`" />
              </template>
            </div>
            <div class="curd-row__right">
              <template v-for="(compName, cIndex) in layout[0][1]" :key="`r0-${cIndex}`">
                <slot :name="`${compName}Before`" />
                <slot v-if="$slots[compName]" :name="compName" />
                <component
                  v-else
                  :is="getComponent(compName)"
                  v-bind="getComponentProps(compName)"
                  v-on="getComponentEvents(compName)"
                  :class="`${compName}-box`"
                >
                  <template
                    v-for="slotItem in getComponentSlots(compName)"
                    :key="slotItem.source"
                    #[slotItem.dest]="slotProps"
                  >
                    <slot :name="slotItem.source" v-bind="slotProps" />
                  </template>
                </component>
                <slot :name="`${compName}After`" />
              </template>
            </div>
          </template>
          <template v-else>
            <template v-for="(compName, cIndex) in layout[0]" :key="`c0-${cIndex}`">
              <slot :name="`${compName}Before`" />
              <slot v-if="$slots[compName]" :name="compName" class="curd-item" />
              <component
                v-else
                :is="getComponent(compName)"
                v-bind="getComponentProps(compName)"
                v-on="getComponentEvents(compName)"
                class="curd-item"
                :class="`${compName}-box`"
              >
                <template
                  v-for="slotItem in getComponentSlots(compName)"
                  :key="slotItem.source"
                  #[slotItem.dest]="slotProps"
                >
                  <slot :name="slotItem.source" v-bind="slotProps" />
                </template>
              </component>
              <slot :name="`${compName}After`" />
            </template>
          </template>
        </div>
      </div>

      <div class="curd-box curd-box--content">
        <div
          v-for="(row, rIndex) in layout.slice(1)"
          :key="rIndex + 1"
          class="curd-row"
          v-show="isRowVisible(row)"
          :class="[
            {
              'curd-row--split': isSplitRow(row),
              'curd-row-card-top': cardIndexMaps[rIndex + 1]?.top,
              'curd-row-card-bottom': cardIndexMaps[rIndex + 1]?.bottom
            },
            getRowClass(row),
            isCard(row, rIndex + 1)
          ]"
        >
          <template v-if="isSplitRow(row)">
            <div class="curd-row__left">
              <template v-for="(compName, cIndex) in row[0]" :key="`l-${rIndex + 1}-${cIndex}`">
                <slot :name="`${compName}Before`" />
                <slot v-if="$slots[compName]" :name="compName" />
                <component
                  v-else
                  :is="getComponent(compName)"
                  v-bind="getComponentProps(compName)"
                  v-on="getComponentEvents(compName)"
                  :class="`${compName}-box`"
                >
                  <template
                    v-for="slotItem in getComponentSlots(compName)"
                    :key="slotItem.source"
                    #[slotItem.dest]="slotProps"
                  >
                    <slot :name="slotItem.source" v-bind="slotProps" />
                  </template>
                </component>
                <slot :name="`${compName}After`" />
              </template>
            </div>
            <div class="curd-row__right">
              <template v-for="(compName, cIndex) in row[1]" :key="`r-${rIndex + 1}-${cIndex}`">
                <slot :name="`${compName}Before`" />
                <slot v-if="$slots[compName]" :name="compName" />
                <component
                  v-else
                  :is="getComponent(compName)"
                  v-bind="getComponentProps(compName)"
                  v-on="getComponentEvents(compName)"
                  :class="`${compName}-box`"
                >
                  <template
                    v-for="slotItem in getComponentSlots(compName)"
                    :key="slotItem.source"
                    #[slotItem.dest]="slotProps"
                  >
                    <slot :name="slotItem.source" v-bind="slotProps" />
                  </template>
                </component>
                <slot :name="`${compName}After`" />
              </template>
            </div>
          </template>
          <template v-else>
            <template v-for="(compName, cIndex) in row" :key="`${rIndex + 1}-${cIndex}`">
              <slot :name="`${compName}Before`" />
              <slot v-if="$slots[compName]" :name="compName" class="curd-item" />
              <component
                v-else
                :is="getComponent(compName)"
                v-bind="getComponentProps(compName)"
                v-on="getComponentEvents(compName)"
                class="curd-item"
                :class="`${compName}-box`"
              >
                <template
                  v-for="slotItem in getComponentSlots(compName)"
                  :key="slotItem.source"
                  #[slotItem.dest]="slotProps"
                >
                  <slot :name="slotItem.source" v-bind="slotProps" />
                </template>
              </component>
              <slot :name="`${compName}After`" />
            </template>
          </template>
        </div>
      </div>
    </template>

    <template v-else>
      <div
        v-for="(row, rIndex) in layout"
        :key="rIndex"
        class="curd-row"
        v-show="isRowVisible(row)"
        :class="[
          {
            'curd-row--split': isSplitRow(row),
            'curd-row-card-top': cardIndexMaps[rIndex]?.top,
            'curd-row-card-bottom': cardIndexMaps[rIndex]?.bottom
          },
          getRowClass(row),
          isCard(row, rIndex)
        ]"
      >
        <template v-if="isSplitRow(row)">
          <div class="curd-row__left">
            <template v-for="(compName, cIndex) in row[0]" :key="`l-${cIndex}`">
              <slot :name="`${compName}Before`" />
              <slot v-if="$slots[compName]" :name="compName" />
              <component
                v-else
                :is="getComponent(compName)"
                v-bind="getComponentProps(compName)"
                v-on="getComponentEvents(compName)"
                :class="`${compName}-box`"
              >
                <template
                  v-for="slotItem in getComponentSlots(compName)"
                  :key="slotItem.source"
                  #[slotItem.dest]="slotProps"
                >
                  <slot :name="slotItem.source" v-bind="slotProps" />
                </template>
              </component>
              <slot :name="`${compName}After`" />
            </template>
          </div>
          <div class="curd-row__right">
            <template v-for="(compName, cIndex) in row[1]" :key="`r-${cIndex}`">
              <slot :name="`${compName}Before`" />
              <slot v-if="$slots[compName]" :name="compName" />
              <component
                v-else
                :is="getComponent(compName)"
                v-bind="getComponentProps(compName)"
                v-on="getComponentEvents(compName)"
                :class="`${compName}-box`"
              >
                <template
                  v-for="slotItem in getComponentSlots(compName)"
                  :key="slotItem.source"
                  #[slotItem.dest]="slotProps"
                >
                  <slot :name="slotItem.source" v-bind="slotProps" />
                </template>
              </component>
              <slot :name="`${compName}After`" />
            </template>
          </div>
        </template>
        <template v-else>
          <template v-for="(compName, cIndex) in row" :key="cIndex">
            <slot :name="`${compName}Before`" />
            <slot v-if="$slots[compName]" :name="compName" class="curd-item" />
            <component
              v-else
              :is="getComponent(compName)"
              v-bind="getComponentProps(compName)"
              v-on="getComponentEvents(compName)"
              class="curd-item"
              :class="`${compName}-box`"
            >
              <template
                v-for="slotItem in getComponentSlots(compName)"
                :key="slotItem.source"
                #[slotItem.dest]="slotProps"
              >
                <slot :name="slotItem.source" v-bind="slotProps" />
              </template>
            </component>
            <slot :name="`${compName}After`" />
          </template>
        </template>
      </div>
    </template>
    <!-- Form Dialog -->
    <CurdForm
      v-model:visible="dialogVisible"
      :mode="dialogMode"
      :form-config="_formConfig.items"
      v-model="formData"
      :loading="formLoading"
      :width="_formConfig.width"
      :type="_formConfig.type"
      @submit="handleFormSubmit"
      @empty-action="handleFormEmptyAction"
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
   * @description Integrates Search, Table, Pagination, and Form components with flexible layout.
   */
  import { ref, reactive, computed, onMounted, onActivated, watch, useSlots, useAttrs } from 'vue'
  import { useRoute, useRouter } from 'vue-router'
  import { ElMessage, ElMessageBox } from 'element-plus'
  import { usePermissionStore } from '@/auth/store'

  // Components
  import CurdTitle from './components/CurdTitle.vue'
  import CurdAdd from './components/CurdAdd.vue'
  import CurdRefresh from './components/CurdRefresh.vue'
  import CurdOption from './components/CurdOption.vue'
  import CurdToolbar from './components/CurdToolbar.vue'
  import CurdSearch from './Search/index.vue'
  import CurdTable from './Table/index.vue'
  import CurdPagination from './Pagination/index.vue'
  import CurdForm from './Form/index.vue'

  const props = defineProps({
    curdContainerStyle: {
      type: Object,
      default: () => ({})
    },
    splitSearchBox: {
      type: Boolean,
      default: true
    },
    // Layout Config
    layout: {
      type: Array,
      default: () => [['search'], [['title'], ['add', 'refresh', 'option']], ['table'], ['page']]
    },

    layoutConfig: {
      type: Object,
      default: () => ({})
    },

    // API methods: { list, add, edit, del, get }
    api: { type: Object, required: true },

    // --- New Config Props (Categorized) ---
    searchConfig: { type: [Array, Object], default: undefined }, // Array for items, or Object with { items, ...config }
    formConfig: { type: [Array, Object], default: () => [] },
    addConfig: { type: Object, default: () => ({}) }, // { show: true, text: '新增', ... }
    pageConfig: { type: Object, default: () => ({}) }, // { show: true, pageSize: 10, ... }
    tableConfig: { type: Object, default: () => ({}) }, // { columns: [], selection: false, index: false, border: true, ... }
    optionConfig: { type: Object, default: () => ({}) }, // { show: true, ... }
    refreshConfig: { type: Object, default: () => ({}) }, // { show: true, ... }
    treeProps: { type: Object, default: () => ({}) }, // { children: 'children', hasChildren: 'hasChildren' }
    // --- Legacy Props (For Backward Compatibility) ---
    // These will be merged into the new config objects
    tableColumns: { type: Array, default: undefined },
    autoLoad: { type: Boolean, default: true },
    queryCacheKey: { type: String, default: '' },
    tableCacheKey: { type: String, default: '' },
    tableCacheByRoute: { type: Boolean, default: true },
    pagination: { type: Boolean, default: undefined },
    labelWidth: { type: [String, Number], default: '' },
    selection: { type: Boolean, default: undefined },
    index: { type: Boolean, default: undefined },
    border: { type: Boolean, default: undefined },
    showAdd: { type: Boolean, default: undefined },
    showOperation: { type: Boolean, default: undefined },
    showToolbarRight: { type: Boolean, default: undefined },
    operationWidth: { type: [String, Number], default: undefined },
    actionConfig: { type: Object, default: undefined },
    customActions: { type: Array, default: undefined },
    actionBefore: { type: Function, default: undefined },
    viewAction: { type: Function, default: undefined },
    editAction: { type: Function, default: undefined },
    delAction: { type: Function, default: undefined },
    dialogWidth: { type: [String, Number], default: undefined },
    title: { type: String, default: '' },
    showTotal: { type: Boolean, default: undefined },
    totalUnit: { type: String, default: '' },
    beforeRequest: { type: Function },
    afterRequest: { type: Function },
    dataMap: { type: Object, default: () => ({ list: 'list', total: 'total' }) },
    viewText: { type: String, default: '查看' },
    editText: { type: String, default: '编辑' },
    delText: { type: String, default: '删除' },
    listPermissionCode: { type: String, default: '' }
  })

  const emit = defineEmits(['load', 'selection-change', 'action'])
  const route = useRoute()
  const router = useRouter()
  const permissionStore = usePermissionStore()

  const listPermissionCodeValue = computed(() =>
    String(props.listPermissionCode || route.meta?.permissionCode || '').trim()
  )

  // --- Config Normalization (Merge Legacy Props) ---
  const _searchConfig = computed(() => {
    const raw = Array.isArray(props.searchConfig) && props.searchConfig?.length === 0 ? undefined : props.searchConfig
    // If searchConfig is not provided (undefined), return null to indicate no search
    if (raw === undefined) return null

    const base = Array.isArray(raw) ? { items: raw } : raw
    // Even if provided as [], treat as valid but empty search
    return {
      items: base.items || [],
      labelWidth: props.labelWidth || base.labelWidth || '',
      ...base
    }
  })

  const _tableConfig = computed(() => {
    return {
      columns: props.tableColumns || props.tableConfig.columns || [],
      selection: props.selection !== undefined ? props.selection : props.tableConfig.selection || false,
      index: props.index !== undefined ? props.index : props.tableConfig.index || false,
      border: props.border !== undefined ? props.border : props.tableConfig.border !== false, // default true
      showOperation:
        props.showOperation !== undefined ? props.showOperation : props.tableConfig.showOperation !== false, // default true
      operationWidth: props.operationWidth || props.tableConfig.operationWidth || 200,
      actionConfig: props.actionConfig || props.tableConfig.actionConfig || { view: true, edit: true, del: true },
      customActions: props.customActions || props.tableConfig.customActions || [],
      cacheKey: props.tableCacheKey || props.tableConfig.cacheKey || (props.tableCacheByRoute ? route.path : ''),
      viewText: props.viewText || props.tableConfig.viewText || '查看',
      editText: props.editText || props.tableConfig.editText || '编辑',
      delText: props.delText || props.tableConfig.delText || '删除',
      actionButtonSize: props.tableConfig.actionButtonSize || 'default',
      ...props.tableConfig
    }
  })

  const _formConfig = computed(() => {
    return {
      items: props.formConfig, // Direct pass, CurdForm handles array/obj check
      width: props.dialogWidth || (props.formConfig && props.formConfig.width) || '500px',
      type: (props.formConfig && props.formConfig.type) || 'drawer',
      ...props.formConfig
    }
  })

  const _pageConfig = computed(() => {
    return {
      show: props.pagination !== undefined ? props.pagination : props.pageConfig.show !== false, // default true
      pageSize: (props.pageConfig && props.pageConfig.pageSize) || 10,
      ...props.pageConfig
    }
  })

  const _addConfig = computed(() => {
    const listCode = listPermissionCodeValue.value
    const addCode = listCode ? permissionStore.getActionPermissionCode(listCode, 'add') : ''
    const canAdd = listCode ? Boolean(addCode) && permissionStore.hasPermission(addCode) : true

    return {
      show: (props.showAdd !== undefined ? props.showAdd : props.addConfig.show !== false) && canAdd,
      ...props.addConfig
    }
  })

  const _refreshConfig = computed(() => {
    // refresh logic usually tied to showToolbarRight in legacy
    // Default show to false if not explicitly enabled
    const show = props.showToolbarRight !== undefined ? props.showToolbarRight : props.refreshConfig.show === true
    return {
      show,
      ...props.refreshConfig
    }
  })

  const _optionConfig = computed(() => {
    const show =
      props.optionConfig && props.optionConfig.show !== undefined
        ? props.optionConfig.show
        : props.showToolbarRight !== undefined
          ? props.showToolbarRight
          : true
    return {
      show,
      ...props.optionConfig
    }
  })

  // --- Layout Logic ---
  const isSplitRow = (row) => {
    return Array.isArray(row) && row.length > 0 && Array.isArray(row[0])
  }

  const splitSearchBoxEnabled = computed(() => {
    if (!props.splitSearchBox) return false
    if (!Array.isArray(props.layout) || props.layout.length <= 1) return false
    const first = props.layout[0]
    if (!Array.isArray(first) || isSplitRow(first)) return false
    return first.includes('search')
  })

  const isComponentVisible = (name) => {
    if (slots[name] || slots[`${name}Before`] || slots[`${name}After`]) return true
    if (name === 'search') return !!_searchConfig.value
    if (name === 'add') return !!_addConfig.value.show
    if (name === 'refresh') return !!_refreshConfig.value.show
    if (name === 'option') return !!_optionConfig.value.show
    if (name === 'title') return !!props.title || !!props.showTotal
    if (name === 'toolbar') return _addConfig.value.show || _refreshConfig.value.show || _optionConfig.value.show
    if (name === 'page') return !!_pageConfig.value.show
    return true
  }

  const isRowVisible = (row) => {
    if (isSplitRow(row)) {
      return row[0].some(isComponentVisible) || row[1].some(isComponentVisible)
    } else {
      return row.some(isComponentVisible)
    }
  }

  const componentMap = {
    title: CurdTitle,
    add: CurdAdd,
    refresh: CurdRefresh,
    option: CurdOption,
    toolbar: CurdToolbar,
    search: CurdSearch,
    table: CurdTable,
    page: CurdPagination
  }

  const getRowClass = (row) => {
    if (row.some((item) => Array.isArray(item))) {
      return [...row.map((item) => item.join('-') + '-row')]
    } else {
      return row.map((item) => `${item}-row`)
    }
  }

  // 卡片显示索引
  let cardIndexMaps = {}
  let lastTop = 0
  const isCard = (row, index) => {
    if (splitSearchBoxEnabled.value) {
      cardIndexMaps[index] = { bottom: false, top: false }
      return ''
    }
    cardIndexMaps[index] = {
      bottom: false,
      top: false
    }
    if (props.layoutConfig[index]?.isCard || props.layoutConfig[row?.join('-')]?.isCard) {
      //在卡片样式添加之后需要对前后的行加入对应的卡片样式用以区分卡片块的分布
      let i = index - 1 < 0 ? 9999 : index - 1
      // 计算添加结尾卡片，如果当前卡片添加了卡片样式，则需要将上一个行的底部添加卡片圆角
      if (i < 9999) {
        cardIndexMaps[i].bottom = true
      }
      lastTop = index + 1
      // 添加卡片样式
      return 'row-card-box'
    } else {
      // lastTop 表示上次添加的顶部卡片圆角
      if (lastTop === index) {
        cardIndexMaps[lastTop].top = true
      }
      if (index === props.layout?.length - 1) {
        cardIndexMaps[index].bottom = true
      }
      return ``
    }
  }

  const getComponent = (name) => {
    // Logic to hide search if no config
    if (name === 'search' && !_searchConfig.value) {
      return 'div' // render empty div or null
    }
    // Logic to hide add/refresh/option/toolbar based on config
    if (name === 'add' && !_addConfig.value.show) return 'div'
    if (name === 'refresh' && !_refreshConfig.value.show) return 'div'
    if (name === 'option' && !_optionConfig.value.show) return 'div'

    return componentMap[name] || name // Fallback to string name for custom components or HTML tags
  }

  const getDestSlotName = (compName, slotKey) => {
    const prefix = `${compName}-`
    if (slotKey.startsWith(prefix)) {
      return slotKey.slice(prefix.length)
    }
    return slotKey // Also pass original slot name for backward compatibility and simpler usage
  }

  const slots = useSlots()
  const attrs = useAttrs()

  const getComponentSlots = (compName) => {
    return Object.keys(slots).map((key) => ({
      source: key,
      dest: getDestSlotName(compName, key)
    }))
  }

  // --- State ---
  const loading = ref(false)
  const tableData = ref([])
  const total = ref(0)
  const searchParams = ref({})
  const paginationParams = reactive({
    page: 1,
    pageSize: _pageConfig.value.pageSize || 10
  })
  const visibleColumns = ref([]) // Controlled visible columns

  // Dialog State
  const dialogVisible = ref(false)
  const dialogMode = ref('add') // add, edit, view
  const formData = ref({})
  const formLoading = ref(false)
  const formDraftKey = computed(() => {
    const titlePart = props.title ? `_${props.title}` : ''
    return `curd_form_draft_${route.path}${titlePart}`
  })

  // --- Computed Props ---
  const flatFormConfig = computed(() => {
    const conf = _formConfig.value.items
    const flatten = (items) => {
      if (!Array.isArray(items)) return []
      return items.flatMap((it) => {
        if (it && it.type === 'group' && Array.isArray(it.children)) {
          return flatten(it.children)
        }
        return it ? [it] : []
      })
    }
    if (Array.isArray(conf)) return flatten(conf)
    return flatten([...(conf.add || []), ...(conf.edit || []), ...(conf.view || [])])
  })

  const mergedTableColumns = computed(() => {
    return _tableConfig.value.columns.map((col) => {
      if (col.options) return col
      const searchItem = _searchConfig.value?.items?.find((item) => item.prop === col.prop)
      if (searchItem && searchItem.options) {
        return { ...col, options: searchItem.options }
      }
      return col
    })
  })

  const tableActionConfigWithPermission = computed(() => {
    return _tableConfig.value.actionConfig || {}
  })

  const customActionsWithPermission = computed(() => {
    return Array.isArray(_tableConfig.value.customActions) ? _tableConfig.value.customActions : []
  })

  // --- Component Props & Events ---
  const getComponentProps = (name) => {
    switch (name) {
      case 'title':
        return {
          title: props.title,
          showTotal: props.showTotal,
          total: total.value,
          unit: props.totalUnit
        }
      case 'add':
        return {
          ..._addConfig.value
        }
      case 'refresh':
        return {
          ..._refreshConfig.value
        }
      case 'option':
        return {
          columns: mergedTableColumns.value,
          modelValue: visibleColumns.value,
          ..._optionConfig.value
        }
      case 'toolbar':
        return {
          showAdd: _addConfig.value.show,
          showRefresh: _refreshConfig.value.show,
          showOption: _optionConfig.value.show,
          columns: mergedTableColumns.value,
          visibleColumns: visibleColumns.value
        }
      case 'search':
        if (!_searchConfig.value) return {}
        return {
          modelValue: searchParams.value,
          searchConfig: _searchConfig.value.items,
          labelWidth: _searchConfig.value.labelWidth,
          isCard: _searchConfig.value.isCard,
          showReset: _searchConfig.value.showReset !== false
        }
      case 'table':
        return {
          columns: mergedTableColumns.value,
          data: tableData.value,
          loading: loading.value,
          selection: _tableConfig.value.selection,
          index: _tableConfig.value.index,
          border: _tableConfig.value.border,
          showOperation: _tableConfig.value.showOperation,
          operationWidth: _tableConfig.value.operationWidth,
          actionConfig: tableActionConfigWithPermission.value,
          customActions: customActionsWithPermission.value,
          actionButtonSize: _tableConfig.value.actionButtonSize,
          visibleColumns: visibleColumns.value.length ? visibleColumns.value : undefined,
          showToolbar: false,
          viewText: _tableConfig.value.viewText,
          editText: _tableConfig.value.editText,
          delText: _tableConfig.value.delText,
          listPermissionCode: listPermissionCodeValue.value
          // Forward $attrs but be careful not to duplicate props
        }
      case 'page':
        return {
          total: total.value,
          page: paginationParams.page,
          limit: paginationParams.pageSize,
          ..._pageConfig.value
        }
      default:
        return {}
    }
  }

  const getComponentEvents = (name) => {
    switch (name) {
      case 'add':
        return { click: handleAdd }
      case 'refresh':
        return { click: getData }
      case 'option':
        return { 'update:modelValue': (val) => (visibleColumns.value = val) }
      case 'toolbar':
        return {
          add: handleAdd,
          refresh: getData,
          'update:visibleColumns': (val) => (visibleColumns.value = val)
        }
      case 'search':
        return {
          'update:modelValue': (val) => (searchParams.value = val),
          search: handleSearch,
          reset: handleReset
        }
      case 'table':
        return {
          'selection-change': handleSelectionChange,
          action: handleAction,
          'update:visibleColumns': (val) => (visibleColumns.value = val) // Sync back from table if it inits
        }
      case 'page':
        return {
          'update:page': (val) => (paginationParams.page = val),
          'update:limit': (val) => (paginationParams.pageSize = val),
          pagination: getData
        }
      default:
        return {}
    }
  }

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
      let params = { ...searchParams.value }
      if (_pageConfig.value.show) {
        params.page = paginationParams.page
        params.pageSize = paginationParams.pageSize
      }
      if (props.beforeRequest) {
        params = props.beforeRequest(params)
      }
      saveCache()
      const res = await props.api.list(params)
      let list = []
      let totalCount = 0
      if (props.afterRequest) {
        const result = props.afterRequest(res)
        list = result.list
        totalCount = result.total
      } else {
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
    getData()
  }

  // --- Action Handling ---
  const handleAdd = (initialData = {}) => {
    dialogMode.value = 'add'
    formData.value = { ...initialData }
    dialogVisible.value = true
  }

  const handleAction = ({ type, row }) => {
    // 如果actionBefore返回false，则不执行默认逻辑。
    const actionBefore = typeof props.actionBefore === 'function' ? props.actionBefore(type, row) : true
    // If external action listener exists, use it and skip default logic
    if (!actionBefore) return
    if (type === 'view') {
      // If external viewAction listener exists, use it and skip default view dialog
      if (props.viewAction) {
        props.viewAction(row)
        return
      }
      dialogMode.value = 'view'
      formData.value = JSON.parse(JSON.stringify(row))
      dialogVisible.value = true
    } else if (type === 'edit') {
      if (props.editAction) {
        props.editAction(row)
        return
      }
      dialogMode.value = 'edit'
      formData.value = JSON.parse(JSON.stringify(row))
      dialogVisible.value = true
    } else if (type === 'del') {
      if (props.delAction) {
        props.delAction(row)
        return
      }
      handleDelete(row)
    } else {
      emit('action', { type, row })
    }
  }

  const handleDelete = async (row) => {
    if (!props.api.del) return
    try {
      await props.api.del(row)
      ElMessage.success('删除成功')
      getData()
    } catch (error) {
      console.error(error)
    }
  }

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

  const saveFormDraft = (payload) => {
    try {
      localStorage.setItem(formDraftKey.value, JSON.stringify(payload))
    } catch (e) {
      console.error(e)
    }
  }

  const clearFormDraft = () => {
    try {
      localStorage.removeItem(formDraftKey.value)
    } catch (e) {
      console.error(e)
    }
  }

  const tryResumeFormDraft = async () => {
    if (dialogVisible.value) return
    const raw = localStorage.getItem(formDraftKey.value)
    if (!raw) return
    let draft = null
    try {
      draft = JSON.parse(raw)
    } catch (e) {
      clearFormDraft()
      return
    }
    if (!draft || typeof draft !== 'object' || !draft.formData) {
      clearFormDraft()
      return
    }

    try {
      await ElMessageBox.confirm('检测到上次未完成的表单操作，是否继续？', '继续上次操作', {
        confirmButtonText: '继续',
        cancelButtonText: '放弃',
        type: 'warning'
      })
      dialogMode.value = draft.mode || 'add'
      formData.value = draft.formData || {}
      dialogVisible.value = true
      clearFormDraft()
    } catch (e) {
      clearFormDraft()
    }
  }

  const handleFormEmptyAction = async ({ item, action, mode, formData: snapshot }) => {
    if (!action) return
    const actionMode = action.mode || 'route'
    if (actionMode === 'callback') {
      if (typeof action.callback === 'function') {
        return action.callback({
          item,
          mode,
          formData: snapshot,
          close: () => (dialogVisible.value = false)
        })
      }
      return
    }

    const to = action.to
    if (!to) return
    saveFormDraft({
      mode: dialogMode.value,
      formData: snapshot,
      from: route.fullPath,
      at: Date.now(),
      field: item?.prop
    })
    dialogVisible.value = false
    router.push(to)
  }

  const handleSelectionChange = (val) => {
    emit('selection-change', val)
  }

  // Init Columns logic moved here partially, or rely on Table to init and sync back?
  // If we pass visibleColumns=undefined initially, Table will init and use internal.
  // But we bind visibleColumns.value.
  // So we should init visibleColumns here to avoid undefined prop causing Table to go uncontrolled but then we update it?
  // Actually, if we pass [] (empty array), Table might think we want NO columns visible.
  // So we should initialize visibleColumns with all columns if cache not present.
  // But Table handles cache.
  // Let's rely on Table to emit 'update:visibleColumns' on mount if we pass undefined?
  // Table's initColumns calls emit if props.visibleColumns !== undefined.
  // If props.visibleColumns is undefined, it uses internal.
  // To make it fully controlled by CurdOption (which uses `visibleColumns` ref in this component),
  // we MUST have `visibleColumns` ref populated.
  // So we should duplicate init logic or extract it?
  // Let's extract init logic to here.
  const initColumns = () => {
    if (_tableConfig.value.cacheKey) {
      const cached = localStorage.getItem(`curd_table_cols_${_tableConfig.value.cacheKey}`)
      if (cached) {
        try {
          visibleColumns.value = JSON.parse(cached)
          return
        } catch (e) {
          console.error('Failed to parse cached visibleColumns', e)
        }
      }
    }
    // Default all
    visibleColumns.value = _tableConfig.value.columns.map((col) => col.prop).filter(Boolean)
  }

  // Initialize immediately
  initColumns()

  watch(
    visibleColumns,
    (val) => {
      if (_tableConfig.value.cacheKey && val.length) {
        localStorage.setItem(`curd_table_cols_${_tableConfig.value.cacheKey}`, JSON.stringify(val))
      }
    },
    { deep: true }
  )

  // Lifecycle
  onMounted(() => {
    initCache()
    // initColumns() // Moved to setup
    if (props.autoLoad) {
      getData()
    }
    tryResumeFormDraft()
  })

  onActivated(() => {
    tryResumeFormDraft()
  })

  defineExpose({
    getData,
    searchParams,
    handleAdd,
    handleAction
  })
</script>

<style scoped>
  .curd-container {
    padding: 10px;
    border-radius: 10px;
    background: #fff;
  }
  .curd-container--split-search {
    padding: 0;
    background: #f4f9ff;
  }
  .curd-row {
    display: flex;
    background: #fff;
    align-items: center;
    row-gap: 16px;
    flex-wrap: wrap;
    /* margin-bottom: 10px; */
    padding: 10px;
  }
  .curd-container--split-search .curd-row {
    background: transparent;
    padding: 0;
  }
  .curd-row--split {
    justify-content: space-between;
  }
  .curd-row__left,
  .curd-row__right {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 16px;
  }
  .curd-row__right {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
  }
  .curd-item {
    width: 100%; /* Default full width for items in non-split row like Search, Table */
  }
  .curd-box {
    background: #fff;
    border-radius: 10px;
    padding: 20px;
  }
  .curd-box--search {
    margin-bottom: 16px;
  }
  .curd-box--content {
    display: flex;
    flex-direction: column;
    row-gap: 20px;
  }
  .row-card-box {
    border-radius: 8px;
    margin-bottom: 10px;
    padding: 20px;
  }
  .curd-row-card-top {
    border-radius: 8px 8px 0 0;
  }
  .curd-row-card-bottom {
    border-radius: 0 0 8px 8px;
  }
</style>
