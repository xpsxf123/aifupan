<template>
  <div class="staff-performance-page">
    <div class="filter-panel">
      <el-form :inline="true" :model="queryForm" class="filter-form">
        <el-form-item label="所属公司">
          <el-select
            v-model="queryForm.companyId"
            filterable
            remote
            clearable
            placeholder="全部"
            style="width: 200px"
            :remote-method="fetchCompanyOptions"
            :loading="companyLoading"
            class="rounded"
          >
            <el-option v-for="opt in companyOptions" :key="opt.key" :label="opt.label" :value="opt.key" />
          </el-select>
        </el-form-item>

        <el-form-item label="所属部门">
          <el-select
            v-model="queryForm.deptId"
            filterable
            remote
            clearable
            placeholder="全部"
            style="width: 200px"
            :remote-method="fetchDeptOptions"
            :loading="deptLoading"
            :disabled="!queryForm.companyId"
            class="rounded"
          >
            <el-option v-for="opt in deptOptions" :key="opt.key" :label="opt.label" :value="opt.key" />
          </el-select>
        </el-form-item>

        <el-form-item label="所属小组" v-show="!searchCollapsed">
          <el-select
            v-model="queryForm.teamId"
            filterable
            remote
            clearable
            placeholder="全部"
            style="width: 200px"
            :remote-method="fetchTeamOptions"
            :loading="teamLoading"
            :disabled="!queryForm.deptId"
            class="rounded"
          >
            <el-option v-for="opt in teamOptions" :key="opt.key" :label="opt.label" :value="opt.key" />
          </el-select>
        </el-form-item>

        <el-form-item label="所属岗位" v-show="!searchCollapsed">
          <el-select
            v-model="queryForm.positionId"
            filterable
            remote
            clearable
            placeholder="全部"
            style="width: 200px"
            :remote-method="fetchPositionOptions"
            :loading="positionLoading"
            class="rounded"
          >
            <el-option v-for="opt in positionOptions" :key="opt.key" :label="opt.label" :value="opt.key" />
          </el-select>
        </el-form-item>

        <el-form-item label="在职" v-show="!searchCollapsed">
          <el-select
            v-model="queryForm.accountStatus"
            clearable
            placeholder="全部"
            style="width: 200px"
            class="rounded"
          >
            <el-option label="全部" value="" />
            <el-option label="在职" :value="1" />
            <el-option label="停用" :value="0" />
          </el-select>
        </el-form-item>

        <el-form-item label="姓名">
          <el-input
            v-model="queryForm.employeeName"
            clearable
            placeholder="请输入"
            style="width: 200px"
            class="rounded"
          />
        </el-form-item>

        <el-form-item label="手机号" v-show="!searchCollapsed">
          <el-input v-model="queryForm.mobile" clearable placeholder="请输入" style="width: 200px" class="rounded" />
        </el-form-item>

        <el-form-item class="search-collapse-item">
          <el-button type="primary" link @click="toggleSearchCollapse">
            {{ searchCollapsed ? '展开' : '收起' }}
            <el-icon class="el-icon--right">
              <ArrowDown v-if="searchCollapsed" />
              <ArrowUp v-else />
            </el-icon>
          </el-button>
        </el-form-item>

        <el-form-item>
          <el-button class="custom-btn" round plain type="primary" @click="handleSearch">查询</el-button>
          <el-button class="custom-btn" round @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="card-container">
      <div class="count-bar">成员人数（{{ total }}个）</div>

      <div class="list-container">
        <el-skeleton v-if="loading" :rows="6" animated />
        <template v-else>
          <div v-if="list.length" class="card-list">
            <EmployeePerformanceCard
              v-for="item in list"
              :key="item.employeeId"
              :data="item"
              @detail="goDetail(item)"
            />
          </div>
          <el-empty v-else description="暂无数据" />
        </template>
      </div>
    </div>

    <div class="page-bar" v-if="total > 0">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="pageSize"
        :total="total"
        background
        layout="total, prev, pager, next, sizes"
        :page-sizes="[10, 20, 50, 100]"
        @current-change="fetchList"
        @size-change="handleSizeChange"
      />
    </div>
  </div>
</template>

