<template>
  <div class="user-info-container">
    <div v-if="userId">
      <div class="user-profile-container">
        <div v-if="form.avatar" class="avatar-section">
          <el-image :src="form.avatar" class="user-avatar"></el-image>
        </div>
        <div class="info-section">
          <!-- 用户信息卡片 -->
          <el-card class="info-card">
            <template #header>
              <div class="card-header">
                <h3 class="card-title">
                  <SvgIcon
                      :icon-style="{ width: '24px', height: '24px' }"
                      name="userInfo"
                  />
                  <span>用户信息</span>
                </h3>
                <el-button
                    v-if="isSaveBut"
                    class="submit-btn"
                    size="small"
                    type="primary"
                    @click="addOrUpdateHandle()"
                >
                  <template #icon>
                    <SvgIcon name="save"/>
                  </template>
                  {{ saveTitle }}
                </el-button>
              </div>
            </template>
            <div class="info-grid">
              <div class="info-item">
                <label class="info-label">用户名</label>
                <div class="info-value">{{ form.username }}</div>
              </div>
              <div class="info-item">
                <label class="info-label">昵称</label>
                <el-input
                    v-model="form.nickName"
                    class="info-input"
                    clearable
                    placeholder="请输入昵称"
                />
              </div>
              <div class="info-item">
                <label class="info-label">手机号码</label>
                <div class="info-value">
                  <span style="margin-right: 10px">
                    {{ form.phone }}
                  </span>
                  <el-button
                      v-if="form.phone"
                      class="reset-pwd-btn"
                      type="text"
                      @click="handleCopy(form.phone)"
                  >
                    <el-icon>
                      <DocumentCopy/>
                    </el-icon>
                    点击复制
                  </el-button>
                </div>
              </div>
              <div class="info-item">
                <label class="info-label">密码</label>
                <el-button
                    class="reset-pwd-btn"
                    style="color: #f56c6c"
                    type="text"
                    @click="resetPassword"
                >
                  <el-icon>
                    <Refresh/>
                  </el-icon>
                  重置密码
                </el-button>
              </div>
              <div class="info-item">
                <label class="info-label responsive-select">性别</label>
                <el-select
                    v-model="form.sex"
                    class="info-select custom-width"
                    clearable
                    placeholder="请选择性别"
                >
                  <el-option
                      v-for="item in userDict.sex"
                      :key="item.value"
                      :label="item.label"
                      :value="item.value"
                  />
                </el-select>
              </div>
              <div class="info-item">
                <label class="info-label">邮箱</label>
                <el-input
                    v-model="form.email"
                    class="info-input"
                    clearable
                    placeholder="请输入邮箱"
                />
              </div>
              <div class="info-item">
                <label class="info-label">当前版本</label>
                <div class="info-value">
                  <span class="crystal-badge">
                    <el-icon><CollectionTag/></el-icon>
                    <span>{{ form.packageName }}</span>
                  </span>
                </div>
              </div>
              <div class="info-item">
                <label class="info-label">激活状态</label>
                <el-switch
                    v-model="value"
                    active-color="#13ce66"
                    active-text="已激活"
                    inactive-color="#dcdfe6"
                    inactive-text="未激活"
                    @change="handleSwitchChange"
                />
              </div>
              <div class="info-item">
                <label class="info-label">归属行业</label>
                <el-cascader
                    :key="cascaderNum"
                    v-model="form.tradeId"
                    :options="tradeTreeList"
                    :props="{
                    checkStrictly: true,
                    value: 'id',
                    label: 'name',
                    emitPath: false,
                  }"
                    :value="form.tradeId"
                    class="info-cascader"
                    clearable
                    filterable
                    placeholder="请选择行业"
                    style="width: 100%"
                    @change="tradeChange"
                />
              </div>
              <div class="info-item">
                <label class="info-label">注册时间</label>
                <div class="info-value">{{ form.registerDate }}</div>
              </div>
              <div class="info-item">
                <label class="info-label">账号类型</label>
                <div class="info-value">
                  {{ getLabel(userDict.userType, form.userType) }}
                </div>
              </div>
              <div class="info-item">
                <label class="info-label"> 父账号</label>
                <div class="info-value">
                  <span
                      v-if="!form.parentId || form.parentId == 0"
                      class="no-data"
                  >空</span
                  >
                  <el-button
                      v-else
                      class="parent-account-btn"
                      icon="View"
                      type="text"
                      @click="refresh(form.parentId)"
                  >
                    点击查看
                  </el-button>
                </div>
              </div>
              <div class="info-item">
                <label class="info-label">用户来源渠道</label>
                <el-cascader
                    :key="cascaderNum"
                    v-model="form.channelId"
                    :options="channelTreeList"
                    :props="{
                    checkStrictly: true,
                    value: 'id',
                    label: 'channelName',
                    emitPath: false,
                  }"
                    class="info-cascader"
                    clearable
                    filterable
                    placeholder="请选择来源渠道"
                    style="width: 100%"
                    @change="chaneelChange"
                />
              </div>
              <div class="info-item">
                <label class="info-label">跟进销售姓名</label>
                <!--                <el-cascader-->
                <!--                    :disabled="hasFollowSalesNamePermission"-->
                <!--                    v-model="form.saleId"-->
                <!--                    :options="salesList"-->
                <!--                    :key="cascaderNum"-->
                <!--                    class="info-cascader"-->
                <!--                    :props="{ checkStrictly: true, value: 'id', label: 'salesName' }"-->
                <!--                    filterable-->
                <!--                    @change="salesChange"-->
                <!--                    placeholder="请选择销售人员"-->
                <!--                    clearable-->
                <!--                />-->
                <el-select
                    v-model="form.saleId"
                    :disabled="hasFollowSalesNamePermission"
                    class="info-select"
                    clearable
                    placeholder="请选择销售人员"
                    style="margin-right: 2px"
                >
                  <el-option
                      v-for="item in salesList"
                      :key="item.id"
                      :label="item.salesName"
                      :value="item.id"
                  />
                </el-select>
                <el-button :disabled="disCodeBtn" :icon="Promotion" text type="primary" @click="throttledSendCode">
                  {{ disCodeBtn ? `${codeTime}秒后再发送` : '发送获客短信' }}
                </el-button>
              </div>
              <div class="info-item">
                <label class="info-label">用户微信昵称</label>
                <el-input
                    v-model="form.wxName"
                    class="info-input"
                    clearable
                    placeholder="请输入微信昵称"
                />
              </div>
              <div class="info-item">
                <label class="info-label">代理商销售</label>
                <el-select
                    v-model="form.agentSaleId"
                    :disabled="!form.channelId"
                    :placeholder="agentSalesPlaceholder"
                    class="info-select"
                    clearable
                >
                  <el-option
                      v-for="item in agentSaleListOptions"
                      :key="item.id"
                      :label="item.saleName"
                      :value="item.id"
                  >
                  </el-option>
                </el-select>
              </div>
              <div class="info-item">
                <label class="info-label">是否已演示</label>
                <el-select
                    v-model="form.isShow"
                    class="info-select"
                    clearable
                    placeholder="请选择是否已演示"
                >
                  <el-option
                      v-for="item in demoOptions"
                      :key="item.value"
                      :label="item.label"
                      :value="item.value"
                  />
                </el-select>
              </div>
              <div class="info-item">
                <label class="info-label">腾讯会议地址</label>
                <el-input
                    v-model="form.videoMeetPath"
                    class="info-input"
                    clearable
                    placeholder="请输入腾讯会议地址"
                />
              </div>
              <div class="info-item">
                <label class="info-label">客户意向等级</label>
                <el-select
                    v-model="form.userAmbition"
                    class="info-select"
                    clearable
                    placeholder="请选择客户意向等级"
                >
                  <el-option
                      v-for="item in intentionOptions"
                      :key="item.value"
                      :label="item.label"
                      :value="item.value"
                  />
                </el-select>
              </div>
              <div class="info-item">
                <label class="info-label">客户类型</label>
                <el-select
                    v-model="form.userBelongType"
                    class="info-select"
                    clearable
                    placeholder="请选择客户类型"
                >
                  <el-option
                      v-for="item in customerTypeOptions"
                      :key="item.value"
                      :label="item.label"
                      :value="item.value"
                  />
                </el-select>
              </div>
              <div class="info-item">
                <label class="info-label">是否登录过</label>
                <div class="info-value">
                  <el-tag
                      :type="form?.isLoggedIn === 1 ? 'success' : 'danger'"
                      size="small"
                  >
                    {{ loginStatus }}
                  </el-tag>
                </div>
              </div>
            </div>
          </el-card>
          <!-- 公司信息卡片 -->
          <el-card class="company-card">
            <div class="card-header">
              <!--  <h3 class="card-title">🏢 公司信息</h3> -->
              <h3 class="card-title">
                <SvgIcon
                    :icon-style="{ width: '24px', height: '24px' }"
                    name="company"
                />
                <span>用户信息</span>
              </h3>
              <el-button
                  v-if="isSaveCompany"
                  class="submit-btn"
                  size="small"
                  type="primary"
                  @click="addOrUpdateHandle()"
              >
                <template #icon>
                  <SvgIcon name="save"/>
                </template>
                {{ saveTitle }}
              </el-button>
            </div>

            <div class="info-grid">
              <!-- 第一行 -->
              <div class="info-item">
                <label class="info-label">公司名称</label>
                <el-input
                    v-model="companyForm.name"
                    class="info-input"
                    clearable
                    placeholder="请输入公司名称"
                />
              </div>

              <div class="info-item">
                <label class="info-label">公司规模</label>
                <el-input
                    v-model="companyForm.scales"
                    class="info-input"
                    clearable
                    placeholder="请输入公司人数"
                />
              </div>

              <!-- 第二行 -->
              <div class="info-item">
                <label class="info-label">联系人</label>
                <el-input
                    v-model="companyForm.linkman"
                    class="info-input"
                    clearable
                    placeholder="请输入联系人"
                />
              </div>

              <div class="info-item">
                <label class="info-label">职位</label>
                <el-input
                    v-model="companyForm.position"
                    class="info-input"
                    clearable
                    placeholder="请输入职位"
                />
              </div>

              <!-- 第三行 -->
              <div class="info-item">
                <label class="info-label">联系电话</label>
                <el-input
                    v-model="companyForm.phones"
                    class="info-input"
                    clearable
                    placeholder="请输入联系电话"
                />
              </div>

              <div class="info-item">
                <label class="info-label">公司地址</label>
                <el-input
                    v-model="companyForm.address"
                    class="info-input"
                    clearable
                    placeholder="请输入公司地址"
                />
              </div>
            </div>
          </el-card>
        </div>
      </div>
      <!-- 标签页区域 -->
      <div class="tab-section">
        <el-tabs v-model="activeName" type="border-card">
          <el-tab-pane label="用户资产" lazy name="first">
            <property :userId="userid"></property>
          </el-tab-pane>
          <el-tab-pane label="跟进记录" lazy name="userRemark">
            <user-remark :user-id="userid"></user-remark>
          </el-tab-pane>
          <el-tab-pane label="全部主播记录" lazy name="second">
            <anchorRecord :userId="userid"></anchorRecord>
          </el-tab-pane>
          <el-tab-pane label="当前添加主播" lazy name="addAnchorRecord">
            <addAnchorRecord :user-id="userid"></addAnchorRecord>
          </el-tab-pane>
          <el-tab-pane label="录制记录" lazy name="third">
            <anchorVideoRecord :userId="userid"></anchorVideoRecord>
          </el-tab-pane>
          <el-tab-pane label="录制分析" lazy name="fourth">
            <analysisVideo :userId="userid"></analysisVideo>
          </el-tab-pane>
          <el-tab-pane label="文件上传分析" lazy name="file">
            <analysisFile :userId="userid"></analysisFile>
          </el-tab-pane>
          <el-tab-pane label="订单记录" lazy name="orderDetails">
            <orderDetails :user-id="userid"></orderDetails>
          </el-tab-pane>
          <el-tab-pane label="子账号记录" lazy name="childUserDetails">
            <childUserDetails :user-id="userid"></childUserDetails>
          </el-tab-pane>
          <el-tab-pane label="登录记录" name="loginLog">
            <login-log-list :user-id="userid"></login-log-list>
          </el-tab-pane>
          <el-tab-pane label="修改记录" lazy name="operationRecord">
            <operation-record
                ref="operationRecordRef"
                :user-id="userid"
            ></operation-record>
          </el-tab-pane>
        </el-tabs>
      </div>
      <!--密码提示弹窗-->
      <el-dialog
          v-model="passWordDialogVisible"
          title="密码提示"
          width="500px"
      >
        <p>密码已成功重置为 <span class="rest-password-class">{{ restPassWord }}</span> ，请及时修改密码！</p>
        <template #footer>
          <div class="dialog-footer">
            <el-button type="primary" @click="handleKnow">
              我已知晓
            </el-button>
          </div>
        </template>
      </el-dialog>
    </div>
    <el-empty v-else :image-size="200" description="请选择用户"></el-empty>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted, nextTick } from 'vue'
