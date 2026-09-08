---
name: 'date-quick-picker'
description: 'A date picker component enhanced with quick selection buttons (Yesterday, Last 7 Days, etc.).'
---

# Date Quick Picker

## Description

A composite date picker that simplifies date range selection.

- **Quick Buttons**: Predefined ranges (Yesterday, 7 Days, 15 Days, 30 Days).
- **Custom Range**: Standard `el-date-picker` for manual selection.
- **Logic**: Automatically calculates dates based on quick selection.

## Usage

```vue
<template>
  <DateQuickPicker v-model="dateRange" />
</template>

<script setup>
  import { ref } from 'vue'
  const dateRange = ref([]) // ['2023-01-01', '2023-01-07']
</script>
```
