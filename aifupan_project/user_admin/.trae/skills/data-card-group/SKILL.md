---
name: 'data-card-group'
description: 'A grid layout wrapper for rendering multiple DataCard components from a configuration array.'
---

# Data Card Group

## Description

A wrapper component that renders a responsive grid of `DataCard` components.

- **Responsive**: Adapts to screen size (xs, sm, md, lg, xl).
- **Slot Forwarding**: Supports dynamic slots for card icons.

## Usage

```vue
<template>
  <DataCardGroup :cards="cardsConfig">
    <template #icon-0>
      <img src="custom-icon.png" />
    </template>
  </DataCardGroup>
</template>

<script setup>
  const cardsConfig = [
    { title: 'Card 1', data: {...}, config: [...] },
    { title: 'Card 2', data: {...}, config: [...] }
  ]
</script>
```