import property from '@/views/userInfo/userList/components/property.vue'
import orderDetails from '@/views/userInfo/userList/components/orderDetails.vue'
import childUserDetails from '@/views/userInfo/userList/components/childUserDetails.vue'
import anchorRecord from '@/views/userInfo/userList/components/anchorRecord.vue'
import anchorVideoRecord from '@/views/userInfo/userList/components/anchorVideoRecord.vue'
import analysisVideo from '@/views/userInfo/userList/components/analysisVideo.vue'
import analysisFile from '@/views/userInfo/userList/components/analysisFile.vue'
import addAnchorRecord from '@/views/userInfo/userList/components/addAnchorRecord.vue'
import LoginLogList from '@/views/userInfo/userList/components/loginLogList.vue'
import userRemark from '@/views/userInfo/userList/components/userRemark.vue'
import OperationRecord from '@/views/userInfo/userList/components/operation-record.vue'
import { Promotion } from '@element-plus/icons-vue'
import emitter from '@/utils/emitter'
import { throttle } from 'lodash'
import api from '@/utils/request-api'
import { useDict } from '@/hooks/useDict'
import { useBtnPermission } from '@/hooks/useBtnPermission'
import { useRoute } from 'vue-router'
import { useUserInfoStore } from '@/store'
import { storeToRefs } from 'pinia'

