<template>
  <div class="mod-config">
    <el-form :inline="true" :model="dataForm">
      <el-form-item>
        <el-input
            v-model="dataForm.userName"
            clearable
            placeholder="输入昵称/账号/手机号搜索"
            style="width: 210px"
            suffix-icon="el-icon-search"
        ></el-input>
      </el-form-item>
      <el-form-item>
        <el-input
            v-model="dataForm.videoName"
            clearable
            placeholder="输入视频名称搜索"
            style="width: 160px"
            suffix-icon="el-icon-search"
        ></el-input>
      </el-form-item>
      <el-form-item>
        <el-input
            v-model="dataForm.anchorName"
            clearable
            placeholder="输入主播名称搜索"
            style="width: 160px"
            suffix-icon="el-icon-search"
        ></el-input>
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
        <el-cascader
            :key="cascaderNum"
            v-model="dataForm.tradeId"
            :options="tradeTreeList"
            :props="{ checkStrictly: true, value: 'id', label: 'name' }"
            class="custom-select"
            clearable
            filterable
            placeholder="选择录制行业搜索"
            @change="tradeChange"
        >
        </el-cascader>
      </el-form-item>
      <el-form-item>
        <el-select
            v-model="dataForm.analysisStatus"
            clearable
            placeholder="选择分析状态搜索"
            style="width: 180px"
        >
          <el-option
              v-for="item in analysisStatusOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
          >
          </el-option>
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-input-number
            v-model="dataForm.totalWatchNum"
            :min="1"
            clearable
            placeholder="场观大于"
        />
      </el-form-item>
      <el-form-item>
        <el-input-number
            v-model="dataForm.volume"
            :min="1"
            clearable
            placeholder="销售额大于"
            style="width: 180px"
        />
      </el-form-item>
      <el-form-item>
        <el-button
            class="audiao-video-button"
            icon="Search"
            plain
            type="primary"
            @click="search()"
        >查询
        </el-button>
        <el-button icon="Delete" type="danger" @click="deleteHandle"
        >删除
        </el-button>
        <!-- <el-button type="primary" @click="addOrUpdateHandle()">新增</el-button> -->
      </el-form-item>
    </el-form>

    <el-table
        v-loading="dataListLoading"
        :data="dataList"
        :element-loading-spinner="customSvg"
        border
        header-row-class-name="my-header-row"
        stripe
        style="width: 100%"
        @selection-change="handleSelectionChange"
    >
      <el-table-column
          align="center"
          header-align="center"
          label="选择"
          type="selection"
      ></el-table-column>
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
          align="center"
          header-align="center"
          label="视频名称"
          min-width="390px"
          prop="videoName"
          show-overflow-tooltip
      >
      </el-table-column>

      <el-table-column
          align="center"
          header-align="center"
          label="主播名称"
          min-width="160px"
          prop="anchorName"
          show-overflow-tooltip
      >
      </el-table-column>
      <el-table-column
          :width="100"
          align="center"
          header-align="center"
          label="视频大小"
          prop="vedioSizie"
      >
        <template #default="scope">
          {{ formatSize(scope.row.vedioSizie) }}
        </template>
      </el-table-column>
      <!-- <el-table-column prop="platform" header-align="center" align="center" label="开始时间">
       <template slot-scope="scope">
         <span v-if="scope.row.platform ===0 ">抖音</span>
         <span v-else-if="scope.row.platform ===1">快手 </span>
         <span v-else>视频号</span>
       </template>
     </el-table-column> -->
      <el-table-column
          :width="120"
          align="center"
          header-align="center"
          label="视频时长"
          prop="duration"
      >
      </el-table-column>

      <el-table-column
          :formatter="formatterAnalysisStatus"
          :width="120"
          align="center"
          header-align="center"
          label="分析状态"
          prop="analysisStatus"
      >
      </el-table-column>
      <el-table-column
          :min-width="180"
          align="center"
          header-align="center"
          label="录制行业"
          prop="tradeName"
          show-overflow-tooltip
      >
      </el-table-column>
      <el-table-column
          :width="180"
          align="center"
          header-align="center"
          label="录制时间"
          prop="createDate"
      >
      </el-table-column>
      <el-table-column
          :width="80"
          align="center"
          header-align="center"
          label="场观"
      >
        <template #default="{ row }">
          <span>{{ row.totalWatchNum }} </span>
          <span v-if="row.totalWatchNum">人</span>
        </template>
      </el-table-column>
      <el-table-column
          align="center"
          header-align="center"
          label="销售额"
          min-width="150px"
      >
        <template #default="{ row }">
          <span v-if="row.volumeStart && row.volumeEnd"
          >{{ row.volumeStart }}元-{{ row.volumeEnd }}元</span
          >
        </template>
      </el-table-column>
      <el-table-column label="修正数据看板数据" width="150px">
        <template #default="{ row }">
          <div style="display: flex; justify-content: center">
            <el-button
                class="audiao-video-button"
                size="small"
                type="success"
                @click="handleFixed(row)"
            >
              <template #icon>
                <SvgIcon name="repair"/>
              </template>
              修正
            </el-button>
          </div>
        </template>
      </el-table-column>
      <el-table-column
          align="center"
          fixed="right"
          header-align="center"
          label="操作"
          width="150"
      >
        <template #default="scope">
          <!-- 子组件传给父组件 -->
          <!-- <el-button type="primary" size="mini" @click="hideContent(scope.row.id)">查看分析记录</el-button> -->
          <!--  -->
          <!-- <el-button type="primary" size="mini" @click="pushByUrl(scope.row.videoId)">查看分析记录</el-button> -->
          <el-button
              class="audiao-video-button"
              size="small"
              type="primary"
              @click="selectVideoAnalysis(scope.row.videoId)"
          >
            查看分析记录
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
    >
    </el-pagination>

    <AnchorVideoAnalysis
        v-if="analysisDialogVisible"
        ref="anchorVideoAnalysis"
        :isMobile="isMobile"
    ></AnchorVideoAnalysis>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import myUtils from '@/utils/utils'
