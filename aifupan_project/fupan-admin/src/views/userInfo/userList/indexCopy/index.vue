<template>
  <div class="mod-config">
    <el-form :inline="true" :model="form">
      <el-form-item label="用户搜索：">
        <el-input
            v-model="form.keyword"
            clearable
            placeholder="昵称/手机号搜索"
            style="width: 140px"
        ></el-input>
      </el-form-item>
      <el-form-item label="微信名称搜索：">
        <el-input
            v-model="form.userWxName"
            clearable
            placeholder="微信名称搜索"
            style="width: 140px"
        ></el-input>
      </el-form-item>
      <el-form-item label="公司搜索：">
        <el-input
            v-model="form.companyName"
            clearable
            placeholder="输入公司名称"
            style="width: 140px"
        ></el-input>
      </el-form-item>
      <el-form-item label="行业搜索：">
        <el-cascader
            :key="cascaderNum"
            v-model="form.tradeId"
            :options="tradeTreeList"
            :props="{ checkStrictly: true, value: 'id', label: 'name' }"
            clearable
            filterable
            placeholder="归属行业"
            style="width: 140px"
            @change="tradeChange"
        >
        </el-cascader>
      </el-form-item>
      <el-form-item label="版本搜索：">
        <el-select
            v-model="form.packageId"
            clearable
            placeholder="版本"
            style="width: 140px; margin-right: 10px"
        >
          <el-option
              v-for="item in packageIdS"
              :key="item.id"
              :label="item.name"
              :value="item.id"
          >
          </el-option>
        </el-select>
        <el-form-item label="销售搜索：">
          <el-select
              v-model="form.salesId"
              clearable
              placeholder="销售人员姓名"
              style="width: 140px"
          >
            <el-option
                v-for="item in salesList"
                :key="item.id"
                :label="item.salesName"
                :value="item.id"
            >
            </el-option>
          </el-select>
        </el-form-item>
      </el-form-item>
      <el-form-item label="付费到期时间（低于）:">
        <el-select
            v-model="form.expireTime"
            clearable
            placeholder="到期时间"
            style="width: 140px; margin-left: 10px"
        >
          <el-option
              v-for="item in expireTimeList"
              :key="item.id"
              :label="item.name"
              :value="item.value"
          >
          </el-option>
        </el-select>
      </el-form-item>
      <el-form-item label="日平均分析搜索：">
        <div style="width: 230px; display: flex; align-items: center">
          <div style="flex: 1">
            <el-input
                v-model="form.startDayAnalysis"
                :min="1"
                clearable
                placeholder="开始值"
                type="number"
            />
          </div>
          <div style="text-align: center; width: 25px">至</div>
          <div style="flex: 1">
            <el-input
                v-model="form.endDayAnalysis"
                :min="1"
                clearable
                placeholder="结束值"
                type="number"
            />
          </div>
        </div>
      </el-form-item>
      <el-form-item label="多久未分析搜索：">
        <div style="width: 230px; display: flex; align-items: center">
          <div style="flex: 1">
            <el-input
                v-model="form.startLongNotAnalysis"
                :min="1"
                clearable
                placeholder="开始值"
                type="number"
            />
          </div>
          <div style="text-align: center; width: 25px">至</div>
          <div style="flex: 1">
            <el-input
                v-model="form.endLongNotAnalysis"
                :min="1"
                clearable
                placeholder="结束值"
                type="number"
            />
          </div>
        </div>
      </el-form-item>
      <el-form-item label="时间搜索：">
        <el-date-picker
            v-model="form.startEndDate"
            :default-time="[
            new Date(0, 0, 0, 0, 0, 0),
            new Date(0, 0, 0, 23, 59, 59),
          ]"
            end-placeholder="结束日期"
            format="YYYY-MM-DD"
            range-separator="至"
            start-placeholder="开始日期"
            style="width: 260px"
            type="daterange"
            value-format="YYYY-MM-DD HH:mm:ss"
        />
      </el-form-item>
      <el-form-item>
        <el-button icon="Search" plain type="primary" @click="search"
        >查询
        </el-button
        >
        <el-button type="primary" @click="exportUserInfo"
        >导出用户主播列表
        </el-button
        >
        <el-button v-if="admin" type="danger" @click="deleteHandle()"
        >删除
        </el-button
        >
        <el-button type="success" @click="invokeTimingUpdateData"
        >更新用户汇总
        </el-button
        >
      </el-form-item>
    </el-form>
    <el-table
        v-loading="dataListLoading"
        :data="dataList"
        :element-loading-spinner="customSvg"
        border
        header-row-class-name="my-header-row"
        stripe
        style="width: 100%"
        @selection-change="handleSelectionChange"
    >
      <el-table-column
          v-if="admin"
          align="center"
          header-align="center"
          label="选择"
          type="selection"
          width="40"
      ></el-table-column>
      <el-table-column
          align="center"
          header-align="center"
          label="昵称"
          min-width="100"
          prop="nickName"
          show-overflow-tooltip
      />
      <el-table-column
          align="center"
          header-align="center"
          label="微信昵称"
          min-width="140"
          prop="wxName"
          show-overflow-tooltip
      />
      <el-table-column
          align="center"
          header-align="center"
          label="来源渠道"
          min-width="140"
          prop="channelName"
          show-overflow-tooltip
      />
      <el-table-column
          align="center"
          header-align="center"
          label="手机号"
          prop="phone"
          show-overflow-tooltip
          width="120"
      />
      <el-table-column
          align="center"
          header-align="center"
          label="版本"
          min-width="120"
          prop="packageName"
          show-overflow-tooltip
      >
        <template #default="scope">
          <span v-if="scope.row.packageId">{{ scope.row.packageName }}</span>
          <span v-else
          ><el-button type="text" @click="addNowPackage(scope.row)"
          >添加激活版</el-button
          >
          </span>
        </template>
      </el-table-column>
      <el-table-column
          align="center"
          header-align="center"
          label="账号类型"
          min-width="100"
          prop="userType"
          show-overflow-tooltip
      >
        <template #default="scope">
          {{ getLabel(userDict.userType, scope.row.userType) }}
        </template>
      </el-table-column>
      <el-table-column
          align="center"
          header-align="center"
          label="跟进销售"
          prop="salesName"
          show-overflow-tooltip
          width="140"
      />
      <el-table-column
          align="center"
          header-align="center"
          label="状态"
          prop="status"
          width="150"
      >
        <template #default="scope">
          <el-switch
              v-model="scope.row.status"
              :active-value="0"
              :inactive-value="1"
              active-text="正常"
              inactive-text="冻结"
              @change="handleSwitchChange(scope.row)"
          >
          </el-switch>
        </template>
      </el-table-column>
      <el-table-column
          align="center"
          header-align="center"
          label="联系人"
          min-width="110"
          prop="linkman"
          show-overflow-tooltip
      />
      <el-table-column
          align="center"
          header-align="center"
          label="归属公司"
          min-width="120"
          prop="companyName"
          show-overflow-tooltip
      />
      <el-table-column
          align="center"
          header-align="center"
          label="归属行业"
          min-width="130"
          prop="tradeName"
          show-overflow-tooltip
      />
      <el-table-column
          align="center"
          header-align="center"
          label="注册时间"
          min-width="160"
          prop="createDate"
          show-overflow-tooltip
      />
      <el-table-column
          align="center"
          header-align="center"
          label="最高版本付费到期时间"
          prop="expireTime"
          width="210px"
      >
        <template #header="">
          <div class="header-with-icon">
            <span>最高版本付费到期时间</span>
            <div
                style="display: flex; flex-direction: column; margin-left: 10px"
            >
              <el-tooltip
                  :enterable="false"
                  class="item"
                  content="升序排列"
                  effect="dark"
                  placement="top-start"
              >
                <img
                    alt=""
                    src="@/assets/images/word_list_up.png"
                    style="width: 50%"
                    @click="handleSortClick(0)"
                />
              </el-tooltip>
              <el-tooltip
                  :enterable="false"
                  class="item"
                  content="降序排列"
                  effect="dark"
                  placement="bottom-start"
              >
                <img
                    src="@/assets/images/word_list_down.png"
                    style="width: 50%"
                    @click="handleSortClick(1)"
                />
              </el-tooltip>
            </div>
          </div>
        </template>
        <template #default="scope">
          <span v-if="scope.row.expireTime">{{ scope.row.expireTime }}天</span>
          <span v-else>暂无付费套餐</span>
        </template>
      </el-table-column>
      <el-table-column
          align="center"
          header-align="center"
          label="总分析条数"
          prop="sumAnalysis"
          width="140"
      >
        <template #header="">
          <div class="header-with-icon">
            <span>总分析条数</span>
            <div
                style="display: flex; flex-direction: column; margin-left: 10px"
            >
              <el-tooltip
                  :enterable="false"
                  class="item"
                  content="升序排列"
                  effect="dark"
                  placement="top-start"
              >
                <img
                    src="@/assets/images/word_list_up.png"
                    style="width: 50%"
                    @click="handleSortClick(2)"
                />
              </el-tooltip>
              <el-tooltip
                  :enterable="false"
                  class="item"
                  content="降序排列"
                  effect="dark"
                  placement="bottom-start"
              >
                <img
                    src="@/assets/images/word_list_down.png"
                    style="width: 50%"
                    @click="handleSortClick(3)"
                />
              </el-tooltip>
            </div>
          </div>
        </template>
        <template #default="scope"> {{ scope.row.sumAnalysis }}条</template>
      </el-table-column>
      <el-table-column
          align="center"
          header-align="center"
          label="日平均分析条数"
          prop="dayAnalysis"
          width="170"
      >
        <template #header="">
          <div class="header-with-icon">
            <span>日平均分析条数</span>
            <div
                style="display: flex; flex-direction: column; margin-left: 10px"
            >
              <el-tooltip
                  :enterable="false"
                  class="item"
                  content="升序排列"
                  effect="dark"
                  placement="top-start"
              >
                <img
                    src="@/assets/images/word_list_up.png"
                    style="width: 50%"
                    @click="handleSortClick(4)"
                />
              </el-tooltip>
              <el-tooltip
                  :enterable="false"
                  class="item"
                  content="降序排列"
                  effect="dark"
                  placement="bottom-start"
              >
                <img
                    src="@/assets/images/word_list_down.png"
                    style="width: 50%"
                    @click="handleSortClick(5)"
                />
              </el-tooltip>
            </div>
          </div>
        </template>
        <template #default="scope"> {{ scope.row.dayAnalysis }}条</template>
      </el-table-column>
      <el-table-column
          align="center"
          header-align="center"
          label="对比分析条数"
          prop="contrastAnalysis"
          width="170px"
      >
        <template #header="">
          <div class="header-with-icon">
            <span>对比分析条数</span>
            <div
                style="display: flex; flex-direction: column; margin-left: 10px"
            >
              <el-tooltip
                  :enterable="false"
                  class="item"
                  content="升序排列"
                  effect="dark"
                  placement="top-start"
              >
                <img
                    src="@/assets/images/word_list_up.png"
                    style="width: 50%"
                    @click="handleSortClick(8)"
                />
              </el-tooltip>
              <el-tooltip
                  :enterable="false"
                  class="item"
                  content="降序排列"
                  effect="dark"
                  placement="bottom-start"
              >
                <img
                    src="@/assets/images/word_list_down.png"
                    style="width: 50%"
                    @click="handleSortClick(9)"
                />
              </el-tooltip>
            </div>
          </div>
        </template>
        <template #default="scope">
          {{ scope.row.contrastAnalysis }}条
        </template>
      </el-table-column>
      <el-table-column
          align="center"
          header-align="center"
          label="多久未分析"
          prop="longNotAnalysis"
          width="140px"
      >
        <template #header="">
          <div class="header-with-icon">
            <span>多久未分析</span>
            <div
                style="display: flex; flex-direction: column; margin-left: 10px"
            >
              <el-tooltip
                  :enterable="false"
                  class="item"
                  content="升序排列"
                  effect="dark"
                  placement="top-start"
              >
                <img
                    src="@/assets/images/word_list_up.png"
                    style="width: 50%"
                    @click="handleSortClick(6)"
                />
              </el-tooltip>
              <el-tooltip
                  :enterable="false"
                  class="item"
                  content="降序排列"
                  effect="dark"
                  placement="bottom-start"
              >
                <img
                    src="@/assets/images/word_list_down.png"
                    style="width: 50%"
                    @click="handleSortClick(7)"
                />
              </el-tooltip>
            </div>
          </div>
        </template>
        <template #default="scope">
          <span v-if="scope.row.longNotAnalysis"
          >{{ scope.row.longNotAnalysis }}天</span
          >
          <span v-else>从未分析</span>
        </template>
      </el-table-column>
      <el-table-column
          align="center"
          fixed="right"
          header-align="center"
          label="操作"
          width="200"
      >
        <template #default="scope">
          <el-button
              class="btn"
              size="small"
              type="text"
              @click="addOrUpdateHandle(scope.row.id)"
          >详情
          </el-button
          >
          <el-button
              v-if="false"
              :disabled="scope.row.packageLevel <= 0 || scope.row.userType == 2"
              class="btn"
              size="small"
              type="text"
              @click="userRenewal(scope.row)"
          >续费
          </el-button>
          <el-button
              v-if="false"
              :disabled="scope.row.userType == 2"
              class="btn"
              size="small"
              type="text"
              @click="packageUpgrade(scope.row)"
          >版本升级
          </el-button>
          <el-button
              v-if="false"
              :disabled="scope.row.userType == 2"
              class="btn"
              size="small"
              type="text"
              @click="incrementBuy(scope.row)"
          >购买增量包
          </el-button>
          <el-button
              class="btn"
              size="small"
              text
              type="danger"
              @click="resetPassword(scope.row.id)"
          >重置密码
          </el-button
          >
        </template>
      </el-table-column>
    </el-table>
    <pagination
        v-model:limit="form.limit"
        v-model:page="form.page"
        :total="totalCount"
        @change="getDataList"
    ></pagination>
    <Details
        v-if="addOrUpdate"
        ref="addOrUpdate"
        @refreshDataList="getDataList"
    ></Details>
    <userProperty
        v-if="addOrUserProperty"
        ref="addOrUpdate"
        @refreshDataList="getDataList"
    ></userProperty>

    <purchase-package
        v-if="purchasePackageShow"
        ref="purchasePackageRef"
        @close="purchasePackageShow = false"
        @is-ok="getDataList"
    />
    <UserRenewal
        v-if="userRenewalShow"
        ref="userRenewalRef"
        @close="userRenewalShow = false"
        @is-ok="getDataList"
    ></UserRenewal>
    <IncrementBuy
        v-if="incrementBuyShow"
        ref="incrementBuyRef"
        @close="incrementBuyShow = false"
        @is-ok="getDataList"
    ></IncrementBuy>
    <exportUserDialog
        v-if="exportUserDialogVisible"
        ref="exportUserD"
    ></exportUserDialog>
    <!--密码提示弹窗-->
    <el-dialog
        v-model="passWordDialogVisible"
        title="密码提示"
        width="500px"
    >
      <p>密码已成功重置为 <span class="rest-password-class">{{ restPassWord }}</span> ，请及时修改密码！</p>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="handleKnow">
            我已知晓
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '@/utils/request-api'
import Details from './details.vue'
import userProperty from './userProperty.vue'
import PurchasePackage from '@/views/userInfo/userList/indexCopy/purchasePackage.vue'
import purchasePackage from '@/views/userInfo/userList/indexCopy/purchasePackage.vue'
import UserRenewal from '@/views/userInfo/userList/indexCopy/userRenewal.vue'
import IncrementBuy from '@/views/userInfo/userList/indexCopy/incrementBuy.vue'
import pagination from '@/components/commonComponent/pagination.vue'
import exportUserDialog from '../components/exportUserDialog.vue'
import { customSvg } from '@/utils/icon.js'
import * as XLSX from 'xlsx'

