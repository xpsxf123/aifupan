---
name: 'page-header-info'
description: 'A flexible page header component with title, description, avatar/icon, and customizable stats slots.'
---

# Page Header Info

## Description

A standard page header component used to display primary information about a page or entity (e.g., User Profile, Department Overview).

- **Left**: Icon/Avatar, Title, Description.
- **Right**: Stats area (flexible slot or prop-driven).

## Usage

### Simple Usage

```vue
<PageHeaderInfo
  title="Department A"
  description="Sales Department"
  icon="DataAnalysis"
  :stats="[{ label: 'Sales', value: '1000' }]"
/>
```

### Advanced Usage with Slots

```vue
<PageHeaderInfo title="User Profile">
  <template #icon>
    <img src="avatar.png" />
  </template>
  <template #description>
    <el-tag>Admin</el-tag>
  </template>
  <template #stats>
    <div class="custom-stat">Custom Content</div>
  </template>
</PageHeaderInfo>
```

## Props

- `title` (String)
- `description` (String)
- `icon` (String|Object): Element Plus icon name or component.
- `avatar` (String): Image URL.
- `stats` (Array): `{ label, value, unit }`.