<script setup>
  /**
   * @file index.vue
   * @description 人员业绩列表页面（按人员维度聚合业绩）
   */
  import { onMounted, reactive, ref, watch } from 'vue'
  import { ArrowDown, ArrowUp } from '@element-plus/icons-vue'
  import { useRouter } from 'vue-router'
  import apiModule from '@/http/api'
  import EmployeePerformanceCard from './components/EmployeePerformanceCard.vue'
  import { normalizeKeyLabelOptions } from '@/utils/options'

  const router = useRouter()

  const loading = ref(false)
  const list = ref([])
  const total = ref(0)
  const page = ref(1)
  const pageSize = ref(10)

  const queryForm = reactive({
    companyId: '',
    deptId: '',
    teamId: '',
    positionId: '',
    accountStatus: '',
    employeeName: '',
    mobile: ''
  })

  const searchCollapsed = ref(true)

  const companyLoading = ref(false)
  const deptLoading = ref(false)
  const teamLoading = ref(false)
  const positionLoading = ref(false)

  const companyOptions = ref([])
  const deptOptions = ref([])
  const teamOptions = ref([])
  const positionOptions = ref([])

  /**
   * @description 拉取子公司下拉
   * @param {string} keyword - 关键字
   */
  const fetchCompanyOptions = async (keyword) => {
    companyLoading.value = true
    try {
      const res = await apiModule.subCompany.options({ keyword, limit: 50 })
      companyOptions.value = normalizeKeyLabelOptions(res?.data || [])
    } finally {
      companyLoading.value = false
    }
  }

  /**
   * @description 拉取部门下拉（依赖公司）
   * @param {string} keyword - 关键字
   */
  const fetchDeptOptions = async (keyword) => {
    if (!queryForm.companyId) return
    deptLoading.value = true
    try {
      const res = await apiModule.dept.options({ companyId: queryForm.companyId, keyword, limit: 50 })
      deptOptions.value = normalizeKeyLabelOptions(res?.data || [])
    } finally {
      deptLoading.value = false
    }
  }

  /**
   * @description 拉取小组下拉（依赖部门）
   * @param {string} keyword - 关键字
   */
  const fetchTeamOptions = async (keyword) => {
    if (!queryForm.deptId) return
    teamLoading.value = true
    try {
      const res = await apiModule.team.options({ deptId: queryForm.deptId, keyword, limit: 50 })
      teamOptions.value = normalizeKeyLabelOptions(res?.data || [])
    } finally {
      teamLoading.value = false
    }
  }

  /**
   * @description 拉取岗位下拉
   * @param {string} keyword - 关键字
   */
  const fetchPositionOptions = async (keyword) => {
    positionLoading.value = true
    try {
      const res = await apiModule.position.options({ keyword, limit: 50 })
      positionOptions.value = normalizeKeyLabelOptions(res?.data || [])
    } finally {
      positionLoading.value = false
    }
  }

  /**
   * @description 拉取人员业绩列表
   */
  const fetchList = async () => {
    loading.value = true
    try {
      const res = await apiModule.employeePerformance.page({
        page: page.value,
        limit: pageSize.value,
        companyId: queryForm.companyId || undefined,
        deptId: queryForm.deptId || undefined,
        teamId: queryForm.teamId || undefined,
        positionId: queryForm.positionId || undefined,
        accountStatus: queryForm.accountStatus === '' ? undefined : queryForm.accountStatus,
        employeeName: queryForm.employeeName || undefined,
        mobile: queryForm.mobile || undefined
      })
      list.value = res?.data?.list || []
      total.value = res?.data?.totalCount || 0
    } finally {
      loading.value = false
    }
  }

  /**
   * @description 查询
   */
  const handleSearch = () => {
    page.value = 1
    fetchList()
  }

  const toggleSearchCollapse = () => {
    searchCollapsed.value = !searchCollapsed.value
  }

  /**
   * @description 重置
   */
  const handleReset = () => {
    queryForm.companyId = ''
    queryForm.deptId = ''
    queryForm.teamId = ''
    queryForm.positionId = ''
    queryForm.accountStatus = ''
    queryForm.employeeName = ''
    queryForm.mobile = ''
    deptOptions.value = []
    teamOptions.value = []
    page.value = 1
    fetchList()
  }

  /**
   * @description 切换页大小
   */
  const handleSizeChange = () => {
    page.value = 1
    fetchList()
  }

  /**
   * @description 跳转详情
   * @param {Object} row - 人员业绩行
   */
  const goDetail = (row) => {
    router.push(`/staff-management/performance/detail/${row.employeeId}`)
  }

  watch(
    () => queryForm.companyId,
    () => {
      queryForm.deptId = ''
      queryForm.teamId = ''
      deptOptions.value = []
      teamOptions.value = []
      if (queryForm.companyId) fetchDeptOptions('')
    }
  )

  watch(
    () => queryForm.deptId,
    () => {
      queryForm.teamId = ''
      teamOptions.value = []
      if (queryForm.deptId) fetchTeamOptions('')
    }
  )

  onMounted(() => {
    fetchCompanyOptions('')
    fetchPositionOptions('')
    fetchList()
  })
</script>

<style scoped lang="scss">
  .staff-performance-page {
  }

  .filter-panel {
    background: #fff;
    border-radius: 10px;
    padding: 14px 16px;
    margin-bottom: 16px;
  }

  .filter-form :deep(.el-form-item) {
    margin-bottom: 10px;
  }
  .card-container {
    padding: 20px;
    border-radius: 10px;
    .count-bar {
      margin-bottom: 20px;
      font-size: 16px;
      font-weight: bold;
      color: #151719;
    }

    .list-container {
      display: flex;
      flex-direction: column;
      gap: 12px;
    }
    background-color: #fff;
  }

  .card-list {
    display: flex;
    flex-direction: column;
    gap: 16px;
  }

  .page-bar {
    background: #fff;
    border-radius: 10px;
    padding: 12px 16px;
    display: flex;
    justify-content: center;
    margin-top: 12px;
  }

  .custom-btn {
    width: 74px;
    height: 32px;
  }

  .search-collapse-item {
    margin-left: 6px;
  }
</style>
