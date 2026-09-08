<template>
  <div class="mod-config">
    <el-form :inline="true" :model="form">
      <el-form-item>
        <el-input
            v-model="form.id"
            clearable
            placeholder="输入订单编号"
            style="width: 170px"
        ></el-input>
      </el-form-item>
      <el-form-item>
        <el-input
            v-model="form.title"
            clearable
            placeholder="商品名称"
            style="width: 170px"
        ></el-input>
      </el-form-item>
      <el-form-item>
        <el-input
            v-model="form.userName"
            clearable
            placeholder="下单人"
            style="width: 170px"
        ></el-input>
      </el-form-item>
      <el-form-item>
        <el-select
            v-model="form.status"
            clearable
            placeholder="订单状态"
            style="width: 170px"
        >
          <el-option
              v-for="option in orderDict.orderStatus"
              :key="`status-${option.value}`"
              :label="option.label"
              :value="option.value + ''"
          >
          </el-option>
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-date-picker
            v-model="form.startEndCreateDate"
            :default-time="[
            new Date(0, 0, 0, 0, 0, 0),
            new Date(0, 0, 0, 23, 59, 59),
          ]"
            end-placeholder="结束日期"
            format="YYYY-MM-DD"
            range-separator="至"
            start-placeholder="开始日期"
            style="width: 300px"
            type="daterange"
            value-format="YYYY-MM-DD HH:mm:ss"
        >
        </el-date-picker>
      </el-form-item>
      <el-form-item>
        <el-select
            v-model="form.createId"
            clearable
            filterable
            placeholder="请选择订单创建人"
            style="width: 170px"
        >
          <el-option
              v-for="item in createUserList"
              :key="`create-user-${item.id}`"
              :label="item.nickName"
              :value="item.id"
          >
          </el-option>
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-select
            v-model="form.orderType"
            clearable
            placeholder="订单类型"
            style="width: 170px"
        >
          <el-option
              v-for="option in orderDict.orderType"
              :key="`order-type-${option.value}`"
              :label="option.label"
              :value="option.value + ''"
          >
          </el-option>
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button icon="Search" plain type="primary" @click="search"
        >查询
        </el-button
        >
      </el-form-item>
    </el-form>
    <el-table
        v-loading="dataListLoading"
        :data="dataList"
        :element-loading-spinner="customSvg"
        background
        border
        header-row-class-name="my-header-row"
        size="default"
        stripe
        style="width: 100%"
    >
      <el-table-column
          align="center"
          header-align="center"
          label="商品名称"
          min-width="150"
          prop="title"
          show-overflow-tooltip
      />
      <el-table-column
          align="center"
          header-align="center"
          label="下单人"
          min-width="90"
          prop="userName"
          show-overflow-tooltip
      />
      <el-table-column
          align="center"
          header-align="center"
          label="实付金额"
          min-width="90"
          prop="realPrice"
          show-overflow-tooltip
      >
        <template #default="scope">
          <span>{{ scope.row.totalPrice / 100 }}元</span>
        </template>
      </el-table-column>
      <el-table-column
          :formatter="(row) => getLabel(orderDict.orderType, row.orderType)"
          align="center"
          header-align="center"
          label="订单类型"
          min-width="160"
          prop="orderType"
          show-overflow-tooltip
      />
      <el-table-column
          :formatter="(row) => getLabel(orderDict.orderSource, row.source)"
          align="center"
          header-align="center"
          label="订单来源"
          min-width="100"
          prop="source"
          show-overflow-tooltip
      />
      <el-table-column
          :formatter="(row) => getLabel(orderDict.orderStatus, row.status)"
          :min-width="130"
          align="center"
          header-align="center"
          label="订单状态"
          prop="status"
          show-overflow-tooltip
      />
      <el-table-column
          align="center"
          header-align="center"
          label="新签/续费类别"
          min-width="120"
          prop="commissionType"
          show-overflow-tooltip
      />
      <el-table-column
          align="center"
          header-align="center"
          label="订单佣金比例%"
          min-width="130"
          prop="commission"
          show-overflow-tooltip
      />
      <el-table-column
          :formatter="formatterCommissionMoney"
          align="center"
          header-align="center"
          label="订单佣金"
          min-width="90"
          prop="commissionMoney"
          show-overflow-tooltip
      />
      <el-table-column
          align="center"
          header-align="center"
          label="订单创建人"
          min-width="100"
          prop="createName"
          show-overflow-tooltip
      />
      <el-table-column
          :width="180"
          align="center"
          header-align="center"
          label="开始时间"
          prop="startDate"
          show-overflow-tooltip
      />
      <el-table-column
          :width="180"
          align="center"
          header-align="center"
          label="结束时间"
          prop="endDate"
          show-overflow-tooltip
      />
      <el-table-column
          :width="180"
          align="center"
          header-align="center"
          label="下单时间"
          prop="createDate"
          show-overflow-tooltip
      />
      <el-table-column
          align="center"
          fixed="right"
          header-align="center"
          label="操作"
          width="100"
      >
        <template #default="scope">
          <el-button
              style="color: #007aff"
              type="text"
              @click="addOrUpdateHandle(scope.row.id)"
          >
            详情
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
        v-model:limit="form.limit"
        v-model:page="form.page"
        :small="isMobile"
        :total="totalCount"
        @change="getDataList"
    ></pagination>

    <orderDetail
        v-if="orderDetailsShow"
        ref="addOrUpdate"
        :isMobile="isMobile"
        @refreshDataList="getDataList"
    ></orderDetail>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import orderDetail from './orderDetails.vue'
