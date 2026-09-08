<template>
  <el-dialog
      v-model="visible"
      :close-on-click-modal="false"
      :title="!dataForm.id ? '新增' : '修改'"
      class="invitationcodebatch-add-or-update-dialog"
      @close="clone"
  >
    <el-form
        ref="dataFormRef"
        :model="dataForm"
        :rules="dataRule"
        label-width="130px"
    >
      <el-form-item label="邀请名称" prop="name">
        <el-input v-model="dataForm.name" placeholder="邀请名称"></el-input>
      </el-form-item>
      <el-form-item label="创建总数" prop="quantity">
        <el-input
            v-model="dataForm.quantity"
            :disabled="dataForm.id ? true : false"
            min="1"
            placeholder="创建总数"
            type="number"
        ></el-input>
      </el-form-item>
      <el-form-item label="版本形式" prop="commodityType">
        <el-select
            v-model="dataForm.commodityType"
            class="width-70"
            placeholder="版本形式"
            style="width: 100%"
            @change="selectCommodityType"
        >
          <el-option
              v-for="(item, index) in [
              '用户版本(每个月会重置资源)',
              '活动版本(一次性的，类似于加量包，到期就失效)',
            ]"
              :key="`commodity-type-${index}`"
              :label="item"
              :value="index"
          >
          </el-option>
        </el-select>
      </el-form-item>
      <el-form-item label="选择内容" prop="commodityName">
        <div style="display: flex; align-items: center; gap: 20px; width: 100%">
          <el-select
              v-model="dataForm.commodityId"
              :class="dataForm.commodityName ? 'custom-input' : ''"
              :placeholder="`${
              dataForm.commodityName ? dataForm.commodityName : '版本'
            }`"
              style="width: 35%"
              @change="selectCommodity"
          >
            <el-option
                v-for="item in packageList"
                :key="`package-${item.id}`"
                :label="item.name"
                :value="item.id"
            >
            </el-option>
          </el-select>
          <el-select
              v-model="dataForm.commodityPriceId"
              :class="validityDate ? 'custom-input' : ''"
              :placeholder="validityDate ? validityDate : '时间'"
              style="width: 35%"
              @change="selectCommodityPrice"
          >
            <el-option
                v-for="item in commodityList"
                :key="`commodity-${item.id}`"
                :label="
                item.validityNum +
                getLabel(orderDict.timeUnit, item.validityUnit)
              "
                :value="item.id"
            >
            </el-option>
          </el-select>
          <div>
            原价：{{ retainDecimals(dataForm.commodityRealPrice / 100) }}元
          </div>
        </div>
      </el-form-item>
      <el-form-item label="实际价格" prop="price">
        <div style="display: flex; align-items: center; gap: 10px; width: 100%">
          <el-input
              v-model="dataForm.price"
              min="0"
              placeholder="实际价格"
              style="width: 70%"
              type="number"
          ></el-input>
          <div>总价：{{ dataForm.price * dataForm.quantity }}元</div>
        </div>
      </el-form-item>
      <el-form-item label="邀请码类型" prop="type">
        <el-select
            v-model="dataForm.type"
            placeholder="请选择"
            style="width: 100%"
        >
          <el-option
              v-for="(item, index) in ['机构码', '个人码']"
              :key="`type-${index}`"
              :label="item"
              :value="index"
          >
          </el-option>
        </el-select>
      </el-form-item>
      <el-form-item label="邀请码有效期" prop="validityStartDate">
        <el-date-picker
            v-model="validityDateArr"
            :default-time="[
            new Date(0, 0, 0, 0, 0, 0),
            new Date(0, 0, 0, 23, 59, 59),
          ]"
            end-placeholder="结束日期"
            format="YYYY-MM-DD"
            range-separator="至"
            start-placeholder="开始日期"
            style="width: 260px"
            type="daterange"
            value-format="YYYY-MM-DD HH:mm:ss"
        />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="dataForm.status" placeholder="请选择">
          <el-option
              v-for="(item, index) in ['正常', '禁用']"
              :key="`status-${index}`"
              :label="item"
              :value="index"
          >
          </el-option>
        </el-select>
      </el-form-item>
      <el-form-item label="免费的" prop="isGratis">
        <el-select
            v-model="dataForm.isGratis"
            placeholder="一个账号只能使用一张免费邀请码"
        >
          <el-option
              v-for="(item, index) in ['否', '是']"
              :key="`gratis-${index}`"
              :label="item"
              :value="index"
          >
          </el-option>
        </el-select>
      </el-form-item>
      <el-form-item label="使用次数" prop="isInfinite">
        <el-select v-model="dataForm.isInfinite" placeholder="请选择使用次数">
          <el-option :value="0" label="一次"></el-option>
          <el-option :value="1" label="无限次"></el-option>
        </el-select>
      </el-form-item>
      <el-form-item label="申请原因" prop="remarks">
        <el-input
            v-model="dataForm.remarks"
            placeholder="申请原因"
            type="textarea"
        ></el-input>
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
import {
  ref,
  reactive,
  computed,
  nextTick
} from 'vue'
import { useDict } from '@/hooks/useDict.js'
import api from '@/utils/request-api'
import myUtils from '@/utils/utils'

const emit = defineEmits(['is-ok', 'refreshDataList'])

const {orderDict, getLabel} = useDict()

const visible = ref(false)
const validityDateArr = ref([])
const commodityId = ref('')
const commodityPriceId = ref('')
const dataFormRef = ref()

const dataForm = reactive({
  id: null,
  name: '',
  commodityId: '',
  commodityPriceId: '',
  userId: '',
  type: 0,
  quantity: '',
  validityStartDate: '',
  validityEndDate: '',
  status: 0,
  isLssued: '',
  remarks: '',
  createDate: '',
  updateDate: '',
  isDeleted: '',
  isGratis: 0,
  commodityRealPrice: '',
  commodityValidityNum: '',
  commodityValidityUnit: '',
  price: '',
  isInfinite: null,
  resourceType: 0,
  commodityType: ''
})

const currentCommodity = ref({})
const currentCommodityPrice = ref({})
const packageList = ref([])
const commodityList = ref([])

const checkCommodityId = (rule, value, callback) => {
  let commodityId = dataForm.commodityId
  let commodityName = dataForm.commodityName
  let commodityPriceId = dataForm.commodityPriceId
  let commodityRealPrice = dataForm.commodityRealPrice
  if (!commodityId && !commodityName) {
    return callback(new Error('请选择版本'))
  }
  console.log(commodityId, commodityName, commodityPriceId, commodityRealPrice)
  if (
      !(
          commodityPriceId ||
          (commodityRealPrice !== null &&
              commodityRealPrice !== undefined &&
              commodityRealPrice !== '')
      )
  ) {
    return callback(new Error('请选择时间'))
  }
  callback()
}

const dataRule = reactive({
  commodityType: [
    {required: true, message: '不能为空', trigger: ['blur', 'change']}
  ],
  name: [
    {
      required: true,
      message: '邀请名称不能为空',
      trigger: ['blur', 'change']
    }
  ],
  commodityName: [
    {
      required: true,
      validator: checkCommodityId,
      trigger: ['blur', 'change']
    }
  ],
  type: [
    {required: true, message: '类型不能为空', trigger: ['blur', 'change']}
  ],
  quantity: [
    {
      required: true,
      message: '创建总数不能为空',
      trigger: ['blur', 'change']
    }
  ],
  validityStartDate: [
    {required: true, message: '有效期不能为空', trigger: ['blur', 'change']}
  ],
  status: [
    {required: true, message: '状态不能为空', trigger: ['blur', 'change']}
  ],
  isGratis: [
    {required: true, message: '不能为空', trigger: ['blur', 'change']}
  ],
  price: [
    {required: true, message: '价格不能为空', trigger: ['blur', 'change']}
  ],
  isInfinite: [
    {required: true, message: '不能为空', trigger: ['blur', 'change']}
  ]
})

const validityDate = computed(() => {
  if (
      dataForm.commodityValidityNum !== null &&
      dataForm.commodityValidityNum !== undefined &&
      dataForm.commodityValidityNum !== '' &&
      dataForm.commodityValidityUnit !== null &&
      dataForm.commodityValidityUnit !== undefined &&
      dataForm.commodityValidityUnit !== ''
  ) {
    return `${dataForm.commodityValidityNum}${getLabel(
        orderDict.timeUnit,
        dataForm.commodityValidityUnit
    )}`
  } else {
    return ''
  }
})

const selectCommodityType = () => {
  dataForm.commodityId = ''
  dataForm.commodityName = ''
  dataForm.commodityPriceId = ''
  dataForm.commodityRealPrice = ''
  dataForm.commodityValidityNum = ''
  dataForm.commodityValidityUnit = ''
  dataForm.price = ''
  packageList.value = []
  commodityList.value = []
  getPackageList(dataForm.commodityType)
}

const retainDecimals = (val) => {
  if (isNaN(val)) return 0
  return myUtils.retainDecimals(val)
}

const selectCommodity = (commodityId, setCommodityPrice = true) => {
  dataForm.commodityPriceId = ''
  if (setCommodityPrice) {
    dataForm.commodityRealPrice = null
    dataForm.commodityValidityNum = null
    dataForm.commodityValidityUnit = null
  }
  commodityList.value = []
  packageList.value.forEach((item) => {
    if (item.id == commodityId) {
      currentCommodity.value = item
      commodityList.value = currentCommodity.value.commodityPriceList
      currentCommodityPrice.value = {}
    }
  })
  if (dataForm?.commodityPriceId)
    selectCommodityPrice(dataForm.commodityPriceId)
}

const selectCommodityPrice = (val) => {
  currentCommodity.value.commodityPriceList.forEach((item) => {
    if (item.id == val) {
      currentCommodityPrice.value = item
      dataForm.commodityRealPrice = item.realPrice
    }
  })
}

const getPackageList = async (packageType) => {
  packageList.value = []
  const res = await api.package.list({
    limit: -1,
    packageType: packageType + 1
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
  const data = await api.invitationcodebatch.info({id: dataForm.id}, {showLoading: true})
  if (data && data.code === 0) {
    Object.assign(dataForm, data.data)
    dataForm.price = dataForm.price / 100
    commodityId.value = dataForm.commodityId
    commodityPriceId.value = dataForm.commodityPriceId
    validityDateArr.value.push(dataForm.validityStartDate)
    validityDateArr.value.push(dataForm.validityEndDate)

    await getPackageList(dataForm.commodityType, 1)
    selectCommodity(dataForm.commodityId, false)
    dataForm.commodityId = null
    dataForm.commodityPriceId = null
  }
}

const init = async (id) => {
  dataForm.id = id || null
  validityDateArr.value = []
  visible.value = true
  await nextTick(() => {
    dataFormRef.value.resetFields()
    if (dataForm.id) {
      getInfo()
    }
  })
}

const clone = () => {
  emit('is-ok')
}

const dataFormSubmit = () => {
  dataFormRef.value.validate(async (valid) => {
    if (valid) {
      let requestData = JSON.parse(JSON.stringify(dataForm))

      if (requestData.commodityId) {
        let data =
            packageList.value.find(
                (item) => item.id === requestData.commodityId
            ) ?? null
        if (data) {
          data.commodityName = data.name
          data.commodityLevel = data.level
        }
      } else {
        requestData.commodityId = commodityId.value
      }

      if (requestData.commodityPriceId) {
        let data =
            commodityList.value.find(
                (item) => item.id === requestData.commodityPriceId
            ) ?? null
        if (data) {
          requestData.commodityRealPrice = data.realPrice
          requestData.commodityValidityNum = data.validityNum
          requestData.commodityValidityUnit = data.validityUnit
        }
      } else {
        requestData.commodityPriceId = commodityPriceId.value
      }

      // 判断是否填写版本和时间
      if (!requestData.commodityId || !requestData.commodityPriceId) {
        ElMessage.error('请完善选择内容')
        return
      }
      requestData.price = requestData.price * 100
      if (requestData.id) {
        // 修改
        const res = await api.invitationcodebatch.update(requestData)
        if (res && res.code === 0) {
          emit('refreshDataList')
          ElMessage({
            message: res.msg,
            type: 'success'
          })
          visible.value = false
        }
      } else {
        // 新增
        requestData.id = ''
        const res = await api.invitationcodebatch.save(requestData)
        if (res && res.code === 0) {
          emit('refreshDataList')
          ElMessage({
            message: res.msg,
            type: 'success'
          })
          visible.value = false
        }
      }
    }
  })
}

defineExpose({
  init
})
</script>

<style lang="scss" scoped>
.width-70 {
  width: 70%;
}

:deep(.custom-input .el-input__inner::placeholder) {
  color: #606266; /* 修改 placeholder 颜色 */
}
</style>

<style>
.invitationcodebatch-add-or-update-dialog {
  width: 40%;
  min-width: 600px;
}
</style>
