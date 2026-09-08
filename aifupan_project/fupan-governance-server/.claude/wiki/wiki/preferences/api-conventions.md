# 后端接口设计规范

> 来源: distilling from `.agents/llm_wiki/wiki/preferences/api-conventions.md` (2026-05-25)
> 这是项目约定（禁止 Path 变量、标准动词后缀、统一响应格式），不是 API 参考文档。

---

## 一、 接口路径命名规则

1. **统一风格**: 小写字母，多词用 `-` 分隔 (kebab-case)
2. **【强制】禁止 Path 变量传参**: ~~`/menu/delete/{id}`~~ 禁止。参数通过 Query String 或 Request Body 传递
3. **操作动词后缀**: `/add` (POST), `/update` (POST), `/delete` (POST), `/detail` (GET), `/list` (POST)
4. **模块层级**: `/{模块名}/{资源名}/{操作}`

## 二、 全局响应格式 (ApiResponse)

| 字段 | 类型 | 说明 |
|---|---|---|
| code | Integer | 0=成功，非0=失败 |
| msg | String | 提示消息 |
| data | Object (T) | 业务数据 |
| timestamp | Long | Unix 毫秒时间戳 |

## 三、 分页规范 (PageData)

| 字段 | 类型 | 说明 |
|---|---|---|
| currPage | Long | 当前页码 |
| pageSize | Long | 每页条数 |
| totalCount | Long | 总记录数 |
| totalPage | Long | 总页数 |
| list | List\<T\> | 数据列表 |
