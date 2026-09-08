<template>
  <div class="mod-config">
    <el-form :inline="true" :model="dataForm">
      <el-form-item>
        <el-input
            v-model="dataForm.keyword"
            clearable
            placeholder="输入模型名称搜索"
        ></el-input>
      </el-form-item>
      <el-form-item>
        <el-button icon="Search" plain type="primary" @click="search">查询</el-button>
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
      <el-table-column type="expand">
        <template #default="{ row }">
          <div style="padding: 10px">
            <el-form
                v-for="(item, index) in row.cruxTypeScaleBos"
                :key="item.cruxTypeId || index"
                class="demo-table-expand"
                inline
            >
              <el-form-item>
                <span>{{ item.cruxTypeName }}</span>
              </el-form-item>
              <el-form-item>
                <span>{{ Math.round(item.scale * 100) }}%</span>
              </el-form-item>
            </el-form>
          </div>
        </template>
      </el-table-column>

      <el-table-column
          align="center"
          header-align="center"
          label="模型名称"
          min-width="120px"
          prop="name"
      >
      </el-table-column>
      <el-table-column
          align="center"
          header-align="center"
          label="类型"
          min-width="100px"
          prop="type"
      >
        <template #default="{ row }">
          <span>
            {{ row.type === 0 ? '通用模型' : '行业模型' }}
          </span>
        </template>
      </el-table-column>
      <el-table-column
          align="center"
          header-align="center"
          label="行业"
          prop="tradeId"
      >
        <template #default="{ row }">
          <span>
            {{ row.tradeId === '0' ? '无行业' : row.tradeName }}
          </span>
        </template>
      </el-table-column>
      <el-table-column
          align="center"
          header-align="center"
          label="创建时间"
          prop="createDate"
          width="180px"
      >
      </el-table-column>
      <el-table-column
          align="center"
          fixed="right"
          header-align="center"
          label="操作"
          width="150"
      >
        <template #default="{ row }">
          <el-button
              size="small"
              type="primary"
              @click="addOrUpdateHandle(row.id)"
          >修改
          </el-button>
          <el-button size="small" type="danger" @click="deleteHandle(row.id)"
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
    <SaveModelDialog
        v-if="saveModelDialogVisible"
        ref="saveModelD"
        @refreshDataList="getDataList"
    />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import api from '@/utils/request-api'
import SaveModelDialog from './saveModelDialog.vue'
import { customSvg } from '@/utils/icon.js'

const dataForm = reactive({
  keyword: '',
  type: 0
})

const dataList = ref([])
const pageIndex = ref(1)
const pageSize = ref(10)
const totalCount = ref(0)
const dataListLoading = ref(false)
const saveModelDialogVisible = ref(false)
const tradeName = ref('')

const saveModelD = ref(null)

function search() {
  pageIndex.value = 1
  dataList.value = []
  getDataList()
}

function getDataList() {
  dataListLoading.value = true
  const params = {
    ...dataForm,
    page: pageIndex.value,
    limit: pageSize.value
  }
  api.dataModel
      .modelCruxTypeList(params)
      .then((res) => {
        if (res && res.code === 0) {
          dataList.value = (res.data?.list || []).map((it) => ({
            ...it
            // 展开项百分比展示在模板中已处理为 Math.round(item.scale * 100)
          }))
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
  console.log('id', id)
  saveModelDialogVisible.value = true
  await nextTick()
  saveModelD.value?.init(id)
}

async function deleteHandle(id) {
  await ElMessageBox.confirm('确定要进行删除吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  const res = await api.dataModel.deleteDataModel({id: id})
  if (res && res.code === 0) {
    getDataList()
    ElMessage({
      message: res.msg,
      type: 'success'
    })
  }
}

onMounted(() => {
  getDataList()
})
</script>
<style scoped>
.mod-config {
  padding: 15px;
}

:deep(.el-table__fixed-right) {
  height: 100% !important;
}

/* 如果使用 :deep() */
:deep(.el-table__expanded-cell) {
  padding: 0;
}

.demo-table-expand {
  font-size: 0;
}

.demo-table-expand .el-form-item {
  margin-right: 0;
  margin-bottom: 0;
  width: 120px;
  border: 1px solid #ccc;
}

.demo-table-expand .el-form-item span {
  margin-left: 10px;
}
</style>
