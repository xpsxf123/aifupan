<template>
  <div class="live-room-list">
    <!-- 搜索区域 -->
    <div class="list-search">
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="所属平台">
          <el-select v-model="searchForm.platform" clearable placeholder="全部" style="width: 200px" class="rounded">
            <el-option label="全部" value="" />
            <el-option label="抖音" :value="0" />
            <el-option label="快手" :value="1" />
            <el-option label="视频号" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="所属公司">
          <el-select
            v-model="searchForm.companyId"
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
        <el-form-item label="所属部门" v-show="!searchCollapsed">
          <el-select
            v-model="searchForm.deptId"
            filterable
            remote
            clearable
            placeholder="全部"
            style="width: 200px"
            :remote-method="fetchDeptOptions"
            :loading="deptLoading"
            :disabled="!searchForm.companyId"
            class="rounded"
          >
            <el-option v-for="opt in deptOptions" :key="opt.key" :label="opt.label" :value="opt.key" />
          </el-select>
        </el-form-item>
        <el-form-item label="所属小组" v-show="!searchCollapsed">
          <el-select
            v-model="searchForm.teamId"
            filterable
            remote
            clearable
            placeholder="全部"
            style="width: 200px"
            :remote-method="fetchTeamOptions"
            :loading="teamLoading"
            :disabled="!searchForm.deptId"
            class="rounded"
          >
            <el-option v-for="opt in teamOptions" :key="opt.key" :label="opt.label" :value="opt.key" />
          </el-select>
        </el-form-item>
        <el-form-item label="直播间">
          <el-input
            v-model="searchForm.anchorName"
            placeholder="直播间名称"
            clearable
            style="width: 200px"
            class="rounded"
          />
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
          <el-button class="custom-btn" type="primary" round @click="handleSearch" plain>查询</el-button>
          <el-button class="custom-btn" round @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="main-container">
      <!-- 列表标题 -->
      <div class="list-header" v-if="total !== undefined">
        <span class="title">直播间 ({{ total }}个)</span>
      </div>

      <div class="list-content">
        <div class="card-wrapper" v-if="list && list.length">
          <LiveRoomCard
            v-for="(item, index) in list"
            :key="item.id || index"
            :data="item"
            :active="activeId !== undefined && activeId !== null ? item.id === activeId : false"
            :stats-config="statsConfig"
            :unit-config="unitConfig"
            @edit="(data) => $emit('edit', data)"
            @history="(data) => $emit('select', data)"
            @select="(data) => $emit('select', data)"
          />
        </div>
        <el-empty v-else description="暂无数据" />
      </div>
    </div>
  </div>
</template>

<script setup>
  /**
   * @file LiveRoomList.vue
   * @description 直播间列表组件，包含搜索和滚动列表
   */
  import { onMounted, ref, watch } from 'vue'
  import { ArrowDown, ArrowUp, Search } from '@element-plus/icons-vue'
  import LiveRoomCard from '../LiveRoomCard/index.vue'
  import apiModule from '@/http/api'
  import { normalizeKeyLabelOptions } from '@/utils/options'

  const props = defineProps({
    // 列表数据
    list: {
      type: Array,
      default: () => []
    },
    activeId: {
      type: [Number, String],
      default: undefined
    },
    // 总数
    total: {
      type: Number,
      default: 0
    },
    // 列表高度
    height: {
      type: String,
      default: '600px'
    },
    // 加载状态
    loading: {
      type: Boolean,
      default: false
    },
    // 统计指标配置
    statsConfig: {
      type: Array,
      default: () => []
    },
    // 单位换算配置
    unitConfig: {
      type: Array,
      default: () => []
    }
  })

  const emit = defineEmits(['search', 'reset', 'edit', 'history', 'select'])

  const searchForm = ref({
    platform: '',
    companyId: '',
    deptId: '',
    teamId: '',
    anchorName: ''
  })

  const searchCollapsed = ref(true)

  const companyLoading = ref(false)
  const deptLoading = ref(false)
  const teamLoading = ref(false)

  const companyOptions = ref([])
  const deptOptions = ref([])
  const teamOptions = ref([])

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
    if (!searchForm.value.companyId) return
    deptLoading.value = true
    try {
      const res = await apiModule.dept.options({ companyId: searchForm.value.companyId, keyword, limit: 50 })
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
    if (!searchForm.value.deptId) return
    teamLoading.value = true
    try {
      const res = await apiModule.team.options({ deptId: searchForm.value.deptId, keyword, limit: 50 })
      teamOptions.value = normalizeKeyLabelOptions(res?.data || [])
    } finally {
      teamLoading.value = false
    }
  }

  const handleSearch = () => {
    emit('search', searchForm.value)
  }

  const toggleSearchCollapse = () => {
    searchCollapsed.value = !searchCollapsed.value
  }

  const handleReset = () => {
    searchForm.value = {
      platform: '',
      companyId: '',
      deptId: '',
      teamId: '',
      anchorName: ''
    }
    deptOptions.value = []
    teamOptions.value = []
    emit('reset')
  }

  watch(
    () => searchForm.value.companyId,
    () => {
      searchForm.value.deptId = ''
      searchForm.value.teamId = ''
      deptOptions.value = []
      teamOptions.value = []
      if (searchForm.value.companyId) fetchDeptOptions('')
    }
  )

  watch(
    () => searchForm.value.deptId,
    () => {
      searchForm.value.teamId = ''
      teamOptions.value = []
      if (searchForm.value.deptId) fetchTeamOptions('')
    }
  )

  onMounted(() => {
    fetchCompanyOptions('')
  })
</script>

<style scoped>
  .main-container {
    padding: 20px;
    border-radius: 10px;
    background-color: #fff;
  }

  .live-room-list {
    border-radius: 8px;
  }

  .list-search {
    background: #fff;
    padding: 16px 16px 0;
    border-radius: 8px;
    margin-bottom: 16px;
  }

  .list-header {
    margin-bottom: 12px;
    font-size: 16px;
    font-weight: 600;
    color: #303133;
  }

  .card-wrapper {
    display: flex;
    flex-direction: column;
    row-gap: 16px;
  }

  .search-collapse-item {
    margin-left: 6px;
  }

  .custom-btn {
    width: 74px;
    height: 32px;
  }
</style>
