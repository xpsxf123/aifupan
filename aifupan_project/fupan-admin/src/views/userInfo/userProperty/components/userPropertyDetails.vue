<template>
  <myDialog
    v-model="visible"
    :title="title"
    :width="width"
    :footer="false"
    max-height
    @submit="submit"
    @close="close"
  >
    <div style="padding: 0 15px">
      <el-descriptions title="用户资产">
        <el-descriptions-item label="用户名">{{
          userData.username
        }}</el-descriptions-item>
        <el-descriptions-item label="昵称">{{
          userData.nickName
        }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{
          userData.phone
        }}</el-descriptions-item>
        <el-descriptions-item label="注册时间">{{
          userData.registerDate
        }}</el-descriptions-item>
        <el-descriptions-item label="当前版本">{{
          userData.packageName
        }}</el-descriptions-item>
        <el-descriptions-item label="版本到期时间">{{
          userData.expirationDate
        }}</el-descriptions-item>
      </el-descriptions>
      <el-descriptions title="用户资产">
        <el-descriptions-item
          v-for="item in propertyList"
          :key="`property-${item.commodityTypeCode}`"
          :label="item.commodityTypeName"
        >
          <span
            v-if="
              !item.commodityTypeCode.startsWith('child_') &&
              item.commodityTypeCode !== 'monitorNum'
            "
          >
            {{
              convertUnit(
                (item.totalQuantity ?? 0) - (item.useQuantity ?? 0),
                item.commodityTypeCode,
                item.commodityTypeUnit
              )
            }}
            /
            {{
              convertUnit(
                item.totalQuantity,
                item.commodityTypeCode,
                item.commodityTypeUnit
              )
            }}
          </span>
          <span v-else>
            {{
              convertUnit(
                item.totalQuantity,
                item.commodityTypeCode,
                item.commodityTypeUnit
              )
            }}
          </span>
        </el-descriptions-item>
      </el-descriptions>
      <div style="margin-top: 5px">
        <span class="my-title">有效订单</span>
        <el-table
          :data="orderList"
          :element-loading-spinner="customSvg"
          border
          stripe
          size="default"
          style="width: 100%; margin-top: 10px"
          header-row-class-name="my-header-row"
          background
        >
          <el-table-column
            prop="title"
            header-align="center"
            align="center"
            label="商品名称"
            min-width="120"
            show-overflow-tooltip
          />
          <el-table-column
            prop="totalPrice"
            header-align="center"
            align="center"
            label="订单价格"
            min-width="90"
            show-overflow-tooltip
          >
            <template #default="scope">
              <span>{{ scope.row.totalPrice / 100 }}元</span>
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
          />
          <el-table-column
            prop="source"
            header-align="center"
            align="center"
            label="订单来源"
            :formatter="(row) => getLabel(orderDict.orderSource, row.source)"
            min-width="100"
            show-overflow-tooltip
          />
          <el-table-column
            prop="status"
            header-align="center"
            align="center"
            label="订单状态"
            :formatter="(row) => getLabel(orderDict.orderStatus, row.status)"
            min-width="100"
            show-overflow-tooltip
          />
          <el-table-column
            prop="startDate"
            header-align="center"
            align="center"
            label="开始时间"
            min-width="160"
            show-overflow-tooltip
          />
          <el-table-column
            prop="endDate"
            header-align="center"
            align="center"
            label="结束时间"
            min-width="160"
            show-overflow-tooltip
          />
          <el-table-column
            prop="createDate"
            header-align="center"
            align="center"
            label="下单时间"
            min-width="160"
            show-overflow-tooltip
          />
          <el-table-column
            fixed="right"
            header-align="center"
            align="center"
            width="60"
            label="操作"
          >
            <template #default="scope">
              <el-button
                type="text"
                style="color: #007aff"
                @click="addOrUpdateHandle(scope.row.id)"
                >详情</el-button
              >
            </template>
          </el-table-column>
        </el-table>
      </div>
      <div style="margin-top: 20px">
        <span class="my-title">资产进出记录</span>
        <el-table
          :data="propertyDetailsList"
          :element-loading-spinner="customSvg"
          border
          stripe
          size="default"
          style="width: 100%; margin-top: 10px"
          header-row-class-name="my-header-row"
          background
        >
          <el-table-column
            label="资产名称"
            prop="commodityTypeName"
            min-width="150"
            show-overflow-tooltip
          >
            <template #default="scope">
              <span
                v-if="butOpenCodes.includes(scope.row.commodityTypeCode)"
                @click="handleOpenAiToken(scope.row)"
                style="color: #3f3ff2; cursor: pointer"
              >
                {{ scope.row.commodityTypeName }}
              </span>
              <span v-else>{{ scope.row.commodityTypeName }}</span>
            </template>
          </el-table-column>
          <el-table-column label="使用数量" prop="commodityTypeName">
            <template #default="scope">
              <div :class="scope.row.signs ? 'green' : 'red'">
                <span>{{ scope.row.signs ? '+' : '-' }}</span>
                <span>{{
                  convertUnit(
                    scope.row.quantity,
                    scope.row.commodityTypeCode,
                    scope.row.commodityTypeUnit
                  )
                }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column
            label="使用人"
            prop="userName"
            min-width="80"
            show-overflow-tooltip
          />
          <el-table-column label="记录时间" prop="createDate" min-width="160" />
          <el-table-column label="备注" prop="remarks" min-width="200" />
        </el-table>
        <pagination
          v-model:limit="propertyDetailsForm.limit"
          v-model:page="propertyDetailsForm.page"
          :total="propertyDetailsCount"
          @change="propertyDetails"
        ></pagination>
      </div>
    </div>

    <orderDetail v-if="orderDetailsShow" ref="orderDetailRef"></orderDetail>

    <aiTokenDetails v-if="aiTokenShow" ref="aiTokenDetailsRef"></aiTokenDetails>
  </myDialog>
</template>

<script setup>
import { ref, reactive, nextTick } from 'vue'
import myDialog from '@/components/commonComponent/myDialog.vue'
import orderDetail from '@/views/userInfo/order/orderDetails.vue'
import pagination from '@/components/commonComponent/pagination.vue'
import aiTokenDetails from '@/views/userInfo/userProperty/components/aiTokenDetails.vue'
import { useDict } from '@/hooks/useDict.js'
import { useCommonHooks } from '@/hooks/useCommonHooks.js'
import { customSvg } from '@/utils/icon.js'
import api from '@/utils/request-api'

const props = defineProps({
  userId: {
    type: String,
    default: '',
  },
})

const emit = defineEmits(['submit', 'close'])

const { orderDict, getLabel } = useDict()
const { convertUnit } = useCommonHooks()

const orderDetailsShow = ref(false)
const aiTokenShow = ref(false)
const visible = ref(false)
const title = ref('资产详情')
const width = ref('800px')
const selectData = ref({})
const userData = ref({})
const propertyList = ref([])
const orderList = ref([])
const propertyDetailsList = ref([])
const propertyDetailsCount = ref(0)
const butOpenCodes = ref(['imgIdentifyNum', 'aiTokenNum'])

const orderDetailRef = ref(null)
const aiTokenDetailsRef = ref(null)

const propertyDetailsForm = reactive({
  limit: 10,
  page: 1,
})

const init = (data) => {
  propertyList.value = []
  orderList.value = []
  propertyDetailsList.value = []
  selectData.value = data
  visible.value = true
  getUser()
  getProperty()
  getOrder()
}

const getUser = async () => {
  const res = await api.user.userDetailByUserId({
    userId: selectData.value.userId,
  })
  if (res && res.code === 0) {
    userData.value = res.data
  }
  if (userData.value) {
    if (userData.value.userType === 2) {
      Object.assign(propertyDetailsForm, {
        page: 1,
        limit: 10,
        userId: userData.value.userId,
        parentUserId: userData.value.parentId,
      })
      propertyDetails()
    } else {
      Object.assign(propertyDetailsForm, {
        page: 1,
        limit: 10,
        propertyId: selectData.value.id,
      })
      propertyDetails()
    }
  }
}

const addOrUpdateHandle = async (id) => {
  orderDetailsShow.value = true
  await nextTick()
  orderDetailRef.value.init(id)
}

const getProperty = async () => {
  const res = await api.userproperty.getPropertyByPropertyId({
    propertyId: selectData.value.id,
  })
  if (res?.code == 0) {
    propertyList.value = res.data
  }
}

const getOrder = async () => {
  const data = {
    limit: -1,
    userId: selectData.value.userId,
    statusList: [1, 2, 7],
  }
  const res = await api.order.list(data)
  if (res?.code == 0) {
    orderList.value = res.data?.list ?? []
  }
}

const propertyDetails = async () => {
  const res = await api.userproperty.pagePropertyDetails(propertyDetailsForm)
  if (res?.code == 0) {
    propertyDetailsList.value = res.data.list
    propertyDetailsCount.value = res.data.totalCount
  }
}

const handleOpenAiToken = async (row) => {
  aiTokenShow.value = true
  await nextTick()
  aiTokenDetailsRef.value.init(row.id)
}

const submit = () => {
  visible.value = false
  emit('submit')
}

const close = () => {
  visible.value = false
  emit('close')
}

defineExpose({
  init,
})
</script>

<style scoped lang="scss">
.my-title {
  font-size: 16px;
  font-weight: bold;
}

.green {
  color: green;
}

.red {
  color: red;
}
</style>
