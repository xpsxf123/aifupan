<template>
  <div>
    <el-dialog
      v-model="dialogVisible"
      width="900px"
      @close="handleClose"
      :title="title"
    >
      <el-form
        ref="formRef"
        :model="formData"
        label-width="130px"
        :rules="rules"
      >
        <el-form-item label="代理商:" class="form-items" prop="agentId">
          <el-select
            v-model="formData.agentId"
            class="input-items"
            size="small"
            disabled
            style="width: 250px"
          >
            <el-option
              v-for="item in agentListOptions"
              :key="item.value"
              :label="item.agentName"
              :value="item.id"
            >
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="活动名称:" class="form-items" prop="activityName">
          <el-input
            v-model="formData.activityName"
            placeholder="活动名称"
            class="input-items"
            size="small"
            style="width: 250px"
          ></el-input>
        </el-form-item>
        <el-form-item
          label="活动开始时间:"
          class="form-items"
          prop="activityStartTime"
        >
          <el-date-picker
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss"
            v-model="formData.activityStartTime"
            class="input-items"
            size="small"
            type="datetime"
            placeholder="选择日期时间"
            style="width: 250px"
          >
          </el-date-picker>
        </el-form-item>
        <el-form-item
          label="活动结束时间:"
          class="form-items"
          prop="activityEndTime"
        >
          <el-date-picker
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss"
            v-model="formData.activityEndTime"
            class="input-items"
            size="small"
            type="datetime"
            placeholder="选择日期时间"
            style="width: 250px"
          >
          </el-date-picker>
        </el-form-item>
        <el-form-item label="活动状态：" prop="activityStatus">
          <el-radio v-model="formData.activityStatus" :label="0">关闭</el-radio>
          <el-radio v-model="formData.activityStatus" :label="1">启用</el-radio>
        </el-form-item>
        <el-form-item label="进度：">
          <el-button type="primary" size="small" @click="handleAddProgress"
            >添加进度</el-button
          >
        </el-form-item>
        <el-form-item>
          <template
            v-for="(
              clientInviteProgressBoItem, ProgressBoItemIndex
            ) in formData.clientInviteProgressBoList"
            :key="
              clientInviteProgressBoItem.progressId ||
              'progress-' + ProgressBoItemIndex
            "
          >
            <el-form
              ref="clientInviteProgressBoRef"
              :model="clientInviteProgressBoItem"
              label-width="130px"
              class="inviteProgress"
            >
              <div class="circle">{{ ProgressBoItemIndex + 1 }}</div>
              <el-form-item
                label="奖励对象："
                prop="inviteProgressType"
                :rules="rules.inviteProgressType"
              >
                <el-select
                  @change="
                    handleInviteProgressTypeChange($event, ProgressBoItemIndex)
                  "
                  v-model="clientInviteProgressBoItem.inviteProgressType"
                  class="input-items"
                  size="small"
                >
                  <el-option
                    v-for="item in inviteProgressTypeOptions"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value"
                  >
                  </el-option>
                </el-select>
              </el-form-item>
              <el-form-item
                label="奖励要求："
                prop="inviteProgressCode"
                :rules="rules.inviteProgressCode"
              >
                <el-select
                  v-model="clientInviteProgressBoItem.inviteProgressCode"
                  placeholder="请选择"
                  class="input-items"
                  size="small"
                >
                  <el-option
                    v-for="item in codeOptions"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value"
                  >
                  </el-option>
                </el-select>
              </el-form-item>
              <!--              <el-form-item label="进度值：" prop="inviteProgressValue" :rules="rules.inviteProgressValue">
                              <el-input v-model.number="clientInviteProgressBoItem.inviteProgressValue" class="input-items"
                                        placeholder="进度值" size="mini"
                              ></el-input>
                            </el-form-item>-->
              <el-form-item
                label="要求标题："
                prop="inviteProgressTitle"
                :rules="rules.inviteProgressTitle"
              >
                <el-input
                  v-model="clientInviteProgressBoItem.inviteProgressTitle"
                  class="input-items"
                  placeholder="要求标题"
                  size="small"
                ></el-input>
              </el-form-item>
              <el-form-item
                label="要求明细："
                prop="inviteProgressRequire"
                :rules="rules.inviteProgressRequire"
              >
                <el-input
                  v-model="clientInviteProgressBoItem.inviteProgressRequire"
                  class="input-items"
                  placeholder="要求明细"
                  size="small"
                ></el-input>
              </el-form-item>
              <el-form-item label-width="130px">
                <!--                <el-radio v-model="clientInviteProgressBoItem.inviteProgressStatus" :label="0" size="mini">停用-->
                <!--                </el-radio>-->
                <!--                <el-radio v-model="clientInviteProgressBoItem.inviteProgressStatus" :label="1" size="mini">启用中-->
                <!--                </el-radio>-->
                <el-form-item label="进度奖励：" label-width="90px">
                  <el-button
                    size="small"
                    type="primary"
                    @click="handleAddReward(ProgressBoItemIndex)"
                    plain
                    >添加奖励
                  </el-button>
                </el-form-item>
                <template
                  v-for="(
                    rewardItem, rewardIndex
                  ) in clientInviteProgressBoItem.rewardList"
                  :key="
                    rewardItem.id ||
                    rewardItem.progressId ||
                    'reward-' + rewardIndex
                  "
                >
                  <el-form
                    ref="rewardRefs"
                    :model="rewardItem"
                    label-width="100px"
                    class="award"
                    :rules="rules"
                  >
                    <div class="circle">
                      {{ ProgressBoItemIndex + 1 }}-{{ rewardIndex + 1 }}
                    </div>
                    <el-form-item label="奖励类型：" :rules="rules.rewardType">
                      <el-select
                        @change="handleChangeReward(rewardItem)"
                        size="small"
                        class="input-items"
                        v-model="rewardItem.rewardType"
                      >
                        <el-option
                          v-for="item in rewardTypeOptions"
                          :disabled="
                            shouldDisableOption(
                              item,
                              clientInviteProgressBoItem
                            )
                          "
                          :key="item.value"
                          :label="item.label"
                          :value="item.value"
                        >
                        </el-option>
                      </el-select>
                    </el-form-item>
                    <el-form-item
                      label="版本："
                      v-if="rewardItem.rewardType === 0"
                      prop="packageId"
                      :rules="rules.packageId"
                    >
                      <el-select
                        size="small"
                        class="input-items"
                        v-model="rewardItem.packageId"
                        @change="handlePackageChange($event, rewardItem)"
                      >
                        <el-option
                          v-for="item in VersionListOptions"
                          :key="item.id"
                          :label="item.name"
                          :value="item.id"
                        >
                        </el-option>
                      </el-select>
                    </el-form-item>
                    <el-form-item
                      label="版本价格："
                      v-if="rewardItem.rewardType === 0"
                      prop="packagePriceId"
                      :rules="rules.packagePriceId"
                    >
                      <el-select
                        size="small"
                        class="input-items"
                        v-model="rewardItem.packagePriceId"
                      >
                        <el-option
                          v-for="item in rewardItem.packagePriceList"
                          :key="item.value"
                          :label="item.label"
                          :value="item.value"
                        >
                        </el-option>
                      </el-select>
                    </el-form-item>
                    <template v-if="rewardItem.rewardType === 1">
                      <div class="incremental-package">
                        <el-form-item
                          label="增量包："
                          label-width="85px"
                          :rules="rules.commodityTypeId"
                          prop="commodityTypeId"
                        >
                        </el-form-item>
                        <el-form-item>
                          <el-select
                            style="width: 125px"
                            size="small"
                            class="input-items"
                            v-model="rewardItem.commodityTypeId"
                            @change="handleCommodityTypeIdChange(rewardItem)"
                          >
                            <el-option
                              v-for="item in incrementalPackageOptions"
                              :key="item.id"
                              :label="item.name"
                              :value="item.id"
                            >
                            </el-option>
                          </el-select>
                        </el-form-item>
                        <el-form-item
                          :rules="rules.commodityNumber"
                          prop="commodityNumber"
                        >
                          <el-input
                            v-model="rewardItem.commodityNumber"
                            class="package-items"
                            placeholder="数量"
                            size="small"
                          >
                            <template #suffix>{{
                              takeTheUnit(rewardItem.commodityTypeId)
                            }}</template>
                          </el-input>
                        </el-form-item>
                        <el-form-item
                          :rules="rules.validityNum"
                          prop="validityNum"
                        >
                          <el-input
                            v-model="rewardItem.validityNum"
                            class="package-items"
                            placeholder="时间"
                            size="small"
                          ></el-input>
                        </el-form-item>
                        <el-form-item
                          :rules="rules.validityUnit"
                          prop="validityUnit"
                        >
                          <el-select
                            style="width: 105px"
                            v-model="rewardItem.validityUnit"
                            class="package-items"
                            size="small"
                            placeholder="请选择"
                          >
                            <el-option
                              v-for="item in validityUnitOptions"
                              :key="item.value"
                              :label="item.label"
                              :value="item.value"
                            >
                            </el-option>
                          </el-select>
                        </el-form-item>
                      </div>
                    </template>
                    <el-form-item class="del-btn">
                      <el-popconfirm
                        :title="`是否删除第${ProgressBoItemIndex + 1}-${
                          rewardIndex + 1
                        }个奖励`"
                        @confirm="
                          handleDelReward(ProgressBoItemIndex, rewardIndex)
                        "
                      >
                        <template #reference>
                          <el-button
                            type="danger"
                            size="small"
                            icon="DeleteFilled"
                            plain
                          >
                            删除奖励
                          </el-button>
                        </template>
                      </el-popconfirm>
                    </el-form-item>
                  </el-form>
                </template>
              </el-form-item>
              <el-form-item class="del-btn">
                <el-popconfirm
                  :title="`是否删除第${ProgressBoItemIndex + 1}个进度`"
                  @confirm="handleDelProgress(ProgressBoItemIndex)"
                >
                  <template #reference>
                    <el-button size="small" type="danger" icon="DeleteFilled">
                      删除进度
                    </el-button>
                  </template>
                </el-popconfirm>
              </el-form-item>
            </el-form>
          </template>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="handleClose" size="small">取 消</el-button>
        <el-button type="primary" @click="handleConfirm" size="small"
          >确 定</el-button
        >
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount, nextTick } from 'vue'
import api from '@/utils/request-api'
import emitter from '@/utils/emitter'
import { useDict } from '@/hooks/useDict.js'

const emit = defineEmits(['refresh', 'change-survive'])
const title = ref('修改')
const dialogVisible = ref(false)
const formRef = ref(null)
const clientInviteProgressBoRef = ref([])
const rewardRefs = ref([])

const rules = {
  agentId: [{ required: true, message: '请选择代理商', trigger: 'change' }],
  activityName: [
    { required: true, message: '请输入活动名称', trigger: 'blur' },
  ],
  activityStartTime: [
    { required: true, message: '请选择活动开始时间', trigger: 'change' },
  ],
  activityEndTime: [
    { required: true, message: '请选择活动结束时间', trigger: 'change' },
  ],
  activityStatus: [
    { required: true, message: '请选择活动状态', trigger: 'change' },
  ],
  inviteProgressType: [
    { required: true, message: '请选择邀请进度类型', trigger: 'change' },
  ],
  inviteProgressCode: [
    { required: true, message: '请选择进度code', trigger: 'change' },
  ],
  // inviteProgressValue: [
  //   { required: true, message: '请输入邀请进度值', trigger: 'blur' },
  //   { pattern: /^(?!0+(\.0+)?$)\d+(\.\d+)?$/, message: '请输入正数', trigger: 'blur' }
  // ],
  inviteProgressTitle: [
    { required: true, message: '请输入邀请进度标题', trigger: 'blur' },
  ],
  inviteProgressRequire: [
    { required: true, message: '请选择邀请进度要求', trigger: 'change' },
  ],
  // inviteProgressStatus: [
  //   { required: true, message: '请选择邀请进度状态', trigger: 'change' }
  // ],
  rewardType: [
    { required: true, message: '请选择奖励类型', trigger: 'change' },
  ],
  packageId: [{ required: true, message: '请选择版本', trigger: 'change' }],
  packagePriceId: [
    { required: true, message: '请选择版本价格', trigger: 'change' },
  ],
  commodityTypeId: [
    { required: true, message: '请选择商品类型', trigger: 'change' },
  ],
  commodityNumber: [
    { required: true, message: '请输入商品数量', trigger: 'blur' },
    {
      pattern: /^(?!0+(\.0+)?$)\d+(\.\d+)?$/,
      message: '请输入正数',
      trigger: 'blur',
    },
  ],
  validityNum: [
    { required: true, message: '请输入有效期', trigger: 'blur' },
    {
      pattern: /^(?!0+(\.0+)?$)\d+(\.\d+)?$/,
      message: '请输入正数',
      trigger: 'blur',
    },
  ],
  validityUnit: [
    { required: true, message: '请选择有效期单位', trigger: 'change' },
  ],
}

const formData = reactive({
  agentId: '',
  activityName: '',
  activityStartTime: '',
  activityEndTime: '',
  activityStatus: '',
  clientInviteProgressBoList: [
    {
      inviteActivityId: '',
      inviteProgressType: '',
      inviteProgressCode: '',
      inviteProgressValue: 1,
      inviteProgressTitle: '',
      inviteProgressRequire: '',
      inviteProgressStatus: 1,
      rewardList: [
        {
          progressId: '',
          rewardType: '',
          packageId: '',
          packagePriceId: '',
          commodityTypeId: '',
          commodityNumber: '',
          validityNum: '',
          validityUnit: '',
        },
      ],
    },
  ],
})

const codeOptions = ref([])
const rewardTypeOptions = ref([
  { value: 0, label: '版本' },
  { value: 1, label: '增量包' },
])
const agentListOptions = ref([])
const inviteProgressTypeOptions = ref([
  { value: 0, label: '被邀请人' },
  { value: 1, label: '邀请人' },
])
const validityUnitOptions = ref([
  // { value: 0, label: '小时' },
  { value: 1, label: '天' },
  { value: 2, label: '月' },
  { value: 3, label: '季度' },
  // { value: 4, label: '半年' },
  { value: 5, label: '年' },
])
const VersionListOptions = ref([])
const VersionPriceListOptions = ref([])
const incrementalPackageOptions = ref([])

const { orderDict, getLabel } = useDict()

async function openDialog(row) {
  dialogVisible.value = true
  if (row?.id) {
    title.value = '修改'
    const res = await api.invitation.info({ id: row.id }, { showLoading: true })
    if (res.code === 0) {
      await nextTick()
      const data = res.data
      data.clientInviteProgressBoList = data.progressList
      delete data.progressList
      await getAllPackage()
      handleVersionPrice(data)
      Object.assign(formData, handleFormData(data, 2))
    }
  } else {
    title.value = '添加'
  }
}

function handleAddProgress() {
  formData.clientInviteProgressBoList.push(progressParams())
  ElMessage.success('添加进度成功,请补全内容')
  nextTick(() => {
    clearValidatedAll()
  })
}

function handleDelProgress(index) {
  if (formData.clientInviteProgressBoList.length < 2) {
    ElMessage.error('至少保留一个进度')
    return
  }
  formData.clientInviteProgressBoList.splice(index, 1)
  ElMessage.success('删除进度成功')
  nextTick(() => {
    clearValidatedAll()
  })
}

function handleAddReward(ProgressBoItemIndex) {
  formData.clientInviteProgressBoList[ProgressBoItemIndex].rewardList.push(
    rewardParams()
  )
  ElMessage.success('添加奖励成功,请补全内容')
  nextTick(() => {
    clearValidatedAll()
  })
}

function handleDelReward(ProgressBoItemIndex, rewardIndex) {
  if (
    formData.clientInviteProgressBoList[ProgressBoItemIndex].rewardList.length <
    2
  ) {
    ElMessage.error('至少保留一个奖励')
    return
  }
  formData.clientInviteProgressBoList[ProgressBoItemIndex].rewardList.splice(
    rewardIndex,
    1
  )
  ElMessage.success('删除奖励成功')
  nextTick(() => {
    clearValidatedAll()
  })
}

function progressParams() {
  return {
    inviteActivityId: '',
    inviteProgressType: '',
    inviteProgressCode: '',
    inviteProgressValue: 1,
    inviteProgressTitle: '',
    inviteProgressRequire: '',
    inviteProgressStatus: 1,
    rewardList: [
      {
        progressId: '',
        rewardType: '',
        packageId: '',
        packagePriceId: '',
        commodityTypeId: '',
        commodityNumber: '',
        validityNum: '',
        validityUnit: '',
      },
    ],
  }
}

function rewardParams() {
  return {
    progressId: '',
    rewardType: '',
    packageId: '',
    packagePriceId: '',
    commodityTypeId: '',
    commodityNumber: '',
    validityNum: '',
    validityUnit: '',
  }
}

function handleChangeReward(rewardItem) {
  if (rewardItem.rewardType === 1) {
    rewardItem.commodityTypeId = ''
    rewardItem.commodityNumber = ''
    rewardItem.validityNum = ''
    rewardItem.validityUnit = ''
  } else {
    rewardItem.packageId = ''
    rewardItem.packagePriceId = ''
  }
  nextTick(() => {
    ;(rewardRefs.value || []).map((item) => item?.clearValidate())
  })
}

function handlePackageChange(val, rewardItem) {
  if (val && VersionListOptions.value && VersionListOptions.value.length > 0) {
    rewardItem.packagePriceId = ''
    const data =
      VersionListOptions.value.find((item) => item.id === val)
        ?.commodityPriceList || []
    rewardItem.packagePriceList = data.map((item) => {
      return {
        label: `${item.realPrice / 100}/${item.validityNum}${getLabel(
          orderDict.timeUnit,
          item.validityUnit
        )}`,
        value: item.id,
      }
    })
  }
}

async function validateAll() {
  try {
    const formValidatePromise = formRef.value?.validate()
    const clientInvitePromises =
      clientInviteProgressBoRef.value?.map((item) => item.validate()) || []
    const rewardPromises =
      rewardRefs.value?.map((item) => item.validate()) || []
    await Promise.all([
      formValidatePromise,
      ...clientInvitePromises,
      ...rewardPromises,
    ])
    return true
  } catch (error) {
    return false
  }
}

function clearValidatedAll() {
  formRef.value?.clearValidate()
  clientInviteProgressBoRef.value?.map((item) => item.clearValidate())
  rewardRefs.value?.map((item) => item.clearValidate())
}

function handleInviteProgressTypeChange(val, ProgressBoItemIndex) {
  if (val === 1) {
    if (formData.clientInviteProgressBoList[ProgressBoItemIndex].rewardList) {
      formData.clientInviteProgressBoList[
        ProgressBoItemIndex
      ].rewardList.forEach((item) => (item.rewardType = 1))
    }
  }
}

function shouldDisableOption(item, clientInviteProgressBoItem) {
  if (clientInviteProgressBoItem.inviteProgressType === 1) {
    return item.value === 0
  }
}

async function handleConfirm() {
  const isValid = await validateAll()
  if (!isValid) return
  const data = handleFormData(formData, 1)
  if (formData.id) {
    const res = await api.invitation.update(data)
    if (res.code === 0) {
      dialogVisible.value = false
      ElMessage.success('修改成功')
      emit('refresh')
    }
  } else {
    const res = await api.invitation.save(data)
    if (res.code === 0) {
      dialogVisible.value = false
      ElMessage.success('新增成功')
      emit('refresh')
    }
  }
}

function handleFormData(formDataIn, type) {
  const data = JSON.parse(JSON.stringify(formDataIn))
  if (
    data.clientInviteProgressBoList &&
    data.clientInviteProgressBoList.length
  ) {
    data.clientInviteProgressBoList.forEach((outerItem) => {
      if (outerItem.rewardList && outerItem.rewardList.length) {
        outerItem.rewardList.forEach((innerItem) => {
          if (innerItem.commodityTypeId) {
            const code = takeTheCode(innerItem.commodityTypeId)
            if (type === 1) {
              innerItem.commodityNumber = getConvertUnitValue(
                innerItem.commodityNumber,
                code
              )
            } else {
              innerItem.commodityNumber = setConvertUnitValue(
                innerItem.commodityNumber,
                code
              )
            }
          }
        })
      }
    })
    return data
  } else if (!data.clientInviteProgressBoList) {
    data.clientInviteProgressBoList = []
    return data
  } else {
    return data
  }
}

function setConvertUnitValue(value, code) {
  if (isNaN(value) || !value) return value
  if (
    code === 'textExtractionNum' ||
    code === 'aiAnalysisTime' ||
    code === 'textExtraction'
  ) {
    let val = (Number(value) / 60).toFixed(2)
    return parseFloat(val)
  } else if (code === 'storageNum') {
    let val = (Number(value) / 1024 / 1024).toFixed(2)
    return parseFloat(val)
  } else {
    return value
  }
}

function getConvertUnitValue(value, code) {
  if (isNaN(value) || !value) return value
  if (
    code === 'textExtractionNum' ||
    code === 'aiAnalysisTime' ||
    code === 'textExtraction'
  ) {
    let val = (Number(value) * 60).toFixed(2)
    return parseFloat(val)
  } else if (code === 'storageNum') {
    let val = (Number(value) * 1024 * 1024).toFixed(2)
    return parseFloat(val)
  } else {
    return value
  }
}

function handleVersionPrice(data) {
  if (
    data.clientInviteProgressBoList &&
    data.clientInviteProgressBoList.length
  ) {
    data.clientInviteProgressBoList.forEach((outerItem) => {
      if (outerItem.rewardList && outerItem.rewardList.length) {
        outerItem.rewardList.forEach((innerItem) => {
          if (
            innerItem.packageId &&
            VersionListOptions.value &&
            VersionListOptions.value.length
          ) {
            const list =
              VersionListOptions.value.find(
                (item) => item.id === innerItem.packageId
              )?.commodityPriceList || []
            innerItem.packagePriceList = list.map((item) => ({
              label: `${item.realPrice / 100}/${item.validityNum}${getLabel(
                orderDict.timeUnit,
                item.validityUnit
              )}`,
              value: item.id,
            }))
          }
        })
      }
    })
  }
}

function takeTheUnit(id) {
  if (
    id &&
    incrementalPackageOptions.value &&
    incrementalPackageOptions.value.length > 0
  ) {
    return incrementalPackageOptions.value.filter((item) => {
      if (item.id === id) {
        return true
      }
    })[0]?.unit
  }
}

function takeTheCode(id) {
  if (
    id &&
    incrementalPackageOptions.value &&
    incrementalPackageOptions.value.length > 0
  ) {
    return incrementalPackageOptions.value.filter((item) => {
      if (item.id === id) {
        return true
      }
    })[0]?.code
  }
}

function handleCommodityTypeIdChange(item) {
  item.commodityNumber = ''
}

function handleClose() {
  dialogVisible.value = false
  emit('change-survive', false)
}

async function getAllPackage() {
  const res = await api.package.list(
    { limit: -1, packageType: 1 },
    { showLoading: true }
  )
  if (res.code === 0) {
    VersionListOptions.value = res.data.list
  }
}

async function getIncrementalPackageType() {
  const res = await api.commoditytype.list({ limit: -1 })
  if (res.code === 0) {
    incrementalPackageOptions.value = res.data.list
  }
}

async function getAgentList() {
  const res = await api.user.agentSalesList({ page: 0, limit: -1 })
  if (res.code === 0) {
    agentListOptions.value = res.data.list
  }
}

async function getCodeList() {
  const res = await api.invitation.codeList({
    typeLogo: 'invite_user_reward_code',
    limit: -1,
  })
  if (res.code === 0) {
    codeOptions.value = res.data.list
  }
}

onMounted(() => {
  emitter.on('openDialog', openDialog)
  Promise.all([getAgentList(), getIncrementalPackageType(), getCodeList()])
})

onBeforeUnmount(() => {
  emitter.off('openDialog', openDialog)
})
</script>

<style lang="less" scoped>
.el-form-item {
  margin-bottom: 10px;
}

.form-items {
  margin-bottom: 10px;
}

.inviteProgress,
.award {
  width: 100%;
  position: relative;
  border: 1px solid #ddd;
  border-radius: 5px;
  margin-bottom: 10px;

  .circle {
    position: absolute;
    right: 10px;
    top: 5px;
    width: 35px;
    height: 35px;
    border-radius: 50%; /* 圆形 */
    background-color: #409eff; /* 圆圈颜色 */
    color: white; /* 文字颜色 */
    text-align: center; /* 文字居中 */
    line-height: 35px;
    font-size: 12px; /* 数字大小 */
    font-weight: bold; /* 加粗 */
    user-select: none;
  }
}

.input-items {
  width: 200px;
}

::v-deep(.incremental-package) {
  gap: 5px;
  .el-form-item {
    width: 130px;

    .el-form-item__error {
      width: 100px;
    }
  }

  display: flex;
}

.package-items {
  width: 125px;
}
.del-btn {
  :deep(.el-form-item__content) {
    display: flex;
    justify-content: flex-end;
    padding-right: 20px;
  }
}
</style>
