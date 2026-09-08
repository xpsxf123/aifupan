<template>
  <div class="component-doc">
    <h2>GlobalSearch 全局搜索</h2>
    <p>用于顶部的全局搜索输入框。</p>

    <div class="demo-block">
      <GlobalSearch />
    </div>

    <div class="doc-section">
      <h3>说明</h3>
      <p>目前组件内使用模拟数据（setTimeout），选中后会触发 router.push 跳转。</p>
    </div>
  </div>
</template>

<script setup>
  import GlobalSearch from '@/components/GlobalSearch/index.vue'
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
