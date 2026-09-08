<template>
  <div class="product-ranking">
    <alert-box class="mb-20"></alert-box>
    <div class="product-ranking-content">
      <div class="curd-wrapper" v-empty="emptyConfig">
        <Curd
          ref="curdRef"
          :api="api"
          :table-columns="tableColumns"
          :layout="customLayout"
          :show-operation="false"
          :show-add="false"
          :border="false"
          :page-size="20"
          @load="handleLoad"
        >
          <template #left-search>
            <RoundSearch v-model="searchKeyword" placeholder="请输入商品关键词搜索" @search="handleSearch" />
            <DateQuickPicker v-model="dateRange" @change="handleDateChange" />
          </template>
          <template #table-rank="{ index }">
            <div class="rank-cell">
              <IconImage v-if="index < 3" :name="getRankIconName(index)" width="24px" height="24px" />
              <span v-else class="rank-number">{{ index + 1 }}</span>
            </div>
          </template>

          <template #table-productName="{ row }">
            <div class="product-info-cell">
              <img
                class="product-thumb"
                :src="row.imageUri"
                @error="(e) => { e.target.style.display = 'none' }"
              />
              <el-tooltip :content="row.productName" placement="top" :show-after="500">
                <span class="product-name-text">{{ row.productName || '-' }}</span>
              </el-tooltip>
            </div>
          </template>

          <template #table-sessionCount="{ row }">
            <el-link type="primary" :underline="false" :disabled="!row.sessionCount" @click="openRelatedLive(row)">
              {{ row.sessionCount || 0 }}
            </el-link>
          </template>

          <template #table-companyCount="{ row }">
            <el-link type="primary" :underline="false" :disabled="!row.companyCount" @click="openSourceOrg(row)">
              {{ row.companyCount || 0 }}
            </el-link>
          </template>
        </Curd>
      </div>
    </div>

    <RelatedLiveDialog
      v-model="relatedLiveVisible"
      :product-id="activeProduct.productId"
      :product-name="activeProduct.productName"
      :count="activeProduct.sessionCount"
      :date-range="dateRange"
    />
    <SourceOrgDialog
      v-model="sourceOrgVisible"
      :product-id="activeProduct.productId"
      :product-name="activeProduct.productName"
      :count="activeProduct.companyCount"
      :date-range="dateRange"
    />
  </div>
</template>

