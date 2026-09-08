# 数据模型（Markdown Code）

说明：前端数据实体与字段约束，以 json 代码块提供。

```json
{
  "项目名称": "{project_name}",
  "分析时间": "{date}",
  "数据实体": [
    {
      "实体名称": "{entity_name}",
      "实体代码": "{entity_code}",
      "实体描述": "{entity_desc}",
      "字段定义": [
        {"字段名": "{field}", "字段类型": "字符串|数字|布尔|日期时间|枚举", "业务含义": "{meaning}", "是否必填": true, "是否唯一": false}
      ]
    }
  ]
}
```
