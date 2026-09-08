<template>
  <div class="mod-config">
    <el-form :inline="true" :model="dataForm">
      <el-form-item>
        <el-input
            v-model="dataForm.anchorName"
            clearable
            placeholder="输入主播名称搜索"
            style="width: 150px"
        ></el-input>
      </el-form-item>

      <el-form-item>
        <el-select
            v-model="dataForm.platform"
            clearable
            placeholder="选择平台"
            style="width: 150px"
        >
          <el-option
              v-for="option in options"
              :key="`platform-${option.value}`"
              :label="option.label"
              :value="option.value"
          >
          </el-option>
        </el-select>
      </el-form-item>

      <el-form-item>
        <el-select
            v-model="dataForm.isNon"
            clearable
            placeholder="是否添白名单"
            style="width: 150px"
        >
          <el-option
              v-for="option in optionsss"
              :key="`whitelist-${option.value}`"
              :label="option.label"
              :value="option.value"
          >
          </el-option>
        </el-select>
      </el-form-item>

      <el-form-item>
        <el-button
            class="operate-button"
            icon="Search"
            plain
            type="primary"
            @click="search"
        >查询
        </el-button>
      </el-form-item>
    </el-form>

    <el-table
        v-loading="dataListLoading"
        :data="dataList"
        :element-loading-spinner="customSvg"
        background
        border
        header-row-class-name="my-header-row"
        size="default"
        stripe
        style="width: 100%"
    >
      <el-table-column
          :min-width="200"
          align="center"
          header-align="center"
          label="主播名称"
          prop="anchorName"
          show-overflow-tooltip
      >
      </el-table-column>

      <el-table-column
          align="center"
          header-align="center"
          label="主播主页地址"
          min-width="110px"
          prop="homeUrl"
      >
        <template #default="{ row }">
          <el-link type="primary">
            <a
                :href="row.homeUrl"
                class="look-anchor-url"
                rel="noopener noreferrer"
                target="_blank"
            >点击查看</a
            ></el-link
          >
        </template>
      </el-table-column>

      <el-table-column
          :min-width="100"
          align="center"
          header-align="center"
          label="平台类型"
          prop="platform"
      >
        <template #default="scope">
          <span v-if="scope.row.platform === 0">抖音</span>
          <span v-else-if="scope.row.platform === 1">快手</span>
          <span v-else>视频号</span>
        </template>
      </el-table-column>

      <el-table-column
          align="center"
          header-align="center"
          label="白名单人数"
          prop="userCounts"
          width="110px"
      >
        <template #default="{ row }">
          <span
              v-if="row?.userCounts !== null && row?.userCounts !== undefined"
          >
            {{ row.userCounts }}人
          </span>
        </template>
      </el-table-column>

      <el-table-column
          :width="100"
          align="center"
          header-align="center"
          label="关联统计"
          prop="ucounts"
      >
        <template #default="{ row }">
          <span v-if="row?.ucounts !== null && row?.ucounts !== undefined">
            {{ row.ucounts }}次
          </span>
        </template>
      </el-table-column>

      <el-table-column
          :width="100"
          align="center"
          header-align="center"
          label="录制统计"
          prop="videCounts"
      >
        <template #default="{ row }">
          <span
              v-if="row?.videCounts !== null && row?.videCounts !== undefined"
          >
            {{ row.videCounts }}次
          </span>
        </template>
      </el-table-column>

      <el-table-column
          align="center"
          fixed="right"
          header-align="center"
          label="操作"
          width="200"
      >
        <template #default="scope">
          <el-button
              class="operate-button"
              size="small"
              type="primary"
              @click="anchorInfoDialog(scope.row.secUid)"
          >
            详情信息
          </el-button>
          <el-button
              class="operate-button"
              size="small"
              type="success"
              @click="anchorWhiteDialog(scope.row.secUid)"
          >
            白名单
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

    <add-or-update
        v-if="addOrUpdateVisible"
        ref="addOrUpdate"
        :isMobile="isMobile"
        @refreshDataList="getDataList"
    ></add-or-update>
    <UserAnchorUrl
        v-if="userAnchorUrlVisible"
        ref="userAnchorUr"
        :isMobile="isMobile"
        @refreshDataList="getDataList"
    />
    <AddAnchouUrl
        v-if="addAnchouUrlVisible"
        ref="addAnchouUrl"
        :isMobile="isMobile"
        @refreshDataList="getDataList"
    />
    <AnchorUrlInfo
        v-if="anchorInfoDialogVisible"
        ref="anchorUrlInfo"
        :isMobile="isMobile"
    />
    <AnchorWhiteUser
        v-if="anchorWhiteDialogVisible"
        ref="anchorWhiteUser"
        :isMobile="isMobile"
        @refreshDataList="getDataList"
    />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import AddOrUpdate from './components/add-or-update.vue'
