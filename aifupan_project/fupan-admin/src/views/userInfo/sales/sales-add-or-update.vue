<template>
  <el-dialog
      v-model="visible"
      :close-on-click-modal="false"
      :title="!dataForm.id ? '新增' : '修改'"
      :width="600"
      @close="handleClose"
  >
    <el-form
        ref="dataFormRef"
        :model="dataForm"
        :rules="dataRule"
        label-width="130px"
    >
      <el-form-item label="跟进销售人员名" prop="salesName">
        <el-input
            v-model="dataForm.salesName"
            placeholder="跟进销售人员名"
        ></el-input>
      </el-form-item>
      <el-form-item label="手机号" prop="phone">
        <el-input
            v-model="dataForm.phone"
            placeholder="跟进销售人员手机号"
        ></el-input>
      </el-form-item>
      <el-form-item label="获客助手链接" prop="salesIntroductionUrl">
        <el-input v-model="dataForm.salesIntroductionUrl" placeholder="获客助手链接">

        </el-input>
      </el-form-item>
      <el-form-item label="是否分配线索" prop="isChoose">
        <el-switch
            v-model="dataForm.isChoose"
            :active-value="1"
            :inactive-value="0"
            active-color="#13ce66"
            active-text="开"
            inactive-color="#dcdfe6"
            inactive-text="关"
        >
        </el-switch>
      </el-form-item>
      <el-form-item label="销售人员二维码" prop="qrcodeImgId">
        <uploadImg
            :fileList="fileList"
            :showFileSize="false"
            @on-success="handleAvatarSuccess"
            @on-remove="handleRemove"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <span class="dialog-footer">
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="dataFormSubmit()">确定</el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, nextTick } from 'vue'
import api from '@/utils/request-api'
import uploadImg from '@/views/userInfo/agent/components/upload-img.vue'

const emit = defineEmits(['refreshDataList'])

const visible = ref(false)
const fileList = ref([])
const dataFormRef = ref(null)

const dataForm = reactive({
  id: 0,
  parentId: 0,
  salesName: '',
  isChoose: 0,
  createDate: '',
  updateDate: '',
  isDeleted: '',
  phone: '',
  qrcodeImgId: '',
  salesIntroductionUrl: ''
})

const dataRule = reactive({
  parentId: [{required: true, message: '父ID不能为空', trigger: 'blur'}],
  salesName: [
    {required: true, message: '跟进销售人员名不能为空', trigger: 'blur'}
  ],
  createDate: [
    {required: true, message: '创建时间不能为空', trigger: 'blur'}
  ],
  updateDate: [
    {required: true, message: '最后修改时间不能为空', trigger: 'blur'}
  ],
  isDeleted: [
    {required: true, message: '是否已删除不能为空', trigger: 'blur'}
  ],
  isChoose: [{required: true, message: '是否分配线索', trigger: 'change'}],
  phone: [
    {required: true, message: '手机号不能为空', trigger: 'blur'},
    {
      trigger: 'blur',
      pattern: /^1[3-9]\d{9}$/,
      message: '请输入有效的手机号码'
    }
  ],
  qrcodeImgId: [
    {required: true, message: '请上传销售人员二维码', trigger: 'change'}
  ]
})

const init = (id) => {
  dataForm.id = id || 0
  visible.value = true
  fileList.value = []
  nextTick(() => {
    dataFormRef.value?.resetFields()
    if (dataForm.id) {
      api.sales.info({id: dataForm.id}, {showLoading: true}).then((data) => {
        if (data && data.code === 0) {
          Object.assign(dataForm, {
            ...dataForm,
            ...data.data
          })
          if (data.data.qrcodeImgInfo?.url) {
            fileList.value.push({
              url: data.data.qrcodeImgInfo.url
            })
          }
          if (data.data.qrcodeImgId === '0') {
            dataForm.qrcodeImgId = ''
          }
        }
      })
    }
  })
}

const dataFormSubmit = () => {
  dataFormRef.value?.validate(async (valid) => {
    if (valid) {
      let requestData = JSON.parse(JSON.stringify(dataForm))

      if (dataForm.id) {
        const res = await api.sales.update(requestData)
        if (res && res.code === 0) {
          emit('refreshDataList')
          ElMessage({
            message: res.msg,
            type: 'success'
          })
          visible.value = false
        }
      } else {
        requestData.id = ''
        const res = await api.sales.save(requestData)
        if (res && res.code === 0) {
          emit('refreshDataList')
          ElMessage({
            message: res.msg,
            type: 'success'
          })
          visible.value = false
        }
      }
    }
  })
}

const handleAvatarSuccess = (response, file, fileListParam) => {
  if (response.code === 0) {
    dataForm.qrcodeImgId = response.data.id
    fileList.value = fileListParam
    dataFormRef.value?.validateField('qrcodeImgId')
  } else {
    ElMessage.error(response.msg)
    fileList.value = []
  }
}

const handleRemove = (file, fileListParam) => {
  fileList.value = fileListParam
  dataForm.qrcodeImgId = ''
}
// 关闭弹窗
const handleClose = () => {
  dataFormRef.value.resetFields()
}
defineExpose({
  init
})
</script>
