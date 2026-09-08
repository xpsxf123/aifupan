<template>
  <el-drawer
    v-model="visibleProxy"
    :size="size"
    direction="rtl"
    :show-close="false"
    :close-on-click-modal="false"
    :header-class="headerClass"
    footer-class="common-drawer-footer-style"
    class="schedule-member-drawer"
  >
    <template #header>
      <div class="schedule-member-drawer__header">
        <CloseSvg @click="visibleProxy = false" />
        <div class="schedule-member-drawer__title">{{ title }}</div>
      </div>
    </template>
    <div class="schedule-member-drawer__filters">
      <el-form inline>
        <el-form-item label="姓名" class="margin-bottom-0" style="margin-right: 0">
          <el-input v-model="keywordProxy" placeholder="请输入姓名" style="width: 220px" clearable class="rounded" />
        </el-form-item>
        <!--
        <el-form-item label="所选组织" class="margin-bottom-0" style="margin-right: 0">
          <el-select v-model="deptIdProxy" placeholder="请选择" style="width: 200px" clearable class="rounded">
            <el-option v-for="o in deptOptions" :key="o.key" :label="o.label" :value="o.key" />
          </el-select>
        </el-form-item>
        -->
      </el-form>

      <el-button type="primary" plain class="schedule-member-drawer__search" @click="$emit('search')">查找</el-button>
    </div>

    <el-table
      ref="tableRef"
      v-loading="loading"
      :data="list"
      header-cell-class-name="common-table-header-bg"
      @row-click="handleRowClick"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="56" />
      <el-table-column label="姓名" min-width="180">
        <template #default="{ row }">
          <div class="schedule-member-drawer__member-cell">
            <div class="avtar-container">
              <el-avatar :size="32" :src="row.avatar || defaultImg" />
            </div>
            <span class="schedule-member-drawer__member-name">{{ row.name }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="分公司" prop="companyName" min-width="160" />
      <el-table-column label="所属组织" prop="orgName" min-width="180" show-overflow-tooltip />
      <el-table-column label="岗位" prop="positionName" min-width="120" />
    </el-table>

    <template #footer>
      <div class="schedule-member-drawer__footer">
        <div class="schedule-member-drawer__footer-actions">
          <el-button class="status-btn" v-auth="confirmPermissionCode" type="primary" @click="handleConfirm">{{
            confirmText
          }}</el-button>
          <el-button class="status-btn" @click="handleCancel">{{ cancelText }}</el-button>
        </div>
        <el-pagination
          background
          v-model:current-page="pageProxy"
          v-model:page-size="limitProxy"
          layout="prev, pager, next"
          :total="total"
          @current-change="$emit('page-change')"
        />
      </div>
    </template>
  </el-drawer>
</template>

<script setup>
  import { computed, nextTick, ref, watch } from 'vue'
  import { ElMessage } from 'element-plus'
  import CloseSvg from '@/components/CloseSvgIcon/index.vue'
  import defaultImg from '@/assets/images/funnel/defaultAvatar.png'

  const props = defineProps({
    modelValue: { type: Boolean, default: false },
    title: { type: String, default: '选择成员' },
    size: { type: String, default: '902px' },
    headerClass: { type: String, default: 'attribute-drawer-header' },
    confirmText: { type: String, default: '确定' },
    confirmPermissionCode: { type: [String, Array], default: '' },
    cancelText: { type: String, default: '取消' },
    keyword: { type: String, default: '' },
    deptId: { type: String, default: '' },
    page: { type: Number, default: 1 },
    limit: { type: Number, default: 10 },
    deptOptions: { type: Array, default: () => [] },
    list: { type: Array, default: () => [] },
    total: { type: Number, default: 0 },
    loading: { type: Boolean, default: false },
    selectedId: { type: [String, Number], default: '' }
  })

  const emit = defineEmits([
    'update:modelValue',
    'update:keyword',
    'update:deptId',
    'update:page',
    'update:limit',
    'search',
    'page-change',
    'confirm',
    'cancel'
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
      return
    }
    selectedRow.value = selection[0] || null
  }

  const handleConfirm = () => {
    const row = selectedRow.value
    if (!row) {
      ElMessage.error('请选择成员')
      return
    }
    emit('confirm', row)
    visibleProxy.value = false
  }

  const handleCancel = () => {
    visibleProxy.value = false
    emit('cancel')
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
</script>

<style scoped lang="scss">
  .avtar-container {
    display: flex;
    align-items: center;
  }

  .schedule-member-drawer__header {
    display: flex;
    width: 100%;
    gap: 13px;
    align-items: center;
  }

  .schedule-member-drawer__filters {
    display: flex;
    flex-wrap: wrap;
    gap: 12px;
    align-items: center;
    padding-bottom: 24px;
  }

  .schedule-member-drawer__search {
    height: 34px;
    width: 84px;
    border-radius: 54px;
  }

  .schedule-member-drawer__member-cell {
    display: flex;
    align-items: center;
    gap: 10px;
  }

  .schedule-member-drawer__member-name {
    display: inline-block;
    line-height: 1;
  }

  .schedule-member-drawer__footer {
    display: flex;
    align-items: center;
    justify-content: space-between;
    width: 100%;
    gap: 12px;
  }

  .schedule-member-drawer__footer-actions {
    display: flex;
    gap: 12px;
    align-items: center;
    .el-button {
      height: 34px;
      border-radius: 56px;
    }
    .status-btn {
      width: 84px;
    }
  }
</style>

<style lang="scss">
  .attribute-drawer-header {
    padding: 11px 30px;
    margin-bottom: 0;
    border-bottom: 1px solid #dcdcdc;
    .form-section__title {
      font-size: 16px;
      background-color: transparent;
      border: none;
      padding: 0;
    }
    .header-title {
      display: flex;
      width: 100%;
      gap: 13px;
    }
  }
</style>
