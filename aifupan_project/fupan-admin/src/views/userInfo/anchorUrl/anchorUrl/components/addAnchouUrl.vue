<template>
  <el-dialog
    :title="'新增'"
    :close-on-click-modal="false"
    v-model="visible"
    :fullscreen="isMobile"
    :class="{ 'mobile-dialog-innner-custom': isMobile }"
  >
    <el-form
      :model="dataForm"
      :rules="dataRule"
      ref="dataFormRef"
      label-width="110px"
    >
      <el-form-item label="主播唯一标识" prop="phone">
        <el-input
          v-model="dataForm.phone"
          placeholder="输入数字或字母"
        ></el-input>
      </el-form-item>
      <el-form-item label="主页url" prop="password">
        <el-input
          v-model="dataForm.password"
          placeholder=""
          show-password
        ></el-input>
      </el-form-item>
      <el-form-item label="直播间url" prop="nickName">
        <el-input v-model="dataForm.nickName" placeholder="昵称"></el-input>
      </el-form-item>
      <el-form-item label="主播名称" prop="nickName">
        <el-input v-model="dataForm.nickName" placeholder="昵称"></el-input>
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
const props = defineProps({
  isMobile: {
    type: Boolean,
    default: false,
  },
})
const emit = defineEmits(['refreshDataList'])

const visible = ref(false)
const dataFormRef = ref(null)
const roleOptions = ref([])

const dataForm = reactive({
  id: 0,
  username: '',
  password: '',
  nickName: '',
  createDate: '',
  updateDate: '',
  isDeleted: '',
  phone: '',
  ips: '',
  roleIdList: [],
})

const dataRule = reactive({
  username: [{ required: true, message: '登录账号不能为空', trigger: 'blur' }],
  password: [{ required: true, message: '密码不能为空', trigger: 'blur' }],
  nickName: [{ required: true, message: '昵称不能为空', trigger: 'blur' }],
  phone: [
    {
      required: true,
      message: '手机号不能为空',
      trigger: 'blur',
    },
    {
      pattern:
        /^(13[0-9]|14[01456879]|15[0-35-9]|16[2567]|17[0-8]|18[0-9]|19[0-35-9])\d{8}$/,
      message: '请填写正确的手机号码',
      trigger: 'blur',
    },
  ],
})

const getRoleList = async () => {
  try {
    const res = await api.role.list({ limit: -1 })
    if (res && res.code === 0) {
      roleOptions.value = res.data.list
    }
  } catch (error) {
    console.error('获取角色列表失败:', error)
  }
}

const init = async (id) => {
  dataForm.roleIdList = []
  await getRoleList()
  dataForm.id = id || 0
  visible.value = true

  await nextTick(() => {
    dataFormRef.value?.resetFields()
    if (dataForm.id) {
      api.user.info({ id: dataForm.id }).then((data) => {
        if (data && data.code === 0) {
          Object.assign(dataForm, data.data)
        }
      })
    }
  })
}

const dataFormSubmit = () => {
  dataFormRef.value?.validate(async (valid) => {
    if (valid) {
      const requestDate = JSON.parse(JSON.stringify(dataForm))

      try {
        if (dataForm.id) {
          const res = await api.user.update(requestDate)
          if (res && res.code === 0) {
            ElMessage({
              message: res.msg,
              type: 'success',
              duration: 1500,
              onClose: () => {
                visible.value = false
                emit('refreshDataList')
              },
            })
          } else {
            ElMessage.error(res.msg)
          }
        } else {
          requestDate.id = ''
          const res = await api.user.save(requestDate)
          if (res && res.code === 0) {
            ElMessage({
              message: res.msg,
              type: 'success',
              duration: 1500,
              onClose: () => {
                visible.value = false
                emit('refreshDataList')
              },
            })
          } else {
            ElMessage.error(res.msg)
          }
        }
      } catch (error) {
        ElMessage.error('操作失败')
        console.error('操作失败:', error)
      }
    }
  })
}

defineExpose({
  init,
})
</script>
