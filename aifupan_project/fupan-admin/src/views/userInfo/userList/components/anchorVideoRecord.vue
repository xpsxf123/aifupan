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
        style="width: 100%"
    >
      <el-table-column
          align="center"
          header-align="center"
          label="视频名称"
          prop="videoName"
          show-overflow-tooltip
          width="380"
      >
      </el-table-column>
      <!-- <el-table-column prop="userName" header-align="center" align="center" label="用户昵称">
      </el-table-column> -->
      <el-table-column
          align="center"
          header-align="center"
          label="主播名称"
          prop="anchorName"
          show-overflow-tooltip
          width="150"
      >
      </el-table-column>
      <el-table-column
          :min-width="100"
          align="center"
          header-align="center"
          label="视频大小"
          prop="vedioSizie"
      >
        <template #default="scope">
          {{ formatSize(scope.row.vedioSizie) }}
        </template>
      </el-table-column>
      <el-table-column
          :min-width="100"
          align="center"
          header-align="center"
          label="视频时长"
          prop="duration"
      >
      </el-table-column>

      <el-table-column
          :min-width="100"
          align="center"
          header-align="center"
          label="分析状态"
          prop="analysisStatus"
      >
        <template #default="scope">
          <span v-if="scope.row.analysisStatus === 0">未分析 </span>
          <span v-else-if="scope.row.analysisStatus === 1">分析中 </span>
          <span v-else-if="scope.row.analysisStatus === 2">分析完成 </span>
          <span v-else-if="scope.row.analysisStatus === 3">分析错误 </span>
        </template>
      </el-table-column>
      <el-table-column
          :min-width="100"
          align="center"
          header-align="center"
          label="录制行业"
          prop="tradeName"
      >
      </el-table-column>
      <el-table-column
          align="center"
          header-align="center"
          label="录制时间"
          prop="startTime"
          width="160"
      >
      </el-table-column>
    </el-table>

    <el-pagination
        v-model:current-page="pageIndex"
        v-model:page-size="pageSize"
        :page-sizes="[5, 10]"
        :total="totalCount"
        background
        layout="total, sizes, prev, pager,next,->, jumper"
        style="text-align: center; margin-top: 10px"
        @size-change="sizeChangeHandle"
        @current-change="currentChangeHandle"
    >
    </el-pagination>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import api from '@/utils/request-api'
import myUtils from '@/utils/utils'
import { customSvg } from '@/utils/icon.js'

const props = defineProps({
  userId: {
    type: String,
    default: ''
  }
})

const analysisDialogVisible = ref(false)
const searchDataFormVisible = ref(true)

const dataForm = reactive({
  userName: '',
  anchorName: '',
  videoName: ''
})

const selectedRows = ref([])
const selectedIds = ref([])
const value1 = ref([])
const dataList = ref([])
const pageIndex = ref(1)
const pageSize = ref(5)
const totalCount = ref(0)
const dataListLoading = ref(false)
const dataListSelections = ref([])
const addOrUpdateVisible = ref(false)

const formatSize = (sizeInKb) => {
  const sizeInMb = (sizeInKb / 1024 / 1024).toFixed(2)
  return `${sizeInMb} MB`
}

const getDataList = async () => {
  dataListLoading.value = true
  dataForm.page = pageIndex.value
  dataForm.limit = pageSize.value
  dataForm.userId = props.userId

  console.log(dataForm.userId)

  try {
    const res = await api.anchorurl.AnchorVideo(dataForm)
    if (res && res.code === 0 && res.data != null) {
      dataList.value = res.data.list
      dataList.value.forEach((item) => {
        item.duration = myUtils.toformatTime(item.duration * 1000)
      })
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
      userName: '',
      anchorName: '',
      videoName: ''
    })
  }
  getDataList()
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

.audiao-video-button:hover {
  transform: translateY(-2px);
  color: rgb(0, 255, 242);
}
</style>
