<template>
  <div class="personal-performance">
    <UserProfileCard :user-info="userInfo" />
    <div class="tab-container">
      <PageTabs v-model="activeTab" :tabs="tabs">
        <template #data>
          <LiveDataTab v-if="employeeId" :employee-id="employeeId" @switch-tab="handleSwitchTab" />
        </template>

        <template #session>
          <LiveSessionTab v-if="employeeId" ref="sessionTabRef" :employee-id="employeeId" />
        </template>
      </PageTabs>
    </div>
  </div>
</template>

<script setup>
  /**
   * @file PersonalPerformance.vue
   * @description 个人业绩页面 (重构版)
   */
  import { ref, onMounted, computed } from 'vue'
  import { useUserStore } from '@/store/user'
  import PageTabs from '@/components/PageTabs/index.vue'
  import UserProfileCard from '@/components/UserProfileCard/index.vue'
  import LiveDataTab from '@/components/Performance/LiveDataTab.vue'
  import LiveSessionTab from '@/components/Performance/LiveSessionTab.vue'

  const userStore = useUserStore()

  const userInfo = computed(() => userStore.userInfo)
  const employeeId = computed(() => userStore.userInfo?.employeeId)

  const sessionTabRef = ref(null)

  const activeTab = ref('data')

  const tabs = [
    { label: '直播数据', value: 'data' },
    { label: '直播场次', value: 'session' }
  ]

  const handleSwitchTab = (payload) => {
    if (typeof payload === 'string') {
      activeTab.value = payload
      return
    }
    if (!payload) return
    activeTab.value = payload.tab || 'session'
    if (payload.dateRange) sessionTabRef.value?.setDateRange?.(payload.dateRange)
  }

  const fetchProfile = async () => {
    if (!userStore.userInfo?.employeeId) {
      await userStore.getUserInfo()
    }
  }

  onMounted(() => {
    fetchProfile()
  })
</script>

<style lang="scss" scoped>
  .personal-performance {
    display: flex;
    flex-direction: column;
  }
  .tab-container {
    margin-top: 12px;
  }
</style>
