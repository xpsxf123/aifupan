<template>
  <el-dialog
      v-model="visible"
      :close-on-click-modal="false"
      :title="!dataForm.id ? '新增' : '修改'"
      :width="800"
  >
    <el-form ref="dataFormRef" :model="dataForm" :rules="dataRule" label-width="80px">
      <el-form-item label="标题" prop="title">
        <el-input v-model="dataForm.title" placeholder="文章标题"></el-input>
      </el-form-item>
      <el-form-item label="类型" prop="type">
        <el-select v-model="dataForm.type" placeholder="请选择" style="width: 100%;">
          <el-option v-for="item in typeList" :key="item.id" :label="item.label" :value="item.value"/>
        </el-select>
      </el-form-item>
      <el-form-item label="备注" prop="remarks">
        <el-input v-model="dataForm.remarks" placeholder="备注"></el-input>
      </el-form-item>
      <el-form-item label="内容" prop="content">
        <Index ref="editor" v-model="dataForm.content"></Index>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" @click="dataFormSubmit()">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, nextTick } from 'vue'
import api from '@/utils/request-api'
import Index from '@/components/editor/index.vue'

const emit = defineEmits(['refreshDataList'])
const visible = ref(false)
const typeList = ref([])
const dataForm = reactive({
  id: 0,
  userId: '',
  title: '',
  remarks: '',
  type: '',
  content: '',
  createDate: '',
  updateDate: '',
  isDeleted: ''
})
const dataRule = reactive({
  title: [{required: true, message: '文章标题不能为空', trigger: 'blur'}],
  type: [{required: true, message: '类型不能为空', trigger: 'blur'}],
  content: [{required: true, message: '内容不能为空', trigger: 'blur'}]
})

const dataFormRef = ref()
const editor = ref()

const getTypeList = async () => {
  typeList.value = []
  const res = await api.dictdata.list({limit: -1, typeLogo: 'article_type'})
  if (res.code == 0) {
    typeList.value = res.data.list
    typeList.value.forEach(item => {
      item.value = parseInt(item.value)
    })
  }
}

const init = async (id) => {
  await getTypeList()
  dataForm.id = id || 0
  visible.value = true
  await nextTick()
  dataFormRef.value?.resetFields()
  if (dataForm.id) {
    const data = await api.article.info({id: dataForm.id}, {showLoading: true})
    if (data && data.code === 0) {
      Object.assign(dataForm, data.data)
      editor.value?.destroy()
      editor.value?.init(dataForm.content)
    }
  } else {
    editor.value?.destroy()
    editor.value?.init(dataForm.content)
  }
}

const dataFormSubmit = () => {
  dataFormRef.value?.validate(async (valid) => {
    if (valid) {
      const requestData = JSON.parse(JSON.stringify(dataForm))
      if (dataForm.id) {
        const res = await api.article.update(requestData)
        if (res && res.code === 0) {
          emit('refreshDataList')
          visible.value = false
          ElMessage({
            message: res.msg,
            type: 'success'
          })
        }
      } else {
        requestData.id = ''
        const res = await api.article.save(requestData)
        if (res && res.code === 0) {
          emit('refreshDataList')
          visible.value = false
          ElMessage({
            message: res.msg,
            type: 'success'
          })
        }
      }
    }
  })
}

defineExpose({init})
</script>