const route = useRoute()
const router = useRouter()
const purchasePackageShow = ref(false)
const userRenewalShow = ref(false)
const incrementBuyShow = ref(false)
const addOrUpdate = ref(false)
const selectedRows = ref([])
const dataList = ref([])
const pageIndex = ref(1)
const pageSize = ref(10)
const totalCount = ref(0)
const dataListLoading = ref(false)
const addOrUserProperty = ref(false)
const tradeTreeList = ref([])
const cascaderNum = ref(0)
const admin = ref(0)
const packageIdS = ref([])
const exportUserDialogVisible = ref(false)
const invitationCodeArrays = ref([])
const salesList = ref([])
const searchUpOrDown = ref(0)

const incrementBuyRef = ref()
const userRenewalRef = ref()
const purchasePackageRef = ref()
const exportUserD = ref()
let passWordDialogVisible = ref(false)
let restPassWord = ref('')
const form = reactive({
  page: 1,
  limit: 10,
  keyword: '',
  userWxName: '',
  companyName: '',
  tradeId: '',
  packageId: '',
  salesId: '',
  expireTime: '',
  startDayAnalysis: '',
  endDayAnalysis: '',
  startLongNotAnalysis: '',
  endLongNotAnalysis: '',
  startEndDate: [],
  startTime: null,
  endTime: null,
  specialSorting: 0,
  searchUpOrDown: 0
})

