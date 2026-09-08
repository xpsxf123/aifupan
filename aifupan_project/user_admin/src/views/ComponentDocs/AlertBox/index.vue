<template>
  <div class="component-doc">
    <h2>AlertBox 警告框</h2>
    <p>用于分公司管理员的静态警告提示组件。</p>

    <div class="demo-block">
      <AlertBox />
    </div>

    <div class="doc-section">
      <h3>说明</h3>
      <p>目前该组件内容是静态的，专门用于显示“当前为子公司管理员...”的提示信息。</p>
    </div>
  </div>
</template>

<script setup>
  import AlertBox from '@/components/AlertBox/index.vue'
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
