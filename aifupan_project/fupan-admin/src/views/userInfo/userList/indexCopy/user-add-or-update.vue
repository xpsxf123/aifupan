<template>
  <my-dialog
      v-model="visible"
      :close-on-click-modal="false"
      :title="!dataFormData.id ? '新增用户' : '修改用户'"
      width="1200px"
  >
    <el-form ref="dataForm" :model="dataFormData" :rules="dataRule" class="user-form" label-width="120px">
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="头像">
            <el-upload
                :action="uploadUrl"
                :before-upload="beforeAvatarUpload"
                :on-success="handleAvatarSuccess"
                :show-file-list="false"
                class="avatar-uploader"
            >
              <el-image v-if="dataFormData.avatar" :src="dataFormData.avatar" class="avatar"/>
              <el-icon v-else class="avatar-uploader-icon">
                <Plus/>
              </el-icon>
            </el-upload>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="用户类型" prop="userType">
            <el-select v-model="dataFormData.userType" placeholder="请选择用户类型" style="width: 100%">
              <el-option
                  v-for="item in userTypeOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="手机号" prop="phone">
            <el-input v-model="dataFormData.phone" placeholder="请输入手机号"/>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="登录账号" prop="username">
            <el-input v-model="dataFormData.username" placeholder="请输入登录账号"/>
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="昵称" prop="nickName">
            <el-input v-model="dataFormData.nickName" placeholder="请输入昵称"/>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="真实姓名" prop="realName">
            <el-input v-model="dataFormData.realName" placeholder="请输入真实姓名"/>
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="邮箱" prop="email">
            <el-input v-model="dataFormData.email" placeholder="请输入邮箱"/>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="性别" prop="gender">
            <el-select v-model="dataFormData.gender" placeholder="请选择性别" style="width: 100%">
              <el-option :value="1" label="男"/>
              <el-option :value="2" label="女"/>
              <el-option :value="0" label="未知"/>
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="交易类型" prop="tradeType">
            <el-cascader
                v-model="dataFormData.tradeType"
                :options="tradeTreeList"
                :props="{ checkStrictly: true, value: 'id', label: 'name', children: 'children' }"
                clearable
                placeholder="请选择交易类型"
                style="width: 100%"
                @change="tradeChange"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="渠道" prop="channelId">
            <el-cascader
                v-model="dataFormData.channelId"
                :options="channelTreeList"
                :props="{ checkStrictly: true, value: 'id', label: 'name', children: 'children' }"
                clearable
                placeholder="请选择渠道"
                style="width: 100%"
                @change="channelChange"
            />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="销售" prop="salesId">
            <el-select v-model="dataFormData.salesId" placeholder="请选择销售" style="width: 100%">
              <el-option
                  v-for="item in salesList"
                  :key="item.id"
                  :label="item.name"
                  :value="item.id"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="状态" prop="status">
            <el-switch
                v-model="dataFormData.status"
                :active-value="1"
                :inactive-value="0"
                active-text="启用"
                inactive-text="禁用"
            />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="20">
        <el-col :span="24">
          <el-form-item label="公司信息">
            <el-input
                v-model="dataFormData.companyInfo"
                :rows="3"
                placeholder="请输入公司信息"
                type="textarea"
            />
          </el-form-item>
        </el-col>
      </el-row>

      <el-tabs v-model="activeTab" type="card">
        <el-tab-pane label="基本信息" name="basic">
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="注册时间">
                <el-date-picker
                    v-model="dataFormData.createDate"
                    format="YYYY-MM-DD HH:mm:ss"
                    placeholder="选择注册时间"
                    style="width: 100%"
                    type="datetime"
                    value-format="YYYY-MM-DD HH:mm:ss"
                />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="最后登录时间">
                <el-date-picker
                    v-model="dataFormData.lastLoginTime"
                    format="YYYY-MM-DD HH:mm:ss"
                    placeholder="最后登录时间"
                    style="width: 100%"
                    type="datetime"
                    value-format="YYYY-MM-DD HH:mm:ss"
                />
              </el-form-item>
            </el-col>
          </el-row>
        </el-tab-pane>

        <el-tab-pane label="操作记录" name="record">
          <operation-record v-if="dataFormData.id" :user-id="dataFormData.id"/>
        </el-tab-pane>
      </el-tabs>
    </el-form>

    <template #footer>
      <el-button @click="close">取消</el-button>
      <el-button type="primary" @click="dataFormSubmit">确定</el-button>
      <el-button v-if="dataFormData.id" type="warning" @click="resetPassword">重置密码</el-button>
    </template>
  </my-dialog>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/utils/request-api'
import MyDialog from '@/components/commonComponent/myDialog.vue'
import OperationRecord from '../components/operation-record.vue'

const emit = defineEmits(['refreshDataList', 'close'])

const router = useRouter()
const visible = ref(false)
const activeTab = ref('basic')
const dataForm = ref(null)
const uploadUrl = ref('/api/upload')

const dataFormData = reactive({
  id: 0,
  username: '',
  password: '',
  nickName: '',
  realName: '',
  phone: '',
  email: '',
  avatar: '',
  gender: 0,
  userType: 1,
  tradeType: [],
  channelId: [],
  salesId: '',
  status: 1,
  companyInfo: '',
  createDate: '',
  lastLoginTime: ''
})

