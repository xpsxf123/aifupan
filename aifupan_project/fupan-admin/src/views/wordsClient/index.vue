<template>
  <div class="mod-config">
    <el-form :inline="true" :model="dataForm">
      <el-form-item>
        <el-input
            v-model="dataForm.keyword"
            clearable
            placeholder="输入敏感词搜索"
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
        <div style="display: flex">
          <el-button icon="Search" plain type="primary" @click="search">查询</el-button>
          <!-- <el-button type="primary" @click="addOrUpdateHandle()">新增</el-button>
          <el-button type="primary" @click="batchAdd()">批量新增</el-button>
          <el-upload :action="uploadUrl" :headers="uploadHeader" :file-list="fileList" :limit="99"
            style="margin-left: 10px;" accept=".xlsx" :show-file-list="false">
            <el-button type="primary">导入excel</el-button>
          </el-upload> -->
        </div>
      </el-form-item>
    </el-form>
    <!-- size 从 medium 改为 default 以适配 Element Plus -->
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
          label="词语"
          prop="name"
          show-overflow-tooltip
      >
      </el-table-column>
      <el-table-column
          align="center"
          header-align="center"
          label="词语类型"
          min-width="120px"
          prop="wordsType"
          show-overflow-tooltip
      >
        <template #default="{ row }">
          <span>{{ ['敏感词', '关键词', '敏感词白名单'][row.wordsType] }}</span>
        </template>
      </el-table-column>
      <el-table-column
          align="center"
          header-align="center"
          label="平台类型"
          min-width="120px"
          prop="platformType"
      >
        <template #default="{ row }">
          <span>{{
              (platformList.find((item) => item.value == row.platformType) || {})
                  .label || ''
            }}</span>
        </template>
      </el-table-column>
      <el-table-column
          align="center"
          header-align="center"
          label="行业"
          min-width="160px"
          prop="tradeId"
          show-overflow-tooltip
      >
        <template #default="{ row }">
          <span v-if="tradeList && tradeList.length > 0">{{
              (tradeList.find((item) => item.id == row.tradeId) || {}).name || ''
            }}</span>
        </template>
      </el-table-column>
      <el-table-column
          align="center"
          header-align="center"
          label="添加人"
          prop="userNickName"
      >
      </el-table-column>
      <el-table-column
          align="center"
          header-align="center"
          label="添加人手机号"
          prop="userPhone"
          width="120px"
      >
      </el-table-column>
      <el-table-column
          align="center"
          header-align="center"
          label="创建时间"
          prop="createDate"
          width="120px"
      >
        <template #default="{ row }">
          <span>{{
              row.createDate ? row.createDate.substring(0, 10) : ''
            }}</span>
        </template>
      </el-table-column>
      <el-table-column
          align="center"
          header-align="center"
          label="最后修改时间"
          prop="updateDate"
          width="120px"
      >
        <template #default="{ row }">
          <span>{{
              row.updateDate ? row.updateDate.substring(0, 10) : ''
            }}</span>
        </template>
      </el-table-column>
      <!-- <el-table-column fixed="right" header-align="center" align="center" width="150" label="操作">
        <template slot-scope="scope">
          <el-button type="primary" size="mini" @click="addOrUpdateHandle(scope.row.id)">修改</el-button>
          <el-button type="danger" size="mini" @click="deleteHandle(scope.row.id)">删除</el-button>
        </template>
      </el-table-column> -->
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
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import api from '@/utils/request-api'
import { customSvg } from '@/utils/icon.js'

const tradeTreeList = ref([])
const tradeList = ref([])
const platformList = ref([])
const resourceList = ref([])
const typeList = ref([])
const LevelList = ref([])

const dataForm = reactive({
  keyword: '',
  resourceType: 1,
  type: '',
  platformType: '',
  level: '',
  tradeId: '',
  groupStr: '',
  wordsType: '',
  wordLength: ''
})

const dataList = ref([])
const pageIndex = ref(1)
const pageSize = ref(10)
const totalCount = ref(0)
const dataListLoading = ref(false)
const dataListSelections = ref([])
const addOrUpdateVisible = ref(false)
const batchAddVisible = ref(false)
const tradeIdArr = ref([])

const route = useRoute()

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

function getTradeList() {
  tradeList.value = []
  api.trade
      .list({limit: -1})
      .then((res) => {
        if (res.code == 0) {
          tradeList.value = res.data.list
        }
      })
      .catch(() => {
      })
}

function getPlatformList() {
  platformList.value = []
  api.dictdata
      .list({limit: -1, typeLogo: 'words_platform_type'})
      .then((res) => {
        if (res.code == 0) {
          platformList.value = res.data.list
        }
      })
      .catch(() => {
      })
}

function getResourceList() {
  resourceList.value = []
  api.dictdata
      .list({limit: -1, typeLogo: 'words_resource_type'})
      .then((res) => {
        if (res.code == 0) {
          resourceList.value = res.data.list
        }
      })
      .catch(() => {
      })
}

function getTypeList() {
  typeList.value = []
  api.dictdata
      .list({limit: -1, typeLogo: 'sensitive_words_type'})
      .then((res) => {
        if (res.code == 0) {
          typeList.value = res.data.list
        }
      })
      .catch(() => {
      })
}

function getLevelList() {
  LevelList.value = []
  api.dictdata
      .list({limit: -1, typeLogo: 'sensitive_words_level_type'})
      .then((res) => {
        if (res.code == 0) {
          LevelList.value = res.data.list
        }
      })
      .catch(() => {
      })
}

function getDataList() {
  dataListLoading.value = true
  const params = {
    ...dataForm,
    page: pageIndex.value,
    limit: pageSize.value
  }
  api.sensitivewordsClient
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

function sizeChangeHandle(val) {
  pageSize.value = val
  pageIndex.value = 1
  getDataList()
}

function currentChangeHandle(val) {
  pageIndex.value = val
  getDataList()
}

async function addOrUpdateHandle(id) {
  addOrUpdateVisible.value = true
  await nextTick()
  // 组件未在当前模板中使用，此处保留调用注释
  // addOrUpdateRef.value?.init(id, tradeIdArr.value, 0)
}

async function batchAdd() {
  batchAddVisible.value = true
  await nextTick()
  // 组件未在当前模板中使用，此处保留调用注释
  // batchAddRef.value?.init(tradeIdArr.value)
}

async function deleteHandle(id) {
  try {
    await ElMessageBox.confirm('确定要进行删除吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    const res = await api.sensitivewords.delete({id})
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
      ElMessage.error(res?.msg || '删除失败')
    }
  } catch (err) {
    // 用户取消或请求异常时无需处理
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

  getTradeList()
  getTradeTreeList()
  getPlatformList()
  getResourceList()
  getTypeList()
  getLevelList()
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
