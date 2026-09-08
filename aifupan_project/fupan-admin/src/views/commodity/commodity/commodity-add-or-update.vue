<template>
  <el-dialog
    :title="!dataForm.id ? '新增' : '修改'"
    :close-on-click-modal="false"
    v-model="visible"
    :width="700"
  >
    <el-form
      :model="dataForm"
      :rules="dataRule"
      ref="dataFormRef"
      label-width="120px"
    >
      <el-form-item label="商品名" prop="name">
        <el-input v-model.trim="dataForm.name" placeholder="商品名"></el-input>
      </el-form-item>
      <el-form-item label="商品类型" prop="commodityTypeId">
        <el-select v-model="dataForm.commodityTypeId" placeholder="请选择">
          <el-option
            v-for="item in commodityTypeList"
            :key="`commodity-type-${item.id}`"
            :label="item.name"
            :value="item.id"
          >
          </el-option>
        </el-select>
      </el-form-item>
      <el-form-item
        prop="number"
        :label="`数量${
          dataForm.commodityTypeUnit ? `(${dataForm.commodityTypeUnit})` : ''
        }`"
      >
        <el-input
          v-model.trim="dataForm.number"
          placeholder="请输入"
          type="number"
        ></el-input>
      </el-form-item>
      <el-form-item label="商品状态" prop="status">
        <el-select v-model="dataForm.status" placeholder="请选择">
          <el-option
            v-for="(item, index) in ['下架', '上架']"
            :key="`status-${index}`"
            :label="item"
            :value="index"
          >
          </el-option>
        </el-select>
      </el-form-item>
      <el-form-item label="价格体系" class="priceContainer">
        <div
          style="width: 100%"
          v-if="
            dataForm.commodityPriceList &&
            dataForm.commodityPriceList.length > 0
          "
        >
          <div class="priceTitleContainer">
            <div class="priceTitleItem right-border">有效期</div>
            <div class="priceTitleItem right-border">有效期单位</div>
            <div class="priceTitleItem right-border">价格(元)</div>
            <div class="priceTitleItem">操作</div>
          </div>
          <template v-if="dataForm.commodityPriceList?.length > 0">
            <div
              v-for="(priceItem, priceIndex) in dataForm.commodityPriceList"
              :key="`price-item-${priceIndex}`"
              class="priceItemContainer"
            >
              <div class="priceItem right-border bottom-border left-border">
                <el-input
                  v-model="dataForm.commodityPriceList[priceIndex].validityNum"
                  type="number"
                  placeholder="请输入"
                ></el-input>
              </div>
              <div class="priceItem right-border bottom-border">
                <el-select
                  v-model="dataForm.commodityPriceList[priceIndex].validityUnit"
                  placeholder="请选择"
                >
                  <template
                    v-for="item in orderDict.timeUnit"
                    :key="`time-unit-${item.value}`"
                  >
                    <el-option
                      v-if="!item.isDelete"
                      :label="item.label"
                      :value="item.value"
                    >
                    </el-option>
                  </template>
                </el-select>
              </div>
              <div class="priceItem right-border bottom-border">
                <el-input
                  v-model="
                    dataForm.commodityPriceList[priceIndex].originalPrice
                  "
                  type="number"
                  placeholder="请输入"
                ></el-input>
              </div>
              <div class="priceItem right-border bottom-border">
                <span
                  style="color: blue; cursor: pointer"
                  @click="addPrice"
                  v-if="priceIndex == dataForm.commodityPriceList.length - 1"
                  >添加</span
                >
                <span
                  style="color: red; margin-left: 10px; cursor: pointer"
                  @click="removePrice(priceIndex)"
                  >删除</span
                >
              </div>
            </div>
          </template>
        </div>
        <div v-else>
          <el-button type="primary" size="small" @click="addPrice"
            >添加价格</el-button
          >
        </div>
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
import { ref, reactive, watch, nextTick } from 'vue'
import { useDict } from '@/hooks/useDict.js'
import { useCommonHooks } from '@/hooks/useCommonHooks.js'
import api from '@/utils/request-api'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['update:visible', 'refreshDataList'])

const { orderDict } = useDict()
const { setConvertUnitValue, getConvertUnitValue } = useCommonHooks()

const dataFormRef = ref()
const visible = ref(false)

const dataForm = reactive({
  id: null,
  name: '',
  commodityTypeId: '',
  correlationsId: '',
  isPackage: 0,
  originalPrice: '',
  discount: '',
  realPrice: '',
  number: '',
  validityNum: '',
  validityUnit: 1,
  status: 1,
  commodityPriceList: [],
  commodityTypeUnit: '',
  commodityTypeName: '',
  commodityTypeCode: '',
})

const commodityTypeList = ref([])
const vipLevelList = ref([])
const tagList = ref([])
const priceListAll = ref([])

const validateNumber = (rule, value, callback) => {
  if (!value) {
    callback(new Error('数量不能为空'))
  } else if (/^(0\d*)$/.test(value)) {
    callback(new Error('不能以0开头'))
  } else if (!/^(0|[1-9]\d*)$/.test(value)) {
    callback(new Error('数量为正整数'))
  } else {
    callback()
  }
}

const dataRule = reactive({
  name: [{ required: true, message: '商品名不能为空', trigger: 'blur' }],
  commodityTypeBean: [
    { required: true, message: '商品类型不能为空', trigger: 'blur' },
  ],
  isPackage: [{ required: true, message: '选项不能为空', trigger: 'blur' }],
  originalPrice: [
    { required: true, message: '原价格不能为空', trigger: 'blur' },
  ],
  realPrice: [{ required: true, message: '折后价格不能为空', trigger: 'blur' }],
  number: [
    { required: true, validator: validateNumber, trigger: ['blur', 'change'] },
  ],
  validityNum: [{ required: true, message: '有效期不能为空', trigger: 'blur' }],
  status: [{ required: true, message: '商品状态不能为空', trigger: 'blur' }],
  correlationsId: [
    { required: true, message: '会员等级不能为空', trigger: 'blur' },
  ],
})

watch(
  () => props.visible,
  (newVal) => {
    visible.value = newVal
  }
)

watch(visible, (newVal) => {
  emit('update:visible', newVal)
})

watch(
  dataForm,
  (newVal, oldVal) => {
    if (dataForm.commodityTypeId) {
      const find = commodityTypeList.value.find(
        (item) => item.id === dataForm.commodityTypeId
      )
      if (find) {
        dataForm.commodityTypeUnit = find?.unit
        dataForm.commodityTypeName = find?.name
        dataForm.commodityTypeCode = find?.code
      }
    }
    if (newVal.commodityPriceList && newVal.commodityPriceList.length > 0) {
      newVal.commodityPriceList.forEach((item, index) => {
        if (item.originalPrice && item.discount) {
          let temp = (item.originalPrice * item.discount)
            .toFixed(2)
            .replace(/\.?0+$/, '')
          dataForm.commodityPriceList[index].realPrice = temp
        }
      })
    }
  },
  { deep: true }
)

const removePrice = async (index) => {
  let priceId = dataForm?.commodityPriceList[index]?.id
  if (!priceId) {
    dataForm.commodityPriceList.splice(index, 1)
    return
  }

  const res = await api.commodity.isDeletePriceId({ priceId: priceId })
  if (res && res.code === 0) {
    if (res.data) {
      dataForm.commodityPriceList.splice(index, 1)
    } else {
      ElMessage.error('当前的价格正在使用中，不能删除')
    }
  } else {
    ElMessage.error(res.msg)
  }
}

const addPrice = () => {
  if (!dataForm.commodityPriceList) {
    dataForm.commodityPriceList = []
  }
  dataForm.commodityPriceList.push({
    validityUnit: 2,
    type: 0,
  })
}

const getCommodityTypeList = async () => {
  commodityTypeList.value = []
  const res = await api.commoditytype.list({ limit: -1 })
  if (res.code == 0) {
    commodityTypeList.value = res.data.list
  }
}

const init = async (id) => {
  await getCommodityTypeList()
  dataForm.commodityPriceList = []

  dataForm.id = id || null
  visible.value = true

  await nextTick(() => {
    dataFormRef.value?.resetFields()
    dataForm.number = ''
    dataForm.correlationsId = ''

    if (dataForm.id) {
      api.commodity
        .info({ id: dataForm.id }, { showLoading: true })
        .then((data) => {
          if (data && data.code === 0) {
            Object.assign(dataForm, data.data)
            dataForm.commodityPriceList.forEach((item) => {
              if (item.originalPrice)
                item.originalPrice = item.originalPrice / 100
              if (item.realPrice) item.realPrice = item.realPrice / 100
            })
            dataForm.number = setConvertUnitValue(
              dataForm.number,
              dataForm.commodityTypeCode ?? dataForm?.commodityType?.code
            )
            priceListAll.value = []
            if (dataForm.commodityPriceList) {
              priceListAll.value = JSON.parse(
                JSON.stringify(dataForm.commodityPriceList)
              )
            }
          }
        })
    }
  })
}

const dataFormSubmit = () => {
  dataFormRef.value?.validate(async (valid) => {
    if (valid) {
      let data = JSON.parse(JSON.stringify(dataForm))
      if (!data.commodityPriceList || data.commodityPriceList.length < 1) {
        ElMessage.error('请至少添加一个价格')
        return
      }

      let allowAdd = true
      let errorMsg = ''
      data.commodityPriceList.forEach((item) => {
        if (!item.originalPrice || item.originalPrice <= 0) {
          errorMsg = errorMsg ? errorMsg : '请完善价格，价格需要大于0元'
          allowAdd = false
        }
        if (!item.validityNum || item.validityNum <= 0) {
          errorMsg = errorMsg ? errorMsg : '请完善有效期，值需要大于0'
          allowAdd = false
        }
      })
      if (!allowAdd) {
        ElMessage.error(errorMsg)
        return
      }

      data.commodityPriceList.forEach((item) => {
        item.originalPrice = item.originalPrice * 100
        if (item.discount) {
          item.realPrice = item.originalPrice * item.discount
        }
      })
      data.number = getConvertUnitValue(
        data.number,
        dataForm.commodityTypeCode ?? dataForm?.commodityType?.code
      )

      if (priceListAll.value?.length > 0) {
        priceListAll.value.forEach((item) => {
          let tempData = data.commodityPriceList.find(
            (item2) => item2.id === item.id
          )
          if (!tempData) {
            data.commodityPriceList.push({ id: item.id, isDeleted: 1 })
          }
        })
      }

      const res = await api.commodity.saveOrUpdate(data)
      if (res?.code === 0) {
        ElMessage.success(res.msg)
        visible.value = false
        emit('refreshDataList')
      } else {
        ElMessage.error(res.msg)
      }
    }
  })
}

defineExpose({
  init,
})
</script>

<style lang="less" scoped>
.priceContainer {
  :deep(.el-input__inner) {
    border: none;
    border-radius: 0;
  }
}

.right-border {
  border-right: 0.5px solid #ddd;
}

.left-border {
  border-left: 0.5px solid #ddd;
}

.top-border {
  border-top: 0.5px solid #ddd;
}

.bottom-border {
  border-bottom: 0.5px solid #ddd;
}

.priceItem {
  display: flex;
  align-items: center;
  justify-content: center;
  box-sizing: border-box;
  flex: 1;
  height: 42px;
}

.priceItemContainer {
  display: flex;
  align-items: center;
  width: 100%;
}

.priceTitleItem {
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #e1e1e1;
  font-size: 15px;
  box-sizing: border-box;
  height: 38px;
  flex: 1;
}

.priceTitleContainer {
  display: flex;
  align-items: center;
  width: 100%;
  justify-content: space-between;
}

.el-select {
  width: 100%;
}
.priceItem :deep(.el-select__wrapper) {
  border: none !important;
  box-shadow: none !important;
}

.priceItem :deep(.el-input__wrapper) {
  border: none !important;
  box-shadow: none !important;
}
</style>
