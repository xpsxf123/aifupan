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
      <el-form-item label="code" prop="kvKey">
        <el-input v-model="dataForm.kvKey" placeholder="code"></el-input>
      </el-form-item>
      <el-form-item label="value" prop="kvValue">
        <el-input v-model="dataForm.kvValue" placeholder="value"></el-input>
      </el-form-item>
      <el-form-item label="备注" prop="remarks">
        <el-input v-model="dataForm.remarks" placeholder="备注"></el-input>
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
  kvKey: '',
  kvValue: '',
  remarks: ''
})

const dataRule = {
  kvKey: [{required: true, message: 'code不能为空', trigger: 'blur'}],
  kvValue: [{required: true, message: 'value不能为空', trigger: 'blur'}],
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
      api.systemkv.info({id: dataForm.id}).then((data) => {
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
        api.systemkv.update(requestData).then((res) => {
          if (res && res.code === 0) {
            visible.value = false
            emit('refresh-data-list')
            ElMessage({
              message: res.msg,
              type: 'success'
            })
          }
        })
      } else {
        // 新增
        requestData.id = ''
        api.systemkv.save(requestData).then((res) => {
          if (res && res.code === 0) {
            visible.value = false
            emit('refresh-data-list')
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
