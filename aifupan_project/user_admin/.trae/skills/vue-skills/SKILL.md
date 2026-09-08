---
name: 'vue-skills'
description: 'Vue 3 开发全栈最佳实践，涵盖 Composition API、Pinia 状态管理、Vue Router 路由及组件设计规范。在开发 Vue 功能时调用。'
---

# Vue 3 开发最佳实践 (Vue Skills)

本技能集合了 Vue 3 生态系统的核心开发规范，旨在提升代码质量、可维护性和性能。

## 1. Composition API 核心规范

- **语法糖**：强制使用 `<script setup>` 语法。
- **响应式数据**：
  - 优先使用 `ref` 定义基本类型和对象（保持一致性，访问时需 `.value`）。
  - 仅在确实需要解构且保持响应性时使用 `reactive`。
- **计算属性**：使用 `computed` 缓存复杂逻辑。
- **生命周期**：使用 `onMounted`, `onUnmounted` 等钩子，避免使用 Options API 的生命周期。

## 2. 组件设计 (Component Design)

- **Props 定义**：使用 `defineProps` 并提供详细的类型定义和默认值。
  ```javascript
  const props = defineProps({
    modelValue: { type: String, required: true },
    disabled: { type: Boolean, default: false }
  })
  ```
- **Emits 定义**：使用 `defineEmits` 明确声明所有发出的事件。
- **组件命名**：文件名使用大驼峰 (PascalCase)，如 `UserProfile.vue`。
- **插槽**：使用具名插槽传递复杂内容。

## 3. 状态管理 (Pinia)

- **Store 定义**：使用 Setup Store 语法（`defineStore` + 函数）。
  ```javascript
  export const useUserStore = defineStore('user', () => {
    const count = ref(0)
    const doubleCount = computed(() => count.value * 2)
    function increment() {
      count.value++
    }
    return { count, doubleCount, increment }
  })
  ```
- **持久化**：对于需要保留的状态（如 Token），结合 `localStorage` 或插件使用。
- **解构**：在组件中使用 `storeToRefs` 解构状态，保持响应性。

## 4. 路由管理 (Vue Router)

- **路由守卫**：在 `router.beforeEach` 中处理权限验证和重定向。
- **动态路由**：使用 `addRoute` 动态加载权限路由。
- **懒加载**：路由组件必须使用动态导入 `component: () => import(...)`。

## 5. 性能优化

- **v-if vs v-show**：频繁切换使用 `v-show`，条件渲染使用 `v-if`。
- **Key 管理**：`v-for` 必须绑定唯一的 `key`（避免使用 index）。
- **异步组件**：对于大型组件使用 `defineAsyncComponent`。

## 6. 组合式函数 (Composables)

- **命名**：以 `use` 开头，如 `useTheme`。
- **封装**：将业务逻辑提取到 `src/hooks/` 或 `src/composables/` 中。
- **返回值**：返回响应式对象或方法，方便组件解构。

## 7. 模板规范

- **指令简写**：使用 `@` (v-on) 和 `:` (v-bind)。
- **自闭合**：对于无内容的组件使用自闭合标签 `<MyComponent />`。
