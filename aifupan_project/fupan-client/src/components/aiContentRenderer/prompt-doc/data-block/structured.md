## type=structured

适用：通用结构化 JSON 数据块（无固定 schema）。

渲染方式：

- 前端会使用通用 JSON 渲染器（`JsonBlock.vue`）进行展示（主要用于“可读展示”，不是强语义 UI）。

### 建议约定（用于提示词）

尽量使用“稳定字段 + 数组列表”的方式表达数据，便于展示与后续扩展：

- `title`：数据块标题（可选）
- `summary`：汇总信息（对象，可选）
- `items`：条目数组（可选）
- `meta`：元信息（对象，可选）

示例：

~~~text
<aifupan-data-block visible="true" type="structured" cacheKey="example">
```json
{
  "title": "监控位配额",
  "summary": { "total": 10, "used": 3, "remain": 7 },
  "items": [
    { "name": "话术质检", "total": 10, "used": 3, "remain": 7 }
  ]
}
```
</aifupan-data-block>
~~~

约束：

- JSON 必须是合法 JSON（不要输出注释、不要输出尾随逗号）
- 数据量不宜过大（避免超长输出影响渲染性能）

