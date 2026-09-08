<template>
  <div class="mod-config">
    <el-form :inline="true" :model="dataForm">
      <el-form-item>
        <el-input
            v-model="dataForm.keyword"
            clearable
            placeholder="输入邀请名称搜索"
            style="width: 150px"
        ></el-input>
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
        <el-button type="primary" @click="addOrUpdateHandle()">新增</el-button>
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
    >
      <el-table-column
          align="center"
          header-align="center"
          label="批次码"
          min-width="180"
          prop="id"
      >
      </el-table-column>
      <el-table-column
          align="center"
          header-align="center"
          label="邀请名称"
          min-width="120"
          prop="name"
          show-overflow-tooltip
      >
      </el-table-column>
      <el-table-column
          align="center"
          header-align="center"
          label="版本"
          min-width="100"
          prop="commodityName"
          show-overflow-tooltip
      >
      </el-table-column>
      <el-table-column
          align="center"
          header-align="center"
          label="时长"
          min-width="100"
          prop="commodityValidityNum"
          show-overflow-tooltip
      >
        <template #default="scope">
          {{
            scope.row.commodityValidityNum +
            getLabel(orderDict.timeUnit, scope.row.commodityValidityUnit)
          }}
        </template>
      </el-table-column>
      <el-table-column
          align="center"
          header-align="center"
          label="邀请码使用情况"
          min-width="280"
          prop="quantity"
      >
        <template #default="scope">
          {{
            '总数：' +
            scope.row.codeCount +
            '，已使用：' +
            scope.row.useCodeCount +
            '，未使用：' +
            scope.row.notUseCodeCount
          }}
        </template>
      </el-table-column>
      <el-table-column
          align="center"
          header-align="center"
          label="有效期"
          min-width="200"
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
          min-width="80"
          prop="status"
      >
        <template #default="scope">
          {{ ['正常', '禁用'][scope.row.status] }}
        </template>
      </el-table-column>
      <el-table-column
          align="center"
          header-align="center"
          label="是否已下发"
          min-width="120"
          prop="isLssued"
      >
        <template #default="scope">
          {{ ['否', '是'][scope.row.isLssued] }}
        </template>
      </el-table-column>
      <el-table-column
          :formatter="formatterIsInfinite"
          align="center"
          header-align="center"
          label="使用次数"
          min-width="150"
          prop="isInfinite"
          show-overflow-tooltip
      >
      </el-table-column>
      <el-table-column
          align="center"
          header-align="center"
          label="申请原因"
          min-width="150"
          prop="remarks"
          show-overflow-tooltip
      >
      </el-table-column>
      <el-table-column
          align="center"
          fixed="right"
          header-align="center"
          label="操作"
          width="270"
      >
        <template #default="scope">
          <el-button
              size="small"
              type="primary"
              @click="exportDetail(scope.row.id)"
          >详情
          </el-button
          >
          <el-button
              :disabled="scope.row?.useCodeCount > 0"
              size="small"
              type="primary"
              @click="addOrUpdateHandle(scope.row.id)"
          >修改
          </el-button>
          <el-button
              size="small"
              type="primary"
              @click="exportHandle(scope.row)"
          >导出
          </el-button
          >
          <el-button
              :disabled="scope.row?.useCodeCount > 0"
              size="small"
              type="danger"
              @click="deleteHandle(scope.row.id)"
          >删除
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
        @is-ok="addOrUpdateVisible = false"
    ></add-or-update>
    <batchDetails ref="batchDetailRef"></batchDetails>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import { customSvg } from '@/utils/icon.js'
import { useDict } from '@/hooks/useDict.js'
import api from '@/utils/request-api'
import AddOrUpdate from './invitationcodebatch-add-or-update.vue'
import * as XLSX from 'xlsx'
import batchDetails from '@/views/invitationCode/batch/batchDetails.vue'

const {orderDict, getLabel} = useDict()

const dataForm = reactive({
  keyword: '',
  status: ''
})
const mapIsInfinite = reactive({
  0: '一次',
  1: '无限次'
})
const dataList = ref([])
const pageIndex = ref(1)
const pageSize = ref(10)
const totalCount = ref(0)
const dataListLoading = ref(false)
const dataListSelections = ref([])
const addOrUpdateVisible = ref(false)
const addOrUpdateRef = ref()
const batchDetailRef = ref()

// 导出邀请码批次
const invitationCodeList = ref([])
// 将获取到的邀请码转换成二维数组
const invitationCodeArrays = ref([])
// 导出邀请码Excel表头
const headers = [
  '邀请码',
  '状态',
  '版本',
  '时长',
  '邀请码有效期',
  '使用人',
  '使用时间'
]

