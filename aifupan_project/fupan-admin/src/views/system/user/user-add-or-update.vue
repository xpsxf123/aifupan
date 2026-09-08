<template>
  <el-dialog v-model="visible" :close-on-click-modal="false" :title="!dataFormData.id ? '新增' : '修改'" :width="550">
    <el-form ref="dataForm" :model="dataFormData" :rules="dataRule" label-width="80px">
      <el-form-item label="手机号" prop="phone">
        <el-input v-model="dataFormData.phone" placeholder="手机号"></el-input>
      </el-form-item>
      <el-form-item label="登录账号" prop="username">
        <el-input v-model="dataFormData.username" placeholder="登录账号"></el-input>
      </el-form-item>
      <el-form-item v-if="!dataFormData.id" label="密码" prop="password">
        <el-input v-model="dataFormData.password" placeholder="密码" show-password></el-input>
      </el-form-item>
      <el-form-item label="昵称" prop="nickName">
        <el-input v-model="dataFormData.nickName" placeholder="昵称"></el-input>
      </el-form-item>
      <el-form-item label="角色">
        <el-select v-model="dataFormData.roleIdList" multiple placeholder="请选择" style="width: 100%">
          <el-option v-for="item in roleOptions" :key="item.id" :label="item.name" :value="item.id">
          </el-option>
        </el-select>
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

const emit = defineEmits(['refreshDataList'])

const visible = ref(false)
const roleOptions = ref([])
const dataForm = ref(null)

const dataFormData = reactive({
  id: 0,
  username: '',
  password: '',
  nickName: '',
  createDate: '',
  updateDate: '',
  isDeleted: '',
  phone: '',
  ips: '',
  roleIdList: []
})

const dataRule = reactive({
  username: [
    {required: true, message: '登录账号不能为空', trigger: 'blur'}
  ],
  password: [
    {required: true, message: '密码不能为空', trigger: 'blur'}
  ],
  nickName: [
    {required: true, message: '昵称不能为空', trigger: 'blur'}
  ],
  phone: [
    {
      required: true,
      message: '手机号不能为空',
      trigger: 'blur'
    }, {
      pattern: /^(13[0-9]|14[01456879]|15[0-35-9]|16[2567]|17[0-8]|18[0-9]|19[0-35-9])\d{8}$/,
      message: '请填写正确的手机号码',
      trigger: 'blur'
    }
  ]
})

const getRoleList = () => {
  api.role.list({limit: -1}).then((res) => {
    if (res && res.code === 0) {
      roleOptions.value = res.data.list
    }
  })
}

const init = (id) => {
  dataFormData.roleIdList = []
  getRoleList()
  dataFormData.id = id || 0
  visible.value = true
  nextTick(() => {
    dataForm.value.resetFields()
    if (dataFormData.id) {
      api.user.info({id: dataFormData.id}).then((data) => {
        if (data && data.code === 0) {
          Object.assign(dataFormData, data.data)
        }
      })
    }
  })
}

const dataFormSubmit = () => {
  dataForm.value.validate((valid) => {
    if (valid) {
      let requestDate = JSON.parse(JSON.stringify(dataFormData))

      if (dataFormData.id) {
        // 修改
        api.user.update(requestDate).then((res) => {
          if (res && res.code === 0) {
            visible.value = false
            emit('refreshDataList')
            ElMessage({
              message: res.msg,
              type: 'success'
            })
          } else {
            ElMessage.error(res.msg)
          }
        })
      } else {
        // 新增
        requestDate.id = ''
        api.user.save(requestDate).then((res) => {
          if (res && res.code === 0) {
            visible.value = false
            emit('refreshDataList')
            ElMessage({
              message: res.msg,
              type: 'success'
            })
          } else {
            ElMessage.error(res.msg)
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
