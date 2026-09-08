<template>
  <div class="component-doc">
    <h2>FunnelChart 漏斗图</h2>
    <p>自定义样式的转化漏斗图。</p>

    <div class="demo-block">
      <div style="width: 600px; height: 400px">
        <FunnelChart :data="funnelData" />
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
  import FunnelChart from '@/components/FunnelChart/index.vue'

  const funnelData = ref({
    label: '转化率',
    rate: ['12', '5%'],
    unit: '',
    datas: {
      top: { label: '访问人数', data: 1000, unit: '人' },
      bottom: { label: '下单人数', data: 125, unit: '人' }
    }
  })

  const propsData = [{ name: 'data', desc: '漏斗数据配置', type: 'Object', default: '{}' }]
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
