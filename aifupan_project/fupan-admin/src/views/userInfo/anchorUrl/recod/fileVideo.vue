<template>
  <div class="file-video">
    <el-form :inline="true" :model="dataForm" class="file-video__form">
      <el-form-item>
        <el-input
            v-model="dataForm.userName"
            clearable
            placeholder="输入昵称/账号/手机号搜索"
            style="width: 210px"
        />
      </el-form-item>
      <el-form-item>
        <el-select
            v-model="dataForm.userSalesID"
            placeholder="请选择销售姓名"
            style="width: 160px"
        >
          <el-option
              v-for="item in salesList"
              :key="`sales-${item.id}`"
              :label="item.salesName"
              :value="item.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-input
            v-model="dataForm.fileName"
            clearable
            placeholder="输入文件名称搜索"
            style="width: 160px"
        />
      </el-form-item>
      <el-form-item>
        <el-date-picker
            v-model="value1"
            :default-time="[
            new Date(0, 0, 0, 0, 0, 0),
            new Date(0, 0, 0, 23, 59, 59),
          ]"
            end-placeholder="结束日期"
            format="YYYY-MM-DD"
            range-separator="至"
            start-placeholder="开始日期"
            style="width: 260px"
            type="daterange"
            value-format="YYYY-MM-DD HH:mm:ss"
        />
      </el-form-item>

      <el-form-item>
        <el-button
            class="file-video__button"
            icon="Search"
            plain
            type="primary"
            @click="search"
        >
          查询
        </el-button>
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
          label="用户昵称"
          min-width="150px"
          prop="userName"
          show-overflow-tooltip
      >
        <template #default="scope">
          <el-tooltip
              :key="showTipKey"
              :enterable="false"
              content="点击查看用户详情"
              placement="top"
          >
            <span
                class="user-name-link"
                @click="jumpUserDetail(scope.row.userId)"
            >
              {{ scope.row.userName }}
            </span>
          </el-tooltip>
        </template>
      </el-table-column>
      <el-table-column
          :width="130"
          align="center"
          header-align="center"
          label="销售人员"
          prop="userSales"
      />
      <el-table-column
          align="center"
          header-align="center"
          label="版本名"
          prop="packageName"
      />
      <el-table-column
          align="center"
          header-align="center"
          label="版本到期时间"
          prop="packageExpiredTime"
          width="180px"
      />
      <el-table-column
          :min-width="180"
          align="center"
          header-align="center"
          label="文件名称"
          prop="fileName"
          show-overflow-tooltip
      />
      <el-table-column
          :width="120"
          align="center"
          header-align="center"
          label="文件类型"
          prop="fileType"
      >
        <template #default="scope">
          <span v-if="scope.row.fileType === '0'">mp4</span>
          <span v-else-if="scope.row.fileType === '1'">mp3</span>
          <span v-else-if="scope.row.fileType === '2'">txt</span>
          <span v-else></span>
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
      <el-table-column
          align="center"
          header-align="center"
          label="分析状态"
          prop="analysisStatus"
          width="100"
      >
        <template #default="scope">
          <span v-if="scope.row.analysisStatus === 0">未分析</span>
          <span v-else-if="scope.row.analysisStatus === 1">分析中</span>
          <span v-else-if="scope.row.analysisStatus === 2">分析完成</span>
          <span v-else-if="scope.row.analysisStatus === 3">分析错误</span>
          <span v-else>视频号</span>
        </template>
      </el-table-column>
      <el-table-column
          align="center"
          header-align="center"
          label="AI分析关键词个数"
          prop="sensitiveWordTotal"
          width="160"
      />
      <el-table-column
          align="center"
          header-align="center"
          label="未在词库个数"
          prop="sensitiveWordMark"
          width="150"
      >
        <template #default="scope">
          <span>{{ scope.row.sensitiveWordMark }}</span>
          <el-button
              v-if="scope.row.sensitiveWordMark"
              type="text"
              @click="lookNotMarkWord(scope.row)"
          >
            查看
          </el-button>
        </template>
      </el-table-column>
      <el-table-column
          :min-width="180"
          align="center"
          header-align="center"
          label="行业"
          prop="tradeName"
          show-overflow-tooltip
      />
      <el-table-column
          align="center"
          header-align="center"
          label="分析时间"
          prop="analysisTime"
          width="160"
      />
      <el-table-column
          align="center"
          fixed="right"
          header-align="center"
          label="内容"
          width="150"
      >
        <template #default="scope">
          <el-button
              class="file-video__analysis-button"
              size="small"
              type="primary"
              @click="viewAnalysis(scope.row.fileId)"
          >
            查看分析内容
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination
        v-model:current-page="pageIndex"
        v-model:page-size="pageSize"
        :page-sizes="[10, 20, 50]"
        :small="isMobile"
        :total="totalCount"
        background
        layout="total, sizes, prev, pager,next,->, jumper"
        style="text-align: center; margin-top: 10px"
        @size-change="sizeChangeHandle"
        @current-change="currentChangeHandle"
    />

    <FileAnalysis
        v-if="addOrUpdateVisible"
        ref="fileAnalysisRef"
        @refresh-data-list="getDataList"
    />
    <AudioAnalysisOnline
        v-if="analysisOnlineVisible"
        ref="audioAnalysisOnlineRef"
    />
    <NotMarkWord v-if="notMarkWordVisible" ref="notMarkWordRef"/>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { customSvg } from '@/utils/icon.js'
