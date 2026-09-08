<template>
  <div class="component-doc">
    <h2>数据卡片 (DataCard)</h2>

    <div class="doc-section">
      <h3>使用示例</h3>
      <div class="demo-box" style="width: 250px">
        <DataCard
          title="销售额"
          icon="Money"
          :data="{ today: 12345, todayTrend: 'up', todayTrendValue: '1.2' }"
          :config="[{ key: 'today', label: '今日', unit: 'w' }]"
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
          <tr><td>title</td><td>卡片标题</td><td>String</td><td>Required</td></tr>
          <tr><td>icon</td><td>Element Plus 图标名</td><td>String</td><td>''</td></tr>
          <tr><td>iconColor</td><td>图标颜色</td><td>String</td><td>'#409eff'</td></tr>
          <tr><td>iconBgColor</td><td>图标背景色</td><td>String</td><td>'#ecf5ff'</td></tr>
          <tr><td>data</td><td>数据对象</td><td>Object</td><td>{}</td></tr>
          <tr><td>config</td><td>配置数组 [{key, label, unit}]</td><td>Array</td><td>Default Config</td></tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
  import DataCard from '@/components/DataCard/index.vue'
  import { Money } from '@element-plus/icons-vue'
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
