<template>
  <el-dialog v-model="visible" :close-on-click-modal="false" :title="!dataForm.id ? '新增' : '修改'" :width="550">
    <el-form ref="dataFormRef" :model="dataForm" :rules="dataRule" label-width="120px">
      <el-form-item label="字典类型" prop="typeId">
        <el-select v-model="dataForm.typeId" filterable placeholder="请选择" style="width: 100%">
          <el-option v-for="item in typeList" :key="item.id" :label="item.name" :value="item.id">
          </el-option>
        </el-select>
      </el-form-item>
      <el-form-item label="字典label" prop="label">
        <el-input v-model="dataForm.label" placeholder="字典label"></el-input>
      </el-form-item>
      <el-form-item label="字典value" prop="value">
        <el-input v-model="dataForm.value" placeholder="字典value" rows="1" type="textarea"></el-input>
      </el-form-item>
      <el-form-item label="排序" prop="sort">
        <el-input v-model="dataForm.sort" placeholder="排序"></el-input>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="dataForm.status" placeholder="请选择" style="width: 100%">
          <el-option v-for="(item, index) in ['启用', '禁用']" :key="item" :label="item" :value="index">
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

const typeList = ref([])
const visible = ref(false)
const dataForm = reactive({
  id: 0,
  typeId: '',
  label: '',
  value: '',
  sort: '',
  createDate: '',
  updateDate: '',
  isDeleted: '',
  status: ''
})

const dataRule = reactive({
  typeId: [
    {required: true, message: '字典类型id不能为空', trigger: 'blur'}
  ],
  label: [
    {required: true, message: '字典label不能为空', trigger: 'blur'}
  ],
  value: [
    {required: true, message: '字典value不能为空', trigger: 'blur'}
  ],
  sort: [{required: true, message: '排序不能为空', trigger: 'blur'}],
  status: [
    {required: true, message: '状态不能为空', trigger: 'blur'}
  ]
})

const dataFormRef = ref(null)

const getTypeList = () => {
  api.dicttype.list({limit: -1}).then((res) => {
    if (res && res.code === 0) {
      typeList.value = res.data.list
    } else {
      typeList.value = []
    }
  })
}

const init = (id) => {
  getTypeList()
  dataForm.id = id || 0
  visible.value = true
  nextTick(() => {
    dataFormRef.value.resetFields()
    if (dataForm.id) {
      api.dictdata.info({id: dataForm.id}).then((data) => {
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
      let requestDate = JSON.parse(JSON.stringify(dataForm))

      if (dataForm.id) {
        api.dictdata.update(requestDate).then((res) => {
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
        api.dictdata.save(requestDate).then((res) => {
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