import UserAnchorUrl from './components/user-anchor-url.vue'
import AddAnchouUrl from './components/addAnchouUrl.vue'
import AnchorUrlInfo from './components/anchorUrlInfo.vue'
import AnchorWhiteUser from './components/anchorUrlWhite.vue'
import { customSvg } from '@/utils/icon.js'
import { useSystemInfoStore } from '@/store'
import { storeToRefs } from 'pinia'
import api from '@/utils/request-api'

const route = useRoute()
const {isMobile} = storeToRefs(useSystemInfoStore())

const anchorInfoDialogVisible = ref(false)
const anchorWhiteDialogVisible = ref(false)
const searchDataFormVisible = ref(true)
const dataList = ref([])
const pageIndex = ref(1)
const pageSize = ref(10)
const totalCount = ref(0)
const dataListLoading = ref(false)
const userAnchorUrlVisible = ref(false)
const addAnchouUrlVisible = ref(false)
const addOrUpdateVisible = ref(false)

const addOrUpdate = ref(null)
const userAnchorUr = ref(null)
const addAnchouUrl = ref(null)
const anchorUrlInfo = ref(null)
const anchorWhiteUser = ref(null)

const dataForm = reactive({
  userId: null,
  username: null,
  anchorName: null,
  platform: null,
  isNon: null
})

const options = ref([
  {value: 0, label: '抖音'},
  {value: 1, label: '快手'},
  {value: 2, label: '视频号'}
])

const optionsss = ref([
  {value: 0, label: '是'},
  {value: 1, label: '否'}
])

const anchorWhiteDialog = async (id) => {
  anchorWhiteDialogVisible.value = true
  await nextTick()
  anchorWhiteUser.value.init(id)
}

const anchorInfoDialog = async (id) => {
  anchorInfoDialogVisible.value = true
  await nextTick()
  anchorUrlInfo.value.init(id)
}

const getDataList = async (id) => {
  dataListLoading.value = true
  dataForm.userId = id
  dataForm.page = pageIndex.value
  dataForm.limit = pageSize.value

  const res = await api.anchorurl.seletAnchorUrl(dataForm)
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
    Object.assign(dataForm, {
      userId: null,
      username: null,
      anchorName: null,
      platform: null,
      isNon: null
    })
  }
  getDataList()
}

const search = () => {
  if (
      dataForm.platform === null &&
      dataForm.anchorName === null &&
      dataForm.isNon === null
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
})
</script>
<style lang="scss" scoped>
.mod-config {
  padding: 15px;
}

// :deep(.el-table__fixed-right) {
//   height: 100% !important;
// }

// .operate-button:hover {
//   transform: translateY(-2px);
//   color: rgb(0, 255, 242);
// }

// .look-anchor-url {
//   color: #007bff;
//   text-decoration: none;
//   display: inline-block;
//   border-radius: 5px;
//   background-color: #fffafa;
//   border: 1px solid #fffafa;
//   transition: all 0.3s ease;
//   box-shadow: 2px 2px 5px rgba(0, 0, 0, 0.1);
// }

// .look-anchor-url:hover {
//   color: white;
//   background-color: #0056b3;
//   transform: translateY(-2px);
// }

// .look-anchor-url:active {
//   background-color: #004085;
//   box-shadow: inset 2px 2px 5px rgba(0, 0, 0, 0.5);
//   transform: translateY(0);
// }
</style>
