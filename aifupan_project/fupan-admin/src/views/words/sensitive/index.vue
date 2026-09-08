<template>
  <div class="mod-config">
    <el-form :inline="true" :model="dataForm">
      <el-form-item>
        <el-input
            v-model="dataForm.keyword"
            clearable
            placeholder="输入敏感词筛选"
            style="width: 150px"
        ></el-input>
      </el-form-item>
      <el-form-item>
        <el-input
            v-model="dataForm.groupStr"
            clearable
            placeholder="输入分组筛选"
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
        <el-select
            v-model="dataForm.level"
            clearable
            placeholder="按敏感词等级筛选"
            style="width: 180px"
        >
          <el-option
              v-for="item in LevelList"
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
            v-model="dataForm.type"
            clearable
            placeholder="按敏感词类型筛选"
            style="width: 180px"
        >
          <el-option
              v-for="item in typeList"
              :key="item.id"
              :label="item.label"
              :value="item.value"
          >
          </el-option>
        </el-select>
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
        <div style="display: flex">
          <el-button icon="Search" plain type="primary" @click="search">查询</el-button>
          <el-button type="primary" @click="addOrUpdateHandle()"
          >新增
          </el-button>
          <el-button type="primary" @click="batchAddHandle()"
          >批量新增
          </el-button>
          <el-upload
              :action="uploadUrl"
              :file-list="fileList"
              :headers="uploadHeader"
              :limit="99"
              :show-file-list="false"
              accept=".xlsx"
              style="margin-left: 10px"
          >
            <el-button type="primary">导入excel</el-button>
          </el-upload>
        </div>
      </el-form-item>
    </el-form>
    <el-table
        v-loading="loadingFlag"
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
                v-for="similarItem in props.row.similarWordList"
                :key="similarItem.id || similarItem.name"
                style="padding: 4px 16px"
            >
              <span
                  :style="
                  similarItem.isMark && similarItem.isMark == 1
                    ? 'background:pink'
                    : ''
                "
              >{{ '相似词：' + similarItem.name }}</span
              >
              <span v-if="similarItem.remarks">{{
                  '，描述：' + similarItem.remarks
                }}</span>
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
          min-width="180px"
          prop="groupStr"
          show-overflow-tooltip
      >
      </el-table-column>
      <el-table-column
          align="center"
          header-align="center"
          label="敏感词"
          prop="name"
          show-overflow-tooltip
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
          label="敏感词类型"
          min-width="100px"
          prop="type"
          show-overflow-tooltip
      >
        <template #default="scope">
          <span>{{
              typeList.filter((item) => item.value == scope.row.type)[0]?.label ||
              ''
            }}</span>
        </template>
      </el-table-column>
      <el-table-column
          align="center"
          header-align="center"
          label="平台类型"
          min-width="100px"
          prop="platformType"
      >
        <template #default="scope">
          <span>{{
              platformList.filter(
                  (item) => item.value == scope.row.platformType
              )[0]?.label || ''
            }}</span>
        </template>
      </el-table-column>
      <el-table-column
          align="center"
          header-align="center"
          label="敏感词等级"
          min-width="150px"
          prop="level"
          show-overflow-tooltip
      >
        <template #default="scope">
          <span>{{
              LevelList.filter((item) => item.value == scope.row.level)[0]
                  ?.label || ''
            }}</span>
        </template>
      </el-table-column>
      <el-table-column
          align="center"
          header-align="center"
          label="行业"
          min-width="180px"
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
          label="概览"
          min-width="120px"
          prop="overView"
          show-overflow-tooltip
      >
      </el-table-column>
      <el-table-column
          align="center"
          header-align="center"
          label="描述"
          min-width="180px"
          prop="remarks"
          show-overflow-tooltip
      >
      </el-table-column>
      <el-table-column
          align="center"
          header-align="center"
          label="创建时间"
          prop="createDate"
          width="130px"
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
          width="130px"
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
          </el-button>
          <el-button
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
        ref="addOrUpdate"
        @refreshDataList="getDataList"
    ></add-or-update>
    <!-- 弹窗, 批量添加 -->
    <batch-add
        v-if="batchAddVisible"
        ref="batchAdd"
        @refreshDataList="getDataList"
    ></batch-add>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { useUserInfoStore } from '@/store'
