# 接口依赖（Markdown Code）

说明：前端调用接口清单，使用 json 代码块承载。

```json
{
  "项目名称": "{project_name}",
  "分析时间": "{date}",
  "接口清单": [
    {
      "接口ID": "{api_id}",
      "接口名称": "{api_name}",
      "接口用途": "{purpose}",
      "调用页面": ["{page_id}"] ,
      "HTTP方法": "GET|POST|PUT|DELETE",
      "接口路径": "/api/path",
      "请求参数": [
        {"参数名": "{param}", "参数位置": "query|path|body", "参数类型": "字符串|数字|布尔", "是否必填": true, "说明": "{desc}"}
      ],
      "响应示例": {}
    }
  ]
}
```
