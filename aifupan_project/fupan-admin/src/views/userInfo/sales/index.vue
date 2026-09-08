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
        <el-button icon="Search" plain type="primary" @click="search()"
        >查询
        </el-button
        >
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
          label="跟进销售人员名"
          prop="salesName"
          width="150px"
      >
      </el-table-column>
      <el-table-column
          align="center"
          header-align="center"
          label="获客助手链接"
          min-width="200px"
          prop="salesIntroductionUrl"
      >
      </el-table-column>
      <el-table-column
          align="center"
          header-align="center"
          label="创建时间"
          min-width="180px"
          prop="createDate"
      >
      </el-table-column>
      <el-table-column
          align="center"
          header-align="center"
          label="最后修改时间"
          min-width="180px"
          prop="updateDate"
      >
      </el-table-column>
      <el-table-column
          align="center"
          header-align="center"
          label="是否分配线索"
          min-width="150px"
          prop="isChoose"
      >
        <template #default="{ row }">
          <el-switch
              v-model="row.isChoose"
              :active-value="1"
              :inactive-value="0"
              active-color="#13ce66"
              active-text="开"
              inactive-color="#dcdfe6"
              inactive-text="关"
              @change="handleSwitchChange($event, row.id)"
          >
          </el-switch>
        </template>
      </el-table-column>
      <el-table-column
          align="center"
          fixed="right"
          header-align="center"
          label="操作"
          width="150"
      >
        <template #default="{ row }">
          <el-button
              size="small"
              type="primary"
              @click="addOrUpdateHandle(row.id)"
          >修改
          </el-button
          >
          <el-button size="small" type="danger" @click="deleteHandle(row.id)"
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
        layout="total, sizes, prev, pager, next, ->, jumper"
        style="text-align: center; margin-top: 10px"
        @size-change="sizeChangeHandle"
        @current-change="currentChangeHandle"
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
import api from '@/utils/request-api'
import { customSvg } from '@/utils/icon.js'
import AddOrUpdate from './sales-add-or-update.vue'

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

const search = () => {
  pageIndex.value = 1
  getDataList()
}

const getDataList = async () => {
  dataListLoading.value = true
  dataForm.page = pageIndex.value
  dataForm.limit = pageSize.value

  const res = await api.sales.list(dataForm)
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
    addOrUpdateRef.value?.init(id)
  })
}

const deleteHandle = (id) => {
  ElMessageBox.confirm('确定要进行删除吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    const res = await api.sales.delete({id})
    if (res && res.code === 0) {
      getDataList()
      ElMessage({
        message: res.msg,
        type: 'success'
      })
    }
  })
}

const handleSwitchChange = async (val, id) => {
  const {code} = await api.sales.update({
    id,
    isChoose: val
  })
  if (code === 0) {
    ElMessage({
      message: '操作成功',
      type: 'success'
    })
    getDataList()
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
