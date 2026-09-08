<template>
  <div class="empty-state-overlay" :style="{ backdropFilter: `blur(${blur}px)` }">
    <div class="empty-content">
      <img v-if="image" :src="image" class="empty-image" alt="Empty State" />
      <div v-else class="empty-image-placeholder">
        <!-- 默认图片 -->
        <svg viewBox="0 0 64 41" xmlns="http://www.w3.org/2000/svg" width="120" height="120">
          <g transform="translate(0 1)" fill="none" fill-rule="evenodd">
            <ellipse fill="#F5F5F5" cx="32" cy="33" rx="32" ry="7"></ellipse>
            <g fill-rule="nonzero" stroke="#D9D9D9">
              <path
                d="M55 12.76L44.854 1.258C44.367.474 43.656 0 42.907 0H21.093c-.749 0-1.46.474-1.947 1.257L9 12.761V22h46v-9.24z"
              ></path>
              <path
                d="M41.613 15.931c0-1.605.994-2.93 2.227-2.931H55v18.137C55 33.26 53.68 35 52.05 35h-40.1C10.32 35 9 33.259 9 31.137V13h11.16c1.233 0 2.227 1.323 2.227 2.928v.022c0 1.605 1.005 2.901 2.237 2.901h14.752c1.232 0 2.237-1.308 2.237-2.913v-.007z"
                fill="#FAFAFA"
              ></path>
            </g>
          </g>
        </svg>
      </div>

      <h3 class="empty-title" v-if="title">{{ title }}</h3>
      <p class="empty-description" v-if="description">{{ description }}</p>

      <div class="empty-actions" v-if="buttons && buttons.length">
        <el-button
          v-for="(btn, index) in buttons"
          :key="index"
          :type="btn.type || 'primary'"
          :size="btn.size || 'default'"
          :plain="btn.plain"
          :round="btn.round"
          :circle="btn.circle"
          :icon="btn.icon"
          :disabled="btn.disabled"
          :loading="btn.loading"
          :link="btn.link"
          :text="btn.textBtn"
          :bg="btn.bg"
          @click="handleBtnClick(btn)"
        >
          {{ btn.text }}
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
  import { defineProps } from 'vue'
  import { ElButton } from 'element-plus'

  defineProps({
    visible: {
      type: Boolean,
      default: false
    },
    image: {
      type: String,
      default: ''
    },
    title: {
      type: String,
      default: '暂无数据'
    },
    description: {
      type: String,
      default: ''
    },
    blur: {
      type: Number,
      default: 4
    },
    buttons: {
      type: Array,
      default: () => []
    }
  })

  const handleBtnClick = (btn) => {
    if (btn.click && typeof btn.click === 'function') {
      btn.click()
    }
  }
</script>

<style scoped>
  .empty-state-overlay {
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background-color: rgba(255, 255, 255, 0.9);
    z-index: 2000;
    display: flex;
    justify-content: center;
    align-items: center;
    transition: opacity 0.3s;
  }

  .empty-content {
    text-align: center;
    display: flex;
    flex-direction: column;
    align-items: center;
  }

  .empty-image {
    width: 160px;
    height: auto;
    margin-bottom: 16px;
  }

  .empty-image-placeholder {
    margin-bottom: 16px;
  }

  .empty-title {
    font-size: 16px;
    font-weight: 500;
    color: #303133;
    margin: 0 0 8px 0;
  }

  .empty-description {
    font-size: 14px;
    color: #909399;
    margin: 0 0 24px 0;
    line-height: 1.5;
    max-width: 400px;
  }

  .empty-actions {
    display: flex;
    gap: 12px;
  }
</style>
