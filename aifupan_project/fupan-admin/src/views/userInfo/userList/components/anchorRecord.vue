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
          :key="'anchorName'"
          align="center"
          header-align="center"
          label="主播名称"
          min-width="200"
          prop="anchorName"
          show-overflow-tooltip
      />

      <el-table-column
          :key="'homeUrl'"
          align="center"
          header-align="center"
          label="主播主页地址"
          min-width="120px"
          prop="homeUrl"
          show-overflow-tooltip
      >
        <template #default="{ row }">
          <a :href="row.homeUrl" class="look-anchor-url" rel="noopener noreferrer" target="_blank">点击查看</a>
        </template>
      </el-table-column>

      <el-table-column
          :key="'accountType'"
          align="center"
          header-align="center"
          label="是否为自有帐号"
          min-width="140px"
          prop="accountType"
          show-overflow-tooltip
      >
        <template #default="{ row }">
          <span>{{ row.accountType === 0 ? '是' : '否' }}</span>
        </template>
      </el-table-column>

      <el-table-column
          :key="'platform'"
          :min-width="100"
          align="center"
          header-align="center"
          label="平台类型"
          prop="platform"
      >
        <template #default="{ row }">
          <span v-if="row.platform === 0">抖音</span>
          <span v-else-if="row.platform === 1">快手</span>
          <span v-else>视频号</span>
        </template>
      </el-table-column>

      <el-table-column
          :key="'userCounts'"
          align="center"
          header-align="center"
          label="白名单人数"
          min-width="120px"
          prop="userCounts"
          show-overflow-tooltip
      />

      <el-table-column
          :key="'ucounts'"
          :min-width="100"
          align="center"
          header-align="center"
          label="关联统计"
          prop="ucounts"
      />

      <el-table-column
          :key="'videCounts'"
          :min-width="100"
          align="center"
          header-align="center"
          label="录制统计"
          prop="videCounts"
      />

      <el-table-column
          :key="'anchorSituation'"
          align="center"
          header-align="center"
          label="主播账号情况描述"
          min-width="150px"
          prop="anchorSituation"
          show-overflow-tooltip
      />
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
    />
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { customSvg } from '@/utils/icon.js'
import api from '@/utils/request-api'

const props = defineProps({
  userId: {
    type: String,
    required: true
  }
})

const searchDataFormVisible = ref(true)
const dataForm = ref({
  anchorName: null,
  userId: '',
  page: 1,
  limit: 5
})
const dataList = ref([])
const pageIndex = ref(1)
const pageSize = ref(5)
const totalCount = ref(0)
const dataListLoading = ref(false)

const options = ref([
  {value: 0, label: '抖音'},
  {value: 1, label: '快手'},
  {value: 2, label: '视频号'}
])

const optionsss = ref([
  {value: 0, label: '是'},
  {value: 1, label: '否'}
])

const getDataList = async () => {
  dataListLoading.value = true
  dataForm.value.userId = props.userId
  dataForm.value.page = pageIndex.value
  dataForm.value.limit = pageSize.value

  try {
    const res = await api.anchorurl.selectAnchorByUserId(dataForm.value)
    if (res && res.code === 0 && res.data != null) {
      dataList.value = res.data.list
      totalCount.value = res.data.totalCount
    } else {
      dataList.value = []
      totalCount.value = 0
    }
  } catch (error) {
    console.error('获取主播记录失败:', error)
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
    dataForm.value = {
      anchorName: null,
      userId: props.userId,
      page: val,
      limit: pageSize.value
    }
  }
  getDataList()
}

watch(
    () => props.userId,
    () => {
      getDataList()
    }
)

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

// .operate-button{
//   font-size: 12px;
//   color: white;
//   transition: all 0.3s ease;
//   border: 1px solid white;
//   background-color: #0077FF;
// }

.operate-button:hover {
  // scale: 1.06;
  transform: translateY(-2px);
  color: rgb(0, 255, 242);
}

.look-anchor-url {
  color: #007BFF;
  text-decoration: none;
  display: inline-block;
  border-radius: 5px;
  background-color: #fffafa;
  border: 1px solid #fffafa;
  transition: all 0.3s ease;
  box-shadow: 2px 2px 5px rgba(0, 0, 0, 0.1);
}

.look-anchor-url:hover {
  color: white;
  background-color: #0056b3;
  transform: translateY(-2px);
}

.look-anchor-url:active {
  background-color: #004085;
  box-shadow: inset 2px 2px 5px rgba(0, 0, 0, 0.5);
  transform: translateY(0);
}

</style>