const {breadcrumbList} = storeToRefs(useUserInfoStore())

const props = defineProps({
  userId: {
    type: String
  }
})
const addUserDetailBreadcrumb = () => {
  const exists = breadcrumbList.value.some((item) => item.name === '用户详情')
  if (!exists) {
    breadcrumbList.value.push({
      name: '用户详情',
      url: '/userInfo/userDeatail',
      id: '/userInfo/userDeatail'
    })
  }
}
addUserDetailBreadcrumb()
const emit = defineEmits(['close', 'refreshDataList'])

const route = useRoute()
const {userDict, getLabel} = useDict()
const {checkPermission} = useBtnPermission()

const isSaveBut = ref(false)
const isSaveCompany = ref(false)
const activeName = ref('first')
const userid = ref(null)
const value = ref(true)
const visible = ref(false)
const tradeTreeList = ref([])
const channelTreeList = ref([])
const salesList = ref([])
const agentSaleListOptions = ref([])
const cascaderNum = ref(0)
let passWordDialogVisible = ref(false)
let restPassWord = ref('')
const form = reactive({
  tradeId: null,
  sex: null,
  company: {},
  anchorType: 1,
  nickName: '',
  agentSaleId: '',
  isShow: '',
  userAmbition: '',
  videoMeetPath: '',
  userBelongType: ''
})

