<template>
  <div class="component-doc">
    <h2>数据概览复合组件 (DataOverview)</h2>

    <div class="doc-section">
      <h3>使用示例</h3>
      <div class="demo-box">
        <DataOverview
          :api="api"
          :table-columns="tableColumns"
          :chart-data="chartData"
          :unit="unit"
          @tab-change="handleChartTabChange"
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
          <tr><td>api</td><td>列表接口（Curd 约定：list）</td><td>Object</td><td>-</td></tr>
          <tr><td>tableColumns</td><td>表格列配置</td><td>Array</td><td>[]</td></tr>
          <tr><td>chartData</td><td>趋势图数据</td><td>Object</td><td>{ xData: [], yData: [] }</td></tr>
          <tr><td>unit</td><td>趋势图单位</td><td>String</td><td>w</td></tr>
          <tr><td>extraParams</td><td>额外的查询参数（透传到 list 接口）</td><td>Object</td><td>{}</td></tr>
          <tr><td>showExport</td><td>是否显示导出按钮</td><td>Boolean</td><td>true</td></tr>
          <tr><td>showDatePicker</td><td>是否显示日期筛选</td><td>Boolean</td><td>true</td></tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
  /**
   * @file views/ComponentDocs/DataOverview/index.vue
   * @description DataOverview 组件文档与示例
   */
  import { onMounted, reactive, ref } from 'vue'
  import DataOverview from '@/components/DataOverview/index.vue'

  const unit = ref('w')
  const chartData = reactive({
    xData: [],
    yData: []
  })

  const generateChartData = () => {
    const days = 30
    const x = []
    const y = []
    for (let i = 1; i <= days; i++) {
      x.push(`1/${i}`)
      y.push(Math.floor(Math.random() * 800) + 200)
    }
    chartData.xData = x
    chartData.yData = y
  }

  const handleChartTabChange = (val) => {
    generateChartData()
    unit.value = val === 'view' ? 'w' : val === 'ads' ? '' : 'w'
  }

  const api = {
    list: () => {
      const list = Array.from({ length: 10 }, (_, i) => ({
        id: i,
        date: '2026/1/22',
        view: 263,
        salesAmount: '750-1000w',
        refund: '1,562',
        netSales: '755w',
        ads: '6316',
        roi: '6316'
      }))
      return Promise.resolve({
        code: 200,
        data: {
          list,
          total: 30
        }
      })
    }
  }

  const tableColumns = [
    { label: '日期', prop: 'date', sortable: true },
    { label: '场观', prop: 'view', sortable: true },
    { label: '销售额', prop: 'salesAmount', sortable: true },
    { label: '退款', prop: 'refund', sortable: true },
    { label: '净销售额', prop: 'netSales', sortable: true },
    { label: '投放', prop: 'ads', sortable: true },
    { label: 'ROI', prop: 'roi', sortable: true }
  ]

  onMounted(() => {
    generateChartData()
  })
</script>

<style scoped lang="scss">
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
