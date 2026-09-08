<template>
  <div class="mod-config">
    <el-form :inline="true" :model="dataForm">
      <el-form-item>
        <el-input
          style="width: 150px"
          v-model="dataForm.keyword"
          placeholder="输入邀请码搜索"
          clearable
        ></el-input>
      </el-form-item>
      <el-form-item>
        <el-select
          style="width: 150px"
          v-model="dataForm.useStatus"
          placeholder="按使用状态筛选"
          clearable
        >
          <el-option
            v-for="(item, index) in ['未使用', '已使用']"
            :key="`use-status-${index}`"
            :label="item"
            :value="index"
          >
          </el-option>
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-select
          v-model="dataForm.status"
          placeholder="按状态筛选"
          clearable
          style="width: 150px"
        >
          <el-option
            v-for="(item, index) in ['正常', '禁用']"
            :key="`status-${index}`"
            :label="item"
            :value="index"
          >
          </el-option>
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button @click="search()" type="primary" icon="Search" plain
          >查询</el-button
        >
      </el-form-item>
    </el-form>
    <el-table
      :data="dataList"
      border
      stripe
      size="default"
      v-loading="dataListLoading"
      style="width: 100%"
      :element-loading-spinner="customSvg"
      header-row-class-name="my-header-row"
    >
      <el-table-column
        prop="code"
        header-align="center"
        align="center"
        label="邀请码"
        min-width="130"
      >
      </el-table-column>
      <el-table-column
        prop="status"
        header-align="center"
        align="center"
        label="状态"
        min-width="120"
      >
        <template #default="scope">
          <span v-if="scope.row.status == 0">正常</span>
          <span v-if="scope.row.status == 1" style="color: red">禁用</span>
        </template>
      </el-table-column>
      <el-table-column
        prop="validityStartDate"
        header-align="center"
        align="center"
        label="有效期"
        show-overflow-tooltip
        min-width="210"
      >
        <template #default="scope">
          {{
            scope.row.validityStartDate.substring(0, 10) +
            '至' +
            scope.row.validityEndDate.substring(0, 10)
          }}
        </template>
      </el-table-column>
      <el-table-column
        prop="useStatus"
        header-align="center"
        align="center"
        label="使用状态"
        min-width="130"
      >
        <template #default="scope">
          <span v-if="scope.row.useStatus == 0">未使用</span>
          <span v-if="scope.row.useStatus == 1" style="color: red">已使用</span>
        </template>
      </el-table-column>
      <el-table-column
        prop="userName"
        header-align="center"
        align="center"
        label="使用人"
        min-width="130"
      >
      </el-table-column>
    </el-table>
    <el-pagination
      style="text-align: center; margin-top: 10px"
      @size-change="sizeChangeHandle"
      @current-change="currentChangeHandle"
      :current-page="pageIndex"
      :page-sizes="[8, 20, 50, 100]"
      :page-size="pageSize"
      :total="totalCount"
      layout="total, sizes, prev, pager,next,->, jumper"
      background
    >
    </el-pagination>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { customSvg } from '@/utils/icon.js'
import api from '@/utils/request-api'

const props = defineProps({
  codeBatchId: {
    type: String,
    default: '',
  },
})

const dataForm = reactive({
  keyword: '',
  codeBatchId: '',
  useStatus: '',
  status: '',
})

const dataList = ref([])
const pageIndex = ref(1)
const pageSize = ref(8)
const totalCount = ref(0)
const dataListLoading = ref(false)

const search = () => {
  pageIndex.value = 1
  getDataList()
}

const getDataList = async () => {
  dataListLoading.value = true
  dataForm.page = pageIndex.value
  dataForm.limit = pageSize.value
  dataForm.codeBatchId = props.codeBatchId

  const res = await api.invitationcode.list(dataForm)
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
