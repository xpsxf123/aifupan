<template>
  <div class="component-doc">
    <h2>数据趋势图 (DataTrendChart)</h2>

    <div class="doc-section">
      <h3>使用示例</h3>
      <div class="demo-box">
        <DataTrendChart
          :chart-data="{
            xData: ['1/1', '1/2', '1/3'],
            yData: [120, 200, 150]
          }"
          unit="w"
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
          <tr><td>chartData</td><td>图表数据 {xData, yData}</td><td>Object</td><td>{x:[], y:[]}</td></tr>
          <tr><td>barColor</td><td>柱状图颜色</td><td>String</td><td>'#f6bd16'</td></tr>
          <tr><td>unit</td><td>Y轴单位</td><td>String</td><td>''</td></tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
  import DataTrendChart from '@/components/DataTrendChart/index.vue'
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
    background: #fff;
    border: 1px solid #eee;
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