const companyForm = reactive({
  id: '',
  name: '',
  scales: '',
  linkman: '',
  position: '',
  phones: '',
  address: ''
})

const demoOptions = [
  {
    value: 1,
    label: '是'
  },
  {
    value: 0,
    label: '否'
  }
]

const intentionOptions = [
  {
    value: 'S',
    label: 'S 级'
  },
  {
    value: 'A',
    label: 'A 级'
  },
  {
    value: 'B',
    label: 'B 级'
  },
  {
    value: 'C',
    label: 'C 级'
  }
]

const customerTypeOptions = [
  {
    value: 0,
    label: '个人'
  },
  {
    value: 1,
    label: '工作室'
  },
  {
    value: 2,
    label: '企业'
  }
]

const enumFields = [
  'name',
  'scales',
  'linkman',
  'position',
  'phones',
  'address'
]
const disCodeBtn = ref(false)
const codeTime = ref(60)
const loginStatus = computed(() => {
  const val = form?.isLoggedIn
  if (val === 0) return '未登录过'
  if (val === 1) return '已登录过'
  return '未登录过'
})

const hasFollowSalesNamePermission = computed(() => {
  return !checkPermission('跟进销售姓名', route.path)
})

const saveTitle = computed(() => {
  if (isSaveCompany.value && isSaveBut.value) {
    return '保存用户及公司信息'
  } else {
    return '保存'
  }
})

