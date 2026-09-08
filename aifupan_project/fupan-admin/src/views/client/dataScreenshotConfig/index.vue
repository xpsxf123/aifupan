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
        <el-button icon="Search" plain type="primary" @click="search"
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
          :min-width="150"
          align="center"
          header-align="center"
          label="标题"
          prop="title"
          show-overflow-tooltip
      />
      <el-table-column
          :min-width="150"
          align="center"
          header-align="center"
          label="code"
          prop="screenshotCode"
          show-overflow-tooltip
      />
      <el-table-column
          :width="80"
          align="center"
          header-align="center"
          label="排序"
          prop="sort"
      />
      <el-table-column
          align="center"
          header-align="center"
          label="示例图片"
          prop="example"
          width="150px"
      >
        <template #default="{ row }">
          <div class="img-container">
            <template v-if="row.exampleList?.length > 0">
              <img
                  v-for="(item, idx) in row.exampleList"
                  :key="'example-' + idx"
                  :src="item.url"
                  style="width: 100px; height: auto"
              />
            </template>
          </div>
        </template>
      </el-table-column>
      <el-table-column
          :min-width="160"
          align="center"
          header-align="center"
          label="备注"
          prop="remarks"
          show-overflow-tooltip
      />
      <el-table-column
          align="center"
          header-align="center"
          label="创建时间"
          prop="createDate"
          width="180px"
      />
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
        layout="total, sizes, prev, pager,next,->, jumper"
        style="text-align: center; margin-top: 10px"
        @size-change="sizeChangeHandle"
        @current-change="currentChangeHandle"
    />
    <!-- 弹窗, 新增 / 修改 -->
    <AddOrUpdate
        v-if="addOrUpdateVisible"
        ref="addOrUpdate"
        @refreshDataList="getDataList"
    />
  </div>
</template>

<script setup>
import { ref, reactive, nextTick, onMounted } from 'vue'
import AddOrUpdate from './components/dataScreenshotConfigEdit.vue'
import api from '@/utils/request-api'
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
const addOrUpdate = ref()

const search = () => {
  pageIndex.value = 1
  getDataList()
}

const getDataList = async () => {
  dataListLoading.value = true
  dataForm.page = pageIndex.value
  dataForm.limit = pageSize.value

  const res = await api.dataScreenshotConfig.list(dataForm)
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
  await nextTick()
  addOrUpdate.value?.init(id)
}

const deleteHandle = (id) => {
  ElMessageBox.confirm(`确定要进行删除吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    const res = await api.dataScreenshotConfig.delete({id})
    if (res && res.code === 0) {
      ElMessage.success(res.msg)
      getDataList()
    }
  })
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

.img-container {
  display: flex;
  justify-content: center;
  text-align: center;

  img {
    max-width: 100%;
  }
}
</style>
