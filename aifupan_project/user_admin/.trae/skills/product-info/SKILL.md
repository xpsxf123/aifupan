---
name: 'product-info'
description: 'A comprehensive product display component with image, name, price, shop info, and tooltip support.'
---

# Product Info

## Description

A specialized component for displaying product details in lists or cards.

- **Layout**: Image on left, details on right.
- **Overflow Handling**: Automatically detects text overflow for Name and Shop Name, enabling tooltips only when necessary.
- **Visuals**: Price highlighting, Shop icon, Rating display.

## Usage

```vue
<template>
  <ProductInfo image="url" name="Product Name" price="99.00" shopName="Shop A" shopRating="4.9" />
</template>
```
