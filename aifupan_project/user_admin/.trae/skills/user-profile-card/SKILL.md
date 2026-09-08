---
name: 'user-profile-card'
description: 'A card component for displaying user profile information in a structured layout.'
---

# User Profile Card

## Description

A display component for user information, typically used on profile or dashboard pages.

- **Left Section**: Avatar, Name, Live Room/Description.
- **Right Section**: Grid of details (Organization, Position, Phone, Email).

## Usage

```vue
<template>
  <UserProfileCard :userInfo="user" />
</template>

<script setup>
  const user = {
    name: 'John Doe',
    avatar: 'url',
    organization: 'Tech Dept',
    position: 'Manager',
    phone: '1234567890',
    email: 'john@example.com'
  }
</script>
```

## Props

- `userInfo` (Object): The user data object.
