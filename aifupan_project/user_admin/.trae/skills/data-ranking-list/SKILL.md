---
name: 'data-ranking-list'
description: 'A ranking list component displaying top items with medals for top 3 and optional category icons.'
---

# Data Ranking List

## Description

A component to display ranked data (e.g., Top Selling Products, Top Anchors).

- **Visuals**: Medals for 1st, 2nd, 3rd place.
- **Header**: Title with highlighted "TOP" text.
- **Icons**: Supports header icons for categories (chart, chat, video).

## Usage

```vue
<template>
  <DataRankingList title="TOP Sales" :list="rankingList" iconType="video" />
</template>

<script setup>
  const rankingList = [
    { id: 1, name: 'Item A', value: '1000', avatar: 'url' },
    { id: 2, name: 'Item B', value: '800', avatar: 'url' }
  ]
</script>
```
