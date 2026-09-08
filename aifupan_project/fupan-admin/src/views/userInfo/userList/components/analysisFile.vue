<template>
  <div class="mod-config">

    <el-table
        v-loading="dataListLoading"
        :data="dataList"
        :element-loading-spinner="customSvg"
        border
        header-row-class-name="my-header-row"
        size="default"
        stripe
        style="width: 100%">
      <!-- <el-table-column width="100" prop="userName" header-align="center" align="center" label="用户账号">
      </el-table-column> -->
      <el-table-column align="center" header-align="center" label="文件名称" prop="fileName">
      </el-table-column>
      <el-table-column :min-width="100" align="center" header-align="center" label="文件类型" prop="fileType"
      >
        <template #default="scope">
          <span v-if="scope.row.fileType === '0'">mp4</span>
          <span v-else-if="scope.row.fileType === '1'">mp3 </span>
          <span v-else-if="scope.row.fileType === '2'">txt </span>
          <span v-else> </span>
        </template>
      </el-table-column>
      <el-table-column align="center" header-align="center" label="文件大小" prop="fileSize" width="100">
        <template #default="scope">
          {{ formatSize(scope.row.fileSize) }}
        </template>
      </el-table-column>

      <el-table-column align="center" header-align="center" label="分析状态" prop="analysisStatus" width="100">
        <template #default="scope">
          <span v-if="scope.row.analysisStatus === 0">未分析</span>
          <span v-else-if="scope.row.analysisStatus === 1">分析中 </span>
          <span v-else-if="scope.row.analysisStatus === 2">分析完成 </span>
          <span v-else-if="scope.row.analysisStatus === 3">分析错误 </span>
          <span v-else>视频号</span>
        </template>
      </el-table-column>
      <el-table-column align="center" header-align="center" label="行业" prop="tradeName" width="80">
      </el-table-column>
      <el-table-column align="center" header-align="center" label="分析时间" prop="analysisTime" width="180">
      </el-table-column>
    </el-table>
    <el-pagination
        v-model:current-page="pageIndex"
        v-model:page-size="pageSize"
        :page-sizes="[5,10]"
        :total="totalCount"
        background
        layout="total, sizes, prev, pager,next,->, jumper"
        style="text-align: center; margin-top: 10px"
        @size-change="sizeChangeHandle"
        @current-change="currentChangeHandle">
    </el-pagination>

  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import api from '@/utils/request-api'
import { customSvg } from '@/utils/icon.js'

const props = defineProps({
  userId: {
    type: String,
    default: null
  }
})

const searchDataFormVisible = ref(true)

const dataForm = reactive({
  fileName: null,
  userName: null,
  userId: null,
  secUid: null
})

const value1 = ref([])
const dataList = ref([])
const pageIndex = ref(1)
const pageSize = ref(5)
const totalCount = ref(0)
const dataListLoading = ref(false)
const dataListSelections = ref([])
const addOrUpdateVisible = ref(false)
const fileAnalysis = ref(null)

const formatSize = (sizeInKb) => {
  const sizeInMb = (sizeInKb / 1024 / 1024).toFixed(2)
  return `${sizeInMb} MB`
}

const getDataList = async () => {
  dataListLoading.value = true
  dataForm.page = pageIndex.value
  dataForm.limit = pageSize.value
  dataForm.userId = props.userId

  try {
    const res = await api.fileUpload.fileAnalysisByUserId(dataForm)
    if (res && res.code === 0 && res.data != null) {
      dataList.value = res.data.list
      totalCount.value = res.data.totalCount
    } else {
      dataList.value = []
      totalCount.value = 0
    }
  } catch (error) {
    console.error('获取数据失败:', error)
    dataList.value = []
    totalCount.value = 0
  } finally {
    dataListLoading.value = false
  }
}

const sizeChangeHandle = (val) => {
  pageSize.value = val
  pageIndex.value = 1
  getDataList()
}

const currentChangeHandle = (val) => {
  pageIndex.value = val
  if (searchDataFormVisible.value) {
    Object.assign(dataForm, {
      fileName: null,
      userName: null,
      userId: null,
      secUid: null
    })
    value1.value = []
  }
  getDataList()
}

const addOrUpdateHandle = async (fileId) => {
  addOrUpdateVisible.value = true
  await nextTick()
  fileAnalysis.value.init(fileId)
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

.file-video-button:hover {
  // scale: 1.06;
  transform: translateY(-2px);
  color: rgb(0, 255, 242);
}
</style>