<template>
  <div class="data-card">
    <div class="card-header">
      <div class="title">{{ title }}</div>
      <div class="icon-wrapper" :style="{ backgroundColor: iconBgColor, color: iconColor }">
        <el-icon v-if="icon"><component :is="icon" /></el-icon>
        <slot name="icon" v-else></slot>
      </div>
    </div>

    <div class="card-content">
      <div class="data-row" v-for="(item, index) in displayData" :key="index">
        <div class="row-label">{{ item.label }}</div>
        <div class="row-value-wrapper">
          <span class="row-value">{{ item.formattedValue }}</span>

          <!-- 升降趋势 -->
          <span v-if="item.trend" class="trend-indicator" :class="item.trend">
            {{ item.trendValue }}
            <el-icon v-if="item.trend === 'up'"><Top /></el-icon>
            <el-icon v-if="item.trend === 'down'"><Bottom /></el-icon>
          </span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
  /**
   * @file components/DataCard/index.vue
   * @description 指标数据卡片：展示多行指标值与趋势（上升/下降）
   */
  import { computed, defineProps } from 'vue'
  import { Top, Bottom } from '@element-plus/icons-vue'

  const props = defineProps({
    title: {
      type: String,
      required: true
    },
    icon: {
      type: String,
      default: ''
    },
    iconColor: {
      type: String,
      default: '#409eff'
    },
    iconBgColor: {
      type: String,
      default: '#ecf5ff'
    },
    // 数据对象 { today: 100, yesterday: 90, ... }
    data: {
      type: Object,
      default: () => ({})
    },
    // 配置项
    // [{ key: 'today', label: '今日', unit: 'w' }, ...]
    config: {
      type: Array,
      default: () => [
        { key: 'today', label: '今日' },
        { key: 'yesterday', label: '昨日' },
        { key: 'thisWeek', label: '本周' },
        { key: 'lastWeek', label: '上周' },
        { key: 'thisMonth', label: '本月' },
        { key: 'lastMonth', label: '上月' }
      ]
    }
  })

  /**
   * @description 格式化展示值（支持 w 单位规则）
   * @param {*} value - 原始值
   * @param {string} unit - 单位标识
   * @returns {*}
   */
  const formatValue = (value, unit) => {
    if (value === undefined || value === null) return '-'

    // 特殊处理 'w' 单位：只有大于 10000 才显示 w，否则原样显示
    if (unit === 'w') {
      const num = Number(value)
      if (!isNaN(num) && num >= 10000) {
        return (num / 10000).toFixed(1) + 'w'
      }
      return value // 小于 10000 不加单位，或者加其他单位？按需求是“正常按照原始数据显示”
    } else if (unit) {
      return `${value}${unit}`
    }

    return value
  }

  const displayData = computed(() => {
    return props.config.map((item) => {
      const rawValue = props.data[item.key]
      const formattedValue = formatValue(rawValue, item.unit)

      // 趋势数据: 假设 data 中有对应的 trend 字段，例如：
      // data: { today: 10000, todayTrend: 'up', todayTrendValue: '1.9' }

      // 支持 config 中直接指定 trendKey
      const trendKey = item.trendKey || `${item.key}Trend`
      const trendValueKey = item.trendValueKey || `${item.key}TrendValue`

      return {
        label: item.label,
        value: rawValue,
        formattedValue,
        trend: props.data[trendKey], // 'up' or 'down'
        trendValue: props.data[trendValueKey]
      }
    })
  })
</script>

<style scoped lang="scss">
  .data-card {
    background: #fff;
    border-radius: var(--border-radius-base);
    padding: 20px 20px 20px 30px;
    box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
    display: flex;
    flex-direction: column;
    height: 100%;
    box-sizing: border-box;
  }

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;

    .title {
      font-size: 16px;
      font-weight: 500;
      color: #484a4c;
    }

    .icon-wrapper {
      width: 32px;
      height: 32px;
      border-radius: 8px;
      display: flex;
      justify-content: center;
      align-items: center;
      font-size: 18px;
    }
  }

  .card-content {
    flex: 1;
    display: flex;
    flex-direction: column;
    justify-content: space-between;
    gap: 14px;
    :first-child .row-value {
      color: #191615;
      font-size: 18px;
      font-weight: 500;
    }
  }

  .data-row {
    display: flex;
    justify-content: flex-start;
    align-items: center;
    font-size: 14px;
    .row-label {
      color: #484a4c;
      white-space: nowrap;
    }

    .row-value-wrapper {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 16px;
    }

    .row-value {
      color: #484a4c;
      font-weight: 500;
      padding-left: 25px;
    }

    .trend-indicator {
      display: flex;
      align-items: center;
      font-size: 12px;

      &.up {
        color: #f56c6c; // 红色上升
      }

      &.down {
        color: #67c23a; // 绿色下降
      }

      .el-icon {
        margin-left: 2px;
      }
    }
  }
</style>
