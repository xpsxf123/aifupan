<template>
  <div class="mod-config">
    <el-form :inline="true" :model="dataForm">
      <el-form-item>
        <el-input
            v-model="dataForm.userNickName"
            clearable
            placeholder="输入用户昵称搜索"
            style="width: 160px"
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
      <!-- <el-form-item>
        <el-input v-model="dataForm.fileName" placeholder="输入文件名称搜索" clearable suffix-icon="el-icon-search"></el-input>
      </el-form-item> -->
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
        <el-button icon="Search" plain type="primary" @click="search"
        >查询
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
          prop="userNickName"
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
              {{ scope.row.userNickName }}
            </span>
          </el-tooltip>
        </template>
      </el-table-column>
      <el-table-column
          align="center"
          header-align="center"
          label="销售人员"
          prop="userSales"
          width="100"
      >
      </el-table-column>
      <el-table-column
          align="center"
          header-align="center"
          label="版本名"
          prop="packageName"
          width="100"
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
          align="center"
          header-align="center"
          label="视频文件一名称"
          min-width="150px"
          prop="videoNickNameOne"
          show-overflow-tooltip
      >
      </el-table-column>
      <el-table-column
          align="center"
          header-align="center"
          label="视频文件二名称"
          min-width="150px"
          prop="videoNickNameTwo"
          show-overflow-tooltip
      >
      </el-table-column>
      <el-table-column
          align="center"
          header-align="center"
          label="视频文件一行业"
          prop="tradeNickNameOne"
          show-overflow-tooltip
          width="130"
      >
      </el-table-column>
      <el-table-column
          align="center"
          header-align="center"
          label="视频文件二行业"
          min-width="150px"
          prop="tradeNickNameTwo"
          show-overflow-tooltip
          width="130"
      >
      </el-table-column>
      <el-table-column
          align="center"
          header-align="center"
          label="对比分析时间"
          prop="contrastTime"
          width="180"
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
              class="audiao-analysis-button"
              size="small"
              type="primary"
              @click="viewAnalysis(scope.row.contrastId)"
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
        layout="total, sizes, prev, pager,next,->, jumper"
        style="text-align: center; margin-top: 10px"
        @size-change="sizeChangeHandle"
        @current-change="currentChangeHandle"
    >
    </el-pagination>
    <!-- 详情内容 -->
    <analysisContrastIndex
        v-if="analysisOnlineVisible"
        ref="audioAnalysisOnlineD"
    ></analysisContrastIndex>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/utils/request-api'
import { customSvg } from '@/utils/icon.js'
import analysisContrastIndex from './analysis-contrast-index.vue'

const props = defineProps({
  isMobile: {
    type: Boolean,
    default: false
  }
})
// 响应式数据
const router = useRouter()
const searchDataFormVisible = ref(true)
const showTipKey = ref(1)
const dataForm = reactive({
  fileName: null,
  userName: null,
  userId: null,
  secUid: null,
  userSalesID: null,
  userNickName: null,
  endTime: null,
  startTime: null,
  page: 1,
  limit: 10
})

const value1 = ref([])
const dataList = ref([])
const pageIndex = ref(1)
const pageSize = ref(10)
const totalCount = ref(0)
const dataListLoading = ref(false)
const dataListSelections = ref([])
const analysisOnlineVisible = ref(false)
const salesList = ref([])

/**
 * 查看用户详情
 * @param {string} userId - 用户ID
 */
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

/**
 * 获取跟进销售人员列表
 */
const getSalesList = async () => {
  salesList.value = []
  const res = await api.sales.list({
    limit: -1
  })
  if (res && res.code === 0) {
    salesList.value = res.data.list
  }
}

/**
 * 查看分析内容
 * @param {string} contrastId - 对比ID
 */
const viewAnalysis = (contrastId) => {
  const url = router.resolve({
    name: 'contrast',
    query: {contrastId: contrastId}
  })
  window.open(url.href, '_blank')
}

/**
 * 格式化文件大小
 * @param {number} sizeInKb - KB大小
 * @returns {string} 格式化后的大小
 */
const formatSize = (sizeInKb) => {
  const sizeInMb = (sizeInKb / 1024 / 1024).toFixed(2)
  return `${sizeInMb} MB`
}

/**
 * 获取数据列表
 * @param {string} id - 查询ID
 */
const getDataList = async (id) => {
  if (value1.value?.length > 0) {
    dataForm.endTime = value1.value[1]
    dataForm.startTime = value1.value[0]
  } else {
    dataForm.endTime = null
    dataForm.startTime = null
  }
  dataListLoading.value = true
  dataForm.page = pageIndex.value
  dataForm.limit = pageSize.value

  const res = await api.synccontrast.listAllSyncContrast(dataForm)
  if (res && res.code === 0 && res.data != null) {
    dataList.value = res.data.list
    totalCount.value = res.data.totalCount
  } else {
    dataList.value = []
    totalCount.value = 0
  }
  dataListLoading.value = false
}

/**
 * 每页数变化处理
 * @param {number} val - 每页数量
 */
const sizeChangeHandle = (val) => {
  pageSize.value = val
  pageIndex.value = 1
  getDataList()
}

/**
 * 当前页变化处理
 * @param {number} val - 当前页码
 */
const currentChangeHandle = (val) => {
  pageIndex.value = val
  if (searchDataFormVisible.value) {
    Object.assign(dataForm, {
      fileName: null,
      userName: null,
      userId: null,
      secUid: null,
      userSalesID: null,
      userNickName: null,
      endTime: null,
      startTime: null
    })
    value1.value = []
  }
  getDataList()
}

/**
 * 查询后重置当前页
 */
const search = () => {
  if (
      dataForm.userName === null &&
      dataForm.fileName === null &&
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

onMounted(() => {
  const routeQuery = router.currentRoute.value.query
  getDataList(routeQuery.id)
  getSalesList()
})
</script>
<style lang="less" scoped>
:deep(.el-table__fixed-right) {
  height: 100% !important;
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
