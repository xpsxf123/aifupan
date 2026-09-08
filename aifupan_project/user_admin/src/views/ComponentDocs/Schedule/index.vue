<template>
  <div class="component-doc">
    <h2>Schedule 排班组件</h2>
    <p>支持拖拽、跨天、复制粘贴的排班组件。</p>

    <div class="demo-block">
      <Schedule :data="[]" />
    </div>

    <div class="doc-section">
      <h3>交互说明</h3>
      <ul>
        <li>支持左右拖拽调整时间。</li>
        <li>支持跨越0点（自动分割或合并）。</li>
        <li>点击“复制”可复制某天的排班，点击“粘贴”应用到另一天。</li>
      </ul>
    </div>
  </div>
</template>

<script setup>
  import Schedule from '@/components/Schedule/index.vue'
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
