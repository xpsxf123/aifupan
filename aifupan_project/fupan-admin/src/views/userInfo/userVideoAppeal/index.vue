<template>
  <div class="mod-config">
    <el-tabs v-model="dataForm.status" type="card">
      <el-tab-pane label="全部" name="all"></el-tab-pane>
      <el-tab-pane label="未处理" name="0"></el-tab-pane>
      <el-tab-pane label="已处理" name="1"></el-tab-pane>
    </el-tabs>
    <div>
      <el-form ref="myForm" :inline="true" :model="dataForm">
        <el-form-item prop="nickName">
          <el-input
              v-model="dataForm.nickName"
              clearable
              placeholder="用户昵称"
          ></el-input>
        </el-form-item>
        <el-form-item prop="liveUserName">
          <el-input
              v-model="dataForm.liveUserName"
              clearable
              placeholder="直播间账号昵称"
          ></el-input>
        </el-form-item>
        <el-form-item prop="contacts">
          <el-input
              v-model="dataForm.contacts"
              clearable
              placeholder="联系人"
          ></el-input>
        </el-form-item>
        <el-form-item prop="phone">
          <el-input
              v-model="dataForm.phone"
              clearable
              placeholder="手机号码"
          ></el-input>
        </el-form-item>
        <el-form-item prop="startAndEndCreate">
          <el-date-picker
              v-model="dataForm.startAndEndCreate"
              :default-time="['00:00:00', '23:59:59']"
              end-placeholder="结束创建日期"
              format="YYYY-MM-DD"
              range-separator="至"
              start-placeholder="开始创建日期"
              type="daterange"
              value-format="YYYY-MM-DD HH:mm:ss"
          >
          </el-date-picker>
        </el-form-item>
        <el-form-item>
          <el-button icon="Search" plain type="primary" @click="search"
          >搜索
          </el-button
          >
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
            align="center"
            header-align="center"
            label="序"
            show-overflow-tooltip
            type="index"
            width="50"
        />
        <el-table-column
            align="center"
            header-align="center"
            label="用户昵称"
            min-width="120"
            prop="nickName"
            show-overflow-tooltip
        >
          <template #default="scope">
            <span>{{ scope.row.nickName }}</span>
          </template>
        </el-table-column>
        <el-table-column
            align="center"
            header-align="center"
            label="直播间账号昵称"
            min-width="130"
            prop="liveUserName"
            show-overflow-tooltip
        />
        <el-table-column
            align="center"
            header-align="center"
            label="公司名称"
            min-width="130"
            prop="companyName"
            show-overflow-tooltip
        />
        <el-table-column
            align="center"
            header-align="center"
            label="联系人"
            min-width="110"
            prop="contacts"
            show-overflow-tooltip
        />
        <el-table-column
            align="center"
            header-align="center"
            label="手机号码"
            min-width="150"
            prop="phone"
            show-overflow-tooltip
        />
        <el-table-column
            align="center"
            header-align="center"
            label="主播名称"
            min-width="140"
            prop="anchorUrlName"
            show-overflow-tooltip
        >
          <template #default="scope">
            <span
                style="color: #007aff; cursor: pointer"
                @click="openAnchorUrl(scope.row)"
            >{{ scope.row.anchorUrlName }}</span
            >
          </template>
        </el-table-column>
        <el-table-column
            align="center"
            header-align="center"
            label="视频名称"
            min-width="150"
            prop="anchorVideoName"
            show-overflow-tooltip
        >
          <template #default="scope">
            <span
                style="color: #007aff; cursor: pointer"
                @click="openAnchorVideo(scope.row)"
            >{{ scope.row.anchorVideoName }}</span
            >
          </template>
        </el-table-column>
        <el-table-column
            align="center"
            header-align="center"
            label="申述原因"
            min-width="150"
            prop="appealReason"
            show-overflow-tooltip
        />
        <el-table-column
            align="center"
            header-align="center"
            label="状态"
            min-width="100"
            prop="status"
        >
          <template #default="scope">
            <span v-if="scope.row.status === 0">未处理</span>
            <span v-if="scope.row.status === 1">已处理</span>
          </template>
        </el-table-column>
        <el-table-column
            align="center"
            header-align="center"
            label="创建时间"
            prop="createDate"
            width="180px"
        />
        <el-table-column
            align="center"
            header-align="center"
            label="处理时间"
            prop="handleDate"
            width="180px"
        />
        <el-table-column
            align="center"
            header-align="center"
            label="处理备注"
            min-width="160"
            prop="handleRemarks"
            show-overflow-tooltip
        />
        <el-table-column
            align="center"
            fixed="right"
            header-align="center"
            label="操作"
            prop="handle"
            width="100"
        >
          <template #default="scope">
            <el-button
                :disabled="scope.row.status !== 0"
                type="text"
                @click="handle(scope.row)"
            >处理
            </el-button
            >
            <el-button
                v-if="admin"
                style="color: red"
                type="text"
                @click="handleDelete(scope.row)"
            >删除
            </el-button
            >
          </template>
        </el-table-column>
      </el-table>
      <pagination
          v-show="total > 0"
          v-model:limit="form.limit"
          v-model:page="form.page"
          :total="total"
          background
          layout="total, sizes, prev, pager,next,->, jumper"
          @pagination="getList"
          @size-change="sizeChangeHandle"
          @current-change="currentChangeHandle"
      />
    </div>
    <!--处理申诉-->
    <HandleAppeal
        v-if="showHandleAppeal"
        ref="handleAppeal"
        @close="showHandleAppeal = false"
        @submit="handleSubmit"
    />
    <!--主播-->
    <AnchorUrlInfo
        v-if="anchorInfoDialogVisible"
        ref="anchorUrlInfo"
    ></AnchorUrlInfo>
    <!--视频-->
    <AnchorVideoAnalysis
        v-if="analysisDialogVisible"
        ref="anchorVideoAnalysis"
    ></AnchorVideoAnalysis>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/utils/request-api'
