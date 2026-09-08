<template>
  <div>
    <el-upload
        :action="uploadUrl"
        :before-upload="beforeUpload"
        :file-list="fileList"
        :headers="uploadHeader"
        :multiple="multiple"
        :on-preview="handlePictureCardPreview"
        :on-remove="handleRemove"
        :on-success="handleSuccess"
        list-type="picture-card"
    >
      <el-icon>
        <Plus/>
      </el-icon>
    </el-upload>
    <el-image-viewer
        v-if="showPreview"
        :initial-index="previewIndex"
        :url-list="urlList"
        show-progress
        @close="showPreview = false"
    />
  </div>
</template>
<script setup>
import { ref, computed } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import api from '@/utils/request-api'
import { useUserInfoStore } from '@/store'
import { storeToRefs } from 'pinia'

const props = defineProps({
  uploadExtraData: {
    type: Object,
    default: () => ({})
  },
  multiple: {
    type: Boolean,
    default: false
  },
  fileList: {
    type: Array,
    default: () => []
  },
  beforeUpload: {
    type: Function,
    default: () => ({})
  }
})
const showPreview = ref(false)
const emit = defineEmits(['on-remove', 'on-success', 'on-handleFail'])

const {token} = storeToRefs(useUserInfoStore())

const previewIndex = ref(0)

// 从外部传入的 fileList 中抽取预览用的 url 列表
const urlList = computed(() => {
  const list = Array.isArray(props.fileList) ? props.fileList : []
  return list.map(
      (f) => f.url || f.response?.url || f.response?.data?.url || ''
  )
})

const handleRemove = (file, fileList) => {
  emit('on-remove', file, fileList)
}

const handlePictureCardPreview = (file) => {
  const clickedUrl =
      file?.url || file?.response?.url || file?.response?.data?.url || ''
  // 计算预览初始索引，若未匹配到则回退到 0
  const idx = urlList.value.findIndex((u) => u === clickedUrl)
  previewIndex.value = idx >= 0 ? idx : 0
  showPreview.value = true
}

const handleSuccess = (response, file, fileList) => {
  if (response?.code === 0) {
    emit('on-success', fileList)
  } else {
    ElMessage.error(response.msg + '（已自动移除违规图片）')
    emit('on-handleFail', file)
  }
}

const uploadUrl = computed(() => {
  return api.common.uploadImg
})

const uploadHeader = computed(() => {
  return {
    token: token.value
  }
})
</script>

<style lang="scss" scoped>
.img-container {
  text-align: center;

  img {
    width: auto; /* 保持原始宽度 */
    height: auto; /* 保持原始高度 */
    max-width: 100%; /* 确保不超出容器 */
    vertical-align: bottom;
    object-fit: contain;
  }
}

:deep(.my-dialog) {
  min-width: 500px;
}
</style>
