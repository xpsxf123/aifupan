# LiveRoomCard & LiveRoomList 组件说明

## 简介

`LiveRoomCard` 系列组件用于展示直播间的详细业绩信息，包含头部信息（LiveRoomHeader）和数据指标（LiveRoomStats）。`LiveRoomList` 是基于 Card 的高级列表组件，集成了搜索和滚动功能。

## 组件结构

- `LiveRoomCard/index.vue`: 卡片容器
- `LiveRoomCard/LiveRoomHeader.vue`: 卡片头部
- `LiveRoomCard/LiveRoomStats.vue`: 数据指标区域（支持单位换算）
- `LiveRoomList/index.vue`: 列表容器（含搜索、滚动）

## LiveRoomStats 单位换算配置

通过 `unitConfig` 属性配置单位换算规则。规则格式：`[{ value: Number, label: String }]` 逻辑：

1. 将配置按 value 从大到小排序。
2. 遍历配置，若数值 >= value，则除以 value 并保留2位小数，拼接 label。
3. 若均不匹配，显示原值。

示例：

```javascript
const unitConfig = [
  { value: 10000, label: 'w' },
  { value: 100000000, label: '亿' }
]
// 123456789 -> 1.23亿
// 50000 -> 5.00w
// 5000 -> 5000
```

## 使用示例

```vue
<template>
  <LiveRoomList
    :list="list"
    :total="100"
    height="calc(100vh - 200px)"
    :stats-config="statsConfig"
    :unit-config="unitConfig"
    @search="handleSearch"
  />
</template>

<script setup>
  import LiveRoomList from './components/LiveRoomList/index.vue'

  const statsConfig = [
    { label: '场次', prop: 'session', unit: '' }, // 场次不换算
    { label: '场观', prop: 'views', unit: 'auto' }, // 自动换算
    { label: '销售', prop: 'sales', unit: 'auto' }
  ]

  const unitConfig = [
    { value: 10000, label: 'w' },
    { value: 100000000, label: '亿' }
  ]

  const list = [
    {
      header: {
        name: '国服第一诺手',
        avatar: '...',
        tags: [{ label: '巨量已失效', type: 'danger' }],
        org: '久益公司',
        managers: '鲁浩'
      },
      stats: {
        session: '8场 (8小时)',
        views: 452000,
        sales: 10000000
      }
    }
  ]
</script>
```
