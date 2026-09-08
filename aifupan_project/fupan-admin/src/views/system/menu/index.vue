<template>
  <div class="mod-config">
    <el-form :inline="true" :model="dataForm">
      <el-form-item>
        <el-button :icon="Plus" type="primary" @click="addOrUpdateHandle(0, 0)"
        >新增一级菜单
        </el-button>
      </el-form-item>
    </el-form>
    <el-table
        v-loading="loadingFlag"
        :data="dataList"
        :element-loading-spinner="customSvg"
        border
        header-row-class-name="my-header-row"
        row-key="id"
        size="default"
        stripe
        style="width: 100%"
    >
      <el-table-column
          align="left"
          header-align="center"
          label="菜单名"
          min-width="200px"
          prop="name"
      >
      </el-table-column>
      <el-table-column
          align="center"
          header-align="center"
          label="菜单url"
          min-width="250px"
          prop="url"
      >
      </el-table-column>
      <el-table-column
          align="center"
          header-align="center"
          label="类型"
          prop="type"
      >
        <template #default="{ row }">
          <span>{{ ['📜菜单', '📌功能', '📁目录'][row.type] }}</span>
        </template>
      </el-table-column>
      <el-table-column
          align="center"
          header-align="center"
          label="排序"
          prop="sort"
      >
        <template #default="{ row }">
          <span v-if="row.type === 1">-</span>
          <span v-else>{{ row.sort }}</span>
        </template>
      </el-table-column>
      <el-table-column
          align="center"
          header-align="center"
          label="图标"
          prop="img"
      >
        <template #default="{ row }">
          <div class="icon-container">
            <SvgIcon v-if="row.img" :name="row.img"></SvgIcon>
            <span v-else>-</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column
          :width="200"
          fixed="right"
          header-align="center"
          label="操作"
      >
        <template #default="{ row }">
          <el-button
              v-if="!(row.type === 1)"
              class="btn"
              size="small"
              type="primary"
              @click="addOrUpdateHandle(0, row.id)"
          >添加子菜单
          </el-button>
          <el-button
              class="btn"
              size="small"
              type="primary"
              @click="addOrUpdateHandle(row.id, row.parentId)"
          >修改
          </el-button>
          <el-button
              v-if="row.children.length < 1"
              class="btn"
              size="small"
              type="danger"
              @click="deleteHandle(row.id)"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <!-- 弹窗, 新增 / 修改 -->
    <add-or-update
        v-if="addOrUpdateVisible"
        ref="addOrUpdate"
        @refreshDataList="getDataList"
    ></add-or-update>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import AddOrUpdate from './menu-add-or-update.vue'
import api from '@/utils/request-api'
import { customSvg } from '@/utils/icon.js'

const loadingFlag = ref(false)
const dataForm = ref({
  keyword: ''
})
const dataList = ref([])
const addOrUpdateVisible = ref(false)
const addOrUpdate = ref(null)

const getDataList = () => {
  loadingFlag.value = true
  api.menu.listTreeSelf({}).then((res) => {
    if (res && res.code === 0) {
      dataList.value = res.data.menuTreeList
    } else {
      dataList.value = []
    }
    loadingFlag.value = false
  })
}

const addOrUpdateHandle = (id, parentId) => {
  addOrUpdateVisible.value = true
  nextTick(() => {
    addOrUpdate.value.init(id, parentId)
  })
}

const deleteHandle = (id) => {
  ElMessageBox.confirm('确定要进行删除吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    api.menu
        .delete({
          id
        })
        .then((res) => {
          if (res && res.code === 0) {
            getDataList()
            // ElMessage({
            //   message: '删除成功，需重新登录以生效。',
            //   type: 'success'
            // })
            ElNotification({
              title: '操作成功',
              type: 'warning',
              message: '删除已完成，请重新登录以使更改生效。',
              duration: 0
            })
          }
        })
  })
}

onMounted(() => {
  getDataList()
})
</script>
<style lang="scss" scoped>
.mod-config {
  padding: 15px;
}

.icon-container {
  display: flex;
  justify-content: center;

}

:deep(.el-table__fixed-right) {
  height: 100% !important;
}

.btn {
  padding: 0 5px;
}
</style>
