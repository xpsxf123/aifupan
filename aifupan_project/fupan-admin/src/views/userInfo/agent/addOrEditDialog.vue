<template>
  <el-dialog
    v-model="dialogVisible"
    :title="formData.id ? '修改' : '新增'"
    custom-class="my-dialog"
    modal-append-to-body
    top="5vh"
    @close="handelClose"
    :width="700"
  >
    <el-form ref="formRef" :model="formData" :rules="rules" label-width="140px">
      <el-form-item label-width="30px" style="margin-bottom: 0">
        <h3>基础信息</h3>
      </el-form-item>
      <el-form-item label="代理商名称：" prop="channelId">
        <el-cascader
          :key="cascaderNum"
          v-model="formData.channelId"
          :options="AgentTreeList"
          :props="{
            checkStrictly: true,
            value: 'id',
            label: 'channelName',
            emitPath: false,
          }"
          clearable
          filterable
          placeholder="选择代理商名称"
          style="width: 100%"
          @change="tradeAgentChange"
        >
        </el-cascader>
      </el-form-item>
      <el-form-item label="代理商类型" prop="agentType">
        <el-select
          v-model="formData.agentType"
          placeholder="选择代理商类型"
          style="width: 100%"
        >
          <el-option
            v-for="item in agentTypeOptions"
            :key="`agent-type-${item.value}`"
            :label="item.label"
            :value="item.value"
          >
          </el-option>
        </el-select>
      </el-form-item>
      <el-form-item label="平台运营：" prop="operationUserId">
        <el-select
          v-model="formData.operationUserId"
          placeholder="选择平台运营"
          style="width: 100%"
        >
          <el-option
            v-for="(item, index) in operationUsers"
            :key="`operation-${item.id}-${index}`"
            :label="item.nickName"
            :value="item.id"
          >
          </el-option>
        </el-select>
      </el-form-item>
      <el-form-item label="代理商行业：" prop="tradeId">
        <el-cascader
          :key="cascaderNum"
          v-model="formData.tradeId"
          :options="tradeTreeList"
          :props="{
            checkStrictly: true,
            value: 'id',
            label: 'name',
            emitPath: false,
          }"
          clearable
          filterable
          placeholder="选择代理商行业"
          style="width: 100%"
        >
        </el-cascader>
      </el-form-item>
      <el-form-item label="联系人姓名：" prop="contactName">
        <el-input
          v-model="formData.contactName"
          placeholder="输入联系人姓名"
        ></el-input>
      </el-form-item>
      <el-form-item label="联系人手机号码：" prop="contactPhone">
        <el-input
          v-model="formData.contactPhone"
          placeholder="输入联系人手机号码"
        ></el-input>
      </el-form-item>
      <el-form-item label="代理商地址：" prop="contactAddress">
        <el-input
          v-model="formData.contactAddress"
          placeholder="输入代理商地址"
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
        >
          {{ btnContent }}
        </el-button>
      </el-form-item>
      <el-form-item
        label="海报图片："
        prop="posterImgIds"
        style="margin-bottom: 0"
      >
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
      <el-form-item label-width="30px" style="margin-bottom: 0">
        <h3>结算信息</h3>
      </el-form-item>
      <el-form-item label="新签佣金比例：" prop="commissionRate">
        <el-input
          v-model="formData.commissionRate"
          placeholder="输入新签佣金比例"
        >
          <template #suffix>%</template>
        </el-input>
      </el-form-item>
      <el-form-item label="续费佣金比例：" prop="renewalCommissionRate">
        <el-input
          v-model="formData.renewalCommissionRate"
          placeholder="输入续费佣金比例"
        >
          <template #suffix>%</template>
        </el-input>
      </el-form-item>
    </el-form>
    <template #footer>
      <span class="dialog-footer">
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="handelConfirm">确 定</el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import emitter from '@/utils/emitter'
import uploadImg from './components/upload-img.vue'
import Big from 'big.js'
import api from '@/utils/request-api'

