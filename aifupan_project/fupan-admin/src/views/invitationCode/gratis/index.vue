<template>
  <div class="container">
    <div class="announcement">
      <div class="property-text">邀请名称：</div>
      <div class="property-value">{{ dataForm?.name }}</div>
    </div>

    <div class="announcement">
      <div class="property-text">版本形式：</div>
      <div class="property-value" v-if="dataForm?.commodityType == 0">
        正常包月形式(每个月会重置资源)
      </div>
      <div class="property-value" v-if="dataForm?.commodityType == 1">
        到期资源失效(一次性的，类似于加量包，到期就失效)
      </div>
    </div>

    <div class="announcement">
      <div class="property-text">邀请码类型：</div>
      <div class="property-value" v-if="dataForm?.type == 0">机构码</div>
      <div class="property-value" v-if="dataForm?.type == 1">个人码</div>
      <div class="property-value" v-if="dataForm?.type == 2">激活码</div>
    </div>

    <div class="announcement">
      <div class="property-text">批次状态：</div>
      <div v-if="admin">
        <el-switch
          v-model="dataForm.status"
          :active-value="0"
          :inactive-value="1"
          active-text="正常"
          inactive-text="禁用"
        ></el-switch>
      </div>
      <div v-else>
        <div class="property-value" v-if="dataForm?.status == 0">正常</div>
        <div class="property-value" v-if="dataForm?.status == 1">禁用</div>
      </div>
    </div>

    <!--    <div class="announcement">-->
    <!--      <div class="property-text">备注：</div>-->
    <!--      <div class="property-value">{{ dataForm?.remarks }}</div>-->
    <!--    </div>-->

    <div class="announcement" style="align-items: center; margin-bottom: 20px">
      <div class="property-text">邀请码：</div>
      <div v-if="admin">
        <el-input v-model="code"></el-input>
      </div>
      <div v-else>
        <span class="property-value code">{{
          dataForm?.codeList?.length > 0 ? dataForm?.codeList[0].code : ''
        }}</span>
        <i
          @click="copyCode"
          title="复制邀请码"
          class="el-icon-document-copy icon-cilck"
        ></i>
      </div>
    </div>

    <div
      class="announcement"
      style="align-items: center; margin-bottom: 20px; border: 0"
      v-if="admin"
    >
      <div class="property-text">
        <el-button type="primary" @click="submit">修改保存</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import myUtils from '@/utils/utils.js'
import api from '@/utils/request-api.js'

const route = useRoute()

const code = ref('')
const validityDateArr = ref([])
const admin = ref(0)

const dataForm = reactive({
  id: 0,
  name: '',
  commodityId: '',
  commodityPriceId: '',
  userId: '',
  type: 0,
  quantity: '',
  validityStartDate: '',
  validityEndDate: '',
  status: '',
  isLssued: '',
  remarks: '',
  createDate: '',
  updateDate: '',
  isDeleted: '',
  isGratis: '',
  commodityRealPrice: '',
  commodityValidityNum: '',
  commodityValidityUnit: '',
  price: '',
  isInfinite: '',
  resourceType: 0,
  commodityType: '',
})

const currentCommodity = ref({})
const currentCommodityPrice = ref({})
const packageList = ref([])

const submit = async () => {
  let res = await api.invitationcodebatch.update(dataForm)
  if (!res?.code == 0) {
    ElMessage.error(res.msg)
  }
  let data = dataForm?.codeList?.length > 0 ? dataForm?.codeList[0] : null
  if (data) {
    res = await api.invitationcode.update({ id: data.id, code: code.value })
    if (res?.code == 0) {
      admin.value = 0
      ElMessage.success(res.msg)
      getInfo()
    } else {
      ElMessage.error(res.msg)
    }
  } else {
    ElMessage.error('批次中没有邀请码')
  }
}

const selectCommodityType = () => {
  getPackageList(dataForm.commodityType)
}

const selectCommodityPrice = (val) => {
  currentCommodity.value.commodityPriceList.forEach((item) => {
    if (item.id == val) {
      currentCommodityPrice.value = item
      dataForm.commodityRealPrice = currentCommodityPrice.value.realPrice
      dataForm.commodityValidityNum = currentCommodityPrice.value.validityNum
      dataForm.commodityValidityUnit = currentCommodityPrice.value.validityUnit
    }
  })
}

const retainDecimals = (val) => {
  return myUtils.retainDecimals(val)
}

const updateCode = async (codeId, codeStatus) => {
  codeStatus = codeStatus == 0 ? 1 : 0
  const res = await api.invitationcode.update({
    id: codeId,
    status: codeStatus,
  })
  if (res.code == 0) {
    ElMessage.success('操作成功')
    getInfo()
  }
}

const selectCommodity = (commodityId) => {
  packageList.value.forEach((item) => {
    if (item.id == commodityId) {
      currentCommodity.value = item
      currentCommodityPrice.value = {}
      dataForm.commodityPriceId = ''
    }
  })
}

const getPackageList = async (packageType) => {
  packageList.value = []
  const res = await api.package.list({
    limit: -1,
    packageType: packageType + 1,
  })
  if (res && res.code === 0) {
    packageList.value = res.data.list
  }
}

const selectValidityDate = () => {
  dataForm.validityStartDate = validityDateArr.value[0]
  dataForm.validityEndDate = validityDateArr.value[1]
}

const getInfo = async () => {
  const data = await api.invitationcodebatch.checkOnlyActivationCode({})
  if (data && data.code === 0) {
    Object.assign(dataForm, data.data ?? {})
    validityDateArr.value.push(dataForm.validityStartDate)
    validityDateArr.value.push(dataForm.validityEndDate)
    if (dataForm?.codeList?.length > 0) {
      code.value = dataForm?.codeList[0].code
    }
  }
}

const copyCode = async () => {
  const textarea = document.createElement('textarea')
  textarea.value = dataForm?.codeList[0]?.code ?? ''

  document.body.appendChild(textarea)

  textarea.select()
  textarea.setSelectionRange(0, 99999)

  document.execCommand('copy')

  document.body.removeChild(textarea)

  ElMessage({
    message: '邀请码已复制到粘贴板',
    type: 'success',
    duration: 1000,
  })
}

onMounted(() => {
  admin.value = route.query?.admin ?? 0
  dataForm.id = BigInt('4370279576429920256')
  validityDateArr.value = []
  getInfo()
})
</script>

<style scoped>
.dialog-footer {
  margin-top: 10px;
}

.container {
  margin: 0px 30px;
  border: 3px solid #6f9f9f;
  padding: 20px 40px 20px 40px;
}

.announcement {
  display: flex;
  border-bottom: 1px solid #6f9f9f;
  padding-bottom: 8px;
  margin-top: 20px;
}

.property-text {
  text-align: end;
  color: #5c6f86;
  font-size: 20px;
  font-weight: bold;
  width: 120px;
  margin-right: 10px;
}

.property-value {
  font-size: 20px;
  color: #6f9f9f;
  font-weight: bold;
}

.code {
  margin-right: 10px;
  color: #24c1c1;
  font-size: 28px;
}

.icon-cilck {
  font-size: 20px;
}

.icon-cilck:hover {
  cursor: pointer;
  color: #5cd9d9;
}

.cellClassName {
  color: red;
}
</style>
