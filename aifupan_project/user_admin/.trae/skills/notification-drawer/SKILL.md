---
name: 'notification-drawer'
description: 'A notification drawer component with list and detail views for system messages.'
---

# Notification Drawer

## Description

A drawer component for displaying notifications.

- Supports two views: **List** (overview) and **Detail** (full content).
- Tracks "read" status (local state).
- Emits `read` event when a notification is clicked.

## Usage

```vue
<template>
  <NotificationDrawer v-model="visible" @read="handleRead" />
</template>

<script setup>
  const handleRead = (id) => {
    console.log('Notification read:', id)
  }
</script>
```

## Props

- `modelValue` (Boolean): Controls drawer visibility.

## Events

- `update:modelValue`: Sync visibility.
- `read`: Emitted with notification ID when viewed.
