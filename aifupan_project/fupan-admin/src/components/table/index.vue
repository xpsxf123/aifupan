<template>
  <div class="base-table">
    <!-- 表格 -->
    <el-table
      v-loading="loadingFlag"
      :border="border"
      :data="tableData"
      :default-expand-all="defaultExpandAll"
      :default-sort="defaultSort"
      :element-loading-spinner="elementLoadingSpinner"
      :header-row-class-name="headerRowClassName"
      :height="height"
      :max-height="maxHeight"
      :row-key="rowKey || undefined"
      :stripe="stripe"
      :tree-props="treeProps"
      style="width: 100%; margin-top: 10px"
      @sort-change="handleSortChange"
      @selection-change="handleSelectionChange"
      @row-click="handleRowClick"
    >
      <!-- 多选列 -->
      <el-table-column v-if="showSelection" type="selection" width="55" />

      <!-- 动态列 -->
      <template v-for="column in columns" :key="column.prop || column.label">
        <el-table-column
          :align="column.align || align"
          :fixed="column.fixed"
          :formatter="column.formatter"
          :label="column.label"
          :min-width="column.minWidth"
          :prop="column.prop"
          :show-overflow-tooltip="showOverflowTooltip"
          :sortable="column.sortable || false"
          :width="column.width"
        >
          <!-- 插槽支持 -->
          <template v-if="column.slotName" v-slot="{ row }">
            <slot :name="column.slotName" :row="row" />
          </template>
        </el-table-column>
      </template>
    </el-table>
    <!-- 分页 -->
    <div class="page">
      <el-pagination
        v-if="showPagination"
        :current-page="currentPage"
        :page-size="pageSize"
        :page-sizes="[10, 20, 50]"
        :total="total"
        :background="true"
        class="pagination"
        layout="total, sizes, prev, pager,next,->, jumper"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>
  </div>
</template>

<script setup>
import { customSvg } from '@/utils/icon.js'

defineOptions({
  name: 'BaseTable',
})

const props = defineProps({
  // 表格数据
  tableData: {
    type: Array,
    default: () => [],
  },
  // 列配置
  columns: {
    type: Array,
    default: () => [],
  },
  // 对齐方式
  align: {
    type: String,
    default: 'center',
  },
  // 是否默认展开所有行
  defaultExpandAll: {
    type: Boolean,
    default: false,
  },
  fixed: {
    type: String,
    default: '',
  },
  // 表格加载状态
  loadingFlag: {
    type: Boolean,
    default: false,
  },
  // 是否显示多选列
  showSelection: {
    type: Boolean,
    default: false,
  },
  // 渲染嵌套数据的配置选项
  treeProps: {
    type: Object,
    default: () => ({ children: 'children', hasChildren: 'hasChildren' }),
  },
  // 是否显示分页
  showPagination: {
    type: Boolean,
    default: true,
  },
  // 当前页码
  currentPage: {
    type: Number,
    default: 1,
  },
  // 每页条数
  pageSize: {
    type: Number,
    default: 10,
  },
  // 总条数
  total: {
    type: Number,
    default: 0,
  },
  // 是否显示边框
  border: {
    type: Boolean,
    default: true,
  },
  // 是否显示斑马纹
  stripe: {
    type: Boolean,
    default: true,
  },
  // 表格高度
  height: {
    type: [String, Number],
    default: null,
  },
  // 表格最大高度
  maxHeight: {
    type: [String, Number],
    default: null,
  },
  // 行数据的 Key
  rowKey: {
    type: String,
    default: '',
  },
  // 默认排序
  defaultSort: {
    type: Object,
    default: () => ({}),
  },
  // 当内容过长被隐藏时显示 tooltip
  showOverflowTooltip: {
    type: Boolean,
    default: true,
  },
  // 表头行类名
  headerRowClassName: {
    type: [String, Function],
    default: 'my-header-row',
  },
  elementLoadingSpinner: {
    type: String,
    default: customSvg,
  },
})

const emit = defineEmits([
  'on-pageChange',
  'sort-change',
  'selection-change',
  'row-click',
])

const handleSizeChange = (size) => {
  emit('on-pageChange', { size: size, page: 1 })
}

const handleCurrentChange = (page) => {
  emit('on-pageChange', { page, size: props.pageSize })
}

const handleSortChange = ({ column, prop, order }) => {
  emit('sort-change', { column, prop, order })
}

const handleSelectionChange = (selection) => {
  emit('selection-change', selection)
}

const handleRowClick = (row, column, event) => {
  emit('row-click', row, column, event)
}
</script>

<style scoped>
.base-table {
  width: 100%;
}

.pagination {
  margin-top: 16px;
  text-align: right;
}
</style>
