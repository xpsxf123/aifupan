---
name: 'custom-tabs'
description: 'A custom tab component with a split layout: tabs on the left and a tools slot on the right.'
---

# Custom Tabs

## Description

A tab navigation component designed for page-level or section-level switching.

- **Left**: Scrollable tab items.
- **Right**: `tools` slot for placing actions like date pickers, buttons, etc.
- **Divider**: Visual separator between tab items.

## Usage

```vue
<template>
  <CustomTabs v-model="activeTab" :options="tabOptions">
    <template #tools>
      <el-button>Export</el-button>
    </template>
  </CustomTabs>
</template>

<script setup>
  import { ref } from 'vue'
  const activeTab = ref('overview')
  const tabOptions = [
    { label: 'Overview', value: 'overview' },
    { label: 'Details', value: 'details' }
  ]
</script>
```

## Props

- `modelValue` (String|Number): Active tab value.
- `options` (Array): `{ label, value }`.
