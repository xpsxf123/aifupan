<template>
  <div class="mod-config">
    <el-form :inline="true" :model="dataForm">
      <el-form-item>
        <el-cascader
            v-model="dataForm.tradeId"
            :options="tradeTreeList"
            :props="{ checkStrictly: true, value: 'id', label: 'name' }"
            clearable
            filterable
            placeholder="按行业筛选"
            @change="tradeChange"
        >
        </el-cascader>
      </el-form-item>

      <el-form-item>
        <el-input
            v-model="dataForm.keyword"
            clearable
            placeholder="输入按钮名称筛选"
            style="width: 180px"
        ></el-input>
      </el-form-item>

      <el-form-item>
        <el-select
            v-model="dataForm.buttonType"
            clearable
            placeholder="类型"
            style="width: 150px"
        >
          <el-option label="运营按钮提示词" value="0"></el-option>
          <el-option label="违规按钮提示词" value="1"></el-option>
        </el-select>
      </el-form-item>

      <!--      <el-form-item >-->
      <!--          <el-select v-model="dataForm.scope" placeholder="范围" clearable style="width: 150px;">-->
      <!--            <el-option label="全文" value= '0' ></el-option>-->
      <!--            <el-option label="段落" value= '1' ></el-option>-->
      <!--          </el-select>-->
      <!--      </el-form-item>-->

      <!--      <el-table-column prop="sort" header-align="center" align="center" label="排序" width="100">-->
      <!--      </el-table-column>-->

      <el-form-item>
        <div style="display: flex">
          <el-button icon="Search" plain type="primary" @click="search">查询</el-button>
          <el-button type="primary" @click="addOrUpdateHandle()"
          >新增
          </el-button
          >
          <el-upload
              :action="uploadUrl"
              :file-list="fileList"
              :headers="uploadHeader"
              :limit="99"
              :show-file-list="false"
              accept=".xlsx"
              style="margin-left: 10px"
          >
            <!--            <el-button type="primary">导入excel</el-button>-->
          </el-upload>
        </div>
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
          label="行业"
          min-width="160px"
          prop="tradeId"
          show-overflow-tooltip
      >
        <template #default="scope">
          <span v-if="tradeList && tradeList.length > 0">{{
              tradeList.filter((item) => item.id == scope.row.tradeId).length > 0
                  ? tradeList.filter((item) => item.id == scope.row.tradeId)[0].name
                  : ''
            }}</span>
        </template>
      </el-table-column>

      <el-table-column
          align="center"
          header-align="center"
          label="类型"
          min-width="150px"
          prop="buttonType"
          show-overflow-tooltip
      >
        <template #default="scope">
          <span>{{
              scope.row.buttonType == 0 ? '运营按钮提示词' : '违规按钮提示词'
            }}</span>
        </template>
      </el-table-column>

      <!--      <el-table-column prop="scope" header-align="center" align="center" label="范围">-->
      <!--        <template slot-scope="scope">-->
      <!--          <span>{{ scope.row.scope == 0 ? '全文' : '段落' }}</span>-->
      <!--        </template>-->
      <!--      </el-table-column>-->

      <el-table-column
          align="center"
          header-align="center"
          label="按钮名称"
          min-width="150px"
          prop="buttonName"
          show-overflow-tooltip
      >
      </el-table-column>

      <el-table-column
          align="center"
          header-align="center"
          label="实际提示语句"
          min-width="150px"
          prop="problem"
          show-overflow-tooltip
      >
      </el-table-column>

      <el-table-column
          align="center"
          header-align="center"
          label="描述"
          prop="remarks"
          show-overflow-tooltip
      >
      </el-table-column>

      <el-table-column
          align="center"
          header-align="center"
          label="排序"
          prop="sort"
          show-overflow-tooltip
      >
      </el-table-column>

      <el-table-column
          align="center"
          header-align="center"
          label="创建时间"
          min-width="120px"
          prop="createDate"
      >
        <template #default="scope">
          <span>{{ scope.row.createDate.substring(0, 10) }}</span>
        </template>
      </el-table-column>

      <el-table-column
          align="center"
          header-align="center"
          label="最后修改时间"
          min-width="120px"
          prop="updateDate"
      >
        <template #default="scope">
          <span>{{ scope.row.updateDate.substring(0, 10) }}</span>
        </template>
      </el-table-column>

      <el-table-column
          align="center"
          fixed="right"
          header-align="center"
          label="操作"
          width="150"
      >
        <template #default="scope">
          <el-button
              size="small"
              type="primary"
              @click="addOrUpdateHandle(scope.row.id)"
          >修改
          </el-button
          >
          <el-button
              size="small"
              type="danger"
              @click="deleteHandle(scope.row.id)"
          >删除
          </el-button
          >
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
        ref="addOrUpdate"
        @refreshDataList="getDataList"
    ></add-or-update>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, nextTick } from 'vue'

