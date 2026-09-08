<template>
  <div class="page-container">
    <UserProfileCard :user-info="userInfo" class="user-info" />

    <div class="tab-container">
      <PageTabs v-model="activeTab" :tabs="tabs">
        <template #data>
          <LiveDataTab :employee-id="employeeId" @switch-tab="handleSwitchTab" />
        </template>
        <template #session>
          <LiveSessionTab ref="sessionTabRef" :employee-id="employeeId" :date-range="sessionDateRange" />
        </template>
      </PageTabs>
    </div>
  </div>
</template>

<script setup>
  /**
   * @file index.vue
   * @description 人员业绩详情页（按员工维度：概览/趋势/按天与按班次明细）
   */
  import { computed, onMounted, reactive, ref, watch } from 'vue'
  import { useRoute } from 'vue-router'

  import PageTabs from '@/components/PageTabs/index.vue'
  import LiveDataTab from '@/components/Performance/LiveDataTab.vue'
  import LiveSessionTab from '@/components/Performance/LiveSessionTab.vue'
  import apiModule from '@/http/api'
  import UserProfileCard from '@/components/UserProfileCard/index.vue'

  const route = useRoute()

  const sessionDateRange = computed(() => {
    const normalize = (value) => (Array.isArray(value) ? value[0] : value)
    const startTime = normalize(route.query?.startTime)
    const endTime = normalize(route.query?.endTime)
    if (!startTime || !endTime) return []
    return [String(startTime).slice(0, 10), String(endTime).slice(0, 10)]
  })

  const employeeId = computed(() => (route.params.employeeId ? String(route.params.employeeId) : ''))

  const activeTab = ref('data')
  const sessionTabRef = ref(null)

  const tabs = [
    { label: '直播数据', value: 'data' },
    { label: '直播场次', value: 'session' }
  ]

  const userInfo = reactive({
    name: '-',
    liveRoom: '-',
    userAvatar: '',
    organization: '-',
    position: '-',
    phone: '-',
    email: '-'
  })

  /**
   * @description 将 employee.detail 响应映射到头部展示数据
   * @param {Object} data - 员工详情
   */
  const fillHeader = (data) => {
    const org = [data?.companyName, data?.deptName, data?.teamName].filter(Boolean).join('-') || '-'
    const jobTypeText = data?.jobType === 'FULL_TIME' ? '全职' : data?.jobType === 'PART_TIME' ? '兼职' : ''
    const positionText = `${data?.positionName || '-'}${jobTypeText ? `（${jobTypeText}）` : ''}`
    const rooms = data?.roomInfos || data?.liveRoomList || []
    const liveRoomText =
      Array.isArray(rooms) && rooms.length
        ? rooms
            .map((x) => x?.anchorName || x?.liveRoomName)
            .filter(Boolean)
            .join('、')
        : '-'

    userInfo.name = data?.name || data?.employeeName || '-'
    userInfo.userAvatar = data?.userAvatar
    userInfo.organization = org
    userInfo.position = positionText
    userInfo.phone = data?.mobile || '-'
    userInfo.email = data?.email || '-'
    userInfo.liveRoom = liveRoomText
  }

  /**
   * @description 拉取员工详情
   */
  const fetchEmployeeDetail = async () => {
    if (!employeeId.value) return
    try {
      const res = await apiModule.employeeProfile.base({ employeeId: employeeId.value, loadRoom: true })
      fillHeader(res?.data || {})
    } catch (e) {
      void e
    }
  }

  /**
   * @description 处理“查看场次明细”联动
   * @param {string|Object} payload - tabValue 或包含筛选的对象
   */
  const handleSwitchTab = (payload) => {
    if (typeof payload === 'string') {
      activeTab.value = payload
      return
    }
    if (!payload) return
    activeTab.value = payload.tab || 'session'
    if (payload.dateRange) sessionTabRef.value?.setDateRange?.(payload.dateRange)
  }

  watch(() => route.query, (newQuery) => {
    if (newQuery.tagType) {
      activeTab.value = newQuery.tagType === '1' ? 'session' : 'data'
    }
  }, { immediate: true })

  onMounted(() => {
    fetchEmployeeDetail()
  })
</script>

<style scoped lang="scss">
  .user-info {
    margin-bottom: 12px;
  }
</style>
