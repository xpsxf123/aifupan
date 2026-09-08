<template>
  <div class="live-room-card" :class="{ 'is-active': active }">
    <!--  <div class="live-room-card" :class="{ 'is-active': active }" @click="$emit('select', data)">-->
    <img :src="mapPlatform[data.raw.platform]" alt="" class="platform-icon" />
    <LiveRoomHeader :info="data.header" @edit="$emit('edit', data)" @history="$emit('history', data)" />
    <LiveRoomStats :data="data.stats" :config="statsConfig" :unit-config="unitConfig" />
  </div>
</template>

<script setup>
  /**
   * @file LiveRoomCard.vue
   * @description 直播间卡片组件，整合头部和数据指标
   */
  import LiveRoomHeader from './LiveRoomHeader.vue'
  import LiveRoomStats from './LiveRoomStats.vue'
  import dy from '@/assets/images/icon/dy20.png'
  import ks from '@/assets/images/icon/ks20.png'
  import sph from '@/assets/images/icon/sph20.png'

  defineProps({
    data: {
      type: Object,
      required: true
    },
    active: {
      type: Boolean,
      default: false
    },
    // 透传给 Stats 的配置
    statsConfig: {
      type: Array,
      default: () => []
    },
    unitConfig: {
      type: Array,
      default: () => []
    }
  })

  defineEmits(['edit', 'history', 'select'])

  const mapPlatform = {
    0: dy,
    1: ks,
    2: sph
  }
</script>

<style scoped>
  .live-room-card {
    position: relative;
    border: 1px solid #dfeaf6;
    border-radius: 10px;
    padding: 16px 20px;
    cursor: pointer;
  }

  .platform-icon {
    position: absolute;
    left: 0;
    top: 0;
    width: 20px;
  }
</style>