const customHeaders = reactive({
  nickName: '用户昵称',
  userPhone: '用户手机号',
  userCreateDate: '用户注册日期',
  anchorNickName: '主播昵称',
  anchorUrl: '主播抖音号',
  anchorTrade: '主播行业',
  addAnchorDate: '添加主播日期'
})

const expireTimeList = ref([
  {id: 1, name: '30天', value: 30},
  {id: 2, name: '15天', value: 15},
  {id: 3, name: '7天', value: 7},
  {id: 4, name: '3天', value: 3}
])

const userDict = reactive({
  userType: []
})

const getLabel = (dict, value) => {
  const item = dict.find((item) => item.value === value)
  return item ? item.label : value
}

const setUrlParams = () => {
  // URL参数设置逻辑
}

const setUrl = () => {
  // URL设置逻辑
}

const exportToExcel = () => {
  const headers = Object.keys(customHeaders).map((key) => customHeaders[key])

  const dataWithHeader = [
    headers,
    ...invitationCodeArrays.value.map((item) => Object.values(item))
  ]

  const ws = XLSX.utils.json_to_sheet(dataWithHeader, {skipHeader: true})
  const wb = XLSX.utils.book_new()
  XLSX.utils.book_append_sheet(wb, ws, 'Sheet1')
  XLSX.writeFile(wb, 'exported_data.xlsx')
}

