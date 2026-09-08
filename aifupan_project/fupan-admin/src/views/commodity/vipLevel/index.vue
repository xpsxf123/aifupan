<template>
  <div class="mod-config">
    <el-form :inline="true" :model="dataForm">
      <el-form-item>
        <el-input
            v-model="dataForm.name"
            clearable
            placeholder="输入关键字搜索"
        ></el-input>
      </el-form-item>
      <el-form-item>
        <el-button icon="Search" plain type="primary" @click="search"
        >查询
        </el-button
        >
        <el-button v-if="admin" type="primary" @click="addOrUpdateHandle()"
        >新增
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
        size="small"
        stripe
        style="width: 100%"
    >
      <el-table-column
          align="center"
          header-align="center"
          label="版本名称"
          min-width="120"
          prop="name"
      >
      </el-table-column>
      <el-table-column
          align="center"
          header-align="center"
          label="版本商品"
          min-width="230"
          prop="version"
      >
        <template #default="scope">
          <div v-if="scope.row.typeConsumptionList?.length > 0">
            <div
                v-for="(item, index) in scope.row.typeConsumptionList"
                :key="`type-${scope.row.id}-${index}`"
            >
              <span
              >{{ item.commodityTypeName }}:{{
                  setConvertUnitValue(item.number, item.commodityTypeCode)
                }}{{ item.commodityTypeUnit }}</span
              >
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column
          align="center"
          header-align="center"
          label="版本价格"
          min-width="200"
          prop="commodityPriceList"
      >
        <template #default="scope">
          <template v-if="scope.row.commodityPriceList?.length > 0">
            <div
                v-for="(item, index) in scope.row.commodityPriceList"
                :key="`price-${scope.row.id}-${index}`"
            >
              <span v-if="item.discount == 1"
              >{{ item.realPrice / 100 }}元/{{
                  item.validityNum
                }}{{ getLabel(orderDict.timeUnit, item.validityUnit) }}</span
              >
              <span v-else>
                <span style="color: #bababa; text-decoration: line-through"
                >({{ item.originalPrice / 100 }}元)</span
                >
                <span style="color: red">{{ item.realPrice / 100 }}元</span>
                /{{
                  item.validityNum
                }}{{ getLabel(orderDict.timeUnit, item.validityUnit) }}
              </span>
            </div>
          </template>
        </template>
      </el-table-column>
      <el-table-column
          align="center"
          header-align="center"
          label="套餐数量"
          min-width="80"
          prop="incrementList"
      >
        <template #default="scope">
          <span>{{
              scope.row?.incrementList?.filter((val) => val.status == 1).length ??
              0
            }}</span>
        </template>
      </el-table-column>
      <el-table-column
          align="center"
          header-align="center"
          label="可以压缩"
          min-width="80"
          prop="isCompress"
      >
        <template #default="scope">
          <span>{{ ['否', '是'][scope.row.isCompress] }}</span>
        </template>
      </el-table-column>
      <el-table-column
          align="center"
          header-align="center"
          label="状态"
          min-width="60"
          prop="status"
      >
        <template #default="scope">
          <span>{{ ['下架', '上架'][scope.row.status] }}</span>
        </template>
      </el-table-column>
      <el-table-column
          align="center"
          header-align="center"
          label="创建时间"
          min-width="160"
          prop="createDate"
      >
      </el-table-column>
      <el-table-column
          align="center"
          fixed="right"
          header-align="center"
          label="操作"
          width="110"
      >
        <template #default="scope">
          <el-button
              size="small"
              type="primary"
              @click="addOrUpdateHandle(scope.row.id)"
          >修改
          </el-button
          >
          <el-button
              v-if="admin"
              size="small"
              type="primary"
              @click="synchronousPackage(scope.row.id)"
          >同步版本
          </el-button>
          <el-button
              v-if="admin"
              size="small"
              type="danger"
              @click="deleteHandle(scope.row.id)"
          >删除
          </el-button
          >
        </template>
      </el-table-column>
    </el-table>
    <el-pagination
        :current-page="pageIndex"
        :page-size="pageSize"
        :page-sizes="[10, 20, 50]"
        :total="totalCount"
        background
        layout="total, sizes, prev, pager,next,->, jumper"
        style="text-align: center; margin-top: 10px"
        @size-change="sizeChangeHandle"
        @current-change="currentChangeHandle"
    >
    </el-pagination>
    <!-- 弹窗, 新增 / 修改 -->
    <saveOrUpdate
        v-if="addOrUpdateVisible"
        ref="addOrUpdateRef"
        v-model:addOrUpdateVisible="addOrUpdateVisible"
        @is-ok="getDataList"
    ></saveOrUpdate>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { customSvg } from '@/utils/icon.js'
