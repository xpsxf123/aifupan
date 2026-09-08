<template>
  <el-dialog
      v-model="Infovisible"
      :class="{ 'mobile-dialog-innner-custom': isMobile }"
      :close-on-click-modal="false"
      :fullscreen="isMobile"
      :width="900"
      title="主播详情"
  >
    <div class="my-div">
      <div style="margin: 0px 0px 30px 10px">
        <div class="achor-info">
          <div style="flex: 1.7">
            主播名称：
            <span class="anchor-info-item">{{ anchorInfo.anchorName }}</span>
          </div>
          <div style="display: flex;align-items: center; flex: 1">
            主播主页地址：<!--<a
              :href="anchorInfo.liveUrl"
              class="look-url"
              target="_blank"
          >
            点击查看
          </a>-->
            <el-link :href="anchorInfo.liveUrl" class="look-url" target="_blank" type="primary">点击查看</el-link>
          </div>
          <div style="flex: 1">
            平台类型：
            <span v-if="anchorInfo.platform == 0" class="anchor-info-item"
            >抖音</span
            >
            <span v-else-if="anchorInfo.platform == 1" class="anchor-info-item"
            >快手</span
            >
            <span v-else class="anchor-info-item">视频号</span>
          </div>
        </div>
        <div class="achor-info" style="margin-top: 20px">
          <div style="flex: 1.7">
            是否被收入白名单：
            <span v-if="anchorInfo.userCounts >= 1" class="anchor-info-item"
            >是</span
            >
            <span v-else class="anchor-info-item">否</span>
          </div>
          <div style="flex: 1">
            关联统计：
            <span class="anchor-info-item">{{ anchorInfo.ucounts }}次</span>
          </div>
          <div style="flex: 1">
            录制统计：
            <span class="anchor-info-item"> {{ anchorInfo.videCounts }}次</span>
          </div>
        </div>
      </div>

      <div style="margin: 0 30px; padding-bottom: 20px">
        <el-tabs v-model="activeName" :stretch="true" :stripe="true">
          <el-tab-pane label="关联账号" name="achorInAccount">
            <template #label>
              <span
                  :class="{
                  'custom-tab-label': true,
                  'custom-tab-label-active': activeName == 'achorInAccount',
                }"
              >关联账号</span
              >
            </template>
            <div class="">
              <el-table
                  :data="anchorInAccount"
                  :element-loading-spinner="customSvg"
                  border
                  header-row-class-name="my-header-row"
                  size="default"
                  stripe
              >
                <el-table-column
                    align="center"
                    header-align="center"
                    label="昵称"
                    prop="nickName"
                ></el-table-column>
                <el-table-column
                    align="center"
                    header-align="center"
                    label="账号"
                    prop="username"
                ></el-table-column>
                <el-table-column
                    align="center"
                    header-align="center"
                    label="手机号码"
                    prop="phone"
                ></el-table-column>
                <el-table-column
                    align="center"
                    header-align="center"
                    label="行业"
                    prop="tradeName"
                ></el-table-column>
              </el-table>
            </div>

            <el-pagination
                :current-page="accountPageIndex"
                :page-size="accountPageSize"
                :page-sizes="[10, 20, 50]"
                :total="accountTotalCount"
                background
                layout="total, sizes, prev, pager,next,->, jumper"
                style="text-align: center; margin-top: 10px"
                @size-change="accountSizeChangeHandle"
                @current-change="accountCurrentChangeHandle"
            >
            </el-pagination>
          </el-tab-pane>

          <el-tab-pane label="已录制视频" name="achorInVideo">
            <template #label>
              <span
                  :class="{
                  'custom-tab-label': true,
                  'custom-tab-label-active': activeName == 'achorInVideo',
                }"
              >已录制视频</span
              >
            </template>
            <div class="">
              <el-table
                  :data="anchorInVideo"
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
                    min-width="250"
                    prop="videoName"
                    show-overflow-tooltip
                ></el-table-column>
                <el-table-column
                    :width="100"
                    align="center"
                    header-align="center"
                    label="用户名称"
                    prop="userName"
                    show-overflow-tooltip
                ></el-table-column>
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
                <el-table-column
                    :width="100"
                    align="center"
                    header-align="center"
                    label="视频时长"
                    prop="duration"
                ></el-table-column>
                <el-table-column
                    :width="100"
                    align="center"
                    header-align="center"
                    label="状态"
                    prop="analysisStatus"
                >
                  <template #default="{ row }">
                    <span v-if="row.analysisStatus === 0">未分析</span>
                    <span v-if="row.analysisStatus === 1">分析中</span>
                    <span v-if="row.analysisStatus === 2">分析完成</span>
                    <span v-if="row.analysisStatus === 3">分析错误</span>
                  </template>
                </el-table-column>
                <el-table-column
                    align="center"
                    header-align="center"
                    label="行业"
                    prop="tradeName"
                ></el-table-column>
                <el-table-column
                    align="center"
                    header-align="center"
                    label="录制时间"
                    prop="startTime"
                    width="160"
                ></el-table-column>
              </el-table>
            </div>

            <el-pagination
                :current-page="videoPageIndex"
                :page-size="videoPageSize"
                :page-sizes="[10, 20, 50]"
                :total="videoTotalCount"
                background
                layout="total, sizes, prev, pager,next,->, jumper"
                style="text-align: center; margin-top: 10px"
                @size-change="videoSizeChangeHandle"
                @current-change="videoCurrentChangeHandle"
            >
            </el-pagination>
          </el-tab-pane>
        </el-tabs>
      </div>
    </div>
  </el-dialog>
