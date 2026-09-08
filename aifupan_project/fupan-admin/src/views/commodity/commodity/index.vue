<template>
  <div class="mod-config">
    <el-form :inline="true" :model="dataForm">
      <el-form-item>
        <el-input
          v-model="dataForm.name"
          placeholder="输入商品名称搜索"
          clearable
          style="width: 160px"
        ></el-input>
      </el-form-item>
      <el-form-item>
        <el-select
          style="width: 160px"
          v-model="dataForm.commodityTypeId"
          placeholder="选择商品类型搜索"
          clearable
        >
          <el-option
            v-for="item in commodityTypeBeanList"
            :key="`commodity-type-${item.id}`"
            :label="item.name"
            :value="item.id"
          ></el-option>
        </el-select>
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
        label="商品名"
        width="200"
      >
      </el-table-column>
      <el-table-column
        prop="commodityTypeBean"
        header-align="center"
        align="center"
        label="商品类型"
        :width="160"
        show-overflow-tooltip
      >
        <template #default="scope">
          <span>{{ scope.row.commodityTypeName }}</span>
        </template>
      </el-table-column>
      <el-table-column
        prop="number"
        header-align="center"
        align="center"
        label="数量"
      >
        <template #default="scope">
          <span
            >{{
              setConvertUnitValue(
                scope.row.number,
                scope.row.commodityTypeCode
              )
            }}{{ scope.row.commodityTypeUnit ?? '' }}</span
          >
        </template>
      </el-table-column>
      <el-table-column
        prop="status"
        header-align="center"
        align="center"
        label="商品状态"
        :width="160"
      >
        <template #default="scope">
          <span>{{ ['未上架', '已上架'][scope.row.status] }}</span>
        </template>
      </el-table-column>
      <el-table-column
        prop="createDate"
        header-align="center"
        align="center"
        label="创建时间"
        width="180"
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
    <add-or-update
      v-if="addOrUpdateVisible"
      ref="addOrUpdateRef"
      @refreshDataList="getDataList"
    ></add-or-update>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import { useCommonHooks } from '@/hooks/useCommonHooks.js'
import { customSvg } from '@/utils/icon.js'
import api from '@/utils/request-api'
import AddOrUpdate from './commodity-add-or-update.vue'

// Hooks
const { setConvertUnitValue } = useCommonHooks()

// 响应式数据
const dataForm = reactive({
  name: '',
  commodityTypeBean: null,
  correlationsId: null,
})

const dataList = ref([])
const commodityTypeBeanList = ref([])
const pageIndex = ref(1)
const pageSize = ref(10)
const totalCount = ref(0)
const dataListLoading = ref(false)
const dataListSelections = ref([])
const addOrUpdateVisible = ref(false)

// Refs
const addOrUpdateRef = ref(null)

/**
 * 获取商品类型列表
 */
const getCommodityTypeList = async () => {
  commodityTypeBeanList.value = []
  const res = await api.commoditytype.list({ limit: -1 })
  if (res.code == 0) {
    commodityTypeBeanList.value = res.data.list
  }
}

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
    isPackage: 0,
  }

  const res = await api.commodity.list(params)
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
    addOrUpdateRef.value.init(id)
  })
}

/**
 * 删除处理
 * @param {string} id - 记录ID
 */
const deleteHandle = (id) => {
  ElMessageBox.confirm('确定要进行删除吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(async () => {
    const res = await api.commodity.delete({ id })
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
  getCommodityTypeList()
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
</style>
