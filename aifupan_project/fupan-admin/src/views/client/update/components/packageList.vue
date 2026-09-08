<template>
  <my-dialog v-model="dialogVisible" :footer="false" :title="title" :width="width" @close="close" @submit="submit">
    <el-button size="small" style="margin-bottom: 10px;" type="primary" @click="openPackageEdit({ parentId })">添加
    </el-button>
    <el-table
        :data="dataList"
        :element-loading-spinner="customSvg"
        border
        header-row-class-name="my-header-row"
        size="default"
        stripe
        style="width: 100%"
    >
      <el-table-column align="center" label="序" type="index" width="55"></el-table-column>
      <el-table-column align="center" header-align="center" label="版本号" prop="versionNum"></el-table-column>
      <el-table-column align="center" header-align="center" label="版本值" prop="version"></el-table-column>
      <el-table-column align="center" header-align="center" label="添加时间" prop="updateTime"></el-table-column>
      <el-table-column align="center" fixed="right" header-align="center" label="操作" width="250">
        <template #default="{ row }">
          <el-button type="text" @click="openPackageEdit(row)">修改</el-button>
          <el-button style="color: red;" type="text" @click="openDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-model:limit="form.limit" v-model:page="form.page" :total="totalCount" @change="getList"></pagination>

    <packageEdit v-if="showEdit" ref="packageEditRef" @close="showEdit = false"
                 @is-ok="() => { showEdit = false; getList() }"></packageEdit>
  </my-dialog>
</template>

<script setup>
import { ref, reactive, nextTick } from 'vue'
import myDialog from '@/components/commonComponent/myDialog.vue'
import pagination from '@/components/commonComponent/pagination.vue'
import packageEdit from '@/views/client/update/components/packageEdit.vue'
import api from '@/utils/request-api'
import { customSvg } from '@/utils/icon.js'

const dialogVisible = ref(false)
const title = ref('补丁包')
const width = ref('900px')
const dataList = ref([])
const form = reactive({limit: 10, page: 1})
const totalCount = ref(0)
const parentId = ref(null)
const showEdit = ref(false)
const packageEditRef = ref()

const emit = defineEmits(['is-ok', 'close'])

const init = (row) => {
  dialogVisible.value = true
  parentId.value = row.id
  getList()
}

const getList = async () => {
  form.parentId = parentId.value
  const res = await api.clientupdate.list(form)
  if (res?.code === 0) {
    dataList.value = res.data.list
    totalCount.value = res.data.totalCount
  }
}

const openDelete = (row) => {
  ElMessageBox.confirm('确定要删除选中的记录?', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    const res = await api.clientupdate.delete([row.id])
    if (res && res.code === 0) {
      form.page = 1
      getList()
    }
  })
}

const openPackageEdit = (row) => {
  showEdit.value = true
  nextTick(() => {
    packageEditRef.value && packageEditRef.value.init(row)
  })
}

const submit = () => {
  emit('is-ok')
}

const close = () => {
  dialogVisible.value = false
  emit('close')
}

defineExpose({init})
</script>

<style lang="less" scoped>

</style>