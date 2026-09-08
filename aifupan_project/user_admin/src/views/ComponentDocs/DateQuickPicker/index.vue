<template>
  <div class="component-doc">
    <h2>日期快捷选择器 (DateQuickPicker)</h2>

    <div class="doc-section">
      <h3>使用示例</h3>
      <div class="demo-box">
        <DateQuickPicker v-model="date" />
        <p>选中值: {{ date }}</p>
      </div>
    </div>

    <div class="doc-section">
      <h3>配置文档</h3>
      <table class="doc-table">
        <thead
          ><tr><th>参数名</th><th>说明</th><th>类型</th><th>默认值</th></tr></thead
        >
        <tbody>
          <tr><td>modelValue</td><td>选中日期范围 (v-model)</td><td>Array</td><td>[]</td></tr>
          <tr><td>quickOptions</td><td>快捷选项 [{label, value}]</td><td>Array</td><td>默认选项</td></tr>
          <tr><td>type</td><td>选择器类型</td><td>String</td><td>'daterange'</td></tr>
          <tr><td>format</td><td>显示格式</td><td>String</td><td>'YYYY-MM-DD'</td></tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
  import { ref } from 'vue'
  import DateQuickPicker from '@/components/DateQuickPicker/index.vue'

  const date = ref([])
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
