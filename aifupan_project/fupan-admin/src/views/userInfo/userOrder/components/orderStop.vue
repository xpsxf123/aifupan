<template>
  <my-dialog v-model="dialogVisible" :footer="false" :title="title" :width="width" @close="close">
    <el-table
        :data="orderList"
        :element-loading-spinner="customSvg"
        background
        border
        header-row-class-name="my-header-row"
        size="default"
        stripe
        style="width: 100%"
    >
      <el-table-column align="center" header-align="center" label="商品名称" min-width="120" prop="title"
                       show-overflow-tooltip/>
      <el-table-column align="center" header-align="center" label="订单价格" min-width="90" prop="totalPrice"
                       show-overflow-tooltip>
        <template #default="scope">
          <span>{{ scope.row.totalPrice / 100 }}元</span>
        </template>
      </el-table-column>
      <el-table-column :formatter="(row)=> getLabel(orderDict.orderType, row.orderType)" align="center"
                       header-align="center" label="订单类型"
                       min-width="100" prop="orderType"
                       show-overflow-tooltip/>
      <el-table-column :formatter="(row)=> getLabel(orderDict.orderSource, row.source)" align="center"
                       header-align="center" label="订单来源"
                       min-width="100" prop="source"
                       show-overflow-tooltip/>
      <el-table-column :formatter="(row)=> getLabel(orderDict.orderStatus, row.status)" align="center"
                       header-align="center" label="订单状态"
                       min-width="100" prop="status"
                       show-overflow-tooltip/>
      <el-table-column align="center" header-align="center" label="开始时间" min-width="160" prop="startDate"
                       show-overflow-tooltip/>
      <el-table-column align="center" header-align="center" label="结束时间" min-width="160" prop="endDate"
                       show-overflow-tooltip/>
      <el-table-column align="center" header-align="center" label="下单时间" min-width="160" prop="createDate"
                       show-overflow-tooltip/>
      <el-table-column align="center" fixed="right" header-align="center" label="操作" width="100">
        <template #default="scope">
          <el-button :disabled="scope.row.commodityType == 1 && scope.row.level <= 0" size="small" type="text"
                     @click="orderStop(scope.row)">订单取消
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-model:limit="form.limit" v-model:page="form.page" :total="totalCount"
                @change="getList()"></pagination>
  </my-dialog>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessageBox } from 'element-plus'
import MyDialog from '@/components/commonComponent/myDialog.vue'
import pagination from '@/components/commonComponent/pagination.vue'
import { useDict } from '@/hooks/useDict.js'
import { customSvg } from '@/utils/icon.js'
import api from '@/utils/request-api'

const props = defineProps({
  userId: {
    type: [String, Number],
    default: null
  }
})

const emit = defineEmits(['close', 'is-ok'])

const {orderDict, getLabel} = useDict()

const title = ref('订单停用')
const width = ref('1200px')
const dialogVisible = ref(false)
const totalCount = ref(0)
const userId = ref(null)
const orderList = ref([])

const form = reactive({
  limit: 10,
  page: 1
})

const init = (userIdParam = userId.value) => {
  dialogVisible.value = true
  userId.value = userIdParam
  getList()
}

const getList = async () => {
  if (!userId.value) return
  form.userId = userId.value
  form.statusList = [1, 2]

  const res = await api.order.list(form)
  if (res?.code === 0) {
    orderList.value = res.data.list
    totalCount.value = res.data.totalCount
  }
}

const orderStop = async (row) => {
  await ElMessageBox.confirm('确定要取消订单吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })

  const res = await api.order.orderStop({orderId: row.id})
  if (res?.code === 0) {
    ElMessage.success('订单取消成功')
    getList()
    emit('is-ok')
  }
}

const close = () => {
  dialogVisible.value = false
  emit('close')
}

defineExpose({
  init
})
</script>

<style lang="scss" scoped>

</style>