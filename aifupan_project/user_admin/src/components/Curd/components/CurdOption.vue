<template>
  <el-popover v-if="showComputed" placement="bottom-end" :width="200" trigger="click">
    <template #reference>
      <span style="display: inline-block">
        <el-button round class="list-setting">
          <template #icon>
            <div class="svg-icon-container">
              <SvgIcon name="listSetting" :iconStyle="iconStyle" />
            </div>
          </template>
          列表配置
        </el-button>
      </span>
    </template>
    <div class="column-setting">
      <div class="column-setting-title">列展示</div>
      <el-checkbox-group v-model="visibleCols">
        <div v-for="col in columns" :key="col.prop" class="column-setting-item">
          <el-checkbox :label="col.label" :value="col.prop" :disabled="isDisabled(col.prop)" />
        </div>
      </el-checkbox-group>
      <div class="column-setting-footer">
        <el-button size="small" link type="primary" @click="resetColumns">重置</el-button>
      </div>
    </div>
  </el-popover>
</template>

<script setup>
  import { computed } from 'vue'
  import { Setting } from '@element-plus/icons-vue'

  const props = defineProps({
    show: { type: Boolean, default: true },
    columns: { type: Array, default: () => [] },
    modelValue: { type: Array, default: () => [] },
    showThreshold: { type: Number, default: 5 }
  })

  const emit = defineEmits(['update:modelValue'])

  const showComputed = computed(() => {
    return props.show && props.columns.length > props.showThreshold
  })

  const visibleCols = computed({
    get: () => props.modelValue,
    set: (val) => emit('update:modelValue', val)
  })

  const isDisabled = (colProp) => {
    if (visibleCols.value.length <= 1 && visibleCols.value.includes(colProp)) {
      return true
    }

    return false
  }

  const iconStyle = computed(() => {
    return {
      width: '16px',
      height: '16px'
    }
  })

  const resetColumns = () => {
    const allProps = props.columns.map((col) => col.prop)
    emit('update:modelValue', allProps)
  }
</script>

<style scoped>
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
  .list-setting {
    color: #444dff;
    border: 1px solid #444dff;
  }
</style>