import { customSvg } from '@/utils/icon.js'
import Pagination from '@/components/commonComponent/pagination.vue'
// @ts-ignore
import HandleAppeal from './components/HandleAppeal.vue'
import AnchorUrlInfo from '@/views/userInfo/anchorUrl/anchorUrl/components/anchorUrlInfo.vue'
import AnchorVideoAnalysis from '@/views/userInfo/anchorUrl/transcribe/components/anchorVideoAnalysisDialog.vue'

const pageIndex = ref(1)
const pageSize = ref(10)
const router = useRouter()
const dataListLoading = ref(false)
// 响应式数据
const form = reactive({
  page: 1,
  limit: 10
})

const dataForm = reactive({
  status: 'all',
  nickName: '',
  liveUserName: '',
  companyName: '',
  contacts: '',
  phone: '',
  startAndEndCreate: []
})

const dataList = ref([])
const total = ref(0)
const admin = ref(0)
const showHandleAppeal = ref(false)
const anchorInfoDialogVisible = ref(false)
const analysisDialogVisible = ref(false)

// 组件引用
const myForm = ref(null)
const handleAppeal = ref(null)
const anchorUrlInfo = ref(null)
const anchorVideoAnalysis = ref(null)

// 处理申诉提交
const handleSubmit = () => {
  showHandleAppeal.value = false
  getList()
}

// 重置表单
const reset = () => {
  myForm.value?.resetFields()
  search()
}

// 搜索
const search = () => {
  form.page = 1
  getList()
}

// 处理状态变化
const handleClick = () => {
  reset()
}

// 每页数
function sizeChangeHandle(val) {
  pageSize.value = val
  pageIndex.value = 1
  getList()
}

// 当前页
function currentChangeHandle(val) {
  pageIndex.value = val
  getList()
}

// 获取列表数据
const getList = async () => {
  console.log(form)
  let data = JSON.parse(JSON.stringify({...form, ...dataForm}))
  data.status = data.status === 'all' ? null : data.status
  data.startCreateDate = null
  data.endCreateDate = null
  if (data.startAndEndCreate?.length > 1) {
    data.startCreateDate = data.startAndEndCreate[0]
    data.endCreateDate = data.startAndEndCreate[1]
  }
  dataListLoading.value = true
  const res = await api.userVideoAppeal.list(data)
  if (res?.code === 0) {
    dataList.value = res.data.list
    total.value = res.data.totalCount
    dataListLoading.value = false
  }
}

// 打开主播信息
const openAnchorUrl = (row) => {
  anchorInfoDialogVisible.value = true
  nextTick(() => {
    anchorUrlInfo.value?.init(row.anchorUrlId)
  })
}

// 打开主播视频
const openAnchorVideo = (row) => {
  analysisDialogVisible.value = true
  nextTick(() => {
    anchorVideoAnalysis.value?.init(row.anchorVideoId)
  })
}

// 处理申诉
const handle = (row) => {
  showHandleAppeal.value = true
  nextTick(() => {
    handleAppeal.value?.init(row)
  })
}

// 删除
const handleDelete = async (row) => {
  await ElMessageBox.confirm('确定删除该申诉?', '提示', {})
  const res = await api.userVideoAppeal.delete({id: row.id})
  if (res?.code === 0) {
    ElMessage.success('删除申诉成功')
    getList()
  }
}

// 监听状态变化
watch(
    () => dataForm.status,
    (val, oldVal) => {
      handleClick()
    }
)

// 生命周期
onMounted(() => {
  admin.value = router.currentRoute.value.query?.admin || 0
  getList()
})
</script>

<style lang="scss" scoped>
.mod-config {
  padding: 15px;
}
</style>
