---
name: 'common-dialog'
description: 'A customized wrapper around el-dialog with consistent styling, header, and close button behavior.'
---

# Common Dialog

## Description

A wrapper for `el-dialog` that provides:

- Consistent header styling with title and close button.
- Default `width` (440px) and `minHeight`.
- `align-center` and `destroy-on-close` enabled by default.
- Custom footer slot styling.

## Usage

```vue
<template>
  <CommonDialog v-model="visible" title="Edit User" width="500px">
    <el-form>...</el-form>

    <template #footer>
      <el-button @click="visible = false">Cancel</el-button>
      <el-button type="primary">Confirm</el-button>
    </template>
  </CommonDialog>
</template>
```

## Props

- `modelValue` (Boolean): Visibility control.
- `title` (String): Dialog title.
- `width` (String|Number): Dialog width (default '440px').
- `minHeight` (String): Content minimum height (default '420px').
