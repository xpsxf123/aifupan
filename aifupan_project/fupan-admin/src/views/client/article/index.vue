<template>
  <div class="mod-config">
    <el-form :inline="true" :model="dataForm">
      <el-form-item>
        <el-input v-model="dataForm.keyword" clearable placeholder="输入关键字搜索" style="width: 160px"></el-input>
      </el-form-item>
      <el-form-item>
        <el-select v-model="dataForm.type" clearable placeholder="按文章类型筛选" style="width: 160px">
          <el-option v-for="item in typeList" :key="item.id" :label="item.label" :value="item.value">
          </el-option>
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button icon="search" plain type="primary" @click="search()">查询</el-button>
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
        style="width: 100%;"
    >
      <!-- <el-table-column prop="id" header-align="center" align="center" label="ID">
      </el-table-column> -->
      <el-table-column :min-width="160" align="center" header-align="center" label="文章标题" prop="title"
                       show-overflow-tooltip>
      </el-table-column>
      <el-table-column align="center" header-align="center" label="备注" prop="remarks" show-overflow-tooltip>
      </el-table-column>
      <el-table-column align="center" header-align="center" label="类型" prop="type" show-overflow-tooltip>
        <template #default="{ row }">
          <span>{{ typeList.filter(item => item.value == row.type)[0]?.label }}</span>
        </template>
      </el-table-column>
      <el-table-column :width="180" align="center" header-align="center" label="创建时间" prop="createDate"
                       show-overflow-tooltip>
      </el-table-column>
      <el-table-column :width="180" align="center" header-align="center" label="最后修改时间" prop="updateDate"
                       show-overflow-tooltip>
      </el-table-column>
      <el-table-column align="center" fixed="right" header-align="center" label="操作" width="150">
        <template #default="{ row }">
          <el-button size="small" type="primary" @click="addOrUpdateHandle(row.id)">修改</el-button>
          <el-button size="small" type="danger" @click="deleteHandle(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination
        :background="true"
        :current-page="pageIndex"
        :page-size="pageSize"
        :page-sizes="[10, 20, 50]"
        :total="totalCount"
        layout="total, sizes, prev, pager,next,->, jumper"
        style="text-align: center; margin-top: 10px"
        @size-change="sizeChangeHandle"
        @current-change="currentChangeHandle"
    >
    </el-pagination>
    <!-- 弹窗, 新增 / 修改 -->
    <add-or-update v-if="addOrUpdateVisible" ref="addOrUpdate" @refreshDataList="getDataList"></add-or-update>
  </div>
</template>
<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import api from '@/utils/request-api'
import { customSvg } from '@/utils/icon.js'
import AddOrUpdate from './article-add-or-update.vue'

const typeList = ref([])
const dataForm = reactive({
  keyword: '',
  type: ''
})
const dataList = ref([])
const pageIndex = ref(1)
const pageSize = ref(10)
const totalCount = ref(0)
const dataListLoading = ref(false)
const addOrUpdateVisible = ref(false)
const addOrUpdate = ref()

const getTypeList = async () => {
  typeList.value = []
  const res = await api.dictdata.list({limit: -1, typeLogo: 'article_type'})
  if (res.code == 0) {
    typeList.value = res.data.list
    typeList.value.forEach(item => {
      item.value = parseInt(item.value)
    })
  }
}

const search = () => {
  pageIndex.value = 1
  getDataList()
}

const getDataList = async () => {
  dataListLoading.value = true
  const params = {...dataForm, page: pageIndex.value, limit: pageSize.value}
  const res = await api.article.list(params)
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

const deleteHandle = async (id) => {
  await ElMessageBox.confirm(`确定要进行删除吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  const res = await api.article.delete({id})
  if (res && res.code === 0) {
    await getDataList()
    ElMessage({
      message: res.msg,
      type: 'success'
    })
  }
}

onMounted(async () => {
  await Promise.all([getTypeList(), getDataList()])
})
</script>
<style lang="less" scoped>
.mod-config {
  padding: 15px;
}

:deep(.el-table__fixed-right ) {
  height: 100% !important;
}
</style>