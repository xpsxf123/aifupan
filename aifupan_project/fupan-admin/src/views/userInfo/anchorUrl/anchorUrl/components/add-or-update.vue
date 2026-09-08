<template>
  <el-dialog
    :title="type == 1 ? '添加白名单' : '已添加的白名单'"
    :close-on-click-modal="false"
    v-model="visible"
    :fullscreen="isMobile"
    :class="{ 'mobile-dialog-innner-custom': isMobile }"
  >
    <el-form :inline="true" :model="dataForm" style="margin-bottom: 10px">
      <el-form-item>
        <el-input
          v-model="dataForm.keyword"
          placeholder="输入账号/昵称搜索"
          clearable
          style="width: 200px"
        ></el-input>
      </el-form-item>
      <el-form-item>
        <el-input
          v-model="dataForm.phone"
          placeholder="输入账号手机号搜索"
          clearable
          style="width: 200px"
        ></el-input>
      </el-form-item>
      <el-form-item>
        <el-button @click="getDataList()">查询</el-button>
      </el-form-item>
    </el-form>

    <el-table
      :data="dataList"
      :element-loading-spinner="customSvg"
      border
      stripe
      size="default"
      v-loading="dataListLoading"
      style="width: 100%"
      header-row-class-name="my-header-row"
      background
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="55"></el-table-column>
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
        <el-button type="primary" @click="dataFormSubmit()">{{
          type == 1 ? '确定' : '移除'
        }}</el-button>
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
  if (type.value == 1) {
    const res = await api.user.list(dataForm)
    if (res && res.code === 0) {
      dataList.value = res.data.list
    }
  } else {
    const res = await api.anchorurl.seletUidAnchorUrlWhite(dataForm)
    if (res && res.code === 0) {
      if (res.data == null) {
        dataList.value = null
      } else {
        dataList.value = res.data.list
      }
    }
  }
}

const handleSelectionChange = (rows) => {
  console.log(rows)
  selectedRows.value = rows
}

const init = (id, typeParam) => {
  type.value = typeParam
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
}

defineExpose({
  init,
})
</script>
