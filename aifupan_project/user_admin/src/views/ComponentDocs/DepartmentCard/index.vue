<template>
  <div class="component-doc">
    <h2>部门信息卡片 (DepartmentCard)</h2>

    <div class="doc-section">
      <h3>使用示例</h3>
      <div class="demo-box">
        <DepartmentCard
          title="测试部门"
          :data="{ org: '测试组织', count: 10 }"
          :config="[
            { label: '组织', prop: 'org' },
            { label: '人数', prop: 'count' }
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
          <tr><td>title</td><td>卡片标题</td><td>String</td><td>Required</td></tr>
          <tr><td>icon</td><td>Element Plus 图标名</td><td>String</td><td>''</td></tr>
          <tr><td>data</td><td>数据对象</td><td>Object</td><td>{}</td></tr>
          <tr><td>config</td><td>配置项数组 [{label, prop, width}]</td><td>Array</td><td>[]</td></tr>
          <tr><td>active</td><td>是否激活状态</td><td>Boolean</td><td>false</td></tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
  import DepartmentCard from '@/components/DepartmentCard/index.vue'
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
