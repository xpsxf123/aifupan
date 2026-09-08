---
name: 'breadcrumb'
description: 'Breadcrumb navigation component that automatically generates breadcrumbs based on Vue Router configuration.'
---

# Breadcrumb

## Description

A breadcrumb navigation component that listens to route changes and generates breadcrumb items based on `route.matched`.

- Ignores routes without `meta.title`.
- Ignores routes with `meta.breadcrumb: false`.
- Supports `redirect: 'noRedirect'` to make items non-clickable.

## Usage

```vue
<template>
  <Breadcrumb />
</template>

<script setup>
  import Breadcrumb from '@/components/Breadcrumb/index.vue'
</script>
```

## Configuration

In your router configuration:

```javascript
{
  path: '/dashboard',
  meta: {
    title: 'Dashboard',
    breadcrumb: true // default is true
  }
}
```
