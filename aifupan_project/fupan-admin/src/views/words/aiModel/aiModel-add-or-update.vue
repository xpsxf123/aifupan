<template>
  <el-dialog
      v-model="visible"
      :close-on-click-modal="false"
      :title="!dataForm.id ? '新增' : '修改'"
      :width="700"
  >
    <el-form
        ref="dataFormRef"
        :model="dataForm"
        :rules="dataRule"
        label-width="80px"
    >
      <el-form-item label="厂商类型" label-width="100px" prop="resourceType">
        <el-select v-model="dataForm.resourceType" placeholder="请选择">
          <el-option
              v-for="item in resourceList"
              :key="item.id"
              :label="item.label"
              :value="item.value"
          >
          </el-option>
        </el-select>
      </el-form-item>
      <el-form-item label="使用方式" label-width="100px" prop="useType">
        <el-select v-model="dataForm.useType" placeholder="请选择">
          <el-option
              v-for="item in useTypeList"
              :key="item.id"
              :label="item.label"
              :value="item.value"
          >
          </el-option>
        </el-select>
      </el-form-item>
      <el-form-item label="code" label-width="100px" prop="modelCode">
        <el-input v-model="dataForm.modelCode" placeholder="code"></el-input>
      </el-form-item>
      <el-form-item label="模型名称" label-width="100px" prop="modelName">
        <el-input
            v-model="dataForm.modelName"
            placeholder="模型名称"
        ></el-input>
      </el-form-item>
      <el-form-item label="模型id" label-width="100px" prop="endpointId">
        <el-input v-model="dataForm.endpointId" placeholder="模型id"></el-input>
      </el-form-item>
      <el-form-item label="接口apiKey" label-width="100px" prop="apiKey">
        <el-input v-model="dataForm.apiKey" placeholder="接口apiKey"></el-input>
      </el-form-item>
      <el-form-item
          label="输出数据流大小"
          label-width="150px"
          prop="outSize"
      >
        <el-input
            v-model="dataForm.outSize"
            placeholder="输出数据流大小"
        >
          <template #suffix>
            KB
          </template>
        </el-input>
      </el-form-item>
      <el-form-item
          label="输入数据流大小"
          label-width="150px"
          prop="inputSize"
      >
        <el-input
            v-model="dataForm.inputSize"
            placeholder="输入数据流大小"
        >
          <template #suffix>
            KB
          </template>
        </el-input>
      </el-form-item>
      <el-form-item
          label="缓存上下文大小"
          label-width="150px"
          prop="contextSize"
      >
        <el-input
            v-model="dataForm.contextSize"
            placeholder="缓存上下文大小"
        >
          <template #suffix>
            KB
          </template>
        </el-input>
      </el-form-item>
      <el-form-item
          label="限制使用字数数量"
          label-width="150px"
          prop="wordsNum"
      >
        <el-input
            v-model="dataForm.wordsNum"
            placeholder="限制使用字数数量"
        ></el-input>
      </el-form-item>
      <el-form-item label="推荐输出字数" label-width="150px" prop="outWordNum">
        <el-input
            v-model="dataForm.outWordNum"
            placeholder="限制使用字数数量"
        ></el-input>
      </el-form-item>

      <el-form-item label="排序" prop="sort">
        <el-input v-model="dataForm.sort" placeholder="排序"></el-input>
      </el-form-item>

      <el-form-item label="描述" prop="remarks">
        <el-input v-model="dataForm.remarks" placeholder="描述"></el-input>
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

const visible = ref(false)
const dataFormRef = ref(null)

const resourceList = ref([
  {
    id: 0,
    value: 0,
    label: '豆包'
  },
  {
    id: 1,
    value: 1,
    label: '通义'
  }
])

const useTypeList = ref([
  {
    id: 0,
    value: 0,
    label: '分析内容'
  },
  {
    id: 1,
    value: 1,
    label: '数据截图'
  }
])

const dataForm = reactive({
  id: 0,
  modelCode: '',
  resourceType: '',
  useType: 0,
  modelName: '',
  endpointId: '',
  apiKey: '',
  outSize: '',
  inputSize: '',
  contextSize: '',
  wordsNum: '',
  outWordNum: 0,
  remarks: '',
  sort: 0,
  createDate: '',
  updateDate: '',
  isDeleted: ''
})

const dataRule = reactive({
  modelCode: [{required: true, message: 'code不能为空', trigger: 'blur'}],
  resourceType: [{required: true, message: '厂商类型', trigger: 'blur'}],
  useType: [{required: true, message: '使用方式不能为空', trigger: 'blur'}],
  modelName: [{required: true, message: '模型名称不能为空', trigger: 'blur'}],
  endpointId: [{required: true, message: '模型id不能为空', trigger: 'blur'}],
  apiKey: [{required: true, message: '接口apiKey不能为空', trigger: 'blur'}],
  wordsNum: [
    {required: true, message: '限制使用字数数量不能为空', trigger: 'blur'}
  ],
  outSize: [
    {required: true, message: '输出数据流大小(kb)不能为空', trigger: 'blur'}
  ],
  inputSize: [
    {required: true, message: '输入数据流大小(kb)不能为空', trigger: 'blur'}
  ],
  contextSize: [
    {required: true, message: '缓存上下文大小(kb)不能为空', trigger: 'blur'}
  ]
})
const emit = defineEmits(['refreshDataList'])

const init = (id) => {
  dataForm.id = id || 0
  visible.value = true
  nextTick(() => {
    dataFormRef.value.resetFields()
    if (dataForm.id) {
      api.aiModel.info({id: dataForm.id}).then((data) => {
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
        api.aiModel.update(requestData).then((res) => {
          if (res && res.code === 0) {
            emit('refreshDataList')
            ElMessage({
              message: res.msg,
              type: 'success'
            })
            visible.value = false
          }
        })
      } else {
        requestData.id = ''
        api.aiModel.save(requestData).then((res) => {
          if (res && res.code === 0) {
            emit('refreshDataList')
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
