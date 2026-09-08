<template>
  <div class="carousel-manager">
    <el-button type="primary" @click="handleAdd">新增</el-button>
    <Table
        :columns="columns"
        :loadingFlag="loadingFlag"
        :tableData="tableData"
        v-bind="pageInfo"
        @on-pageChange="handlePageChange"
    >
      <template #imgUrl="{ row }">
        <div class="img-container">
          <img :src="row.url" alt=""/>
        </div>
      </template>
      <template #operation="{ row }">
        <el-button type="text" @click="handleEdit(row)">编辑</el-button>
        <el-button style="color: rgb(245, 108, 108)" type="text" @click="handleDel(row)">删除</el-button>
        <el-popconfirm
            :title="row.imgStatus === 1 ? '确定启用吗？' : '确定停用吗？'"
            style="margin-left: 10px"
            @confirm="handleConfirm(row)"
        >
          <template #reference>
            <el-button v-if="row.imgStatus === 0" style="color: rgb(245, 108, 108)" type="text"
            >停用
            </el-button
            >
            <el-button v-if="row.imgStatus === 1" type="text">启用</el-button>
          </template>
        </el-popconfirm>
      </template>
    </Table>
    <AddOrEditDialog @get-data-list="getDataList"/>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import Table from '@/components/table/index.vue'
import AddOrEditDialog from './components/addOrEditDialog.vue'
import emitter from '@/utils/emitter'
import api from '@/utils/request-api'

// 表格列定义
const columns = ref([
  {label: '轮播图链接', prop: 'url', minWidth: 180},
  {label: '轮播排序', prop: 'sort', width: 100},
  {label: '轮播图图片', slotName: 'imgUrl', minWidth: 180},
  {width: 180, label: '操作', slotName: 'operation', fixed: 'right'}
])

// 表格数据与状态
const tableData = ref([])
const loadingFlag = ref(false)
const pageInfo = reactive({
  currentPage: 1,
  pageSize: 10,
  total: 50
})

// 新增轮播图
const handleAdd = (id = '') => {
  emitter.emit('openDialog', id)
}

// 获取数据列表
const getDataList = async () => {
  loadingFlag.value = true
  const res = await api.clientCarouselManager.list({
    page: pageInfo.currentPage,
    limit: pageInfo.pageSize
  })
  if (res.code === 0) {
    tableData.value = res.data.list ? res.data.list : []
    pageInfo.total = res.data.totalCount
  }
  loadingFlag.value = false
}

// 编辑
const handleEdit = (row) => {
  emitter.emit('openDialog', row)
}

// 删除
const handleDel = async (row) => {
  try {
    await ElMessageBox.confirm('此操作将永久删除该文件, 是否继续?', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    const res = await api.clientCarouselManager.del({id: row.id})
    if (res.code === 0) {
      await getDataList()
      ElMessage.success('删除成功')
    }
  } catch (e) {
    // 取消或错误无需额外处理
  }
}

// 分页变化
const handlePageChange = ({page, size}) => {
  pageInfo.currentPage = page
  pageInfo.pageSize = size
  getDataList()
}

// 启用/停用
const handleConfirm = async (row) => {
  const formData = JSON.parse(JSON.stringify(row))
  formData.imgStatus = row.imgStatus === 0 ? 1 : 0
  const res = await api.clientCarouselManager.update(formData)
  if (res.code === 0) {
    await getDataList()
    ElMessage.success('操作成功')
  }
}

onMounted(() => {
  getDataList()
})
</script>

<style lang="less" scoped>
.img-container {
  display: flex;
  justify-content: center;
  align-items: center;

  img {
    width: 80px;
    height: auto;
  }
}

.carousel-manager {
  padding: 15px;
}
</style>
