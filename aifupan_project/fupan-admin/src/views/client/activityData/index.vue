<template>
  <div class="activity-data">
    <header>
      <el-form inline>
        <el-form-item>
          <el-select
              v-model="searchForm.progressCode"
              clearable
              placeholder="选择奖励要求查询"
              style="width: 160px"
              @clear="handleClear"
          >
            <el-option
                v-for="item in codeTypeOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
            >
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-input
              v-model="searchForm.inviteeName"
              clearable
              placeholder="输入被邀请人查询"
              style="width: 160px"
              @clear="handleClear"
          ></el-input>
        </el-form-item>
        <el-form-item>
          <el-input
              v-model="searchForm.inviterName"
              clearable
              placeholder="输入邀请人查询"
              style="width: 150px"
              @clear="handleClear"
          ></el-input>
        </el-form-item>
        <el-form-item>
          <el-date-picker
              v-model="startTimeAndEndTime"
              end-placeholder="结束日期"
              format="YYYY-MM-DD HH:mm:ss"
              range-separator="至"
              start-placeholder="开始日期"
              type="daterange"
              value-format="YYYY-MM-DD HH:mm:ss"
              @change="handelChangeTime"
          >
          </el-date-picker>
        </el-form-item>
        <el-form-item>
          <el-button icon="Search" plain type="primary" @click="handleSearch"
          >查询
          </el-button
          >
        </el-form-item>
      </el-form>
    </header>
    <Table
        :columns="columns"
        :loadingFlag="loadingFlag"
        :tableData="tableData"
        :total="total"
        v-bind="{ currentPage: pageInfo.page, pageSize: pageInfo.limit }"
        @on-pageChange="onPageChange"
    >
      <template #rewardList="{ row }">
        <div>
          <div v-for="(item, index) in row.rewardList" :key="'reward-' + index">
            {{ item }}
          </div>
        </div>
      </template>
    </Table>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import Table from '@/components/table/index.vue'
import api from '@/utils/request-api'

const searchForm = reactive({
  progressCode: '',
  inviterName: '', // 被邀请人名称
  inviteeName: '', // 邀请人名称
  startTime: '',
  endTime: ''
})

const tableData = ref([])
const codeTypeOptions = ref([])
const startTimeAndEndTime = ref([])
const loadingFlag = ref(false)
const pageInfo = reactive({
  page: 1,
  limit: 10
})
const total = ref(0)

const columns = [
  {label: '邀请人', prop: 'inviterName', width: 120},
  {label: '被邀请人', prop: 'inviteeName'},
  {label: '奖励要求', prop: 'progressCodeStr'},
  {
    label: '奖励明细',
    prop: 'rewardList',
    slotName: 'rewardList',
    minWidth: 160
  },
  {
    label: '奖励状态',
    prop: 'rewardStatus',
    formatter: (row, column, cellValue) =>
        cellValue === 0 ? '待发放' : '已发放',
    width: 100
  },
  {label: '奖励时间', prop: 'sendDate', width: 180}
]

const getDataList = async () => {
  loadingFlag.value = true
  const res = await api.invitation.listByBack(
      Object.assign({}, searchForm, pageInfo)
  )
  if (res.code === 0) {
    tableData.value = res.data.list ? res.data.list : []
    total.value = res.data.totalCount
    loadingFlag.value = false
  }
}

const handleSearch = () => {
  pageInfo.page = 1
  getDataList()
}

// 获取code类型
const getCodeType = async () => {
  const res = await api.invitation.codeList({
    limit: -1,
    typeLogo: 'invite_user_reward_code'
  })
  if (res.code === 0) {
    codeTypeOptions.value = res.data.list
  }
}

const handelChangeTime = (val) => {
  if (val && val.length > 0) {
    searchForm.startTime = val[0]
    searchForm.endTime = val[1]
  } else {
    searchForm.startTime = ''
    searchForm.endTime = ''
    getDataList()
  }
}

const onPageChange = ({size, page}) => {
  pageInfo.page = page
  pageInfo.limit = size
  getDataList()
}

const handleClear = () => {
  pageInfo.page = 1
  getDataList()
}

onMounted(() => {
  getCodeType()
  getDataList()
})
</script>

<style lang="less" scoped>
.activity-data {
  padding: 15px;
}
</style>
