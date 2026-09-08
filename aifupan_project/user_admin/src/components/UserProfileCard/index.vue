<template>
  <div class="user-profile-card">
    <div class="left-section">
      <div class="avtar-container">
        <el-avatar :size="64" :src="userInfo.userAvatar || defaultImg" />
      </div>
      <div class="info-content">
        <div class="name">{{ userInfo.name }}</div>
        <div class="sub-info">直播间：{{ userInfo.liveRoom }}</div>
      </div>
    </div>
    <div class="right-section">
      <div class="info-item">
        <div class="label">所属组织</div>
        <div class="value">{{ userInfo.organization }}</div>
      </div>
      <div class="info-item">
        <div class="label">岗位</div>
        <div class="value position">{{ userInfo.position }}</div>
      </div>
      <div class="info-item">
        <div class="label">电话</div>
        <div class="value">{{ userInfo.phone }}</div>
      </div>
      <div class="info-item">
        <div class="label">邮箱</div>
        <div class="value">{{ userInfo.email }}</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import defaultImg from '@/assets/images/funnel/defaultAvatar.png'
  /**
   * @file UserProfileCard.vue
   * @description 用户信息展示卡片
   */
  defineProps({
    userInfo: {
      type: Object,
      required: true,
      default: () => ({})
    }
  })
</script>

<style scoped lang="scss">
  .avtar-container {
    display: flex;
    align-items: center;
  }

  .user-profile-card {
    background: #fff;
    padding: 28px 20px;
    border-radius: 10px;
    display: flex;
    align-items: center;

    .left-section {
      display: flex;
      align-items: center;
      margin-right: 60px;
      padding-right: 60px;
      border-right: 1px solid #ebeef5;

      .info-content {
        margin-left: 16px;

        .name {
          font-size: 16px;
          font-weight: 600;
          white-space: nowrap;
          color: #303133;
          margin-bottom: 4px;
        }

        .sub-info {
          font-size: 12px;
          color: #909399;
          white-space: nowrap;
          overflow: hidden;
          text-overflow: ellipsis;
          max-width: 200px;
        }
      }
    }

    .right-section {
      display: flex;
      column-gap: 60px;
      row-gap: 10px;
      flex: 1;
      flex-wrap: wrap;

      .info-item {
        display: flex;
        flex-direction: column;

        .label {
          font-size: 14px;
          color: #484a4c;
          margin-bottom: 8px;
          font-weight: 500;
        }

        .value {
          font-size: 14px;
          color: #484a4c;
          font-weight: 400;
        }
        .position {
          color: #444dff;
        }
      }
    }
  }
</style>
