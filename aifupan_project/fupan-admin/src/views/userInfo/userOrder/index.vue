<template>
  <div class="mod-config">
    <el-form
        ref="myForm"
        :inline="true"
        :model="form.commodityTypeCode"
        label-suffix=":"
    >
      <el-form-item label="用户搜索">
        <el-input
            v-model="form.keywords"
            clearable
            placeholder="输入账号、昵称、手机号"
        ></el-input>
      </el-form-item>
      <span v-if="commodityTypeList?.length > 0">
        <template
            v-for="item in commodityTypeList"
            :key="`commodity-${item.code}`"
        >
          <el-form-item
              v-if="item.showSearch"
              :label="`${item.name}范围`"
              :prop="`${item.code}`"
              :rules="{
              validator: checkCommodityCode,
              trigger: ['blur', 'change'],
            }"
          >
            <div style="width: 230px; display: flex; align-items: center">
              <div
                  v-if="form.commodityTypeCode[item.code]?.length > 0"
                  style="flex: 1"
              >
                <el-input
                    v-model="form.commodityTypeCode[item.code][0]"
                    :min="1"
                    clearable
                    placeholder="开始值"
                    type="number"
                />
              </div>
              <div style="text-align: center; width: 25px">至</div>
              <div
                  v-if="form.commodityTypeCode[item.code]?.length > 0"
                  style="flex: 1"
              >
                <el-input
                    v-model="form.commodityTypeCode[item.code][1]"
                    :min="form.commodityTypeCode[item.code][0] ?? 1"
                    clearable
                    placeholder="结束值"
                    type="number"
                />
              </div>
            </div>
          </el-form-item>
        </template>
      </span>
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
          fixed
          header-align="center"
          label="昵称"
          min-width="100"
          prop="nickName"
          show-overflow-tooltip
      >
      </el-table-column>
      <el-table-column
          :width="120"
          align="center"
          fixed
          header-align="center"
          label="手机号"
          prop="phone"
          show-overflow-tooltip
      >
      </el-table-column>
      <el-table-column
          :width="100"
          align="center"
          header-align="center"
          label="账号类型"
          prop="userType"
          show-overflow-tooltip
      >
        <template #default="scope">
          {{ getLabel(userDict.userType, scope.row.userType) }}
        </template>
      </el-table-column>
      <el-table-column
          :width="100"
          align="center"
          header-align="center"
          label="版本"
          prop="packageName"
          show-overflow-tooltip
      >
        <template #default="scope">
          <span v-if="scope.row.packageName">
            {{ scope.row.packageName }}
          </span>
          <span v-else
          ><el-button type="text" @click="addNowPackage(scope.row)"
          >添加激活版</el-button
          ></span
          >
        </template>
      </el-table-column>
      <el-table-column
          :width="180"
          align="center"
          header-align="center"
          label="到期时间"
          prop="expirationTime"
      >
        <template #default="scope">
          <span v-if="scope.row.expirationTime">
            {{ scope.row.expirationTime }}
          </span>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column
          v-for="item in commodityTypeList"
          :key="`table-col-${item.code}`"
          :label="item.name"
          :prop="item.code"
          :width="180"
          align="center"
          header-align="center"
          min-width="150"
      >
        <template #default="scope">
          <span v-if="scope.row[item.code]">
            <span
                v-if="
                !item.code.startsWith('child_') && item.code !== 'monitorNum'
              "
            >
              <el-tooltip placement="top">
                <template #content>
                  <div>
                    {{
                      convertUnit(
                          scope.row[item.code].useQuantity ?? 0,
                          item.code,
                          item.unit
                      )
                    }}/{{
                      convertUnit(
                          scope.row[item.code].totalQuantity ?? 0,
                          item.code,
                          item.unit
                      )
                    }}
                  </div>
                </template>
                <span>
                  {{
                    convertUnit(
                        (scope.row[item.code].totalQuantity ?? 0) -
                        (scope.row[item.code].useQuantity ?? 0),
                        item.code,
                        item.unit
                    )
                  }}
                </span>
              </el-tooltip>
            </span>
            <span v-else>{{
                convertUnit(
                    scope.row[item.code].totalQuantity ?? 0,
                    item.code,
                    item.unit
                )
              }}</span>
          </span>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column
          align="center"
          fixed="right"
          header-align="center"
          label="操作"
          width="380"
      >
        <template #default="scope">
          <el-button
              class="btn"
              size="small"
              type="text"
              @click="openUserDetails(scope.row.userId)"
          >用户详情
          </el-button>
          <el-button
              class="btn"
              size="small"
              type="text"
              @click="openDetails(scope.row)"
          >资产详情
          </el-button>
          <el-button
              :disabled="scope.row.userType == 2"
              class="btn"
              size="small"
              type="text"
              @click="packageUpgrade(scope.row)"
          >
            版本升级
          </el-button>
          <el-button
              class="btn"
              size="small"
              type="text"
              @click="userRenewal(scope.row)"
          >订单取消
          </el-button>
          <el-button
              class="btn"
              size="small"
              type="text"
              @click="updateUserProperty(scope.row)"
          >重新计算用户资产
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination
        v-model:limit="form.limit"
        v-model:page="form.page"
        :total="totalCount"
        @change="getDataList"
    ></pagination>
    <UserPropertyDetails
        v-if="showDetails"
        ref="userPropertyDetailsRef"
        :userId="currentRow.userId"
    ></UserPropertyDetails>
    <purchase-package
        v-if="purchasePackageShow"
        ref="purchasePackageRef"
        @close="purchasePackageShow = false"
        @is-ok="getDataList"
    />
    <IncrementBuy
        v-if="incrementBuyShow"
        ref="incrementBuyRef"
        @close="incrementBuyShow = false"
        @is-ok="getDataList"
    ></IncrementBuy>
    <orderStop
        v-if="orderStopShow"
        ref="orderStopRef"
        :userId="currentRow.userId"
        @close="orderStopShow = false"
        @is-ok="getDataList"
    ></orderStop>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import { ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import pagination from '@/components/commonComponent/pagination.vue'
import UserPropertyDetails from '@/views/userInfo/userProperty/components/userPropertyDetails.vue'
import PurchasePackage from '../userList/indexCopy/purchasePackage.vue'
import IncrementBuy from '@/views/userInfo/userList/indexCopy/incrementBuy.vue'
import orderStop from '@/views/userInfo/userOrder/components/orderStop.vue'
import { useDict } from '@/hooks/useDict.js'
import { useCommonHooks } from '@/hooks/useCommonHooks.js'
import { useMixinTable } from '@/hooks/useMixinTable.js'
import { customSvg } from '@/utils/icon.js'
import api from '@/utils/request-api'

const router = useRouter()
const {userDict, getLabel} = useDict()
const {convertUnit, getConvertUnitValue} = useCommonHooks()

const showDetails = ref(false)
const commodityTypeList = ref([])
const dataList = ref([])
const totalCount = ref(0)
const dataListLoading = ref(false)
const currentRow = ref({})
const purchasePackageShow = ref(false)
const incrementBuyShow = ref(false)
const orderStopShow = ref(false)

const myForm = ref(null)
const userPropertyDetailsRef = ref(null)
const purchasePackageRef = ref(null)
const incrementBuyRef = ref(null)
const orderStopRef = ref(null)

const form = reactive({
  keywords: '',
  commodityTypeCode: {},
  page: 1,
  limit: 10
})

const getDataList = async () => {
  dataListLoading.value = true
  const data = JSON.parse(JSON.stringify(form))
  const temp = data.commodityTypeCode
  data.commodityTypeCode = []
  if (temp) {
    for (const key in temp) {
      const min = getConvertUnitValue(temp[key][0], key)
      const max = getConvertUnitValue(temp[key][1], key)
      data.commodityTypeCode.push({
        code: key,
        min: min,
        max: max
      })
    }
  }

  const res = await api.userproperty.list(data)

  if (res && res.code === 0) {
    if (res.data?.list?.length > 0) {
      res.data.list.forEach((item) => {
        item.userPropertyTypeList?.forEach((val) => {
          item[val.commodityTypeCode] = val
        })
      })
    }
    dataList.value = res.data.list
    totalCount.value = res.data.totalCount
    dataListLoading.value = false
  } else {
    dataList.value = []
    totalCount.value = 0
  }
}

const {setUrlParams, setUrl, form: mixinForm} = useMixinTable(getDataList)

Object.assign(form, mixinForm.value)

const checkCommodityCode = (rule, value, callback) => {
  if (value?.length === 2) {
    if (value[0] && !/^[1-9]\d*$/.test(value[0])) {
      return callback(new Error('请输入大于0的正整数'))
    }
    if (value[1] && !/^[1-9]\d*$/.test(value[1])) {
      return callback(new Error('请输入大于0的正整数'))
    }
  }
  callback()
}

const getCommodityTypeList = async () => {
  const res = await api.commoditytype.list({limit: -1})
  if (res?.code == 0) {
    if (res.data?.list?.length > 0) {
      res.data.list.reverse()
    }
    res?.data?.list?.forEach((item) => {
      if (item?.code.startsWith('child_')) {
        item.showSearch = false
      } else {
        item.showSearch = true
        form.commodityTypeCode[item.code] = [null, null]
      }
    })
    commodityTypeList.value = res.data.list
  }
}

const search = async () => {
  await myForm.value.validate()
  form.page = 1
  await getDataList()
}

const openDetails = async (row) => {
  currentRow.value = row
  showDetails.value = true
  await nextTick()
  userPropertyDetailsRef.value.init(row)
}

const openUserDetails = async (userId) => {
  const resolved = router.resolve({
    path: '/userInfo/userList',
    query: {
      userId,
      componentName: 'userDetail'
    }
  })
  window.open(window.location.origin + resolved.href, '_blank')
}

const changeSubAccount = (item) => {
  if (item.userType == 2) {
    ElMessage.error('子账号不能购买版本或者增量包')
    throw new Error('子账号不能购买版本或者增量包')
  }
  if (!item.packageName) {
    ElMessage.error('用户未购买版本')
    throw new Error('用户未购买版本')
  }
}

const userRenewal = async (item) => {
  changeSubAccount(item)
  orderStopShow.value = true
  await nextTick()
  orderStopRef.value.init(item.userId)
}

const packageUpgrade = async (item) => {
  changeSubAccount(item)
  purchasePackageShow.value = true
  await nextTick()
  purchasePackageRef.value.packageUpgrade(item.userId)
}

const addNowPackage = async (row) => {
  const res = await api.order.newPcCreateOrder({userId: row.userId})
  if (res?.code == 0) {
    ElMessage.success('处理成功')
    await getDataList()
  }
}

const updateUserProperty = async (row) => {
  await ElMessageBox.confirm(`确定用户要重新统计资产吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })

  const res = await api.userproperty.statisticsUserProperty([row.userId])
  if (res?.code == 0) {
    ElMessage.success(res.msg)
    await getDataList()
  }
}

onMounted(async () => {
  setUrlParams()
  form.commodityTypeCode = {}
  await Promise.all([getCommodityTypeList(), getDataList()])
})
</script>
<style lang="scss" scoped>
.mod-config {
  padding: 15px;

  .btn {
    padding: 0;
  }
}
</style>
