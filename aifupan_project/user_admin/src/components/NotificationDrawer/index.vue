<template>
  <el-drawer
    v-model="visible"
    :title="drawerTitle"
    direction="rtl"
    size="400px"
    :before-close="handleClose"
    custom-class="notification-drawer"
  >
    <div v-if="currentView === 'list'" class="notification-list">
      <div
        v-for="item in notifications"
        :key="item.id"
        class="notification-item"
        :class="{ 'is-read': item.read }"
        @click="viewDetail(item)"
      >
        <div class="item-header">
          <div class="item-title">{{ item.title }}</div>
          <div class="item-time">{{ item.time }}</div>
        </div>
        <div class="item-content">{{ item.content }}</div>
      </div>
      <el-empty v-if="notifications.length === 0" description="暂无消息" />
    </div>

    <div v-else class="notification-detail">
      <div class="detail-header">
        <el-icon class="back-icon" @click="backToList"><Back /></el-icon>
        <span class="detail-title">消息详情</span>
      </div>
      <div class="detail-body">
        <h3>{{ currentNotification.title }}</h3>
        <p class="detail-time">{{ currentNotification.time }}</p>
        <div class="detail-content">{{ currentNotification.fullContent || currentNotification.content }}</div>
      </div>
    </div>
  </el-drawer>
</template>

<script setup>
  /**
   * @file src/components/NotificationDrawer/index.vue
   * @description 消息通知抽屉组件
   */
  import { ref, computed } from 'vue'
  import { Back } from '@element-plus/icons-vue'

  const props = defineProps({
    modelValue: {
      type: Boolean,
      default: false
    }
  })

  const emit = defineEmits(['update:modelValue', 'read'])

  const visible = computed({
    get: () => props.modelValue,
    set: (val) => emit('update:modelValue', val)
  })

  // 视图状态：'list' | 'detail'
  const currentView = ref('list')
  const currentNotification = ref({})

  // 模拟消息数据
  const notifications = ref([
    {
      id: 1,
      title: '系统通知',
      content: '您的账号将于明日过期，请及时续费以免影响使用。',
      fullContent: '您的账号将于明日过期，请及时续费以免影响使用。如有疑问请联系管理员。',
      time: '10:00',
      read: false
    },
    {
      id: 2,
      title: '排班提醒',
      content: '您有新的排班待确认，请前往排班页面查看。',
      fullContent: '您有新的排班待确认，请前往排班页面查看。排班周期：2026/01/20 - 2026/01/26。',
      time: '昨天',
      read: true
    },
    {
      id: 3,
      title: '业绩日报',
      content: '昨日直播间业绩已生成，点击查看详情。',
      fullContent: '昨日直播间业绩已生成，点击查看详情。总销售额：￥50,000。',
      time: '01-18',
      read: true
    }
  ])

  const drawerTitle = computed(() => {
    return currentView.value === 'list' ? '消息通知' : ''
  })

  const viewDetail = (item) => {
    currentNotification.value = item
    currentView.value = 'detail'

    // 标记为已读
    if (!item.read) {
      item.read = true
      emit('read', item.id)
    }
  }

  const backToList = () => {
    currentView.value = 'list'
    currentNotification.value = {}
  }

  const handleClose = (done) => {
    backToList() // 重置视图
    done()
  }
</script>

<style lang="scss" scoped>
  .notification-list {
    padding: 0;
  }

  .notification-item {
    padding: 16px;
    border-bottom: 1px solid #ebeef5;
    cursor: pointer;
    transition: background-color 0.2s;

    &:hover {
      background-color: #f5f7fa;
    }

    &.is-read {
      .item-title,
      .item-content,
      .item-time {
        color: #909399;
      }
    }

    .item-header {
      display: flex;
      justify-content: space-between;
      margin-bottom: 8px;

      .item-title {
        font-weight: 500;
        color: #303133;
        font-size: 14px;
      }

      .item-time {
        font-size: 12px;
        color: #909399;
      }
    }

    .item-content {
      font-size: 13px;
      color: #606266;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      width: 100%;
    }
  }

  .notification-detail {
    .detail-header {
      display: flex;
      align-items: center;
      margin-bottom: 20px;
      padding-bottom: 10px;
      border-bottom: 1px solid #ebeef5;

      .back-icon {
        font-size: 20px;
        cursor: pointer;
        margin-right: 10px;
        &:hover {
          color: var(--el-color-primary);
        }
      }

      .detail-title {
        font-size: 16px;
        font-weight: 500;
      }
    }

    .detail-body {
      h3 {
        margin-top: 0;
        color: #303133;
      }
      .detail-time {
        color: #909399;
        font-size: 12px;
        margin-bottom: 20px;
      }
      .detail-content {
        color: #606266;
        line-height: 1.6;
        font-size: 14px;
      }
    }
  }
</style>
