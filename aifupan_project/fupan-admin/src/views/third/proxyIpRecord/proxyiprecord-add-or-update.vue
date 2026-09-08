<template>
  <el-dialog
      v-model="visible"
      :close-on-click-modal="false"
      :title="!dataForm.id ? '新增' : '修改'"
  >
    <el-form
        ref="dataFormRef"
        :model="dataForm"
        :rules="dataRule"
        label-width="80px"
    >
      <el-form-item label="用户id" prop="userId">
        <el-input v-model="dataForm.userId" placeholder="用户id"></el-input>
      </el-form-item>
      <el-form-item label="IP" prop="ipStr">
        <el-input v-model="dataForm.ipStr" placeholder="IP"></el-input>
      </el-form-item>
      <el-form-item label="端口号" prop="portStr">
        <el-input v-model="dataForm.portStr" placeholder="端口号"></el-input>
      </el-form-item>
      <el-form-item label="提取时间" prop="extractDate">
        <el-input
            v-model="dataForm.extractDate"
            placeholder="提取日期"
        ></el-input>
      </el-form-item>
      <el-form-item label="有效期" prop="validityDate">
        <el-input
            v-model="dataForm.validityDate"
            placeholder="有效期"
        ></el-input>
      </el-form-item>
      <el-form-item label="ip有效时长" prop="ipEffectiveTime">
        <el-input
            v-model="dataForm.ipEffectiveTime"
            placeholder="ip有效时长，分钟"
        ></el-input>
      </el-form-item>
      <el-form-item label="创建时间" prop="createDate">
        <el-input
            v-model="dataForm.createDate"
            placeholder="创建时间"
        ></el-input>
      </el-form-item>
      <el-form-item label="最后修改时间" prop="updateDate">
        <el-input
            v-model="dataForm.updateDate"
            placeholder="最后修改时间"
        ></el-input>
      </el-form-item>
      <el-form-item label="是否已删除" prop="isDeleted">
        <el-input
            v-model="dataForm.isDeleted"
            placeholder="是否已删除"
        ></el-input>
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

const emit = defineEmits(['refresh-data-list'])

const visible = ref(false)
const dataFormRef = ref(null)

const dataForm = reactive({
  id: 0,
  userId: '',
  ipStr: '',
  portStr: '',
  extractDate: '',
  validityDate: '',
  ipEffectiveTime: '',
  createDate: '',
  updateDate: '',
  isDeleted: ''
})

const dataRule = {
  userId: [{required: true, message: '用户id不能为空', trigger: 'blur'}],
  ipStr: [{required: true, message: 'ip地址不能为空', trigger: 'blur'}],
  portStr: [{required: true, message: '端口号不能为空', trigger: 'blur'}],
  extractDate: [
    {required: true, message: '提取日期不能为空', trigger: 'blur'}
  ],
  validityDate: [
    {required: true, message: '有效期不能为空', trigger: 'blur'}
  ],
  ipEffectiveTime: [
    {required: true, message: 'ip有效时长，分钟不能为空', trigger: 'blur'}
  ],
  createDate: [
    {required: true, message: '创建时间不能为空', trigger: 'blur'}
  ],
  updateDate: [
    {required: true, message: '最后修改时间不能为空', trigger: 'blur'}
  ],
  isDeleted: [
    {required: true, message: '是否已删除不能为空', trigger: 'blur'}
  ]
}

const init = (id) => {
  dataForm.id = id || 0
  visible.value = true
  nextTick(() => {
    dataFormRef.value.resetFields()
    if (dataForm.id) {
      api.proxyiprecord.info({id: dataForm.id}).then((data) => {
        if (data && data.code === 0) {
          Object.assign(dataForm, data.data)
        }
      })
    }
  })
}

const dataFormSubmit = () => {
  dataFormRef.value.validate((valid) => {
    if (valid) {
      let requestData = JSON.parse(JSON.stringify(dataForm))

      if (dataForm.id) {
        // 修改
        api.proxyiprecord.update(requestData).then((res) => {
          if (res && res.code === 0) {
            emit('refresh-data-list')
            ElMessage({
              message: res.msg,
              type: 'success'
            })
            visible.value = false
          }
        })
      } else {
        // 新增
        requestData.id = ''
        api.proxyiprecord.save(requestData).then((res) => {
          if (res && res.code === 0) {
            emit('refresh-data-list')
            ElMessage({
              message: res.msg,
              type: 'success'
            })
            visible.value = false
          }
        })
      }
    }
  })
}

defineExpose({
  init
})
</script>
