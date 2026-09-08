<template>
  <el-drawer
    v-model="visibleProxy"
    header-class="schedule-copy-drawer-header"
    body-class="schedule-copy-drawer-body"
    footer-class="common-drawer-footer-style"
    direction="rtl"
    :size="size"
    :show-close="false"
    :close-on-click-modal="closeOnClickModal"
    class="schedule-copy-drawer"
  >
    <template #header>
      <div class="role-drawer__header">
        <CloseSvg @click="visibleProxy = false" />
        <div class="role-drawer__title">{{ title }}</div>
      </div>
    </template>

    <div class="copy-filters">
      <p>轮班时间</p>
      <div style="width: 200px">
        <el-time-picker
          v-model="planRangeProxy"
          is-range
          start-placeholder="轮班开始"
          end-placeholder="轮班结束"
          format="HH:mm"
          value-format="HH:mm"
          class="rounded"
          style="width: 90%"
        />
      </div>
      <el-input
        v-model="keywordProxy"
        style="width: 186px"
        class="rounded"
        placeholder="请输入直播间名称搜索"
        clearable
      />
      <el-button type="primary" plain class="search-btn" @click="emitSearch">查询</el-button>
    </div>

    <el-table
      ref="tableRef"
      :data="list"
      header-cell-class-name="common-table-header-bg"
      @row-click="handleRowClick"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="60" />
      <el-table-column label="直播间" min-width="220">
        <template #default="{ row }">
          <div class="copy-room-cell">
            <div class="avtar-container">
              <el-avatar :size="32" :src="row.image || defaultImg" />
            </div>
            <span class="name">{{ row.name }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="轮班时间" prop="planText" width="160" />
      <el-table-column label="班次时长" prop="shiftText" min-width="220" />
      <el-table-column label="中场休息时间" prop="restText" min-width="220" />
    </el-table>

    <template #footer>
      <div class="drawer-footer attribute-drawer-footer">
        <el-button v-auth="confirmPermissionCode" type="primary" class="confirm-btn" @click="handleConfirm"
          >确定</el-button
        >
        <el-pagination
          v-model:current-page="pageProxy"
          v-model:page-size="limitProxy"
          layout="prev, pager, next"
          :total="total"
          background
          @current-change="emitPageChange"
        />
      </div>
    </template>
  </el-drawer>
</template>

<script setup>
  import { computed, ref, watch, nextTick } from 'vue'
  import { ElMessage } from 'element-plus'
  import CloseSvg from '@/components/CloseSvgIcon/index.vue'
  import defaultImg from '@/assets/images/funnel/defaultAvatar.png'

  const props = defineProps({
    modelValue: {
      type: Boolean,
      default: false
    },
    title: {
      type: String,
      default: '选择直播间'
    },
    size: {
      type: String,
      default: '902px'
    },
    closeOnClickModal: {
      type: Boolean,
      default: false
    },
    planRange: {
      type: Array,
      default: () => ['', '']
    },
    keyword: {
      type: String,
      default: ''
    },
    page: {
      type: Number,
      default: 1
    },
    limit: {
      type: Number,
      default: 10
    },
    confirmPermissionCode: {
      type: [String, Array],
      default: ''
    },
    total: {
      type: Number,
      default: 0
    },
    list: {
      type: Array,
      default: () => []
    },
    selectedId: {
      type: [String, Number],
      default: ''
    }
  })

  const emit = defineEmits([
    'update:modelValue',
    'update:planRange',
    'update:keyword',
    'update:page',
    'update:limit',
    'search',
    'page-change',
    'selection-change',
    'confirm'
  ])

  const tableRef = ref(null)
  const selectedRow = ref(null)

  const visibleProxy = computed({
    get() {
      return props.modelValue
    },
    set(v) {
      emit('update:modelValue', v)
    }
  })

  const planRangeProxy = computed({
    get() {
      return props.planRange
    },
    set(v) {
      emit('update:planRange', v)
    }
  })

  const keywordProxy = computed({
    get() {
      return props.keyword
    },
    set(v) {
      emit('update:keyword', v)
    }
  })

  const pageProxy = computed({
    get() {
      return props.page
    },
    set(v) {
      emit('update:page', v)
    }
  })

  const limitProxy = computed({
    get() {
      return props.limit
    },
    set(v) {
      emit('update:limit', v)
    }
  })

  const clearSelection = () => {
    selectedRow.value = null
    tableRef.value?.clearSelection?.()
    emit('selection-change', null)
  }

  const syncSelectionFromProps = async () => {
    if (!props.modelValue) return
    const id = props.selectedId !== undefined && props.selectedId !== null ? String(props.selectedId) : ''
    if (!id) return
    const list = Array.isArray(props.list) ? props.list : []
    const hit = list.find((x) => String(x?.id || '') === id)
    if (!hit) return
    await nextTick()
    tableRef.value?.clearSelection?.()
    tableRef.value?.toggleRowSelection?.(hit, true)
  }

  watch(
    () => props.modelValue,
    (v) => {
      if (!v) return
      if (!props.selectedId) {
        clearSelection()
        return
      }
      syncSelectionFromProps()
    }
  )

  watch([() => props.list, () => props.selectedId], () => syncSelectionFromProps(), { deep: true })

  const emitSearch = () => {
    emit('search')
  }

  const emitPageChange = () => {
    emit('page-change')
  }

  const handleRowClick = (row) => {
    if (!row) return
    tableRef.value?.clearSelection?.()
    tableRef.value?.toggleRowSelection?.(row, true)
  }

  const handleSelectionChange = (rows) => {
    const selection = Array.isArray(rows) ? rows : []
    if (selection.length > 1) {
      const last = selection[selection.length - 1]
      tableRef.value?.clearSelection?.()
      tableRef.value?.toggleRowSelection?.(last, true)
      selectedRow.value = last || null
      emit('selection-change', last || null)
      return
    }
    selectedRow.value = selection[0] || null
    emit('selection-change', selection[0] || null)
  }

  const handleConfirm = () => {
    if (!selectedRow.value) {
      ElMessage.error('请选择直播间')
      return
    }
    emit('confirm', selectedRow.value)
    visibleProxy.value = false
  }

  defineExpose({ clearSelection })
</script>

<style scoped lang="scss">
  .avtar-container {
    display: flex;
    align-items: center;
  }

  .role-drawer__header {
    display: flex;
    width: 100%;
    gap: 13px;
    align-items: center;
  }

  .copy-filters {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-bottom: 24px;
  }

  .search-btn {
    width: 72px;
    height: 34px;
    border-radius: 50px;
  }

  .copy-room-cell {
    display: flex;
    align-items: center;
    gap: 10px;

    .name {
      color: #303133;
      font-weight: 500;
    }
  }

  .drawer-footer {
    display: flex;
    gap: 12px;
    align-items: center;
  }

  .attribute-drawer-footer {
    justify-content: space-between;
  }

  .confirm-btn {
    width: 84px;
    height: 34px;
    border-radius: 56px;
  }
</style>

<style>
  .schedule-copy-drawer-header {
    padding: 11px 30px;
    margin-bottom: 0;
    border-bottom: 1px solid #dcdcdc;
  }
  .schedule-copy-drawer-body {
    padding: 30px 30px 0;
  }
</style>
