<template>
  <div>
    <el-table
      :data="dataList"
      border
      stripe
      size="default"
      style="width: 100%"
      :element-loading-spinner="customSvg"
      header-row-class-name="my-header-row"
    >
      <el-table-column
        prop="title"
        header-align="center"
        align="center"
        label="商品名称"
        min-width="120"
        show-overflow-tooltip
        :key="'title'"
      />
      <el-table-column
        prop="totalPrice"
        header-align="center"
        align="center"
        label="订单价格"
        min-width="90"
        show-overflow-tooltip
        :key="'totalPrice'"
      >
        <template #default="{ row }">
          <span>{{ row.totalPrice / 100 }}元</span>
        </template>
      </el-table-column>
      <el-table-column
        prop="orderType"
        header-align="center"
        align="center"
        label="订单类型"
        :formatter="(row) => getLabel(orderDict.orderType, row.orderType)"
        min-width="100"
        show-overflow-tooltip
        :key="'orderType'"
      />
      <el-table-column
        prop="source"
        header-align="center"
        align="center"
        label="订单来源"
        :formatter="(row) => getLabel(orderDict.orderSource, row.source)"
        min-width="100"
        show-overflow-tooltip
        :key="'source'"
      />
      <el-table-column
        prop="status"
        header-align="center"
        align="center"
        label="订单状态"
        :formatter="(row) => getLabel(orderDict.orderStatus, row.status)"
        min-width="100"
        show-overflow-tooltip
        :key="'status'"
      />
      <el-table-column
        prop="createName"
        header-align="center"
        align="center"
        label="创建人"
        min-width="100"
        show-overflow-tooltip
        :key="'createName'"
      />
      <el-table-column
        prop="updateName"
        header-align="center"
        align="center"
        label="修改人"
        min-width="100"
        show-overflow-tooltip
        :key="'updateName'"
      />
      <el-table-column
        prop="createDate"
        header-align="center"
        align="center"
        label="创建时间"
        min-width="160"
        show-overflow-tooltip
        :key="'createDate'"
      />
      <el-table-column
        prop="updateDate"
        header-align="center"
        align="center"
        label="修改时间"
        min-width="160"
        show-overflow-tooltip
        :key="'updateDate'"
      />
      <el-table-column
        prop="startDate"
        header-align="center"
        align="center"
        label="开始时间"
        min-width="160"
        show-overflow-tooltip
        :key="'startDate'"
      />
      <el-table-column
        prop="endDate"
        header-align="center"
        align="center"
        label="结束时间"
        min-width="160"
        show-overflow-tooltip
        :key="'endDate'"
      />
      <el-table-column
        prop="createDate"
        header-align="center"
        align="center"
        label="下单时间"
        min-width="160"
        show-overflow-tooltip
        :key="'orderDate'"
      />
      <el-table-column
        fixed="right"
        header-align="center"
        align="center"
        width="100"
        label="操作"
        :key="'actions'"
      >
        <template #default="{ row }">
          <el-button type="primary" link @click="addOrUpdateHandle(row.id)"
            >详情</el-button
          >
        </template>
      </el-table-column>
    </el-table>
    <pagination
      v-model:limit="form.limit"
      v-model:page="form.page"
      :total="totalCount"
      @change="getDataList"
      layout="total, sizes, prev, pager,next,->, jumper"
      background
    />
    <orderDetail
      v-if="orderDetailsShow"
      ref="addOrUpdate"
      @refreshDataList="getDataList"
    />
  </div>
</template>

<script setup>
import { ref, watch, onMounted, nextTick } from 'vue'
import { customSvg } from '@/utils/icon.js'
import { useDict } from '@/hooks/useDict'
import api from '@/utils/request-api'
import pagination from '@/components/commonComponent/pagination.vue'
import orderDetail from '@/views/userInfo/order/orderDetails.vue'

const props = defineProps({
  userId: {
    type: String,
    default: '',
  },
})

const { orderDict, getLabel } = useDict()

const dataList = ref([])
const totalCount = ref(0)
const orderDetailsShow = ref(false)
const addOrUpdate = ref(null)
const form = ref({
  page: 1,
  limit: 10,
  userId: '',
})

const getDataList = async () => {
  form.value.userId = props.userId
  const res = await api.order.list(form.value)
  if (res && res.code === 0) {
    dataList.value = res.data?.list ?? []
    totalCount.value = res.data?.totalCount ?? 0
  }
}

const addOrUpdateHandle = async (id) => {
  orderDetailsShow.value = true
  await nextTick()
  addOrUpdate.value?.init(id)
}

watch(
  () => props.userId,
  () => {
    getDataList()
  }
)

onMounted(() => {
  getDataList()
})
</script>

<style scoped lang="less"></style>
