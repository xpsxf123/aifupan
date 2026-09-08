<template>
  <div class="mod-config">
    <el-form :inline="true" :model="dataForm">
      <el-form-item>
        <el-input
          v-model="dataForm.name"
          placeholder="版本名称搜索"
          clearable
        ></el-input>
      </el-form-item>
      <el-form-item>
        <el-button @click="search" type="primary" plain icon="Search"
          >查询</el-button
        >
        <el-button type="primary" @click="addOrUpdateHandle()">新增</el-button>
      </el-form-item>
    </el-form>
    <el-table
      :data="dataList"
      border
      stripe
      size="default"
      v-loading="dataListLoading"
      :element-loading-spinner="customSvg"
      header-row-class-name="my-header-row"
      style="width: 100%"
    >
      <el-table-column
        prop="name"
        header-align="center"
        align="center"
        label="版本名称"
        min-width="120"
      >
      </el-table-column>
      <el-table-column
        prop="version"
        header-align="center"
        align="center"
        label="版本商品"
        min-width="230"
      >
        <template #default="scope">
          <div v-if="scope.row.typeConsumptionList?.length > 0">
            <div
              v-for="(item, index) in scope.row.typeConsumptionList"
              :key="`type-consumption-${index}`"
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
        prop="commodityPriceList"
        header-align="center"
        align="center"
        label="版本价格"
        min-width="200"
      >
        <template #default="scope">
          <template v-if="scope.row.commodityPriceList?.length > 0">
            <div
              v-for="(item, index) in scope.row.commodityPriceList"
              :key="`price-${index}`"
            >
              <span v-if="item.discount == 1"
                >{{ item.realPrice / 100 }}元/{{ item.validityNum
                }}{{ getLabel(orderDict.timeUnit, item.validityUnit) }}</span
              >
              <span v-else>
                <span style="color: #bababa; text-decoration: line-through"
                  >({{ item.originalPrice / 100 }}元)</span
                >
                <span style="color: red">{{ item.realPrice / 100 }}元</span>
                /{{ item.validityNum
                }}{{ getLabel(orderDict.timeUnit, item.validityUnit) }}
              </span>
            </div>
          </template>
        </template>
      </el-table-column>
      <el-table-column
        prop="status"
        header-align="center"
        align="center"
        label="状态"
        min-width="60"
      >
        <template #default="scope">
          <span>{{ ['下架', '上架'][scope.row.status] }}</span>
        </template>
      </el-table-column>
      <el-table-column
        prop="createDate"
        header-align="center"
        align="center"
        label="创建时间"
        min-width="160"
      >
      </el-table-column>
      <el-table-column
        fixed="right"
        header-align="center"
        align="center"
        width="150"
        label="操作"
      >
        <template #default="scope">
          <el-button
            type="primary"
            size="small"
            @click="addOrUpdateHandle(scope.row.id)"
            >修改</el-button
          >
          <el-button
            type="danger"
            size="small"
            @click="deleteHandle(scope.row.id)"
            >删除</el-button
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
    <!-- 弹窗, 新增 / 修改 -->
    <saveOrUpdate
      v-if="addOrUpdateVisible"
      v-model:addOrUpdateVisible="addOrUpdateVisible"
      ref="addOrUpdateRef"
      @is-ok="getDataList"
    ></saveOrUpdate>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { useDict } from '@/hooks/useDict.js'
import { useCommonHooks } from '@/hooks/useCommonHooks.js'
import { customSvg } from '@/utils/icon.js'
import api from '@/utils/request-api'
import saveOrUpdate from './saveOrUpdate.vue'

// 路由
const route = useRoute()

// Hooks
const { orderDict, getLabel } = useDict()
const { setConvertUnitValue } = useCommonHooks()

// 响应式数据
const dataForm = reactive({
  name: '',
})

const dataList = ref([])
const pageIndex = ref(1)
const pageSize = ref(10)
const totalCount = ref(0)
const dataListLoading = ref(false)
const dataListSelections = ref([])
const addOrUpdateVisible = ref(false)
const showPrice = ref(false)
const commodity = ref({})
const currentRow = ref({})
const admin = ref(false)

// Refs
const addOrUpdateRef = ref(null)

/**
 * 查询数据
 */
const search = () => {
  pageIndex.value = 1
  getDataList()
}

/**
 * 获取数据列表
 */
const getDataList = async () => {
  dataListLoading.value = true
  const params = {
    ...dataForm,
    page: pageIndex.value,
    limit: pageSize.value,
    packageType: 2,
  }

  const res = await api.package.list(params)
  if (res && res.code === 0) {
    dataList.value = res.data.list
    totalCount.value = res.data.totalCount
  } else {
    dataList.value = []
    totalCount.value = 0
  }
  dataListLoading.value = false
}

/**
 * 每页数变化处理
 * @param {number} val - 每页数量
 */
const sizeChangeHandle = (val) => {
  pageSize.value = val
  pageIndex.value = 1
  getDataList()
}

/**
 * 当前页变化处理
 * @param {number} val - 当前页码
 */
const currentChangeHandle = (val) => {
  pageIndex.value = val
  getDataList()
}

/**
 * 新增/修改处理
 * @param {string} id - 记录ID
 */
const addOrUpdateHandle = (id) => {
  addOrUpdateVisible.value = true
  nextTick(() => {
    if (addOrUpdateRef.value) {
      addOrUpdateRef.value.init(id)
    }
  })
}

/**
 * 设置价格
 * @param {Object} item - 商品项
 */
const setPrice = (item) => {
  showPrice.value = true
  currentRow.value = item
  nextTick(() => {
    // 这里需要根据实际的ref名称调整
    // this.$refs.commodityAddOrUpdate.init(this.currentRow)
  })
}

/**
 * 提交价格
 */
const submitPrice = () => {
  console.log('点击提交')
  ElMessage.success('提交成功')
}

/**
 * 保存价格
 */
const savePrice = async () => {
  const res = await api.commodity.packageSaveOrUpdate(commodity.value)
  return res?.code === 0
}

/**
 * 添加价格
 */
const addPrice = () => {
  if (commodity.value) {
    if (!commodity.value.priceList) commodity.value.priceList = []
    commodity.value.priceList.push({
      tagId: '',
      validityNum: '',
      validityUnit: '',
      originalPrice: '',
      discount: '',
      realPrice: '',
    })
  } else {
    commodity.value = {
      name: currentRow.value.name,
      status: 1,
      priceList: [],
    }
  }
}

/**
 * 移除价格
 * @param {number} index - 索引
 */
const removePrice = (index) => {
  commodity.value.priceList.splice(index, 1)
}

/**
 * 删除处理
 * @param {string} id - 记录ID
 */
const deleteHandle = (id) => {
  ElMessageBox.confirm(`确定要进行删除吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(async () => {
    const res = await api.package.delete({ id })
    if (res && res.code === 0) {
      ElMessage.success(res.msg)
      getDataList()
    } else {
      ElMessage.error(res.msg)
    }
  })
}

// 生命周期
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
