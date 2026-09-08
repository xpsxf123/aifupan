<template>
  <div class="component-doc">
    <h2>DataPieChart 数据饼图</h2>
    <p>基于 ECharts 的环形饼图。</p>

    <div class="demo-block">
      <div style="width: 500px; height: 400px">
        <DataPieChart title="用户来源" :data="chartData" />
      </div>
    </div>

    <div class="doc-section">
      <h3>Props</h3>
      <el-table :data="propsData" border>
        <el-table-column prop="name" label="参数" width="150" />
        <el-table-column prop="desc" label="说明" />
        <el-table-column prop="type" label="类型" width="150" />
        <el-table-column prop="default" label="默认值" width="150" />
      </el-table>
    </div>
  </div>
</template>

<script setup>
  import { ref } from 'vue'
  import DataPieChart from '@/components/DataPieChart/index.vue'

  const chartData = ref([
    { value: 1048, name: '搜索引擎' },
    { value: 735, name: '直接访问' },
    { value: 580, name: '邮件营销' },
    { value: 484, name: '联盟广告' },
    { value: 300, name: '视频广告' }
  ])

  const propsData = [
    { name: 'title', desc: '图表标题', type: 'String', default: "'分公司占比'" },
    { name: 'data', desc: '数据源 [{value, name}]', type: 'Array', default: '[]' }
  ]
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
