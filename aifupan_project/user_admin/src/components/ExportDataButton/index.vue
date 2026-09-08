<template>
  <el-button type="primary" round :disabled="isDisabled" @click="handleExport">
    {{ buttonText }}
  </el-button>
</template>

<script setup>
  import { computed } from 'vue'
  import { ElMessage } from 'element-plus'
  import { buildExportPayload, downloadExportFile } from '@/utils/exportData'

  const emit = defineEmits(['export'])

  const props = defineProps({
    data: { type: Array, default: () => [] },
    columns: { type: Array, default: undefined },
    fileName: { type: String, default: '导出记录' },
    buttonText: { type: String, default: '导出数据' },
    disabled: { type: Boolean, default: false }
  })

  const exportData = computed(() => (Array.isArray(props.data) ? props.data : []))
  const isEmpty = computed(() => exportData.value.length === 0)
  const isDisabled = computed(() => props.disabled)

  const handleExport = () => {
    if (isEmpty.value) {
      ElMessage.warning('暂无可导出数据')
      return
    }
    const payload = buildExportPayload({
      data: exportData.value,
      columns: props.columns,
      format: 'csv',
      fileName: props.fileName
    })
    if (!payload) {
      ElMessage.error('导出失败')
      return
    }
    downloadExportFile(payload)
    emit('export', { format: 'csv', fileName: payload.fileName, count: exportData.value.length })
    ElMessage.success('开始导出')
  }
</script>

<style scoped lang="scss"></style>
