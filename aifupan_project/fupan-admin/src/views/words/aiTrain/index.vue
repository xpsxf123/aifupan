<template>
  <div class="mod-config">
    <el-form :inline="true" :model="dataForm">
      <!-- <el-form-item>
        <el-input v-model="dataForm.keyword" placeholder="输入关键字搜索" clearable></el-input>
      </el-form-item> -->
      <el-form-item>
        <el-select
            v-model="dataForm.aiStatus"
            clearable
            placeholder="按训练状态筛选"
            style="width: 200px"
        >
          <el-option
              v-for="(item, index) in [
              '训练中',
              '后台手动训练完成',
              '超时训练完成',
            ]"
              :key="`status-${index}`"
              :label="item"
              :value="index"
          >
          </el-option>
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button icon="Search" plain type="primary" @click="search">查询</el-button>
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
          label="用户"
          prop="userNickName"
          show-overflow-tooltip
      >
      </el-table-column>
      <el-table-column
          align="center"
          header-align="center"
          label="视频"
          prop="videoName"
          show-overflow-tooltip
          width="550"
      >
      </el-table-column>
      <el-table-column
          align="center"
          header-align="center"
          label="行业"
          prop="tradeName"
          show-overflow-tooltip
      >
      </el-table-column>
      <el-table-column
          align="center"
          header-align="center"
          label="状态"
          prop="aiStatus"
          show-overflow-tooltip
      >
        <template #default="scope">
          {{
            ['训练中', '后台手动训练完成', '超时训练完成'][scope.row.aiStatus]
          }}
        </template>
      </el-table-column>
      <el-table-column
          :width="120"
          align="center"
          header-align="center"
          label="进步幅度"
          prop="progressRange"
      >
        <template #default="scope">
          <span v-if="scope.row.progressRange"
          >{{ retainDecimals(scope.row.progressRange * 100) }}%</span
          >
        </template>
      </el-table-column>
      <el-table-column
          :width="180"
          align="center"
          header-align="center"
          label="创建时间"
          prop="createDate"
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
          <!-- <el-button type="primary" size="small" @click="addOrUpdateHandle(scope.row.id)">修改</el-button>
          <el-button type="danger" size="small" @click="deleteHandle(scope.row.id)">删除</el-button> -->
          <el-button
              v-if="scope.row.aiStatus == 0"
              size="small"
              type="primary"
              @click="completeTrain(scope.row.id)"
          >完成训练
          </el-button>
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
    <add-or-update
        v-if="addOrUpdateVisible"
        ref="addOrUpdate"
        @refreshDataList="getDataList"
    ></add-or-update>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import AddOrUpdate from './aitrain-add-or-update.vue'
import myUtils from '@/utils/utils'
import api from '@/utils/request-api'
import { customSvg } from '@/utils/icon.js'

const dataForm = reactive({
  keyword: '',
  aiStatus: ''
})

const dataList = ref([])
const pageIndex = ref(1)
const pageSize = ref(10)
const totalCount = ref(0)
const dataListLoading = ref(false)
const addOrUpdateVisible = ref(false)
const addOrUpdate = ref(null)

onMounted(() => {
  getDataList()
})
const completeTrain = async (id) => {
  await ElMessageBox.confirm('确定操作训练完成吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  const res = await api.aitrain.completeTrain({id})
  if (res && res.code === 0) {
    getDataList()
    ElMessage({
      message: res.msg,
      type: 'success'
    })
  }
}
const retainDecimals = (val) => {
  if (isNaN(val)) return 0
  return myUtils.retainDecimals(val)
}

const search = () => {
  pageIndex.value = 1
  getDataList()
}

const getDataList = async () => {
  dataListLoading.value = true
  dataForm.page = pageIndex.value
  dataForm.limit = pageSize.value

  try {
    const res = await api.aitrain.list(dataForm)
    if (res && res.code === 0) {
      dataList.value = res.data.list
      totalCount.value = res.data.totalCount
    } else {
      dataList.value = []
      totalCount.value = 0
    }
  } catch (error) {
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
  addOrUpdate.value.init(id)
}
const deleteHandle = async (id) => {
  await ElMessageBox.confirm('确定要进行删除吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })

  const res = await api.aitrain.delete({id})
  if (res && res.code === 0) {
    getDataList()
    ElMessage({
      message: res.msg,
      type: 'success'
    })
  }
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
