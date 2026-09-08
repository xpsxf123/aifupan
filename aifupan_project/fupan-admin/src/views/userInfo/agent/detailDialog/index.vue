<template>
  <div v-if="detailDialogFlag">
    <el-dialog
      v-model="dialogVisible"
      title="代理商详情"
      width="1000px"
      @close="handleClose"
    >
      <div class="InfoBox">
        <div class="infoTitle">基础信息</div>
        <div class="infoRow">
          <div class="infoCloum">
            <div class="labelBox">代理商名称：</div>
            <div class="valueBox">{{ detailForm.agentName }}</div>
          </div>
          <div class="infoCloum">
            <div class="labelBox">代理商ID：</div>
            <div class="valueBox">{{ detailForm.id }}</div>
          </div>
        </div>
        <div class="infoRow">
          <div class="infoCloum">
            <div class="labelBox">代理商类型：</div>
            <div class="valueBox">
              {{ detailForm.agentType === 0 ? '普通代理商' : '渠道代理商' }}
            </div>
          </div>
          <div class="infoCloum">
            <div class="labelBox">代理商行业：</div>
            <div class="valueBox">{{ detailForm.tradeName }}</div>
          </div>
        </div>
        <div class="infoRow">
          <div class="infoCloum">
            <div class="labelBox">联系人姓名：</div>
            <div class="valueBox">{{ detailForm.contactName }}</div>
          </div>
          <div class="infoCloum">
            <div class="labelBox">联系人手机号：</div>
            <div class="valueBox">{{ detailForm.contactPhone }}</div>
          </div>
        </div>
        <div class="infoRow">
          <div class="infoCloum">
            <div class="labelBox">平台运营：</div>
            <div class="valueBox">{{ detailForm.operationUserName }}</div>
          </div>
          <div class="infoCloum">
            <div class="labelBox">代理商地址：</div>
            <div class="valueBox">{{ detailForm.contactAddress }}</div>
          </div>
        </div>
        <div class="infoRow">
          <div class="infoCloum">
            <div class="labelBox">海报图片：</div>
            <div class="valueBox img-box">
              <el-image
                ref="imageRef"
                :max-scale="7"
                :min-scale="0.2"
                :preview-src-list="[detailForm.posterImageUrl]"
                :src="detailForm.posterImageUrl"
                :zoom-rate="1.2"
                fit="cover"
                show-progress
                style="width: 100px; height: 100px"
              />
              <div class="modal" @click="openImgDialog">
                <SvgIcon class="magnifyingLens" name="magnifyingLens"></SvgIcon>
              </div>
            </div>
          </div>
          <div class="infoCloum">
            <div class="labelBox">代理商URL链接：</div>
            <div class="valueBox">{{ detailForm.url }}</div>
          </div>
        </div>
        <div class="infoRow">
          <div class="infoCloum">
            <div class="labelBox">状态：</div>
            <div class="valueBox">
              {{ detailForm.agentStatus === 0 ? '未启用' : '启用' }}
            </div>
          </div>
          <div class="infoCloum">
            <div class="labelBox">创建人：</div>
            <div class="valueBox">{{ detailForm.createUserName }}</div>
          </div>
        </div>
        <div class="infoRow">
          <div class="infoCloum">
            <div class="labelBox">按钮文案：</div>
            <div class="valueBox">{{ detailForm.btContent }}</div>
          </div>
        </div>
        <div class="infoRow">
          <div class="infoCloum">
            <div class="labelBox">创建时间：</div>
            <div class="valueBox">{{ detailForm.createDate }}</div>
          </div>
          <div class="infoCloum">
            <div class="labelBox">最后修改时间：</div>
            <div class="valueBox">{{ detailForm.updateDate }}</div>
          </div>
        </div>
      </div>
      <div class="InfoBox">
        <div class="infoTitle">结算信息</div>
        <div class="last-infoRow">
          <div class="grid-left">
            新签佣金比例：{{ formatRate(detailForm.commissionRate) }}
          </div>
          <div class="grid-right">
            续费佣金比例：{{ formatRate(detailForm.renewalCommissionRate) }}
          </div>
        </div>
      </div>
      <div class="nav-container">
        <div class="nav-header">
          <el-tabs v-model="activeName">
            <el-tab-pane label="平台销售" name="first">
              <platformSales v-if="activeName === 'first'" :agentId="agentId" />
            </el-tab-pane>
            <el-tab-pane label="渠道明细" name="second">
              <promotionChannel
                v-if="activeName === 'second'"
                :agentId="agentId"
                :parentId="detailForm.channelId"
              />
            </el-tab-pane>
            <el-tab-pane label="代理商销售" name="third">
              <agentSales v-if="activeName === 'third'" :agentId="agentId" />
            </el-tab-pane>
            <el-tab-pane label="佣金结算记录" name="fourth">
              <commissionHistory
                v-if="activeName === 'fourth'"
                :agentId="agentId"
              />
            </el-tab-pane>
          </el-tabs>
        </div>
      </div>
      <template #footer>
        <span class="dialog-footer"> </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import emitter from '@/utils/emitter'
