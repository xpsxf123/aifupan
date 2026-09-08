# Images Directory

This directory (`src/assets/images`) is used to store image resources that will be processed by the build tool (Vite). These images can be imported in your Vue components, styles, or scripts.

## Usage

You can import images using the `@` alias (which points to `src`).

### In `<template>`:

```html
<img src="@/assets/images/logo.png" alt="Logo" />
```

### In `<script>`:

```javascript
import logo from '@/assets/images/logo.png'
```

### In SCSS/CSS:

```scss
.background {
  background-image: url('@/assets/images/background.jpg');
}
```

## Public Directory

For static assets that should be served as-is (without processing), use the `public/images` directory. Files in `public` are served at the root path `/`.

Example: `public/images/icon.png` -> `<img src="/images/icon.png" />`

## Structure

- Use subdirectories to organize images by module or feature if necessary.
- Use descriptive file names (kebab-case recommended).
