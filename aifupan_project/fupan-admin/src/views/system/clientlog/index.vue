<template>
  <div class="mod-config">
    <el-form :inline="true" :model="dataForm">
      <el-form-item>
        <el-input v-model="dataForm.actionName" clearable placeholder="请输入动作名称" style="width: 170px;"></el-input>
      </el-form-item>
      <el-form-item>
        <el-date-picker v-model="value1" end-placeholder="结束日期" range-separator="至" start-placeholder="开始日期"
                        type="datetimerange">
        </el-date-picker>
      </el-form-item>
      <el-form-item>
        <el-select v-model="defaultLogType" clearable placeholder="请选择类型" style="width: 170px">
          <el-option v-for="item in logTypeoptions" :key="item.value" :label="item.label" :value="item.value"
          >
          </el-option>
        </el-select>
      </el-form-item>
      <el-form-item style="margin-left: 5px;">
        <el-button icon="Search" plain type="primary" @click="seach">查询</el-button>
      </el-form-item>
      <el-form-item>
        <el-button type="danger"
                   @click="handleDeleteById">选中删除
        </el-button>
      </el-form-item>
      <el-form-item>
        <el-button type="danger" @click="handleDeleteBySearch">根据查询条件删除
        </el-button>
      </el-form-item>
    </el-form>
    <el-table v-loading="dataListLoading" :data="dataList" :element-loading-spinner="customSvg" border
              header-row-class-name="my-header-row"
              size="default" stripe style="width: 100%"
              @selection-change="handleSelectionChange">
      <el-table-column align="center" type="selection" width="55">
      </el-table-column>
      <el-table-column align="left" header-align="center" label="动作" prop="actionName" show-overflow-tooltip>
      </el-table-column>
      <el-table-column align="left" header-align="center" label="描述" prop="actionInfo"
                       show-overflow-tooltip></el-table-column>

      <el-table-column align="center" header-align="center" label="版本" prop="clientVersion">
      </el-table-column>
      <el-table-column align="center" header-align="center" label="操作用户" prop="clientUser" show-overflow-tooltip>
      </el-table-column>
      <el-table-column align="center" header-align="center" label="类型" prop="logType">
        <template #default="scope">
          <span>{{ scope.row.logType == 0 ? '正常日志' : '错误日志' }}</span>
        </template>
      </el-table-column>
      <el-table-column align="center" header-align="center" label="操作时间" prop="executeTime" width="160px">
      </el-table-column>
      <el-table-column align="center" fixed="right" header-align="center" label="操作" width="100">
        <template #default="scope">
          <el-button size="small" type="primary" @click="checkLogDetails(scope.row)">查看详情</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination :current-page="pageIndex" :page-size="pageSize"
                   :page-sizes="[10, 20, 50]" :total="totalCount" background
                   layout="total, sizes, prev, pager,next,->, jumper"
                   style="text-align: center; margin-top: 10px" @size-change="sizeChangeHandle"
                   @current-change="currentChangeHandle">
    </el-pagination>
    <LogDetails v-if="showDetails" ref="refLogDetailsRef"></LogDetails>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import api from '@/utils/request-api'
import LogDetails from './details.vue'
import { customSvg } from '@/utils/icon.js'

const logTypeoptions = [
  {
    value: '-1',
    label: '全部'
  },
  {
    value: '0',
    label: '正常日志'
  },
  {
    value: '1',
    label: '错误日志'
  }
]

const dataForm = reactive({
  actionName: null,
  startTime: null,
  endTime: null,
  logType: '-1'
})

const searchForm = reactive({
  actionName: null,
  startTime: null,
  endTime: null,
  logType: '-1'
})

const showDetails = ref(false)
const value1 = ref([])
const dataList = ref([])
const pageIndex = ref(1)
const pageSize = ref(10)
const totalCount = ref(0)
const totalPage = ref(0)
const dataListLoading = ref(false)
const dataListSelections = ref([])
const defaultLogType = ref('-1')
const refLogDetailsRef = ref(null)

onMounted(() => {
  getDataList()
})
const handleSelectionChange = (rows) => {
  let checkedLength = rows.length
  if (checkedLength > 0) {
    dataListSelections.value = []
    for (let i = 0; i < rows.length; i++) {
      dataListSelections.value.push(rows[i].id)
    }
  } else {
    dataListSelections.value = []
  }
}

const handleDeleteById = () => {
  ElMessageBox.confirm('确定要删除选中的记录?', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    if (dataListSelections.value.length > 0) {
      api.clientLog.deleteByIds(
          dataListSelections.value
      ).then((res) => {
        if (res && res.code === 0) {
          pageIndex.value = 1
          getDataList()
        }
      })
    } else {
      ElMessage.error('请选择要删除的数据')
    }
  })
}

const handleDeleteBySearch = () => {
  ElMessageBox.confirm('设置了查询条件，将会根据查询条件进行删除，如果查询条件不设置，将全部删除，您是否确定?', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    if (value1.value != '' && value1.value != null) {
      dataForm.startTime = formatDate(value1.value[0])
      dataForm.endTime = formatDate(value1.value[1])
    } else {
      dataForm.endTime = null
      dataForm.startTime = null
    }
    dataForm.logType = defaultLogType.value
    api.clientLog.delete(dataForm).then((res) => {
      if (res && res.code === 0) {
        dataForm.endTime = null
        dataForm.startTime = null
        dataForm.logType = -1
        dataForm.actionName = ''
        pageIndex.value = 1
        getDataList()
      }
    })
  })
}

const checkLogDetails = (row) => {
  showDetails.value = true
  nextTick(() => {
    refLogDetailsRef.value.init(row)
  })
}

const seach = () => {
  pageIndex.value = 1
  if (value1.value != '' && value1.value != null) {
    dataForm.startTime = formatDate(value1.value[0])
    dataForm.endTime = formatDate(value1.value[1])
  } else {
    dataForm.endTime = null
    dataForm.startTime = null
  }
  dataForm.logType = defaultLogType.value
  Object.assign(searchForm, dataForm)
  getDataList()
}

const getDataList = () => {
  dataListLoading.value = true
  if (totalPage.value == 0) {
    pageIndex.value = 1
  } else {
    if (pageIndex.value > totalPage.value) {
      pageIndex.value = totalPage.value
    }
  }
  api.clientLog.list(searchForm, pageIndex.value, pageSize.value).then((res) => {
    if (res && res.code === 0) {
      dataList.value = res.data.list
      totalCount.value = res.data.totalCount
      totalPage.value = res.data.totalPage
      dataListLoading.value = false
    }
  })
}

const formatDate = (date) => {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  const seconds = String(date.getSeconds()).padStart(2, '0')

  return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
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
</script>
<style lang="less" scoped>
.mod-config {
  padding: 15px;
}

:deep(.el-table__fixed-right) {
  height: 100% !important;
}

.mod-config {
  padding: 15px;
}

.second-button {
  margin-top: 14px;
  /* 5px + 9px = 14px，向左偏移 9px */
  // margin-right: 80px;
}

.second-button1 {
  margin-top: 0px;
  /* 5px + 9px = 14px，向左偏移 9px */
  // margin-left: 100px;
}
</style>