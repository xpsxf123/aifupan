<template>
  <el-pagination
    v-if="total"
    style="text-align: center; margin-top: 10px"
    @size-change="toSize"
    @current-change="toPage"
    :current-page="page"
    :page-sizes="pageSizes"
    :page-size="limit"
    :total="total"
    :background="true"
    layout="total, sizes, prev, pager,next,->, jumper"
  >
  </el-pagination>
</template>
<script setup>
import { reactive, watch } from 'vue'

const props = defineProps({
  limit: {
    type: Number,
    default: 10,
  },
  pageSizes: {
    type: Array,
    default: () => [10, 20, 50],
  },
  total: {
    type: Number,
    default: 0,
  },
  page: {
    type: Number,
    default: 1,
  },
})

const emit = defineEmits([
  'toSize',
  'update:limit',
  'change',
  'toPage',
  'update:page',
])

const form = reactive({
  limit: props.limit,
  page: props.page,
})

watch(
  () => props.limit,
  (val) => {
    form.limit = val
  }
)

watch(
  () => props.page,
  (val) => {
    form.page = val
  }
)

const toSize = (val) => {
  form.limit = val
  form.page = 1
  emit('update:limit', val)
  emit('update:page', 1)
  emit('toSize', val)
  emit('change', val)
}

const toPage = (val) => {
  form.page = val
  emit('toPage', val)
  emit('update:page', val)
  emit('change', val)
}
</script>

<style scoped lang="less"></style>
