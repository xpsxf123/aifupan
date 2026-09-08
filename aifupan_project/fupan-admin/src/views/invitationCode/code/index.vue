<template>
  <div class="mod-config">
    <el-form :inline="true" :model="dataForm">
      <el-form-item>
        <el-input
            v-model="dataForm.code"
            clearable
            placeholder="输入邀请码搜索"
            style="width: 150px"
        ></el-input>
      </el-form-item>
      <el-form-item>
        <el-select
            v-model="dataForm.codeBatchId"
            clearable
            filterable
            placeholder="按批次筛选"
            style="width: 150px"
        >
          <el-option
              v-for="item in codeBatchList"
              :key="item.id"
              :label="item.name"
              :value="item.id"
          >
          </el-option>
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-select
            v-model="dataForm.useStatus"
            clearable
            placeholder="按使用状态筛选"
            style="width: 150px"
        >
          <el-option
              v-for="(item, index) in ['未使用', '已使用']"
              :key="`use-status-${index}`"
              :label="item"
              :value="index"
          >
          </el-option>
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-select
            v-model="dataForm.status"
            clearable
            placeholder="按状态筛选"
            style="width: 150px"
        >
          <el-option
              v-for="(item, index) in ['正常', '禁用']"
              :key="`status-${index}`"
              :label="item"
              :value="index"
          >
          </el-option>
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button icon="Search" plain type="primary" @click="search()"
        >查询
        </el-button
        >
        <el-button type="primary" @click="exportHandle()">导出</el-button>
        <!-- <el-button type="primary" @click="addOrUpdateHandle()">新增</el-button> -->
      </el-form-item>
    </el-form>
    <el-table
        v-loading="dataListLoading"
        :data="dataList"
        :element-loading-spinner="customSvg"
        border
        header-row-class-name="my-header-row"
        size="default"
        stripe
        style="width: 100%"
        @selection-change="selectionChangeHandle"
    >
      <!-- <el-table-column type="selection" hearder-align="center" align="center" label="选择" :selectable="selectable"> -->
      <el-table-column
          align="center"
          hearder-align="center"
          label="选择"
          type="selection"
          width="40"
      >
      </el-table-column>

      <el-table-column
          align="center"
          header-align="center"
          label="邀请码"
          min-width="100"
          prop="code"
      >
      </el-table-column>
      <el-table-column
          align="center"
          header-align="center"
          label="是否已下发"
          min-width="115"
          prop="isLssued"
      >
        <template #default="scope">
          <span v-if="scope.row.isLssued == 0">否</span>
          <span v-if="scope.row.isLssued == 1">是</span>
        </template>
      </el-table-column>
      <el-table-column
          align="center"
          header-align="center"
          label="邀请名称"
          min-width="160"
          prop="batchName"
          show-overflow-tooltip
      >
      </el-table-column>
      <el-table-column
          align="center"
          header-align="center"
          label="批次码"
          min-width="180"
          prop="batchId"
      >
      </el-table-column>
      <el-table-column
          align="center"
          header-align="center"
          label="有效期"
          min-width="230"
          prop="validityStartDate"
      >
        <template #default="scope">
          {{
            scope.row.validityStartDate.substring(0, 10) +
            '至' +
            scope.row.validityEndDate.substring(0, 10)
          }}
        </template>
      </el-table-column>
      <el-table-column
          align="center"
          header-align="center"
          label="状态"
          min-width="90"
          prop="status"
      >
        <template #default="scope">
          <span v-if="scope.row.status == 0">正常</span>
          <span v-if="scope.row.status == 1" style="color: rgb(245, 108, 108)">禁用</span>
        </template>
      </el-table-column>
      <el-table-column
          align="center"
          header-align="center"
          label="使用状态"
          min-width="100"
          prop="useStatus"
      >
        <template #default="scope">
          <span v-if="scope.row.useStatus == 0">未使用</span>
          <span v-if="scope.row.useStatus == 1" style="color: rgb(245, 108, 108)">已使用</span>
        </template>
      </el-table-column>
      <el-table-column
          :width="130"
          align="center"
          header-align="center"
          label="使用人"
          prop="userName"
      >
        <template #default="{ row }">
          <el-tooltip
              :key="showTipKey"
              content="点击查看用户详情"
              placement="top"
          >
            <span class="user-name-link" @click="jumpUserDetail(row.userId)">
              {{ row.userName }}
            </span>
          </el-tooltip>
        </template>
      </el-table-column>
      <el-table-column
          :width="180"
          align="center"
          header-align="center"
          label="使用时间"
          prop="useDate"
      >
      </el-table-column>
      <el-table-column
          align="center"
          fixed="right"
          header-align="center"
          label="操作"
          width="90"
      >
        <template #default="scope">
          <!-- <el-button type="primary" size="small" @click="addOrUpdateHandle(scope.row.id)">修改</el-button> -->
          <!-- <el-button type="danger" size="small" @click="deleteHandle(scope.row.id)">删除</el-button> -->
          <el-button
              v-if="scope.row.useStatus == 0"
              size="small"
              type="text"
              @click="updateCode(scope.row.id, scope.row.status)"
          >
            <span v-if="scope.row.status == 0" style="color: rgb(245, 108, 108)">禁用</span>
            <span v-else>解禁</span>
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination
        :current-page="pageIndex"
        :page-size="pageSize"
        :page-sizes="[10, 20, 50]"
        :total="totalCount"
        background
        layout="total, sizes, prev, pager,next,->, jumper"
        style="text-align: center; margin-top: 10px"
        @size-change="sizeChangeHandle"
        @current-change="currentChangeHandle"
    >
    </el-pagination>
    <!-- 弹窗, 新增 / 修改 -->
    <add-or-update
        v-if="addOrUpdateVisible"
        ref="addOrUpdateRef"
        @refreshDataList="getDataList"
    ></add-or-update>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { customSvg } from '@/utils/icon.js'
