<template>
  <div class="live-room-header">
    <div class="header-left">
      <div class="avtar-container">
        <el-avatar :size="40" :src="info.avatar || defaultImg" />
      </div>
      <div class="room-info">
        <div class="room-name">
          <span class="name">{{ info.name }}</span>
          <div class="tags">
            <el-tag
              v-for="(tag, index) in info.tags"
              :key="index"
              :type="tag.type"
              size="small"
              effect="plain"
              class="tag"
            >
              {{ tag.label }}
            </el-tag>
          </div>
        </div>
        <div class="room-meta">
          <div class="meta-item">
            <span class="label">所属组织：</span>
            <span class="value">{{ info.org }}</span>
          </div>
          <div class="meta-item" v-if="info.managers">
            <span class="label">管理者：</span>
            <span class="value">{{ info.managers }}</span>
          </div>
        </div>
      </div>
    </div>
    <div class="header-right">
      <el-button v-auth="'room:performance:update'" type="text" @click.stop="$emit('edit')">编辑</el-button>
      <el-button type="primary" plain @click.stop="$emit('history')" style="border-radius: 39px; height: 28px"
        >查看历史业绩</el-button
      >
    </div>
  </div>
</template>

<script setup>
  /**
   * @file LiveRoomHeader.vue
   * @description 直播间卡片头部组件，包含头像、名称、标签及操作按钮
   */
  import defaultImg from '@/assets/images/funnel/defaultAvatar.png'

  defineProps({
    info: {
      type: Object,
      required: true,
      default: () => ({
        avatar: '',
        name: '',
        tags: [], // [{ label: '巨量已失效', type: 'danger' }]
        org: '',
        managers: ''
      })
    }
  })

  defineEmits(['edit', 'history'])
</script>

<style scoped>
  .avtar-container {
    display: flex;
    align-items: center;
  }
  .live-room-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    padding-bottom: 12px;
  }

  .header-left {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  .room-info {
    display: flex;
    gap: 4px;
    font-size: 14px;
  }

  .room-name {
    display: flex;
    align-items: center;
    gap: 30px;
  }

  .name {
    font-weight: 600;
    color: #303133;
  }

  .tags {
    display: flex;
    gap: 4px;
  }

  .room-meta {
    color: #909399;
    display: flex;
    align-items: center;
    gap: 30px;
  }

  .room-meta .label {
    color: #909399;
  }

  .room-meta .value {
    color: #606266;
  }

  .header-right {
    display: flex;
    align-items: center;
    gap: 8px;
  }
</style>
