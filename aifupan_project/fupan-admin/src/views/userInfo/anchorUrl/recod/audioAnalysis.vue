<template>
  <div class="mod-config">
    <el-form :inline="true" :model="dataForm" class="mod-config">
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
        <el-select
            v-model="dataForm.userSalesID"
            clearable
            placeholder="请选择销售姓名"
            style="width: 160px"
        >
          <el-option
              v-for="item in salesList"
              :key="item.id"
              :label="item.salesName"
              :value="item.id"
          >
          </el-option>
        </el-select>
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
        <el-cascader
            :key="cascaderNum"
            v-model="dataForm.tradeId"
            :options="tradeTreeList"
            :props="{ checkStrictly: true, value: 'id', label: 'name' }"
            clearable
            filterable
            placeholder="选择行业搜索"
            style="width: 170px"
            @change="tradeChange"
        >
        </el-cascader>
      </el-form-item>
      <el-form-item>
        <el-select
            v-model="dataForm.accountType"
            clearable
            placeholder="选择主播账号归属类型"
            style="width: 190px"
        >
          <el-option
              v-for="item in accountTypeOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-select
            v-model="dataForm.packageId"
            clearable
            placeholder="选择版本名搜索"
            style="width: 190px"
        >
          <el-option
              v-for="item in packageOptions"
              :key="item.id"
              :label="item.name"
              :value="item.id"
          />
        </el-select>
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
            class="audiao-analysis-button"
            icon="Search"
            plain
            type="primary"
            @click="search()"
        >查询
        </el-button>
      </el-form-item>
    </el-form>

    <el-table
        v-loading="dataListLoading"
        :data="dataList"
        :element-loading-spinner="customSvg"
        border
        class="mod-config"
        header-row-class-name="my-header-row"
        stripe
    >
      <el-table-column
          align="center"
          header-align="center"
          label="用户昵称"
          min-width="150px"
          prop="userName"
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
          show-overflow-tooltip
      >
      </el-table-column>
      <el-table-column
          :width="100"
          align="center"
          header-align="center"
          label="版本名"
          prop="packageName"
      >
      </el-table-column>
      <el-table-column
          align="center"
          header-align="center"
          label="版本到期时间"
          prop="packageExpiredTime"
          width="180px"
      >
      </el-table-column>
      <el-table-column
          :min-width="160"
          align="center"
          header-align="center"
          label="主播名称"
          prop="anchorName"
          show-overflow-tooltip
      >
      </el-table-column>
      <el-table-column
          :formatter="accountTypeFormat"
          align="center"
          header-align="center"
          label="主播账号归属类型"
          prop="accountType"
          show-overflow-tooltip
          width="160"
      >
      </el-table-column>
      <el-table-column
          :width="160"
          align="center"
          header-align="center"
          label="视频时长"
          prop="duration"
      >
      </el-table-column>
      <el-table-column
          :width="120"
          align="center"
          header-align="center"
          label="场观"
          prop="tradeName"
          show-overflow-tooltip
      >
        <template #default="{ row }">
          {{ row.totalWatchNum ? row.totalWatchNum + '人' : '' }}
        </template>
      </el-table-column>
      <el-table-column
          align="center"
          header-align="center"
          label="销售额"
          min-width="150px"
          prop="volumeStar"
          show-overflow-tooltip
      >
        <template #default="{ row }">
          <span v-if="row.volumeStart || row.volumeEnd">
            {{ formatAmount(row.volumeStart) }} ~
            {{ formatAmount(row.volumeEnd) }}
          </span>
        </template>
      </el-table-column>
      <el-table-column
          :min-width="180"
          align="center"
          header-align="center"
          label="行业名称"
          prop="tradeName"
          show-overflow-tooltip=""
      >
      </el-table-column>
      <el-table-column
          align="center"
          header-align="center"
          label="AI分析关键词个数"
          prop="sensitiveWordTotal"
          width="150px"
      >
      </el-table-column>
      <el-table-column
          align="center"
          header-align="center"
          label="未在词库个数"
          prop="sensitiveWordMark"
          width="150px"
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
          :width="120"
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
          :width="100"
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
          prop="createDate"
          width="180"
      >
      </el-table-column>

      <el-table-column
          align="center"
          fixed="right"
          header-align="center"
          label="操作"
          width="160"
      >
        <template #default="scope">
          <el-button
              class="audio-analysis__button"
              size="small"
              type="primary"
              @click="viewAnalysis(scope.row.videoId)"
          >
            查看分析内容
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
        :current-page="pageIndex"
        :page-size="pageSize"
        :page-sizes="[10, 20, 50]"
        :small="isMobile"
        :total="totalCount"
        background
        class="audio-analysis__pagination"
        layout="total, sizes, prev, pager,next,->, jumper"
        style="margin-top: 10px"
        @size-change="sizeChangeHandle"
        @current-change="currentChangeHandle"
    >
    </el-pagination>

    <VideroAnalysis
        v-if="addOrUpdateVisible"
        ref="fileAnalysis"
        @refreshDataList="getDataList"
    ></VideroAnalysis>
    <audioAnalysisOnline
        v-if="analysisOnlineVisible"
        ref="audioAnalysisOnlineD"
    ></audioAnalysisOnline>
    <not-mark-word v-if="notMarkWordVisible" ref="notMarkWord"></not-mark-word>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick, reactive } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/utils/request-api'
