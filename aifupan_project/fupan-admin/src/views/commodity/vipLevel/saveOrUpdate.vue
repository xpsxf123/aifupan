<template>
  <myDialog
      v-model="dialog"
      :footer="footer"
      :title="title"
      :width="width"
      maxHeight
      @close="handleClose"
      @submit="submit"
  >
    <div v-if="dialog">
      <el-form
          ref="dataFormRef"
          :model="dataForm"
          :rules="dataRule"
          label-width="140px"
      >
        <el-tabs v-model="activeName" type="card">
          <el-tab-pane class="my-height" label="基本信息" name="base">
            <el-form-item label="版本名称" prop="name">
              <el-input
                  v-model="dataForm.name"
                  :disabled="!isAdd"
                  placeholder="请填写版本名称"
              ></el-input>
            </el-form-item>
            <el-form-item v-if="isAdd" label="版本等级" prop="level">
              <el-input
                  v-model="dataForm.level"
                  placeholder="请填写版本等级"
              ></el-input>
            </el-form-item>
            <el-form-item label="版本描述" prop="description">
              <el-input
                  v-model="dataForm.description"
                  placeholder="请填写版本描述"
              ></el-input>
            </el-form-item>
            <el-form-item label="可以压缩" prop="isCompress">
              <el-select
                  v-model="dataForm.isCompress"
                  placeholder="请选择是否可以压缩"
                  style="width: 100%"
              >
                <el-option :value="1" label="是"></el-option>
                <el-option :value="0" label="否"></el-option>
              </el-select>
            </el-form-item>
            <!--            <el-form-item label="版本类型" prop="packageType" v-if="isAdd">-->
            <!--              <el-select v-model="dataForm.packageType" placeholder="请选择版本类型" style="width: 100%;">-->
            <!--                <el-option label="主要版本" :value="1"></el-option>-->
            <!--                <el-option label="次要版本" :value="2"></el-option>-->
            <!--              </el-select>-->
            <!--            </el-form-item>-->
            <el-form-item label="状态" prop="status">
              <el-select
                  v-model="dataForm.status"
                  placeholder="请选择状态"
                  style="width: 100%"
              >
                <el-option :value="0" label="下架"></el-option>
                <el-option :value="1" label="上架"></el-option>
              </el-select>
            </el-form-item>
            <el-form-item label="logo图片" prop="logoImgList">
              <upload-img
                  :fileList="dataForm.logoImgList"
                  :limit="1"
                  @imgChange="imgChangeLogo"
              ></upload-img>
            </el-form-item>
            <el-form-item label="官网logo图片" prop="websiteLogoImagesList">
              <upload-img
                  :fileList="dataForm.websiteLogoImagesList"
                  :limit="1"
                  @imgChange="imgChangeWebsiteLogo"
              ></upload-img>
            </el-form-item>
          </el-tab-pane>
          <el-tab-pane label="商品设置" name="commodity">
            <el-form-item class="priceContainer" label="选择商品">
              <div
                  v-if="
                  dataForm.typeConsumptionList &&
                  dataForm.typeConsumptionList.length > 0
                "
                  style="width: 100%"
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
                      :key="typeIndex"
                      class="priceItemContainer"
                  >
                    <div
                        class="priceItem right-border bottom-border left-border"
                    >
                      <el-select
                          v-model="
                          dataForm.typeConsumptionList[typeIndex]
                            .commodityTypeId
                        "
                          class="commodity-slect"
                          placeholder="请选择"
                      >
                        <el-option
                            v-for="item in commodityTypeList"
                            :key="item.id"
                            :disabled="typeConsumptionDisabled(item)"
                            :label="item.name"
                            :value="item.id"
                        >
                        </el-option>
                      </el-select>
                    </div>
                    <div class="priceItem right-border bottom-border">
                      <el-input
                          v-model="dataForm.typeConsumptionList[typeIndex].number"
                          :min="0"
                          :step="1"
                          controls-position="right"
                          placeholder="请输入"
                          style="width: 100%"
                          type="number"
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
                          v-if="
                          typeIndex === dataForm.typeConsumptionList.length - 1
                        "
                          style="color: blue; cursor: pointer"
                          @click="addPrice2"
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
                <el-button size="small" type="primary" @click="addPrice2"
                >添加商品
                </el-button
                >
              </div>
            </el-form-item>
            <el-form-item class="priceContainer" label="价格体系">
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
                  <div class="priceTitleItem right-border">官网显示</div>
                  <div class="priceTitleItem">操作</div>
                </div>
                <template v-if="dataForm.commodityPriceList?.length > 0">
                  <div
                      v-for="(
                      priceItem, priceIndex
                    ) in dataForm.commodityPriceList"
                      :key="priceIndex"
                      class="priceItemContainer"
                  >
                    <div
                        class="priceItem right-border bottom-border left-border"
                    >
                      <el-input
                          v-model="
                          dataForm.commodityPriceList[priceIndex].validityNum
                        "
                          placeholder="请输入"
                          type="number"
                      ></el-input>
                    </div>
                    <div class="priceItem right-border bottom-border">
                      <el-select
                          v-model="
                          dataForm.commodityPriceList[priceIndex].validityUnit
                        "
                          class="commodity-slect"
                          placeholder="请选择"
                      >
                        <template
                            v-for="item in orderDict.timeUnit"
                            :key="item.value"
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
                          :min="0"
                          :step="1"
                          controls-position="right"
                          placeholder="请输入"
                          style="width: 100%"
                          type="number"
                          @change="updateRealPrice(priceIndex)"
                      ></el-input>
                    </div>
                    <div class="priceItem right-border bottom-border">
                      <el-input-number
                          v-model="
                          dataForm.commodityPriceList[priceIndex].discount
                        "
                          :max="1"
                          :min="0.1"
                          :step="0.01"
                          controls-position="right"
                          placeholder="请输入"
                          style="width: 100%"
                          @change="updateRealPrice(priceIndex)"
                      ></el-input-number>
                    </div>
                    <div class="priceItem right-border bottom-border">
                      <el-input
                          v-model="
                          dataForm.commodityPriceList[priceIndex].realPrice
                        "
                          :min="0"
                          :step="1"
                          controls-position="right"
                          placeholder="请输入"
                          style="width: 100%"
                          type="number"
                      ></el-input>
                    </div>
                    <div class="priceItem right-border bottom-border">
                      <el-switch
                          v-model="
                          dataForm.commodityPriceList[priceIndex].showStatus
                        "
                          :active-value="1"
                          :inactive-value="0"
                          active-color="#13ce66"
                          inactive-color="#e1e1e1"
                      >
                      </el-switch>
                    </div>
                    <div class="priceItem right-border bottom-border">
                      <span
                          v-if="
                          priceIndex === dataForm.commodityPriceList.length - 1
                        "
                          style="color: blue; cursor: pointer"
                          @click="addPrice"
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
                <el-button size="small" type="primary" @click="addPrice"
                >添加价格
                </el-button
                >
              </div>
            </el-form-item>
          </el-tab-pane>
          <el-tab-pane label="增量包设置" name="second">
            <el-form-item class="priceContainer" label-width="0">
              <div style="text-align: right">
                <el-button size="small" type="primary" @click="addCommodity()"
                >添加增量包
                </el-button
                >
              </div>
              <el-table
                  :data="dataForm.incrementList"
                  :header-cell-style="{ padding: '0px' }"
                  :header-row-style="{ padding: '10px' }"
                  border
                  stripe
                  style="width: 100%"
              >
                <el-table-column
                    align="center"
                    type="index"
                    width="50"
                ></el-table-column>
                <el-table-column
                    align="center"
                    label="名称"
                    prop="commodityName"
                ></el-table-column>
                <el-table-column
                    align="center"
                    label="商品类型"
                    prop="commodityTypeName"
                ></el-table-column>
                <el-table-column align="center" label="价格" prop="price">
                  <template #default="scope">
                    <span v-if="scope.row.realPrice == scope.row.originalPrice">
                      {{ scope.row.realPrice }}元/{{
                        scope.row.validityNum
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
                      /{{
                        scope.row.validityNum
                      }}{{
                        getLabel(orderDict.timeUnit, scope.row.validityUnit)
                      }}
                    </span>
                  </template>
                </el-table-column>
                <!--                <el-table-column prop="status" label="状态" width="80" align="center">-->
                <!--                  <template slot-scope="scope">-->
                <!--                    <span>{{ ['下架', '上架'][scope.row.status] }}</span>-->
                <!--                  </template>-->
                <!--                </el-table-column>-->
                <el-table-column
                    align="center"
                    label="操作"
                    prop="remark"
                    width="150"
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
        <!--        <commodity-save-or-update :commodity-type-list="commodityTypeList" :data-form="selectCommodity" @submit="commoditySubmit" @close="commodityDialog = false" >-->
        <!--        </commodity-save-or-update>-->
        <select-commodity
            :data-form="selectCommodity"
            @close="commodityDialog = false"
            @submit="commoditySubmit"
        ></select-commodity>
      </div>
    </div>
  </myDialog>
</template>

<script setup>
import { ref, reactive, watch, onMounted, nextTick } from 'vue'
import { useDict } from '@/hooks/useDict.js'
import { useCommonHooks } from '@/hooks/useCommonHooks.js'
import api from '@/utils/request-api'
import MyDialog from '@/components/commonComponent/myDialog.vue'
import uploadImg from '@/components/commonComponent/uploadImg.vue'
import SelectCommodity from './selectCommodity.vue'

const addOrUpdateVisible = defineModel('addOrUpdateVisible', {
  type: Boolean,
  default: false
})

// Emits
const emit = defineEmits(['input', 'is-ok'])

// Reactive data
const timeFrame2 = ref([2, 5, 3, 4])
const dialog = ref(false)
const commodityDialog = ref(false)
const title = ref('')
const width = ref(950)
const footer = ref(true)
const activeName = ref('base')
const isAdd = ref(false)
const commodityTypeList = ref([])
const selectCommodityIndex = ref(-1)
const selectCommodity = ref({})

const dataForm = reactive({
  id: '',
  name: '',
  isCompress: 1,
  packageType: 1,
  status: 1,
  logoImgList: [],
  websiteLogoImagesList: [],
  detailImgList: [],
  commodityPriceList: [],
  typeConsumptionList: [],
  incrementList: []
})

const dataRule = reactive({
  name: [{required: true, message: '请输入名称', trigger: 'blur'}],
  level: [{required: true, message: '版本等级', trigger: 'blur'}],
  isCompress: [
    {required: true, message: '请选择是否可以压缩', trigger: 'blur'}
  ],
  description: [{required: true, message: '请填写版本描述', trigger: 'blur'}],
  packageType: [{required: true, message: '请选择版本类型', trigger: 'blur'}],
  status: [{required: true, message: '请选择状态', trigger: 'blur'}],
  logoImgList: [{required: true, message: '请上传logo图片', trigger: 'blur'}],
  websiteLogoImagesList: [
    {required: true, message: '请上传官网logo图片', trigger: 'blur'}
  ]
})

// Hooks
const {orderDict, getLabel} = useDict()
const {setConvertUnitValue, getConvertUnitValue} = useCommonHooks()

// Refs
const dataFormRef = ref(null)

// Watchers
watch(dialog, (newVal) => {
  emit('input', newVal)
})

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
    {deep: true}
)

watch(
    () => dataForm.commodityPriceList,
    (newVal) => {
      if (newVal && newVal.length > 0 && commodityTypeList.value && commodityTypeList.value.length > 0) {
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
    {deep: true}
)

const updateRealPrice = (index) => {
  let item = dataForm.commodityPriceList[index]
  if (item.originalPrice && item.discount) {
    let temp = (item.originalPrice * item.discount)
        .toFixed(2)
        .replace(/\.?0+$/, '')
    console.log('temp', temp)

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
  dialog.value = true
  if (activeNameParam) {
    activeName.value = activeNameParam
  }
  if (id) {
    isAdd.value = false
    title.value = '版本修改'
    const res = await api.package.info({id: id})
    if (res.code === 0) {
      if (res?.data) {
        // 商品
        res.data?.typeConsumptionList?.forEach((item) => {
          item.number = setConvertUnitValue(item.number, item.commodityTypeCode)
        })
        // 价格
        res?.data?.commodityPriceList?.forEach((item) => {
          item.originalPrice = item.originalPrice / 100
          item.realPrice = item.realPrice / 100
        })
        // 增量包
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
}

const addCommodity = (item, index) => {
  console.log('eee', item)

  if (item) {
    selectCommodityIndex.value = index
  } else {
    selectCommodityIndex.value = -1
  }
  selectCommodity.value = item
  commodityDialog.value = true
}

const commoditySubmit = (form) => {
  if (!dataForm?.incrementList) {
    dataForm.incrementList = []
  }
  if (selectCommodityIndex.value === -1) {
    dataForm.incrementList.push(form)
  } else {
    dataForm.incrementList[selectCommodityIndex.value] = form
  }
  commodityDialog.value = false
}

const getCommodityTypeList = async () => {
  const res = await api.commoditytype.list({limit: -1}, {showLoading: true})
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
    showStatus: 0
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
    showStatus: 0
  })
}

const detailImgChange = (fileList) => {
  dataForm.detailImgList = fileList
}

const submit = () => {
  dataFormRef.value.validate(async (valid) => {
    if (valid) {
      let data = JSON.parse(JSON.stringify(dataForm))
      console.log('data', data)

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

      // 商品
      data?.typeConsumptionList?.forEach((item) => {
        item.number = getConvertUnitValue(item.number, item.commodityTypeCode)
      })

      // 价格
      data?.commodityPriceList?.forEach((item) => {
        if (!item.validityNum) {
          ElMessage.error('请您完善商品设置中有效期填写')
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

      // 增量包
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
        emit('input', false)
        emit('is-ok')
        addOrUpdateVisible.value = false
        dialog.value = false
        ElMessage.success(res.msg)
      }
    } else {
      ElMessage.error('请完善基础信息')
    }
  })
}
const imgChangeLogo = (fileList) => {
  dataFormRef.value.validateField('logoImgList')
  dataForm.logoImgList = fileList
}
const imgChangeWebsiteLogo = (fileList) => {
  dataFormRef.value.validateField('websiteLogoImagesList')
  dataForm.websiteLogoImagesList = fileList
}
const handleClose = () => {
  dialog.value = false
  addOrUpdateVisible.value = false
}

// Lifecycle
onMounted(() => {
  getCommodityTypeList()
})

// Expose
defineExpose({
  init
})
</script>

<style lang="less" scoped>
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

.commodity-slect :deep(.el-select__wrapper) {
  border: none !important;
  box-shadow: none !important;
}

.priceItem :deep(.el-input__wrapper) {
  border: none !important;
  box-shadow: none !important;
}

.ellipsis {
  white-space: nowrap; /* 不换行 */
  overflow: hidden; /* 隐藏超出部分 */
  text-overflow: ellipsis; /* 显示省略号 */
}
</style>

<style>
.el-select-dropdown__item.is-selected {
  color: #409eff;
}
</style>
