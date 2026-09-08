<template>
  <div class="mod-config">
    <el-form :inline="true" :model="dataForm">
      <el-form-item>
        <el-input
          v-model="dataForm.keyword"
          placeholder="输入关键字搜索"
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
        label="商品名称"
        show-overflow-tooltip
      >
      </el-table-column>
      <el-table-column
        prop="code"
        header-align="center"
        align="center"
        label="商品类型code"
        show-overflow-tooltip
        :min-width="160"
      >
      </el-table-column>
      <el-table-column
        prop="unit"
        header-align="center"
        align="center"
        label="商品类型单位"
        min-width="150px"
      >
      </el-table-column>
      <el-table-column
        prop="createDate"
        header-align="center"
        align="center"
        label="创建时间"
        min-width="160px"
      >
      </el-table-column>
      <el-table-column
        fixed="right"
        header-align="center"
        align="center"
        :width="250"
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
            type="primary"
            size="small"
            @click="synchronousUserAssets(scope.row)"
            >同步资产类型</el-button
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
import { customSvg } from '@/utils/icon.js'
import api from '@/utils/request-api'
import AddOrUpdate from './commoditytype-add-or-update.vue'

const dataForm = reactive({
  keyword: '',
})

const dataList = ref([])
const pageIndex = ref(1)
const pageSize = ref(10)
const totalCount = ref(0)
const dataListLoading = ref(false)
const dataListSelections = ref([])
const addOrUpdateVisible = ref(false)
const addOrUpdateRef = ref()

const search = () => {
  pageIndex.value = 1
  getDataList()
}

const getDataList = async () => {
  dataListLoading.value = true
  dataForm.page = pageIndex.value
  dataForm.limit = pageSize.value

  const res = await api.commoditytype.list(dataForm)
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

const addOrUpdateHandle = async (id) => {
  addOrUpdateVisible.value = true
  await nextTick(() => {
    addOrUpdateRef.value.init(id)
  })
}

const synchronousUserAssets = async (row) => {
  await ElMessageBox.confirm(`确定同步系统用户的资产类型？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  })

  const res = await api.commoditytype.synchronousUserAssets({ id: row.id })
  if (res && res.code === 0) {
    ElMessage.success('同步成功')
  }
}

const deleteHandle = async (id) => {
  try {
    await ElMessageBox.confirm(`确定要进行删除吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })

    const res = await api.commoditytype.delete({ id })
    if (res && res.code === 0) {
      getDataList()
      ElMessage({
        message: res.msg,
        type: 'success',
      })
    }
  } catch (error) {
    // 用户取消操作
  }
}

onMounted(() => {
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
