<template>
  <div class="date-quick-picker">
    <!-- 快捷选项 -->
    <el-radio-group v-model="activeQuick" @change="handleQuickChange" class="quick-group">
      <el-radio-button v-for="item in quickOptions" :key="item.value" :label="item.value">
        {{ item.label }}
      </el-radio-button>
    </el-radio-group>

    <!-- 日期选择器 -->
    <el-date-picker
      v-model="dateRange"
      :type="type"
      :range-separator="rangeSeparator"
      :start-placeholder="startPlaceholder"
      :end-placeholder="endPlaceholder"
      :clearable="false"
      :format="format"
      :value-format="valueFormat"
      class="date-picker"
      @change="handleDateChange"
    />
  </div>
</template>

<script setup>
  import { ref, watch, defineProps, defineEmits } from 'vue'
  import dayjs from 'dayjs'

  const props = defineProps({
    // v-model 绑定值 [startDate, endDate]
    modelValue: {
      type: Array,
      default: () => []
    },
    // 快捷选项配置
    quickOptions: {
      type: Array,
      default: () => [
        { label: '昨日', value: 1 },
        { label: '近7日', value: 7 },
        { label: '近15天', value: 15 },
        { label: '近30天', value: 30 }
      ]
    },
    // 日期选择器类型 'daterange' | 'datetimerange'
    type: {
      type: String,
      default: 'daterange'
    },
    format: {
      type: String,
      default: 'YYYY-MM-DD'
    },
    valueFormat: {
      type: String,
      default: 'YYYY-MM-DD'
    },
    rangeSeparator: {
      type: String,
      default: '-'
    },
    startPlaceholder: {
      type: String,
      default: '开始日期'
    },
    endPlaceholder: {
      type: String,
      default: '结束日期'
    }
  })

  const emit = defineEmits(['update:modelValue', 'change'])

  const activeQuick = ref(null)
  const dateRange = ref(props.modelValue)

  const getQuickRange = (days) => {
    const d = Number(days)
    if (!Number.isFinite(d) || d <= 0) return []

    const formatStr = props.valueFormat || 'YYYY-MM-DD'
    const end = dayjs().subtract(1, 'day')
    const start = d === 1 ? end : end.subtract(d - 1, 'day')
    return [start.format(formatStr), end.format(formatStr)]
  }

  const syncActiveQuick = (range) => {
    if (!Array.isArray(range) || range.length !== 2) {
      activeQuick.value = null
      return
    }

    const [start, end] = range
    const hasMatch = (props.quickOptions || []).some((item) => {
      const expected = getQuickRange(item?.value)
      if (!Array.isArray(expected) || expected.length !== 2) return false
      const [es, ee] = expected
      if (String(start) !== String(es)) return false
      if (String(end) !== String(ee)) return false
      activeQuick.value = item.value
      return true
    })

    if (!hasMatch) activeQuick.value = null
  }

  // 监听外部 modelValue 变化
  watch(
    () => props.modelValue,
    (val) => {
      dateRange.value = val
      syncActiveQuick(val)
    },
    { immediate: true }
  )

  const handleQuickChange = (val) => {
    const nextRange = getQuickRange(val)
    if (!Array.isArray(nextRange) || nextRange.length !== 2) return
    dateRange.value = nextRange
    emit('update:modelValue', dateRange.value)
    emit('change', dateRange.value)
  }

  const handleDateChange = (val) => {
    activeQuick.value = null
    emit('update:modelValue', val)
    emit('change', val)
  }
</script>

<style scoped lang="scss">
  .date-quick-picker {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 16px;
    .quick-group {
      flex-shrink: 0;
    }
  }

  :deep(.date-picker) {
    width: 230px;
  }
</style>
