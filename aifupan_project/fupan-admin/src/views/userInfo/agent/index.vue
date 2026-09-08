<template>
  <div class="mod-config">
    <el-form inline>
      <el-form-item>
        <el-input v-model="searchFrom.agentName" clearable placeholder="输入代理商名称" style="width: 150px"
                  @clear="getDataList"></el-input>
      </el-form-item>
      <el-form-item>
        <el-input v-model="searchFrom.contactKeyword" clearable placeholder="输入联系人姓名/手机号码"
                  style="width: 190px"
                  @clear="handleClear"></el-input>
      </el-form-item>
      <el-form-item>
        <el-select v-model="searchFrom.operationUserId" clearable placeholder="选择平台运营" style="width: 150px"
                   @clear="getDataList">
          <el-option
              v-for="(item, index) in operationUsers"
              :key="`operation-${item.id}-${index}`"
              :label="item.nickName"
              :value="item.id">
          </el-option>
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-select v-model="searchFrom.channelId" clearable placeholder="选择一级渠道" style="width: 150px"
                   @clear="handleClear">
          <el-option
              v-for="item in oneChannelOptions"
              :key="`channel-${item.id}`"
              :label="item.channelName"
              :value="item.id">
          </el-option>
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button icon="Search" plain type="primary" @click="handleSearch">查询</el-button>
        <el-button type="primary" @click="handleAdd">新建代理商</el-button>
      </el-form-item>
    </el-form>
    <BaseTable :columns="columns" :currentPage="pageInfo.page" :loadingFlag="loadingFlag" :pageSize="pageInfo.limit"
               :tableData="tableData" :total="total"
               @on-pageChange="onPageChange">

      <template #agentStatus="{row}">
        <div v-if="row.agentStatus===1" class="open status">
          <SvgIcon :icon-style="{width:'15px',height:'15px'}" name="dot"/>
          <p>启用</p>
        </div>
        <div v-else class="close status">
          <SvgIcon :icon-style="{width:'15px',height:'15px'}" name="dot-green"/>
          <p>关闭</p>
        </div>
      </template>
      <template #operate="{row}">
        <el-button class="operate-btn" size="small" type="text" @click="showDetail(row)">查看详情</el-button>
        <el-button class="operate-btn" size="small" type="text" @click="handelEdit(row)">编辑</el-button>
        <el-button class="operate-btn" size="small" type="text" @click="copyLink(row)">复制链接</el-button>
        <el-popconfirm
            :title="row.agentStatus === 1 ? '确定停用吗？' : '确定启用吗？'"
            style="margin-left: 10px"
            @confirm="handleConfirm(row)"
        >
          <template #reference>
            <el-button v-if="row.agentStatus === 0" class="operate-btn" size="small" type="text">启用</el-button>
            <el-button v-if="row.agentStatus === 1" class="operate-btn" size="small" style="color: red" type="text">停用
            </el-button>
          </template>
        </el-popconfirm>
      </template>
    </BaseTable>
    <!--新增和编辑弹窗-->
    <addOrEditDialog :operationUsers="operationUsers" @get-dataList="getDataList"/>
    <!--详情弹窗-->
    <DetailDialog/>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import BaseTable from '@/components/table/index.vue'
import addOrEditDialog from './addOrEditDialog.vue'
import emitter from '@/utils/emitter'
import DetailDialog from './detailDialog/index.vue'
import Big from 'big.js'
import api from '@/utils/request-api'

const searchFrom = reactive({
  agentName: '', // 代理商名称
  contactName: '', // 联系人姓名
  contactPhone: '', // 联系人手机号码
  contactKeyword: '', // 联系人姓名/手机号码
  operationUserId: '', // 平台运营ID
  channelId: '' // 一级渠道
})

const oneChannelOptions = ref([]) // 一级渠道列表数据
const pageInfo = reactive({
  page: 1, // 当前页码，默认1
  limit: 10 // 每页显示条数，默认10
})
const total = ref(0)
const loadingFlag = ref(false)
const tableData = ref([]) // 表格数据
const operationUsers = ref([]) // 平台运营人员列表

