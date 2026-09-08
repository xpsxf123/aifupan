<template>
  <div class="mod-config">
    <el-form :inline="true" :model="dataForm">
      <el-form-item>
        <el-input
            v-model="dataForm.keyword"
            clearable
            placeholder="输入关键词搜索"
            style="width: 150px"
        ></el-input>
      </el-form-item>
      <el-form-item>
        <el-input
            v-model="dataForm.groupStr"
            clearable
            placeholder="输入分组搜索"
            style="width: 150px"
        ></el-input>
      </el-form-item>
      <el-form-item>
        <el-select
            v-model="dataForm.platformType"
            clearable
            placeholder="按平台筛选"
            style="width: 150px"
        >
          <el-option
              v-for="item in platformList"
              :key="item.id"
              :label="item.label"
              :value="item.value"
          >
          </el-option>
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-cascader
            v-model="tradeIdArr"
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
        <el-cascader
            v-model="dataForm.cruxTypeId"
            :options="cruxTypeTreeList"
            :props="{
            checkStrictly: true,
            expandTrigger: 'click',
            value: 'id',
            label: 'name',
            emitPath: false,
          }"
            clearable
            placeholder="按关键词分类筛选"
        >
        </el-cascader>
      </el-form-item>
      <!-- <el-form-item>
        <el-select v-model="dataForm.resourceType" placeholder="按来源类型筛选" clearable style="width: 180px;">
          <el-option v-for="item in resourceList" :key="item.id" :label="item.label" :value="item.value">
          </el-option>
        </el-select>
      </el-form-item> -->
      <el-form-item>
        <el-select
            v-model="dataForm.wordLength"
            clearable
            placeholder="按词语字数筛选"
            style="width: 180px"
        >
          <el-option v-for="item in 20" :key="item" :label="item" :value="item">
          </el-option>
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button icon="Search" plain type="primary" @click="search">查询</el-button>
        <el-button type="primary" @click="addOrUpdateHandle()">新增</el-button>
        <el-button type="primary" @click="openBatchAdd()">批量新增</el-button>
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
      <el-table-column type="expand">
        <template #default="props">
          <div
              v-if="
              props.row.similarWordList && props.row.similarWordList.length > 0
            "
          >
            <div
                v-for="(similarItem, index) in props.row.similarWordList"
                :key="similarItem.id || index"
                style="padding: 4px 16px"
            >
              <!-- {{index + 1 +  ".相似词：" + similarItem.name + (similarItem.remarks ? "，描述：" + similarItem.remarks : "") }} -->
              <span
                  :style="
                  similarItem.isMark && similarItem.isMark == 1
                    ? 'background:pink'
                    : ''
                "
              >{{ index + 1 + '.相似词：' + similarItem.name }}</span
              >
            </div>
          </div>
        </template>
      </el-table-column>
      <!-- <el-table-column prop="id" header-align="center" align="center" label="ID" width="180">
      </el-table-column> -->
      <el-table-column
          align="center"
          header-align="center"
          label="分组"
          prop="groupStr"
          show-overflow-tooltip
      >
      </el-table-column>
      <el-table-column
          align="center"
          header-align="center"
          label="关键词"
          prop="name"
      >
      </el-table-column>
      <!-- <el-table-column prop="resourceType" header-align="center" align="center" label="来源类型">
        <template slot-scope="scope">
          <span>{{ resourceList.length > 0 ? resourceList.filter(item => item.value ==
            scope.row.resourceType)[0].label
            : '-' }}</span>
        </template>
      </el-table-column> -->
      <el-table-column
          align="center"
          header-align="center"
          label="关键词分类"
          prop="cruxTypeName"
          width="100px"
      >
      </el-table-column>
      <el-table-column
          align="center"
          header-align="center"
          label="平台类型"
          prop="platformType"
          show-overflow-tooltip=""
          width="120px"
      >
        <template #default="scope">
          <span>{{
              (
                  platformList.find(
                      (item) => item.value == scope.row.platformType
                  ) || {}
              )?.label || '-'
            }}</span>
        </template>
      </el-table-column>
      <el-table-column
          align="center"
          header-align="center"
          label="行业"
          min-width="160px"
          prop="tradeId"
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
          label="概览"
          prop="overView"
          show-overflow-tooltip
      >
      </el-table-column>

      <el-table-column
          align="center"
          header-align="center"
          label="描述"
          min-width="120px"
          prop="remarks"
          show-overflow-tooltip
      >
      </el-table-column>
      <el-table-column
          align="center"
          header-align="center"
          label="创建时间"
          prop="createDate"
          width="120px"
      >
        <template #default="scope">
          <span>{{ scope.row.createDate.substring(0, 10) }}</span>
        </template>
      </el-table-column>
      <el-table-column
          align="center"
          header-align="center"
          label="最后修改时间"
          prop="updateDate"
          width="120px"
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
        style="margin-top: 10px"
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
    <!-- 弹窗, 批量添加 -->
    <batch-add
        v-if="batchAddVisible"
        ref="batchAddRef"
        @refreshDataList="getDataList"
    ></batch-add>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import api from '@/utils/request-api'
