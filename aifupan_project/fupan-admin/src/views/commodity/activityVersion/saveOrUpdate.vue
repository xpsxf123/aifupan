<template>
  <myDialog
    v-model="dialog"
    :title="title"
    :width="width"
    :footer="footer"
    maxHeight
    @submit="submit"
    @close="dialog = false"
  >
    <div v-if="dialog">
      <el-form
        label-width="120px"
        :model="dataForm"
        :rules="dataRule"
        ref="dataFormRef"
      >
        <el-tabs v-model="activeName" type="card">
          <el-tab-pane label="基本信息" name="base" class="my-height">
            <el-form-item label="版本名称" prop="name">
              <el-input
                v-model="dataForm.name"
                placeholder="请填写版本名称"
                :disabled="!isAdd"
              ></el-input>
            </el-form-item>
            <el-form-item label="状态" prop="status">
              <el-select
                v-model="dataForm.status"
                placeholder="请选择状态"
                style="width: 100%"
              >
                <el-option label="下架" :value="0"></el-option>
                <el-option label="上架" :value="1"></el-option>
              </el-select>
            </el-form-item>
          </el-tab-pane>
          <el-tab-pane label="商品设置" name="commodity">
            <el-form-item label="选择商品" class="priceContainer">
              <div
                style="width: 100%"
                v-if="
                  dataForm.typeConsumptionList &&
                  dataForm.typeConsumptionList.length > 0
                "
              >
                <div class="priceTitleContainer">
                  <div class="priceTitleItem right-border">商品项</div>
                  <div class="priceTitleItem right-border">数量</div>
                  <div class="priceTitleItem right-border">单位</div>
                  <div class="priceTitleItem">操作</div>
                </div>
                <template v-if="dataForm.typeConsumptionList?.length > 0">
                  <div
                    v-for="(
                      priceItem, typeIndex
                    ) in dataForm.typeConsumptionList"
                    :key="`type-consumption-${typeIndex}`"
                    class="priceItemContainer"
                  >
                    <div
                      class="priceItem right-border bottom-border left-border"
                    >
                      <el-select
                        class="select-custom"
                        v-model="
                          dataForm.typeConsumptionList[typeIndex]
                            .commodityTypeId
                        "
                        placeholder="请选择"
                      >
                        <el-option
                          v-for="item in commodityTypeList"
                          :key="`commodity-type-${item.id}`"
                          :label="item.name"
                          :value="item.id"
                          :disabled="typeConsumptionDisabled(item)"
                        >
                        </el-option>
                      </el-select>
                    </div>
                    <div class="priceItem right-border bottom-border">
                      <el-input
                        class="input-custom"
                        v-model="dataForm.typeConsumptionList[typeIndex].number"
                        controls-position="right"
                        :min="0"
                        placeholder="请输入"
                        :step="1"
                        type="number"
                        style="width: 100%"
                      ></el-input>
                    </div>
                    <div class="priceItem right-border bottom-border">
                      <span>{{
                        dataForm.typeConsumptionList[typeIndex]
                          .commodityTypeUnit
                      }}</span>
                    </div>
                    <div class="priceItem right-border bottom-border">
                      <span
                        style="color: blue; cursor: pointer"
                        @click="addPrice2"
                        v-if="
                          typeIndex === dataForm.typeConsumptionList.length - 1
                        "
                        >添加</span
                      >
                      <span
                        style="color: red; margin-left: 10px; cursor: pointer"
                        @click="removePrice2(typeIndex)"
                        >删除</span
                      >
                    </div>
                  </div>
                </template>
              </div>
              <div v-else>
                <el-button type="primary" size="small" @click="addPrice2"
                  >添加商品</el-button
                >
              </div>
            </el-form-item>
            <el-form-item label="价格体系" class="priceContainer">
              <div
                v-if="
                  dataForm.commodityPriceList &&
                  dataForm.commodityPriceList.length > 0
                "
              >
                <div class="priceTitleContainer">
                  <div class="priceTitleItem right-border">有效期</div>
                  <div class="priceTitleItem right-border">有效期单位</div>
                  <div class="priceTitleItem right-border">原价(元)</div>
                  <div class="priceTitleItem right-border">折扣</div>
                  <div class="priceTitleItem right-border">折扣价(元)</div>
                  <div class="priceTitleItem">操作</div>
                </div>
                <template v-if="dataForm.commodityPriceList?.length > 0">
                  <div
                    v-for="(
                      priceItem, priceIndex
                    ) in dataForm.commodityPriceList"
                    :key="`price-${priceIndex}`"
                    class="priceItemContainer"
                  >
                    <div
                      class="priceItem right-border bottom-border left-border"
                    >
                      <el-input
                        v-model="
                          dataForm.commodityPriceList[priceIndex].validityNum
                        "
                        type="number"
                        placeholder="请输入"
                      ></el-input>
                    </div>
                    <div class="priceItem right-border bottom-border">
                      <el-select
                        class="select-custom"
                        v-model="
                          dataForm.commodityPriceList[priceIndex].validityUnit
                        "
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
                        controls-position="right"
                        :min="0"
                        placeholder="请输入"
                        :step="1"
                        type="number"
                        style="width: 100%"
                        @change="updateRealPrice(priceIndex)"
                      ></el-input>
                    </div>
                    <div class="priceItem right-border bottom-border">
                      <el-input-number
                        v-model="
                          dataForm.commodityPriceList[priceIndex].discount
                        "
                        controls-position="right"
                        :min="0.1"
                        :max="1"
                        placeholder="请输入"
                        :step="0.01"
                        style="width: 100%"
                        @change="updateRealPrice(priceIndex)"
                      ></el-input-number>
                    </div>
                    <div class="priceItem right-border bottom-border">
                      <el-input
                        v-model="
                          dataForm.commodityPriceList[priceIndex].realPrice
                        "
                        controls-position="right"
                        :min="0"
                        placeholder="请输入"
                        :step="1"
                        type="number"
                        style="width: 100%"
                      ></el-input>
                    </div>
                    <div class="priceItem right-border bottom-border">
                      <span
                        style="color: blue; cursor: pointer"
                        @click="addPrice"
                        v-if="
                          priceIndex === dataForm.commodityPriceList.length - 1
                        "
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
          </el-tab-pane>
          <el-tab-pane label="增量包设置" name="second" v-if="false">
            <el-form-item class="priceContainer" label-width="0">
              <div style="text-align: right">
                <el-button type="primary" size="small" @click="addCommodity()"
                  >添加增量包</el-button
                >
              </div>
              <el-table
                :data="dataForm.incrementList"
                border
                stripe
                style="width: 100%"
                :header-cell-style="{ padding: '0px' }"
                :header-row-style="{ padding: '10px' }"
                :element-loading-spinner="customSvg"
                header-row-class-name="my-header-row"
              >
                <el-table-column
                  type="index"
                  width="50"
                  align="center"
                ></el-table-column>
                <el-table-column
                  prop="commodityName"
                  label="名称"
                  align="center"
                ></el-table-column>
                <el-table-column
                  prop="commodityTypeName"
                  label="商品类型"
                  align="center"
                ></el-table-column>
                <el-table-column prop="price" label="价格" align="center">
                  <template #default="scope">
                    <span v-if="scope.row.realPrice == scope.row.originalPrice">
                      {{ scope.row.realPrice }}元/{{ scope.row.validityNum
                      }}{{
                        getLabel(orderDict.timeUnit, scope.row.validityUnit)
                      }}
                    </span>
                    <span v-else>
                      <span
                        style="color: #bababa; text-decoration: line-through"
                      >
                        ({{ scope.row.originalPrice }}元)
                      </span>
                      <span style="color: red"
                        >{{ scope.row.realPrice }}元</span
                      >
                      /{{ scope.row.validityNum
                      }}{{
                        getLabel(orderDict.timeUnit, scope.row.validityUnit)
                      }}
                    </span>
                  </template>
                </el-table-column>
                <el-table-column
                  prop="remark"
                  label="操作"
                  width="150"
                  align="center"
                >
                  <template #default="scope">
                    <span
                      style="color: blue; cursor: pointer"
                      @click="addCommodity(scope.row, scope.$index)"
                      >修改</span
                    >
                    <span
                      style="color: red; margin-left: 10px; cursor: pointer"
                      @click="removeCommodity(scope)"
                      >删除</span
                    >
                  </template>
                </el-table-column>
              </el-table>
            </el-form-item>
          </el-tab-pane>
        </el-tabs>
      </el-form>
      <div v-if="commodityDialog">
        <select-commodity
          :data-form="selectCommodity"
          @submit="commoditySubmit"
          @close="commodityDialog = false"
        ></select-commodity>
      </div>
    </div>
  </myDialog>
