<template>
  <el-dialog
    :model-value="modelValue"
    :title="dialogTitle"
    :width="996"
    class="common-dialog"
    destroy-on-close
    @close="handleClose"
  >
    <div class="dialog-body">
      <!--      <div class="dialog-subtitle">
        <span class="subtitle-label">商品：</span>
        <span class="subtitle-value" :title="productName">{{ productName || '-' }}</span>
      </div>-->

      <el-table v-loading="loading" :data="tableData" :border="false" class="dialog-table">
        <el-table-column label="主播" min-width="180">
          <template #default="{ row }">
            <div class="anchor-cell">
              <div class="avtar-container">
                <el-avatar :size="28" :src="row.anchorAvatar || defaultImg" />
              </div>
              <span class="anchor-name">{{ row.anchorName || '-' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="startTime" label="开播时间" width="180" />
        <el-table-column label="场次时长" width="120">
          <template #default="{ row }">
            <span>{{ formatDuration(row.duration) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="quantity" label="销量" width="90" />
        <el-table-column label="销售额" width="120">
          <template #default="{ row }">
            <span>{{ formatMoney(row.salesAmount) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="viewCount" label="场观" width="110" />
      </el-table>
      <div class="dialog-page" v-if="total > 0">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :total="total"
          background
          layout="prev, pager, next"
          @current-change="fetchList"
          @size-change="handleSizeChange"
        />
      </div>
    </div>
  </el-dialog>
</template>

<script setup>
  /**
   * @file RelatedLiveDialog.vue
   * @description 商品排行-关联直播弹窗（按商品查询关联直播场次并分页展示）
   */
  import { computed, ref, watch } from 'vue'
  import apiModule from '@/http/api'
  import defaultImg from '@/assets/images/funnel/defaultAvatar.png'

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
  const page = ref(1)
  const pageSize = ref(10)
  const total = ref(0)

  const dialogTitle = computed(() => `关联直播（${props.count || 0}）`)

  /**
   * @description 将秒数格式化为 mm:ss 或 HH:mm:ss
   * @param {number} seconds - 秒
   * @returns {string}
   */
  const formatDuration = (seconds) => {
    const s = Number(seconds) || 0
    const h = Math.floor(s / 3600)
    const m = Math.floor((s % 3600) / 60)
    const ss = s % 60
    const pad = (n) => String(n).padStart(2, '0')
    if (h > 0) return `${pad(h)}:${pad(m)}:${pad(ss)}`
    return `${pad(m)}:${pad(ss)}`
  }

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
   * @description 拉取关联直播场次分页数据
   * @returns {Promise<void>}
   */
  const fetchList = async () => {
    if (!props.productId) return
    const [startDate, endDate] = props.dateRange || []
    if (!startDate || !endDate) return

    loading.value = true
    try {
      const res = await apiModule.productPerformance.sessions({
        productId: props.productId,
        startDate,
        endDate,
        page: page.value,
        limit: pageSize.value,
        sortBy: 'startTime',
        sortOrder: 'desc'
      })
      tableData.value = res?.data?.list || []
      total.value = res?.data?.totalCount || 0
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

  /**
   * @description 切换分页大小后重置到第一页
   */
  const handleSizeChange = () => {
    page.value = 1
    fetchList()
  }

  watch(
    () => [props.modelValue, props.productId, props.dateRange?.[0], props.dateRange?.[1]],
    ([visible]) => {
      if (!visible) return
      page.value = 1
      fetchList()
    }
  )
</script>

<style scoped lang="scss">
  .avtar-container {
    display: flex;
    align-items: center;
  }

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

  .anchor-cell {
    display: flex;
    align-items: center;
    gap: 8px;
    min-width: 0;
  }

  .anchor-name {
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .dialog-page {
    display: flex;
    justify-content: center;
    padding-top: 8px;
  }
</style>