import AddOrUpdate from '../sensitive/sensitivewords-add-or-update.vue'
import BatchAdd from './sensitivewords-batch-add.vue'
import { customSvg } from '@/utils/icon.js'

const tradeTreeList = ref([])
const tradeList = ref([])
const platformList = ref([])
const resourceList = ref([])
const typeList = ref([])
const cruxTypeTreeList = ref([])

const dataForm = reactive({
  keyword: '',
  resourceType: 0,
  type: '',
  platformType: '',
  level: '',
  tradeId: '',
  groupStr: '',
  wordsType: 1,
  wordLength: '',
  cruxTypeId: ''
})

const dataList = ref([])
const pageIndex = ref(1)
const pageSize = ref(10)
const totalCount = ref(0)
const dataListLoading = ref(false)
const addOrUpdateVisible = ref(false)
const batchAddVisible = ref(false)
const tradeIdArr = ref([])

const addOrUpdate = ref(null)
const batchAddRef = ref(null)

const route = useRoute()

function getCruxTypeTreeList() {
  api.cruxtype
      .listTree()
      .then((res) => {
        if (res?.code === 0) {
          cruxTypeTreeList.value = res.data
        }
      })
      .catch(() => {
      })
}

// 选中行业回调
function tradeChange(value) {
  if (value && value.length > 0) {
    dataForm.tradeId = value[value.length - 1]
  } else {
    dataForm.tradeId = ''
  }
}

function search() {
  pageIndex.value = 1
  getDataList()
}

// 获取行业列表树形
function getTradeTreeList() {
  tradeTreeList.value = []
  api.trade
      .listTree({})
      .then((res) => {
        if (res && res.code === 0) {
          tradeTreeList.value = res.data
        }
      })
      .catch(() => {
      })
}

// 获取行业列表
function getTradeList() {
  tradeList.value = []
  api.trade
      .list({limit: -1})
      .then((res) => {
        if (res.code === 0) {
          tradeList.value = res.data.list
        }
      })
      .catch(() => {
      })
}

// 获取平台列表
function getPlatformList() {
  platformList.value = []
  api.dictdata
      .list({limit: -1, typeLogo: 'words_platform_type'})
      .then((res) => {
        if (res.code === 0) {
          platformList.value = res.data.list
        }
      })
      .catch(() => {
      })
}

// 获取来源类型列表
function getResourceList() {
  resourceList.value = []
  api.dictdata
      .list({limit: -1, typeLogo: 'words_resource_type'})
      .then((res) => {
        if (res.code === 0) {
          resourceList.value = res.data.list
        }
      })
      .catch(() => {
      })
}

// 获取关键词类型列表
function getTypeList() {
  typeList.value = []
  api.dictdata
      .list({limit: -1, typeLogo: 'crux_words_type'})
      .then((res) => {
        if (res.code === 0) {
          typeList.value = res.data.list
        }
      })
      .catch(() => {
      })
}

// 获取数据列表
function getDataList() {
  dataListLoading.value = true
  const params = {
    ...dataForm,
    page: pageIndex.value,
    limit: pageSize.value
  }
  api.sensitivewords
      .list(params)
      .then((res) => {
        if (res && res.code === 0) {
          dataList.value = res.data.list
          totalCount.value = res.data.totalCount
        } else {
          dataList.value = []
          totalCount.value = 0
        }
      })
      .catch(() => {
        dataList.value = []
        totalCount.value = 0
      })
      .finally(() => {
        dataListLoading.value = false
      })
}

// 每页数
function sizeChangeHandle(val) {
  pageSize.value = val
  pageIndex.value = 1
  getDataList()
}

// 当前页
function currentChangeHandle(val) {
  pageIndex.value = val
  getDataList()
}

// 新增 / 修改
async function addOrUpdateHandle(id) {
  addOrUpdateVisible.value = true
  await nextTick()
  addOrUpdate.value?.init(id, tradeIdArr.value, 1)
}

// 批量新增
async function openBatchAdd() {
  batchAddVisible.value = true
  await nextTick()
  batchAddRef.value?.init(tradeIdArr.value)
}

// 删除
async function deleteHandle(id) {
  await ElMessageBox.confirm('确定要进行删除吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  const res = await api.sensitivewords.delete({id})
  if (res && res.code === 0) {
    getDataList()
    ElMessage({
      message: res.msg,
      type: 'success'
    })
  }
}

onMounted(() => {
  dataForm.tradeId = ''
  tradeIdArr.value = []
  const query = route.query
  if (query && query.tradeArr) {
    const arr = Array.isArray(query.tradeArr)
        ? query.tradeArr
        : [query.tradeArr]
    dataForm.tradeId = arr[arr.length - 1]
    tradeIdArr.value = arr
  }
  getCruxTypeTreeList()
  getTradeList()
  getTradeTreeList()
  getPlatformList()
  getResourceList()
  getTypeList()
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
</style>
