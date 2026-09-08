<template>
  <my-dialog v-model="dialogVisible" :title="title" :width="width" append-to-body @close="close" @submit="submit">
    <el-form ref="packageEditRef" :model="dataForm" :rules="dataRule" label-width="120px">
      <el-form-item label="版本号" prop="versionNum">
        <el-input v-model="dataForm.versionNum" placeholder="版本号"></el-input>
      </el-form-item>
      <el-form-item label="版本值" prop="version">
        <el-input v-model="dataForm.version" placeholder="版本值"></el-input>
      </el-form-item>
      <el-form-item label="上传更新文件" prop="cosKey">
        <!--        <el-upload class="upload-demo" :action="uploadUrl" :data="uploadData" :headers="uploadHeader"-->
        <!--                   :before-upload="beforeUpload" :on-preview="handlePreview" :on-remove="handleRemove"-->
        <!--                   :before-remove="beforeRemove" multiple :limit="10" :on-exceed="handleExceed"-->
        <!--                   :file-list="dataForm.fileList" :on-success="handleSuccess">-->
        <!--          <el-button size="small" type="primary">点击上传</el-button>-->
        <!--        </el-upload>-->
        <el-upload :before-upload="beforeUpload" :file-list="dataForm.fileList" :http-request="handleUpload" :limit="1"
                   :on-exceed="handleExceed" :on-remove="handleRemove" :on-success="handleSuccess" action="#">
          <el-button size="small" type="primary">点击上传</el-button>
        </el-upload>
        <!-- 显示上传进度 -->
        <el-progress
            v-if="progressVisible"
            :percentage="progressPercent"
            status="success"
        ></el-progress>
      </el-form-item>
    </el-form>
  </my-dialog>
</template>
<script setup>
import { ref, reactive } from 'vue'
import myDialog from '@/components/commonComponent/myDialog.vue'
import COS from 'cos-js-sdk-v5'
import api from '@/utils/request-api'

const emit = defineEmits(['is-ok', 'close'])

const dialogVisible = ref(false)
const title = ref('编辑')
const width = ref('600px')

const dataForm = reactive({
  id: null,
  versionNum: '',
  updateInfo: '无',
  updateTime: null,
  updateType: 1,
  isFront: 2,
  isPreserve: 1,
  fileList: [],
  cosKey: '',
  version: null,
  parentId: null
})

const dataRule = {
  versionNum: [{required: true, message: '版本名称不能为空', trigger: ['blur', 'change']}],
  version: {required: true, message: '版本编号不能为空', trigger: ['blur', 'change']},
  cosKey: {required: true, message: '请上传文件', trigger: ['blur', 'change']}
}

const files = ref([])
const beforUploaDelete = ref(false)
const progressVisible = ref(false)
const progressPercent = ref(0)

const packageEditRef = ref()

const init = async (row) => {
  dialogVisible.value = true
  if (row?.parentId) dataForm.parentId = row.parentId
  if (row?.id) {
    const res = await api.clientupdate.info({id: row?.id})
    if (res && res.code === 0) {
      res.data?.fileList?.forEach((item) => {
        item.name = item.fileName
        item.fileId = item.id
        files.value.push({fileId: item.id, fileName: item.fileName})
      })
      Object.assign(dataForm, res.data)
    }
  } else {
    // 新增
  }
}

const handleRemove = () => {
  files.value = []
}

const handlePreview = (file) => {
}

const handleExceed = (filesParam, fileListParam) => {
  ElMessage.warning(`当前限制选择 1 个文件，本次选择了 ${filesParam.length} 个文件，共选择了 ${filesParam.length + fileListParam.length} 个文件`)
}

const handleSuccess = (response) => {
  files.value.push(response)
  dataForm.cosKey = response.fileName
  dataForm.fileMd5 = response.fileMd5
}

const beforeRemove = (file, fileList) => {
}

const handleUpload = async (options) => {
  const {file} = options
  const res = await api.tencentCos.cosPublicReadTempToken({})
  if (res?.code === 0) {
    const credential = res.data
    const startTime = Math.floor(Date.now() / 1000)
    const expiredTime = startTime + 120
    const cos = new COS({
      getAuthorization: (options, callback) => {
        callback({
          TmpSecretId: credential.tempSecretId,
          TmpSecretKey: credential.tempSecretKey,
          SecurityToken: credential.token,
          StartTime: startTime,
          ExpiredTime: expiredTime
        })
      }
    })

    const fileName = 'updatePackage/' + dataForm.versionNum + '-' + getUniqueId() + '.' + getFileExtension(file.name)

    progressVisible.value = true
    progressPercent.value = 0

    cos.putObject(
        {
          Bucket: credential.bucketName,
          Region: credential.region,
          Key: fileName,
          Body: file,
          onProgress: (progressData) => {
            progressPercent.value = Math.floor(progressData.percent * 100)
          }
        },
        (err, data) => {
          if (err) {
            console.log('上传cos失败:', err)
            ElMessage.error('文件上传失败')
            options.onError(err)
          } else {
            const str = data.ETag
            const result = str.replace(/"/g, '')
            options.onSuccess({fileId: '', fileName: fileName, fileMd5: result})
          }
          progressVisible.value = false
        }
    )
  }
}

const getUniqueId = () => {
  const timestamp = Date.now().toString(36)
  const random = Math.random().toString(36).substring(2)
  return `${timestamp}-${random}`
}

const getFileExtension = (filename) => {
  const parts = filename.split('.')
  if (parts.length === 1 || (parts[0] === '' && parts.length === 2)) {
    return ''
  }
  return parts.pop()
}

const beforeUpload = (file) => {
  if (files.value.length > 0) {
    for (let i = 0; i < files.value.length; i++) {
      if (files.value[i].fileName == file.name) {
        ElMessage.error(file.name + ',文件已存在')
        beforUploaDelete.value = true
        return false
      }
    }
  }
  return true
}

const submit = async () => {
  const flag = await packageEditRef.value.validate()
  if (flag) {
    const data = JSON.parse(JSON.stringify(dataForm))
    const res = data?.id ? await api.clientupdate.update(data) : await api.clientupdate.save(data)
    if (res && res.code === 0) {
      ElMessage.success(res.msg)
      dialogVisible.value = false
      emit('is-ok')
    }
  }
}

const close = () => {
  dialogVisible.value = false
  emit('close')
}

defineExpose({init})
</script>

<style lang="less" scoped>

</style>