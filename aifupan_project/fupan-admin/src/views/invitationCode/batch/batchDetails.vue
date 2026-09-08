<template>
  <my-dialog
      v-model="visible"
      :footer="false"
      :title="'详情'"
      max-height
      width="800px"
      @close="close"
  >
    <div v-if="visible">
      <el-tabs v-model="activeName" type="card">
        <el-tab-pane class="p-lr-20" label="基本信息" lazy name="base">
          <el-descriptions title="">
            <el-descriptions-item label="邀请名称">{{
                dataForm.name
              }}
            </el-descriptions-item>
            <el-descriptions-item label="创建总送"
            >{{ dataForm.quantity ?? 0 }}个
            </el-descriptions-item
            >
            <el-descriptions-item label="版本形式">{{
                ['用户版本', '活动版本'][dataForm.commodityType]
              }}
            </el-descriptions-item>
            <el-descriptions-item label="版本名称">{{
                dataForm.commodityName
              }}
            </el-descriptions-item>
            <el-descriptions-item label="有效时间"
            >{{
                dataForm.commodityValidityNum
              }}{{
                getLabel(orderDict.timeUnit, dataForm.commodityValidityUnit)
              }}
            </el-descriptions-item
            >
            <el-descriptions-item label="原单价"
            >{{ dataForm.commodityRealPrice / 100 }}元
            </el-descriptions-item
            >
            <el-descriptions-item label="优惠单价"
            >{{ dataForm.price / 100 }}元
            </el-descriptions-item
            >
            <el-descriptions-item label="总价格"
            >{{
                ((dataForm.price ?? 0) * (dataForm.quantity ?? 0)) / 100
              }}元
            </el-descriptions-item
            >
            <el-descriptions-item label="邀请码类型">{{
                ['机构码', '个人码', '激活码'][dataForm.type]
              }}
            </el-descriptions-item>
            <el-descriptions-item label="邀请码有效期">{{
                dataForm.validityStartDate + '至' + dataForm.validityEndDate
              }}
            </el-descriptions-item>
            <el-descriptions-item label="是否免费">{{
                ['否', '是'][dataForm.isGratis ?? 0]
              }}
            </el-descriptions-item>
            <el-descriptions-item label="是否免费">{{
                ['正常', '禁用'][dataForm.status ?? 0]
              }}
            </el-descriptions-item>
            <el-descriptions-item label="申请原因">{{
                dataForm.remarks
              }}
            </el-descriptions-item>
          </el-descriptions>
          <el-descriptions
              v-if="typeConsumptionList?.length > 0"
              style="margin-top: 15px"
              title="邀请码资源"
          >
            <el-descriptions-item
                v-for="(item, index) in typeConsumptionList"
                :key="`consumption-${index}`"
                :label="item.commodityTypeName"
            >
              {{
                convertUnit(
                    item.number,
                    item.commodityTypeCode,
                    item.commodityTypeUnit
                )
              }}
            </el-descriptions-item>
          </el-descriptions>
        </el-tab-pane>
        <el-tab-pane label="邀请码列表" lazy name="list">
          <codeList :codeBatchId="rowId"></codeList>
        </el-tab-pane>
      </el-tabs>
    </div>
  </my-dialog>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useCommonHooks } from '@/hooks/useCommonHooks.js'
import { useDict } from '@/hooks/useDict.js'
import api from '@/utils/request-api'
import myDialog from '@/components/commonComponent/myDialog.vue'
import codeList from '@/views/invitationCode/batch/components/CodeList.vue'

const {convertUnit} = useCommonHooks()
const {orderDict, getLabel} = useDict()

const activeName = ref('base')
const visible = ref(false)
const dataForm = ref({})
const rowId = ref('')
const typeConsumptionList = ref([])

const init = async (id) => {
  activeName.value = 'base'
  rowId.value = id
  visible.value = true
  dataForm.value = {}

  const res = await api.invitationcodebatch.info({id: id}, {showLoading: true})
  if (res?.code == 0) {
    if (res.data) {
      res.data.validityStartDate = res.data.validityStartDate
          ? res.data.validityStartDate.substring(0, 10)
          : ''
      res.data.validityEndDate = res.data.validityEndDate
          ? res.data.validityEndDate.substring(0, 10)
          : ''
    }
    dataForm.value = res.data
  }

  const typeRes = await api.invitationcodebatch.getTypeConsumptionById({
    id: id
  })
  if (typeRes?.code == 0) {
    typeConsumptionList.value = typeRes.data
  }
}

const close = () => {
  visible.value = false
}

defineExpose({
  init
})
</script>

<style lang="less" scoped>
.p-lr-20 {
  padding: 0 20px;
}
</style>
