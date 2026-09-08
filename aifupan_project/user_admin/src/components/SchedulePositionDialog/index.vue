<template>
  <el-dialog
    v-model="visibleProxy"
    :width="width"
    :close-on-click-modal="closeOnClickModal"
    class="common-dialog"
    :class="dialogClass"
  >
    <template #header>
      <div class="dialog-title">{{ title }}</div>
      <div v-if="tip" class="position-tip">{{ tip }}</div>
    </template>

    <el-checkbox-group v-model="selectedProxy" class="position-grid">
      <el-checkbox v-for="p in options" :key="String(p.id)" :label="String(p.id)" :disabled="isOptionDisabled(p.id)">
        {{ p.name }}
      </el-checkbox>
    </el-checkbox-group>

    <template #footer>
      <el-button class="custom-btn" @click="handleCancel">{{ cancelText }}</el-button>
      <el-button v-auth="confirmPermissionCode" class="custom-btn" type="primary" @click="$emit('confirm')">
        {{ confirmText }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
  import { computed } from 'vue'

  const props = defineProps({
    modelValue: {
      type: Boolean,
      default: false
    },
    selectedIds: {
      type: Array,
      default: () => []
    },
    options: {
      type: Array,
      default: () => []
    },
    max: {
      type: Number,
      default: 3
    },
    title: {
      type: String,
      default: '设置直播间岗位指标'
    },
    tip: {
      type: String,
      default: '直播间岗位最多选择3个'
    },
    confirmText: {
      type: String,
      default: '确定'
    },
    cancelText: {
      type: String,
      default: '取消'
    },
    confirmPermissionCode: {
      type: [String, Array],
      default: ''
    },
    width: {
      type: String,
      default: '520px'
    },
    closeOnClickModal: {
      type: Boolean,
      default: false
    },
    dialogClass: {
      type: String,
      default: 'schedule-position-dialog'
    }
  })

  const emit = defineEmits(['update:modelValue', 'update:selectedIds', 'confirm', 'cancel'])

  const visibleProxy = computed({
    get() {
      return props.modelValue
    },
    set(v) {
      emit('update:modelValue', v)
    }
  })

  const selectedProxy = computed({
    get() {
      return Array.isArray(props.selectedIds) ? props.selectedIds : []
    },
    set(v) {
      emit('update:selectedIds', Array.isArray(v) ? v : [])
    }
  })

  const isOptionDisabled = (id) => {
    const selected = Array.isArray(props.selectedIds) ? props.selectedIds.map((x) => String(x)) : []
    const key = String(id)
    if (selected.includes(key)) return false
    const max = Number(props.max) || 0
    if (!max) return false
    return selected.length >= max
  }

  const handleCancel = () => {
    visibleProxy.value = false
    emit('cancel')
  }
</script>

<style scoped lang="scss">
  .position-tip {
    color: #909399;
    font-size: 12px;
  }

  .position-grid {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 10px 16px;
  }

  .custom-btn {
    width: 84px;
    height: 34px;
    border-radius: 50px;
  }

  :deep(.el-dialog__footer) {
    border-top: none;
    padding: 0 40px 28px 0;
  }
</style>