import pagination from '@/components/commonComponent/pagination.vue'
import { useDict } from '@/hooks/useDict.js'
import { useMixinTable } from '@/hooks/useMixinTable.js'
import { customSvg } from '@/utils/icon.js'
import api from '@/utils/request-api'
import { useSystemInfoStore } from '@/store'
import { storeToRefs } from 'pinia'

const {orderDict, getLabel} = useDict()
const {isMobile} = storeToRefs(useSystemInfoStore())
const orderDetailsShow = ref(false)
const dataList = ref([])
const totalCount = ref(0)
const createUserList = ref([])
const addOrUpdate = ref(null)
const dataListLoading = ref(false)
const form = reactive({
  createId: '',
  id: '',
  title: '',
  userName: '',
  status: '',
  startEndCreateDate: null,
  orderType: '',
  page: 1,
  limit: 10
})

const getDataList = async () => {
  dataListLoading.value = true
  setUrl()
  if (form.startEndCreateDate?.length > 1) {
    form.startTime = form.startEndCreateDate[0]
    form.endTime = form.startEndCreateDate[1]
  } else {
    form.endTime = null
    form.startTime = null
  }

  const res = await api.order.list(form)
  if (res && res.code === 0) {
    dataListLoading.value = false
    dataList.value = res.data?.list ?? []
    totalCount.value = res.data?.totalCount ?? 0
  }
}

const {setUrlParams, setUrl, form: mixinForm} = useMixinTable(getDataList)

// 合并form数据
Object.assign(form, mixinForm.value)

const search = () => {
  form.page = 1
  getDataList()
}

const addOrUpdateHandle = async (id) => {
  orderDetailsShow.value = true
  await nextTick()
  addOrUpdate.value.init(id)
}

const getCreateUserList = async () => {
  const {code, data} = await api.order.getCreateUserList({
    page: 1,
    limit: -1
  })
  if (code === 0) {
    if (data.list) {
      createUserList.value = data.list
    }
  }
}

const formatterCommissionMoney = (row) => {
  if (row.commissionMoney) {
    return `${row.commissionMoney}元`
  }
}
onMounted(() => {
  setUrlParams()
  getDataList()
  getCreateUserList()
})
</script>
<style lang="scss" scoped>
.mod-config {
  padding: 15px;
}
</style>
