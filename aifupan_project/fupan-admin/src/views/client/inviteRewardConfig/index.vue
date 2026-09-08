<template>
  <div class="invitation-event">
    <el-button
        v-if="!(tableData.length > 0)"
        size="small"
        type="primary"
        @click="handleAddOrEdit"
    >新增
    </el-button
    >
    <Table
        :columns="columns"
        :tableData="tableData"
        v-bind="pageInfo"
        @on-pageChange="handlePageChange"
    >
      <template #activityStatus="{ row }">
        <div v-if="row.activityStatus === 1" class="open status">
          <SvgIcon :icon-style="{ width: '15px', height: '15px' }" name="dot"/>
          <p>启用</p>
        </div>
        <div v-else class="close status">
          <SvgIcon
              :icon-style="{ width: '15px', height: '15px' }"
              name="dot-green"
          />
          <p>禁用</p>
        </div>
      </template>
      <template #operate="{ row }">
        <el-button size="small" type="primary" @click="handleAddOrEdit(row)"
        >修改
        </el-button
        >
      </template>
    </Table>
    <AddOrEditDialog
        v-if="isSurvive"
        @refresh="getDataList"
        @change-survive="isSurvive = $event"
    />
  </div>
</template>
<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import api from '@/utils/request-api'
import Table from '@/components/table/index.vue'
import AddOrEditDialog from './components/addOrEditDialog.vue'
import emitter from '@/utils/emitter'

// 响应式状态
const isSurvive = ref(false)
const openDialog = ref(false)
const columns = [
  {label: 'ID', prop: 'id', width: 50},
  {label: '代理商ID', prop: 'agentId', width: 160},
  {label: '活动名称', prop: 'activityName', minWidth: 160},
  {label: '活动时间', prop: 'activityStartTime', width: 160},
  {
    label: '启用状态',
    prop: 'activityStatus',
    minWidth: 100,
    slotName: 'activityStatus'
    // formatter: (row) => {
    //   return row.activityStatus === 1 ? '启用' : '未启用'
    // },
  },
  {label: '创建时间', prop: 'createDate', width: '180px'},
  {label: '修改时间', prop: 'updateDate', width: '180px'},
  {label: '操作', prop: 'operate', slotName: 'operate', fixed: 'right'}
]

const tableData = ref([])
const loadingFlag = ref(false)
const pageInfo = reactive({
  currentPage: 1,
  pageSize: 10,
  total: 0
})

// 获取表格数据
const getDataList = async () => {
  loadingFlag.value = true
  const res = await api.invitation.list({
    page: pageInfo.currentPage,
    limit: pageInfo.pageSize
  })
  if (res.code === 0) {
    pageInfo.total = res.data.totalCount
    loadingFlag.value = false
    tableData.value = res.data.list
  } else {
    loadingFlag.value = false
  }
}

// 分页发生变化后的回调
const handlePageChange = ({size, page}) => {
  pageInfo.currentPage = page
  pageInfo.pageSize = size
  getDataList()
}

// 新增和编辑按钮的回调
const handleAddOrEdit = (row = {}) => {
  isSurvive.value = true
  openDialog.value = true
  nextTick(() => {
    emitter.emit('openDialog', row)
  })
}

onMounted(() => {
  getDataList()
})
</script>
<style lang="less" scoped>
.invitation-event {
  padding: 15px;
}

.status {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
}
</style>