</template>

<script setup>
import { ref, reactive, watch, onMounted, nextTick } from 'vue'
import { useDict } from '@/hooks/useDict.js'
import { useCommonHooks } from '@/hooks/useCommonHooks.js'
import { customSvg } from '@/utils/icon.js'
import api from '@/utils/request-api'
import MyDialog from '@/components/commonComponent/myDialog.vue'
import uploadImg from '@/components/commonComponent/uploadImg.vue'
import SelectCommodity from '@/views/commodity/vipLevel/selectCommodity.vue'

const props = defineProps({
  addOrUpdateVisible: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['update:addOrUpdateVisible', 'is-ok'])

const { orderDict } = useDict()
const { getLabel, setConvertUnitValue, getConvertUnitValue } = useCommonHooks()

const timeFrame2 = ref([2, 5, 3, 4])
const dialog = ref(false)
const commodityDialog = ref(false)
const title = ref('')
const width = ref(900)
const footer = ref(true)
const activeName = ref('base')
const isAdd = ref(false)
const commodityTypeList = ref([])
const selectCommodityIndex = ref(-1)
const selectCommodity = ref({})
const dataFormRef = ref(null)

const dataForm = reactive({
  id: '',
  name: '',
  isCompress: 1,
  packageType: 2,
  status: 1,
  level: 10,
  logoImgList: [],
  detailImgList: [],
  commodityPriceList: [],
  typeConsumptionList: [],
  incrementList: [],
})

const dataRule = reactive({
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  level: [{ required: true, message: '版本等级', trigger: 'blur' }],
  isCompress: [
    { required: true, message: '请选择是否可以压缩', trigger: 'blur' },
  ],
  packageType: [{ required: true, message: '请选择版本类型', trigger: 'blur' }],
  status: [{ required: true, message: '请选择状态', trigger: 'blur' }],
  logoImgList: [{ required: true, message: '请上传logo图片', trigger: 'blur' }],
})

watch(dialog, (newVal) => {
  emit('update:addOrUpdateVisible', newVal)
})

// 监听父组件传入的visible状态
watch(
  () => props.addOrUpdateVisible,
  (newVal) => {
    console.log('saveOrUpdate组件接收到props.addOrUpdateVisible变化:', newVal)
    dialog.value = newVal
  }
)

watch(
  () => dataForm.typeConsumptionList,
  (newVal) => {
    if (newVal) {
      newVal.forEach((item, index) => {
        if (item.number && item.number !== '') {
          const regex = /^[1-9]\d*$/
          if (!regex.test(item.number)) {
            dataForm.typeConsumptionList[index].number = null
          }
        }
        if (item.commodityTypeId) {
          let temp = commodityTypeList.value.find(
            (val) => val.id === item.commodityTypeId
          )
          dataForm.typeConsumptionList[index].commodityTypeUnit = temp?.unit
          dataForm.typeConsumptionList[index].commodityTypeCode = temp?.code
          dataForm.typeConsumptionList[index].commodityTypeName = temp?.name
        }
      })
    }
  },
  { deep: true }
)

watch(
  () => dataForm.commodityPriceList,
  (newVal) => {
    if (newVal) {
      newVal.forEach((item, index) => {
        if (item.validityNum && item.validityNum !== '') {
          const regex = /^[1-9]\d*$/
          if (!regex.test(item.validityNum)) {
            dataForm.commodityPriceList[index].validityNum = null
          }
        }
      })
    }
  },
  { deep: true }
)

const updateRealPrice = (index) => {
  let item = dataForm.commodityPriceList[index]
  if (item.originalPrice && item.discount) {
    let temp = (item.originalPrice * item.discount)
      .toFixed(2)
      .replace(/\.?0+$/, '')
    dataForm.commodityPriceList[index].realPrice = temp
  }
}

const typeConsumptionDisabled = (item) => {
  let fin = dataForm?.typeConsumptionList?.find(
    (val) => val.commodityTypeId === item.id
  )
  return !!fin
}

const monthDisabled = (index) => {
  let fin = dataForm?.commodityPriceList?.find(
    (val) => val.validityUnit === index
  )
  return !!fin
}

const init = async (id, activeNameParam) => {
  if (activeNameParam) {
    activeName.value = activeNameParam
  }
  if (id) {
    isAdd.value = false
    title.value = '版本修改'
    const res = await api.package.info({ id: id }, { showLoading: true })
    if (res.code === 0) {
      if (res?.data) {
        res.data?.typeConsumptionList?.forEach((item) => {
          item.number = setConvertUnitValue(item.number, item.commodityTypeCode)
        })
        res?.data?.commodityPriceList?.forEach((item) => {
          item.originalPrice = item.originalPrice / 100
          item.realPrice = item.realPrice / 100
        })
        res?.data?.incrementList?.forEach((item) => {
          item.commodityName = item?.commodity?.name
          item.commodityTypeName = item?.commodity?.commodityTypeName
          item.originalPrice = item?.commodityPrice?.originalPrice ?? 0
          item.validityNum = item.commodityPrice?.validityNum ?? 0
          item.validityUnit = item.commodityPrice?.validityUnit ?? 2

          item.originalPrice = item.originalPrice / 100
          item.realPrice = item.realPrice / 100
        })
      }
      Object.assign(dataForm, res.data)
    }
  } else {
    isAdd.value = true
    title.value = '版本添加'
  }
  await nextTick(() => {
    dialog.value = true
  })
}

const addCommodity = (item, index) => {
  console.log('addCommodity called with:', {
    item,
    index,
    selectCommodityIndex: selectCommodityIndex.value,
  })

  if (item && index !== undefined) {
    selectCommodityIndex.value = index
    selectCommodity.value = { ...item }
  } else {
    selectCommodityIndex.value = -1
    selectCommodity.value = {}
  }
  commodityDialog.value = true
}

const commoditySubmit = (form) => {
  console.log('commoditySubmit called with:', {
    form,
    selectCommodityIndex: selectCommodityIndex.value,
    incrementListLength: dataForm.incrementList?.length || 0,
  })

  if (!dataForm?.incrementList) dataForm.incrementList = []

  if (selectCommodityIndex.value === -1) {
    console.log('Adding new commodity to incrementList')
    dataForm.incrementList.push(form)
    console.log(
      'After add - incrementList length:',
      dataForm.incrementList.length
    )
  } else {
    console.log(
      'Modifying existing commodity at index:',
      selectCommodityIndex.value
    )
    dataForm.incrementList[selectCommodityIndex.value] = form
    console.log(
      'After modify - incrementList length:',
      dataForm.incrementList.length
    )
  }

  selectCommodityIndex.value = -1
  selectCommodity.value = {}
  commodityDialog.value = false
}

const getCommodityTypeList = async () => {
  const res = await api.commoditytype.list({ limit: -1 })
  if (res.code == 0) {
    commodityTypeList.value = res.data.list
  }
}

const removePrice = (index) => {
  dataForm.commodityPriceList.splice(index, 1)
}

const removePrice2 = (index) => {
  dataForm.typeConsumptionList.splice(index, 1)
}

const removeCommodity = (item) => {
  dataForm.incrementList.splice(item.$index, 1)
}

const addPrice = () => {
  if (!dataForm.commodityPriceList) {
    dataForm.commodityPriceList = []
  }
  let unitIndex = getValidityUnit()
  if (unitIndex === -1) {
    ElMessage.error('已添加所有有效期单位')
    return
  }
  dataForm.commodityPriceList.push({
    type: 1,
    commodityId: '',
    originalPrice: null,
    discount: 1,
    realPrice: null,
    validityNum: '',
    validityUnit: unitIndex,
  })
}

const getValidityUnit = () => {
  let temp = -1
  for (let index of timeFrame2.value) {
    if (!monthDisabled(index)) {
      temp = index
      break
    }
  }
  return temp
}

const addPrice2 = () => {
  if (!dataForm.typeConsumptionList) {
    dataForm.typeConsumptionList = []
  }
  dataForm.typeConsumptionList.push({
    sourceId: '',
    commodityTypeId: '',
    commodityTypeCode: '',
    commodityTypeName: '',
    type: 1,
    number: null,
  })
}

const logoImgChange = (fileList) => {
  dataForm.logoImgList = fileList
}

const detailImgChange = (fileList) => {
  dataForm.detailImgList = fileList
}

const submit = async () => {
  const valid = await dataFormRef.value.validate()
  if (valid) {
    let data = JSON.parse(JSON.stringify(dataForm))
    if (!(data.typeConsumptionList?.length > 0)) {
      ElMessage.error('请您到商品设置中选择商品')
      return
    }
    data?.typeConsumptionList?.forEach((item) => {
      if (!item.commodityTypeId) {
        ElMessage.error('请您完善选择商品中的商品项')
        throw new Error('请您到商品设置中选择商品')
      }
      if (item.number == null || item.number == undefined) {
        ElMessage.error('请您完善选择商品中的数量')
        throw new Error('请您完善选择商品中的数量填写')
      }
      if (item.number <= 0) {
        ElMessage.error(`商品中的[${item.commodityTypeName}]数量必须大于0`)
        throw new Error('请您完善选择商品中的数量填写')
      }
    })
    if (!(data.commodityPriceList?.length > 0)) {
      ElMessage.error('请您到商品设置中设置价格')
      return
    }
    data?.typeConsumptionList?.forEach((item) => {
      item.number = getConvertUnitValue(item.number, item.commodityTypeCode)
    })
    data?.commodityPriceList?.forEach((item) => {
      if (!item.validityNum) {
        ElMessage.error('请您完善商品设置中有效期')
        throw new Error('请您完善商品设置中有效期填写')
      }
      if (!item.originalPrice) item.originalPrice = 0
      if (!item.realPrice) item.realPrice = 0
      if (item.originalPrice) {
        item.originalPrice = item.originalPrice * 100
      }
      if (item.realPrice) {
        item.realPrice = item.realPrice * 100
      }
    })
    data?.incrementList?.forEach((item) => {
      if (item.originalPrice) {
        item.originalPrice = item.originalPrice * 100
      }
      if (item.realPrice) {
        item.realPrice = item.realPrice * 100
      }
    })
    const res = await api.package.saveOrUpdate(data)
    if (res?.code == 0) {
      dialog.value = false
      emit('input', false)
      emit('is-ok')
      ElMessage.success(res.msg)
    } else {
      ElMessage.error(res.msg)
    }
  } else {
    ElMessage.error('请完善基础信息')
  }
}

onMounted(() => {
  getCommodityTypeList()
})

defineExpose({
  init,
})
</script>

<style scoped lang="less">
.commodity-all {
  .commodity-item {
    border: #cccccc solid 1px;
    padding: 10px;
  }
}

:deep(.el-tabs__content) {
  max-height: calc(65vh - 60px);
  overflow-y: auto;
  padding-right: 15px;
}

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
  height: 42px;
  flex: 1;
}

.priceItem2 {
  display: flex;
  align-items: center;
  justify-content: center;
  box-sizing: border-box;
  height: 42px;
  width: 20%;
  padding: 0 5px;
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

.priceTitleItem2 {
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #e1e1e1;
  font-size: 15px;
  box-sizing: border-box;
  height: 38px;
  width: 20%;
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

.ellipsis {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.select-custom :deep(.el-select__wrapper) {
  border: none !important;
  box-shadow: none !important;
}

.priceItem :deep(.el-input__wrapper) {
  border: none !important;
  box-shadow: none !important;
}
</style>