import Big from 'big.js'
import api from '@/utils/request-api'
import platformSales from './platformSales.vue'
import promotionChannel from './promotionChannel.vue'
import agentSales from './agentSales.vue'
import commissionHistory from './commissionHistory.vue'

const dialogVisible = ref(false)
const detailDialogFlag = ref(false)
const activeName = ref('first')
const agentId = ref('')
const imageRef = ref(null)
const detailForm = reactive({
  agentName: '',
  id: '',
  agentType: '',
  tradeName: '',
  contactName: '',
  contactPhone: '',
  operationUserName: '',
  contactAddress: '',
  agentStatus: '',
  btContent: '',
  createUserName: '',
  posterImageUrl: '',
  url: '',
  createDate: '',
  updateDate: '',
  channelId: '',
  commissionRate: '',
  renewalCommissionRate: '',
})

const openDialog = async (row) => {
  detailDialogFlag.value = true
  dialogVisible.value = true
  agentId.value = row.id

  const res = await api.user.getAgentDetail({
    id: row.id,
  })

  if (res.code === 0) {
    Object.keys(detailForm).forEach((item) => {
      if (res.data.hasOwnProperty(item)) {
        detailForm[item] = res.data[item]
      }
    })
    detailForm.posterImageUrl = res.data.posterImgList[0].url
  }
}

const handleClose = () => {
  Object.keys(detailForm).forEach((item) => {
    detailForm[item] = ''
  })
  detailDialogFlag.value = false
}

const openImgDialog = () => {
  imageRef.value.showPreview()
}

const formatRate = (val) => {
  if (val == null || val === '') return '-'
  return `${new Big(val).times(100).toFixed(2)}%`
}

onMounted(() => {
  emitter.on('showDetail', openDialog)
})
</script>

<style lang="scss" scoped>
:deep(.custom-select .el-input__inner) {
  line-height: 1.5;
  height: 100%;
  padding: 0 5px;
  border: none;
  font-size: 14px;
  box-shadow: none;
}

:deep(.custom-select .el-input__icon) {
  line-height: 0 !important;
}

.InfoBox {
  margin: 0 0 0 12px;
  padding: 0;
  text-align: left;
  font-size: 14px;
  width: 100%;
}

.infoTitle {
  width: 100%;
  font-size: 16px;
  font-weight: bold;
  color: #606266;
  text-align: left;
  margin-bottom: 10px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.infoRow {
  display: flex;
  flex-direction: row;
  justify-content: flex-start;
  margin: 0px 0px 10px 0px;
  border-bottom: rgb(195, 195, 212) 1px dashed;
  padding-bottom: 5px;
}

.infoCloum {
  width: 50%;
  display: flex;

  .labelBox {
    white-space: nowrap;
  }
}

.valueBox img {
  width: 70px;
  height: 70px;
  object-fit: fill;
  border-radius: 5px;
}

.img-box {
  width: 70px;
  height: 70px;
  position: relative;
  background-color: skyblue;

  .el-image {
    width: 100% !important;
    height: 100% !important;
  }
}

.modal {
  display: none;
  width: 70px;
  height: 70px;
  position: absolute;
  top: 0;
  left: 0;
  background-color: rgba(0, 0, 0, 0.4);
  color: #fff;
  font-size: 24px;
  text-align: center;
  line-height: 70px;
  cursor: pointer;
  border-radius: 4px;
  z-index: 9;
}

.img-box:hover .modal {
  display: flex;
  justify-content: center;
  align-items: center;
}

.valueBox span {
  color: rgb(24, 144, 255);
  cursor: pointer;
}

.daitas {
  left: 00px;
  margin: 10px 15px 20px 0;
}

.daitas span {
  margin-left: 10px;
}

.ele {
  position: absolute;
  width: 200px;
  height: 150px;
  left: 400px;
  margin: 9px;
}

.ele li {
  list-style: none;
  margin: 5px 5px 5px 5px;
}

.el {
  margin-left: 120px;
  height: 160px;
}

.daitas li {
  list-style: none;
  margin: 5px 0;
}

.ele li {
  list-style: none;
  margin: 5px 0;
}

.ele span.p4 {
  margin-left: 10px;
}

.ele el-switch {
  margin-left: 20px;
}

.nav-container {
  padding: 10px;
  border: 1px solid #ddd;
  width: 98%;
  margin: 0 auto;
  border-radius: 3px;
}

.last-infoRow {
  display: grid;
  grid-template-columns: 50% 50%;
  margin-bottom: 20px;
}
</style>
