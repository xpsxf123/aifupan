<template>
  <el-input
    v-model="model"
    :placeholder="placeholder"
    class="round-search-input"
    :style="inputStyle"
    clearable
    @keyup.enter="handleSearch"
    @change="handleSearch"
    @clear="handleSearch"
  />
</template>

<script setup>
  /**
   * @file RoundSearch 组件
   * @description 一个圆角搜索框组件，用于替代默认的方形搜索框
   */
  const emit = defineEmits(['search'])
  const model = defineModel({ type: String, default: '' })
  defineProps({
    placeholder: {
      type: String,
      default: '请输入名称'
    },
    inputStyle: {
      type: Object,
      default: () => ({
        width: '240px'
      })
    }
  })

  const handleSearch = () => {
    emit('search', model.value)
  }
</script>

<style scoped lang="scss">
  .round-search-input {
    :deep(.el-input__wrapper) {
      padding-left: 15px;
      background-color: #f5f7fa;
      border-radius: 20px;
      box-shadow: none !important;
    }

    :deep(.el-input__inner) {
      color: #606266;
    }
  }
</style>
