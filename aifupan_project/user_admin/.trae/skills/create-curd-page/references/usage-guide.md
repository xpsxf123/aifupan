# CURD Component Usage Guide

## 1. Development Process

1.  **Integrate Component**: Use `Curd` as the main container.
2.  **Standardize**: Ensure all table pages follow the same style.

## 2. Component Configuration

### 2.1 Core Component (Curd)

| Prop                | Type         | Description                                                                  |
| :------------------ | :----------- | :--------------------------------------------------------------------------- |
| `api`               | Object       | API methods: `{ list, add, edit, del, get }`                                 |
| `autoLoad`          | Boolean      | Auto load data on mount (default: `true`)                                    |
| `queryCacheKey`     | String       | Key for caching search params                                                |
| `tableCacheByRoute` | Boolean      | Whether to persist column visibility by current route path (default: `true`) |
| `tableCacheKey`     | String       | Key for caching column visibility; if empty, uses current route path         |
| `dataMap`           | Object       | Response map: `{ list: 'data.list', total: 'data.total' }`                   |
| `searchConfig`      | Array        | Search bar config                                                            |
| `tableColumns`      | Array        | Table columns config                                                         |
| `formConfig`        | Array/Object | Form config                                                                  |

### 2.2 Table Component (CurdTable)

Supports `columns` array:

- `label`: Header text
- `prop`: Data field
- `width`: Column width
- `slotName`: Custom slot name
- `render`: Render function `(row, index) => VNode`
- `type`: Set to `'tag'` for auto el-tag rendering
- `options`: Enum mapping `[{ label, value, type }]`

### 2.3 Search Component (CurdSearch)

Supports `searchConfig` array:

- `label`: Label text
- `prop`: Field name
- `type`: `'input'` (default), `'select'`, `'date'`, `'daterange'`
- `options`: Select options
- `alwaysShow`: Always show (ignore collapse)

### 2.4 Form Component (CurdForm)

Supports `formConfig` array:

- `label`: Field label
- `prop`: Field name
- `type`: `'input'`, `'select'`, `'radio'`, `'date'`, `'textarea'`
- `rules`: Validation rules
- `span`: Grid span (default 24)
- `hidden`: Hidden control `(formData, mode) => boolean`

## 3. Full Example Structure

```vue
<template>
  <Curd
    :api="api"
    :search-config="searchConfig"
    :table-columns="tableColumns"
    :form-config="formConfig"
    query-cache-key="user-list"
    table-cache-by-route
    table-cache-key="user-table"
  >
    <!-- Custom Column -->
    <template #status="{ row }">
      <el-tag :type="row.status ? 'success' : 'info'">
        {{ row.status ? '启用' : '禁用' }}
      </el-tag>
    </template>

    <!-- Custom Search -->
    <template #custom-search>
      <el-input v-model="customVal" />
    </template>
  </Curd>
</template>

<script setup>
  import Curd from '@/components/Curd/index.vue'
  import * as UserApi from '@/api/user'

  // API Mapping
  const api = {
    list: UserApi.getUserList,
    add: UserApi.addUser,
    edit: UserApi.updateUser,
    del: UserApi.deleteUser
  }

  // Search Config
  const searchConfig = [
    { label: '用户名', prop: 'username', placeholder: '请输入用户名' },
    {
      label: '状态',
      prop: 'status',
      type: 'select',
      options: [
        { label: '启用', value: 1 },
        { label: '禁用', value: 0 }
      ]
    }
  ]

  // Table Columns
  const tableColumns = [
    { label: 'ID', prop: 'id', width: 80 },
    { label: '用户名', prop: 'username' },
    { label: '状态', prop: 'status', slotName: 'status' }, // Use slot
    { label: '创建时间', prop: 'createTime' }
  ]

  // Form Config
  const formConfig = [
    { label: '用户名', prop: 'username', rules: [{ required: true, message: '必填' }] },
    {
      label: '状态',
      prop: 'status',
      type: 'radio',
      options: [
        { label: '启用', value: 1 },
        { label: '禁用', value: 0 }
      ]
    }
  ]
</script>
```
