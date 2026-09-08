# 交互流程（Markdown Code）

说明：页面交互行为定义，使用 json 代码块。

```json
{
  "项目名称": "{project_name}",
  "分析时间": "{date}",
  "交互定义": [
    {
      "页面ID": "{page_id}",
      "页面名称": "{page_name}",
      "交互行为": [
        {"行为ID": "{action_id}", "行为名称": "{action_name}", "触发元素": "{elem_id}", "触发事件": "点击|变更|提交", "执行动作": "{do}", "前置条件": "{pre}", "后置结果": "{post}"}
      ]
    }
  ]
}
```
