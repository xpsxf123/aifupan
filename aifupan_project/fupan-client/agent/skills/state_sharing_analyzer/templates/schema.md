# 状态共享（Markdown Code）

说明：全局与页面状态定义，json 代码块承载。

```json
{
  "项目名称": "{project_name}",
  "分析时间": "{date}",
  "全局状态": [
    {"状态名称": "{state_name}", "状态代码": "{state_code}", "状态类型": "对象|数组|字符串|数字|布尔", "状态描述": "{desc}", "使用页面": ["{page_id}"], "初始值": null, "是否需要持久化": true, "持久化方式": "localStorage|sessionStorage", "更新时机": ["登录成功|切换组织"]}
  ],
  "页面状态": [
    {"页面ID": "{page_id}", "本地状态": [
      {"状态名称": "{local_state}", "状态代码": "{code}", "状态类型": "布尔值|字符串|数组", "初始值": false}
    ]}
  ]
}
```
