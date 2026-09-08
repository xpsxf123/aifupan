---
name: 'data-trend-chart'
description: 'A bar chart component with tab-based data switching (View, Sales, Refund, etc.) and unit formatting.'
---

# Data Trend Chart

## Description

A business component that renders a bar chart using ECharts.

- **Tabs**: Built-in tabs (View, Sales, Refund, Net Sales, Ads) to switch data context.
- **Formatting**: Auto-formats Y-axis values (e.g., adds 'w' for >10k).
- **Responsive**: Adapts to container resize.

## Usage

```vue
<template>
  <DataTrendChart :chart-data="{ xData: ['Mon', 'Tue'], yData: [120, 200] }" unit="w" @tab-change="handleTabChange" />
</template>

<script setup>
  const handleTabChange = (tabValue) => {
    // Fetch new data based on tabValue
  }
</script>
```
