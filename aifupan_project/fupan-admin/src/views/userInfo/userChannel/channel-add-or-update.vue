<template>
  <el-dialog
    :title="!dataForm.id ? '新增' : '修改'"
    :close-on-click-modal="false"
    :width="600"
    v-model="visible"
  >
    <el-form
      :model="dataForm"
      :rules="dataRule"
      ref="dataFormRef"
      label-width="90px"
    >
      <el-form-item label="渠道名称" prop="channelName">
        <el-input
          v-model="dataForm.channelName"
          placeholder="渠道名称"
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

const emit = defineEmits(['refreshDataList'])

const visible = ref(false)
const dataFormRef = ref(null)

const dataForm = reactive({
  id: 0,
  parentId: 0,
  channelName: '',
})

const dataRule = reactive({
  parentId: [{ required: true, message: '父ID不能为空', trigger: 'blur' }],
  channelName: [
    { required: true, message: '渠道名称不能为空', trigger: 'blur' },
  ],
  createDate: [
    { required: true, message: '创建时间不能为空', trigger: 'blur' },
  ],
  updateDate: [
    { required: true, message: '最后修改时间不能为空', trigger: 'blur' },
  ],
  isDeleted: [
    { required: true, message: '是否已删除不能为空', trigger: 'blur' },
  ],
})

const init = (id, parentId) => {
  dataForm.id = id || 0
  dataForm.parentId = parentId || 0
  visible.value = true
  nextTick(() => {
    dataFormRef.value?.resetFields()
    if (dataForm.id) {
      api.channel.info({ id: dataForm.id }).then((data) => {
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
      let requestData = JSON.parse(JSON.stringify(dataForm))

      if (dataForm.id) {
        const res = await api.channel.update(requestData)
        if (res && res.code === 0) {
          emit('refreshDataList')
          ElMessage({
            message: res.msg,
            type: 'success',
          })
          visible.value = false
        }
      } else {
        requestData.id = ''
        const res = await api.channel.save(requestData)
        if (res && res.code === 0) {
          emit('refreshDataList')
          ElMessage({
            message: res.msg,
            type: 'success',
          })
          visible.value = false
        }
      }
    }
  })
}

defineExpose({
  init,
})
</script>
