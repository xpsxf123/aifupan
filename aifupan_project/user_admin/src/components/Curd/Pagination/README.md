# Pagination Component

A wrapper around Element Plus Pagination component.

## Usage

```vue
<template>
  <CurdPagination
    v-show="total > 0"
    :total="total"
    v-model:page="queryParams.page"
    v-model:limit="queryParams.limit"
    @pagination="getList"
  />
</template>
```

## Props

| Name | Type | Default | Description | |Data | ---- | ------- | ----------- | | total | Number | required | Total number of items | | page | Number | 1 | Current page number | | limit | Number | 10 | Page size | | pageSizes | Array | [10, 20, 30, 50] | Options for page size | | layout | String | 'total, sizes, prev, pager, next, jumper' | Layout of pagination | | background | Boolean | true | Whether to use background color | | hidden | Boolean | false | Whether to hide the pagination |

## Events

| Name | Parameters | Description | |Data | ---------- | ----------- | | pagination | { page, limit } | Triggered when page or limit changes | | update:page | (val) | Sync modifier for page | | update:limit | (val) | Sync modifier for limit |