const agentSalesPlaceholder = computed(() => {
  if (form.channelId) {
    return '请选择代理商销售'
  } else {
    return '请先选择用户来源渠道'
  }
})
watch(
    form,
    (newVal, oldVal) => {
      isSaveBut.value = true
    },
    {deep: true}
)

watch(
    companyForm,
    (newVal, oldVal) => {
      isSaveCompany.value = true
    },
    {deep: true}
)
const tradeChange = (val) => {
  if (!val) {
    cascaderNum.value++
  }
}

const getTradeTreeList = async () => {
  tradeTreeList.value = []
  const res = await api.trade.listTree({})
  if (res && res.code === 0) {
    tradeTreeList.value = res.data
    cascaderNum.value++
  }
}

const getChannelTreeList = async () => {
  channelTreeList.value = []
  const res = await api.channel.listTree({})
  if (res && res.code === 0) {
    channelTreeList.value = res.data
    cascaderNum.value++
  }
}
const chaneelChange = (value) => {
  form.agentSaleId = ''
  agentSaleListOptions.value = []
  if (value) {
    agentSaleLists(form.channelId)
  } else {
    cascaderNum.value++
  }
}

const getSalesList = async () => {
  salesList.value = []
  const res = await api.sales.list({limit: -1})
  if (res && res.code === 0) {
    salesList.value = res.data.list
  }
}

