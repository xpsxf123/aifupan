<template>
  <div>
    <div style="display: flex; justify-content: end; margin: 0px 20px 10px 0px">
      <el-button size="small" type="primary" @click="addOrUpdateHandle()">
        <SvgIcon :icon-style="{ width: '16px', height: '16px' }" name="pen" />
        添加跟进记录
      </el-button>
    </div>
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
        label="跟进内容"
        min-width="300"
        prop="remark"
        show-overflow-tooltip
      />
      <el-table-column
        align="center"
        header-align="center"
        label="跟进类型"
        prop="followTypeName"
        show-overflow-tooltip
      />
      <el-table-column
        :formatter="formatDateOnly"
        align="center"
        header-align="center"
        label="跟进时间"
        min-width="100px"
        prop="followUpTime"
        show-overflow-tooltip
      />
      <el-table-column
        :formatter="formatDateOnly"
        align="center"
        header-align="center"
        label="下次跟进时间"
        min-width="100px"
        prop="nextFolTime"
        show-overflow-tooltip
      />
      <el-table-column
        align="center"
        fixed="right"
        header-align="center"
        label="操作"
        width="110"
      >
        <template #default="scope">
          <el-button
            class="btn"
            size="small"
            type="text"
            @click="addOrUpdateHandle(scope.row.id)"
            >修改
          </el-button>
          <el-button
            class="btn"
            size="small"
            text
            type="danger"
            @click="deleteHandle(scope.row.id)"
            >删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="pageIndex"
      v-model:page-size="pageSize"
      :page-sizes="[5, 10]"
      :total="totalCount"
      background
      layout="total, sizes, prev, pager,next,->, jumper"
      style="text-align: center; margin-top: 10px"
      @size-change="sizeChangeHandle"
      @current-change="currentChangeHandle"
    >
    </el-pagination>

    <UserRemarkAddOrUpdate
      v-if="addOrUpdateVisible"
      ref="addOrUpdateV"
      @refreshDataList="getDataList"
    ></UserRemarkAddOrUpdate>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import api from '@/utils/request-api'
import UserRemarkAddOrUpdate from './userRemarkAddOrUpdate.vue'
import dayjs from 'dayjs'
import { customSvg } from '@/utils/icon.js'

const props = defineProps({
  userId: {
    type: String,
    default: '',
  },
})

const dataList = ref([])
const dataForm = reactive({})
const pageIndex = ref(1)
const pageSize = ref(5)
const totalCount = ref(0)
const addOrUpdateVisible = ref(false)
const addOrUpdateV = ref(null)

const getDataList = async () => {
  if (!props.userId) return
  dataForm.page = pageIndex.value
  dataForm.limit = pageSize.value
  dataForm.userId = props.userId

  const res = await api.userremark.list(dataForm)
  if (res?.code === 0 && res) {
    dataList.value = res.data.list
    totalCount.value = res.data.totalCount
  }
}

const addOrUpdateHandle = async (remarkId) => {
  addOrUpdateVisible.value = true
  await nextTick()
  addOrUpdateV.value.init(props.userId, remarkId)
}

const deleteHandle = async (id) => {
  await ElMessageBox.confirm('确定要进行删除吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  })
  const res = await api.userremark.delete({ id })
  if (res && res.code === 0) {
    getDataList()
    ElMessage({
      message: res.msg,
      type: 'success',
    })
  }
}

const sizeChangeHandle = (val) => {
  pageSize.value = val
  pageIndex.value = 1
  getDataList()
}

const currentChangeHandle = (val) => {
  pageIndex.value = val
  getDataList()
}

const formatDateOnly = (row, column, cellValue) => {
  if (!cellValue) {
    return ''
  }
  return dayjs(cellValue).format('YYYY-MM-DD')
}

onMounted(() => {
  getDataList()
})
</script>

<style lang="scss" scoped>
.btn {
  padding: 0;
}
</style>
