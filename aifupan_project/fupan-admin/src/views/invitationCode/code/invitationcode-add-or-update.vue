<template>
  <el-dialog
    :title="!dataForm.id ? '新增' : '修改'"
    :close-on-click-modal="false"
    v-model="visible"
  >
    <el-form
      :model="dataForm"
      :rules="dataRule"
      ref="dataFormRef"
      label-width="80px"
    >
      <el-form-item label="关联的邀请码批次id" prop="batchId">
        <el-input
          v-model="dataForm.batchId"
          placeholder="关联的邀请码批次id"
        ></el-input>
      </el-form-item>
      <el-form-item label="邀请码" prop="code">
        <el-input v-model="dataForm.code" placeholder="邀请码"></el-input>
      </el-form-item>
      <el-form-item label="使用状态 0：未使用 1：已使用" prop="useStatus">
        <el-input
          v-model="dataForm.useStatus"
          placeholder="使用状态 0：未使用 1：已使用"
        ></el-input>
      </el-form-item>
      <el-form-item label="有效期开始时间" prop="validityStartDate">
        <el-input
          v-model="dataForm.validityStartDate"
          placeholder="有效期开始时间"
        ></el-input>
      </el-form-item>
      <el-form-item label="有效期结束时间" prop="validityEndDate">
        <el-input
          v-model="dataForm.validityEndDate"
          placeholder="有效期结束时间"
        ></el-input>
      </el-form-item>
      <el-form-item label="状态 0：正常 1：禁用" prop="status">
        <el-input
          v-model="dataForm.status"
          placeholder="状态 0：正常 1：禁用"
        ></el-input>
      </el-form-item>
      <el-form-item label="使用后关联的订单id，未使用时为0" prop="orderId">
        <el-input
          v-model="dataForm.orderId"
          placeholder="使用后关联的订单id，未使用时为0"
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

const emit = defineEmits(['refreshDataList'])

const visible = ref(false)
const dataFormRef = ref()

const dataForm = reactive({
  id: 0,
  batchId: '',
  code: '',
  useStatus: '',
  validityStartDate: '',
  validityEndDate: '',
  status: '',
  orderId: '',
  createDate: '',
  updateDate: '',
  isDeleted: '',
})

const dataRule = reactive({
  batchId: [
    {
      required: true,
      message: '关联的邀请码批次id不能为空',
      trigger: 'blur',
    },
  ],
  code: [{ required: true, message: '邀请码不能为空', trigger: 'blur' }],
  useStatus: [
    {
      required: true,
      message: '使用状态 0：未使用 1：已使用不能为空',
      trigger: 'blur',
    },
  ],
  validityStartDate: [
    {
      required: true,
      message: '有效期开始时间不能为空',
      trigger: 'blur',
    },
  ],
  validityEndDate: [
    {
      required: true,
      message: '有效期结束时间不能为空',
      trigger: 'blur',
    },
  ],
  status: [
    {
      required: true,
      message: '状态 0：正常 1：禁用不能为空',
      trigger: 'blur',
    },
  ],
  orderId: [
    {
      required: true,
      message: '使用后关联的订单id，未使用时为0不能为空',
      trigger: 'blur',
    },
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

const init = async (id) => {
  dataForm.id = id || 0
  visible.value = true
  await nextTick()
  dataFormRef.value.resetFields()
  if (dataForm.id) {
    const data = await api.invitationcode.info({ id: dataForm.id })
    if (data && data.code === 0) {
      Object.assign(dataForm, data.data)
    }
  }
}

const dataFormSubmit = () => {
  dataFormRef.value.validate(async (valid) => {
    if (valid) {
      const requestData = JSON.parse(JSON.stringify(dataForm))

      if (dataForm.id) {
        const res = await api.invitationcode.update(requestData)
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
        requestData.id = ''
        const res = await api.invitationcode.save(requestData)
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
    }
  })
}

defineExpose({
  init
})
</script>
