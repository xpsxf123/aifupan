# Search Component

The Search component is a configurable form component used for filtering data.

## Features

- **Configurable Fields**: Define search fields using a JSON-like configuration array.
- **Collapse/Expand**: Automatically shows a collapse button if the number of fields exceeds the limit.
- **Custom Slots**: Support for custom form items via slots.
- **Responsive**: Built with Element Plus.

## Option Data Format

Recommended:

```js
{ key: '2039560415851450371', label: '剪辑' }
```

Compatible legacy formats: `{ value, label }`, `{ id, name }`.

## Usage

```vue
<template>
  <CurdSearch v-model="searchParams" :search-config="searchConfig" @search="handleSearch" @reset="handleReset" />
</template>

<script setup>
  import { ref } from 'vue'
  import CurdSearch from '@/components/Curd/Search/index.vue'

  const searchParams = ref({})

  const searchConfig = [
    { label: 'Name', prop: 'name', placeholder: 'Enter name' },
    { label: 'Status', prop: 'status', type: 'select', options: [{ label: 'Active', key: 1 }] },
    { label: 'Created At', prop: 'date', type: 'daterange', alwaysShow: true }
  ]

  const handleSearch = (params) => {
    console.log('Search:', params)
  }

  const handleReset = () => {
    console.log('Reset')
  }
</script>
```

## Props

| Name | Type | Default | Description | |Data | ---- | ------- | ----------- | | searchConfig | Array | [] | Configuration for search fields | | modelValue | Object | {} | The search parameters object (v-model) | | defaultCollapsed | Boolean | true | Whether the form is collapsed by default | | collapseCount | Number | 3 | Number of items to show when collapsed |

## Events

| Name | Parameters | Description | |Data | ---------- | ----------- | | search | (params) | Triggered when the search button is clicked | | reset | () | Triggered when the reset button is clicked | | collapse-change | (isCollapsed) | Triggered when collapse state changes |

## Configuration Item Properties

| Property | Type | Description | |Data | ---- | ----------- | | label | String | Label text | | prop | String | Property name in modelValue | | type | String | Field type: 'input' (default), 'select', 'date', 'daterange' | | placeholder | String | Placeholder text | | options | Array | Options for 'select' type | | width | String | Width of the input/select | | alwaysShow | Boolean | If true, always shown regardless of collapse state | | slotName | String | Name of the slot for custom rendering |
