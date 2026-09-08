---
name: 'sms-code-input'
description: 'An SMS verification code input component with countdown timer and API integration.'
---

# SMS Code Input

## Description

An input component designed for SMS verification codes.

- **Countdown Timer**: Automatically disables the button and counts down after sending.
- **Persistence**: Remembers the countdown state in `localStorage` (via `useSmsSender` hook) even after page refresh.
- **Validation**: Basic phone number format check.
- **API Integration**: Accepts an async `api` function prop to trigger the actual SMS sending.

## Usage

```vue
<template>
  <SmsCodeInput v-model="code" :phone="form.phone" :api="sendSmsApi" smsKey="register-form" />
</template>

<script setup>
  import { sendSmsApi } from '@/api/user'
</script>
```

## Props

- `modelValue` (String): The input code.
- `phone` (String): Target phone number.
- `api` (Function): Async function `(phone) => Promise`.
- `smsKey` (String): Unique key for localStorage persistence (default 'default').
