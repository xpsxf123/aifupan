---
name: 'data-pie-chart'
description: 'ECharts-based Pie Chart component with responsive resizing and custom styling.'
---

# Data Pie Chart

## Description

A wrapper around ECharts to display a Pie Chart.

- Automatically handles resize events.
- Custom tooltip and legend configuration.
- Supports donut chart style with `radius: ['40%', '70%']`.

## Usage

```vue
<template>
  <DataPieChart title="Sales Distribution" :data="chartData" />
</template>

<script setup>
  import { ref } from 'vue'
  const chartData = ref([
    { value: 1048, name: 'Search Engine' },
    { value: 735, name: 'Direct' }
  ])
</script>
```

## Props

- `title` (String): Chart title.
- `data` (Array): Array of objects `{ value: Number, name: String }`.
