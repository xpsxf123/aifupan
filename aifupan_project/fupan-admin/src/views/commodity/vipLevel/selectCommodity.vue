<template>
  <my-dialog
    v-model="dialog"
    :title="title"
    :width="width"
    append-to-body
    @submit="submit"
    @close="close"
  >
    <div v-if="dialog">
      <el-form
        :model="form"
        :rules="rules"
        label-width="120px"
        style="overflow-x: hidden"
        ref="selectCommodityRef"
      >
        <el-form-item label="增量包类型">
          <el-select
            v-model="form.commodityTypeId"
            placeholder="请选择增量包类型"
            clearable
            filterable
            @change="updateCommodityId"
          >
            <el-option
              v-for="item in commodityTypeOptions"
              :key="item.commodityTypeId"
              :label="item.commodityTypeName"
              :value="item.commodityTypeId"
            ></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="商品" prop="commodityId">
          <el-select
            v-model="form.commodityId"
            placeholder="请选择商品"
            clearable
            filterable
            @change="updatePriceId"
          >
            <el-option
              v-for="item in commodityList"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            ></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="价格(元)" prop="commodityPriceId">
          <el-select
            v-model="form.commodityPriceId"
            placeholder="请选择商品"
            clearable
            filterable
            @change="updateRealPrice"
          >
            <el-option
              v-for="item in commodityPriceListTemp"
              :key="item.id"
              :label="
                item.originalPrice +
                '元 /' +
                item.validityNum +
                getLabel(orderDict.timeUnit, item.validityUnit)
              "
              :value="item.id"
            ></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="折扣" prop="discount">
          <el-input-number
            v-model="form.discount"
            controls-position="right"
            :min="0.1"
            :max="1"
            placeholder="请输入"
            :step="0.01"
            style="width: 100%"
            @change="updateRealPrice"
          ></el-input-number>
        </el-form-item>
        <el-form-item label="折扣价(元)" prop="realPrice">
          <el-input v-model="form.realPrice" placeholder="请输入"></el-input>
        </el-form-item>
        <!--        <el-form-item label="状态" prop="status">-->
        <!--          <el-select v-model="form.status" placeholder="请选择状态" clearable filterable>-->
        <!--            <el-option label="下架" :value="0"></el-option>-->
        <!--            <el-option label="上架" :value="1"></el-option>-->
        <!--          </el-select>-->
        <!--        </el-form-item>-->
      </el-form>
    </div>
  </my-dialog>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted, nextTick } from 'vue'
import { useDict } from '@/hooks/useDict.js'
import { useCommonHooks } from '@/hooks/useCommonHooks.js'
import api from '@/utils/request-api'
import MyDialog from '@/components/commonComponent/myDialog.vue'

// Props
const props = defineProps({
  dataForm: {
    type: Object,
    default: () => null,
  },
})

// Emits
const emit = defineEmits(['submit', 'close'])

// Reactive data
const dialog = ref(false)
const title = ref('选择商品')
const width = ref('400px')
const commodityPriceList = ref([])
const commodityTypeOptions = ref([])

const form = reactive({
  commodityTypeId: '',
  commodityId: '',
  commodityPriceId: '',
  discount: 1,
  status: 1,
  realPrice: '',
})

const rules = reactive({
  commodityId: [{ required: true, message: '请选择商品', trigger: 'change' }],
  commodityPriceId: [
    { required: true, message: '请选择商品', trigger: 'change' },
  ],
  discount: [{ required: true, message: '请输入折扣', trigger: 'blur' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }],
})

// Hooks
const { orderDict, getLabel } = useDict()
const {} = useCommonHooks()

// Refs
const selectCommodityRef = ref(null)

// Computed
const commodityList = computed(() => {
  return (
    commodityTypeOptions.value.find(
      (val) => val.commodityTypeId === form.commodityTypeId
    )?.commodityListVo ?? []
  )
})

const commodityPriceListTemp = computed(() => {
  let find = commodityList.value.find((val) => val.id === form.commodityId)
  return find?.commodityPriceList ?? []
})

// Watchers
watch(
  () => form.commodityPriceId,
  (newVal) => {
    if (form.commodityPriceId) {
      let temp = commodityPriceListTemp.value.find(
        (val) => val.id === form.commodityPriceId
      )
      form.originalPrice = temp?.originalPrice
    }
  },
  { immediate: true }
)

// Methods
const updateRealPrice = () => {
  console.log('form', form)
  if (form.commodityPriceId) {
    let temp = commodityPriceListTemp.value.find(
      (val) => val.id === form.commodityPriceId
    )
    form.originalPrice = temp?.originalPrice
  }
  if (form?.originalPrice && form?.discount) {
    let temp = (form.originalPrice * form.discount)
      .toFixed(2)
      .replace(/\.?0+$/, '')
    console.log('temp', temp)

    form.realPrice = temp
  }
}

const updateCommodityId = () => {
  form.commodityId = ''
  form.commodityPriceId = ''
  form.realPrice = ''
}

const updatePriceId = () => {
  form.commodityPriceId = ''
  form.realPrice = ''
}

const getCommodityList = async () => {
  const res = await api.commodity.listAll({ limit: -1 })
  if (res.code == 0) {
    if (res?.data) {
      res.data.forEach((outerItem) => {
        if (outerItem.commodityListVo && outerItem.commodityListVo.length > 0) {
          outerItem.commodityListVo.forEach((innerItem) => {
            if (
              innerItem.commodityPriceList &&
              innerItem.commodityPriceList.length > 0
            ) {
              innerItem.commodityPriceList.forEach((priceItem) => {
                if (priceItem.originalPrice)
                  priceItem.originalPrice = priceItem.originalPrice / 100
              })
            }
          })
        }
      })
    }
    commodityTypeOptions.value = res.data
  }
}

const submit = () => {
  console.log(form)
  selectCommodityRef.value.validate((valid) => {
    if (valid) {
      let commodity = commodityList.value.find(
        (val) => val.id === form.commodityId
      )
      form.commodityName = commodity?.name
      form.commodityTypeName = commodity?.commodityTypeName
      form.commodityTypeUnit = commodity?.commodityTypeUnit
      form.commodityTypeCode = commodity?.commodityTypeCode

      let temp = commodityPriceListTemp.value.find(
        (val) => val.id === form.commodityPriceId
      )
      form.originalPrice = temp?.originalPrice
      form.validityNum = temp?.validityNum
      form.validityUnit = temp?.validityUnit
      emit('submit', form)
    }
  })
}

const close = () => {
  emit('close')
}

// Lifecycle
onMounted(async () => {
  getCommodityList()
  await nextTick()
  if (props.dataForm) {
    const { commodity = {}, ...rest } = props.dataForm
    Object.assign(form, {
      ...JSON.parse(JSON.stringify(rest)),
      commodityTypeId: commodity.commodityTypeId || null,
    })
  }
  setTimeout(() => {
    dialog.value = true
  }, 100)
})
</script>

<style scoped lang="less">
.el-select {
  width: 100%;
}
</style>