const salesChange = (value) => {
  if (value && value.length > 0) {
    form.saleId = value[value.length - 1]
  } else {
    form.saleId = ''
  }
}
const addOrUpdateHandle = async () => {
  if (!form?.nickName || form?.nickName?.length < 2) {
    ElMessage.error('昵称不能少于2位')
    return
  }
  const flag = hasTargetValue(form, enumFields)
  if (form.id || flag) {
    form.anchorType = 1
  } else {
    form.anchorType = 0
  }
  let data = {
    id: form.id,
    userId: form.userId,
    sex: form.sex,
    email: form.email ?? '',
    tradeId: form.tradeId,
    channelId: form.channelId,
    saleId: form.saleId,
    wxName: form.wxName,
    position: companyForm.position,
    anchorType: form.anchorType,
    nickName: form.nickName ?? '',
    isShow: form.isShow,
    videoMeetPath: form.videoMeetPath,
    userAmbition: form.userAmbition,
    userBelongType: form.userBelongType,
    agentSaleId: form.agentSaleId,
    company: {
      id: companyForm.id,
      name: companyForm.name ?? '',
      scales: companyForm.scales ?? '',
      linkman: companyForm.linkman ?? '',
      phones: companyForm.phones ?? '',
      address: companyForm.address ?? ''
    }
  }
  const res = await api.userdetails.saveUserDetails(data)
  if (res && res.code === 0) {
    ElMessage.success('保存成功')
    isSaveBut.value = false
    isSaveCompany.value = false
    emitter.emit('refreshData')
  }
  if (operationRecordRef.value) {
    await operationRecordRef.value.getUserOperation()
  }
}
const resetPassword = () => {
  ElMessageBox.confirm('是否重置密码?', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    const res = await api.user.resetPassword({id: userid.value})
    if (res && res.code === 0) {
      restPassWord.value = res.data
      passWordDialogVisible.value = true
    }
  })
}
const handleKnow = () => {
  passWordDialogVisible.value = false
  ElMessage.success('密码重置成功')
}
const handleSwitchChange = async () => {
  try {
    await ElMessageBox.confirm('确定要修改该用户状态吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch (e) {
    // 取消后回滚开关状态
    value.value = !value.value
    ElMessage.info('已取消操作')
    return
  }

  const data = {userId: userid.value, status: value.value ? 0 : 1}
  const res = await api.user.updateUser(data)
  if (res && res.code === 0) {
    ElMessage.success('操作成功')
    emit('refreshDataList')
    if (operationRecordRef.value) {
      await operationRecordRef.value.getUserOperation()
    }
  }
}
const getList = async () => {
  value.value = false
  const res = await api.user.userDetailByUserId({userId: userid.value})
  if (res && res.code === 0) {
    if (res.data.channelId && Number(res.data.channelId)) {
      await agentSaleLists(res.data.channelId)
    }
    res.data.company = res.data.company ? res.data.company : {}
    res.data.company.position = res.data.position ?? ''
    Object.assign(companyForm, JSON.parse(JSON.stringify(res.data.company)))
    Object.assign(form, res.data)
    await nextTick(() => {
      isSaveBut.value = false
      isSaveCompany.value = false
    })
    value.value = form?.userStatus === 0
  } else {
    Object.assign(form, {})
    value.value = false
  }
}
const init = async (id) => {
  userid.value = id
  await Promise.all([
    getList(),
    getTradeTreeList(),
    getChannelTreeList(),
    getSalesList()
  ])
  visible.value = true
}

const refresh = (id) => {
  userid.value = id
  getList()
}

const hasTargetValue = (obj, keys) => {
  for (const key in obj) {
    const value = obj[key]
    if (value !== null && typeof value === 'object') {
      if (hasTargetValue(value, keys)) return true
    } else {
      if (
          keys.includes(key) &&
          value !== '' &&
          value !== null &&
          value !== undefined
      ) {
        return true
      }
    }
  }
  return false
}

const agentSaleLists = async (channelId) => {
  const res = await api.sales.agentSaleLists({channelId: channelId})
  if (res.code === 0 && res.data.length > 0) {
    agentSaleListOptions.value = res.data
  }
}

const sendCode = async () => {
  // 显示验证码已发送的提示
  await sendCodeCallback()
  disCodeBtn.value = true
  codeTime.value = 60
  let timer = setInterval(() => {
    if (--codeTime.value <= 0) {
      clearInterval(timer)
      disCodeBtn.value = false
    }
  }, 1000)
}
// 发送获客短信的回调
const sendCodeCallback = async () => {
  await api.user.sendCustomerAcquisitionMsg({
    userId: props.userId
  })
  ElMessage.success('短信发送成功')
}
// 使用节流函数（3秒只能触发一次）
const throttledSendCode = throttle(() => {
  sendCode()
}, 3000)
const handleCopy = async (phone) => {
  try {
    if (!phone) {
      throw new Error('手机号为空')
    }
    const text = phone
    if (navigator.clipboard && window.isSecureContext) {
      await navigator.clipboard.writeText(text)
    } else {
      // 使用旧方法兜底
      const textarea = document.createElement('textarea')
      textarea.value = text
      textarea.style.position = 'fixed'
      textarea.style.opacity = '0'
      document.body.appendChild(textarea)
      textarea.select()
      document.execCommand('copy')
      document.body.removeChild(textarea)
    }

    ElMessage.success('复制成功')
  } catch (err) {
    if (err.message === '手机号为空') {
      ElMessage.error('没手机号为空')
    } else {
      ElMessage.error('复制失败: ' + err.message)
    }
  }
}

const operationRecordRef = ref(null)

// 生命周期
onMounted(() => {
  if (props.userId) {
    init(props.userId)
  }
})
// 暴露给父组件的方法
defineExpose({
  refresh
})
</script>

<style lang="scss" scoped>
:deep(.el-card__header) {
  border: none;
}

.el-card {
  :deep(.el-card__header) {
    padding: 20px 20px 0;
  }

  :deep(.el-card__body) {
    padding: 0 20px 20px;
  }
}

.user-info-container {
  background-color: #f5f7fa;
  border-radius: 8px;

  .user-profile-container {
    display: flex;
    margin-bottom: 20px;

    .avatar-section {
      margin-right: 20px;
      flex-shrink: 0;

      .user-avatar {
        width: 120px;
        height: 120px;
        border-radius: 50%;
        border: 2px solid #e6e6e6;
        box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
        background-color: #f2f6fc;
        display: flex;
        align-items: center;
        justify-content: center;

        :deep(.el-image__inner) {
          border-radius: 50%;
        }

        .el-icon-user {
          font-size: 50px;
          color: #c0c4cc;
        }
      }
    }

    .info-section {
      flex: 1;

      .info-card,
      .company-card {
        margin-bottom: 20px;
        border-radius: 8px;
        box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);

        .card-header {
          display: flex;
          justify-content: space-between;
          align-items: center;
          margin-bottom: 15px;
          padding-bottom: 10px;
          border-bottom: 1px solid #ebeef5;

          .card-title {
            display: flex;
            align-items: flex-end;
            gap: 5px;
            margin: 0;
            font-size: 18px;
            color: #303133;
            font-weight: 500;

            span {
              line-height: 1; /* ✅ 去掉多余行高 */
            }
          }

          .submit-btn {
            padding: 7px 15px;
          }
        }

        .info-grid {
          display: grid;
          grid-template-columns: repeat(2, 1fr);
          gap: 12px 20px;

          .info-item {
            display: flex;
            align-items: center;
            min-height: 36px;

            .info-label {
              width: 100px;
              color: #606266;
              font-size: 13px;
              flex-shrink: 0;
              text-align: right;
              padding-right: 12px;
              font-weight: 500;
            }

            .info-value {
              flex: 1;
              color: #606266;
              word-break: break-word;
              font-size: 13px;

              .no-data {
                color: #c0c4cc;
              }
            }

            .info-input,
            .info-select,
            .info-cascader {
              flex: 1;

              :deep(.el-input__inner) {
                height: 32px;
                line-height: 32px;
                font-size: 13px;
              }

              :deep(.el-input__icon) {
                line-height: 32px;
              }
            }

            .reset-pwd-btn,
            .parent-account-btn {
              padding: 0;
              color: #409eff;
              font-size: 13px;

              i {
                margin-right: 5px;
              }
            }
          }
        }
      }

      .company-card {
        padding-top: 20px;
      }
    }
  }

  .tab-section {
    :deep(.el-tabs--border-card) {
      border-radius: 8px;
      box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
      overflow: hidden;

      > .el-tabs__header {
        background-color: #f5f7fa;
        border-bottom: 1px solid #e4e7ed;
        border-radius: 8px 8px 0 0;

        .el-tabs__item {
          border-right: 1px solid #e4e7ed;
          color: #909399;
          font-size: 13px;
          height: 40px;
          line-height: 40px;
          padding: 0 20px;

          &.is-active {
            color: #409eff;
            background-color: #fff;
            border-bottom-color: #fff;
            font-weight: 500;
          }
        }
      }
    }
  }
}