const props = defineProps({
  operationUsers: {
    type: Array, // 平台运营人员列表
  },
})

const emit = defineEmits(['get-dataList'])

const dialogVisible = ref(false)
const resourceType = ref('') // 文件上传的附件参数
const tipMsg = ref('点击选择上传图片(支持JPG/PNG,大小限制2MB;宽度800/1280px)')
const formRef = ref(null)
const uploadImgRef = ref(null)

const formData = reactive({
  agentName: '', // 代理商名称
  channelId: '', // 代理商ID
  agentType: '', // 代理商类型
  parentId: '', // 父代理商ID
  operationUserId: '', // 平台运营人员ID
  contactName: '', // 联系人姓名
  contactPhone: '', // 联系人手机号码
  tradeId: '', // 代理商行业
  btnBgColor: '', // 按钮背景颜色
  btContent: '', // 按钮内容
  contactAddress: '', // 代理商地址
  commissionRate: '', // 佣金比例
  renewalCommissionRate: '', // 续费佣金比例
  posterImgIds: '', // 海报图片URL
  createUserId: '', // 创建人用户id
  agentStatus: 1, // 默认为启用状态
  isClientInviteAgent: 0,
})

const fileList = ref([])
const tradeTreeList = ref([]) // 代理商行业树列表
const AgentTreeList = ref([]) // 代理商名称树列表
const cascaderNum = ref(0)

const agentTypeOptions = ref([
  {
    label: '普通代理商',
    value: 0,
  },
  {
    label: '渠道代理商',
    value: 1,
  },
]) // 代理商类型options

const uploadExtraData = ref({
  isCheckSecurity: 0,
})

const rules = ref({
  channelId: [
    { required: true, message: '请选择代理商名称', trigger: 'change' },
  ],
  operationUserId: [
    { required: true, message: '请选择平台运营', trigger: 'change' },
  ],
  tradeId: [{ required: true, message: '请选择代理商行业', trigger: 'change' }],
  contactName: [
    { required: true, message: '请输入联系人姓名', trigger: 'blur' },
  ],
  contactPhone: [
    { required: true, message: '请输入联系人手机号码', trigger: 'blur' },
    {
      trigger: 'blur', // 校验触发时机，这里是失去焦点时
      pattern: /^1[3-9]\d{9}$/, // 手机号码正则表达式
      message: '请输入有效的手机号码', // 正则校验失败时的提示信息
    },
  ],
  posterImgIds: [
    { required: true, message: '请上传海报图片', trigger: 'change' },
  ],
  commissionRate: [
    { required: true, message: '请输入新签佣金比例', trigger: 'blur' },
    {
      pattern: /^(0|[1-9]\d*)(\.\d{1,2})?$/,
      message: '请输入正确的新签佣金比例',
      trigger: 'blur',
    },
  ],
  renewalCommissionRate: [
    { required: true, message: '请输入续费佣金比例', trigger: 'blur' },
    {
      pattern: /^(0|[1-9]\d*)(\.\d{1,2})?$/,
      message: '请输入正确的续费佣金比例',
      trigger: 'blur',
    },
  ],
  agentType: [
    { required: true, message: '请输入代理商类型', trigger: 'change' },
  ],
  isClientInviteAgent: [
    {
      required: true,
      message: '请选择是否是客户邀请的代理商',
      trigger: 'change',
    },
  ],
})

const btnContent = computed(() => {
  return formData.btContent ? formData.btContent : '示例按钮背景颜色'
})

const openDialog = (rowData = {}) => {
  dialogVisible.value = true
  // 使用nextTick确保组件渲染完成后再赋值，防止清空表单失效的问题
  nextTick(() => {
    if (rowData && rowData.id) {
      getRowInfo(rowData.id) // 获取当前行信息
    }
  })
}

