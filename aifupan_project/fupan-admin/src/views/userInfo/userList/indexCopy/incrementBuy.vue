<template>
  <my-dialog
      v-model="dialogVisible"
      :class="{ 'mobile-dialog-innner-custom': isMobile }"
      :customClass="customClass"
      :footer="incrementListAll.length > 0"
      :fullscreen="isMobile"
      :maxHeight="maxHeight"
      :title="title"
      :width="width"
      class="my-dialog"
      @close="close"
      @submit="submit"
  >
    <div v-if="incrementListAll?.length > 0" class="incrementalPackage">
      <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-width="120px"
          size="small"
      >
        <el-form-item
            :required="true"
            label="增量包类型"
            label-width="100px"
            style="display: flex; align-items: center"
        >
          <el-select
              v-model="commodityType"
              placeholder="请选择"
              size="default"
              style="width: 200px"
          >
            <el-option
                v-for="item in typeOptions"
                :key="`type-${item.value}`"
                :label="item.label"
                :value="item.value"
            >
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="选择增量包" label-width="100px" prop="commodityId">
          <div class="general">
            <div
                v-for="item in incrementList"
                :key="`increment-${item.id}`"
                class="item"
                @click="selectIncrement(item)"
            >
              <div
                  :class="{ 'my-select': form.commodityId === item.id }"
                  class="zhong"
              >
                <div class="price-display">
                  <div class="price-section">
                    <span class="currency-symbol">￥</span>
                    <span
                        v-if="item.discount == 1"
                        class="price-amount price-amount--original"
                    >
                      {{ item.commodityPrice?.originalPrice }}
                    </span>
                    <span v-else class="price-amount price-amount--discount">
                      {{ item.commodityPrice?.originalPrice * item.discount }}
                    </span>
                    <span class="validity-section">
                      /{{
                        item.commodityPrice?.validityNum
                      }}{{
                        getLabel(
                            orderDict.timeUnit,
                            item.commodityPrice?.validityUnit
                        )
                      }}
                    </span>
                  </div>
                </div>
                <div class="title">
                  {{ item.commodity?.name ?? '' }}
                </div>
              </div>
            </div>
          </div>
        </el-form-item>
      </el-form>
    </div>
    <div
        v-else
        style="
        height: 200px;
        width: 100%;
        display: flex;
        align-items: center;
        justify-content: center;
      "
    >
      暂无增量包
    </div>
  </my-dialog>
</template>

<script setup>
import { ref, reactive, computed, nextTick } from 'vue'
import { ElMessageBox } from 'element-plus'
import MyDialog from '@/components/commonComponent/myDialog.vue'
import { useDict } from '@/hooks/useDict'
import api from '@/utils/request-api'

