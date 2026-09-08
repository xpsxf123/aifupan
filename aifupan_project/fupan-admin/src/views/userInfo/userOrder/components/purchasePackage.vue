<template>
  <my-dialog
      v-model="dialogVisible"
      :footer="packageList?.length > 0"
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
        label-suffix=":"
        label-width="120px"
    >
      <el-form-item label="选择编辑版本" prop="beforeUpgrading">
        <el-radio-group
            v-model="form.beforeUpgrading"
            @input="selectBeforeUpgrading"
        >
          <div
              v-for="item in userOrderList"
              :key="`order-${item.id}`"
              style="margin-bottom: 10px"
          >
            <el-radio
                :disabled="item.disable"
                :label="item.id"
                :value="item.id"
                border
            >
              {{ item.title }}(使用时间：{{
                formatDate(item.startDate, 'yyyy-MM-dd')
              }}~{{ formatDate(item.endDate, 'yyyy-MM-dd') }}，价格：{{
                item.totalPrice / 100
              }}，总订单天数：{{ item.totalDay }}，使用天数：{{
                item.useDay
              }}，剩余天数：{{ item.surplusDay }})
            </el-radio>
          </div>
        </el-radio-group>
      </el-form-item>
      <div v-if="packageList?.length > 0">
        <el-form-item label="更换版本选择" prop="commodityId">
          <div class="general">
            <div
                v-for="item in packageList"
                :key="`package-${item.id}`"
                class="item"
                @click="selectPackage(item)"
            >
              <el-card
                  :class="{ 'my-select': form.commodityId === item.id }"
                  class="zhong"
                  style="width: 100%; height: 100%"
              >
                <div class="title">
                  {{ item.name }}
                </div>
              </el-card>
            </div>
          </div>
        </el-form-item>
        <el-form-item label="更换价格选择" prop="commodityPriceId">
          <div class="general">
            <div
                v-for="item in commodityPriceList"
                :key="`price-${item.id}`"
                class="item item-price"
                @click="selectPrice(item)"
            >
              <el-card
                  :class="{ 'my-select': form.commodityPriceId === item.id }"
                  class="zhong content"
                  style="width: 100%; height: 100%"
              >
                <div v-if="item.discount != 1" class="discount">
                  {{ item.discount * 10 }}折
                </div>
                <div class="title">
                  <div
                      v-if="item.discount != 1"
                      :class="{ 'my-select': form.commodityId === item.id }"
                      style="font-size: 20px; text-decoration: line-through"
                  >
                    ￥{{ item.originalPrice }}
                  </div>
                  <div
                      :class="{ 'my-select': form.commodityId === item.id }"
                      style="font-size: 20px"
                  >
                    ￥<span style="font-size: 30px"> {{ item.realPrice }}</span
                  >/{{
                      item.validityNum
                    }}{{ getLabel(orderDict.timeUnit, item.validityUnit) }}
                  </div>
                </div>
              </el-card>
            </div>
          </div>
        </el-form-item>
        <el-form-item label="建议价格" prop="totalPrice" style="color: red">
          <el-input
              v-model="form.totalPrice"
              :max="form.realPrice"
              :min="0"
              placeholder="请输入建议价格"
              style="width: 200px"
              type="number"
              @blur="formatNumber"
          >
          </el-input>
        </el-form-item>
      </div>
      <div v-else>
        <div
            style="
            height: 200px;
            width: 100%;
            display: flex;
            align-items: center;
            justify-content: center;
          "
        >
          激活版和免费版本不能编辑
        </div>
      </div>
    </el-form>
  </my-dialog>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { ElMessageBox } from 'element-plus'
import MyDialog from '@/components/commonComponent/myDialog.vue'
import { useDict } from '@/hooks/useDict.js'
import { useCommonHooks } from '@/hooks/useCommonHooks.js'
import api from '@/utils/request-api'

const emit = defineEmits(['close', 'is-ok'])

const {orderDict, getLabel} = useDict()
const {formatDate} = useCommonHooks()

const dialogVisible = ref(false)
const title = ref('')
const width = ref('50%')
const maxHeight = ref(false)
const footer = ref(true)
const appendToBody = ref(false)
const packageList = ref([])
const packageListAll = ref([])
const userId = ref(null)
const userOrderList = ref([])
const showSuggestBut = ref(false)
const formRef = ref(null)

const form = reactive({
  beforeUpgrading: '',
  commodityId: '',
  commodityPriceId: '',
  discountRate: null,
  commodityName: '',
  originalPrice: 0,
  realPrice: 0,
  totalPrice: 0
})

const selectOrderData = reactive({
  oneOrderPrice: 0,
  oldOrderPrice: 0
})

