<template>
  <div class="live-room-stats">
    <div v-for="(item, index) in config" :key="index" class="stat-item">
      <div class="stat-label">{{ item.label }}</div>
      <div class="stat-value" :style="{ color: item.color }">
        {{ formatValue(data[item.prop], item.unit, item) }}
      </div>
      <div class="stat-compare">
        <div v-if="item.yesterday" class="compare-row">
          <div class="compare-label">
            <span class="dot yesterday"></span>
            <span class="label">昨日</span>
          </div>
          <span class="value">{{ formatValue(data[item.yesterday], item.unit, item) }}</span>
        </div>
        <div v-if="item.week" class="compare-row">
          <div class="compare-label">
            <span class="dot this-week"></span>
            <span class="label">本周</span>
          </div>
          <span class="value">{{ formatValue(data[item.week], item.unit, item) }}</span>
        </div>
        <div v-if="item.month" class="compare-row">
          <div class="compare-label">
            <span class="dot this-month"></span>
            <span class="label">本月</span>
          </div>
          <span class="value">{{ formatValue(data[item.month], item.unit, item) }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
  /**
   * @file LiveRoomStats.vue
   * @description 直播间数据指标组件，支持自定义单位换算
   */
  import { defineProps } from 'vue'

  const props = defineProps({
    // 数据对象
    data: {
      type: Object,
      default: () => ({})
    },
    // 指标配置
    // [{ label: '场观', prop: 'views', unit: 'w', color: '#333', yesterday: 'yesterdayViews', week: 'weekViews', month: 'monthViews' }]
    config: {
      type: Array,
      default: () => []
    },
    // 单位换算配置
    // [{ value: 10000, label: 'w' }, { value: 100000000, label: '亿' }]
    unitConfig: {
      type: Array,
      default: () => [
        { value: 10000, label: 'w' },
        { value: 100000000, label: '亿' }
      ]
    }
  })

  /**
   * 格式化数值，支持自动单位换算
   * @param {Number|String} val 原始数值
   * @param {String} unitType 是否启用单位换算，若传 'auto' 则启用，否则原样返回或仅添加后缀
   */
  const formatMinutes = (raw) => {
    const minutes = Math.max(0, Math.floor(Number(raw) || 0))
    const h = Math.floor(minutes / 60)
    const m = minutes % 60
    if (h <= 0) return `${m}分钟`
    if (m <= 0) return `${h}小时`
    return `${h}小时${m}分钟`
  }

  const formatSessionByHour = (val) => {
    if (val === undefined || val === null) return '-'

    if (typeof val === 'object' && val !== null) {
      const count = Number(val?.count) || 0
      const minutes = Number(val?.duration) || 0
      const durationText = formatMinutes(minutes)
      return `${count} (${durationText})`
    }

    if (typeof val === 'string') {
      const match = val.match(/^\s*(\d+)\s*\((.*?)\)\s*$/)
      if (!match) return val

      const count = Number(match[1]) || 0
      const durationText = match[2] || ''
      const hourMatch = durationText.match(/(\d+(?:\.\d+)?)\s*小时/)
      const minuteMatch = durationText.match(/(\d+(?:\.\d+)?)\s*(分|分钟)/)
      const totalMinutes = Math.floor((Number(hourMatch?.[1]) || 0) * 60 + (Number(minuteMatch?.[1]) || 0))
      const normalized = formatMinutes(totalMinutes)
      return `${count} (${normalized})`
    }

    return val
  }

  const formatValue = (val, unitType, item) => {
    if (val === undefined || val === null) return '-'

    if (item?.prop === 'session') {
      return formatSessionByHour(val)
    }

    // 如果不需要自动换算，直接返回
    if (unitType !== 'auto') {
      return val + (unitType || '')
    }

    const num = Number(val)
    if (isNaN(num)) return val

    // 从大到小排序配置
    const sortedConfig = [...props.unitConfig].sort((a, b) => b.value - a.value)

    for (const config of sortedConfig) {
      if (num >= config.value) {
        return (num / config.value).toFixed(2) + config.label
      }
    }

    // 没有匹配到单位，直接返回原值
    return num
  }
</script>

<style scoped>
  .live-room-stats {
    display: flex;
    justify-content: space-evenly;
    padding: 22px 40px;
    border-radius: 4px;
    background-color: #f8f8f8;
  }

  .stat-item {
    display: flex;
    align-items: center;
    flex-direction: column;
    justify-content: space-between;
    text-align: center;
    flex: 1;
    white-space: nowrap;
  }

  .stat-item:last-child {
    border-right: none;
  }

  .stat-label {
    font-size: 16px;
    font-weight: 500;
    color: #484a4c;
  }

  .stat-value {
    font-size: 18px;
    font-weight: 500;
    color: #151719;
    margin: 14px 0 18px 0;
  }

  .stat-compare {
    display: flex;
    flex-direction: column;
    gap: 12px;
    font-size: 14px;
  }

  .compare-row {
    display: flex;
    align-items: center;
    column-gap: 16px;
    line-height: 1;
    .compare-label {
      display: flex;
      align-items: center;
      column-gap: 8px;
      .dot {
        content: '';
        display: block;
        width: 6px;
        height: 6px;
        border-radius: 50%;
      }
      .yesterday {
        background-color: #1890ff;
      }
      .this-week {
        background-color: #13c2c2;
      }
      .this-month {
        background-color: #faad14;
      }
    }
  }

  .compare-row .label {
    color: #909399;
  }

  .compare-row .value {
    color: #606266;
  }
</style>
