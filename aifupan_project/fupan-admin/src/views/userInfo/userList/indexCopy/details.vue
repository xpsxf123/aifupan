<template>
  <el-dialog :title="!dataForm.id ? '新增' : '修改'" :close-on-click-modal="false" v-model="visible">
    <el-form :model="dataForm" :rules="dataRule" ref="dataFormRef" label-width="80px">
      <el-form-item label="手机号" prop="phone">
        <el-input v-model="dataForm.phone" placeholder="手机号"></el-input>
      </el-form-item>
      <el-form-item label="登录账号" prop="username">
        <el-input v-model="dataForm.username" placeholder="登录账号"></el-input>
      </el-form-item>
      <el-form-item v-if="!dataForm.id" label="密码" prop="password">
        <el-input v-model="dataForm.password" placeholder="密码" show-password></el-input>
      </el-form-item>
      <el-form-item label="昵称" prop="nickName">
        <el-input v-model="dataForm.nickName" placeholder="昵称"></el-input>
      </el-form-item>
      <el-form-item label="登录类型">
        <el-select v-model="dataForm.userType" style="width: 100%" placeholder="请选择">
          <el-option
            v-for="item in roleOptions" 
            :key="item.id" 
            :label="item.name" 
            :value="item.id">
          </el-option>
        </el-select>
      </el-form-item>
    </el-form>
    <template #footer>
      <span class="dialog-footer">
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="dataFormSubmit(dataForm)">确定</el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, nextTick } from 'vue'
import api from '@/utils/request-api'

const emit = defineEmits(['refreshDataList'])

const visible = ref(false)
const dataFormRef = ref()

const roleOptions = ref([
  {
    id: 1,
    name: "用户账号"
  },
  {
    id: 2,
    name: "运营账号"
  }
])

const dataForm = reactive({
  id: 0,
  username: "",
  password: "",
  nickName: "",
  createDate: "",
  updateDate: "",
  isDeleted: "",
  phone: "",
  ips: "",
  roleIdList: [],
})

const dataRule = reactive({
  username: [
    { required: true, message: "登录账号不能为空", trigger: "blur" },
  ],
  password: [
    { required: true, message: "密码不能为空", trigger: "blur" },
  ],
  nickName: [
    { required: true, message: "昵称不能为空", trigger: "blur" },
  ],
  phone: [
    {
      required: true,
      message: "手机号不能为空",
      trigger: "blur",
    }, {
      pattern: /^(13[0-9]|14[01456879]|15[0-35-9]|16[2567]|17[0-8]|18[0-9]|19[0-35-9])\d{8}$/,
      message: "请填写正确的手机号码",
      trigger: "blur",
    }
  ],
})

const getRoleList = () => {
  // api.role.list({ limit: -1 }).then((res) => {
  //   if (res && res.code === 0) {
  //     roleOptions.value = res.data.list;
  //   }
  // });
}

const init = (id) => {
  dataForm.roleIdList = []
  getRoleList()
  dataForm.id = id || 0
  visible.value = true
  nextTick(() => {
    dataFormRef.value.resetFields()
    if (dataForm.id) {
      api.user.info({ id: dataForm.id }).then((data) => {
        if (data && data.code === 0) {
          Object.assign(dataForm, data.data)
        }
      })
    }
  })
}

const dataFormSubmit = (formData) => {
  const requestDate = JSON.parse(JSON.stringify(dataForm))
  api.user.updateByUserId(requestDate).then((res) => {
    if (res && res.code === 0) {
      ElMessage({
        message: res.msg,
        type: "success",
        duration: 1500,
        onClose: () => {
          visible.value = false
          emit("refreshDataList")
        },
      })
    } else {
      ElMessage.error(res.msg)
    }
  })
}

defineExpose({
  init
})
</script>