const handelConfirm = async () => {
  await formRef.value.validate()
  const data = JSON.parse(JSON.stringify(formData))
  data.commissionRate = new Big(data.commissionRate).div(100).toNumber()
  data.renewalCommissionRate = new Big(data.renewalCommissionRate)
    .div(100)
    .toNumber()
  if (formData.id) {
    if (!formData.btnBgColor) {
      data.btnBgColor = ''
    }
    await api.user.editAgent(data)
    ElMessage.success('修改成功')
  } else {
    await api.user.addAgent(data)
    ElMessage.success('新增成功')
  }
  emit('get-dataList')
  dialogVisible.value = false
}

const getTradeTreeList = () => {
  tradeTreeList.value = []
  api.trade.listTree({}).then((res) => {
    if (res && res.code === 0) {
      tradeTreeList.value = res.data
      cascaderNum.value++
    }
  })
}

const getAgentTreeList = () => {
  AgentTreeList.value = []
  api.channel.listTree({}).then((res) => {
    if (res && res.code === 0) {
      AgentTreeList.value = disableTopLevelOptions(res.data)
      cascaderNum.value--
    }
  })
}

const tradeAgentChange = (value) => {
  if (value) {
    formData.agentName = findNameById(
      AgentTreeList.value,
      formData.channelId,
      'channelName'
    )
  } else {
    formData.channelId = ''
    formData.agentName = ''
  }
}

const findNameById = (items, targetId, name) => {
  for (const item of items) {
    if (item.id === targetId) {
      return item[name]
    }
    if (item.children) {
      const foundName = findNameById(item.children, targetId, name)
      if (foundName) return foundName
    }
  }
  return null // 如果未找到返回 null
}

const getCurrentUserId = () => {
  formData.createUserId = JSON.parse(
    localStorage.getItem('replayVuex')
  ).loginResultData.id
}

const getRowInfo = async (id) => {
  const res = await api.user.getAgentDetail({ id })
  const rowData = res.data
  Object.keys(formData).forEach((item) => {
    if (rowData.hasOwnProperty(item)) {
      formData[item] = rowData[item]
    }
  })
  formData.commissionRate = (formData.commissionRate * 100).toFixed(2)
  formData.renewalCommissionRate = (
    formData.renewalCommissionRate * 100
  ).toFixed(2)
  formData.id = rowData.id
  fileList.value.push({
    url: rowData.posterImgList[0].url,
  })
}

const handleAvatarSuccess = (response, file, fileListParam) => {
  if (response.code === 0) {
    formData.posterImgIds = response.data.id
    fileList.value = fileListParam
    formRef.value.validateField('posterImgIds')
  } else {
    ElMessage.error(response.msg)
    fileList.value = []
    uploadImgRef.value.clearFileSize()
  }
}

const handleRemove = (file, fileListParam) => {
  fileList.value = fileListParam
  formData.posterImgIds = ''
}

const disableTopLevelOptions = (list) => {
  return list.map((item) => {
    return {
      ...item,
      disabled: true, // 禁用一级节点
      children: item.children
        ? item.children.map((child) => ({
            ...child,
            disabled: false, // 二级节点可以选
            children: child.children
              ? child.children.map((grandchild) => ({
                  ...grandchild,
                  disabled: false,
                }))
              : null,
          }))
        : null,
    }
  })
}

const handelClose = () => {
  formRef.value.resetFields()
  uploadImgRef.value.clearFileSize()
  formData.id = ''
  fileList.value = []
}

onMounted(() => {
  emitter.on('openDialog', openDialog)
  getTradeTreeList()
  getAgentTreeList()
  getCurrentUserId()
})
</script>

<style lang="scss" scoped>
h3 {
  margin: 0;
}

.hide_upload {
  :deep(.el-upload--picture-card) {
    display: none;
  }
}

:deep(.my-dialog) {
  min-width: 400px;
}
</style>
<style>
.avatar-uploader .el-upload {
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
}

.avatar-uploader .el-upload:hover {
  border-color: #409eff;
}
</style>
