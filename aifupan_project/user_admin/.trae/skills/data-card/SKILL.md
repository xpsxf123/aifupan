---
name: 'data-card'
description: 'A data display card showing key metrics with trend indicators (up/down) and configurable fields.'
---

# Data Card

## Description

A single card component for displaying a set of related metrics (e.g., Sales Data).

- **Header**: Title and Icon.
- **Content**: List of data rows with Label, Value, and Trend Indicator.
- **Trend**: Automatically displays Up/Down arrows based on data.

## Usage

```vue
<template>
  <DataCard title="Total Sales" icon="Money" :data="salesData" :config="cardConfig" />
</template>

<script setup>
  const salesData = { today: 1000, yesterday: 900, todayTrend: 'up' }
  const cardConfig = [
    { key: 'today', label: 'Today', unit: '$' },
    { key: 'yesterday', label: 'Yesterday', unit: '$' }
  ]
</script>
```
