# Table Component

A highly configurable table component based on Element Plus Table.

## Features

- **Dynamic Columns**: Configure columns via props.
- **Custom Rendering**: Supports Slots, Render Functions, and Formatters.
- **Column Visibility**: Users can toggle column visibility, persisted to localStorage.
- **Toolbar**: Built-in Add button, Refresh, and Column Settings.
- **Operation Column**: Configurable default actions (Edit, Delete, View) and custom buttons.

## Usage

```vue
<template>
  <CurdTable
    :data="tableData"
    :columns="columns"
    :loading="loading"
    cache-key="user-table"
    @add="handleAdd"
    @action="handleAction"
    @refresh="getData"
  >
    <!-- Custom Slot for a column -->
    <template #status="{ row }">
      <el-tag :type="row.status ? 'success' : 'danger'">
        {{ row.status ? 'Active' : 'Inactive' }}
      </el-tag>
    </template>
  </CurdTable>
</template>
```

## Props

| Name | Type | Default | Description | |Data | ---- | ------- | ----------- | | columns | Array | [] | Column configuration | | data | Array | [] | Table data | | loading | Boolean | false | Loading state | | selection | Boolean | false | Show selection column | | index | Boolean | false | Show index column | | showAdd | Boolean | true | Show Add button in toolbar | | showOperation | Boolean | true | Show Operation column | | operationWidth | String/Number | '200' | Width of Operation column | | actionConfig | Object | { view: true, edit: true, del: true } | Default actions visibility | | customActions | Array | [] | Custom action buttons | | cacheKey | String | '' | Unique key for caching column settings |

## Events

| Name | Parameters | Description | |Data | ---------- | ----------- | | add | () | Triggered when Add button is clicked | | refresh | () | Triggered when Refresh button is clicked | | selection-change | (selection) | Triggered when selection changes | | action | { type, row } | Triggered when an operation button is clicked |

## Column Configuration

| Property | Type | Description | |Data | ---- | ----------- | | label | String | Header text | | prop | String | Data field | | width | String/Number | Column width | | slotName | String | Slot name for custom content | | render | Function | Render function `(row, index) => VNode` | | formatter | Function | Formatter function `(row, col) => string` | | options | Array | Enum options for automatic label mapping: `[{ label, value, type }]` | | type | String | Set to `'tag'` to use el-tag rendering | | tag | Boolean | Set to `true` to use el-tag rendering | | tagProps | Object/Function | Props for el-tag. Object or `(row, val) => object` | | fixed | Boolean/String | Fixed column ('left', 'right') |