import AnchorVideoAnalysis from './anchorVideoAnalysisDialog.vue'
import api from '@/utils/request-api'
import { customSvg } from '@/utils/icon.js'
import { useSystemInfoStore } from '@/store'
import { storeToRefs } from 'pinia'

const {isMobile} = storeToRefs(useSystemInfoStore())
const router = useRouter()
const route = useRoute()

// 响应式数据
const analysisDialogVisible = ref(false)
const showTipKey = ref(1)
const searchDataFormVisible = ref(true)

const dataForm = reactive({
  userName: '',
  anchorName: '',
  videoName: '',
  tradeId: '',
  analysisStatus: '',
  totalWatchNum: undefined,
  volume: undefined
})

const selectedRows = ref([])
const selectedIds = ref([])
const value1 = ref([])
const dataList = ref([])
const pageIndex = ref(1)
const pageSize = ref(10)
const totalCount = ref(0)
const dataListLoading = ref(false)
const cascaderNum = ref(0)
const tradeTreeList = ref([])

const analysisStatusOptions = [
  {
    value: 0,
    label: '未分析'
  },
  {
    value: 1,
    label: '分析中'
  },
  {
    value: 2,
    label: '分析完成'
  },
  {
    value: 3,
    label: '分析错误'
  }
]

const analysisStatusMap = {
  0: '未分析',
  1: '分析中',
  2: '分析完成',
  3: '分析错误'
}

// refs
const anchorVideoAnalysis = ref(null)

// 查看用户详情
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

// 分析记录弹框
const selectVideoAnalysis = (videoId) => {
  analysisDialogVisible.value = true
  nextTick(() => {
    anchorVideoAnalysis.value.init(videoId)
  })
}

// 格式化文件大小
const formatSize = (sizeInKb) => {
  const sizeInMb = (sizeInKb / 1024 / 1024).toFixed(2)
  return `${sizeInMb} MB`
}

