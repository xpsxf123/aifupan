<template>
  <div>
    <div v-if="anchorVideo">
      <AnchorVideo
          @hideContent="handleHideContent"
      ></AnchorVideo>
    </div>
    <div v-if="fileVideo">
      <FileUpload></FileUpload>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import AnchorVideo from './components/anchorVideo.vue'
import FileUpload from './components/fileUpload.vue'

const anchorVideo = ref(true)
const fileVideo = ref(false)
const audioAnalysis = ref(false)

const handleHideContent = () => {
  anchorVideo.value = false
  audioAnalysis.value = true
}
</script>
<style lang="less" scoped>
.mod-config {
  padding: 15px;
}

:deep(.el-table__fixed-right) {
  height: 100% !important;
}
</style>