import api from '@/utils/request-api'
import FileAnalysis from './fileAnalysis.vue'
import AudioAnalysisOnline from '../transcribe/components/audioAnalysisOnline.vue'
import NotMarkWord from './not-mark-word.vue'

const props = defineProps({
  isMobile: {
    type: Boolean,
    default: false
  }
})
console.log('props', props.isMobile)

const route = useRoute()
const router = useRouter()

const searchDataFormVisible = ref(true)
const showTipKey = ref(1)
const dataForm = ref({
  fileName: null,
  userName: null,
  userId: null,
  secUid: null,
  userSalesID: null
})

const value1 = ref([])
const dataList = ref([])
const pageIndex = ref(1)
const pageSize = ref(10)
const totalCount = ref(0)
const dataListLoading = ref(false)
const addOrUpdateVisible = ref(false)
const analysisOnlineVisible = ref(false)
const salesList = ref([])
const notMarkWordVisible = ref(false)

const fileAnalysisRef = ref(null)
const audioAnalysisOnlineRef = ref(null)
const notMarkWordRef = ref(null)

const lookNotMarkWord = (dataInfo) => {
  notMarkWordVisible.value = true
  nextTick(() => {
    notMarkWordRef.value.init(dataInfo)
  })
}

const jumpUserDetail = (userId) => {
  if (userId) {
    showTipKey.value = Date.now()
    const resolved = router.resolve({
      path: '/userInfo/userList',
      query: {
        userId,
        componentName: 'userDetail'
      }
    })
    window.open(window.location.origin + resolved.href, '_blank')
  }
}

const getSalesList = async () => {
  salesList.value = []
  const res = await api.sales.list({
    limit: -1
  })
  if (res && res.code === 0) {
    salesList.value = res.data.list
  }
}

const viewAnalysis = (fileId) => {
  const videoId = ''
  const url = router.resolve({
    name: 'analysis',
    query: {videoId: videoId, fileId: fileId}
  })
  window.open(url.href, '_blank')
}

const formatSize = (sizeInKb) => {
  const sizeInMb = (sizeInKb / 1024 / 1024).toFixed(2)
  return `${sizeInMb} MB`
}

const getDataList = async (id) => {
  if (value1.value?.length > 0) {
    dataForm.value.endTime = value1.value[1]
    dataForm.value.startTime = value1.value[0]
  } else {
    dataForm.value.endTime = null
    dataForm.value.startTime = null
  }
  dataListLoading.value = true
  dataForm.value.page = pageIndex.value
  dataForm.value.limit = pageSize.value

  const res = await api.anchorurl.FileVideo(dataForm.value)
  if (res && res.code === 0 && res.data != null) {
    dataList.value = res.data.list
    totalCount.value = res.data.totalCount
  } else {
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
  if (searchDataFormVisible.value) {
    dataForm.value = {}
    value1.value = []
  }
  getDataList()
}

const addOrUpdateHandle = (row) => {
  addOrUpdateVisible.value = true
  nextTick(() => {
    fileAnalysisRef.value.init(row.fileId, row.tradeId)
  })
}

const search = () => {
  if (
      dataForm.value.userName === null &&
      dataForm.value.fileName === null &&
      dataForm.value.startTime === null &&
      dataForm.value.endTime === null
  ) {
    searchDataFormVisible.value = true
  } else {
    searchDataFormVisible.value = false
  }

  pageIndex.value = 1
  getDataList()
}

onMounted(() => {
  getDataList(route.query.id)
  getSalesList()
})
</script>
<style lang="scss" scoped>
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

.user-name-link {
  color: #1890ff;
  cursor: pointer;
  text-decoration: none;
  display: inline-flex;
  align-items: center;
  padding: 4px 8px;
  border-radius: 4px;
  transition: all 0.3s ease;

  &:hover {
    background-color: #e6f7ff;
    color: #096dd9;
    text-decoration: underline;
    transform: translateY(-1px);
  }
}
</style>