import { customSvg } from '@/utils/icon.js'
import myUtils from '@/utils/utils'
import VideroAnalysis from '@/views/userInfo/anchorUrl/transcribe/components/videroAnalysis.vue'
import audioAnalysisOnline from '../transcribe/components/audioAnalysisOnline.vue'
import NotMarkWord from './not-mark-word.vue'

const props = defineProps({
  parentId: {
    type: String,
    default: null
  },
  isMobile: {
    type: Boolean,
    default: false
  }
})

const router = useRouter()

const searchDataFormVisible = ref(true)
const showTipKey = ref(1)
const dataForm = ref({
  userName: null,
  anchorName: null,
  videoName: null,
  userSalesID: null,
  userId: null,
  PlatformType: null,
  anchorId: null,
  accountType: null,
  packageId: null
})
const packageOptions = ref([])
const accountTypeOptions = reactive([
  {
    value: 0,
    label: '自有账号'
  },
  {
    value: 1,
    label: '同行账号'
  }
])
const value1 = ref([])
const dataList = ref([])
const pageIndex = ref(1)
const pageSize = ref(10)
const totalCount = ref(0)
const dataListLoading = ref(false)
const addOrUpdateVisible = ref(false)
const tradeTreeList = ref([])
const cascaderNum = ref(0)
const analysisOnlineVisible = ref(false)
const salesList = ref([])
const notMarkWordVisible = ref(false)

const fileAnalysis = ref(null)
const audioAnalysisOnlineD = ref(null)
const notMarkWord = ref(null)

const init = () => {
  getDataList(props.parentId)
  getTradeTreeList()
  getSalesList()
  getVersions()
}

const lookNotMarkWord = (dataInfo) => {
  notMarkWordVisible.value = true
  nextTick(() => {
    notMarkWord.value.init(dataInfo)
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

const viewAnalysis = (videoId) => {
  let fileId = ''
  const url = router.resolve({
    name: 'analysis',
    query: {videoId: videoId, fileId: fileId}
  })
  window.open(url.href, '_blank')
}

const getSalesList = async () => {
  salesList.value = []
  try {
    const res = await api.sales.list({
      limit: -1
    })
    if (res && res.code === 0) {
      salesList.value = res.data.list
    }
  } catch (error) {
    console.error('获取销售列表失败:', error)
  }
}

const getTradeTreeList = async () => {
  tradeTreeList.value = []
  try {
    const res = await api.trade.listTree({})
    if (res && res.code === 0) {
      tradeTreeList.value = res.data
      cascaderNum.value++
    }
  } catch (error) {
    console.error('获取行业列表失败:', error)
  }
}

const tradeChange = (value) => {
  if (value && value.length > 0) {
    dataForm.value.tradeId = value[value.length - 1]
  } else {
    dataForm.value.tradeId = ''
  }
}

const addOrUpdateHandle = (row) => {
  addOrUpdateVisible.value = true
  nextTick(() => {
    fileAnalysis.value.init(row.videoId, row.tradeId)
  })
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

  dataForm.value.page = pageIndex.value
  dataForm.value.limit = pageSize.value
  if (id != null) {
    dataForm.value.videoId = id
  }

  try {
    dataListLoading.value = true
    const res = await api.anchorurl.selectAnchorVideoRecod(dataForm.value)
    if (res && res.code === 0) {
      dataList.value = res.data.list
      dataList.value.forEach((item) => {
        item.duration = myUtils.toformatTime(item.duration * 1000)
      })
      totalCount.value = res.data.totalCount
      dataListLoading.value = false
    } else {
      dataList.value = []
      totalCount.value = 0
    }
  } catch (error) {
    console.error('获取数据列表失败:', error)
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

const formatAmount = (value) => {
  if (value == null || value === '') return '--'
  const num = Number(value)
  if (isNaN(num)) return '--'

  if (num >= 100000000) {
    return (
        (num / 100000000)
            .toLocaleString('zh-CN', {
              minimumFractionDigits: 2,
              maximumFractionDigits: 2
            })
            .replace(/\.?0+$/, '') + '亿'
    )
  } else if (num >= 10000) {
    return (
        (num / 10000)
            .toLocaleString('zh-CN', {
              minimumFractionDigits: 2,
              maximumFractionDigits: 2
            })
            .replace(/\.?0+$/, '') + 'w'
    )
  } else {
    return num.toLocaleString('zh-CN', {maximumFractionDigits: 2}) + '元'
  }
}

const search = () => {
  if (
      dataForm.value.userName === null &&
      dataForm.value.anchorName === null &&
      dataForm.value.videoName === null &&
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

const accountTypeFormat = (row, column, cellValue, index) => {
  if (cellValue === 0) {
    return '自有账号'
  } else if (cellValue === 1) {
    return '同行业账号'
  } else {
    return ''
  }
}
const getVersions = () => {
  api.package.list({limit: -1, packageType: 1}).then(res => {
    if (res.data && res.data.list) {
      packageOptions.value = res.data.list
    }
  })
}
onMounted(() => {
  init()
})

defineExpose({
  init
})
</script>
<style lang="scss" scoped>
.mod-config {
  .el-form-item {
    margin-bottom: 15px;
  }
}

.audiao-analysis-button {
  margin-left: 10px;
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

.my-header-row th {
  background-color: #f5f7fa !important;
}
</style>
