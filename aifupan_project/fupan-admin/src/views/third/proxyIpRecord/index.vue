<template>
  <div class="mod-config">
    <el-form :inline="true" :model="dataForm">
      <el-form-item>
        <el-input
            v-model="dataForm.keyword"
            clearable
            placeholder="输入用户手机号或昵称搜索"
        ></el-input>
      </el-form-item>
      <el-form-item>
        <el-button @click="search()">查询</el-button>
        <!-- <el-button type="primary" @click="addOrUpdateHandle()">新增</el-button> -->
      </el-form-item>
    </el-form>
    <el-table
        v-loading="dataListLoading"
        :data="dataList"
        border
        size="default"
        stripe
        style="width: 100%"
    >
      <el-table-column
          align="center"
          header-align="center"
          label="用户"
          prop="userNickName"
      >
      </el-table-column>
      <el-table-column
          align="center"
          header-align="center"
          label="IP"
          prop="ipStr"
      >
      </el-table-column>
      <el-table-column
          align="center"
          header-align="center"
          label="端口号"
          prop="portStr"
      >
      </el-table-column>
      <el-table-column
          align="center"
          header-align="center"
          label="提取时间"
          prop="extractDate"
      >
      </el-table-column>
      <el-table-column
          align="center"
          header-align="center"
          label="有效期"
          prop="validityDate"
      >
      </el-table-column>
      <el-table-column
          align="center"
          header-align="center"
          label="IP有效时长"
          prop="ipEffectiveTime"
      >
        <template #default="scope">
          {{ scope.row.ipEffectiveTime + '分钟' }}
        </template>
      </el-table-column>
      <el-table-column
          align="center"
          header-align="center"
          label="创建时间"
          prop="createDate"
      >
      </el-table-column>
      <!-- <el-table-column fixed="right" header-align="center" align="center" width="150" label="操作">
        <template slot-scope="scope">
          <el-button type="primary" size="mini" @click="addOrUpdateHandle(scope.row.id)">修改</el-button>
          <el-button type="danger" size="mini" @click="deleteHandle(scope.row.id)">删除</el-button>
        </template>
      </el-table-column> -->
    </el-table>
    <el-pagination
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
    <AddOrUpdate
        v-if="addOrUpdateVisible"
        ref="addOrUpdate"
        @refresh-data-list="getDataList"
    ></AddOrUpdate>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import api from '@/utils/request-api'
import AddOrUpdate from './proxyiprecord-add-or-update.vue'

const dataForm = reactive({
  keyword: ''
})

const dataList = ref([])
const pageIndex = ref(1)
const pageSize = ref(10)
const totalCount = ref(0)
const dataListLoading = ref(false)
const addOrUpdateVisible = ref(false)
const addOrUpdate = ref(null)

const search = () => {
  pageIndex.value = 1
  getDataList()
}

const getDataList = () => {
  dataListLoading.value = true
  dataForm.page = pageIndex.value
  dataForm.limit = pageSize.value

  api.proxyiprecord.list(dataForm).then((res) => {
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
    addOrUpdate.value.init(id)
  })
}

const deleteHandle = (id) => {
  ElMessageBox.confirm('确定要进行删除吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    api.proxyiprecord
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
