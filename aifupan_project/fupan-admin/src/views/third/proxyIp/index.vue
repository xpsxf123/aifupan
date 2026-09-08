<template>
  <div class="mod-config">
    <el-form :inline="true" :model="dataForm">
      <!-- <el-form-item>
        <el-input v-model="dataForm.keyword" placeholder="输入关键字搜索" clearable></el-input>
      </el-form-item> -->
      <el-form-item>
        <!-- <el-button @click="search()">查询</el-button> -->
        <el-button type="primary" @click="addOrUpdateHandle()">新增</el-button>
      </el-form-item>
    </el-form>
    <el-table v-loading="dataListLoading" :data="dataList" :element-loading-spinner="customSvg"
              border header-row-class-name="my-header-row" size="default"
              stripe style="width: 100%;">
      <el-table-column align="center" header-align="center" label="提取IP的url" prop="extractUrl" show-overflow-tooltip
                       width="450">
      </el-table-column>
      <el-table-column align="center" header-align="center" label="总IP数" prop="totalNum">
      </el-table-column>
      <el-table-column align="center" header-align="center" label="已用IP数">
        <template #default="scope">
          {{ scope.row.totalNum - scope.row.remainingNum }}
        </template>
      </el-table-column>
      <el-table-column align="center" header-align="center" label="剩余IP数" prop="remainingNum">
      </el-table-column>
      <el-table-column align="center" header-align="center" label="IP有效时长" min-width="100px" prop="ipEffectiveTime">
        <template #default="scope">
          {{ scope.row.ipEffectiveTime + '分钟' }}
        </template>
      </el-table-column>
      <el-table-column align="center" header-align="center" label="时效类型" prop="validityType" width="100px">
        <template #default="scope">
          {{ ['短效', '长效'][scope.row.validityType] }}
        </template>
      </el-table-column>
      <el-table-column align="center" header-align="center" label="代理验证账号" min-width="110px" prop="proxyUsername">
      </el-table-column>
      <el-table-column align="center" header-align="center" label="代理验证密码" min-width="110px" prop="proxyPassword">
      </el-table-column>
      <el-table-column align="center" header-align="center" label="创建时间" prop="createDate" width="180px">
      </el-table-column>
      <el-table-column align="center" header-align="center" label="激活状态" prop="activeStatus" width="100px">
        <!--        <template #default="{row}">
                  {{ ['未激活', '已激活'][scope.row.activeStatus] }}
                </template>-->
        <template #default="{row}">
          <div v-if="row.activeStatus===0" class="close status">
            <SvgIcon :icon-style="{width:'15px',height:'15px'}" name="dot-green"/>
            <p>未激活</p>
          </div>
          <div v-else class="open status">
            <SvgIcon :icon-style="{width:'15px',height:'15px'}" name="dot"/>
            <p>已激活</p>
          </div>
        </template>
      </el-table-column>
      <el-table-column align="center" fixed="right" header-align="center" label="操作" width="150">
        <template #default="scope">
          <el-button size="small" type="primary" @click="addOrUpdateHandle(scope.row.id)">修改</el-button>
          <!--          <el-button type="danger" size="small" @click="deleteHandle(scope.row.id)">删除</el-button> -->
          <el-button :type="scope.row.activeStatus == 0 ? 'success' : 'danger'" size="small"
                     @click="activeHandle(scope.row.id, scope.row.activeStatus)">
            {{ scope.row.activeStatus == 0 ? '激活' : '禁用' }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination :current-page="pageIndex" :page-size="pageSize"
                   :page-sizes="[10, 20, 50]" :total="totalCount" background
                   layout="total, sizes, prev, pager,next,->, jumper"
                   style="text-align: center; margin-top: 10px" @size-change="sizeChangeHandle"
                   @current-change="currentChangeHandle">
    </el-pagination>
    <!-- 弹窗, 新增 / 修改 -->
    <AddOrUpdate v-if="addOrUpdateVisible" ref="addOrUpdate" @refresh-data-list="getDataList"></AddOrUpdate>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import api from '@/utils/request-api'
import AddOrUpdate from './proxyip-add-or-update.vue'
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
const addOrUpdate = ref(null)

const search = () => {
  pageIndex.value = 1
  getDataList()
}

const getDataList = () => {
  dataListLoading.value = true
  dataForm.page = pageIndex.value
  dataForm.limit = pageSize.value

  api.proxyip.list(dataForm).then((res) => {
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

const activeHandle = (id, activeStatus) => {
  activeStatus = activeStatus == 0 ? 1 : 0
  const requestData = {
    id,
    activeStatus
  }

  api.proxyip.update(requestData).then((res) => {
    if (res && res.code === 0) {
      getDataList()
      ElMessage({
        message: res.msg,
        type: 'success'
      })
    }
  })
}

const deleteHandle = (id) => {
  ElMessageBox.confirm('确定要进行删除吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    api.proxyip.delete({
      id
    }).then((res) => {
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

.status {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 5px
}
</style>