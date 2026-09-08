<template>
  <div class="pie-chart-card">
    <div class="chart-header">
      <div class="title">{{ title }}</div>
    </div>
    <div ref="chartRef" class="chart-container"></div>
  </div>
</template>

<script setup>
  /**
   * @file components/DataPieChart/index.vue
   * @description 环形饼图组件（ECharts）：用于展示占比类数据
   */
  import { ref, onMounted, onUnmounted, watch } from 'vue'
  import * as echarts from 'echarts'

  const props = defineProps({
    title: {
      type: String,
      default: '分公司占比'
    },
    data: {
      type: Array,
      default: () => []
    }
  })

  const chartRef = ref(null)
  let chartInstance = null

  /**
   * @description 初始化并渲染图表
   */
  const initChart = () => {
    if (!chartRef.value) return

    chartInstance = echarts.init(chartRef.value)

    const option = {
      tooltip: {
        trigger: 'item'
      },
      legend: {
        bottom: '0%',
        left: 'center',
        icon: 'circle'
      },
      series: [
        {
          name: props.title,
          type: 'pie',
          radius: ['40%', '70%'],
          center: ['50%', '45%'], // Move chart up slightly to fit legend
          avoidLabelOverlap: false,
          itemStyle: {
            borderRadius: 10,
            borderColor: '#fff',
            borderWidth: 2
          },
          label: {
            show: false,
            position: 'center'
          },
          emphasis: {
            label: {
              show: true,
              fontSize: 20,
              fontWeight: 'bold'
            }
          },
          labelLine: {
            show: false
          },
          data: props.data
        }
      ]
    }

    chartInstance.setOption(option)
  }

  /**
   * @description 窗口尺寸变化时触发图表自适应
   */
  const resizeHandler = () => {
    chartInstance?.resize()
  }

  watch(
    () => props.data,
    () => {
      initChart()
    },
    { deep: true }
  )

  onMounted(() => {
    initChart()
    window.addEventListener('resize', resizeHandler)
  })

  onUnmounted(() => {
    window.removeEventListener('resize', resizeHandler)
    chartInstance?.dispose()
  })
</script>

<style scoped lang="scss">
  .pie-chart-card {
    background: #fff;
    border-radius: 8px;
    padding: 20px;
    height: 100%;
    box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
    display: flex;
    flex-direction: column;
  }

  .chart-header {
    margin-bottom: 10px;
  }

  .title {
    font-size: 16px;
    font-weight: bold;
    color: #303133;
  }

  .chart-container {
    flex: 1;
    min-height: 300px;
  }
</style>