const exportUserInfo = () => {
  if (form.startEndDate?.length > 1) {
    form.startTime = form.startEndDate[0]
    form.endTime = form.startEndDate[1]
  } else {
    form.endTime = null
    form.startTime = null
  }
  api.user.exportUserInfoList(form).then((res) => {
    if (res.code === 0 && res.data != null) {
      invitationCodeArrays.value = res.data
      exportToExcel()
    } else {
      ElMessage.warning('当前选择的时间范围内暂无用户信息，请重新选择~')
    }
  })
}

const invokeTimingUpdateData = () => {
  api.openapiv2000.invokeTimingUpdateData().then((res) => {
    ElMessage.success('处理成功')
  })
}

const getPackageList = () => {
  packageIdS.value = []
  api.package.list({packageType: 1, limit: -1}).then((res) => {
    if (res.data && res.code == 0) {
      packageIdS.value = res.data.list
    }
  })
}

const getSalesList = () => {
  salesList.value = []
  api.sales.list({limit: -1}).then((res) => {
    if (res && res.code === 0) {
      salesList.value = res.data.list
      console.log(res.data)
    }
  })
}

const getTradeTreeList = () => {
  tradeTreeList.value = []
  api.trade.listTree({}).then((res) => {
    if (res && res.code === 0) {
      tradeTreeList.value = res.data
      cascaderNum.value++
    }
  })
}