// 删除
const deleteHandle = (id) => {
  selectedIds.value = selectedRows.value.map((row) => row.videoId)
  console.log('selectedIds===' + selectedIds.value)
  ElMessageBox.confirm(`确定要进行删除吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    const res = await api.AnchorVideo.removeByVoidId(selectedIds.value)
    if (res && res.code === 0) {
      ElMessage.success('删除成功')
      getDataList()
    }
  })
}

const handleSelectionChange = (val) => {
  selectedRows.value = val
}

const hideContent = (id) => {
  emit('hideContent')
}

const pushByUrl = (videoId) => {
  router.push({
    path: '/userInfo/anchorUrl/recod',
    query: {videoId: videoId}
  })
}

// 获取数据列表
const getDataList = async (id) => {
  if (value1.value?.length > 0) {
    dataForm.endTime = value1.value[1]
    dataForm.startTime = value1.value[0]
  } else {
    dataForm.endTime = null
    dataForm.startTime = null
  }
  if (dataForm.anchorName === '') {
    dataForm.anchorName = null
  }
  if (dataForm.userName === '') {
    dataForm.userName = null
  }
  if (dataForm.videoName === '') {
    dataForm.videoName = null
  }
  dataListLoading.value = true
  dataForm.page = pageIndex.value
  dataForm.limit = pageSize.value
  dataForm.secUid = id

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
  dataListLoading.value = false
}

// 每页数
const sizeChangeHandle = (val) => {
  pageSize.value = val
  pageIndex.value = 1
  getDataList()
}

// 当前页
const currentChangeHandle = (val) => {
  pageIndex.value = val
  if (searchDataFormVisible.value) {
    Object.assign(dataForm, {
      userName: '',
      anchorName: '',
      videoName: '',
      tradeId: '',
      analysisStatus: '',
      totalWatchNum: undefined,
      volume: undefined
    })
  }
  getDataList()
}

// 新增 / 修改
const addOrUpdateHandle = (id) => {
  console.log('88==' + !visible)
  emit('update-visibility', !visible)
}

// 查询后重置当前页
const search = () => {
  if (
      dataForm.userName === null &&
      dataForm.anchorName === null &&
      dataForm.videoName === null &&
      dataForm.startTime === null &&
      dataForm.endTime === null
  ) {
    searchDataFormVisible.value = true
  } else {
    searchDataFormVisible.value = false
  }

  pageIndex.value = 1
  getDataList()
}

// 选择行业的回调
const tradeChange = (value) => {
  if (value && value.length > 0) {
    dataForm.tradeId = value[value.length - 1]
  } else {
    dataForm.tradeId = null
  }
}

// 获取行业列表树形
const getTradeTreeList = async () => {
  tradeTreeList.value = []
  const res = await api.trade.listTree({})
  if (res && res.code === 0) {
    tradeTreeList.value = res.data
    cascaderNum.value++
  }
}

// 格式化分析状态
const formatterAnalysisStatus = (row) => {
  if (row.analysisStatus !== null) {
    if (row.analysisStatus === 2 && row.useAnalysisPropertyType !== null) {
      return row.useAnalysisPropertyType === 0 ? '分析成功' : '提取成功'
    } else {
      return analysisStatusMap[row.analysisStatus]
    }
  } else {
    return ''
  }
}

// 修正数据按钮的回调
const handleFixed = async (row) => {
  const {videoId} = row
  console.log('videoId', videoId)
  console.log('!videoId', !videoId)
  if (!videoId) return
  const {code} = await api.anchorurl.backRePullData({
    videoId
  })
  if (code === 0) {
    ElMessage.success('修正数据成功')
  }
}

// 生命周期
onMounted(() => {
  getDataList(route.query.id)
  getTradeTreeList()
})

// 定义 emits
const emit = defineEmits(['hideContent', 'update-visibility'])
</script>
<style lang="less" scoped>
.mod-config {
  padding: 15px;
}

:deep(.el-table__fixed-right) {
  height: 100% !important;
}

/*.audiao-video-button:hover {
  transform: translateY(-2px);
  color: rgb(0, 255, 242);
}
*/
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
