<template>
  <my-dialog
      v-model="dialog"
      :class="{ 'mobile-dialog-innner-custom': isMobile }"
      :footer="footer"
      :fullscreen="isMobile"
      :title="title"
      append-to-body
      class="order-dialog-custom"
      max-height
      @close="close"
  >
    <el-row>
      <el-form
          :model="dataForm"
          inline
          label-position="right"
          label-suffix=":"
          label-width="110px"
          style="width: 100%"
      >
        <div class="my-all">
          <div class="block">
            <el-col :span="24">
              <div class="title">订单信息</div>
            </el-col>
            <el-col :span="24">
              <div class="content">
                <el-row>
                  <el-col :span="12">
                    <el-form-item label="订单编号"
                    >{{ dataForm.id }}
                    </el-form-item>
                  </el-col>
                  <el-col :span="12">
                    <el-form-item label="实付金额"
                    >{{ dataForm.totalPrice / 100 }}元
                    </el-form-item>
                  </el-col>
                </el-row>
                <el-row>
                  <el-col :span="12">
                    <el-form-item label="新签/续费类别"
                    >{{ dataForm.commissionType }}
                    </el-form-item>
                  </el-col>
                  <el-col :span="12">
                    <el-form-item label="订单佣金比例"
                    >{{ dataForm.commission }}
                    </el-form-item>
                  </el-col>
                </el-row>
                <el-row>
                  <el-col :span="12">
                    <el-form-item label="订单佣金"
                    >{{ dataForm.commissionMoney }}
                    </el-form-item>
                  </el-col>
                  <el-col :span="12">
                    <el-form-item label="订单创建人"
                    >{{ dataForm.createName }}
                    </el-form-item>
                  </el-col>
                </el-row>
                <el-row>
                  <el-col :span="12">
                    <el-form-item label="订单修改人"
                    >{{ dataForm.updateName }}
                    </el-form-item>
                  </el-col>
                  <el-col :span="12">
                    <el-form-item label="商品"
                    >{{ dataForm.commodityName }}
                    </el-form-item>
                  </el-col>
                </el-row>
                <el-row>
                  <el-col :span="12">
                    <el-form-item label="下单人"
                    >{{ dataForm.userName }}
                    </el-form-item>
                  </el-col>
                  <el-col :span="12">
                    <el-form-item label="商品原价"
                    >{{ dataForm.originalPrice / 100 }}元
                    </el-form-item>
                  </el-col>
                </el-row>
                <el-row>
                  <el-col :span="12">
                    <el-form-item label="折扣"
                    >{{
                        dataForm.discount == 1
                            ? '无'
                            : dataForm.discount * 10 + '折'
                      }}
                    </el-form-item>
                  </el-col>
                  <el-col :span="12">
                    <el-form-item label="折扣价"
                    >{{ dataForm.realPrice / 100 }}元
                    </el-form-item>
                  </el-col>
                </el-row>
                <el-row>
                  <el-col :span="12">
                    <el-form-item label="补差价"
                    >{{ dataForm.discountRate / 100 }}
                    </el-form-item>
                  </el-col>
                  <el-col :span="12">
                    <el-form-item label="订单类型"
                    >{{ getLabel(orderDict.orderType, dataForm.orderType) }}
                    </el-form-item>
                  </el-col>
                </el-row>
                <el-row
                    v-if="dataForm.beforeUpgrading && dataForm.orderType == 1"
                >
                  <el-col :span="12">
                    <el-form-item label="升级前订单">
                      <el-button
                          type="text"
                          @click="init(dataForm.beforeUpgrading)"
                      >点击查看
                      </el-button>
                    </el-form-item>
                  </el-col>
                </el-row>
                <el-row>
                  <el-col :span="12">
                    <el-form-item label="订单来源"
                    >{{ getLabel(orderDict.orderSource, dataForm.source) }}
                    </el-form-item>
                  </el-col>
                  <el-col :span="12">
                    <el-form-item label="订单状态"
                    >{{ getLabel(orderDict.orderStatus, dataForm.status) }}
                    </el-form-item>
                  </el-col>
                </el-row>
                <el-row>
                  <el-col :span="12">
                    <el-form-item label="订单开始时间"
                    >{{ dataForm.startDate }}
                    </el-form-item>
                  </el-col>
                  <el-col :span="12">
                    <el-form-item label="订单结束时间"
                    >{{ dataForm.endDate }}
                    </el-form-item>
                  </el-col>
                </el-row>
                <el-row>
                  <el-col :span="12">
                    <el-form-item label="下单时间"
                    >{{ dataForm.createDate }}
                    </el-form-item>
                  </el-col>
                </el-row>
                <el-row>
                  <el-col :span="24">
                    <el-form-item
                        class="certificate-screenshot"
                        label="凭证截图"
                    >
                      <div v-if="fileList.length > 0" class="img-container">
                        <div
                            v-for="(item, index) in fileList"
                            :key="index"
                            class="img-box"
                        >
                          <el-image
                              :initial-index="index"
                              :preview-src-list="imageUrlList"
                              :src="item.url"
                              class="img-box__image"
                              fit="contain"
                              lazy
                          >
                            <template #error>
                              <div class="img-box__error">
                                <el-icon>
                                  <Picture/>
                                </el-icon>
                              </div>
                            </template>
                          </el-image>
                        </div>
                      </div>
                      <span v-else>无凭证截图</span>
                    </el-form-item>
                  </el-col>
                </el-row>
              </div>
            </el-col>
          </div>
          <div v-if="dataForm?.orderPay?.id" class="block">
            <el-col :span="24" class="m-t-20">
              <div class="title">支付信息</div>
            </el-col>
            <el-col :span="24">
              <div class="content">
                <el-row>
                  <el-col :span="24">
                    <el-form-item label="第三方订单号"
                    >{{ dataForm?.orderPay?.thirdOrderNum }}
                    </el-form-item>
                  </el-col>
                </el-row>
                <el-row>
                  <el-col :span="12">
                    <el-form-item label="支付状态"
                    >{{
                        getLabel(
                            orderDict.orderPayStatus,
                            dataForm?.orderPay?.payStatus
                        )
                      }}
                    </el-form-item>
                  </el-col>
                  <el-col :span="12">
                    <el-form-item label="支付类型"
                    >{{
                        getLabel(orderDict.payType, dataForm?.orderPay?.payType)
                      }}
                    </el-form-item>
                  </el-col>
                </el-row>
                <el-row>
                  <el-col :span="12">
                    <el-form-item label="支付金额">
                      {{
                        dataForm?.orderPay?.thirdOrderNum
                            ? dataForm?.orderPay?.thirdOrderNum / 100
                            : '0'
                      }}元
                    </el-form-item>
                  </el-col>
                  <el-col :span="12">
                    <el-form-item label="支付时间"
                    >{{ dataForm?.orderPay?.payDate }}
                    </el-form-item>
                  </el-col>
                </el-row>
              </div>
            </el-col>
          </div>
          <div class="block">
            <el-col :span="24" class="m-t-20">
              <div class="title">商品详情</div>
            </el-col>
            <el-col :span="24">
              <div class="content">
                <el-table
                    :data="dataForm?.orderDetailList ?? []"
                    border
                    header-row-class-name="my-header-row"
                    stripe
                    style="width: 100%; margin-top: 10px"
                >
                  <el-table-column
                      label="商品名称"
                      prop="commodityTypeName"
                  ></el-table-column>
                  <el-table-column label="总数量" prop="totalNumber">
                    <template #default="scope">
                      {{
                        setConvertUnitValue(
                            scope.row.totalNumber,
                            scope.row.commodityTypeCode
                        )
                      }}
                    </template>
                  </el-table-column>
                  <el-table-column
                      label="单位"
                      prop="commodityTypeUnit"
                  ></el-table-column>
                </el-table>
              </div>
            </el-col>
          </div>
        </div>
      </el-form>
    </el-row>
  </my-dialog>
