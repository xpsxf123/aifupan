<template>
  <div class="component-doc">
    <h2>自定义标签页 (CustomTabs)</h2>

    <div class="doc-section">
      <h3>使用示例</h3>
      <div class="demo-box">
        <CustomTabs
          v-model="active"
          :options="[
            { label: 'Tab1', value: 1 },
            { label: 'Tab2', value: 2 }
          ]"
        >
          <template #tools>
            <el-button>工具栏按钮</el-button>
          </template>
        </CustomTabs>
      </div>
    </div>

    <div class="doc-section">
      <h3>配置文档</h3>
      <table class="doc-table">
        <thead
          ><tr><th>参数名</th><th>说明</th><th>类型</th><th>默认值</th></tr></thead
        >
        <tbody>
          <tr><td>modelValue</td><td>选中值 (v-model)</td><td>String | Number</td><td>Required</td></tr>
          <tr><td>options</td><td>选项数组 [{label, value}]</td><td>Array</td><td>[]</td></tr>
        </tbody>
      </table>
      <h4>Slots</h4>
      <table class="doc-table">
        <thead
          ><tr><th>插槽名</th><th>说明</th></tr></thead
        >
        <tbody>
          <tr><td>tools</td><td>右侧工具栏区域</td></tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
  import { ref } from 'vue'
  import CustomTabs from '@/components/CustomTabs/index.vue'

  const active = ref(1)
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
