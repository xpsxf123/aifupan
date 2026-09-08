<template>
  <div>
    <el-table
        :data="dataList"
        :element-loading-spinner="customSvg"
        border
        header-row-class-name="my-header-row"
        size="default"
        stripe
        style="width: 100%"
    >
      <el-table-column
          :key="'nickName'"
          align="center"
          header-align="center"
          label="昵称"
          min-width="100"
          prop="nickName"
          show-overflow-tooltip
      />
      <el-table-column
          :key="'phone'"
          align="center"
          header-align="center"
          label="手机号"
          min-width="110"
          prop="phone"
          show-overflow-tooltip
      />
      <el-table-column
          :key="'userType'"
          align="center"
          header-align="center"
          label="账号类型"
          min-width="100"
          prop="userType"
          show-overflow-tooltip
      >
        <template #default="{ row }">
          {{ getLabel(userDict.userType, row.userType) }}
        </template>
      </el-table-column>
      <el-table-column
          :key="'status'"
          align="center"
          header-align="center"
          label="状态"
          min-width="150"
          prop="status"
          show-overflow-tooltip
      >
        <template #default="{ row }">
          <span>{{ ['正常', '冻结'][row.status] }}</span>
        </template>
      </el-table-column>
      <el-table-column
          :key="'createDate'"
          align="center"
          header-align="center"
          label="注册时间"
          min-width="160"
          prop="createDate"
          show-overflow-tooltip
      />
      <el-table-column
          :key="'actions'"
          align="center"
          fixed="right"
          header-align="center"
          label="操作"
          width="120"
      >
        <template #default="{ row }">
          <el-button
              link
              size="small"
              type="primary"
              @click="goToDetail(row.id)"
          >详情
          </el-button
          >
          <el-button link size="small" type="primary" @click="unbinding(row)"
          >解绑
          </el-button
          >
        </template>
      </el-table-column>
    </el-table>
    <pagination
        v-model:limit="form.limit"
        v-model:page="form.page"
        :total="totalCount"
        background
        layout="total, sizes, prev, pager,next,->, jumper"
        @change="getList"
    />
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'
import { customSvg } from '@/utils/icon.js'
import { useDict } from '@/hooks/useDict'
import { useUserInfoStore } from '@/store'
import api from '@/utils/request-api'
import pagination from '@/components/commonComponent/pagination.vue'

const props = defineProps({
  userId: {
    type: String,
    default: ''
  }
})

const router = useRouter()
const userStore = useUserInfoStore()
const {loginResultData} = storeToRefs(userStore)
const {userDict, getLabel} = useDict()

const dataList = ref([])
const totalCount = ref(0)
const form = ref({
  page: 1,
  limit: 10
})

const getList = async () => {
  if (props.userId) {
    try {
      const res = await api.user.subAccountList({
        parentId: props.userId,
        ...form.value
      })
      if (res?.code == 0) {
        dataList.value = res.data.list
        totalCount.value = res.data.totalCount
      }
    } catch (error) {
      console.error('获取子账户列表失败:', error)
    }
  }
}

const unbinding = async (row) => {
  try {
    await ElMessageBox.confirm('确定要解绑吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    const logoUser = loginResultData.value
    const unbindReason = `后台管理员解绑，nickName：${logoUser?.nickName},userName: ${logoUser.username}, id=${logoUser.id}`
    const data = {
      currentUserId: props.userId,
      subUserId: row.id,
      unbindReason
    }

    const res = await api.openapi.unbindingSubAccount(data)
    if (res?.code == 0) {
      ElMessage.success('解绑成功')
      getList()
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('解绑失败:', error)
    }
  }
}

const goToDetail = (userId) => {
  const resolved = router.resolve({
    query: {
      userId,
      componentName: 'userDetail'
    }
  })
  window.open(window.location.origin + resolved.href, '_blank')
}

watch(
    () => props.userId,
    () => {
      getList()
    }
)

onMounted(() => {
  getList()
})
</script>

<style lang="less" scoped></style>
