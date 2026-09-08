<template>
  <el-dialog
    :title="'关联账号'"
    :close-on-click-modal="false"
    :fullscreen="isMobile"
    :class="{ 'mobile-dialog-innner-custom': isMobile }"
    v-model="visible"
  >
    <el-table
      :data="dataList"
      border
      stripe
      size="default"
      v-loading="dataListLoading"
      :element-loading-spinner="customSvg"
      header-row-class-name="my-header-row"
      style="width: 100%"
    >
      <el-table-column
        prop="nickName"
        header-align="center"
        align="center"
        label="昵称"
      ></el-table-column>
      <el-table-column
        prop="username"
        header-align="center"
        align="center"
        label="账号"
      ></el-table-column>
      <el-table-column
        prop="phone"
        header-align="center"
        align="center"
        label="手机号"
      ></el-table-column>
      <el-table-column
        prop="tradeName"
        header-align="center"
        align="center"
        label="行业"
      ></el-table-column>
    </el-table>

    <el-pagination
      style="text-align: center; margin-top: 10px"
      @size-change="sizeChangeHandle"
      @current-change="currentChangeHandle"
      :current-page="pageIndex"
      :page-sizes="[10, 20, 50]"
      :page-size="pageSize"
      :total="totalCount"
      layout="sizes,total,->,prev, pager, next, jumper"
      background
    >
    </el-pagination>

    <template #footer>
      <span class="dialog-footer">
        <el-button @click="visible = false">取消</el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { customSvg } from '@/utils/icon.js'
import api from '@/utils/request-api'
const props = defineProps({
  isMobile: {
    type: Boolean,
    default: false,
  },
})
const visible = ref(false)
const selectedRows = ref([])
const dataList = ref([])
const dataListLoading = ref(false)
const pageIndex = ref(1)
const pageSize = ref(10)
const totalCount = ref(0)
const type = ref(1)
const dataForm = reactive({})

const getDataList = async () => {
  dataForm.pageIndex = pageIndex.value
  dataForm.pageSize = pageSize.value
  try {
    const res = await api.user.selectByuseId(dataForm)
    if (res && res.code === 0) {
      dataList.value = res.data.list
    }
  } catch (error) {
    console.error('获取数据失败:', error)
  }
}

const handleSelectionChange = (selectedRowsData) => {
  selectedRows.value = selectedRowsData
}

const init = (id, typeValue) => {
  type.value = typeValue
  dataForm.secUid = id
  dataForm.page = pageIndex.value
  dataForm.limit = pageSize.value
  visible.value = true
  getDataList()
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

const dataFormSubmit = async () => {
  visible.value = false
  const idList = selectedRows.value.map((row) => row.id)
  dataForm.userId = idList

  try {
    if (type.value == 1) {
      const res = await api.anchorurl.saveAnchorUrlWhite(dataForm)
      if (res && res.code === 0) {
        ElMessage.success(res.msg)
      } else {
        ElMessage.error('添加失败')
      }
    } else {
      const res = await api.anchorurl.removeAnchorUrlWhite(dataForm)
      if (res && res.code === 0) {
        ElMessage.success(res.msg)
      } else {
        ElMessage.error('移除失败')
      }
    }
  } catch (error) {
    ElMessage.error('操作失败')
    console.error('操作失败:', error)
  }
}

defineExpose({
  init,
})
</script>
