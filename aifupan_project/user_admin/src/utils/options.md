## 下拉选项数据规范

### 统一格式

所有下拉（select/radio/checkbox）选项统一使用：

```js
{ key: '2039560415851450371', label: '剪辑' }
```

### 兼容字段

为兼容历史接口或手写数据，以下格式也会被自动归一化为 `{ key, label }`：

- `{ value, label }`
- `{ id, name }`

### 推荐做法

- 页面层拿到接口数据后，优先直接使用后端返回的 `{ key, label }`，不要再手动转换成 `{ value, label }`。
- 需要兼容多种字段时，使用工具函数 `normalizeKeyLabelOptions()`。
