<template>
  <div>
    <el-dialog
      v-model="visible"
      :close-on-click-modal="false"
      class="anchor-video-custom"
      title="分析记录"
      :fullscreen="isMobile"
      width="60%"
      :class="{ 'mobile-dialog-innner-custom': isMobile }"
    >
      <div class="my-div">
        <div style="margin: 0px 0px 30px 10px">
          <span class="analysis-info-title">视频信息</span>
          <div class="achor-analysis-info">
            <div style="flex: 2">
              视频名称：
              <span class="anchor-analysis-info-item">{{
                analysisData.videoName
              }}</span>
            </div>
            <div style="flex: 1.3">
              主播名称：
              <span class="anchor-analysis-info-item">{{
                analysisData.anchorName
              }}</span>
            </div>
            <div style="flex: 1">
              用户名称：
              <span class="anchor-analysis-info-item">{{
                analysisData.userName
              }}</span>
            </div>
            <div style="flex: 0.8">
              分析状态：
              <span
                v-if="analysisData.analysisStatus === 0"
                class="anchor-analysis-info-item"
                >未分析</span
              >
              <span
                v-if="analysisData.analysisStatus === 1"
                class="anchor-analysis-info-item"
                >分析中</span
              >
              <span
                v-if="analysisData.analysisStatus === 2"
                class="anchor-analysis-info-item"
                >分析完成</span
              >
              <span
                v-if="analysisData.analysisStatus === 3"
                class="anchor-analysis-info-item"
                >分析错误</span
              >
            </div>
          </div>
          <div class="achor-analysis-info">
            <div style="flex: 2">
              录制行业：
              <span class="anchor-analysis-info-item">{{
                analysisData.tradeName
              }}</span>
            </div>
            <div style="flex: 1.3">
              录制时间：
              <span class="anchor-analysis-info-item">{{
                analysisData.createDate
              }}</span>
            </div>
            <div style="flex: 1">
              视频时长：
              <span class="anchor-analysis-info-item">{{
                analysisData.duration
              }}</span>
            </div>
            <div style="flex: 0.8">
              视频大小：
              <span class="anchor-analysis-info-item">
                {{ formatSize(analysisData.vedioSizie) }}
              </span>
            </div>
          </div>
        </div>
        <div style="margin-left: 10px; padding-bottom: 40px">
          <span class="analysis-info-title">分析内容</span>

          <div class="achor-analysis-info">
            <div style="flex: 1">
              平台类型：
              <span
                v-if="analysisData.platformType == 0"
                class="anchor-analysis-info-item"
                >全平台</span
              >
              <span
                v-if="analysisData.platformType == 1"
                class="anchor-analysis-info-item"
                >抖音</span
              >
              <span
                v-if="analysisData.platformType == 2"
                class="anchor-analysis-info-item"
                >快手</span
              >
              <span
                v-if="analysisData.platformType == 3"
                class="anchor-analysis-info-item"
                >视频号</span
              >
            </div>
            <div style="flex: 1">
              分析次数：
              <span class="anchor-analysis-info-item"
                >{{ analysisData.analysisStatusVersion }} 次</span
              >
            </div>
            <div style="flex: 1">
              分析时间：
              <span class="anchor-analysis-info-item">{{
                analysisData.analysisTime
              }}</span>
            </div>
            <div>
              <!-- <el-button size="small" type="primary" class="anchor-analysis-button" @click="lookAnalysisText(analysisData.videoId,analysisData.tradeId)">查看分析文本</el-button> -->
              <el-button
                class="audiao-analysis-button"
                size="small"
                type="primary"
                @click="viewAnalysis(analysisData.videoId)"
                >查看分析内容
              </el-button>
            </div>
          </div>
        </div>
      </div>
    </el-dialog>
    <VideoAnalysis
      v-if="analysisTextVisible"
      ref="videoAnalysisRef"
    ></VideoAnalysis>
    <audioAnalysisOnline
      v-if="analysisOnlineVisible"
      ref="audioAnalysisOnlineRef"
    ></audioAnalysisOnline>
  </div>
</template>

<script setup>
import { ref, reactive, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import myUtils from '@/utils/utils'
import api from '@/utils/request-api'
import VideoAnalysis from './videroAnalysis.vue'
import audioAnalysisOnline from './audioAnalysisOnline.vue'
const props = defineProps({
  isMobile: {
    type: Boolean,
    default: false,
  },
})
const router = useRouter()

const visible = ref(false)
const analysisTextVisible = ref(false)
const analysisOnlineVisible = ref(false)

const analysisData = reactive({
  videoName: '',
  anchorName: '',
  userName: '',
  analysisStatus: '',
  tradeName: '',
  createDate: '',
  duration: '',
  vedioSizie: '',
  platformType: '',
  analysisStatusVersion: '',
  analysisTime: '',
})

const videoAnalysisRef = ref(null)
const audioAnalysisOnlineRef = ref(null)

const viewAnalysis = (videoId) => {
  let fileId = ''
  const url = router.resolve({
    name: 'analysis',
    query: { videoId: videoId, fileId: fileId },
  })
  window.open(url.href, '_blank')
}

const init = (id) => {
  visible.value = true
  getAnalysisDataList(id)
}

const getAnalysisDataList = async (id) => {
  const res = await api.AnchorVideo.videoAnalysisByVideoId({ videoId: id })
  if (res && res.code === 0) {
    Object.assign(analysisData, res.data)
    console.log(analysisData)

    analysisData.duration = myUtils.toformatTime(analysisData.duration * 1000)
  }
}

const formatSize = (sizeInKb) => {
  const sizeInMb = (sizeInKb / 1024 / 1024).toFixed(2)
  return `${sizeInMb} MB`
}

const lookAnalysisText = async (videoId, tradeId) => {
  analysisTextVisible.value = true
  await nextTick()
  if (videoAnalysisRef.value) {
    videoAnalysisRef.value.init(videoId, tradeId)
  }
}

defineExpose({
  init,
})
</script>

<style scoped>
:deep(.anchor-video-custom) {
  min-width: 700px;
}

:deep(.el-dialog__body) {
  padding: 0px;
}

.analysis-info-title {
  color: #2e3742;
  font-size: 20px;
  font-weight: bold;
}

.achor-analysis-info {
  display: flex;
  color: #5c6f86;
  font-size: 15px;
  font-weight: bold;
  margin: 20px 30px 0px 20px;
  padding-bottom: 8px;

  border-bottom: 2px dashed #6f9f9f;
}

.anchor-analysis-info-item {
  font-size: 16px;
  color: #6f9f9f;
  font-weight: bold;
}

.anchor-analysis-button:hover {
  transform: translateY(-2px);
  color: rgb(0, 255, 242);
}
</style>
