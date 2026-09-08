<script setup>
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import BaseTable from '@/components/table/index.vue'
import uploadImg from '@/views/userInfo/agent/components/upload-img.vue'
import api from '@/utils/request-api'

const props = defineProps({
  agentId: {
    type: String,
    required: true
  },
  parentId: {
    type: String,
    required: true
  }
})

const dialogVisible = ref(false)
const loadingFlag = ref(false)
const tableData = ref([])
const total = ref(0)
const formRef = ref(null)
const uploadImgRef = ref(null)

const formData = reactive({
  agentId: props.agentId,
  promotionName: '',
  promotionStatus: 1,
  posterImgIds: '',
  btContent: '',
  btnBgColor: ''
})

const tipMsg = ref('点击选择上传图片(支持JPG/PNG,大小限制2MB;宽度800/1280px)')
const fileList = ref([])

const uploadExtraData = reactive({
  isCheckSecurity: 0
})

const columns = [
  {
    prop: 'id',
    label: '推广渠道ID'
  },
  {
    prop: 'promotionName',
    label: '推广渠道名称'
  },
  {
    prop: 'url',
    label: '专属URL链接'
  },
  {
    prop: 'promotionStatus',
    label: '状态',
    slotName: 'promotionStatus'
  },
  {
    prop: 'operate',
    slotName: 'operate',
    width: 180,
    label: '操作'
  }
]

const rules = {
  promotionName: [
    {required: true, message: '请输入推广渠道名称', trigger: 'blur'}
  ]
}

const btnContent = computed(() => {
  return formData.btContent ? formData.btContent : '示例按钮背景颜色'
})

const getDataList = async () => {
  loadingFlag.value = true
  const res = await api.user.promotionChannel({agentId: props.agentId})
  if (res.code === 0) {
    tableData.value = res.data ? res.data : []
    total.value = res.data?.totalCount
    loadingFlag.value = false
  }
  loadingFlag.value = false
}

const handleAdd = () => {
  dialogVisible.value = true
}

const handleEdit = (row) => {
  dialogVisible.value = true
  formData.id = row.id
  nextTick(async () => {
    if (row.id) {
      await getChannelDetail(row.id)
    }
  })
}

const closeDialog = () => {
  formRef.value.resetFields()
  formData.id = ''
  uploadImgRef.value.clearFileSize()
  fileList.value = []
}

const formatterCommissionRate = (row, column, cellValue) => {
  return `${(Number(cellValue) * 100).toFixed(2)}%`
}

const handelConfirm = async () => {
  await formRef.value.validate()
  if (formData.id) {
    if (!formData.btnBgColor) {
      formData.btnBgColor = ''
    }
    await api.user.updateChannel(formData)
    ElMessage.success('编辑成功')
  } else {
    await api.user.addChannel(formData)
    ElMessage.success('新增成功')
  }
  await getDataList()
  dialogVisible.value = false
}

const handleConfirm = async (row) => {
  await api.user.updateChannel({
    id: row.id,
    promotionStatus: row.promotionStatus === 1 ? 0 : 1
  })
  await getDataList()
  ElMessage.success('操作成功')
}

const copyLink = async (row) => {
  const text = `给你推荐一款最近很火的直播复盘工具，可以录同行，抓话术，拆竞品、还可以查违规，我用了非常棒，他们今天在搞免费试用的活动，你赶紧点这个链接去注册一下：${row.url}`
  if (!text) {
    ElMessage.error('链接为空，无法复制')
    return
  }

  if (navigator.clipboard && window.isSecureContext) {
    await navigator.clipboard.writeText(text)
  } else {
    const textarea = document.createElement('textarea')
    textarea.value = text
    textarea.style.position = 'fixed'
    textarea.style.opacity = '0'
    document.body.appendChild(textarea)
    textarea.select()
    const result = document.execCommand('copy')
    document.body.removeChild(textarea)
    if (!result) {
      ElMessage.error('复制失败，请手动复制')
      return
    }
  }

  ElMessage.success('复制成功！')
}

