<template>
  <el-dialog
      v-model="visible"
      :close-on-click-modal="false"
      :title="!dataFormData.id ? '新增' : '修改'"
      :width="550"
  >
    <el-form
        ref="dataForm"
        :model="dataFormData"
        :rules="dataRule"
        label-width="120px"
    >
      <el-form-item label="字典类型标识" prop="logo">
        <el-input v-model="dataFormData.logo" placeholder="字典类型标识"></el-input>
      </el-form-item>
      <el-form-item label="字典类型名称" prop="name">
        <el-input v-model="dataFormData.name" placeholder="字典类型名称"></el-input>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select
            v-model="dataFormData.status"
            placeholder="请选择"
            style="width: 100%"
        >
          <el-option
              v-for="(item, index) in ['启用', '禁用']"
              :key="item"
              :label="item"
              :value="index"
          >
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
const dataForm = ref(null)

const dataFormData = reactive({
  id: 0,
  name: '',
  logo: '',
  status: '',
  createDate: '',
  updateDate: '',
  isDeleted: ''
})

const dataRule = reactive({
  logo: [
    {required: true, message: '字典类型标识不能为空', trigger: 'blur'}
  ],
  name: [
    {required: true, message: '字典类型名称不能为空', trigger: 'blur'}
  ],
  status: [
    {
      required: true,
      message: '状态不能为空',
      trigger: 'blur'
    }
  ]
})

const init = (id) => {
  dataFormData.id = id || 0
  visible.value = true
  nextTick(() => {
    dataForm.value.resetFields()
    if (dataFormData.id) {
      api.dicttype.info({id: dataFormData.id}).then((data) => {
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
        api.dicttype.update(requestDate).then((res) => {
          if (res && res.code === 0) {
            visible.value = false
            emit('refreshDataList')
            ElMessage({
              message: res.msg,
              type: 'success'
            })
          }
        })
      } else {
        requestDate.id = ''
        api.dicttype.save(requestDate).then((res) => {
          if (res && res.code === 0) {
            visible.value = false
            emit('refreshDataList')
            ElMessage({
              message: res.msg,
              type: 'success'
            })
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