<script setup>
  /**
   * @file index.vue
   * @description 商品排行页面（商品业绩控制器接口对接 + 关联直播/来源组织弹窗）
   */
  import { ref, reactive } from 'vue'
  import dayjs from 'dayjs'
  import { useRouter } from 'vue-router'

  import Curd from '@/components/Curd/index.vue'
  import IconImage from '@/components/IconImage/index.vue'
  import AlertBox from '@/components/AlertBox/index.vue'
  import DateQuickPicker from '@/components/DateQuickPicker/index.vue'
  import apiModule from '@/http/api'

  import RelatedLiveDialog from './components/RelatedLiveDialog.vue'
  import SourceOrgDialog from './components/SourceOrgDialog.vue'
  import RoundSearch from '@/components/RoundSearch/index.vue'

  const curdRef = ref(null)
  const router = useRouter()
  const searchKeyword = ref('')
  const defaultDateRange = [
    dayjs().subtract(7, 'day').format('YYYY-MM-DD'),
    dayjs().subtract(1, 'day').format('YYYY-MM-DD')
  ]
  const dateRange = ref(defaultDateRange)
  const firstLoaded = ref(false)
  const emptyConfig = reactive({
    visible: false,
    title: '暂无商品业绩数据',
    description: '请先产生直播间场次并录入业绩数据后，再查看商品排行。',
    blur: 2,
    buttons: [
      {
        text: '去看直播间业绩',
        type: 'primary',
        round: true,
        click: () => router.push('/live-room-performance/index')
      },
      {
        text: '去直播间排班',
        type: 'primary',
        plain: true,
        round: true,
        click: () => router.push('/live-room-ranking/index')
      }
    ]
  })

  const isDefaultRange = () => {
    const v = dateRange.value || []
    return v?.[0] === defaultDateRange[0] && v?.[1] === defaultDateRange[1]
  }

  const handleLoad = (list) => {
    if (!firstLoaded.value) firstLoaded.value = true
    const isEmpty = Array.isArray(list) && list.length === 0
    emptyConfig.visible = firstLoaded.value && isEmpty && isDefaultRange() && !searchKeyword.value
  }

  const relatedLiveVisible = ref(false)
  const sourceOrgVisible = ref(false)
  const activeProduct = ref({
    productId: '',
    productName: '',
    sessionCount: 0,
    companyCount: 0
  })

  /**
   * @description 百分比格式化（接口返回为百分数值）
   * @param {number} val - 百分比数值
   * @returns {string}
   */
  const formatPercent = (val) => {
    const n = Number(val)
    if (!Number.isFinite(n)) return '-'
    return `${n.toFixed(2)}%`
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
   * @description 千次观看成交额格式化
   * @param {number} val - 数值
   * @returns {string}
   */
  const formatNumber = (val) => {
    const n = Number(val)
    if (!Number.isFinite(n)) return '-'
    return n.toLocaleString('zh-CN', { maximumFractionDigits: 2 })
  }

  /**
   * @description 商品排行列表 API（Curd 约定：返回 { list, total }）
   */
  const api = {
    list: async (params) => {
      const { page, pageSize, productName } = params || {}
      const [startDate, endDate] = dateRange.value || []

      const res = await apiModule.productPerformance.ranking({
        page: page || 1,
        limit: pageSize || 20,
        productName: productName ? String(productName).trim() : undefined,
        startDate,
        endDate,
        sortBy: 'salesAmount',
        sortOrder: 'desc'
      })

      const list = (res?.data?.list || []).map((row, index) => ({
        ...row,
        rank: (page ? (page - 1) * (pageSize || 20) : 0) + index + 1
      }))

      return {
        list,
        total: res?.data?.totalCount || 0
      }
    }
  }

  const customLayout = [[['left-search'], ['option']], ['table'], ['page']]

  /**
   * @description 表格列配置
   */
  const tableColumns = [
    { label: '排行', prop: 'rank', width: 80, slotName: 'rank', align: 'center' },
    { label: '商品名称', prop: 'productName', minWidth: 320, slotName: 'productName' },
    { label: '销量', prop: 'quantity', width: 100, align: 'right' },
    {
      label: '销售额',
      prop: 'salesAmount',
      width: 140,
      align: 'right',
      formatter: (row) => formatMoney(row.salesAmount)
    },
    {
      label: '曝光点击率',
      prop: 'exposureClickRate',
      width: 120,
      align: 'right',
      formatter: (row) => formatPercent(row.exposureClickRate)
    },
    {
      label: '点击付款率',
      prop: 'clickPaymentRate',
      width: 120,
      align: 'right',
      formatter: (row) => formatPercent(row.clickPaymentRate)
    },
    {
      label: '曝光成交率',
      prop: 'exposureConversionRate',
      width: 120,
      align: 'right',
      formatter: (row) => formatPercent(row.exposureConversionRate)
    },
    { label: '千次成交', prop: 'gpm', width: 120, align: 'right', formatter: (row) => formatNumber(row.gpm) },
    { label: '关联直播', prop: 'sessionCount', width: 100, align: 'center', slotName: 'sessionCount' },
    { label: '来源组织', prop: 'companyCount', width: 100, align: 'center', slotName: 'companyCount' }
  ]

  /**
   * @description 获取排行图标名称
   * @param {number} index - 排行索引（从0开始）
   * @returns {string}
   */
  const getRankIconName = (index) => `top${index + 1}`

  /**
   * @description 日期变更后刷新列表
   */
  const handleDateChange = () => {
    curdRef.value?.getData()
  }

  const handleSearch = () => {
    if (!curdRef.value) return
    const keyword = String(searchKeyword.value || '').trim()
    curdRef.value.searchParams.productName = keyword || undefined
    curdRef.value.getData()
  }

  /**
   * @description 打开关联直播弹窗
   * @param {Object} row - 行数据
   */
  const openRelatedLive = (row) => {
    if (!row?.sessionCount) return
    activeProduct.value = {
      productId: row.productId,
      productName: row.productName,
      sessionCount: row.sessionCount,
      companyCount: row.companyCount
    }
    relatedLiveVisible.value = true
  }

  /**
   * @description 打开来源组织弹窗
   * @param {Object} row - 行数据
   */
  const openSourceOrg = (row) => {
    if (!row?.companyCount) return
    activeProduct.value = {
      productId: row.productId,
      productName: row.productName,
      sessionCount: row.sessionCount,
      companyCount: row.companyCount
    }
    sourceOrgVisible.value = true
  }
</script>

<style lang="scss" scoped>
  .rank-cell {
    display: flex;
    justify-content: center;
    align-items: center;
  }

  .product-ranking-content {
    background-color: #fff;
    border-radius: var(--border-radius-base);
  }

  .rank-number {
    font-size: 16px;
    font-weight: 600;
    color: #606266;
    font-style: italic;
  }

  .product-info-cell {
    display: flex;
    align-items: center;
    gap: 8px;
    width: 100%;
  }

  .product-thumb {
    width: 40px;
    height: 40px;
    border-radius: 4px;
    object-fit: cover;
    flex-shrink: 0;
  }

  .product-name-text {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    line-height: 22px;
    flex: 1;
  }
</style>
