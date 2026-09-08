---
name: 'data-overview'
description: 'A composite component combining a DataTrendChart and a detail table (Curd) with date filtering.'
---

# Data Overview

## Description

A high-level business component that combines:

- **DataTrendChart**: Visual trend analysis.
- **Curd Table**: Detailed data list (read-only mode).
- **DateQuickPicker**: Shared date filtering.

## Usage

```vue
<template>
  <DataOverview :extraParams="{ deptId: 1 }" />
</template>
```

## Features

- **Integrated Logic**: Handles date changes and refreshes both chart and table.
- **Mock API**: Currently contains a mock API implementation (`api` object) which should be replaced by real service calls.