</template>

<script setup>
import { ref, computed } from 'vue'
import { Picture } from '@element-plus/icons-vue'
import myDialog from '@/components/commonComponent/myDialog.vue'
import api from '@/utils/request-api'
import { useDict } from '@/hooks/useDict'
import { useCommonHooks } from '@/hooks/useCommonHooks'

const emit = defineEmits(['close'])
const props = defineProps({
  isMobile: {
    type: Boolean,
    default: false
  }
})
const {orderDict, getLabel} = useDict()
const {setConvertUnitValue} = useCommonHooks()

const dialog = ref(false)
const title = ref('订单详情')
const width = ref('750px')
const footer = ref(false)
const dataForm = ref({})
const fileList = ref([])

const imageUrlList = computed(() => {
  return fileList.value.map((item) => item.url)
})

const init = (orderId) => {
  api.order.info({id: orderId}).then((res) => {
    if (res && res.code === 0) {
      dataForm.value = res.data
      initFileList(res.data)
      dialog.value = true
    }
  })
}

const initFileList = (orderData) => {
  if (
      orderData?.orderExtend?.fileList &&
      orderData.orderExtend.fileList.length > 0
  ) {
    fileList.value = orderData.orderExtend.fileList.map((item) => ({
      name: item.name,
      url: item.url
    }))
  } else {
    fileList.value = []
  }
}

const close = () => {
  dialog.value = false
  emit('close')
}

defineExpose({
  init
})
</script>

<style lang="less" scoped>
.my-all {
  padding: 10px;

  .block {
    .title {
      font-size: 16px;
      font-weight: 600;
      color: #333333;
    }

    .content {
      padding: 0 10px 20px 10px;
    }
  }
}

:deep(.el-form-item) {
  margin-bottom: 0;
}

.m-t-20 {
  margin-top: 20px;
}

.img-container {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  width: 500px;

  .img-box {
    width: 70px;
    height: 70px;
    margin-top: 10px;
    border: 1px dashed #dcdfe6;
    border-radius: 6px;
    overflow: hidden;
    transition: all 0.3s ease;

    &:hover {
      border-color: #409eff;
    }

    &__image {
      width: 100%;
      height: 100%;
      cursor: pointer;
      border-radius: 6px;
    }

    &__error {
      display: flex;
      justify-content: center;
      align-items: center;
      width: 100%;
      height: 100%;
      background-color: #f5f7fa;
      color: #909399;

      i {
        font-size: 20px;
      }
    }
  }
}

// Element UI 图片预览样式覆盖
:global(.el-image-viewer__wrapper) {
  .el-image-viewer__mask {
    background-color: rgba(0, 0, 0, 0.8);
  }
}
</style>

<style>
.order-dialog-custom {
  min-width: 700px !important;
}
</style>
