<template>
  <div class="mod-config">
    <el-form :inline="true" :model="dataForm">
      <el-form-item>
        <el-input
            v-model="dataForm.keyword"
            clearable
            placeholder="输入账号/昵称搜索"
        ></el-input>
      </el-form-item>
      <el-form-item>
        <el-button icon="Search" plain type="primary" @click="getDataList"
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
          label="ID"
          prop="id"
          show-overflow-tooltip
      >
      </el-table-column>
      <el-table-column
          align="center"
          header-align="center"
          label="手机号"
          prop="phone"
          width="120px"
      >
      </el-table-column>
      <el-table-column
          align="center"
          header-align="center"
          label="账号"
          prop="username"
          show-overflow-tooltip
      >
      </el-table-column>
      <el-table-column
          align="center"
          header-align="center"
          label="昵称"
          min-width="120px"
          prop="nickName"
          show-overflow-tooltip
      >
      </el-table-column>
      <el-table-column
          align="center"
          header-align="center"
          label="角色"
          prop="roleName"
          width="150px"
      >
        <template #default="{ row }">
          <div
              v-if="row?.roleList?.length > 0"
              style="display: flex; flex-direction: column"
          >
            <el-tag
                v-for="(item, index) in row.roleList"
                :key="index"
                effect="dark"
                style="margin-bottom: 5px"
                type="primary"
            >
              {{ item.name }}
            </el-tag>
          </div>
          <span v-else>无任何权限</span>
        </template>
      </el-table-column>
      <el-table-column
          align="center"
          header-align="center"
          label="创建时间"
          prop="createDate"
          width="180px"
      >
      </el-table-column>
      <el-table-column
          align="center"
          fixed="right"
          header-align="center"
          label="操作"
          width="250"
      >
        <template #default="scope">
          <el-button
              size="small"
              type="primary"
              @click="resetPassword(scope.row.id)"
          >重置密码
          </el-button>
          <el-button
              size="small"
              type="primary"
              @click="addOrUpdateHandle(scope.row.id)"
          >修改
          </el-button>
          <el-button
              size="small"
              type="danger"
              @click="deleteHandle(scope.row.id)"
          >删除
          </el-button>
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
        ref="addOrUpdate"
        @refreshDataList="getDataList"
    ></AddOrUpdate>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import api from '@/utils/request-api'
import AddOrUpdate from './user-add-or-update.vue'
import { customSvg } from '@/utils/icon.js'

const dataForm = reactive({
  keyword: '',
  userType: 1
})

const dataList = ref([])
const pageIndex = ref(1)
const pageSize = ref(10)
const totalCount = ref(0)
const dataListLoading = ref(false)
const addOrUpdateVisible = ref(false)
const addOrUpdate = ref(null)

const resetPassword = (id) => {
  ElMessageBox.confirm('将重置密码为123456, 是否继续?', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    api.user.resetPassword({id}).then((res) => {
      if (res && res.code === 0) {
        ElMessage.success(res.msg)
      }
    })
  })
}

const getDataList = () => {
  dataListLoading.value = true
  dataForm.page = pageIndex.value
  dataForm.limit = pageSize.value

  api.user.listManage(dataForm).then((res) => {
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
  ElMessageBox.confirm(`确定要进行删除吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    api.user
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
<style lang="scss" scoped>
.mod-config {
  padding: 15px;
}

:deep(.el-table__fixed-right) {
  height: 100% !important;
}
</style>
