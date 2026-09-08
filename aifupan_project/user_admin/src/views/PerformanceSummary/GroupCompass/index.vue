<template>
  <div class="group-compass-page">
    <div class="header-info">
      <div class="header-left">
        <el-icon class="company-icon" :size="64"><OfficeBuilding /></el-icon>
        <div class="info-content">
          <div class="name">{{ headerInfo.sourceName }}</div>
        </div>
      </div>
      <div class="header-right">
        <div class="info-item">
          <div class="label">分公司</div>
          <div class="value">{{ headerInfo.companyCount }}个</div>
        </div>
        <div class="info-item">
          <div class="label">部门</div>
          <div class="value">{{ headerInfo.deptCount }}个</div>
        </div>
        <div class="info-item">
          <div class="label">小组</div>
          <div class="value">{{ headerInfo.teamCount }}个</div>
        </div>
        <div class="info-item">
          <div class="label">直播间</div>
          <div class="value">{{ headerInfo.liveRoomCount }}个</div>
        </div>
      </div>
    </div>

    <DataOverviewTab :source-id="0" source-type="tenant" :date-range="dateRange">
      <template #header-actions>
        <div class="group-compass-page__actions">
          <DateQuickPicker v-model="dateRange" />
        </div>
      </template>
    </DataOverviewTab>
  </div>
</template>

<script setup>
  /**
   * @file index.vue
   * @description 集团业绩罗盘页面
   */
  import { ref, onMounted } from 'vue'
  import dayjs from 'dayjs'
  import { OfficeBuilding } from '@element-plus/icons-vue'
  import DataOverviewTab from './DataOverview/index.vue'
  import DateQuickPicker from '@/components/DateQuickPicker/index.vue'
  import apiModule from '@/http/api'

  const end = dayjs().subtract(1, 'day')
  const start = end.subtract(29, 'day')
  const dateRange = ref([start.format('YYYY-MM-DD'), end.format('YYYY-MM-DD')])

  const headerInfo = ref({
    sourceName: '-',
    companyCount: 0,
    deptCount: 0,
    teamCount: 0,
    liveRoomCount: 0
  })

  const getHeaderInfo = async () => {
    const res = await apiModule.performanceSummary.orgCount({ sourceId: 0, sourceType: 'tenant' })
    headerInfo.value = {
      sourceName: res.data?.sourceName || '-',
      companyCount: res.data?.companyCount || 0,
      deptCount: res.data?.deptCount || 0,
      teamCount: res.data?.teamCount || 0,
      liveRoomCount: res.data?.liveRoomCount || 0
    }
  }

  onMounted(() => {
    getHeaderInfo()
  })
</script>

<style scoped lang="scss">
  .group-compass-page {
    &__actions {
      display: flex;
      align-items: center;
      gap: 16px;
    }
  }

  .header-info {
    display: flex;
    align-items: center;
    padding: 28px 20px;
    margin-bottom: 20px;
    background: #fff;
    border-radius: 8px;

    .header-left {
      display: flex;
      align-items: center;
      padding-right: 60px;
      border-right: 1px solid #dcdcdc;

      .company-icon {
        width: 48px;
        height: 48px;
        padding: 8px;
        color: #409eff;
        background: #ecf5ff;
        border-radius: 8px;
      }

      .info-content {
        margin-left: 16px;
        color: #151719;

        .name {
          font-size: 18px;
          font-weight: 500;
          color: #303133;
        }
      }
    }

    .header-right {
      display: flex;
      gap: 40px;
      padding-left: 60px;
      margin-right: 40px;

      .info-item {
        text-align: left;

        .label {
          font-weight: 500;
          margin-bottom: 4px;
          font-size: 14px;
          color: #484a4c;
        }

        .value {
          font-size: 14px;
          color: #484a4c;
        }
      }
    }
  }
</style>
