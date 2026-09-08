<template>
  <div class="component-doc">
    <h2>数据卡片组 (DataCardGroup)</h2>

    <div class="doc-section">
      <h3>使用示例</h3>
      <div class="demo-box">
        <DataCardGroup
          :cards="[
            { title: '测试1', data: { today: 100 }, config: [{ key: 'today', label: '今日' }] },
            { title: '测试2', data: { today: 200 }, config: [{ key: 'today', label: '今日' }] }
          ]"
        />
      </div>
    </div>

    <div class="doc-section">
      <h3>配置文档</h3>
      <table class="doc-table">
        <thead
          ><tr><th>参数名</th><th>说明</th><th>类型</th><th>默认值</th></tr></thead
        >
        <tbody>
          <tr><td>cards</td><td>卡片配置数组 (同 DataCard Props)</td><td>Array</td><td>[]</td></tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
  import DataCardGroup from '@/components/DataCardGroup/index.vue'
</script>

<style scoped>
  .component-doc {
    padding: 20px;
  }
  .doc-section {
    margin-bottom: 30px;
  }
  .demo-box {
    padding: 20px;
    background: #f5f7fa;
  }
  .doc-table {
    width: 100%;
    border-collapse: collapse;
    margin-top: 10px;
  }
  .doc-table th,
  .doc-table td {
    border: 1px solid #dcdfe6;
    padding: 10px;
    text-align: left;
  }
  .doc-table th {
    background-color: #f5f7fa;
  }
</style>
