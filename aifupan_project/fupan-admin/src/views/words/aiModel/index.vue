<template>
  <div class="mod-config">
    <el-form :inline="true" :model="dataForm">
      <el-form-item prop="resourceType">
        <el-select
          v-model="dataForm.resourceType"
          placeholder="请选择模型类型"
          style="width: 150px"
        >
          <el-option
            v-for="item in resourceList"
            :key="item.id"
            :label="item.label"
            :value="item.value"
          >
          </el-option>
        </el-select>
      </el-form-item>

      <el-form-item>
        <el-input
          v-model="dataForm.keyword"
          clearable
          placeholder="输入模型名称筛选"
          style="width: 190px"
        ></el-input>
      </el-form-item>

      <el-form-item>
        <el-input
          v-model="dataForm.contextSize"
          clearable
          placeholder="输入模型上下文缓存"
          style="width: 190px"
        ></el-input>
      </el-form-item>

      <el-form-item>
        <div style="display: flex">
          <el-button icon="Search" plain type="primary" @click="search"
            >查询</el-button
          >
          <el-button type="primary" @click="addOrUpdateHandle()"
            >新增
          </el-button>
          <!--          <el-upload :action="uploadUrl" :headers="uploadHeader" :file-list="fileList" :limit="99"-->
          <!--            style="margin-left: 10px;" accept=".xlsx" :show-file-list="false">-->
          <!--&lt;!&ndash;            <el-button type="primary">导入excel</el-button>&ndash;&gt;-->
          <!--          </el-upload>-->
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
        :width="100"
        align="center"
        header-align="center"
        label="厂商类型"
        prop="resourceType"
      >
        <template #default="scope">
          <span v-if="resourceList && resourceList.length > 0">{{
            resourceList.filter((item) => item.id == scope.row.resourceType)
              .length > 0
              ? resourceList.filter(
                  (item) => item.id == scope.row.resourceType
                )[0].name
              : ''
          }}</span>
        </template>
      </el-table-column>

      <el-table-column
        align="center"
        label="模型名称"
        prop="modelName"
        show-overflow-tooltip
        width="210px"
      >
      </el-table-column>

      <el-table-column
        align="center"
        label="模型id"
        prop="endpointId"
        show-overflow-tooltip
        width="210px"
      >
      </el-table-column>

      <el-table-column
        align="center"
        label="apiKey"
        prop="apiKey"
        show-overflow-tooltip
        width="310px"
      >
      </el-table-column>

      <el-table-column
        align="center"
        label="限制使用字数数量"
        prop="wordsNum"
        show-overflow-tooltip
        :width="140"
      >
      </el-table-column>

      <el-table-column
        align="center"
        label="输入数据流大小"
        prop="inputSize"
        show-overflow-tooltip
        :width="130"
      >
      </el-table-column>

      <el-table-column
        align="center"
        label="输出数据流大小"
        prop="outSize"
        show-overflow-tooltip
        :width="130"
      >
      </el-table-column>

      <el-table-column
        align="center"
        label="缓存上下文大小"
        prop="contextSize"
        show-overflow-tooltip
        :width="140"
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
        label="描述"
        prop="remarks"
        :min-width="200"
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
import { customSvg } from '@/utils/icon.js'
import { useUserInfoStore } from '@/store'
import api from '@/utils/request-api'
import AddOrUpdate from './aiModel-add-or-update.vue'

const store = useUserInfoStore()

const resourceList = ref([
  //厂商类型
  {
    id: 0,
    value: 0,
    label: '豆包',
    name: '豆包',
  },
  {
    id: 1,
    value: 1,
    label: '通义',
    name: '通义',
  },
])

const dataForm = reactive({
  keyword: '',
  contextSize: '',
  sourceType: '',
})

const dataList = ref([])
const pageIndex = ref(1)
const pageSize = ref(10)
const totalCount = ref(0)
const dataListLoading = ref(false)
const addOrUpdateVisible = ref(false)
const addOrUpdate = ref(null)

// 上传的请求头
const uploadHeader = computed(() => {
  return {
    token: store.token,
  }
})

// 选中行业回调
const resourceChange = (value) => {
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

// 获取数据列表
const getDataList = async () => {
  try {
    dataListLoading.value = true
    dataForm.page = pageIndex.value
    dataForm.limit = pageSize.value

    const res = await api.aiModel.list(dataForm)
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
  const res = await api.aiModel.delete({ id })
  if (res && res.code === 0) {
    ElMessage({
      message: res.msg,
      type: 'success',
    })
    getDataList()
  }
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
</style>
