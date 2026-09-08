<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import BaseTable from '@/components/table/index.vue'
import commissionHistory from '@/views/userInfo/agent/detailDialog/commissionHistory.vue'
import promotionChannel from '@/views/userInfo/agent/detailDialog/promotionChannel.vue'
import api from '@/utils/request-api'

const props = defineProps({
  agentId: {
    type: String,
    required: true
  }
})

const loadingFlag = ref(false)
const dialogVisible = ref(false)
const DetailDialogVisible = ref(false)
const formRef = ref(null)

const detailFrom = reactive({
  id: '',
  saleName: '',
  agentPromotionStr: '',
  agentPromotionList: ''
})

const total = ref(0)
const channelOptions = ref([])
const agentPromotionStr = ref([])

const formData = reactive({
  agentId: props.agentId,
  saleName: '',
  salePhone: '',
  promotionIds: [],
  saleStatus: 1
})

const tableData = ref([])

const columns = [
  {
    prop: 'id',
    label: '代理商销售ID'
  },
  {
    prop: 'saleName',
    label: '代理商销售名'
  },
  {
    prop: 'salePhone',
    label: '代理商销售手机号码'
  },
  {
    prop: 'agentPromotionStr',
    label: '推广渠道'
  },
  {
    prop: 'operate',
    slotName: 'operate',
    width: '180px',
    label: '操作'
  }
]

const rules = {
  saleName: [
    {required: true, message: '请输入代理商销售名', trigger: 'blur'}
  ],
  salePhone: [
    {required: true, message: '请输入代理商销售手机号码', trigger: 'blur'},
    {
      pattern: /^1[3-9]\d{9}$/,
      message: '请输入正确的手机号',
      trigger: 'blur'
    }
  ],
  promotionIds: [
    {required: true, message: '请选择推广渠道', trigger: 'change'}
  ]
}

const getDataList = async () => {
  loadingFlag.value = true
  const res = await api.user.saleList({agentId: props.agentId})
  if (res.code === 0) {
    tableData.value = res.data ? res.data : []
    loadingFlag.value = false
  }
  loadingFlag.value = false
}

const getPromotionChannels = async () => {
  const res = await api.user.promotionChannel({
    agentId: props.agentId
  })
  if (res.code === 0 && res.data && res.data.length) {
    channelOptions.value = res.data.map((item) => {
      return {
        id: item.id,
        promotionName: item.promotionName
      }
    })
  }
}

const handleAdd = () => {
  dialogVisible.value = true
}

const copyLink = async (row) => {
  const text = `给你推荐一款最近很火的直播复盘工具，可以录同行，抓话术，拆竞品、还可以查违规，我用了非常棒，他们今天在搞免费试用的活动，你赶紧点这个链接去注册一下：${row.url}`
  if (!text) {
    ElMessage.error('链接为空，无法复制')
    return
  }

  if (navigator.clipboard && window.isSecureContext) {
    await navigator.clipboard.writeText(text)
  } else {
    const textarea = document.createElement('textarea')
    textarea.value = text
    textarea.style.position = 'fixed'
    textarea.style.opacity = '0'
    document.body.appendChild(textarea)
    textarea.select()
    const result = document.execCommand('copy')
    document.body.removeChild(textarea)
    if (!result) {
      ElMessage.error('复制失败，请手动复制')
      return
    }
  }

  ElMessage.success('复制成功！')
}

const handleEdit = (row) => {
  dialogVisible.value = true
  nextTick(() => {
    Object.keys(formData).forEach((key) => {
      if (row.hasOwnProperty(key)) {
        formData[key] = row[key]
      }
      formData.promotionIds = row.agentPromotionList.map((item) => item.id)
      formData.id = row.id
    })
  })
}

const handleConfirm = async () => {
  await formRef.value.validate()
  if (formData.id) {
    await api.user.updateSale(formData)
    ElMessage.success('修改成功')
  } else {
    await api.user.addSale(formData)
    ElMessage.success('新增成功')
  }
  await getDataList()
  dialogVisible.value = false
}

const updateStatus = async (row) => {
  await api.user.updateSale({
    id: row.id,
    saleStatus: row.saleStatus === 1 ? 0 : 1
  })
  await getDataList()
  ElMessage.success('操作成功')
}

const showDetail = async (row) => {
  DetailDialogVisible.value = true
  const res = await api.user.showSaleDetail({id: row.id})
  detailFrom.id = res.data.agentId
  detailFrom.saleName = res.data.saleName
  detailFrom.agentPromotionStr = res.data.agentPromotionStr
  detailFrom.agentPromotionList = res.data.agentPromotionList
}

const handleClose = () => {
  formRef.value.resetFields()
  formData.id && (formData.id = '')
}

onMounted(() => {
  getDataList()
  getPromotionChannels()
})
</script>

