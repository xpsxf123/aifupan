<template>
  <div class="mod-config">
    <el-button type="primary" @click="addOrUpdateHandle(0, 0, [], 0)"
    >新增一级关键词分类
    </el-button
    >

    <el-table
        :key="tableKey"
        v-loading="dataListLoading"
        :data="dataList"
        :element-loading-spinner="customSvg"
        :row-style="tableRowStyle"
        border
        default-expand-all
        header-row-class-name="my-header-row"
        row-key="id"
        size="default"
        style="width: 100%; margin-top: 10px"
    >
      <el-table-column
          align="left"
          header-align="center"
          label="分类"
          prop="name"
          width="400"
      >
      </el-table-column>
      <el-table-column
          align="left"
          header-align="center"
          label="描述"
          prop="remarks"
      >
      </el-table-column>
      <el-table-column
          align="center"
          header-align="center"
          label="层级"
          prop="level"
          width="100"
      >
        <template #default="scope">
          {{ scope.row.level + '级' }}
        </template>
      </el-table-column>
      <el-table-column
          align="center"
          header-align="center"
          label="排序"
          prop="sort"
          width="100"
      >
      </el-table-column>
      <el-table-column
          align="center"
          header-align="center"
          label="tab排序"
          prop="tabSort"
          width="100"
      >
      </el-table-column>
      <el-table-column
          align="center"
          header-align="center"
          label="关键词数量"
          prop="cruxNum"
          width="180"
      >
      </el-table-column>
      <el-table-column
          align="center"
          header-align="center"
          label="创建时间"
          prop="createDate"
          width="200"
      >
      </el-table-column>
      <el-table-column
          align="left"
          header-align="center"
          label="操作"
          width="200"
      >
        <template #default="scope">
          <el-button
              v-if="scope.row.level < 4"
              size="small"
              style="color: #409eff"
              type="text"
              @click="
              addOrUpdateHandle(
                0,
                scope.row.id,
                scope.row.parentIdArr,
                scope.row.level
              )
            "
          >添加子分类
          </el-button
          >
          <el-button
              size="small"
              style="color: #409eff"
              type="text"
              @click="
              addOrUpdateHandle(
                scope.row.id,
                scope.row.parentId,
                scope.row.parentIdArr,
                scope.row.level
              )
            "
          >修改
          </el-button
          >
          <el-button
              v-if="scope.row.children.length < 1"
              size="small"
              style="color: rgb(245, 108, 108)"
              type="text"
              @click="deleteHandle(scope.row.id)"
          >删除
          </el-button
          >
        </template>
      </el-table-column>
    </el-table>
    <!-- 弹窗, 新增 / 修改 -->
    <AddOrUpdate
        v-if="addOrUpdateVisible"
        ref="addOrUpdate"
        @refreshDataList="getDataList"
    ></AddOrUpdate>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/utils/request-api'
import AddOrUpdate from './cruxtype-add-or-update.vue'
import { customSvg } from '@/utils/icon.js'

const dataList = ref([])
const dataListLoading = ref(false)
const tableKey = ref(0)
const addOrUpdateVisible = ref(false)
const addOrUpdate = ref(null)
const router = useRouter()

function tableRowStyle({row}) {
  if (row.level == 1) {
    return {backgroundColor: '#FF7744'}
  } else if (row.level == 2) {
    return {backgroundColor: '#33FFFF'}
  } else if (row.level == 3) {
    return {backgroundColor: '#33FFAA'}
  } else {
    return {backgroundColor: '#FFCC22'}
  }
}

function findPathToNode(node, targetId, path = []) {
  if (!node) return null
  if (node.id === targetId) {
    return [...path, node.id]
  }
  if (node.children && node.children.length > 0) {
    for (const child of node.children) {
      const result = findPathToNode(child, targetId, [...path, node.id])
      if (result) return result
    }
  }
  return null
}

function getIdsByLevel3Id(level3Id) {
  for (const root of dataList.value) {
    const ids = findPathToNode(root, level3Id)
    if (ids !== null) {
      return [ids[0], ids[1], ids[2]]
    }
  }
  return null
}

function toWordPage(tradeId, pageUrl) {
  const arr = getIdsByLevel3Id(tradeId)
  const tradeArr = []
  arr?.forEach((item) => {
    if (item) tradeArr.push(item)
  })
  router.push({path: pageUrl, query: {tradeArr}})
}

async function getDataList() {
  dataList.value = []
  dataListLoading.value = true
  try {
    const res = await api.cruxtype.listTree({childrenNotNull: 1})
    if (res && res.code === 0) {
      dataList.value = res.data
      tableKey.value++
    }
  } finally {
    dataListLoading.value = false
  }
}

async function addOrUpdateHandle(id, parentId, parentIdArr, level) {
  addOrUpdateVisible.value = true
  await nextTick()
  const arrCopy = JSON.parse(JSON.stringify(parentIdArr))
  addOrUpdate.value?.init(id, parentId, arrCopy, level)
}

async function deleteHandle(id) {
  try {
    await ElMessageBox.confirm('确定要进行删除吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  const res = await api.cruxtype.delete({id})
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
<style lang="less" scoped>
.mod-config {
  padding: 15px;
}

:deep(.el-table__fixed-right) {
  height: 100% !important;
}

:deep(.el-button--small) {
  padding: 0;
}
</style>
