<template>
  <div class="data-card-group">
    <el-row :gutter="16" style="margin-bottom: -16px">
      <el-col
        v-for="(card, index) in cards"
        :key="index"
        :xs="24"
        :sm="12"
        :md="8"
        :lg="4"
        :xl="4"
        style="margin-bottom: 16px"
      >
        <DataCard
          :title="card.title"
          :icon="card.icon"
          :icon-color="card.iconColor"
          :icon-bg-color="card.iconBgColor"
          :data="card.data"
          :config="card.config"
        >
          <!-- 传递插槽 -->
          <template #icon>
            <slot :name="`icon-${index}`"></slot>
          </template>
        </DataCard>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
  import { defineProps } from 'vue'
  import DataCard from '@/components/DataCard/index.vue'

  const props = defineProps({
    // 卡片数组配置
    // [{ title: '直播场次', icon: 'VideoCamera', data: {...}, config: [...] }, ...]
    cards: {
      type: Array,
      default: () => []
    }
  })
</script>

<style scoped>
  .data-card-group {
    width: 100%;
  }
</style>