<template>
  <div class="sale-container">
    <el-button class="add-sale" size="small" type="primary" @click="handleAdd"
    >新建代理商销售
    </el-button
    >
    <BaseTable
        :columns="columns"
        :loadingFlag="loadingFlag"
        :showPagination="false"
        :tableData="tableData"
        :total="total"
        height="200px"
    >
      <template #operate="{ row }">
        <el-button class="btn" size="small" type="text" @click="showDetail(row)"
        >查看详情
        </el-button
        >
        <el-button class="btn" size="small" type="text" @click="handleEdit(row)"
        >编辑
        </el-button
        >
        <el-popconfirm
            :title="row.saleStatus === 1 ? '确定停用吗？' : '确定启用吗？'"
            style="margin-left: 10px"
            @confirm="updateStatus(row)"
        >
          <template #reference>
            <el-button v-if="row.saleStatus === 0" class="btn" type="text">启用</el-button>
            <el-button
                v-if="row.saleStatus === 1"
                class="btn"
                style="color: red"
                type="text"
            >停用
            </el-button
            >
          </template>
        </el-popconfirm>
      </template>
    </BaseTable>

    <el-dialog
        v-model="dialogVisible"
        :title="formData.id ? '编辑' : '新增'"
        :width="550"
        append-to-body
        custom-class="my-dialog"
        @close="handleClose"
    >
      <el-form
          ref="formRef"
          :model="formData"
          :rules="rules"
          label-width="150px"
      >
        <el-form-item label="代理商销售名：" prop="saleName">
          <el-input
              v-model="formData.saleName"
              placeholder="输入代理商销售名"
          ></el-input>
        </el-form-item>
        <el-form-item label="代理商销售手机号:" prop="salePhone">
          <el-input
              v-model="formData.salePhone"
              placeholder="输入代理商销售手机号"
          ></el-input>
        </el-form-item>
        <el-form-item label="推广渠道:" prop="promotionIds">
          <el-select
              v-model="formData.promotionIds"
              multiple
              placeholder="选择推广渠道"
              style="width: 100%"
          >
            <el-option
                v-for="item in channelOptions"
                :key="item.id"
                :label="item.promotionName"
                :value="item.id"
            >
            </el-option>
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取 消</el-button>
          <el-button type="primary" @click="handleConfirm">确 定</el-button>
        </span>
      </template>
    </el-dialog>

    <el-dialog
        v-model="DetailDialogVisible"
        append-to-body
        custom-class="detail-dialog"
        title="代理商销售详情"
        width="50%"
    >
      <div class="InfoBox">
        <div class="infoRow">
          <div class="infoCloum">
            <div class="labelBox">销售名称：</div>
            <div class="valueBox">{{ detailFrom?.saleName }}</div>
          </div>
          <div class="infoCloum">
            <div class="labelBox">代理商销售ID：</div>
            <div class="valueBox">{{ detailFrom?.id }}</div>
          </div>
        </div>
        <div class="infoRow">
          <div class="infoCloum">
            <div class="labelBox">推广渠道：</div>
            <div class="valueBox">{{ detailFrom.agentPromotionStr }}</div>
          </div>
        </div>
        <div
            v-for="item in detailFrom.agentPromotionList"
            :key="item.id"
            class="container"
        >
          <p>{{ item.promotionName }}：{{ item.url }}</p>
          <el-button
              style="margin-left: 20px"
              type="text"
              @click="copyLink(item)"
          >复制链接
          </el-button>
        </div>
      </div>

      <template #footer>
        <span class="dialog-footer"> </span>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.add-sale {
  margin-bottom: 10px;
}

.avatar-uploader-icon {
  height: 80px;
  width: 80px;
  color: #8c939d;
  text-align: center;
  line-height: 80px;
}

.avatar {
  height: 80px;
  width: 80px;
  display: block;
}

.custom-input {
  box-shadow: none;
  border: none;
  outline: none;
  padding: 0 5px;
  width: 200px;
  font-size: 14px;
  line-height: 1.5;
  color: #606266;
}

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

:deep(.el-dialog__body) {
  padding: 10px 20px;
}

.daitas .bu {
  margin-left: 60px;
}

.editButton {
  background-color: green;
  color: white;
  border: none;
  padding: 5px 10px;
  cursor: pointer;
}

.container {
  display: flex;
  flex-direction: row;
  justify-content: flex-start;
}

.imgBox {
  margin: 0px;
  width: 100px;
}

.InfoBox {
  margin: 0px 0px 0px 12px;
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
  width: 45%;
  display: flex;
}

.labelBox {
  text-align: left;
  margin: 0px;
}

.valueBox {
  text-align: left;
  margin-left: 10px;
}

.valueBox img {
  width: 70px;
  height: 70px;
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

:deep(.my-dialog) {
  min-width: 400px !important;
}

:deep(.detail-dialog) {
  min-width: 400px !important;
}

.btn {
  padding: 0 !important;
}
</style>
