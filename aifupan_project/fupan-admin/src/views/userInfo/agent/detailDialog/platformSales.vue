<template>
  <div class="sale-container">
    <el-button class="add-sale" size="small" type="primary" @click="handleAdd">添加平台销售</el-button>
    <BaseTable :columns="columns" :loadingFlag="loadingFlag" :showPagination="false" :tableData="tableData"
               height="200px"
    >
      <template #operate="{row}">
        <el-button class="btn" size="small" type="text" @click="handleEdit(row)">编辑</el-button>
        <el-button class="btn" size="small" text type="danger" @click="handleDel(row)">删除</el-button>
      </template>
    </BaseTable>
    <el-dialog
        v-model="dialogVisible"
        :title="formData.id ? '编辑' : '新增'"
        :width="550"
        append-to-body
        custom-class="my-dialog"
        @close="closeDialog"
    >
      <el-form ref="formRef" :model="formData" :rules="rules" label-width="100px">
        <el-form-item label="平台销售：" prop="saleId">
          <el-select v-model="formData.saleId" :disabled="!!formData.id" placeholder="选择平台销售"
                     @change="handleChange">
            <el-option
                v-for="item in saleOptions"
                :key="item.id"
                :label="item.salesName"
                :value="item.id"
            >
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="渠道二维码:" prop="channelQrcodeImgId">
          <uploadImg :beforeRemove="() => false"
                     :fileList="fileList"
                     :showFileSize="false"
                     hideDelete
                     @on-success="handleAvatarSuccess"
                     @on-remove="handleRemove"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取 消</el-button>
          <el-button type="primary" @click="handelConfirm">确 定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import { ElMessageBox } from 'element-plus'
import api from '@/utils/request-api'
import BaseTable from '@/components/table/index.vue'
import uploadImg from '../components/upload-img.vue'

const props = defineProps({
  agentId: {
    type: String,
    required: true
  }
})

const dialogVisible = ref(false)
const loadingFlag = ref(false)
const fileList = ref([])
const saleOptions = ref([])
const tableData = ref([])
const formRef = ref(null)

const formData = reactive({
  agentId: '',
  saleId: '',
  channelQrcodeImgId: '',
  id: ''
})

const columns = [
  {
    prop: 'id',
    label: '平台销售ID'
  },
  {
    prop: 'salesName',
    label: '平台销售名'
  },
  {
    prop: 'phone',
    label: '平台销售手机号码'
  },
  {
    prop: 'operate',
    slotName: 'operate',
    width: 120,
    label: '操作'
  }
]

const rules = {
  saleId: [
    {required: true, message: '请选择平台销售', trigger: 'blur'}
  ],
  channelQrcodeImgId: [
    {required: true, message: '请上传平台销售图片', trigger: 'change'}
  ]
}

const getDataList = async () => {
  loadingFlag.value = true
  const res = await api.user.platformSaleList({agentId: props.agentId})
  if (res.code === 0) {
    tableData.value = res.data ? res.data : []
    loadingFlag.value = false
  }
}

const handleAdd = () => {
  dialogVisible.value = true
}

const handleEdit = (row) => {
  dialogVisible.value = true
  formData.id = row.id
  nextTick(() => {
    getPlatformSales(row.id)
  })
}

const handleDel = (row) => {
  ElMessageBox.confirm('此操作将永久删除该平台销售, 是否继续?', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    await api.user.delFormSaleList({id: row.id})
    ElMessage({
      type: 'success',
      message: '删除成功!'
    })
    await getDataList()
  }).catch(() => {
  })
}

const closeDialog = () => {
  formRef.value.resetFields()
  formData.id = ''
  fileList.value = []
}

const handelConfirm = async () => {
  await formRef.value.validate()
  formData.agentId = props.agentId
  if (formData.id) {
    await api.user.updateFormSaleList(formData)
    ElMessage.success('编辑成功')
  } else {
    await api.user.addFormSaleList(formData)
    ElMessage.success('新增成功')
  }
  await getDataList()
  dialogVisible.value = false
}

const getPlatformSales = async (id) => {
  const res = await api.user.getFormSaleList({id})
  if (res.code === 0) {
    formData.agentId = res.data.agentId
    formData.saleId = res.data.saleId
    formData.channelQrcodeImgId = res.data.channelQrcodeImg.id
    fileList.value.push({
      url: res.data.channelQrcodeImg.url
    })
  }
}

const handleChange = (id) => {
  const item = saleOptions.value.find(item => item.id === id)
  if (item.qrcodeImgId && item.qrcodeImgId !== '0') {
    fileList.value = []
    formData.channelQrcodeImgId = item.qrcodeImgId
    fileList.value.push({
      url: item.qrcodeImgInfo.url
    })
    formRef.value.validateField('channelQrcodeImgId')
  } else {
    fileList.value = []
    formData.channelQrcodeImgId = ''
  }
}

const handleAvatarSuccess = (response, file, fileListParam) => {
  if (response.code === 0) {
    formData.channelQrcodeImgId = response.data.id
    fileList.value = fileListParam
    formRef.value.validateField('channelQrcodeImgId')
  } else {
    ElMessage.error(response.msg)
    fileList.value = []
  }
}

const handleRemove = (file, fileListParam) => {
  fileList.value = fileListParam
  formData.channelQrcodeImgId = ''
}

onMounted(async () => {
  await getDataList()
  const res = await api.sales.list({limit: -1})
  if (res.code === 0) {
    saleOptions.value = res.data.list
  }
})
</script>

<style scoped>
:deep(.my-dialog) {
  min-width: 400px !important;
}

.add-sale {
  margin-bottom: 10px;
}

.btn {
  padding: 5px;
}
</style>