import { useDict } from '@/hooks/useDict.js'
import api from '@/utils/request-api'
import AddOrUpdate from './invitationcode-add-or-update.vue'
import * as XLSX from 'xlsx'

const router = useRouter()
const {orderDict} = useDict()

const codeBatchList = ref([])
const showTipKey = ref(1)

const dataForm = reactive({
  code: '',
  codeBatchId: '',
  useStatus: '',
  status: ''
})

const dataList = ref([])
const pageIndex = ref(1)
const pageSize = ref(10)
const totalCount = ref(0)
const dataListLoading = ref(false)
const dataListSelections = ref([])
const addOrUpdateVisible = ref(false)
const addOrUpdateRef = ref(null)

const selectionRows = ref([])
const invitationCodeList = ref([])
const invitationCodeArrays = ref([])
const headers = [
  '邀请码',
  '状态',
  '邀请名称',
  '版本',
  '时长',
  '邀请码有效期',
  '使用人',
  '使用时间'
]

const exportHandle = async () => {
  if (selectionRows.value?.length === 0) {
    ElMessage.error('请选择要导出的数据')
    return
  }

  try {
    await ElMessageBox.confirm('确定要导出邀请码吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    let ids = selectionRows.value.map((item) => item.id) ?? []
    if (ids?.length === 0) {
      ElMessage.error('请选择要导出的数据')
      return
    }

    let res = await api.invitationcode.updateIsLssued(ids)
    invitationCodeArrays.value = []
    selectionRows.value.forEach((obj) => {
      if (obj.code != null) {
        let commodityValidityNum =
            obj.commodityValidityNum +
            getLabel(orderDict.value.timeUnit, obj.commodityValidityUnit)
        let tempDate = ''
        if (obj.validityStartDate && obj.validityEndDate) {
          tempDate =
              obj.validityStartDate.substring(0, 10) +
              '至' +
              obj.validityEndDate.substring(0, 10)
        }
        let statusName = obj.useStatus == 1 ? '已使用' : '未使用'
        const row = [
          obj.code,
          statusName,
          obj.batchName,
          obj.packageName,
          commodityValidityNum,
          tempDate,
          obj.userName,
          obj.useDate
        ]
        invitationCodeArrays.value.push(row)
      }
    })
    exportToExcel()
    ElMessage.success(res.msg)
  } catch (error) {
    // 用户取消操作
  }
}

const getLabel = (dict, value) => {
  const item = dict.find((item) => item.value === value)
  return item ? item.label : ''
}

const getCodeBatchList = async () => {
  const res = await api.invitationcodebatch.list({
    limit: -1,
    notTypeList: [2]
  })
  if (res && res.code === 0) {
    codeBatchList.value = res.data.list
  }
}

const updateCode = async (codeId, codeStatus) => {
  codeStatus = codeStatus == 0 ? 1 : 0
  const res = await api.invitationcode.update({
    id: codeId,
    status: codeStatus
  })
  if (res.code == 0) {
    ElMessage.success('操作成功')
    getDataList()
  }
}

const search = () => {
  pageIndex.value = 1
  getDataList()
}

const getDataList = async () => {
  console.log(dataList.value)

  dataListLoading.value = true
  dataForm.page = pageIndex.value
  dataForm.limit = pageSize.value

  const res = await api.invitationcode.list(dataForm)
  if (res && res.code === 0) {
    dataList.value = res.data.list
    totalCount.value = res.data.totalCount
  } else {
    dataList.value = []
    totalCount.value = 0
  }
  dataListLoading.value = false
}

const jumpUserDetail = (userId) => {
  if (userId) {
    showTipKey.value = Date.now()
    const resolved = router.resolve({
      path: '/userInfo/userList',
      query: {
        userId,
        componentName: 'userDetail'
      }
    })
    window.open(window.location.origin + resolved.href, '_blank')
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

const addOrUpdateHandle = async (id) => {
  addOrUpdateVisible.value = true
  await nextTick()
  addOrUpdateRef.value.init(id)
}

const deleteHandle = async (id) => {
  try {
    await ElMessageBox.confirm(`确定要进行删除吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    const res = await api.invitationcode.delete({id})
    if (res && res.code === 0) {
      ElMessage({
        message: res.msg,
        type: 'success',
        duration: 1500,
        onClose: () => {
          getDataList()
        }
      })
    } else {
      ElMessage.error(res.msg)
    }
  } catch (error) {
    // 用户取消操作
  }
}

const selectionChangeHandle = (val) => {
  selectionRows.value = val
}

const exportToExcel = () => {
  const worksheet = XLSX.utils.aoa_to_sheet([
    headers,
    ...invitationCodeArrays.value
  ])
  const worlbook = XLSX.utils.book_new()
  XLSX.utils.book_append_sheet(worlbook, worksheet, '邀请码列表')
  const excelBuffer = XLSX.write(worlbook, {bookType: 'xlsx', type: 'array'})
  downloadFile(
      excelBuffer,
      `邀请码导出-${invitationCodeArrays.value.length}个.xlsx`
  )
}

const downloadFile = (buffer, filename) => {
  const blob = new Blob([buffer], {type: 'application/octet-stream'})
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = filename
  link.click()
  URL.revokeObjectURL(link.href)
}

onMounted(() => {
  getCodeBatchList()
  getDataList()
})
</script>

<style lang="less" scoped>
.mod-config {
  padding: 15px;
}

:deep(.el-table__fixed-right) {
  height: 100% !important;
}

.user-name-link {
  color: #1890ff;
  cursor: pointer;
  text-decoration: none;
  display: inline-flex;
  align-items: center;
  padding: 4px 8px;
  border-radius: 4px;
  transition: all 0.3s ease;

  &:hover {
    background-color: #e6f7ff;
    color: #096dd9;
    text-decoration: underline;
    transform: translateY(-1px);
  }
}
</style>