const getChannelDetail = async (id) => {
  const res = await api.user.getChannelDetail({id})
  if (res.code === 0) {
    Object.keys(formData).forEach((key) => {
      if (res.data.hasOwnProperty(key)) {
        formData[key] = res.data[key]
      }
    })
    if (res.data.posterImgList && res.data.posterImgList.length) {
      fileList.value.push({
        url: res.data.posterImgList[0].url
      })
    }
  }
}

const handleAvatarSuccess = (response, file, fileListParam) => {
  if (response.code === 0) {
    formData.posterImgIds = response.data.id
    fileList.value = fileListParam
  } else {
    ElMessage.error(response.msg)
    fileList.value = []
  }
}

const handleRemove = (file, fileListParam) => {
  fileList.value = fileListParam
  formData.posterImgIds = ''
}

const uploadUrl = () => {
  return api.common.uploadImg
}

onMounted(async () => {
  await getDataList()
})
</script>

<template>
  <div class="sale-container">
    <el-button class="add-sale" size="small" type="primary" @click="handleAdd"
    >新建子渠道
    </el-button
    >
    <BaseTable
        :columns="columns"
        :loadingFlag="loadingFlag"
        :showPagination="false"
        :tableData="tableData"
        :total="total"
        class="base-table"
        height="200px"
        showOverflowTooltip
    >
      <template #promotionStatus="{ row }">
        {{ row.promotionStatus === 1 ? '启用' : '关闭' }}
      </template>
      <template #operate="{ row }">
        <el-button class="btn" size="small" type="text" @click="handleEdit(row)"
        >编辑
        </el-button
        >
        <el-button class="btn" size="small" type="text" @click="copyLink(row)"
        >复制链接
        </el-button
        >
        <el-popconfirm
            :title="row.promotionStatus === 1 ? '确定停用吗？' : '确定启用吗？'"
            style="margin-left: 10px"
            @confirm="handleConfirm(row)"
        >
          <template #reference>
            <el-button v-if="row.promotionStatus === 0" class="btn" type="text"
            >启用
            </el-button
            >
            <el-button
                v-if="row.promotionStatus === 1"
                class="btn"
                style="color: red"
                type="text"
            >停用
            </el-button
            >
          </template>
        </el-popconfirm>
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
      <el-form
          ref="formRef"
          :model="formData"
          :rules="rules"
          label-width="130px"
      >
        <el-form-item label="推广渠道名称：" prop="promotionName">
          <el-input
              v-model="formData.promotionName"
              placeholder="输入推广渠道名称"
          ></el-input>
        </el-form-item>
        <el-form-item label="按钮文案：" prop="btContent">
          <el-input
              v-model="formData.btContent"
              placeholder="输入按钮文案"
          ></el-input>
        </el-form-item>
        <el-form-item label="H5按钮背景颜色" prop="btnBgColor">
          <el-color-picker
              v-model="formData.btnBgColor"
              show-alpha
          ></el-color-picker>
        </el-form-item>
        <el-form-item v-if="formData.btnBgColor" label="H5示例按钮：">
          <el-button
              :style="{ backgroundColor: formData.btnBgColor, color: '#ffffff' }"
              disabled
          >{{ btnContent }}
          </el-button>
        </el-form-item>
        <el-form-item label="海报图片：">
          <uploadImg
              ref="uploadImgRef"
              :VALID_WIDTHS="[800, 1280]"
              :fileList="fileList"
              :tipMsg="tipMsg"
              :uploadExtraData="uploadExtraData"
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

<style scoped>
:deep(.el-dialog) {
  min-width: 400px;
}

.add-sale {
  margin-bottom: 10px;
}

.avatar-uploader-icon {
  height: 80px;
  width: 80px;
  color: #8c939d;
  text-align: center;
  line-height: 80px;
}

.avatar {
  height: 80px;
  width: 80px;
  display: block;
}

:deep(.base-table .el-table .cell) {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

:deep(.my-dialog) {
  min-width: 600px !important;
}

.btn {
  padding: 0 !important;
}
</style>
