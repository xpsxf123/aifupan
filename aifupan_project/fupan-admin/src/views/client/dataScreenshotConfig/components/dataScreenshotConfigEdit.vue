<template>
  <el-dialog
    v-model="visible"
    :close-on-click-modal="false"
    :title="!dataForm.id ? '新增' : '修改'"
    :width="550"
  >
    <el-form
      ref="dataFormRef"
      :model="dataForm"
      :rules="dataRule"
      label-width="80px"
    >
      <el-form-item label="code" prop="screenshotCode">
        <el-input
          v-model="dataForm.screenshotCode"
          :disabled="isAdd"
          placeholder="code"
        />
      </el-form-item>
      <el-form-item label="标题" prop="title">
        <el-input v-model="dataForm.title" placeholder="标题" />
      </el-form-item>
      <el-form-item label="排序" prop="sort">
        <el-input v-model="dataForm.sort" placeholder="排序" />
      </el-form-item>
      <el-form-item label="备注" prop="remarks">
        <el-input
          v-model="dataForm.remarks"
          placeholder="备注"
          rows="2"
          type="textarea"
        />
      </el-form-item>
      <el-form-item label="示例图片" prop="exampleList">
        <upload-img
          :fileList="dataForm.exampleList"
          :limit="1"
          @imgChange="onImgChange"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" @click="dataFormSubmit">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, nextTick } from 'vue'
import uploadImg from '@/components/commonComponent/uploadImg.vue'
import api from '@/utils/request-api'

const emit = defineEmits(['refreshDataList'])

const visible = ref(false)
const isAdd = ref(false)

const dataForm = reactive({
  id: 0,
  screenshotCode: '',
  title: '',
  remarks: '',
  sort: '',
  sourceType: 0,
  example: '',
  exampleList: [],
})

const dataRule = {
  screenshotCode: [
    { required: true, message: 'code不能为空', trigger: 'blur' },
  ],
  title: [{ required: true, message: '标题不能为空', trigger: 'blur' }],
  sort: [{ required: true, message: '排序不能为空', trigger: 'blur' }],
  exampleList: [
    {
      required: true,
      message: '示例图片不能为空',
      trigger: ['blur', 'change'],
    },
  ],
}

const dataFormRef = ref()

const onImgChange = (fileList) => {
  dataForm.exampleList = fileList
}

const init = async (id) => {
  isAdd.value = !!id
  dataForm.id = id || 0
  visible.value = true
  await nextTick()
  dataFormRef.value?.resetFields()
  if (dataForm.id) {
    const data = await api.dataScreenshotConfig.info(
      { id: dataForm.id },
      { showLoading: true }
    )
    if (data && data.code === 0) {
      Object.assign(dataForm, data.data)
    }
  }
}

const dataFormSubmit = async () => {
  const valid = await dataFormRef.value.validate()
  if (valid) {
    const requestData = JSON.parse(JSON.stringify(dataForm))
    requestData.example = dataForm.exampleList[0]?.name

    if (dataForm.id) {
      const res = await api.dataScreenshotConfig.update(requestData)
      if (res && res.code === 0) {
        ElMessage.success(res.msg)
        visible.value = false
        emit('refreshDataList')
      } else {
        ElMessage.error(res.msg)
      }
    } else {
      requestData.id = ''
      const res = await api.dataScreenshotConfig.save(requestData)
      if (res && res.code === 0) {
        ElMessage.success(res.msg)
        visible.value = false
        emit('refreshDataList')
      } else {
        ElMessage.error(res.msg)
      }
    }
  }
}

defineExpose({ init })
</script>