const tradeChange = (value) => {
  if (value && value.length > 0) {
    form.tradeId = value[value.length - 1]
  } else {
    form.tradeId = ''
  }
}

const addNowPackage = (row) => {
  api.order.newPcCreateOrder({userId: row.id}).then((res) => {
    if (res?.code == 0) {
      ElMessage.success('处理成功')
      getDataList()
    } else {
      ElMessage.error(res.msg)
    }
  })
}

const changeSubAccount = (item) => {
  if (item.userType == 2) {
    ElMessage.error('子账号不能购买版本或者增量包')
    throw new Error('子账号不能购买版本或者增量包')
  }
}

const incrementBuy = (item) => {
  changeSubAccount(item)
  incrementBuyShow.value = true
  nextTick(() => {
    incrementBuyRef.value.init(item.id)
  })
}

const userRenewal = (item) => {
  changeSubAccount(item)
  userRenewalShow.value = true
  nextTick(() => {
    userRenewalRef.value.packageUpgrade(item.id)
  })
}

const packageUpgrade = (item) => {
  changeSubAccount(item)
  purchasePackageShow.value = true
  nextTick(() => {
    purchasePackageRef.value.packageUpgrade(item.id)
  })
}

const handleSelectionChange = (val) => {
  selectedRows.value = val
}

