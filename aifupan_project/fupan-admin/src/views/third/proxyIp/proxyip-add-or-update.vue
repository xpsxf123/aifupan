<template>
  <el-dialog v-model="visible" :close-on-click-modal="false" :title="!dataForm.id ? '新增' : '修改'" :width="550">
    <el-form ref="dataFormRef" :model="dataForm" :rules="dataRule" label-width="140px" @keyup.enter="dataFormSubmit()">
      <el-form-item label="提取IP的url" prop="extractUrl">
        <el-input v-model="dataForm.extractUrl" :rows="3" placeholder="提取IP的url" type="textarea"></el-input>
      </el-form-item>
      <el-form-item label="总IP数" prop="totalNum">
        <el-input v-model="dataForm.totalNum" placeholder="总IP数"></el-input>
      </el-form-item>
      <el-form-item label="IP有效时长" prop="ipEffectiveTime">
        <el-input v-model="dataForm.ipEffectiveTime" placeholder="IP有效时长(分钟)"></el-input>
      </el-form-item>
      <el-form-item label="时效类型" prop="validityType">
        <el-select v-model="dataForm.validityType" placeholder="时效类型" style="width: 100%;">
          <el-option :value="0" label="短效"></el-option>
          <el-option :value="1" label="长效"></el-option>
        </el-select>
      </el-form-item>
      <el-form-item label="代理IP账号" prop="proxyUsername">
        <el-input v-model="dataForm.proxyUsername" placeholder="代理IP账号"></el-input>
      </el-form-item>
      <el-form-item label="代理IP密码" prop="proxyPassword">
        <el-input v-model="dataForm.proxyPassword" placeholder="代理IP密码"></el-input>
      </el-form-item>
      <el-form-item label="每天最大使用次数" prop="dayUseNum">
        <el-input v-model="dataForm.dayUseNum" placeholder="每天最大使用次数(-1为不限制)"></el-input>
      </el-form-item>
    </el-form>
    <template #footer>
            <span class="dialog-footer">
                <el-button @click="visible = false">取消</el-button>
                <el-button type="primary" @click="dataFormSubmit">确定</el-button>
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
  extractUrl: '',
  totalNum: '',
  remainingNum: '',
  ipEffectiveTime: '',
  createDate: '',
  updateDate: '',
  isDeleted: '',
  proxyUsername: '',
  proxyPassword: '',
  validityType: 0,
  dayUseNum: null
})

const dataRule = {
  extractUrl: [
    {required: true, message: '提取ip的url不能为空', trigger: 'blur'}
  ],
  totalNum: [
    {required: true, message: '总ip数不能为空', trigger: 'blur'}
  ],
  remainingNum: [
    {required: true, message: '剩余ip数不能为空', trigger: 'blur'}
  ],
  ipEffectiveTime: [
    {required: true, message: '有效时长，分钟不能为空', trigger: 'blur'}
  ],
  validityType: [
    {required: true, message: '时效类型不能为空', trigger: 'blur'}
  ],
  dayUseNum: [
    {required: true, message: '每天最大使用次数不能为空', trigger: 'blur'}
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
      api.proxyip.info({id: dataForm.id}).then((data) => {
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
        api.proxyip.update(requestData).then((res) => {
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
        api.proxyip.save(requestData).then((res) => {
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
