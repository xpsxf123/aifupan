<template>
  <div class="component-doc">
    <h2>NotificationDrawer 消息通知抽屉</h2>
    <p>侧边滑出的消息通知列表。</p>

    <div class="demo-block">
      <el-button @click="visible = true">打开消息通知</el-button>
      <NotificationDrawer v-model="visible" @read="handleRead" />
    </div>

    <div class="doc-section">
      <h3>Events</h3>
      <p>@read(id): 当点击某条消息查看详情时触发。</p>
    </div>
  </div>
</template>

<script setup>
  import { ref } from 'vue'
  import NotificationDrawer from '@/components/NotificationDrawer/index.vue'

  const visible = ref(false)

  const handleRead = (id) => {
    console.log('Read notification:', id)
  }
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
