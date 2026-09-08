---
name: 'icon-image'
description: "A dynamic image loader component for loading icons from 'src/assets/images/icon' using import.meta.glob."
---

# Icon Image

## Description

A utility component to dynamically load and display images from a specific directory (`src/assets/images/icon`) without manual imports.

- **Mechanism**: Uses Vite's `import.meta.glob` to resolve paths at runtime.
- **Props**: `name` (filename without extension), `ext` (default png), `width`, `height`.

## Usage

```vue
<template>
  <!-- Loads src/assets/images/icon/user.png -->
  <IconImage name="user" width="24px" />
</template>
```
