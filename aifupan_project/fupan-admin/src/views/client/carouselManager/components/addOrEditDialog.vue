<template>
  <el-dialog
    :title="formData.id ? '修改' : '新增'"
    v-model="dialogVisible"
    @close="handleClose"
    :width="550"
  >
    <el-form ref="formRef" :model="formData" :rules="rules" label-width="80px">
      <el-form-item label="内容：" prop="content">
        <el-input
          v-model="formData.content"
          placeholder="请输入内容"
        ></el-input>
      </el-form-item>
      <el-form-item label="排序：" prop="sort">
        <el-input v-model="formData.sort" placeholder="请输入内容"></el-input>
      </el-form-item>
      <el-form-item label="轮播图：" prop="fileId">
        <UploadImg
          :showFileSize="false"
          :fileList="fileList"
          @on-success="handleAvatarSuccess"
          @on-remove="handleRemove"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <span class="dialog-footer">
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="handleConfirm">确 定</el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount, nextTick } from 'vue'
import emitter from '@/utils/emitter'
import UploadImg from '@/views/userInfo/agent/components/upload-img.vue'
import api from '@/utils/request-api'
import { useUserInfoStore } from '@/store'
import { storeToRefs } from 'pinia'

const dialogVisible = ref(false)
const fileList = ref([])
const formRef = ref(null)
const emit = defineEmits(['get-data-list'])

const { loginResultData } = storeToRefs(useUserInfoStore())

const formData = reactive({
  id: '',
  userId: '',
  imgStatus: 0, //默认0 启用，1停止展示
  content: '',
  fileId: '',
  createDate: '',
  updateDate: '',
  isDeleted: 0,
  name: '',
  resourceId: '',
  url: '',
  sort: '',
})

const rules = {
  content: [{ required: true, message: '请输入内容', trigger: 'blur' }],
  sort: [
    { required: true, message: '请输入排序', trigger: 'blur' },
    { pattern: /^[0-9]*$/, message: '请输入数字' },
  ],
  fileId: [{ required: true, message: '请上传轮播图' }],
}

const openDialog = async (row) => {
  dialogVisible.value = true
  formData.userId = loginResultData.value?.id ?? ''
  if (row?.id) {
    const res = await api.clientCarouselManager.info(
      { id: row.id },
      { showLoading: true }
    )
    await nextTick()
    if (res.code === 0) {
      Object.assign(formData, res.data)
      fileList.value.push({ url: res.data.url })
    }
  }
}

const handleConfirm = async () => {
  await formRef.value?.validate()
  if (formData.id) {
    const res = await api.clientCarouselManager.update(formData)
    if (res.code === 0) {
      emit('get-data-list')
      ElMessage.success('修改成功')
      dialogVisible.value = false
    }
  } else {
    const res = await api.clientCarouselManager.save(formData)
    if (res.code === 0) {
      emit('get-data-list')
      ElMessage.success('新增成功')
      dialogVisible.value = false
    }
  }
}

const handleAvatarSuccess = (response, file, list) => {
  if (response.code === 0) {
    formData.fileId = response.data.id
    fileList.value = list
    formRef.value?.validateField('fileId')
  } else {
    ElMessage.error(response.msg)
    formData.fileId = ''
    fileList.value = []
  }
}

const handleRemove = (file, list) => {
  removeImgInfo()
}

const removeImgInfo = () => {
  formData.fileId = ''
  fileList.value = []
}

const handleClose = () => {
  formRef.value?.resetFields()
  formData.id = ''
  fileList.value = []
}

onMounted(() => {
  emitter.on('openDialog', openDialog)
})

onBeforeUnmount(() => {
  emitter.off('openDialog', openDialog)
})
</script>

<style lang="less" scoped></style>
