<template>
  <div class="mod-config">
    <el-form :inline="true" :model="searchForm">
      <el-form-item>
        <el-input
            v-model="searchForm.versionNum"
            clearable
            placeholder="输入版本号搜索"
            style="width: 160px"
        ></el-input>
      </el-form-item>
      <el-form-item>
        <el-select
            v-model="searchForm.isFront"
            clearable
            placeholder="选择更新类型查询"
            style="width: 160px"
        >
          <el-option
              v-for="item in isFrontOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
          ></el-option>
        </el-select>
      </el-form-item>
      <el-form-item style="margin-left: 5px">
        <el-button icon="Search" plain type="primary" @click="seach"
        >查询
        </el-button
        >
      </el-form-item>
    </el-form>
    <div style="margin-bottom: 5px">
      <el-button
          icon="Plus"
          size="small"
          type="primary"
          @click="addOrUpdateHandle(0)"
      >添加版本
      </el-button
      >
      <el-button
          icon="DeleteFilled"
          size="small"
          type="danger"
          @click="handleDeleteById"
      >选中删除
      </el-button
      >
    </div>

    <el-table
        v-loading="dataListLoading"
        :data="dataList"
        :element-loading-spinner="customSvg"
        border
        header-row-class-name="my-header-row"
        size="default"
        stripe
        style="width: 100%"
        @selection-change="handleSelectionChange"
    >
      <el-table-column
          align="center"
          type="selection"
          width="55"
      ></el-table-column>
      <el-table-column
          :min-width="130"
          align="center"
          header-align="center"
          label="版本号"
          prop="versionNum"
      ></el-table-column>
      <el-table-column
          :min-width="100"
          align="center"
          header-align="center"
          label="版本值"
          prop="version"
          show-overflow-tooltip
      ></el-table-column>
      <el-table-column
          :width="100"
          align="center"
          header-align="center"
          label="更新方式"
          prop="updateType"
          show-overflow-tooltip
      >
        <template #default="{ row }">
          <span>{{ row.updateType == 0 ? '手动更新' : '强制更新' }}</span>
        </template>
      </el-table-column>
      <el-table-column
          :min-width="150"
          align="center"
          header-align="center"
          label="更新类型"
          prop="isFront"
          show-overflow-tooltip
      >
        <template #default="{ row }">
          <span>{{ row.isFront == 0 ? '爱复盘主程序' : '更新程序' }}</span>
        </template>
      </el-table-column>
      <el-table-column
          :width="100"
          align="center"
          header-align="center"
          label="是否维护"
          prop="isPreserve"
      >
        <template #default="{ row }">
          <span>{{ row.isPreserve == 0 ? '否' : '是' }}</span>
        </template>
      </el-table-column>
      <el-table-column
          align="center"
          header-align="center"
          label="状态"
          prop="status"
          width="160px"
      >
        <template #default="{ row }">
          <el-switch
              v-model="row.status"
              :active-value="1"
              :inactive-value="0"
              active-text="发布"
              inactive-text="开发"
              @change="handleSwitchChange(row)"
          ></el-switch>
        </template>
      </el-table-column>
      <el-table-column
          :width="180"
          align="center"
          header-align="center"
          label="添加时间"
          prop="updateTime"
      ></el-table-column>
      <el-table-column
          align="center"
          fixed="right"
          header-align="center"
          label="操作"
          width="250"
      >
        <template #default="{ row }">
          <el-button
              size="small"
              type="primary"
              @click="addOrUpdateHandle(row.id)"
          >查看编辑
          </el-button
          >
          <el-button
              :disabled="row.isFront !== 0"
              size="small"
              type="primary"
              @click="openPackage(row)"
          >
            补丁包
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
        :background="true"
        :current-page="pageIndex"
        :page-size="pageSize"
        :page-sizes="[10, 20, 50]"
        :total="totalCount"
        layout="total, sizes, prev, pager,next,->, jumper"
        style="text-align: center; margin-top: 10px"
        @size-change="sizeChangeHandle"
        @current-change="currentChangeHandle"
    ></el-pagination>

    <update-or-add
        v-if="showDialog"
        ref="refUpdateOrAdd"
        @refreshDataList="refreshDataList"
    ></update-or-add>
    <patchPackage
        v-if="patchPackageDialog"
        ref="patchPackageRef"
        @close="patchPackageDialog = false"
    ></patchPackage>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import updateOrAdd from './update-or-add.vue'
import patchPackage from '@/views/client/update/components/packageList.vue'
import { customSvg } from '@/utils/icon.js'
import api from '@/utils/request-api'

// 选项
const isFrontOptions = [
  {value: '0', label: '爱复盘主程序'},
  {value: '1', label: '更新程序'}
]

// 状态
const searchForm = reactive({
  versionNum: null,
  pageIndex: 1,
  pageSize: 10,
  isFront: null,
  parentId: 0
})
const showDialog = ref(false)
const value1 = ref([])
const dataList = ref([])
const pageIndex = ref(1)
const pageSize = ref(10)
const totalCount = ref(0)
const totalPage = ref(0)
const dataListLoading = ref(false)
const patchPackageDialog = ref(true)
const dataListSelections = ref([])

// 子组件引用
const refUpdateOrAdd = ref()
const patchPackageRef = ref()

// 行为
const handleSwitchChange = async (row) => {
  const status = row.status || row.status == 1 ? 1 : 0
  const res = await api.clientupdate.updateStatus({id: row.id, status})
  if (res && res.code === 0) {
    ElMessage.success('操作成功')
    getDataList()
  }
}

// 多选触发事件
const handleSelectionChange = (rows) => {
  const checkedLength = rows.length
  if (checkedLength > 0) {
    dataListSelections.value = rows.map((r) => r.id)
  } else {
    dataListSelections.value = []
  }
}

const handleDeleteById = async () => {
  try {
    await ElMessageBox.confirm('确定要删除选中的记录?', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    if (dataListSelections.value.length > 0) {
      const res = await api.clientupdate.delete(dataListSelections.value)
      if (res && res.code === 0) {
        pageIndex.value = 1
        getDataList()
      }
    } else {
      ElMessage.error('请选择要删除的数据')
    }
  } catch (e) {
    // 用户取消
  }
}

const seach = () => {
  pageIndex.value = 1
  getDataList()
}

// 获取数据列表
const getDataList = async () => {
  searchForm.limit = pageSize.value
  searchForm.page = pageIndex.value
  dataListLoading.value = true
  try {
    const res = await api.clientupdate.list(searchForm)
    if (res && res.code === 0) {
      dataList.value = res.data.list
      totalCount.value = res.data.totalCount
      totalPage.value = res.data.totalPage
    }
  } finally {
    dataListLoading.value = false
  }
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

const addOrUpdateHandle = (id) => {
  showDialog.value = true
  nextTick(() => {
    refUpdateOrAdd.value && refUpdateOrAdd.value.init(id)
  })
}

const openPackage = (item) => {
  patchPackageDialog.value = true
  nextTick(() => {
    patchPackageRef.value && patchPackageRef.value.init(item)
  })
}

const refreshDataList = (isAdd) => {
  showDialog.value = false
  if (isAdd == 1) {
    pageIndex.value = 1
  }
  getDataList()
}

onMounted(() => {
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

.mod-config {
  padding: 15px;
}

.second-button {
  margin-top: 14px;
  /* 5px + 9px = 14px，向左偏移 9px */
  // margin-right: 80px;
}

.second-button1 {
  margin-top: 0px;
  /* 5px + 9px = 14px，向左偏移 9px */
  // margin-left: 100px;
}
</style>
