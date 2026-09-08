<template>
  <el-dialog
    :title="'商品信息'"
    :close-on-click-modal="false"
    v-model="visible"
  >
    <div v-if="refreshData">
      <div class="InfoBox">
        <div class="labelBox">已选择</div>
        <div class="infoRow">
          <div class="infoCloum">
            <div class="labelBox">商品名称：</div>
            <div class="valueBox">{{ roworder.name }}</div>
          </div>
          <div class="infoCloum">
            <div class="labelBox">有效期：</div>
            <div class="valueBox">
              {{ roworder.validityNum
              }}{{ validityUnitName(roworder.validityUnit) }}
            </div>
          </div>
          <div class="infoCloum">
            <div class="labelBox">价格：</div>
            <div class="valueBox">{{ roworder.price / 100 }}元</div>
          </div>
        </div>
      </div>
    </div>
    <p></p>
    <el-table
      :data="dataList"
      border
      stripe
      size="small"
      v-loading="dataListLoading"
      style="width: 100%"
    >
      <el-table-column
        prop="name"
        header-align="center"
        align="center"
        label="商品名称"
      >
      </el-table-column>
      <el-table-column
        prop="name"
        header-align="center"
        align="center"
        label="商品类型"
        :formatter="tagIdName"
      >
      </el-table-column>
      <el-table-column
        prop="monitorNum"
        header-align="center"
        align="center"
        label="拥有监控位数量（个）"
      >
      </el-table-column>
      <el-table-column
        prop="anchorNum"
        header-align="center"
        align="center"
        label="添加的主播数量（个）"
      >
      </el-table-column>
      <el-table-column
        prop="aiAnalysisTime"
        header-align="center"
        align="center"
        label="拥有的ai语音分析时长（分）"
      >
      </el-table-column>
      <el-table-column
        prop="storageNum"
        header-align="center"
        align="center"
        label="空间容量KB"
      >
      </el-table-column>
      <el-table-column
        fixed="right"
        header-align="center"
        align="center"
        width="80"
        label="操作"
      >
        <template #default="scope">
          <el-button
            type="primary"
            size="small"
            @click="showRuleList(scope.row)"
            >选择</el-button
          >
        </template>
      </el-table-column>
    </el-table>
    <el-pagination
      style="text-align: center; margin-top: 10px"
      @size-change="sizeChangeHandle"
      @current-change="currentChangeHandle"
      :current-page="pageIndex"
      :page-sizes="[10, 20, 50]"
      :page-size="pageSize"
      :total="totalCount"
      layout="total, sizes, prev, pager,next,->, jumper"
      background
    >
    </el-pagination>
    <template #footer>
      <span class="dialog-footer">
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="dataFormSubmit()">确定</el-button>
      </span>
    </template>

    <rule-list
      v-if="ruleListVisible"
      @refreshDataList1="refreshDataList1"
      ref="ruleListDialogRef"
      @reRender="reRender = !reRender"
    >
    </rule-list>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/utils/request-api'

const router = useRouter()

const userId = ref(null)
const roworder = ref({})
const ruleListVisible = ref(false)
const visible = ref(false)
const dataListLoading = ref(false)
const dataList = ref([])
const selectedRows = ref([])
const pageIndex = ref(1)
const pageSize = ref(10)
const totalCount = ref(0)
const refreshData = ref(false)
const ruleListDialogRef = ref()
const reRender = ref(false)

const dataForm = reactive({})
const Form = reactive({})
const order = reactive({})

const commodityTypeBeanList = ref([])
const vipLevelList = ref([])
const orderList = ref([])

const refreshDataList1 = (row) => {
  roworder.value = null
  refreshData.value = true
  roworder.value = row
  ruleListVisible.value = false
}

const validityUnitName = (row) => {
  const units = {
    0: '小时',
    1: '天',
    2: '月',
    3: '季度',
    4: '半年',
    5: '年',
  }
  return units[row] || row
}

const tagIdName = (row) => {
  const tag = commodityTypeBeanList.value.find(
    (tag) => tag.value === row.commodityTypeBean
  )
  return tag ? tag.label : '未知'
}

const showRuleList = (id) => {
  roworder.value = null
  ruleListVisible.value = true
  nextTick(() => {
    ruleListDialogRef.value.init(id)
  })
}

const getCommodityTypeList = () => {
  api.dictdata
    .list({ limit: -1, typeLogo: 'commodity_type_bean' })
    .then((res) => {
      if (res.code == 0) {
        commodityTypeBeanList.value = res.data.list
      }
    })
}

const getVipLevelList = () => {
  vipLevelList.value = []
  api.viplevel.list({ limit: -1 }).then((res) => {
    if (res.code == 0) {
      vipLevelList.value = res.data.list
    }
  })
}

const init = (id) => {
  console.log('2===' + id)
  refreshData.value = false
  userId.value = id
  getDataList()
  getCommodityTypeList()
  visible.value = true
}

const dataFormSubmit = () => {
  orderList.value = []
  order.commodityId = roworder.value.commodityId
  order.quantity = 1
  order.commodityPriceId = roworder.value.id
  orderList.value.push(order)
  Form.userId = userId.value
  console.log('3===' + userId.value)
  Form.createOrderCommodityBoList = orderList.value
  visible.value = false
  api.order.saveCreate(Form).then((res) => {
    if (res && res.code === 0) {
      visible.value = false
      router.go(0)
    } else {
      ElMessage.error(res.msg)
    }
  })
}

const getDataList = () => {
  dataForm.page = pageIndex.value
  dataForm.limit = pageSize.value
  api.commodity.queryPageCommodity(dataForm).then((res) => {
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

const handleSelectionChange = (selectedRowsData) => {
  selectedRows.value = selectedRowsData
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

defineExpose({
  init,
})
</script>

<style lang="scss" scoped>
.InfoBox {
  margin: 0px 0px 0px 12px;
  padding: 0;
  text-align: left;
  font-size: 14px;
  width: 100%;
}

.infoRow {
  display: flex;
  flex-direction: row;
  justify-content: flex-start;
  margin: 0px 0px 10px 0px;
  border-bottom: rgb(195, 195, 212) 1px dashed;
  padding-bottom: 5px;
}

.infoCloum {
  width: 45%;
  display: flex;
}

.labelBox {
  text-align: left;
  margin: 0px;
}

.valueBox {
  text-align: left;
  margin-left: 10px;
}

.valueBox span {
  color: rgb(24, 144, 255);
  cursor: pointer;
}

.priceContainer {
  :deep(.el-input__inner) {
    border: none;
    border-radius: 0;
  }
}

.right-border {
  border-right: 0.5px solid #ddd;
}

.left-border {
  border-left: 0.5px solid #ddd;
}

.top-border {
  border-top: 0.5px solid #ddd;
}

.bottom-border {
  border-bottom: 0.5px solid #ddd;
}

.priceItem {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 20%;
  box-sizing: border-box;
  height: 42px;
}

.el-select {
  width: 100%;
}

.item-adjust {
  margin-top: -20px;
}
</style>
