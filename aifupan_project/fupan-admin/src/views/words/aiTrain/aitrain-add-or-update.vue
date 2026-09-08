<template>
  <el-dialog
      v-model="visible"
      :close-on-click-modal="false"
      :title="!dataFormData.id ? '新增' : '修改'"
  >
    <el-form
        ref="dataForm"
        :model="dataFormData"
        :rules="dataRule"
        label-width="80px"
    >
      <el-form-item label="用户id" prop="userId">
        <el-input v-model="dataFormData.userId" placeholder="用户id"></el-input>
      </el-form-item>
      <el-form-item label="租户id" prop="tenantId">
        <el-input
            v-model="dataFormData.tenantId"
            placeholder="租户id"
        ></el-input>
      </el-form-item>
      <el-form-item label="视频唯一标识" prop="videoId">
        <el-input
            v-model="dataFormData.videoId"
            placeholder="视频唯一标识"
        ></el-input>
      </el-form-item>
      <el-form-item
          label="状态 0：训练中 1：管理员训练完成 2：超时训练完成"
          prop="aiStatus"
      >
        <el-input
            v-model="dataFormData.aiStatus"
            placeholder="状态 0：训练中 1：管理员训练完成 2：超时训练完成"
        ></el-input>
      </el-form-item>
      <el-form-item label="进步幅度 如：0.0015就是0.15%" prop="progressRange">
        <el-input
            v-model="dataFormData.progressRange"
            placeholder="进步幅度 如：0.0015就是0.15%"
        ></el-input>
      </el-form-item>
      <el-form-item label="创建时间" prop="createDate">
        <el-input
            v-model="dataFormData.createDate"
            placeholder="创建时间"
        ></el-input>
      </el-form-item>
      <el-form-item label="最后修改时间" prop="updateDate">
        <el-input
            v-model="dataFormData.updateDate"
            placeholder="最后修改时间"
        ></el-input>
      </el-form-item>
      <el-form-item label="是否已删除" prop="isDeleted">
        <el-input
            v-model="dataFormData.isDeleted"
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
const dataForm = ref(null)

const dataFormData = reactive({
  id: 0,
  userId: '',
  tenantId: '',
  videoId: '',
  aiStatus: '',
  progressRange: '',
  createDate: '',
  updateDate: '',
  isDeleted: ''
})

const dataRule = reactive({
  userId: [{required: true, message: '用户id不能为空', trigger: 'blur'}],
  tenantId: [{required: true, message: '租户id不能为空', trigger: 'blur'}],
  videoId: [
    {required: true, message: '视频唯一标识不能为空', trigger: 'blur'}
  ],
  aiStatus: [
    {
      required: true,
      message: '状态 0：训练中 1：管理员训练完成 2：超时训练完成不能为空',
      trigger: 'blur'
    }
  ],
  progressRange: [
    {
      required: true,
      message: '进步幅度 如：0.0015就是0.15%不能为空',
      trigger: 'blur'
    }
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
})
const init = async (id) => {
  dataFormData.id = id || 0
  visible.value = true
  await nextTick()
  dataForm.value.resetFields()
  if (dataFormData.id) {
    try {
      const data = await api.aitrain.info({id: dataFormData.id})
      if (data && data.code === 0) {
        Object.assign(dataFormData, data.data)
      }
    } catch (error) {
      console.error('获取数据失败:', error)
    }
  }
}
const dataFormSubmit = () => {
  dataForm.value.validate(async (valid) => {
    if (valid) {
      const requestData = JSON.parse(JSON.stringify(dataFormData))

      let res
      if (dataFormData.id) {
        res = await api.aitrain.update(requestData)
      } else {
        requestData.id = ''
        res = await api.aitrain.save(requestData)
      }
      if (res && res.code === 0) {
        emit('refreshDataList')
        ElMessage({
          message: res.msg,
          type: 'success'
        })
        visible.value = false
      }
    }
  })
}

defineExpose({
  init
})
</script>
