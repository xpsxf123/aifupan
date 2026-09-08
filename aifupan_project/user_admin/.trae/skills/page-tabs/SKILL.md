# PageTabs Component Skill

## Description

PageTabs 是一个用于页面级内容切换的标签页组件，支持自定义标签名称、Badge 徽标和插槽内容。它封装了 Tab 切换逻辑，使得在页面中使用 Tab 变得简单，无需手动处理 `v-if` 或 `v-show`。

## Component Path

- Component: `src/components/PageTabs/index.vue`
- Documentation: `src/views/ComponentDocs/PageTabs/index.vue`

## Features

- **Flexible Input**: Supports string array `['Tab1', 'Tab2']` or object array `[{ label: 'Tab1', value: 't1' }]`.
- **Automatic Slots**: Generates named slots based on tab values.
- **Badge Support**: Supports displaying a badge (number or text) on tabs.
- **Custom Styling**: Clean, modern design with vertical dividers, referencing the "Live Performance" style.
- **Card Mode**: Supports `card` prop to display as a contained card with shadow and padding.
- **V-Model**: Supports two-way binding for the active tab.

## Usage

### Basic

```vue
<template>
  <PageTabs :tabs="['直播业绩', '直播场次']">
    <template #tab1>
      <!-- Content for 直播业绩 -->
    </template>
    <template #tab2>
      <!-- Content for 直播场次 -->
    </template>
  </PageTabs>
</template>
```

### Card Style

```vue
<template>
  <PageTabs card :tabs="['直播业绩', '直播场次']">
    <template #tab1>
      <!-- Content inside card -->
    </template>
  </PageTabs>
</template>
```
