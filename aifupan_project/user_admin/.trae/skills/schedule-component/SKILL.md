---
name: 'schedule-component'
description: 'A comprehensive scheduling component supporting drag-and-drop, cross-day logic, and copy/paste functionality.'
---

# Schedule Component

## Description

A complex scheduling UI component (`src/components/Schedule`) for managing shifts or tasks on a timeline.

- **Drag & Drop**: Supports moving blocks within a day or across days.
- **Cross-Day Logic**: Automatically handles blocks spanning across midnight (splits or extends).
- **Copy/Paste**: Copy a day's schedule and paste it to another day.
- **Read-Only Mode**: Supports `readonly` prop to disable editing.
- **Preview**: Drag preview visualization.

## Usage

```vue
<template>
  <Schedule :data="scheduleData" :readonly="isReadOnly" />
</template>

<script setup>
  import Schedule from '@/components/Schedule/index.vue'
</script>
```

## Data Format

The `data` prop expects an array of daily schedules, each containing roles and time blocks. Refer to `mockData` in `index.vue` for the structure.

## Key Features

- `handleCrossDayDrag`: Core logic for time calculation and block splitting.
- `handleCopyDay` / `handlePasteDay`: Clipboard operations for schedule templates.
