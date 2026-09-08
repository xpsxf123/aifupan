---
name: 'department-card'
description: 'A card component displaying department or group information with configurable fields.'
---

# Department Card

## Description

A card for displaying structured entity information (like a Department or Group).

- **Header**: Icon and Title.
- **Body**: Configurable list of key-value pairs.
- **Active State**: Visual indication for selected items.

## Usage

```vue
<template>
  <DepartmentCard
    title="Sales Team A"
    :data="deptData"
    :config="[{ label: 'Manager', prop: 'manager' }]"
    :active="isSelected"
    @click="selectDept"
  />
</template>
```
