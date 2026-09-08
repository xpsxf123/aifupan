<template>
  <div class="data-ranking-list" :style="bgStyles">
    <div class="ranking-header">
      <span class="title" v-html="formattedTitle"></span>
      <!--      <div class="header-icon">
        <img v-if="iconType === 'chart'" src="@/assets/images/funnel/video.png" alt="chart" />
        <img v-else-if="iconType === 'chat'" src="@/assets/images/icon/top1.png" alt="chat" />
        <img v-else-if="iconType === 'video'" src="@/assets/images/icon/top1.png" alt="video" />
      </div>-->
    </div>

    <div class="ranking-content">
      <div v-for="(item, index) in list" :key="item.id" class="ranking-item" :class="{ 'top-three': index < 3 }">
        <div class="rank-num">
          <img v-if="index === 0" src="@/assets/images/icon/top1.png" alt="1" />
          <img v-else-if="index === 1" src="@/assets/images/icon/top2.png" alt="2" />
          <img v-else-if="index === 2" src="@/assets/images/icon/top3.png" alt="3" />
          <span v-else>{{ index + 1 }}</span>
        </div>

        <div class="rank-info">
          <div v-if="item.avatar" class="avtar-container">
            <el-avatar :size="24" :src="item.avatar || defaultImg" />
          </div>
          <span class="info-name">{{ item.name }}</span>
        </div>

        <div class="rank-value">{{ item.value }}</div>
      </div>
    </div>
  </div>
</template>

<script setup>
  import { computed } from 'vue'
  import chart from '@/assets/images/funnel/chart.png'
  import chat from '@/assets/images/funnel/chat.png'
  import video from '@/assets/images/funnel/video.png'
  import defaultImg from '@/assets/images/funnel/defaultAvatar.png'

  const props = defineProps({
    title: {
      type: String,
      required: true
    },
    list: {
      type: Array,
      default: () => []
    },
    iconType: {
      type: String,
      default: 'chart' // chart, chat, video
    }
  })

  // type 对应背景图
  const bgMap = {
    chart,
    chat,
    video
  }

  // computed 生成背景样式数组
  const bgStyles = computed(() => {
    return {
      background: `url(${bgMap[props.iconType]}) no-repeat right top / cover`
    }
  })

  const formattedTitle = computed(() => {
    // Highlight "TOP" in the title
    return props.title.replace('TOP', '<span class="highlight">TOP</span>')
  })
</script>

<style scoped lang="scss">
  .avtar-container {
    display: flex;
    align-items: center;
  }

  .data-ranking-list {
    border-radius: 8px;
    padding: 16px;
    position: relative;
    overflow: hidden;
    min-width: 200px;
    background: url('@/assets/images/funnel/video.png') no-repeat right top/cover;
  }

  .ranking-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 12px;

    .title {
      font-size: 16px;
      font-weight: 600;
      color: #303133;

      :deep(.highlight) {
        color: #ff6b6b; // Adjust color based on design, seemingly reddish/pinkish for "TOP"
        margin-right: 4px;
      }
    }
  }

  .ranking-item {
    display: flex;
    align-items: center;
    padding: 10px 0;
    border-bottom: 1px solid transparent; // Placeholder

    &:last-child {
      padding-bottom: 0;
    }

    &.top-three {
      .info-name {
        font-weight: 600;
      }
    }

    .rank-num {
      width: 24px;
      height: 24px;
      display: flex;
      justify-content: center;
      align-items: center;
      margin-right: 12px;
      font-size: 14px;
      color: #909399;

      img {
        width: 20px;
        height: 24px;
        object-fit: contain;
      }
    }

    .rank-info {
      flex: 1;
      display: flex;
      align-items: center;
      overflow: hidden;

      .avtar-container {
        margin-right: 8px;
      }

      .info-name {
        font-size: 14px;
        color: #484a4c;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
      }
    }

    .rank-value {
      font-size: 14px;
      color: #606266;
      margin-left: 8px;
    }
  }
</style>