</template>

<script setup>
import { ref, reactive } from 'vue'
import api from '@/utils/request-api'
import { customSvg } from '@/utils/icon.js'
import myUtils from '@/utils/utils'

const props = defineProps({
  isMobile: {
    type: Boolean,
    default: false
  }
})
const Infovisible = ref(false)
const activeName = ref('achorInAccount')
const anchorInfo = reactive({
  anchorName: '',
  liveUrl: '',
  platform: 0,
  userCounts: 0,
  ucounts: 0,
  videCounts: 0
})

const anchorInAccount = ref([])
const accountPageIndex = ref(1)
const accountPageSize = ref(10)
const accountTotalCount = ref(0)

const anchorInVideo = ref([])
const videoPageIndex = ref(1)
const videoPageSize = ref(10)
const videoTotalCount = ref(0)

const anchorInAccountForm = ref({})
const anchorInVideoForm = ref({})
const secUid = ref(0)

const getAccountList = () => {
  anchorInAccountForm.value.secUid = secUid.value
  anchorInAccountForm.value.page = accountPageIndex.value
  anchorInAccountForm.value.limit = accountPageSize.value
  api.user.selectByuseId(anchorInAccountForm.value).then((res) => {
    if (res && res.code === 0 && res.data != null) {
      anchorInAccount.value = res.data.list
      accountTotalCount.value = res.data.totalCount
    }
  })
}

const getAnchorInfo = (id) => {
  api.anchorurl.infoBySecUid({secUid: id}).then((res) => {
    if (res && res.code === 0 && res.data != null) {
      Object.assign(anchorInfo, res.data)
    }
  })
}

const getVideoBySecUid = () => {
  anchorInVideoForm.value.secUid = secUid.value
  anchorInVideoForm.value.page = videoPageIndex.value
  anchorInVideoForm.value.limit = videoPageSize.value
  api.AnchorVideo.selectVideoBySecUid(anchorInVideoForm.value).then((res) => {
    if (res && res.code === 0 && res.data != null) {
      anchorInVideo.value = res.data.list
      anchorInVideo.value.forEach((item) => {
        item.duration = myUtils.toformatTime(item.duration * 1000)
      })
      videoTotalCount.value = res.data.totalCount
    }
  })
}

const init = (id) => {
  Infovisible.value = true
  secUid.value = id
  getAccountList()
  getAnchorInfo(id)
  getVideoBySecUid()
}

const videoSizeChangeHandle = (val) => {
  videoPageSize.value = val
  videoPageIndex.value = 1
  getVideoBySecUid()
}

const videoCurrentChangeHandle = (val) => {
  videoPageIndex.value = val
  getVideoBySecUid()
}

const accountSizeChangeHandle = (val) => {
  accountPageSize.value = val
  accountPageIndex.value = 1
  getAccountList()
}

const accountCurrentChangeHandle = (val) => {
  accountPageIndex.value = val
  getAccountList()
}

const formatSize = (sizeInKb) => {
  const sizeInMb = (sizeInKb / 1024 / 1024).toFixed(2)
  return `${sizeInMb} MB`
}

defineExpose({
  init
})
</script>

<style scoped>
:deep(.el-dialog__body) {
  padding: 0;
}

.anchor-info-item {
  font-size: 16px;
  color: #6f9f9f;
  font-weight: bold;
}

.achor-info {
  display: flex;
  color: #5c6f86;
  font-size: 15px;
  font-weight: bold;
  margin: 20px 30px 0px 20px;
  padding-bottom: 8px;

  border-bottom: 2px dashed #6f9f9f;
}

.custom-tab-label {
  font-size: 14px;
  color: #2e3742;
  transition: all 0.3s ease;
}

.custom-tab-label:hover {
  font-size: 16px;
  color: rgb(76, 170, 232);
}

.custom-tab-label-active {
  font-size: 16px;
  color: rgb(76, 170, 232);
}

.look-url {
  font-size: 16px;
}


:deep(.el-tabs__active-bar) {
  background-color: rgb(76, 170, 232);
}
</style>
