# SchemaForm Usage Guide

## Component Path

`src/components/SchemaForm/index.vue`

## Basic Usage

```vue
<template>
  <SchemaForm v-model="formData" :schema="schema" :state="currentState" />
</template>
```

## Schema Configuration

### Field Configuration

```javascript
{
  prop: 'username',
  label: 'Username',
  type: 'input', // select, date, etc.
  span: 12,

  // State Logic
  visible: ['add', 'view'], // Only visible in these states (Shortcuts)
  readonly: ['view'],       // Readonly in these states
  disabled: ['edit'],       // Disabled in these states

  // Advanced State Logic (Custom states)
  stateConfig: {
    customState: { visible: true, readonly: true },
    add: { visible: true, readonly: false }
  },

  // Slot Logic
  slot: true, // Uses slot name 'username'
  // OR
  slot: ['add', 'view'], // Uses 'add-username', 'view-username'
  // OR
  slot: '*' // Uses '{state}-username' for all states
}
```

### Grouping

```javascript
;[
  {
    title: 'Basic Info',
    children: [...fields]
  },
  {
    title: 'Permissions',
    children: [...fields]
  }
]
```

## Special Requirements Implementation

### Permanent Read-only

Visible only in View mode.

```javascript
{
  prop: 'id',
  visible: ['view']
}
```

### Add Read-only (Add/View available, Edit Readonly)

Editable in Add, Readonly in View (default), Readonly in Edit.

```javascript
{
  prop: 'code',
  visible: ['add', 'view', 'edit'],
  readonly: ['view', 'edit']
}
```

### Not Editable (Only Add and View)

Hidden in Edit mode.

```javascript
{
  prop: 'type',
  visible: ['add', 'view']
}
```

## Action Buttons

Add action buttons (e.g., Edit, Reset) to form fields. Supports single or multiple buttons.

### Configuration

```javascript
{
  prop: 'phone',
  label: 'Phone',

  // Single Button
  actionButton: {
    text: 'Modify',
    icon: 'Edit',
    show: ['view'], // Only show in 'view' state
    onClick: () => { console.log('Modify clicked') }
  },

  // Multiple Buttons
  actionButton: [
    {
      text: 'Edit',
      icon: 'Edit',
      show: ['view'],
      // Example: Switch field to edit mode
      onClick: () => { /* Logic handled via action event */ }
    },
    {
      text: 'Reset',
      icon: 'Refresh',
      show: ['edit'],
      type: 'warning'
    }
  ]
}
```

### Handling Actions

The `SchemaForm` component emits `action-click` with `{ prop, button, actions }`.

- `actions.edit()`: Switch field to edit mode.
- `actions.view()`: Switch field to view mode.

```javascript
<SchemaForm @action-click="handleAction" />

const handleAction = ({ prop, button, actions }) => {
  if (button.text === 'Edit') {
    actions.edit() // Toggle local state to edit
  }
}
```
