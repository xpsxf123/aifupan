<template>
  <div class="component-doc">
    <h2>UserProfileCard 用户信息卡片</h2>
    <p>展示用户详细信息的卡片布局。</p>

    <div class="demo-block">
      <UserProfileCard :userInfo="userInfo" />
    </div>
  </div>
</template>

<script setup>
  import { ref } from 'vue'
  import UserProfileCard from '@/components/UserProfileCard/index.vue'

  const userInfo = ref({
    name: '王五',
    liveRoom: '直播间B',
    avatar: 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png',
    organization: '运营部',
    position: '运营总监',
    phone: '13900139000',
    email: 'wangwu@example.com'
  })
</script>

<style scoped>
  .component-doc {
    padding: 20px;
    background: #fff;
  }
  .demo-block {
    padding: 20px;
    border: 1px solid #ebeef5;
    margin: 20px 0;
  }
</style>
