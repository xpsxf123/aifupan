<script setup>
import { ref, onMounted } from 'vue'
import BaseTable from '@/components/table/index.vue'
import Big from 'big.js'
import api from '@/utils/request-api'

const props = defineProps({
  agentId: {
    type: String,
    required: true
  }
})

const tableData = ref([])
const formatterCommissionRate = (row, column, cellValue) => {
  return `${new Big(cellValue).times(100).toFixed(2)}%`
}
const formatStatus = (row, column, cellValue) => {
  return cellValue === 0 ? '使用中' : '已结束'
}
const columns = [
  {
    prop: 'startDate',
    label: '时间周期',
    slotName: 'startDate',
    minWidth: 180
  },
  {
    prop: 'commissionRate',
    label: '新签佣金比例',
    formatter: formatterCommissionRate,
    width: 120
  },
  {
    prop: 'commissionRateAmount',
    label: '新签实付总额（元）',
    width: 160
  },
  {
    prop: 'renewalCommissionRate',
    label: '续费佣金比例',
    formatter: formatterCommissionRate,
    width: 120
  },
  {
    prop: 'renewalCommissionRateAmount',
    label: '续费实付总额（元)',
    width: 150
  },
  {
    prop: 'settlementAmount',
    label: '结算佣金(元)',
    width: 110
  },
  {
    prop: 'status',
    label: '状态',
    formatter: formatStatus
  }
]

const getDataList = async () => {
  const {code, data} = await api.user.getCommission({
    agentId: props.agentId
  })
  if (code === 0 && Array.isArray(data)) {
    tableData.value = data
  }
}


onMounted(() => {
  getDataList()
})
</script>

<template>
  <div class="sale-container">
    <BaseTable
        :columns="columns"
        :showPagination="false"
        :tableData="tableData"
        height="200px"
    >
      <template #startDate="{row}">
        <span class="date">{{ `${row.startDate}-${row.endDate}` }}</span>
      </template>
    </BaseTable>
  </div>
</template>

<style scoped>
.date {
  color: #2F82E0;
}
</style>

