<template>
  <div>
    <el-upload
        :action="uploadUrl"
        :class="fileListInner.length >= limit ? 'hide_upload' : ''"
        :data="uploadData"
        :file-list="fileListInner"
        :headers="uploadHeader"
        :limit="limit"
        :on-preview="handlePictureCardPreview"
        :on-remove="uploadRemoveHandle"
        :on-success="uploadSuccessHandle"
        list-type="picture-card"
    >
      <el-icon class="avatar-uploader-icon">
        <Plus/>
      </el-icon>
    </el-upload>

    <el-image-viewer
        v-if="imageViewerVisible"
        :hide-on-click-modal="true"
        :initial-index="0"
        :teleported="true"
        :url-list="imageList"
        @close="closeImageViewer"
    />
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useUserInfoStore } from '@/store'
import { storeToRefs } from 'pinia'
import { Plus } from '@element-plus/icons-vue'
import api from '@/utils/request-api'

const emit = defineEmits(['imgChange'])
const props = defineProps({
  fileList: Array,
  limit: {
    type: Number,
    default: 9
  },
  flag: {
    type: Number,
    default: 0
  }
})

const userInfoStore = useUserInfoStore()
const {token} = storeToRefs(userInfoStore)

const imageViewerVisible = ref(false)
const imageList = ref([])
const fileListInner = ref([])
console.log('hello world')

watch(
    () => props.fileList,
    (newVal) => {
      console.log('newVal', newVal)

      fileListInner.value = newVal
    }
)

/**
 * 图片上传成功的回调
 * @param {Object} response - 上传响应数据
 */
const uploadSuccessHandle = (response) => {
  if (response?.data) {
    if (!fileListInner.value) fileListInner.value = []
    fileListInner.value.push({
      id: response.data.id,
      name: response.data.name,
      url: response.data.url,
      resourceId: response.data.resourceId,
      sort: response.data.sort
    })
    console.log(fileListInner.value)
    emit('imgChange', fileListInner.value)
  }
}

/**
 * 删除图片回调
 * @param {Object} file - 删除的文件对象
 * @param {Array} fileList - 当前文件列表
 */
const uploadRemoveHandle = (file, fileList) => {
  console.log('fileList', fileList)
  fileListInner.value = fileList
  emit('imgChange', fileListInner.value)
}

/**
 * 放大图片
 * @param {Object} file - 预览的文件对象
 */
const handlePictureCardPreview = (file) => {
  imageList.value = [file.url]
  imageViewerVisible.value = true
}

/**
 * 关闭图片预览
 */
const closeImageViewer = () => {
  imageViewerVisible.value = false
}

// 上传地址
const uploadUrl = computed(() => {
  return api.common.uploadImg
})

// 上传带的参数
const uploadData = computed(() => {
  let data = {}
  data.flag = props.flag
  if (fileListInner.value && fileListInner.value.length > 0) {
    data.sort = fileListInner.value[fileListInner.value.length - 1].sort + 1
  } else {
    data.sort = 0
  }
  return data
})

// 上传的请求头
const uploadHeader = computed(() => {
  return {
    token: token.value
  }
})
</script>

<style lang="less" scoped>
.hide_upload {
  :deep(.el-upload--picture-card) {
    display: none;
  }
}
</style>
