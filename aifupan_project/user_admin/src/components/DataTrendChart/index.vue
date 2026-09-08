<template>
  <div class="trend-chart-card">
    <div class="chart-header-container">
      <div class="chart-header">
        <div class="text-main fw-500 title-container">
          <div class="name">数据趋势</div>
        </div>
        <div class="subtitle">
          <slot name="subtitle"></slot>
        </div>
      </div>
      <div class="describe">
        <slot name="describe"></slot>
      </div>
    </div>

    <div ref="chartRef" class="chart-container"></div>
  </div>
</template>

<script setup>
  import { ref, onMounted, onUnmounted, watch, defineProps, nextTick, defineEmits } from 'vue'
  import * as echarts from 'echarts'

  const props = defineProps({
    // 图表数据 { xData: [], yData: [] }
    chartData: {
      type: Object,
      default: () => ({ xData: [], yData: [] })
    },
    // 柱状图颜色
    barColor: {
      type: String,
      default: '#f6bd16' // 橙色，参考图片
    },
    unit: {
      type: String,
      default: ''
    }
  })

  const emit = defineEmits(['tab-change'])

  const activeTab = ref('view') // 默认选场观
  const tabs = [
    { label: '场观', value: 'view' },
    { label: '销售额', value: 'sales' },
    { label: '退款', value: 'refund' },
    { label: '净销售', value: 'netSales' },
    { label: '投放', value: 'ads' }
  ]

  const chartRef = ref(null)
  let myChart = null

  const handleTabChange = (val) => {
    activeTab.value = val
    // 这里可以触发事件通知父组件更新数据，或者直接模拟更新
    // 实际项目中应该是 emit('tab-change', val) 然后父组件更新 chartData
    // 为了演示效果，这里直接 emit
    emit('tab-change', val)
  }

  const initChart = () => {
    if (!chartRef.value) return

    myChart = echarts.init(chartRef.value)
    setOption()

    window.addEventListener('resize', resizeChart)
  }

  const setOption = () => {
    if (!myChart) return

    const option = {
      tooltip: {
        trigger: 'axis',
        axisPointer: {
          type: 'shadow' // 阴影指示器
        }
      },
      grid: {
        left: '0',
        right: '0',
        bottom: '3%',
        containLabel: true
      },
      xAxis: [
        {
          type: 'category',
          data: props.chartData.xData,
          axisTick: {
            alignWithLabel: true
          },
          axisLine: {
            lineStyle: {
              color: '#e0e0e0'
            }
          },
          axisLabel: {
            color: '#666'
          }
        }
      ],
      yAxis: [
        {
          type: 'value',
          name: props.unit,
          axisLabel: {
            formatter: (value) => {
              if (value < 10000) return value
              // 进位计算，不保留小数
              return Math.floor(value / 10000) + 'w'
            },
            color: '#999'
          },
          axisLine: {
            show: false
          },
          axisTick: {
            show: false
          },
          splitLine: {
            lineStyle: {
              type: 'dashed',
              color: '#eee'
            }
          }
        },
        {
          type: 'value',
          min: 0,
          max: 25,
          interval: 5,
          axisLabel: {
            formatter: '{value}%',
            color: '#999'
          },
          axisLine: {
            show: false
          },
          axisTick: {
            show: false
          },
          splitLine: {
            show: false
          }
        }
      ],
      series: [
        {
          name: '数值',
          type: 'bar',
          barWidth: '30%', // 柱子宽度
          data: props.chartData.yData,
          itemStyle: {
            color: props.barColor,
            borderRadius: [4, 4, 0, 0] // 顶部圆角
          }
        }
      ]
    }

    myChart.setOption(option)
  }

  const resizeChart = () => {
    myChart?.resize()
  }

  watch(
    () => props.chartData,
    () => {
      setOption()
    },
    { deep: true }
  )

  onMounted(() => {
    nextTick(() => {
      initChart()
    })
  })

  onUnmounted(() => {
    window.removeEventListener('resize', resizeChart)
    myChart?.dispose()
  })
</script>

<style scoped lang="scss">
  .text-main {
    font-weight: bold;
    margin-bottom: 20px;
  }

  .chart-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }

  .title-container {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .header-left {
    display: flex;
    flex-shrink: 0;
    align-items: center;
    gap: 16px;
  }

  .header-right {
    display: flex;
    flex-shrink: 0;
    align-items: center;
  }

  .chart-tabs {
    display: flex;
    border: 1px solid #dcdfe6;
    border-radius: 4px;
    overflow: hidden;
  }

  .chart-container {
    width: 100%;
    height: 350px;
  }
</style>