const emit = defineEmits(['close', 'is-ok'])
const props = defineProps({
  isMobile: {
    type: Boolean,
    default: false
  }
})
const {orderDict, getLabel} = useDict()
const width = ref('50%')
const dialogVisible = ref(false)
const title = ref('购买增量包')
const commodityType = ref('all')
const maxHeight = ref(false)
const typeOptions = ref([])
const customClass = ref('increment-buy-custom')
const form = reactive({
  commodityId: '',
  commodityName: ''
})
const rules = reactive({
  commodityId: [
    {required: true, message: '请选择增量包', trigger: ['blur', 'change']}
  ]
})
const userId = ref({})
const incrementListAll = ref([])
const formRef = ref(null)
const incrementList = computed(() => {
  if (commodityType.value === 'all') {
    return incrementListAll.value
  } else {
    return incrementListAll.value.filter(
        (item) => item.commodity?.commodityTypeName === commodityType.value
    )
  }
})
const selectIncrement = (item) => {
  form.commodityId = item.id
  form.commodityName = item.name
}
const init = async (userIdParam) => {
  try {
    userId.value = userIdParam
    dialogVisible.value = true
    const res = await api.package.incrementByPackageId({userId: userId.value})
    if (res?.code == 0) {
      if (res.data?.incrementList?.length > 0) {
        let data = res.data?.incrementList?.filter((item) => {
          return item.status == 1 && item.commodity?.status == 1
        })
        data.forEach((item) => {
          if (item.commodityPrice) {
            item.commodityPrice.realPrice = item.commodityPrice.realPrice / 100
            item.commodityPrice.originalPrice =
                item.commodityPrice.originalPrice / 100
          }
        })
        incrementListAll.value = data
        filterOptions(data)
      }
    } else {
      close()
    }
  } catch (error) {
    console.error('初始化增量包失败:', error)
    close()
  }
}
const filterOptions = (item) => {
  typeOptions.value = item.map((item) => {
    return {
      label: item.commodity.commodityTypeName,
      value: item.commodity.commodityTypeName
    }
  })
  // 去重
  typeOptions.value = Array.from(
      new Map(typeOptions.value.map((item) => [item.label, item])).values()
  )
  typeOptions.value.unshift({
    label: '全部',
    value: 'all'
  })
}
const submit = async () => {
  const valid = await formRef.value.validate()
  if (valid) {
    const increment = incrementListAll.value.find(
        (item) => item.id === form.commodityId
    )
    console.log(increment)
    await ElMessageBox.confirm(
        `确定选择【${increment?.commodity?.name}】吗？`,
        '提示',
        {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }
    )
    const data = {
      commodityId: form.commodityId,
      userId: userId.value,
      commodityPriceId: form.commodityPriceId,
      commodityType: 0,
      discountRate: 0
    }
    const res = await api.order.pcIncrementsOrder(data)
    if (res?.code == 0) {
      ElMessage.success(res.msg)
      emit('is-ok')
      emit('close')
    }
  }
}
const close = () => {
  nextTick(() => {
    dialogVisible.value = false
    emit('close')
  })
}

defineExpose({
  init
})
</script>

<style lang="less" scoped>
.incrementalPackage {
  padding-right: 20px;
  user-select: none;
}

.general {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px; /* 卡片之间的间距 */
  justify-content: flex-start;

  .item {
    cursor: pointer;
    flex: 0 0 calc(25% - 16px); /* 4列布局，可以根据需要调整 */
    box-sizing: border-box;
  }

  .zhong {
    background: #fff;
    border: 2px solid #e0e0e0;
    border-radius: 12px;
    padding: 16px 5px;
    text-align: center;
    box-shadow: 0 2px 6px rgba(0, 0, 0, 0.05);
    transition: all 0.2s ease;

    &:hover {
      border-color: #409eff;
      box-shadow: 0 4px 10px rgba(0, 0, 0, 0.08);
      transform: translateY(-2px);
    }

    &.my-select {
      border-color: #409eff;
      background: #ecf5ff;
      box-shadow: 0 4px 12px rgba(64, 158, 255, 0.2);
    }

    .price-display {
      display: flex;
      justify-content: center;
      align-items: center;
      margin-bottom: 8px;

      .price-section {
        display: flex;
        align-items: baseline;
        justify-content: center;
        gap: 2px;

        .currency-symbol {
          font-size: 18px;
          font-weight: 500;
          color: #333;
        }

        .price-amount {
          font-weight: 600;
          color: #e74c3c;
          line-height: 1;

          &--original {
            font-size: 28px;
          }

          &--discount {
            font-size: 24px;
          }
        }

        .validity-section {
          font-size: 14px;
          color: #666;
          font-weight: 400;
          margin-left: 4px;
        }
      }
    }

    .title {
      font-size: 16px;
      font-weight: 500;
      color: #333;
      overflow: hidden;
    }
  }
}

:deep(.el-form-item .el-form-item__label) {
  padding: 0 12px 0 0 !important;
  font-size: 14px;
}
</style>
<style>
.increment-buy-custom {
  min-width: 700px;
}
</style>
