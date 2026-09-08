<template>
  <el-dialog
      v-model="visible"
      :close-on-click-modal="false"
      :title="!dataForm.id ? '新增' : '修改'"
      :width="800"
  >
    <el-form
        ref="dataFormRef"
        :model="dataForm"
        :rules="dataRule"
        label-width="120px"
    >
      <el-form-item label="版本号" prop="versionNum">
        <el-input v-model="dataForm.versionNum" placeholder="版本号"></el-input>
      </el-form-item>
      <el-form-item label="版本值" prop="version">
        <el-input v-model="dataForm.version" placeholder="版本值"></el-input>
      </el-form-item>
      <el-form-item label="更新类型">
        <el-select v-model="dataForm.isFront" placeholder="请选择">
          <el-option
              v-for="item in isFrontOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
          >
          </el-option>
        </el-select>
      </el-form-item>
      <el-form-item label="是否维护">
        <el-select v-model="dataForm.isPreserve" placeholder="请选择">
          <el-option
              v-for="item in isPreserveOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
          >
          </el-option>
        </el-select>
      </el-form-item>
      <el-form-item label="更新方式">
        <el-select v-model="dataForm.updateType" placeholder="请选择">
          <el-option
              v-for="item in updateTypeOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
          >
          </el-option>
        </el-select>
      </el-form-item>
      <el-form-item label="上传更新文件">
        <!--        <el-upload class="upload-demo" :action="uploadUrl" :data="uploadData" :headers="uploadHeader"-->
        <!--                   :before-upload="beforeUpload" :on-preview="handlePreview" :on-remove="handleRemove"-->
        <!--                   :before-remove="beforeRemove" multiple :limit="10" :on-exceed="handleExceed"-->
        <!--                   :file-list="dataForm.fileList" :on-success="handleSuccess">-->
        <!--          <el-button size="small" type="primary">点击上传</el-button>-->

        <!--        </el-upload>-->

        <el-upload
            :before-upload="beforeUpload"
            :file-list="dataForm.fileList"
            :http-request="handleUpload"
            :limit="1"
            :on-exceed="handleExceed"
            :on-remove="handleRemove"
            :on-success="handleSuccess"
            action="#"
        >
          <el-button size="small" type="primary">点击上传</el-button>
        </el-upload>
        <!-- 显示上传进度 -->
        <el-progress
            v-if="progressVisible"
            :percentage="progressPercent"
            status="success"
        ></el-progress>
      </el-form-item>
      <el-form-item label="更新日志" prop="updateInfo">
        <Index ref="editor" v-model="dataForm.updateInfo"></Index>
      </el-form-item>
      <el-form-item label="添加时间">
        {{ dataForm.updateTime }}
      </el-form-item>
    </el-form>
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="dataFormSubmit">确定</el-button>
      </div>
    </template>
  </el-dialog>
</template>
<script setup>
import { ref, reactive, nextTick } from 'vue'
import Index from '@/components/editor/index.vue'
import COS from 'cos-js-sdk-v5'
import api from '@/utils/request-api'

const visible = ref(false)
const dataForm = reactive({
  id: 0,
  versionNum: '',
  updateInfo: '',
  updateTime: '',
  updateType: 0,
  isFront: 0,
  isPreserve: 0,
  fileList: [],
  version: 0,
  parentId: 0
})

const isFrontOptions = [
  {value: 0, label: '爱复盘主程序'},
  {value: 1, label: '更新程序'}
]
const isPreserveOptions = [
  {value: 0, label: '否'},
  {value: 1, label: '是'}
]
const updateTypeOptions = [
  {value: 0, label: '手动更新'},
  {value: 1, label: '强制更新'}
]

const commodityTypeBeanList = ref([])
const vipLevelList = ref([])

const dataRule = {
  versionNum: [
    {required: true, message: '版本名称不能为空', trigger: 'blur'}
  ],
  updateInfo: [
    {required: true, message: '更新日志不能为空', trigger: 'blur'}
  ]
}

const files = ref([])
const beforUploaDelete = ref(false)
const progressVisible = ref(false)
const progressPercent = ref(0)

const editor = ref()
const dataFormRef = ref()

const emit = defineEmits(['refreshDataList'])

const init = async (id) => {
  visible.value = true
  Object.assign(dataForm, {
    id: 0,
    versionNum: '',
    updateInfo: '',
    updateTime: '',
    updateType: 0,
    isFront: 0,
    isPreserve: 0,
    fileList: [],
    version: 0,
    parentId: 0
  })
  if (!id || id == 0) {
    await nextTick()
    editor.value && editor.value.destroy()
    editor.value && editor.value.init(dataForm.updateInfo)
  } else {
    const res = await api.clientupdate.info({id: id}, {showLoading: true})
    files.value = []
    if (res && res.code === 0) {
      const responseData = res.data
      dataForm.id = responseData.id
      dataForm.versionNum = responseData.versionNum
      dataForm.updateInfo = responseData.updateInfo
      dataForm.updateTime = responseData.updateTime
      dataForm.updateType = responseData.updateType
      dataForm.isFront = responseData.isFront
      dataForm.isPreserve = responseData.isPreserve
      dataForm.version = responseData.version
      dataForm.parentId = responseData.parentId
      dataForm.cosKey = responseData.cosKey
      dataForm.fileMd5 = responseData.fileMd5
      dataForm.fileList = []
      for (let i = 0; i < responseData.fileList.length; i++) {
        dataForm.fileList.push({name: responseData.fileList[i].fileName})
        files.value.push({
          fileId: responseData.fileList[i].id,
          fileName: responseData.fileList[i].fileName
        })
      }
      await nextTick()
      editor.value && editor.value.destroy()
      editor.value && editor.value.init(dataForm.updateInfo)
    }
  }
}

const handleRemove = () => {
  files.value = []
}

const handlePreview = (file) => {
  console.log(file)
}

const handleExceed = (filesParam, fileListParam) => {
  ElMessage.warning(
      `当前限制选择 10 个文件，本次选择了 ${filesParam.length} 个文件，共选择了 ${
          filesParam.length + fileListParam.length
      } 个文件`
  )
}

const handleSuccess = (response) => {
  console.log('上传成功', response)
  files.value.push(response.data)
  console.log('files', files.value)
  dataForm.cosKey = response.fileName
  dataForm.fileMd5 = response.fileMd5
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

    const fileName =
        'updatePackage/' +
        dataForm.versionNum +
        '-' +
        getUniqueId() +
        '.' +
        getFileExtension(file.name)
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
            console.log('上传cos成功:', data)
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
  if (dataForm.versionNum === '') {
    ElMessage.error('请先添加版本号')
    return false
  }
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

const dataFormSubmit = () => {
  dataFormRef.value.validate(async (valid) => {
    if (valid) {
      const requestData = JSON.parse(JSON.stringify(dataForm))
      if (requestData.id) {
        const res = await api.clientupdate.update(requestData)
        if (res && res.code === 0) {
          ElMessage({message: res.msg, type: 'success'})
          visible.value = false
          emit('refreshDataList', 0)
        }
      } else {
        const res = await api.clientupdate.save(requestData)
        if (res && res.code === 0) {
          ElMessage({message: res.msg, type: 'success'})
          visible.value = false
          emit('refreshDataList', 1)
        }
      }
    }
  })
}

defineExpose({init})
</script>
<style scoped>
.el-select {
  width: 100%;
}
</style>
