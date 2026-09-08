<template>
  <div class="component-doc">
    <h2>PerformanceHeader 业绩头部组件</h2>
    <p>Performance 模块专用的头部组件，基于 PageHeaderInfo 封装。</p>

    <div class="demo-block">
      <PerformanceHeader :userInfo="userInfo" />
    </div>

    <div class="doc-section">
      <h3>说明</h3>
      <p>专门用于展示业绩相关的用户信息，包括组织、岗位、电话、邮箱等。</p>
    </div>
  </div>
</template>

<script setup>
  import { ref } from 'vue'
  import PerformanceHeader from '@/components/Performance/PerformanceHeader.vue'

  const userInfo = ref({
    name: '李四',
    description: '直播间：A001',
    avatar: 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png',
    organization: '销售一部',
    position: '主播',
    phone: '13800138000',
    email: 'lisi@example.com'
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
