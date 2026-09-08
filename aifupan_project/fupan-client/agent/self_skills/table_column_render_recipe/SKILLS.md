# table_column_render_recipe

## 本技能用途

- 统一 `Table/Column` 的列配置与渲染方式，明确什么时候用 slot、formatter、option.render。
- 输出可直接复制的 column 配置片段，保证与项目现有 Column 实现一致。

## 适用场景

- CoreTable/Table 的 column 配置编写与重构
- 需要多行展示、拼接展示、带点击事件的单元格渲染

## 输入

- 字段：prop/label/宽度/对齐
- 展示形态：纯文本 / 多行 / 富文本 / 点击交互
- 数据类型：时间/金额/状态枚举/对象

## 输出

- 推荐渲染方案（slot / formatter / option.render）
- 对应 column 配置片段

## 规则（优先级）

1. 最简单：`formatter(row)` 返回字符串
2. 结构化但不需要响应式：`option.render(row)` 返回 HTML 字符串（Column 内 v-html）
3. 需要组件能力/复杂交互：使用 slot（插槽名 = prop）

## 配置片段（可直接复用）

### formatter（简单格式化）

```js
{
  label: '话术',
  prop: 'script',
  formatter: (row) => (row.analysisStatus === 2 ? '导出' : '-'),
}
```

### option.render（复杂结构，多行展示）

```js
{
  label: '录制时间',
  prop: 'startTime',
  option: {
    width: '150px',
    render: (row) => {
      return (`<div class="recordColContainer">
        <div>${row?.startTime?.substring(0, 16)}</div>
        <div style="line-height: 0.6;">~</div>
        <div>${row?.endTime?.substring(0, 16)}</div>
      </div>`)
    },
  },
}
```

### slot（需要复杂交互）

```js
{
  label: '操作',
  prop: 'menu',
  slot: true,
}
```

```vue
<template #menu="{ row }">
  <el-button type="text" @click="handleEdit(row)">编辑</el-button>
</template>
```

## 组件与实现位置

- Table：`src/components/Table/index.vue`
- Column：`src/components/Table/column.vue`

