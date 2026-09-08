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
            placeholder="输入提示词搜索"
            style="width: 150px"
        ></el-input>
      </el-form-item>

      <el-form-item>
        <el-select
            v-model="dataForm.cueType"
            clearable
            placeholder="类型"
            style="width: 150px"
        >
          <el-option
              v-for="item in wordDict.cueType"
              :key="item.value"
              :label="item.label"
              :value="item.value"
          ></el-option>
        </el-select>
      </el-form-item>

      <el-form-item>
        <el-select
            v-model="dataForm.applyTo"
            clearable
            placeholder="场景"
            style="width: 150px"
        >
          <el-option
              v-for="item in wordDict.applyTo"
              :key="item.value"
              :label="item.label"
              :value="item.value"
          ></el-option>
        </el-select>
      </el-form-item>

      <el-form-item>
        <el-select
            v-model="dataForm.scope"
            clearable
            placeholder="范围"
            style="width: 150px"
        >
          <el-option
              v-for="item in wordDict.cueScope"
              :key="item.value"
              :label="item.label"
              :value="item.value"
          ></el-option>
        </el-select>
      </el-form-item>

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
          :min-width="130"
          align="center"
          header-align="center"
          label="行业"
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
          :min-width="120"
          align="center"
          header-align="center"
          label="类型"
          prop="cueType"
          show-overflow-tooltip
      >
        <template #default="scope">
          <span>{{ getLabel(wordDict.cueType, scope.row.cueType) }}</span>
        </template>
      </el-table-column>

      <el-table-column
          align="center"
          header-align="center"
          label="场景"
          prop="applyTo"
          width="100"
      >
        <template #default="scope">
          <span>{{ getLabel(wordDict.applyTo, scope.row.applyTo) }}</span>
        </template>
      </el-table-column>

      <el-table-column
          align="center"
          header-align="center"
          label="范围"
          prop="scope"
          width="80"
      >
        <template #default="scope">
          <span>{{ getLabel(wordDict.cueScope, scope.row.scope) }}</span>
        </template>
      </el-table-column>

      <el-table-column
          align="center"
          header-align="center"
          label="排序"
          prop="sort"
          show-overflow-tooltip
          width="80"
      >
      </el-table-column>
      <el-table-column
          align="center"
          label="提示词"
          min-width="130"
          prop="cueWord"
          show-overflow-tooltip
      >
      </el-table-column>

      <el-table-column
          align="center"
          header-align="center"
          label="实际提示语句"
          min-width="160"
          prop="problem"
          show-overflow-tooltip
      >
      </el-table-column>
      <el-table-column align="center" header-align="center" label="提示词概要" min-width="100" prop="outline"
                       show-overflow-tooltip>
      </el-table-column>
      <el-table-column
          align="center"
          header-align="center"
          label="描述"
          min-width="100"
          prop="remarks"
          show-overflow-tooltip
      >
      </el-table-column>

      <el-table-column
          align="center"
          header-align="center"
          label="创建时间"
          prop="createDate"
          width="100"
      >
        <template #default="scope">
          <span>{{ scope.row.createDate.substring(0, 10) }}</span>
        </template>
      </el-table-column>

      <el-table-column
          align="center"
          header-align="center"
          label="修改时间"
          prop="updateDate"
          width="100"
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
        :background="true"
        :current-page="pageIndex"
        :page-size="pageSize"
        :page-sizes="[10, 20, 50]"
        :total="totalCount"
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
import { ElMessageBox } from 'element-plus'
import { customSvg } from '@/utils/icon.js'
import api from '@/utils/request-api'
import { useUserInfoStore } from '@/store'
import { storeToRefs } from 'pinia'
import AddOrUpdate from './cuewords-add-or-update.vue'
import { useDict } from '@/hooks/useDict'

const route = useRoute()
const userInfoStore = useUserInfoStore()
const {token} = storeToRefs(userInfoStore)
const {wordDict, getLabel} = useDict()

const fileList = ref([])
const tradeTreeList = ref([])
const tradeList = ref([])
const dataForm = reactive({
  keyword: '',
  cueType: '',
  tradeId: '',
  scope: '',
  applyTo: ''
})
const dataList = ref([])
const pageIndex = ref(1)
const pageSize = ref(10)
const totalCount = ref(0)
const dataListLoading = ref(false)
const addOrUpdateVisible = ref(false)
const tradeId = ref('')
const addOrUpdate = ref(null)

const uploadUrl = computed(() => {
  return api.common.uploadWordExcel
})

const uploadHeader = computed(() => {
  return {
    token: token.value
  }
})

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

const getTradeTreeList = async () => {
  tradeTreeList.value = []
  const res = await api.trade.listTree({})
  if (res && res.code === 0) {
    tradeTreeList.value = res.data
  }
}

const getTradeList = async () => {
  tradeList.value = []
  const res = await api.trade.list({limit: -1})
  if (res.code == 0) {
    tradeList.value = res.data.list
  }
}

const getDataList = async () => {
  dataListLoading.value = true
  dataForm.page = pageIndex.value
  dataForm.limit = pageSize.value

  try {
    const res = await api.cuewords.list(dataForm)
    if (res && res.code === 0) {
      dataList.value = res.data.list
      totalCount.value = res.data.totalCount
    } else {
      dataList.value = []
      totalCount.value = 0
    }
  } catch (error) {
    dataList.value = []
    totalCount.value = 0
  } finally {
    dataListLoading.value = false
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

const addOrUpdateHandle = (id) => {
  addOrUpdateVisible.value = true
  nextTick(() => {
    addOrUpdate.value.init(id, [], 0)
  })
}
const deleteHandle = (id) => {
  ElMessageBox.confirm('确定要进行删除吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    const res = await api.cuewords.delete({id})
    if (res && res.code === 0) {
      getDataList()
      ElMessage({
        message: res.msg,
        type: 'success'
      })
    }
  })
}

onMounted(() => {
  dataForm.tradeId = ''

  if (route.query.tradeId) dataForm.tradeId = route.query.tradeId
  if (route.query.cueType) dataForm.cueType = route.query.cueType
  if (route.query.scope) dataForm.scope = route.query.scope

  getTradeList()
  getTradeTreeList()
  getDataList()
})
</script>
<style lang="less" scoped>
.mod-config {
  padding: 15px;
}

:deep(.el-table__fixed-right ) {
  height: 100% !important;
}
</style>