import { useDict } from '@/hooks/useDict.js'
import { useCommonHooks } from '@/hooks/useCommonHooks.js'
import api from '@/utils/request-api'
import saveOrUpdate from './saveOrUpdate.vue'

const route = useRoute()
const {orderDict, getLabel} = useDict()
const {setConvertUnitValue} = useCommonHooks()

const dataForm = reactive({
  name: ''
})

const dataList = ref([])
const pageIndex = ref(1)
const pageSize = ref(10)
const totalCount = ref(0)
const dataListLoading = ref(false)
const addOrUpdateVisible = ref(false)
const showPrice = ref(false)
const commodity = ref({})
const currentRow = ref({})
const admin = ref(0)
const addOrUpdateRef = ref(null)

const search = () => {
  pageIndex.value = 1
  getDataList()
}

const getDataList = async () => {
  dataListLoading.value = true
  dataForm.page = pageIndex.value
  dataForm.limit = pageSize.value
  dataForm.packageType = 1

  const res = await api.package.list(dataForm)
  if (res && res.code === 0) {
    dataList.value = res.data.list
    totalCount.value = res.data.totalCount
  } else {
    dataList.value = []
    totalCount.value = 0
  }
  dataListLoading.value = false
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

const addOrUpdateHandle = (id) => {
  addOrUpdateVisible.value = true
  nextTick(() => {
    addOrUpdateRef.value.init(id)
  })
}

const setPrice = (item) => {
  showPrice.value = true
  currentRow.value = item
  nextTick(() => {
    // this.$refs.commodityAddOrUpdate.init(this.currentRow)
  })
}

const submitPrice = () => {
  console.log('点击提交')
  ElMessage.success('提交成功')
}

const savePrice = async () => {
  const res = await api.commodity.packageSaveOrUpdate(commodity.value)
  return res?.code === 0
}

const addPrice = () => {
  if (commodity.value) {
    if (!commodity.value.priceList) commodity.value.priceList = []
    commodity.value.priceList.push({
      tagId: '',
      validityNum: '',
      validityUnit: '',
      originalPrice: '',
      discount: '',
      realPrice: ''
    })
  } else {
    commodity.value = {
      name: currentRow.value.name,
      status: 1,
      priceList: []
    }
  }
}

const removePrice = (index) => {
  commodity.value.priceList.splice(index, 1)
}

const synchronousPackage = (id) => {
  ElMessageBox.confirm(`确定要进行版本同步吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    const res = await api.package.synchronousPackage({id})
    if (res && res.code === 0) {
      ElMessage.success(res.msg)
    } else {
      ElMessage.error(res.msg)
    }
  })
}

const deleteHandle = (id) => {
  ElMessageBox.confirm(`确定要进行删除吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    const res = await api.package.delete({id})
    if (res && res.code === 0) {
      ElMessage.success(res.msg)
      getDataList()
    }
  })
}

onMounted(() => {
  admin.value = route?.query?.admin ?? 0
  getDataList()
})
</script>
<style lang="less" scoped>
.mod-config {
  padding: 15px;
}

:deep(.el-table__fixed-right) {
  height: 100% !important;
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

.priceItemContainer {
  display: flex;
  align-items: center;
  width: 100%;
}

.priceTitleItem {
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: rgb(177, 250, 177);
  font-size: 16px;
  box-sizing: border-box;
  height: 38px;
  width: 20%;
}

.priceTitleContainer {
  display: flex;
  align-items: center;
  width: 100%;
  justify-content: space-between;
}

.el-select {
  width: 100%;
}
</style>
