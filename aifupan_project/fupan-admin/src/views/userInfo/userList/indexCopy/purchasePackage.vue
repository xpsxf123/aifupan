<template>
  <my-dialog
      v-model="dialogVisible"
      :class="{ 'mobile-dialog-innner-custom': isMobile }"
      :footer="packageList?.length > 0"
      :fullscreen="isMobile"
      :maxHeight="maxHeight"
      :title="title"
      :width="width"
      @close="close"
      @submit="submit"
  >
    <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        class="demo-ruleForm upgrade-form"
        label-width="120px"
    >
      <!-- 当前版本信息 -->
      <el-form-item class="current-version" label="当前版本">
        <div class="version-info">
          <span class="version-name"></span>
          <el-tag class="version-tag" type="danger"
          >{{ userOrder.commodityName }}
          </el-tag>
          <el-tag class="version-tag" type="primary"
          >剩余天数: {{ userOrder.surplusDay ?? 0 }} 天
          </el-tag>
          <el-tag class="version-tag" type="primary"
          >剩余价格: {{ userOrder.surplusAmount / 100 ?? 0 }} 元
          </el-tag>
        </div>
      </el-form-item>

      <div v-if="packageList?.length > 0" class="upgrade-content">
        <!-- 版本选择 -->
        <el-form-item
            class="package-select"
            label="版本选择"
            prop="commodityId"
        >
          <el-select
              v-model="form.commodityId"
              class="version-select"
              clearable
              placeholder="请选择版本"
              @change="handlePackageChange"
          >
            <el-option
                v-for="item in packageList"
                :key="`package-${item.id}`"
                :label="item.name"
                :value="item.id"
            >
              <span class="option-name">{{ item.name }}</span>
            </el-option>
          </el-select>
        </el-form-item>

        <!-- 价格选择 -->
        <el-form-item
            class="price-select"
            label="价格选择"
            prop="commodityPriceId"
        >
          <el-select
              v-model="form.commodityPriceId"
              :disabled="!form.commodityId"
              :placeholder="pricePlaceHolder"
              class="price-option-select"
              clearable
              @change="handlePriceChange"
          >
            <el-option
                v-for="item in commodityPriceList"
                :key="`price-${item.id}`"
                :label="`${item.realPrice}元/${item.validityNum}${getLabel(
                orderDict.timeUnit,
                item.validityUnit
              )}`"
                :value="item.id"
            >
              <div class="price-option">
                <span class="real-price">{{ item.realPrice }}元</span>
                <span class="validity">
                  /{{
                    item.validityNum
                  }}{{ getLabel(orderDict.timeUnit, item.validityUnit) }}
                  <span v-if="item.discount < 1" class="discount-badge"
                  >{{ item.discount * 10 }}折</span
                  >
                </span>
                <span v-if="item.discount < 1" class="original-price">
                  (原价: {{ item.originalPrice }}元)
                </span>
              </div>
            </el-option>
          </el-select>
        </el-form-item>

        <!-- 当前状态 -->
        <el-form-item class="status-info" label="当前状态">
          <el-tag
              v-if="form.trialOrder !== null"
              :type="form.trialOrder === 1 ? 'danger' : 'success'"
          >
            {{ form.trialOrder === 1 ? '试用' : '正式版' }}
          </el-tag>
          <el-tag v-else type="info">无</el-tag>
        </el-form-item>

        <!-- 支付凭证 -->
        <el-form-item
            :rules="payPicturesProp"
            class="upload-section"
            label="支付凭证"
            prop="payPictures"
        >
          <div class="upload-tip">
            <span>请上传支付凭证图片</span>
            <el-tooltip
                content="支持JPG/PNG格式"
                placement="top"
                style="margin-top: 5px"
            >
              <InfoFilled style="width: 15px;height: 15px;color: #909399"/>
            </el-tooltip>
          </div>
          <UploadImg
              :beforeUpload="beforeUpload"
              :fileList="fileList"
              class="upload-component"
              multiple
              @on-remove="handleRemove"
              @on-success="handleSuccess"
              @clear-fileList="clearFileList"
              @on-handleFail="onHandleFail"
          />
        </el-form-item>

        <!-- 实付金额 -->
        <el-form-item class="payment-amount" label="实付金额" prop="totalPrice">
          <el-input
              v-model="form.totalPrice"
              :min="0"
              class="amount-input"
              placeholder="请输入实付金额"
              type="number"
              @blur="formatNumber"
          >
            <template #append>元</template>
          </el-input>
          <el-tooltip
              class="amount-tip"
              content="计算公式：新版本价格 - ( (原版本总价 / 原版本总天数) × 原版本剩余天数 )"
              placement="top"
          >
            <InfoFilled style="width: 15px;height: 15px;margin-left: 10px;color: #909399"/>
          </el-tooltip>
          <div
              v-if="!form.totalPrice && form.totalPrice !== 0"
              class="amount-warning"
          >
            <SvgIcon name="notice"></SvgIcon>
            <span>请确认金额是否为0元</span>
          </div>
        </el-form-item>
      </div>

      <div v-else class="no-upgrade">
        <el-empty :image-size="100" description="暂无可升级版本"></el-empty>
      </div>
    </el-form>
  </my-dialog>
</template>

<script setup>
import { ref, reactive, computed, nextTick } from 'vue'
import MyDialog from '@/components/commonComponent/myDialog.vue'
import UploadImg from '@/components/upload-img-box/index.vue'
import api from '@/utils/request-api'
import { useDict } from '@/hooks/useDict'

const emit = defineEmits(['close', 'is-ok'])
const props = defineProps({
  isMobile: {
    type: Boolean,
    default: false
  }
})

const {orderDict, getLabel} = useDict()

const formRef = ref(null)
const dialogVisible = ref(false)
const title = ref('')
const width = ref('700px')
const maxHeight = ref(false)

const form = reactive({
  beforeUpgrading: '',
  commodityId: '',
  commodityPriceId: '',
  discountRate: null,
  trialOrder: null,
  payPictures: '',
  totalPrice: ''
})

const packageList = ref([])
const packageListAll = ref([])
const userId = ref({})
const userOrder = ref({})
const selectOrderData = reactive({
  oneOrderPrice: 0,
  oldOrderPrice: 0
})
const fileList = ref([])

const checkDiscountRate = (rule, value, callback) => {
  if (value === null || value === undefined || value === '') {
    callback(new Error('实付金额不能为空'))
  }
  if (value < 0) {
    callback(new Error('实付金额不能小于0'))
  } else {
    callback()
  }
}

const rules = reactive({
  commodityId: [{required: true, message: '请选择版本', trigger: 'change'}],
  commodityPriceId: [
    {required: true, message: '请选择价格', trigger: 'change'}
  ],
  totalPrice: [
    {required: true, validator: checkDiscountRate, trigger: 'blur'}
  ],
  trialOrder: [{required: true, message: '请选择试用', trigger: 'change'}]
})
const commodityPriceList = computed(() => {
  if (form.commodityId) {
    return (
        packageList.value.find((item) => item.id === form.commodityId)
            ?.commodityPriceList ?? []
    )
  }
  return []
})

const pricePlaceHolder = computed(() => {
  if (form.commodityId) {
    return '请选择价格'
  } else {
    return '请先选择版本'
  }
})

const payPicturesProp = computed(() => {
  if (form.trialOrder === 0) {
    return [{required: true, message: '请上传支付凭证', trigger: 'change'}]
  }
  return []
})

const selectPackage = (item) => {
  form.commodityId = item.id
  form.commodityName = item.name
}

const selectPrice = (item) => {
  form.commodityPriceId = item.id
  form.originalPrice = item.originalPrice
  form.realPrice = item.realPrice
  let price = item.realPrice - selectOrderData.oldOrderPrice
  form.totalPrice = Math.round((price ?? 0) < 0 ? 0 : price)
}
const selectBeforeUpgrading = (id) => {
  let data = userOrder.value
  if (packageListAll.value?.length > 0) {
    let tempArray = JSON.parse(JSON.stringify(packageListAll.value))
    packageList.value = tempArray.filter((item) => {
      if (item.level !== 0) {
        // item.commodityPriceList = item?.commodityPriceList?.filter(val => {
        //   if (data.level <= 0) return true
        //   return false;
        // }) ?? []
      }
      return item.level > data.level
    })
  }
  if (data) {
    // 获取一天的价格
    let onePrice = data.realPrice / data.totalDay
    selectOrderData.oneOrderPrice = parseInt(onePrice) / 100

    // 获取剩余的价格
    selectOrderData.oldOrderPrice = parseInt(data.surplusDay * onePrice) / 100

    // 查询是否选择了升级的版本
    if (form.commodityId && form.commodityPriceId) {
      let price = (form.realPrice ?? 0) - selectOrderData.oldOrderPrice
      form.totalPrice = price ?? 0
    }
  }
}
const packageUpgrade = async (userIdParam) => {
  title.value = '版本升级'
  userId.value = userIdParam
  dialogVisible.value = true
  try {
    let res1 = await api.order.userVersionOrder({userId: userIdParam})
    if (res1?.code == 0 && res1.data) {
      userOrder.value = res1.data
    } else {
      close()
      return
    }

    let res = await api.package.list({limit: -1})
    if (res?.code == 0 && res.data.list.length > 0) {
      // 排序，小到大
      res.data.list.sort((a, b) => a.level - b.level)
      res.data.list = res.data.list.filter(
          (item) => item.status == 1 && item.packageType == 1
      )
      res.data.list.forEach((item) => {
        if (item.commodityPriceList.length > 0) {
          item.commodityPriceList.forEach((priceItem) => {
            priceItem.realPrice = priceItem.realPrice / 100
            priceItem.originalPrice = priceItem.originalPrice / 100
          })
        }
      })
      packageListAll.value = res.data.list
      selectBeforeUpgrading()
    } else {
      close()
      return
    }
  } catch (error) {
    close()
  }
}
const formatNumber = () => {
  // 如果数字小于 0，返回 0
  if (form.totalPrice < 0) {
    form.totalPrice = 0
  }
  // 返回数字，保留两位小数
  form.totalPrice = Number(Number(form.totalPrice).toFixed(2))
}
const clearFileList = () => {
  fileList.value = []
}
const submit = () => {
  formRef.value.validate((valid) => {
    if (valid) {
      let commodity = packageList.value.find(
          (item) => item.id === form.commodityId
      )
      ElMessageBox.confirm(`确定选择【${commodity.name}】吗？`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        let temp = Number(Number(form.realPrice - form.totalPrice).toFixed(2))
        let data = {
          commodityId: form.commodityId,
          userId: userId.value,
          commodityPriceId: form.commodityPriceId,
          commodityType: 1,
          discountRate: temp * 100,
          beforeUpgrading: form.beforeUpgrading,
          trialOrder: form.trialOrder,
          payPictures: form.payPictures
        }
        const res = await api.order.pcUpgradeOrder(data)
        if (res?.code === 0) {
          ElMessage.success(res.msg)
          emit('is-ok')
          emit('close')
        }
      })
    }
  })
}
const close = () => {
  dialogVisible.value = false
  emit('close')
}

const handlePackageChange = (val) => {
  form.commodityId = ''
  form.commodityName = ''
  form.commodityPriceId = ''
  form.trialOrder = null
  form.totalPrice = ''
  // 检索出当前的版本
  let currentVersion = packageList.value.find((item) => item.id === val)
  selectPackage(currentVersion)
  nextTick(() => {
    formRef.value.clearValidate('commodityPriceId')
  })
}

const handlePriceChange = (val) => {
  // 检索出当前的价格项
  let priceItem = commodityPriceList.value.find((item) => item.id === val)
  form.trialOrder = priceItem.showStatus === 0 ? 1 : 0
  selectPrice(priceItem)
}
const handleRemove = (file, fileListParam) => {
  form.payPictures = fileListParam
      .map((item) => item.response.data.id)
      .join(',')
  formRef.value.validateField('payPictures')
  fileList.value = fileListParam
}

const handleSuccess = (fileListParam) => {
  form.payPictures = fileListParam
      .map((item) => {
        if (item.response?.data.id) {
          return item.response.data.id
        }
      })
      .join(',')
  formRef.value.validateField('payPictures')
  fileList.value = fileListParam
}

const beforeUpload = (file) => {
  const allowedTypes = ['image/jpeg', 'image/png']
  const isAllowed = allowedTypes.includes(file.type)

  if (!isAllowed) {
    ElMessage.error('只能上传 JPG/PNG 格式的图片！')
    return false
  }

  return true
}

const onHandleFail = (file) => {
  fileList.value = fileList.value.filter((item) => item.uid !== file.uid)
  form.payPictures = fileList.value
      .map((item) => {
        if (item.response?.data.id) {
          return item.response.data.id
        }
      })
      .join(',')
}

defineExpose({
  packageUpgrade
})
</script>

<style lang="scss" scoped>
.upgrade-form {
  padding: 20px;

  .current-version {
    margin-bottom: 30px;

    .version-info {
      display: flex;
      align-items: center;
      gap: 15px;

      .version-name {
        font-weight: bold;
        color: red;
      }

      .version-tag {
        font-size: 14px;
      }
    }
  }

  .upgrade-content {
    .package-select,
    .price-select {
      margin-bottom: 25px;

      .version-select,
      .price-option-select {
        width: 100%;
        // max-width: 400px;
      }
    }

    .price-option {
      display: flex;
      align-items: center;
      gap: 8px;

      .real-price {
        font-weight: bold;
        color: #f56c6c;
      }

      .discount-badge {
        background-color: #f56c6c;
        color: white;
        padding: 0 5px;
        border-radius: 3px;
        font-size: 12px;
      }

      .original-price {
        color: #999;
        text-decoration: line-through;
        font-size: 12px;
      }
    }

    .status-info {
      margin-bottom: 25px;
    }

    .upload-section {
      margin-bottom: 25px;

      .upload-tip {
        margin-bottom: 10px;
        color: #666;
        display: flex;
        align-items: center;
        gap: 5px;
      }

      .upload-component {
        width: 100%;
      }
    }

    .payment-amount {
      .amount-input {
        width: 200px;
      }

      .amount-tip {
        margin-left: 10px;
        color: #909399;
        cursor: pointer;
      }

      .amount-warning {
        margin-left: 15px;
        color: #f56c6c;
        display: flex;
        align-items: center;
      }
    }
  }

  .no-upgrade {
    padding: 40px 0;
    text-align: center;
    color: #999;
  }
}

:deep(.el-form-item .el-form-item__label) {
  padding: 0 12px 0 0 !important;
}
</style>
