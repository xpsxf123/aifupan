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

      <el-table-column align="center" header-align="center" label="主播名称" prop="anchorName" width="300">
      </el-table-column>

      <el-table-column align="center" header-align="center" label="主播主页地址" min-width="120px" prop="homeUrl"
                       show-overflow-tooltip>
        <template #default="{ row }">
          <a :href="row.homeUrl" class="look-anchor-url" rel="noopener noreferrer" target="_blank">点击查看</a>
        </template>
      </el-table-column>

      <el-table-column align="center" header-align="center" label="是否为自有帐号" min-width="140px" prop="accountType"
                       show-overflow-tooltip>
        <template #default="{row}">
          <span>{{ row.accountType === 0 ? '是' : '否' }}</span>
        </template>
      </el-table-column>

      <el-table-column align="center" header-align="center" label="平台类型" min-width="90px" prop="platform">
        <template #default="scope">
          <!-- 自定义显示平台类型 -->
          <span v-if="scope.row.platform === 0">抖音</span>
          <span v-else-if="scope.row.platform === 1">快手 </span>
          <span v-else>视频号</span>
        </template>
      </el-table-column>

      <el-table-column align="center" header-align="center" label="白名单人数" min-width="120px" prop="userCounts"
                       show-overflow-tooltip>
      </el-table-column>

      <el-table-column :min-width="100" align="center" header-align="center" label="关联统计" prop="ucounts">
      </el-table-column>

      <el-table-column :min-width="100" align="center" header-align="center" label="录制统计" prop="videCounts">
      </el-table-column>

      <el-table-column align="center" header-align="center" label="主播账号情况描述" min-width="150px"
                       prop="anchorSituation" show-overflow-tooltip>
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
import { ref, reactive, onMounted } from 'vue'
import api from '@/utils/request-api'
import { customSvg } from '@/utils/icon.js'

const props = defineProps({
  userId: {
    type: String,
    required: true
  }
})

const searchDataFormVisible = ref(true)

const dataForm = reactive({
  anchorName: null
})

const dataList = ref([])
const pageIndex = ref(1)
const pageSize = ref(5)
const totalCount = ref(0)
const dataListLoading = ref(false)

const getDataList = async () => {
  dataListLoading.value = true
  dataForm.userId = props.userId
  dataForm.page = pageIndex.value
  dataForm.limit = pageSize.value

  try {
    const res = await api.anchorurl.userAddAnchorRecord(dataForm)
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
      anchorName: null
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