import api from '@/utils/request-api'
import AddOrUpdate from './sensitivewords-add-or-update.vue'
import BatchAdd from './sensitivewords-batch-add.vue'
import { customSvg } from '@/utils/icon.js'

const loadingFlag = ref(false)
const route = useRoute()
const userInfoStore = useUserInfoStore()
const fileList = ref([])
const tradeTreeList = ref([])
const tradeList = ref([])
const platformList = ref([])
const resourceList = ref([])
const typeList = ref([])
const LevelList = ref([])
const dataForm = reactive({
  keyword: '',
  resourceType: 0,
  type: '',
  platformType: '',
  level: '',
  tradeId: '',
  groupStr: '',
  wordsType: 0,
  wordLength: ''
})
const dataList = ref([])
const pageIndex = ref(1)
const pageSize = ref(10)
const totalCount = ref(0)
const addOrUpdateVisible = ref(false)
const batchAddVisible = ref(false)
const tradeIdArr = ref([])

const addOrUpdate = ref(null)
const batchAdd = ref(null)

const uploadUrl = computed(() => {
  return api.common.uploadWordExcel
})

const uploadHeader = computed(() => {
  return {
    token: userInfoStore.token
  }
})

onMounted(() => {
  dataForm.tradeId = ''
  tradeIdArr.value = []
  if (route.query.tradeArr) {
    dataForm.tradeId = route.query.tradeArr[route.query.tradeArr.length - 1]
    tradeIdArr.value = route.query.tradeArr
  }

  getTradeList()
  getTradeTreeList()
  getPlatformList()
  getResourceList()
  getTypeList()
  getLevelList()
  getDataList()
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

const getTradeTreeList = () => {
  tradeTreeList.value = []
  api.trade.listTree({}).then((res) => {
    if (res && res.code === 0) {
      tradeTreeList.value = res.data
    }
  })
}

const getTradeList = () => {
  tradeList.value = []
  api.trade.list({limit: -1}).then((res) => {
    if (res.code == 0) {
      tradeList.value = res.data.list
    }
  })
}

const getPlatformList = () => {
  platformList.value = []
  api.dictdata
      .list({limit: -1, typeLogo: 'words_platform_type'})
      .then((res) => {
        if (res.code == 0) {
          platformList.value = res.data.list
        }
      })
}

const getResourceList = () => {
  resourceList.value = []
  api.dictdata
      .list({limit: -1, typeLogo: 'words_resource_type'})
      .then((res) => {
        if (res.code == 0) {
          resourceList.value = res.data.list
        }
      })
}

const getTypeList = () => {
  typeList.value = []
  api.dictdata
      .list({limit: -1, typeLogo: 'sensitive_words_type'})
      .then((res) => {
        if (res.code == 0) {
          typeList.value = res.data.list
        }
      })
}

const getLevelList = () => {
  LevelList.value = []
  api.dictdata
      .list({limit: -1, typeLogo: 'sensitive_words_level_type'})
      .then((res) => {
        if (res.code == 0) {
          LevelList.value = res.data.list
        }
      })
}

const getDataList = () => {
  dataForm.page = pageIndex.value
  dataForm.limit = pageSize.value
  loadingFlag.value = true
  api.sensitivewords.list(dataForm).then((res) => {
    if (res && res.code === 0) {
      dataList.value = res.data.list
      totalCount.value = res.data.totalCount
    } else {
      dataList.value = []
      totalCount.value = 0
    }
    loadingFlag.value = false
  })
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
    addOrUpdate.value.init(id, tradeIdArr.value, 0)
  })
}

const batchAddHandle = () => {
  batchAddVisible.value = true
  nextTick(() => {
    batchAdd.value.init(tradeIdArr.value)
  })
}

const deleteHandle = (id) => {
  ElMessageBox.confirm('确定要进行删除吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    api.sensitivewords
        .delete({
          id
        })
        .then((res) => {
          if (res && res.code === 0) {
            ElMessage({
              message: res.msg,
              type: 'success'
            })
            getDataList()
          }
        })
  })
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