import { useRoute } from 'vue-router'
import { useUserInfoStore } from '@/store'
import api from '@/utils/request-api'
import { customSvg } from '@/utils/icon.js'
import AddOrUpdate from './aiCueButton-add-or-update.vue'
import { storeToRefs } from 'pinia'

const route = useRoute()
const {token} = storeToRefs(useUserInfoStore())

const fileList = ref([])
const tradeTreeList = ref([]) // 行业列表树形
const tradeList = ref([]) // 行业列表
const dataForm = reactive({
  keyword: '',
  buttonType: '',
  tradeId: ''
})
const dataList = ref([])
const pageIndex = ref(1)
const pageSize = ref(10)
const totalCount = ref(0)
const dataListLoading = ref(false)
const addOrUpdateVisible = ref(false)
const tradeId = ref('')
const addOrUpdate = ref(null)
// 上传地址
const uploadUrl = computed(() => {
  return api.common.uploadWordExcel
})

// 上传的请求头
const uploadHeader = computed(() => {
  return {
    token: token.value
  }
})

onMounted(() => {
  dataForm.tradeId = ''

  //请求数据解析
  //行业数据
  if (route.query.tradeId) dataForm.tradeId = route.query.tradeId

  //提示词类型
  if (route.query.button_type) dataForm.button_type = route.query.button_type

  // //提示词范围
  // if (route.query.scope) dataForm.scope = route.query.scope;

  getTradeList()
  getTradeTreeList()
  getDataList()
})

// 选中行业回调
const tradeChange = (value) => {
  if (value && value.length > 0) {
    dataForm.tradeId = value[value.length - 1]
  } else {
    dataForm.tradeId = ''
  }
}

const search = () => {
  pageIndex.value = 1
  getDataList()
}

// 获取行业列表树形
const getTradeTreeList = async () => {
  tradeTreeList.value = []
  const res = await api.trade.listTree({})
  if (res && res.code === 0) {
    tradeTreeList.value = res.data
  }
}

// 获取行业列表
const getTradeList = async () => {
  tradeList.value = []
  const res = await api.trade.list({limit: -1})
  if (res.code == 0) {
    tradeList.value = res.data.list
  }
}
// 获取数据列表
const getDataList = async () => {
  try {
    dataListLoading.value = true
    dataForm.page = pageIndex.value
    dataForm.limit = pageSize.value

    const res = await api.aiCueButton.list(dataForm)
    if (res && res.code === 0) {
      dataList.value = res.data.list
      totalCount.value = res.data.totalCount
    } else {
      dataList.value = []
      totalCount.value = 0
    }
  } catch (error) {
    console.error('获取数据列表失败:', error)
    dataList.value = []
    totalCount.value = 0
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

// 新增 / 修改
const addOrUpdateHandle = (id) => {
  addOrUpdateVisible.value = true
  nextTick(() => {
    addOrUpdate.value.init(id, [], 0)
  })
}

// 删除
const deleteHandle = async (id) => {
  await ElMessageBox.confirm('确定要进行删除吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  const res = await api.aiCueButton.delete({id})
  if (res && res.code === 0) {
    getDataList()
    ElMessage({
      message: res.msg,
      type: 'success'
    })
  }
}
</script>
<style lang="less" scoped>
.mod-config {
  padding: 15px;
}

:deep(.el-table__fixed-right) {
  height: 100% !important;
}
</style>