const handleSwitchChange = (formData) => {
  let data = {userId: formData.id, status: formData.status}
  api.user.updateUser(data).then((res) => {
    if (res && res.code === 0) {
      ElMessage.success('处理成功')
      getDataList()
    }
  })
}

const resetPassword = (id) => {
  ElMessageBox.confirm('是否重置密码?', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    api.user.resetPassword({id}).then((res) => {
      if (res && res.code === 0) {
        restPassWord.value = res.data
        passWordDialogVisible.value = true
      }
    })
  })
}
const handleKnow = () => {
  passWordDialogVisible.value = false
  ElMessage.success('密码重置成功')
}
const search = () => {
  form.page = 1
  form.specialSorting = 0
  getDataList()
}

const expireTimeSearch = () => {
  form.specialSorting = 1
  form.searchUpOrDown = searchUpOrDown.value
  getDataList()
}

const handleSortClick = (order) => {
  searchUpOrDown.value = order
  expireTimeSearch()
}

const getDataList = () => {
  setUrl()
  dataListLoading.value = true
  if (form.startEndDate?.length > 1) {
    form.startTime = form.startEndDate[0]
    form.endTime = form.startEndDate[1]
  } else {
    form.endTime = null
    form.startTime = null
  }
  api.user.pageList(form).then((res) => {
    if (res && res.code === 0) {
      dataList.value = res.data.list
      totalCount.value = res.data.totalCount
    } else {
      dataList.value = []
      totalCount.value = 0
    }
    dataListLoading.value = false
  })
}

const sizeChangeHandle = (val) => {
  pageSize.value = val
  pageIndex.value = 1
  getDataList()
}

const currentChangeHandle = (val) => {
  pageIndex.value = val
  getDataList()
}

const addOrUpdateHandle = (userId) => {
  if (userId) {
    const resolved = router.resolve({
      path: '/userInfo/userList',
      query: {
        userId,
        componentName: 'userDetail'
      }
    })
    window.open(window.location.origin + resolved.href, '_blank')
  }
}

const deleteHandle = () => {
  if (selectedRows.value?.length <= 0) {
    ElMessage.error('请选择用户')
    return
  }
  const ids = selectedRows.value.map((row) => row.id)
  console.log('==+' + ids)
  ElMessageBox.confirm(`确定要进行删除吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    api.user.deleteByIds(ids).then((res) => {
      if (res && res.code === 0) {
        getDataList()
        ElMessage.success(res.msg)
      }
    })
  })
}

onMounted(() => {
  admin.value = route?.query?.admin ?? 0
  setUrlParams()
  getDataList()
  getTradeTreeList()
  getPackageList()
  getSalesList()
})
</script>

<style lang="less" scoped>
:deep(.el-form-item__label) {
  padding: 0;
}

.header-with-icon {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.mod-config {
  padding: 15px;
}

:deep(.el-table__fixed-right) {
  height: 100% !important;
}

.mod-config {
  padding: 15px;
}

.second-button {
  margin-top: 14px;
  margin-right: 30px;
  margin-left: -1px;
}

.but-color {
  color: #409eff;
}

:deep(.is-disabled) {
  color: #c0c4cc;
  cursor: not-allowed;
  background-image: none;
  background-color: #ffffff;
}

.rest-password-class {
  color: rgb(245, 108, 108);
}

.btn {
  padding: 5px;
}
</style>
