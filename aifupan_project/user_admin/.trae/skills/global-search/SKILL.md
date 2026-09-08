---
name: 'global-search'
description: 'A global search component using remote search with mock data capability.'
---

# Global Search

## Description

A search input component typically placed in the header for global navigation or searching.

- Features `el-select` with `remote` search.
- Includes loading state and clearable input.
- Automatically navigates (`router.push`) on selection.
- Currently uses mocked data for demonstration (needs backend integration).

## Usage

```vue
<template>
  <GlobalSearch />
</template>
```

## Implementation Note

The `querySearch` function currently contains mocked data and `setTimeout`. It should be replaced with a real API call in production.
