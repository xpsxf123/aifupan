<template>
  <my-dialog
      v-model="dialogVisible"
      :class="{ 'mobile-dialog-innner-custom': isMobile }"
      :footer="footer"
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
        class="demo-ruleForm"
        label-width="120px"
    >
      <!--      <el-form-item label="当前版本" prop="beforeUpgrading">-->
      <!--        <div v-for="(item, index) in userOrderList" style="margin-bottom: 10px;" :style="`${item.isUse == 1 ? 'color: green;' : ''}`">-->
      <!--          {{ item.title }}(使用时间：{{ formatDate(item.startDate, 'yyyy-MM-dd') }}~{{ formatDate(item.endDate, 'yyyy-MM-dd') }}，总订单天数：{{ item.totalDay }}，使用天数：{{ item.useDay }}，剩余天数：{{ item.surplusDay }})-->
      <!--        </div>-->
      <!--      </el-form-item>-->
      <el-form-item label="当前版本">
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
      <el-form-item label="版本选择" prop="commodityId">
        <el-select v-model="form.commodityId" disabled placeholder="请选择">
          <el-option
              v-for="item in packageList"
              :key="`package-${item.id}`"
              :label="item.name"
              :value="item.id"
          >
          </el-option>
        </el-select>
      </el-form-item>
      <el-form-item label="价格选择" prop="commodityPriceId">
        <el-select
            v-model="form.commodityPriceId"
            placeholder="请选择"
            @change="handleChange"
        >
          <el-option
              v-for="item in commodityPriceList"
              :key="`price-${item.id}`"
              :label="`${item.realPrice}/${item.validityNum}${getLabel(
              orderDict.timeUnit,
              item.validityUnit
            )}`"
              :value="item.id"
          >
          </el-option>
        </el-select>
      </el-form-item>
      <el-form-item label="实付金额" prop="totalPrice" style="color: red">
        <!--        <el-input v-model="form.totalPrice" type="number" :min="0" :max="form.realPrice" placeholder="请输入实付金额" style="width: 200px;">-->
        <el-input
            v-model="form.totalPrice"
            :min="0"
            placeholder="请输入实付金额"
            type="number"
        >
        </el-input>
      </el-form-item>
    </el-form>
  </my-dialog>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import MyDialog from '@/components/commonComponent/myDialog.vue'
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

const dialogVisible = ref(false)
const title = ref('')
const width = ref(550)
const maxHeight = ref(false)
const footer = ref(true)
const appendToBody = ref(false)
const formRef = ref(null)

const form = reactive({
  beforeUpgrading: '',
  commodityId: '',
  commodityPriceId: '',
  discountRate: null,
  totalPrice: ''
})

const packageList = ref([])
const userId = ref({})
const userOrderList = ref([])
const userOrder = ref({})
const selectOrderData = reactive({
  oneOrderPrice: 0,
  oldOrderPrice: 0
})
const showSuggestBut = ref(false)

const checkDiscountRate = (rule, value, callback) => {
  if (value === null || value === undefined || value === '') {
    return callback(new Error('实付价格不能为空'))
  }
  if (isNaN(value)) {
    return callback(new Error('请输入正确的值'))
  }
  if (value < 0) {
    return callback(new Error('实付价格不能小于0'))
  }
  return callback()
}

const rules = reactive({
  commodityId: [{required: true, message: '请选择版本', trigger: 'blur'}],
  commodityPriceId: [
    {required: true, message: '请选择价格', trigger: 'blur'}
  ],
  totalPrice: [
    {required: true, validator: checkDiscountRate, trigger: 'blur'}
  ]
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

const selectPackage = (item) => {
  form.commodityId = item.id
  form.commodityName = item.name
  if (item.commodityPriceList.length > 0) {
    selectPrice(item.commodityPriceList[0])
  }
}

const selectPrice = (item) => {
  form.commodityPriceId = item.id
  form.originalPrice = item.originalPrice
  form.realPrice = item.realPrice
  form.totalPrice = item.realPrice
}

const handleChange = (val) => {
  const priceItem = commodityPriceList.value.find((item) => item.id === val)
  selectPrice(priceItem)
}
const getOrder = async (userIdParam) => {
  try {
    const res = await api.order.userVersionOrder({userId: userIdParam})
    if (res?.code == 0 && res.data) {
      userOrder.value = res.data
      return true
    }
    return false
  } catch (error) {
    console.error('获取订单信息失败:', error)
    return false
  }
}
const packageUpgrade = async (userIdParam) => {
  if (!userIdParam) return
  title.value = '版本续费'
  userId.value = userIdParam

  try {
    if (await getOrder(userIdParam)) {
      const res = await api.package.userRenewal({userId: userIdParam})
      if (res?.code == 0 && res.data) {
        res.data.forEach((item) => {
          if (item.commodityPriceList.length > 0) {
            item.commodityPriceList.forEach((priceItem) => {
              priceItem.realPrice = priceItem.realPrice / 100
              priceItem.originalPrice = priceItem.originalPrice / 100
            })
          }
        })
        packageList.value = res.data
        if (packageList.value.length > 0) {
          selectPackage(packageList.value[0])
        }
        dialogVisible.value = true
        return
      }
    }
  } catch (error) {
    console.error('版本升级失败:', error)
  }
  close()
}
const submit = () => {
  formRef.value.validate(async (valid) => {
    if (valid) {
      try {
        const commodity = packageList.value.find(
            (item) => item.id === form.commodityId
        )
        await ElMessageBox.confirm(
            `确定选择【${commodity.name}】吗？`,
            '提示',
            {
              confirmButtonText: '确定',
              cancelButtonText: '取消',
              type: 'warning'
            }
        )

        const temp = Number(Number(form.realPrice - form.totalPrice).toFixed(2))
        const data = {
          commodityId: form.commodityId,
          userId: userId.value,
          commodityPriceId: form.commodityPriceId,
          commodityType: 1,
          discountRate: temp * 100
        }

        const res = await api.order.pcRenewalOrder(data)
        if (res?.code == 0) {
          ElMessage.success(res?.msg || '续费成功')
          emit('is-ok')
          emit('close')
        } else {
          ElMessage.error(res?.msg ?? '续费失败')
        }
      } catch (error) {
        if (error !== 'cancel') {
          console.error('续费失败:', error)
          ElMessage.error('续费失败')
        }
      }
    }
  })
}
const close = () => {
  dialogVisible.value = false
  emit('close')
}

defineExpose({
  packageUpgrade
})
</script>

<style lang="less" scoped>
:deep(.el-form-item .el-form-item__label) {
  padding: 0 12px 0 0 !important;
}

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
</style>
