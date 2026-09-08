<template>
  <div class="mod-config">
    <el-form :inline="true" :model="dataForm">
      <el-form-item>
        <el-input
            v-model="dataForm.keyword"
            clearable
            placeholder="输入关键字搜索"
        ></el-input>
      </el-form-item>
      <el-form-item>
        <el-button icon="Search" plain type="primary" @click="search">查询</el-button>
        <el-button type="primary" @click="addOrUpdateHandle()">新增</el-button>
      </el-form-item>
    </el-form>
    <el-table
        v-loading="dataListLoading"
        :data="dataList"
        :element-loading-spinner="customSvg"
        border
        header-row-class-name="my-header-row"
        size="default"
        stripe
        style="width: 100%"
    >
      <el-table-column
          align="center"
          header-align="center"
          label="code"
          prop="kvKey"
          show-overflow-tooltip
          width="300"
      >
      </el-table-column>
      <el-table-column
          align="center"
          header-align="center"
          label="value"
          min-width="200"
          prop="kvValue"
          show-overflow-tooltip
      >
      </el-table-column>
      <el-table-column
          align="center"
          header-align="center"
          label="备注"
          prop="remarks"
          show-overflow-tooltip
      >
      </el-table-column>
      <el-table-column
          align="center"
          fixed="right"
          header-align="center"
          label="操作"
          width="150"
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
    <AddOrUpdate
        v-if="addOrUpdateVisible"
        ref="addOrUpdateRef"
        @refresh-data-list="getDataList"
    ></AddOrUpdate>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import api from '@/utils/request-api'
import AddOrUpdate from './systemkv-add-or-update.vue'
import { customSvg } from '@/utils/icon.js'

const dataForm = reactive({
  keyword: ''
})

const dataList = ref([])
const pageIndex = ref(1)
const pageSize = ref(10)
const totalCount = ref(0)
const dataListLoading = ref(false)
const addOrUpdateVisible = ref(false)
const addOrUpdateRef = ref(null)

onMounted(() => {
  getDataList()
})

const search = () => {
  pageIndex.value = 1
  getDataList()
}

const getDataList = () => {
  dataListLoading.value = true
  dataForm.page = pageIndex.value
  dataForm.limit = pageSize.value

  api.systemkv.list(dataForm).then((res) => {
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

const addOrUpdateHandle = (id) => {
  addOrUpdateVisible.value = true
  nextTick(() => {
    addOrUpdateRef.value.init(id)
  })
}

const deleteHandle = (id) => {
  ElMessageBox.confirm('确定要进行删除吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    api.systemkv
        .delete({
          id
        })
        .then((res) => {
          if (res && res.code === 0) {
            getDataList()
            ElMessage({
              message: res.msg,
              type: 'success'
            })
          }
        })
  })
}
</script>
<style lang="less" scoped>
.mod-config {
  padding: 15px;
}

:deep(.el-table__fixed-right) {
  height: 100% !important;
}
</style>
