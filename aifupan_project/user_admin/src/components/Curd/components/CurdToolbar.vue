<template>
  <div class="curd-toolbar">
    <div class="toolbar-left">
      <slot name="toolbar-left">
        <el-button v-if="showAdd" type="primary" round :icon="Plus" @click="$emit('add')"> 新增 </el-button>
      </slot>
    </div>
    <div class="toolbar-right">
      <slot name="toolbar-right" />
      <el-tooltip v-if="showRefresh" content="刷新" placement="top">
        <el-button circle :icon="Refresh" @click="$emit('refresh')" />
      </el-tooltip>
      <el-popover v-if="showOptionComputed" placement="bottom-end" :width="200" trigger="click">
        <template #reference>
          <span style="display: inline-block">
            <el-tooltip content="列设置" placement="top">
              <el-button circle :icon="Setting" />
            </el-tooltip>
          </span>
        </template>
        <div class="column-setting">
          <div class="column-setting-title">列展示</div>
          <el-checkbox-group v-model="internalVisibleColumns">
            <div v-for="col in columns" :key="col.prop" class="column-setting-item">
              <el-checkbox :label="col.label" :value="col.prop" />
            </div>
          </el-checkbox-group>
          <div class="column-setting-footer">
            <el-button size="small" link type="primary" @click="resetColumns">重置</el-button>
          </div>
        </div>
      </el-popover>
    </div>
  </div>
</template>

<script setup>
  import { computed } from 'vue'
  import { Plus, Refresh, Setting } from '@element-plus/icons-vue'

  const props = defineProps({
    showAdd: { type: Boolean, default: true },
    showRefresh: { type: Boolean, default: true },
    showOption: { type: Boolean, default: true },
    columns: { type: Array, default: () => [] },
    visibleColumns: { type: Array, default: () => [] }
  })

  const emit = defineEmits(['add', 'refresh', 'update:visibleColumns'])

  const showOptionComputed = computed(() => {
    return props.showOption && props.columns.length > 5
  })

  const internalVisibleColumns = computed({
    get: () => props.visibleColumns,
    set: (val) => emit('update:visibleColumns', val)
  })

  const resetColumns = () => {
    const allProps = props.columns.map((col) => col.prop)
    emit('update:visibleColumns', allProps)
  }
</script>

<style scoped>
  .curd-toolbar {
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
