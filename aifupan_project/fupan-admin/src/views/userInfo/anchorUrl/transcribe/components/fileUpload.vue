<template>
  <div class="mod-config">
    <el-form :inline="true" :model="dataForm">
      <el-form-item>
        <el-input
          v-model="dataForm.userName"
          clearable
          placeholder="输入账号搜索"
        ></el-input>
      </el-form-item>
      <el-form-item>
        <el-input
          v-model="dataForm.fileName"
          clearable
          placeholder="输入文件名称搜索"
        ></el-input>
      </el-form-item>
      <el-form-item>
        <el-date-picker
          v-model="value1"
          end-placeholder="结束日期"
          range-separator="至"
          start-placeholder="开始日期"
          type="datetimerange"
        >
        </el-date-picker>
      </el-form-item>
      <el-form-item>
        <el-button @click="getDataList()">查询</el-button>
        <!-- <el-button type="primary" @click="addOrUpdateHandle()">新增</el-button> -->
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
        label="用户账号"
        prop="userName"
        width="90"
      >
      </el-table-column>
      <el-table-column
        align="center"
        header-align="center"
        label="文件名称"
        prop="fileName"
      >
      </el-table-column>
      <el-table-column
        align="center"
        header-align="center"
        label="文件类型"
        prop="fileType"
        width="80"
      >
        <template #default="scope">
          <span v-if="scope.row.fileType === '0'">mp4</span>
          <span v-else-if="scope.row.fileType === '1'">mp3 </span>
          <span v-else-if="scope.row.fileType === '2'">txt </span>
          <span v-else> </span>
        </template>
      </el-table-column>
      <el-table-column
        align="center"
        header-align="center"
        label="文件大小"
        prop="fileSize"
        width="100"
      >
        <template #default="scope">
          {{ formatSize(scope.row.fileSize) }}
        </template>
      </el-table-column>
      <!-- <el-table-column prop="fileSize" header-align="center" align="center" label="文件字数">
     </el-table-column> -->
      <!-- <el-table-column prop="analysisStatus" header-align="center" align="center" label="分析状态">
      </el-table-column> -->
      <el-table-column
        align="center"
        header-align="center"
        label="分析状态"
        prop="analysisStatus"
        width="80"
      >
        <template #default="scope">
          <span v-if="scope.row.analysisStatus === 0">未分析</span>
          <span v-else-if="scope.row.analysisStatus === 1">分析中 </span>
          <span v-else-if="scope.row.analysisStatus === 2">分析完成 </span>
          <span v-else-if="scope.row.analysisStatus === 3">分析错误 </span>
          <span v-else>视频号</span>
        </template>
      </el-table-column>
      <el-table-column
        align="center"
        header-align="center"
        label="行业"
        prop="tradeName"
        width="100"
      >
      </el-table-column>
      <el-table-column
        align="center"
        header-align="center"
        label="分析时间"
        prop="analysisTime"
      >
      </el-table-column>
      <el-table-column
        align="center"
        fixed="right"
        header-align="center"
        label="内容"
        width="150"
      >
        <template #default="scope">
          <el-button
            size="small"
            type="primary"
            @click="addOrUpdateHandle(scope.row.id)"
            >查看分析内容</el-button
          >
        </template>
      </el-table-column>
    </el-table>
    <el-pagination
      v-model:current-page="pageIndex"
      v-model:page-size="pageSize"
      :page-sizes="[10, 20, 50]"
      :total="totalCount"
      layout="total, sizes, prev, pager,next,->, jumper"
      background
      style="text-align: center; margin-top: 10px"
      @size-change="sizeChangeHandle"
      @current-change="currentChangeHandle"
    >
    </el-pagination>
    <!-- 弹窗, 内容 -->
    <FileAnalysis
      v-if="addOrUpdateVisible"
      ref="fileAnalysis"
      @refreshDataList="getDataList"
    ></FileAnalysis>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { customSvg } from '@/utils/icon.js'
import api from '@/utils/request-api'

// 响应式数据
const dataForm = reactive({
  userName: '',
  analysisStatus: '',
  startTime: '',
  endTime: '',
})

const dataList = ref([])
const pageIndex = ref(1)
const pageSize = ref(10)
const totalCount = ref(0)
const dataListLoading = ref(false)
const dataListSelections = ref([])
const addOrUpdateVisible = ref(false)
const analysisStatusOptions = ref([
  { label: '全部', value: '' },
  { label: '未分析', value: '0' },
  { label: '分析中', value: '1' },
  { label: '分析完成', value: '2' },
  { label: '分析失败', value: '3' },
])

// 组件引用
const addOrUpdateRef = ref(null)

/**
 * 查询数据列表
 */
const getDataList = async () => {
  dataListLoading.value = true
  const params = {
    page: pageIndex.value,
    limit: pageSize.value,
    userName: dataForm.userName,
    analysisStatus: dataForm.analysisStatus,
    startTime: dataForm.startTime,
    endTime: dataForm.endTime,
  }

  const res = await api.get('/sys/anchorurl/list', { params })
  if (res && res.code === 0) {
    dataList.value = res.page.list
    totalCount.value = res.page.totalCount
  } else {
    dataList.value = []
    totalCount.value = 0
  }
  dataListLoading.value = false
}

/**
 * 每页数量改变
 */
const sizeChangeHandle = (val) => {
  pageSize.value = val
  pageIndex.value = 1
  getDataList()
}

/**
 * 当前页改变
 */
const currentChangeHandle = (val) => {
  pageIndex.value = val
  getDataList()
}

/**
 * 多选
 */
const selectionChangeHandle = (val) => {
  dataListSelections.value = val
}

/**
 * 新增 / 修改
 */
const addOrUpdateHandle = (id) => {
  addOrUpdateVisible.value = true
  if (addOrUpdateRef.value) {
    addOrUpdateRef.value.init(id)
  }
}

/**
 * 删除
 */
const deleteHandle = async (id) => {
  const ids = id ? [id] : dataListSelections.value.map((item) => item.id)
  if (ids.length === 0) {
    ElMessage.warning('请选择删除项')
    return
  }

  try {
    await ElMessageBox.confirm('确定进行删除操作?', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })

    const res = await api.post('/sys/anchorurl/delete', ids)
    if (res && res.code === 0) {
      ElMessage.success('操作成功')
      getDataList()
    } else {
      ElMessage.error(res.msg)
    }
  } catch (error) {
    // 用户取消删除
  }
}

/**
 * 格式化分析状态
 */
const formatAnalysisStatus = (row, column, cellValue) => {
  const status = analysisStatusOptions.value.find(
    (item) => item.value === cellValue + ''
  )
  return status ? status.label : '未知'
}

// 生命周期
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
