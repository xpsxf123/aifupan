<template>
  <el-dialog
    :model-value="modelValue"
    :title="dialogTitle"
    class="common-dialog"
    :width="996"
    destroy-on-close
    @close="handleClose"
  >
    <div class="dialog-body">
      <!--      <div class="dialog-subtitle">
        <span class="subtitle-label">商品：</span>
        <span class="subtitle-value" :title="productName">{{ productName || '-' }}</span>
      </div>-->

      <el-table v-loading="loading" :data="tableData" :border="false" class="dialog-table">
        <el-table-column prop="companyName" label="来源组织" min-width="220" show-overflow-tooltip />
        <el-table-column prop="quantity" label="销量" width="100" />
        <el-table-column label="销售额" width="140">
          <template #default="{ row }">
            <span>{{ formatMoney(row.salesAmount) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="viewCount" label="场观" width="120" />
      </el-table>
    </div>
  </el-dialog>
</template>

<script setup>
  /**
   * @file SourceOrgDialog.vue
   * @description 商品排行-来源组织弹窗（按商品查询关联分公司列表并展示）
   */
  import { computed, ref, watch } from 'vue'
  import apiModule from '@/http/api'

  const props = defineProps({
    modelValue: { type: Boolean, default: false },
    productId: { type: String, default: '' },
    productName: { type: String, default: '' },
    count: { type: Number, default: 0 },
    dateRange: { type: Array, default: () => [] }
  })

  const emit = defineEmits(['update:modelValue'])

  const loading = ref(false)
  const tableData = ref([])

  const dialogTitle = computed(() => `来源组织（${props.count || 0}）`)

  /**
   * @description 金额格式化（单位：元）
   * @param {number} amount - 金额
   * @returns {string}
   */
  const formatMoney = (amount) => {
    const n = Number(amount)
    if (!Number.isFinite(n)) return '-'
    return n.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
  }

  /**
   * @description 拉取来源组织列表数据
   * @returns {Promise<void>}
   */
  const fetchList = async () => {
    if (!props.productId) return
    const [startDate, endDate] = props.dateRange || []
    if (!startDate || !endDate) return

    loading.value = true
    try {
      const res = await apiModule.productPerformance.companies({
        productId: props.productId,
        startDate,
        endDate,
        sortBy: 'salesAmount',
        sortOrder: 'desc'
      })
      tableData.value = res?.data || []
    } finally {
      loading.value = false
    }
  }

  /**
   * @description 关闭弹窗
   */
  const handleClose = () => {
    emit('update:modelValue', false)
  }

  watch(
    () => [props.modelValue, props.productId, props.dateRange?.[0], props.dateRange?.[1]],
    ([visible]) => {
      if (!visible) return
      fetchList()
    }
  )
</script>

<style scoped lang="scss">
  /* 设置表头背景色为 #fbfbfb */
  :deep(.el-table th.el-table__cell) {
    background-color: #fbfbfb;
  }
  .dialog-body {
    display: flex;
    flex-direction: column;
    gap: 12px;
  }

  .dialog-subtitle {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 13px;
    color: #606266;
  }

  .subtitle-label {
    flex-shrink: 0;
  }

  .subtitle-value {
    flex: 1;
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    color: #303133;
    font-weight: 500;
  }
</style>