// 导出为Excel
const exportToExcel = (fileName) => {
  // 将数据转换为 worksheet
  const worksheet = XLSX.utils.aoa_to_sheet([
    headers,
    ...invitationCodeArrays.value
  ])

  // 创建 workbook 并添加到 worksheet
  const worlbook = XLSX.utils.book_new()
  XLSX.utils.book_append_sheet(worlbook, worksheet, '邀请码列表')

  // 生成 Excel 文件
  const excelBuffer = XLSX.write(worlbook, {bookType: 'xlsx', type: 'array'})

  // 下载 Excel文件
  downloadFile(excelBuffer, (fileName ?? 'export') + '.xlsx')
}

// 下载Excel文件
const downloadFile = (buffer, filename) => {
  // 创建 Blob对象
  const blob = new Blob([buffer], {type: 'application/octet-stream'})

  // 创建下载链接
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = filename

  // 触发点击事件
  link.click()

  // 清理
  URL.revokeObjectURL(link.href)
}

// 根据邀请码批次Id导出该批次的所有邀请码
const exportHandle = async (item) => {
  try {
    await ElMessageBox.confirm('确定要导出邀请码吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    // 每次导出前清空数组
    let commodityValidityNum =
        item.commodityValidityNum +
        getLabel(orderDict.timeUnit, item.commodityValidityUnit)
    let tempDate = ''
    if (item.validityStartDate && item.validityEndDate) {
      tempDate =
          item.validityStartDate.substring(0, 10) +
          '至' +
          item.validityEndDate.substring(0, 10)
    }
    invitationCodeArrays.value = []

    const res = await api.invitationcodebatch.invitationByBatchId({
      batchId: item.id
    })
    if (res && res.code === 0) {
      invitationCodeList.value = res.data
      invitationCodeList.value.sort(
          (a, b) => a.useStatus ?? 0 - b.useStatus ?? 0
      )
      invitationCodeList.value.forEach((obj) => {
        if (obj.code != null) {
          let statusName = obj.useStatus == 1 ? '已使用' : '未使用'
          const row = [
            obj.code,
            statusName,
            obj.packageName,
            commodityValidityNum,
            tempDate,
            obj.userName,
            obj.useDate
          ]
          invitationCodeArrays.value.push(row)
        }
      })
      exportToExcel(
          `邀请码导出-${item.name}(${item.commodityName} ${commodityValidityNum})`
      )
      ElMessage({
        message: '导出成功',
        type: 'success',
        duration: 1500,
        onClose: () => {
          getDataList()
        }
      })
    }
  } catch (error) {
    // 用户取消操作
  }
}

// 查询
const search = () => {
  pageIndex.value = 1
  getDataList()
}

// 获取数据列表
const getDataList = async () => {
  dataListLoading.value = true
  dataForm.page = pageIndex.value
  dataForm.limit = pageSize.value
  dataForm.notTypeList = [2]

  const res = await api.invitationcodebatch.list(dataForm)
  if (res && res.code === 0) {
    dataList.value = res.data.list
    totalCount.value = res.data.totalCount
  } else {
    dataList.value = []
    totalCount.value = 0
  }
  dataListLoading.value = false
}

// 每页数
const sizeChangeHandle = (val) => {
  pageSize.value = val
  pageIndex.value = 1
  getDataList()
}

// 当前页
const currentChangeHandle = (val) => {
  pageIndex.value = val
  getDataList()
}

const exportDetail = async (id) => {
  await nextTick(() => {
    batchDetailRef.value.init(id)
  })
}

// 新增 / 修改
const addOrUpdateHandle = async (id) => {
  addOrUpdateVisible.value = true
  await nextTick(() => {
    addOrUpdateRef.value.init(id)
  })
}

// 删除
const deleteHandle = async (id) => {
  await ElMessageBox.confirm(`确定要进行删除吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  const res = await api.invitationcodebatch.delete({id})
  if (res && res.code === 0) {
    getDataList()
    ElMessage({
      message: res.msg,
      type: 'success'
    })
  }
}
// 格式化使用次数
const formatterIsInfinite = (row) => {
  if (row.isInfinite !== null && row.isInfinite !== undefined) {
    console.log('row.isInfinite', row.isInfinite)
    return mapIsInfinite[row.isInfinite]
  }
}
onMounted(() => {
  getDataList()
})
</script>

<style lang="less" scoped>
.disabled-cell {
  color: #ccc;
  background-color: grey;
  border-color: grey;
  pointer-events: none; /* 禁用点击 */
  cursor: not-allowed; /* 显示不可点击的光标 */
}

.mod-config {
  padding: 15px;
}

:deep(.el-table__fixed-right) {
  height: 100% !important;
}
</style>