@media (max-width: 800px) {
  .info-grid {
    grid-template-columns: 1fr !important;
  }

  .user-profile-container {
    flex-direction: column;

    .avatar-section {
      margin-right: 0;
      margin-bottom: 20px;
      display: flex;
      justify-content: center;
    }
  }
}

.crystal-badge {
  display: inline-flex;
  align-items: center;
  padding: 6px 12px; /* 更紧凑的尺寸 */
  background: linear-gradient(
          135deg,
          rgba(64, 158, 255, 0.9),
          rgba(103, 194, 58, 0.9)
  );
  color: white;
  font-size: 13px; /* 稍小的字号 */
  font-weight: 600;
  border-radius: 6px; /* 更小的圆角 */
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.3),
    /* 更柔和的阴影 */
  inset 0 1px 1px rgba(255, 255, 255, 0.2);
  position: relative;
  overflow: hidden;
  line-height: 1; /* 更紧凑的行高 */
}

.crystal-badge::before {
  content: '';
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: linear-gradient(
          to bottom right,
          rgba(255, 255, 255, 0) 0%,
          rgba(255, 255, 255, 0.2) 50%,
          rgba(255, 255, 255, 0) 100%
  );
  transform: rotate(30deg);
}

:deep(.el-tabs .el-tabs__nav-next),
:deep(.el-tabs .el-tabs__nav-prev) {
  display: flex;
  place-items: center;
  height: 100%;
  font-size: 20px;
  color: black;
  background-color: #2D8CF0;
}

.crystal-badge i {
  margin-right: 6px; /* 更小的图标间距 */
  font-size: 14px; /* 稍小的图标 */
  text-shadow: 0 1px 1px rgba(0, 0, 0, 0.2); /* 更柔和的文字阴影 */
}

.crystal-badge span {
  position: relative;
  z-index: 1;
  white-space: nowrap; /* 防止文字换行 */
}

/* 淡入淡出动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter,
.fade-leave-to {
  opacity: 0;
}

/* 或者滑动动画 */
.slide-fade-enter-active {
  transition: all 0.3s ease;
}

.slide-fade-leave-active {
  transition: all 0.3s cubic-bezier(1, 0.5, 0.8, 1);
}

.rest-password-class {
  color: rgb(245, 108, 108);
}

.slide-fade-enter,
.slide-fade-leave-to {
  transform: translateX(10px);
  opacity: 0;
}
</style>
