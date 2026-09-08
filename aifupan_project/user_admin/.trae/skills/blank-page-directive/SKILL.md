---
name: 'blank-page-directive'
description: 'Implements a custom v-empty directive for displaying a configurable empty state overlay with blur effect and action buttons. Invoke when needing to handle empty states or loading placeholders.'
---

# Blank Page Directive (v-empty)

This skill provides a reusable Vue 3 directive `v-empty` that overlays a container with a customizable empty state.

## Features

- **Container Overlay**: Covers any relative positioned container.
- **Backdrop Blur**: Configurable Gaussian blur effect for the background.
- **Custom Content**: customizable title, description, and image.
- **Action Buttons**: Supports multiple buttons with custom types and click callbacks.

## Usage

1. **Ensure Components Exist**:
   - `src/components/EmptyState/index.vue` (The overlay component)
   - `src/directives/empty.js` (The directive logic)

2. **Global Registration** (in `main.js`):

   ```javascript
   import empty from './directives/empty'
   app.directive('empty', empty)
   ```

3. **In Template**:

   ```html
   <div class="container" v-empty="emptyConfig">
     <!-- Content to be covered -->
   </div>
   ```

4. **Configuration Object**:
   ```javascript
   const emptyConfig = reactive({
     visible: true, // Controls visibility
     title: 'No Data',
     description: 'Please add some data to get started.',
     image: '', // Optional: URL to custom image. Defaults to internal SVG if empty.
     blur: 4, // Backdrop blur radius in px
     buttons: [
       {
         text: 'Add Data',
         type: 'primary',
         plain: false, // Optional: hollow style
         round: true, // Optional: round style
         icon: 'Plus', // Optional: icon name
         click: () => {
           console.log('Add clicked')
         }
       }
     ]
   })
   ```

```

## Component Implementation Details

### `src/components/EmptyState/index.vue`
Standard Vue component receiving props. Buttons support all standard `el-button` props: `type`, `size`, `plain`, `round`, `circle`, `icon`, `disabled`, `loading`.

### `src/directives/empty.js`
Uses `createApp` to mount the `EmptyState` component into a dynamic `div` and appends it to the bound element. It handles updates by re-mounting or updating the component instance when the binding value changes.
```
