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
      <!-- <el-table-column  prop="userName" header-align="center" align="center" label="用户昵称">
      </el-table-column> -->
      <el-table-column
          align="center"
          header-align="center"
          label="主播名称"
          prop="anchorName"
          show-overflow-tooltip
          width="160"
      >
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
          label="行业名称"
          prop="tradeName"
          show-overflow-tooltip
      >
      </el-table-column>
      <el-table-column
          :min-width="100"
          align="center"
          header-align="center"
          label="平台类型"
          prop="platformType"
      >
        <template #default="scope">
          <span v-if="scope.row.platformType === '0'">全平台</span>
          <span v-else-if="scope.row.platformType === '1'">抖音 </span>
          <span v-else-if="scope.row.platformType === '2'">快手 </span>
          <span v-else-if="scope.row.platformType === '3'">视频号 </span>
        </template>
      </el-table-column>

      <el-table-column
          :min-width="100"
          align="center"
          header-align="center"
          label="分析次数"
          prop="analysisStatusVersion"
      >
        <template #default="scope">
          {{ scope.row.analysisStatusVersion }}次
        </template>
      </el-table-column>

      <el-table-column
          align="center"
          header-align="center"
          label="分析时间"
          prop="analysisTime"
          width="180"
      >
      </el-table-column>
    </el-table>
    <!-- 页码 -->
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

const dataForm = reactive({
  userName: '',
  anchorName: '',
  videoName: '',
  userId: null,
  PlatformType: null
})

const dataList = ref([])
const pageIndex = ref(1)
const pageSize = ref(5)
const totalCount = ref(0)
const dataListLoading = ref(false)

const formatSize = (sizeInKb) => {
  const sizeInMb = (sizeInKb / 1024 / 1024).toFixed(2)
  return `${sizeInMb} MB`
}

const getDataList = async () => {
  dataForm.page = pageIndex.value
  dataForm.limit = pageSize.value
  dataForm.userId = props.userId

  try {
    const res = await api.AnchorVideo.videoAnalysisByUserId(dataForm)
    if (res && res.code === 0) {
      dataList.value = res.data.list
      dataList.value?.forEach((item) => {
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

.audiao-analysis-button:hover {
  transform: translateY(-2px);
  color: rgb(0, 255, 242);
}
</style>