const checkDiscountRate = (rule, value, callback) => {
  if (value === null || value === undefined || value === '') {
    callback(new Error('建议价格不能为空'))
  }
  if (value < 0) {
    callback(new Error('建议价格不能小于0'))
  } else if (value > form.realPrice) {
    callback(new Error('建议价格不能大于' + form.realPrice))
  } else {
    callback()
  }
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
  form.totalPrice = Math.round(item.realPrice ?? 0)
}

const selectBeforeUpgrading = (id) => {
  form.beforeUpgrading = id
  const data = userOrderList.value.find((item) => item.id == id)
  if (packageListAll.value?.length > 0) {
    const tempArray = JSON.parse(JSON.stringify(packageListAll.value))
    packageList.value = data.disable ? [] : tempArray
    if (packageList.value?.length > 0) {
      selectPackage(packageList.value[0])
    }
  }
  if (data) {
    const onePrice = data.realPrice / data.totalDay
    selectOrderData.oneOrderPrice = parseInt(onePrice) / 100
    selectOrderData.oldOrderPrice = parseInt(data.surplusDay * onePrice) / 100

    if (form.commodityId && form.commodityPriceId) {
      const price = form.realPrice ?? 0
      form.totalPrice = price ?? 0
    }
  }
}

const packageUpgrade = async (userIdParam) => {
  title.value = '版本编辑'
  userId.value = userIdParam

  const res1 = await api.order.getOrderByUserId({userId: userIdParam})
  if (res1?.code == 0 && res1.data) {
    if (res1.data?.length > 0) {
      res1.data.forEach((item) => {
        const date1 = new Date(item.startDate)
        const date2 = new Date(item.endDate)
        const timeDiff = Math.abs(date2 - date1)
        item.totalDay = Math.floor(timeDiff / (1000 * 60 * 60 * 24))

        if (item.status == 2) {
          const now = new Date()
          const nowDiff = Math.abs(now - date1)
          item.useDay = Math.floor(nowDiff / (1000 * 60 * 60 * 24))
          item.surplusDay = item.totalDay - item.useDay
        } else {
          item.useDay = 0
          item.surplusDay = item.totalDay
        }
        item.disable = item.level <= 0
      })
      userOrderList.value = res1.data
      if (userOrderList.value.length > 0) {
        selectBeforeUpgrading(userOrderList.value[0].id)
      }
    }
  } else {
    close()
    return
  }

  const res = await api.package.list({limit: -1})
  if (res?.code == 0 && res.data.list.length > 0) {
    res.data.list.sort((a, b) => a.level - b.level)
    res.data.list = res.data.list.filter(
        (item) => item.status == 1 && item.packageType == 1 && item.level > 0
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
    if (form.beforeUpgrading) {
      console.log('form.beforeUpgrading', form.beforeUpgrading)
      selectBeforeUpgrading(form.beforeUpgrading)
    }
    if (packageList.value.length > 0) {
      selectPackage(packageList.value[0])
    }
  } else {
    close()
    return
  }
  dialogVisible.value = true
}

const formatNumber = () => {
  if (form.totalPrice < 0) {
    form.totalPrice = 0
  }
  form.totalPrice = Number(Number(form.totalPrice).toFixed(2))
}

const submit = async () => {
  const valid = await formRef.value.validate()
  if (valid) {
    const commodity = packageList.value.find(
        (item) => item.id === form.commodityId
    )
    try {
      await ElMessageBox.confirm(`确定选择【${commodity.name}】吗？`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      })

      const temp = Number(Number(form.realPrice - form.totalPrice).toFixed(2))
      const data = {
        commodityId: form.commodityId,
        userId: userId.value,
        commodityPriceId: form.commodityPriceId,
        commodityType: 1,
        discountRate: temp * 100,
        beforeUpgrading: form.beforeUpgrading
      }

      const res = await api.order.orderEdit(data)
      if (res?.code == 0) {
        ElMessage.success(res.msg)
        emit('is-ok')
        emit('close')
      }
    } catch (error) {
      // 用户取消操作
    }
  }
}

const close = () => {
  dialogVisible.value = false
  emit('close')
}

defineExpose({
  packageUpgrade
})
</script>

<style lang="scss" scoped>
.general {
  margin-top: 5px;
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
  width: 100%;

  .item {
    width: 200px;
    height: 100px;

    .title {
      font-size: 35px;
    }
  }
}

.content {
  position: relative;

  .discount {
    position: absolute;
    top: 0;
    right: 0;
    background-color: #3498db;
    color: white;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 16px;
    line-height: 16px;
    width: 50px;
    height: 30px;
  }
}

.item-price {
  width: 200px !important;
  height: 100px !important;
}

.my-select {
  background-color: #007aff;
  color: #ffffff;
}

.zhong {
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
