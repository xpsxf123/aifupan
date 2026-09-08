# Form Component

A dynamic form component inside a dialog, supporting Add, Edit, and View modes.

## Option Data Format

Recommended:

```js
{ key: '2039560415851450371', label: '剪辑' }
```

Compatible legacy formats: `{ value, label }`, `{ id, name }`.

## Usage

```vue
<template>
  <CurdForm
    v-model:visible="dialogVisible"
    :mode="currentMode"
    :form-config="formConfig"
    v-model="currentItem"
    @submit="handleSubmit"
  />
</template>
```

## Props

| Name | Type | Default | Description | |Data | ---- | ------- | ----------- | | visible | Boolean | false | Dialog visibility | | mode | String | 'add' | Current mode: 'add', 'edit', 'view' | | formConfig | Array/Object | [] | Field configuration. Can be an array or object `{ add: [], edit: [] }` | | modelValue | Object | {} | Form data object | | loading | Boolean | false | Loading state for submit button | | width | String | '50%' | Dialog width |

## Events

| Name | Parameters | Description | |Data | ---------- | ----------- | | update:visible | (val) | Triggered when visibility changes | | submit | (formData) | Triggered when form is valid and submitted | | close | () | Triggered when dialog is closed |

## Field Configuration

| Property | Type | Description | |Data | ---- | ----------- | | label | String | Field label | | prop | String | Field property name | | type | String | 'input', 'select', 'radio', 'date', 'textarea' | | rules | Array | Validation rules (Element Plus format) | | options | Array | Options for select/radio | | span | Number | Grid span (default 24) | | hidden | Boolean/Function | Whether to hide the field. `(formData, mode) => boolean` | | slotName | String | Slot name for custom content |

## Group Layout

You can group fields into sections by using a `type: 'group'` item.

```js
{
  type: 'group',
  title: '基础信息',
  span: 24,
  childSpan: 12,
  children: [
    { label: '姓名', prop: 'name', type: 'input' },
    { label: '手机号', prop: 'mobile', type: 'input' }
  ]
}
```

## Select Empty Action

When a `select` field has no options, you can show an extra action button next to the select.

### Route Mode

```js
{
  label: '所属公司',
  prop: 'companyId',
  type: 'select',
  options: companyOptions,
  emptyAction: {
    mode: 'route',
    text: '去添加子公司',
    to: '/department-staff/subsidiary'
  }
}
```

Behavior:

- Click action button → saves current form draft locally (by Curd) and navigates to `to`
- When returning to the original route → prompts whether to continue last operation

### Callback Mode

```js
{
  label: '所属公司',
  prop: 'companyId',
  type: 'select',
  options: companyOptions,
  emptyAction: {
    mode: 'callback',
    text: '去添加',
    callback: ({ formData }) => {
      // implement your own modal / add flow here
      console.log(formData)
    }
  }
}
```
