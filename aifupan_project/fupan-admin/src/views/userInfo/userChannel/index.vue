<template>
  <div class="mod-config">
    <el-form :inline="true" :model="dataForm">
      <el-form-item>
        <el-button type="primary" @click="addOrUpdateHandle()"
        >新增一级来源渠道
        </el-button
        >
      </el-form-item>
    </el-form>
    <el-table
        v-loading="dataListLoading"
        :data="dataList"
        :element-loading-spinner="customSvg"
        border
        default-expand-all
        header-row-class-name="my-header-row"
        row-key="id"
        size="default"
        stripe
        style="width: 100%"
    >
      <el-table-column
          align="left"
          header-align="center"
          label="渠道名称"
          prop="channelName"
          width="350"
      ></el-table-column>
      <el-table-column
          align="center"
          header-align="center"
          label="创建时间"
          min-width="160px"
          prop="createDate"
      ></el-table-column>
      <el-table-column
          align="center"
          header-align="center"
          label="最后修改时间"
          min-width="160px"
          prop="updateDate"
      ></el-table-column>
      <el-table-column
          :width="260"
          align="center"
          fixed="right"
          header-align="center"
          label="操作"
      >
        <template #default="scope">
          <el-button
              size="small"
              type="primary"
              @click="addOrUpdateHandle(0, scope.row.id)"
          >添加子渠道
          </el-button
          >
          <el-button
              size="small"
              type="primary"
              @click="addOrUpdateHandle(scope.row.id, 0)"
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
    <AddOrUpdate
        v-if="addOrUpdateVisible"
        ref="addOrUpdate"
        @refreshDataList="getDataList"
    ></AddOrUpdate>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import api from '@/utils/request-api'
import { customSvg } from '@/utils/icon.js'
import AddOrUpdate from './channel-add-or-update.vue'

const dataForm = reactive({
  keyword: ''
})

const dataList = ref([])
const pageIndex = ref(1)
const pageSize = ref(10)
const dataListLoading = ref(false)
const dataListSelections = ref([])
const addOrUpdateVisible = ref(false)

const addOrUpdate = ref(null)

const search = () => {
  pageIndex.value = 1
  getDataList()
}

const getDataList = async () => {
  dataListLoading.value = true
  dataForm.page = pageIndex.value
  dataForm.limit = pageSize.value

  const res = await api.channel.listTree({childrenNotNull: 1})
  if (res && res.code === 0) {
    dataList.value = res.data
  } else {
    dataList.value = []
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

const addOrUpdateHandle = (id, parentId) => {
  addOrUpdateVisible.value = true
  nextTick(() => {
    addOrUpdate.value?.init(id, parentId)
  })
}

const deleteHandle = async (id) => {
  await ElMessageBox.confirm('确定要进行删除吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })

  const res = await api.channel.delete({id})
  if (res && res.code === 0) {
    getDataList()
    ElMessage({
      message: res.msg,
      type: 'success'
    })
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
