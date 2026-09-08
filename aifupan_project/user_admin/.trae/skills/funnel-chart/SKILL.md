---
name: 'funnel-chart'
description: 'A custom funnel chart visualization for conversion rates and data comparison.'
---

# Funnel Chart

## Description

A specialized visualization component for displaying conversion funnels.

- Shows conversion rate in the middle.
- Shows top and bottom data values with labels.
- Uses custom images for background and styling.

## Usage

```vue
<template>
  <FunnelChart :data="funnelData" />
</template>

<script setup>
  const funnelData = {
    label: 'Conversion Rate',
    rate: ['10%', '20%'],
    unit: '%',
    datas: {
      top: { label: 'Visitors', data: 1000 },
      bottom: { label: 'Buyers', data: 100 }
    }
  }
</script>
```

## Props

- `data` (Object): Configuration object with `label`, `rate`, `unit`, and `datas` (top/bottom).