const dataRule = reactive({
  username: [
    {required: true, message: '登录账号不能为空', trigger: 'blur'}
  ],
  nickName: [
    {required: true, message: '昵称不能为空', trigger: 'blur'}
  ],
  phone: [
    {required: true, message: '手机号不能为空', trigger: 'blur'},
    {pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur'}
  ],
  email: [
    {type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur'}
  ]
})

const userTypeOptions = ref([
  {label: '普通用户', value: 1},
  {label: 'VIP用户', value: 2},
  {label: '代理用户', value: 3}
])

const tradeTreeList = ref([])
const channelTreeList = ref([])
const salesList = ref([])

const getTradeTreeList = async () => {
  const res = await api.trade.treeList()
  if (res && res.code === 0) {
    tradeTreeList.value = res.data
  }
}

const getChannelTreeList = async () => {
  const res = await api.channel.treeList()
  if (res && res.code === 0) {
    channelTreeList.value = res.data
  }
}

const getSalesList = async () => {
  const res = await api.sales.list({limit: -1})
  if (res && res.code === 0) {
    salesList.value = res.data.list
  }
}

const tradeChange = (value) => {
  console.log('交易类型变更:', value)
}

const channelChange = (value) => {
  console.log('渠道变更:', value)
}

const handleAvatarSuccess = (res) => {
  if (res.code === 0) {
    dataFormData.avatar = res.data.url
  }
}

const beforeAvatarUpload = (file) => {
  const isJPG = file.type === 'image/jpeg' || file.type === 'image/png'
  const isLt2M = file.size / 1024 / 1024 < 2

  if (!isJPG) {
    ElMessage.error('上传头像图片只能是 JPG/PNG 格式!')
  }
  if (!isLt2M) {
    ElMessage.error('上传头像图片大小不能超过 2MB!')
  }
  return isJPG && isLt2M
}

const close = () => {
  visible.value = false
  emit('close')
}

const init = async (id) => {
  dataFormData.id = id || 0
  visible.value = true

  await getTradeTreeList()
  await getChannelTreeList()
  await getSalesList()

  await nextTick()
  dataForm.value.resetFields()

  if (dataFormData.id) {
    const res = await api.user.info({id: dataFormData.id})
    if (res && res.code === 0) {
      Object.assign(dataFormData, res.data)
    }
  }
}

const dataFormSubmit = () => {
  dataForm.value.validate(async (valid) => {
    if (valid) {
      const requestData = {...dataFormData}

      let res
      if (dataFormData.id) {
        res = await api.user.update(requestData)
      } else {
        res = await api.user.save(requestData)
      }

      if (res && res.code === 0) {
        ElMessage.success(res.msg)
        visible.value = false
        emit('refreshDataList')
        emit('close')
      } else {
        ElMessage.error(res.msg)
      }
    }
  })
}

const resetPassword = async () => {
  const confirmResult = await ElMessageBox.confirm(
      '确定要重置该用户的密码吗？',
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
  ).catch(() => false)

  if (confirmResult) {
    const res = await api.user.resetPassword({id: dataFormData.id})
    if (res && res.code === 0) {
      ElMessage.success('密码重置成功')
    } else {
      ElMessage.error(res.msg)
    }
  }
}

const handleSwitchChange = async (row) => {
  const res = await api.user.updateStatus({
    id: row.id,
    status: row.status
  })
  if (res && res.code === 0) {
    ElMessage.success('状态更新成功')
    emit('refreshDataList')
  } else {
    ElMessage.error(res.msg)
    row.status = row.status === 1 ? 0 : 1
  }
}

const getList = async () => {
  emit('refreshDataList')
}

onMounted(() => {
  getTradeTreeList()
  getChannelTreeList()
  getSalesList()
})

defineExpose({
  init
})
</script>

<style scoped>
.user-form {
  padding: 20px;
}

.avatar-uploader {
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  transition: border-color 0.2s;
}

.avatar-uploader:hover {
  border-color: #409eff;
}

.avatar-uploader-icon {
  font-size: 28px;
  color: #8c939d;
  width: 100px;
  height: 100px;
  text-align: center;
  line-height: 100px;
}

.avatar {
  width: 100px;
  height: 100px;
  display: block;
}

:deep(.el-dialog__body) {
  padding: 15px 25px !important;
}

:deep(.el-form-item__label) {
  font-weight: 500;
}

:deep(.el-tabs__header) {
  margin: 0 0 15px;
}

:deep(.el-tabs__nav-wrap::after) {
  height: 1px;
}

:deep(.el-tabs__item) {
  height: 40px;
  line-height: 40px;
}

:deep(.el-tabs__content) {
  overflow: visible;
}

:deep(.el-tab-pane) {
  overflow: visible;
}

:deep(.el-form-item) {
  margin-bottom: 15px;
}

:deep(.el-form-item__content) {
  line-height: 32px;
}

:deep(.el-input__inner) {
  height: 32px;
  line-height: 32px;
}

:deep(.el-textarea__inner) {
  min-height: 80px;
}

:deep(.el-select) {
  width: 100%;
}

:deep(.el-cascader) {
  width: 100%;
}

:deep(.el-date-editor.el-input) {
  width: 100%;
}

:deep(.el-upload--picture-card) {
  width: 100px;
  height: 100px;
  line-height: 100px;
}

:deep(.el-upload-list--picture-card .el-upload-list__item) {
  width: 100px;
  height: 100px;
}
</style>