const formatterCommissionRate = (row, column, cellValue) => {
  return `${new Big(cellValue).times(100).toFixed(2)}%`
}

const columns = ref([
  {label: '代理商名称', prop: 'agentName', minWidth: 210},
  {label: '代理商行业', prop: 'tradeName', minWidth: 190},
  {label: '平台运营', prop: 'operationUserName', minWidth: 120},
  {label: '联系人姓名', prop: 'contactName', minWidth: 100},
  {label: '联系人手机号码', prop: 'contactPhone', minWidth: 130},
  {label: '新签佣金比例', prop: 'commissionRate', minWidth: 120, formatter: formatterCommissionRate},
  {label: '续费佣金比例', prop: 'renewalCommissionRate', minWidth: 120, formatter: formatterCommissionRate},
  {
    label: '状态',
    slotName: 'agentStatus',
    formatter: (row) => row.agentStatus === 1 ? '启用' : '关闭',
    minWidth: 100
  },
  {label: '创建时间', prop: 'createDate', width: 180},
  {label: '操作', slotName: 'operate', fixed: 'right', width: 230}
])

const getDataList = async () => {
  loadingFlag.value = true
  const {code, data} = await api.user.agentSalesList(Object.assign(searchFrom, pageInfo))
  if (code === 0) {
    total.value = data.totalCount
    tableData.value = data.list ? data.list : []
  }
  loadingFlag.value = false
}

const handleSearch = () => {
  pageInfo.page = 1
  getDataList()
}

const getFirstChannelList = async () => {
  const {code, data} = await api.channel.listTree({childrenNotNull: 1})
  if (code === 0 && Array.isArray(data)) {
    data.forEach(item => {
      if (item.hasOwnProperty('children')) {
        delete item.children
      }
    })
  }
  oneChannelOptions.value = data
}

const getPlatformOperators = async () => {
  const {code, data} = await api.user.getPlatformOperators()
  if (code === 0 && Array.isArray(data)) {
    operationUsers.value = data
  }
}

const handleAdd = () => {
  emitter.emit('openDialog')
}

const handelEdit = (row) => {
  emitter.emit('openDialog', row)
}

const showDetail = (row) => {
  emitter.emit('showDetail', row)
}

const handleConfirm = async (row) => {
  await api.user.editAgent({
    id: row.id,
    agentStatus: row.agentStatus === 1 ? 0 : 1
  })
  await getDataList()
  ElMessage.success('操作成功')
}

const copyLink = async (row) => {
  if (!row.url) {
    ElMessage.error('没有可复制的链接')
    return
  }
  const text = `给你推荐一款最近很火的直播复盘工具，可以录同行，抓话术，拆竞品、还可以查违规，我用了非常棒，他们今天在搞免费试用的活动，你赶紧点这个链接去注册一下：${row.url}`
  if (navigator.clipboard && window.isSecureContext) {
    await navigator.clipboard.writeText(text)
  } else {
    // 使用旧方法兜底
    const textarea = document.createElement('textarea')
    textarea.value = text
    textarea.style.position = 'fixed'
    textarea.style.opacity = '0'
    document.body.appendChild(textarea)
    textarea.select()
    document.execCommand('copy')
    document.body.removeChild(textarea)
  }

  ElMessage.success('复制成功！')
}

const onPageChange = ({page, size}) => {
  pageInfo.page = page
  pageInfo.limit = size
  getDataList()
}

const handleClear = () => {
  pageInfo.page = 1
  searchFrom.contactPhone = ''
  searchFrom.contactName = ''
  getDataList()
}

onMounted(() => {
  Promise.all([getPlatformOperators(), getDataList(), getFirstChannelList()])
})
</script>

<style lang="less" scoped>
.mod-config {
  padding: 15px;
}

.status {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 5px
}

.operate-btn {
  margin-left: 0;
  padding: 5px;
}
</style>