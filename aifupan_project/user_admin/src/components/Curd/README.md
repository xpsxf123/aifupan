# Curd Core Component

A comprehensive CRUD (Create, Read, Update, Delete) component that integrates Search, Table, Pagination, and Form.

## Features

- **Integrated CRUD**: Handles the entire lifecycle of data management.
- **Configurable**: Fully data-driven configuration for Search, Table, and Forms.
- **Caching**: Remembers search parameters, pagination, and column visibility.
- **Extensible**: Slots for every part of the UI (Search, Toolbar, Columns, Form).
- **API Integration**: Hooks for request/response transformation.

## Option Data Format

For select/radio/checkbox options, the recommended normalized format is:

```js
{ key: '2039560415851450371', label: '剪辑' }
```

Compatible legacy formats are also supported (automatically normalized): `{ value, label }`, `{ id, name }`.

## Usage

```vue
<template>
  <Curd
    :api="api"
    :search-config="searchConfig"
    :table-columns="tableColumns"
    :form-config="formConfig"
    query-cache-key="user-list"
    table-cache-key="user-table"
  />
</template>

<script setup>
  import Curd from '@/components/Curd/index.vue'
  import { getUserList, addUser, updateUser, deleteUser } from '@/api/user'

  const api = {
    list: getUserList,
    add: addUser,
    edit: updateUser,
    del: deleteUser
  }

  const searchConfig = [{ label: 'Name', prop: 'name' }]

  const tableColumns = [
    { label: 'Name', prop: 'name' },
    { label: 'Age', prop: 'age' }
  ]

  const formConfig = [
    { label: 'Name', prop: 'name', rules: [{ required: true, message: 'Required' }] },
    { label: 'Age', prop: 'age', type: 'input', inputType: 'number' }
  ]
</script>
```

## Props

### Core

| Name | Type | Description | |Data | ---- | ----------- | | api | Object | API methods: `{ list, add, edit, del, get }` | | autoLoad | Boolean | Auto load data on mount (default: `true`) | | queryCacheKey | String | Key for caching search/pagination params | | tableCacheKey | String | Key for caching column visibility | | beforeRequest | Function | Hook before API call: `(params) => newParams` | | afterRequest | Function | Hook after API call: `(res) => { list, total }` | | dataMap | Object | Response mapping: `{ list: 'data.list', total: 'data.total' }` |

### UI Config

| Name | Type | Description | |Data | ---- | ----------- | | searchConfig | Array | Configuration for Search component | | tableColumns | Array | Configuration for Table columns | | formConfig | Array/Object | Configuration for Form fields | | actionConfig | Object | Toggle default actions: `{ view: true, edit: true, del: true }` | | customActions | Array | Custom buttons in operation column | | pagination | Boolean | Enable pagination (default: `true`) | | dialogWidth | String | Width of the Form dialog (default: '50%') |

## Slots

- **Search**: `search-action`, and any slot defined in `searchConfig`.
- **Table**: `toolbar-left`, `toolbar-right`, and any slot defined in `tableColumns`.
- **Form**: Any slot defined in `formConfig`.

## Sub-Components

This module consists of independent components that can be used separately:

- [Search](./Search/README.md)
- [Table](./Table/README.md)
- [Pagination](./Pagination/README.md)
- [Form](./Form/README.md)
