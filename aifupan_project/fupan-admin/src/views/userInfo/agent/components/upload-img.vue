<script setup>
import { ref, computed } from 'vue'
import { storeToRefs } from 'pinia'
import { useUserInfoStore } from '@/store/modules/user'
import api from '@/utils/request-api'

const props = defineProps({
  fileList: {
    type: Array,
    default: () => [],
  },
  showFileSize: {
    type: Boolean,
    default: true,
  },
  VALID_WIDTHS: {
    type: Array,
    default: () => [],
  },
  MAX_FILE_SIZE: {
    type: Number,
    default: 2 * 1024 * 1024,
  },
  FILE_TYPE: {
    type: Array,
    default: () => ['image/jpg', 'image/png', 'image/jpeg'],
  },
  uploadExtraData: {
    type: Object,
    default: () => ({}),
  },
  tipMsg: {
    type: String,
  },
  hideDelete: {
    type: Boolean,
    default: false,
  },
  beforeRemove: {
    type: Function,
    default: () => () => true,
  },
})

const emit = defineEmits(['on-remove', 'on-success'])

const { token } = storeToRefs(useUserInfoStore())

const dialogVisible = ref(false)
const fileSize = ref('')
const showImgUrl = ref('')

const uploadHeader = computed(() => {
  return {
    token: token.value,
  }
})

const uploadUrl = computed(() => {
  return api.common.uploadImg
})

const showLimitImageWidth = computed(() => {
  if (props.VALID_WIDTHS && props.VALID_WIDTHS.length > 0) {
    return props.VALID_WIDTHS.join('/')
  }
})

const showLimitFileType = computed(() => {
  if (props.FILE_TYPE && props.FILE_TYPE.length > 0) {
    const newArr = props.FILE_TYPE.map((item) => {
      return item.replace(/^image\//, '')
    })
    return newArr.join('/')
  }
})

const handleRemove = (file, fileList) => {
  fileSize.value = ''
  emit('on-remove', file, fileList)
}

const handlePictureCardPreview = (file) => {
  showImgUrl.value = file.url
  dialogVisible.value = true
}

const handleAvatarSuccess = (response, file, fileList) => {
  fileSize.value = (file.size / 1024).toFixed(2)
  emit('on-success', response, file, fileList)
}

const beforeAvatarUpload = (file) => {
  return new Promise((resolve, reject) => {
    if (!props.FILE_TYPE.includes(file.type)) {
      ElMessage.error(`只能上传 ${showLimitFileType.value} 格式的图片`)
      return reject(false)
    }
    if (file.size > props.MAX_FILE_SIZE) {
      ElMessage.error(
        `图片大小不能超过 ${(props.MAX_FILE_SIZE / 1024 / 1024).toFixed(2)}MB`
      )
      return reject(false)
    }
    const reader = new FileReader()
    reader.onload = (e) => {
      const img = new Image()
      img.onload = () => {
        const { width } = img
        if (
          props.VALID_WIDTHS &&
          props.VALID_WIDTHS.length &&
          !props.VALID_WIDTHS.includes(width)
        ) {
          ElMessage.error(`图片宽度只能是${showLimitImageWidth.value}px`)
          return reject(false)
        }
        resolve(true)
      }
      img.src = e.target.result
    }
    reader.readAsDataURL(file)
  })
}

const clearFileSize = () => {
  fileSize.value = ''
}

defineExpose({
  clearFileSize,
})
</script>

<template>
  <div class="upload-img-container" :class="{ 'no-delete': hideDelete }">
    <el-upload
      :class="{ hide_upload: fileList.length >= 1 }"
      class="avatar-uploader"
      :action="uploadUrl"
      :headers="uploadHeader"
      :file-list="fileList"
      :limit="1"
      :data="uploadExtraData"
      :before-remove="beforeRemove"
      list-type="picture-card"
      :on-remove="handleRemove"
      :on-success="handleAvatarSuccess"
      :on-preview="handlePictureCardPreview"
      :before-upload="beforeAvatarUpload"
    >
      <el-icon class="avatar-uploader-icon"><Plus /></el-icon>
      <el-dialog v-model="dialogVisible" append-to-body title="查看图片">
        <img width="100%" :src="showImgUrl" alt="" />
      </el-dialog>
    </el-upload>
    <span></span>
    <span v-show="fileSize && showFileSize"
      >已添加图片, 图片大小 {{ fileSize }} KB</span
    >
    <span v-show="!fileSize && tipMsg">{{ tipMsg }}</span>
  </div>
</template>

<style scoped lang="scss">
.hide_upload {
  :deep(.el-upload--picture-card) {
    display: none;
  }
}

.upload-img-container {
  & > span {
    line-height: 20px;
  }
}

.no-delete :deep(.el-upload-list__item-actions .el-upload-list__item-delete) {
  display: none !important;
}
</style>
