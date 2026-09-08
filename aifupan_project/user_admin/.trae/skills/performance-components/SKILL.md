---
name: 'performance-components'
description: 'A set of shared components for performance monitoring and management pages.'
---

# Performance Components

## Description

This skill covers the shared components used in the Performance module (`src/components/Performance`).

- **PerformanceHeader.vue**: A specialized `PageHeaderInfo` for user/department performance headers.
- **LiveDataTab.vue**: Tab content for displaying live streaming data.
- **LiveSessionTab.vue**: Tab content for displaying live session records.
- **constants.js**: Shared constants (e.g., status codes, labels).

## Usage (PerformanceHeader)

```vue
<template>
  <PerformanceHeader :userInfo="currentUser" />
</template>

<script setup>
  import PerformanceHeader from '@/components/Performance/PerformanceHeader.vue'
</script>
```

## Structure

These components are designed to be reused across `PersonalPerformance`, `DepartmentPerformance`, etc., ensuring consistent UI and logic.
