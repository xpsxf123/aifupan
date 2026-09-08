<template>
  <div>
    <el-table
        :data="dataList"
        :element-loading-spinner="customSvg"
        border
        header-row-class-name="my-header-row"
        size="default"
        stripe
        style="width: 100%"
    >
      <el-table-column
          align="center"
          header-align="center"
          label="序"
          type="index"
          width="50"
      />
      <el-table-column
          :formatter="(row) => getLabel(userDict.loginLogType, row.operaType)"
          align="center"
          header-align="center"
          label="操作类型"
          min-width="100"
          prop="operaType"
          show-overflow-tooltip
      />
      <el-table-column
          align="center"
          header-align="center"
          label="ip"
          min-width="100"
          prop="ipAddress"
          show-overflow-tooltip
      />
      <el-table-column
          align="center"
          header-align="center"
          label="备注"
          min-width="100"
          prop="remarks"
          show-overflow-tooltip
      />
      <el-table-column
          align="center"
          header-align="center"
          label="创建时间"
          min-width="160"
          prop="createDate"
          show-overflow-tooltip
      />
    </el-table>
    <Pagination
        v-model:limit="form.limit"
        v-model:page="form.page"
        :total="total"
        @change="getList"
    ></Pagination>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import api from '@/utils/request-api'
import { useDict } from '@/hooks/useDict'
import Pagination from '@/components/commonComponent/pagination.vue'
import { customSvg } from '@/utils/icon.js'

const props = defineProps({
  userId: {
    type: String,
    default: ''
  }
})

const {userDict} = useDict()

const dataList = ref([])
const form = reactive({
  page: 1,
  limit: 10
})
const total = ref(0)

const getLabel = (dict, value) => {
  const item = dict.find((item) => item.value === value)
  return item ? item.label : value
}

const getList = async () => {
  if (!props.userId) return
  const res = await api.userloginlog.list({...form, userId: props.userId})
  if (res?.code === 0) {
    dataList.value = res.data.list
    total.value = res.data.totalCount
  }
}

onMounted(() => {
  getList()
})
</script>

<style lang="less" scoped></style>
