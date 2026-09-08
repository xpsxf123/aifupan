<template>
  <div class="record-analysis">
    <div class="record-analysis__buttons">
      <el-button
        :class="{
          'record-analysis__button--active': audioAnalysis,
          'record-analysis__button--hover': !audioAnalysis,
        }"
        @click="handleAnchorVideo"
      >
        录制分析
      </el-button>
      <el-button
        :class="{
          'record-analysis__button--active': fileVideo,
          'record-analysis__button--hover': !fileVideo,
        }"
        @click="handleFileVideo"
      >
        文件上传分析
      </el-button>
      <el-button
        :class="{
          'record-analysis__button--active': contrastAnalysisVisible,
          'record-analysis__button--hover': !contrastAnalysisVisible,
        }"
        @click="handleContrast"
      >
        对比分析
      </el-button>
    </div>
    <div v-if="audioAnalysis" class="record-analysis__content">
      <AudioAnalysis :isMobile="isMobile" :parent-id="videoId" />
    </div>
    <div v-if="fileVideo" class="record-analysis__content">
      <FileVideo :isMobile="isMobile" />
    </div>
    <div v-if="contrastAnalysisVisible" class="record-analysis__content">
      <SyncContrastAnalysis :isMobile="isMobile" :parent-id="videoId" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import FileVideo from './fileVideo.vue'
import AudioAnalysis from './audioAnalysis.vue'
import SyncContrastAnalysis from './syncContrastAnalysis.vue'
import { useSystemInfoStore } from '@/store'
import { storeToRefs } from 'pinia'
const route = useRoute()
const { isMobile } = storeToRefs(useSystemInfoStore())
const videoId = ref(null)
const fileVideo = ref(false)
const audioAnalysis = ref(true)
const contrastAnalysisVisible = ref(false)

const handleAnchorVideo = () => {
  audioAnalysis.value = true
  fileVideo.value = false
  contrastAnalysisVisible.value = false
}

const handleFileVideo = () => {
  audioAnalysis.value = false
  fileVideo.value = true
  contrastAnalysisVisible.value = false
}

const handleContrast = () => {
  audioAnalysis.value = false
  fileVideo.value = false
  contrastAnalysisVisible.value = true
}

onMounted(() => {
  if (route.query.videoId !== undefined) {
    videoId.value = route.query.videoId
  }
})
</script>

<style lang="scss" scoped>
.record-analysis {
  padding: 15px;
  &__buttons {
    display: flex;
    gap: 8px;
  }

  &__button {
    &--active {
      color: white;
      transition: all 0.3s ease;
      border: 1px solid white;
      background-color: #409eff;
      width: 100px;
    }

    &--hover:hover {
      transform: translateY(-2px);
    }
  }

  &__content {
    margin-top: 16px;
  }
}
:deep(.el-table__fixed-right) {
  height: 100% !important;
}
</style>
