<!-- module: system -->
<!-- area: api -->
<!-- generated-by: reverse-scan -->
<!-- last-scan: 2026-05-20 -->
<!-- source-paths: replay-system/, replay-api/.../controller/system/ -->
<!-- field-level-detail: true -->

# System API -- 接口契约

> replay-system 模块完整 API 表面。Controller 分布：replay-system 内字典查询 Controller (1) + replay-api 管理端 Controller (3)。

所有 Controller 返回 `R<T>`，类上标 `@CrossOrigin` / `@Tag(...)` / 方法标 `@Operation(...)`。鉴权：管理端接口需 token，字典查询接口多为无 token 公开查询。

---

## 一、管理端字典 CRUD（replay-api 模块）

Base: `com.jiuyu.replay.api.controller.system`

### 1. 字典数据管理（DictDataController） -- `@RequestMapping("replay/dictdata")`

| Method | Path | Auth | Request | Response |
|--------|------|------|---------|----------|
| POST | `/list` | token | DictDataListBo (extends PageBo) | `PageUtils<DictDataListVo>` |
| GET | `/info?id=` | token | id: Long 必填 | DictDataInfoVo |
| POST | `/save` | token | DictDataBo | `R<String>` |
| POST | `/update` | token | DictDataBo | `R<String>` |
| GET | `/delete?id=` | token | id: Long 必填 | `R<String>` |

##### DictDataBo 字段

| Field | Type | Meaning |
|---|---|---|
| id | Long | ID |
| typeId | Long | 字典类型ID |
| label | String | 字典标签 |
| value | String | 字典值 |
| status | Integer | 状态（0启用/1禁用） |
| sort | Integer | 排序 |
| parentId | Long | 父字典ID |

##### DictDataListVo 响应字段（列表/查询）

| Field | Type | Meaning |
|---|---|---|
| id | Long | 字典ID |
| typeId | Long | 字典类型ID |
| label | String | 字典标签（树形查询时为 "父/子/当前" 路径） |
| value | String | 字典值 |
| status | Integer | 状态 |
| sort | Integer | 排序 |
| parentId | Long | 父ID |
| children | List\<DictDataListVo\> | 子节点（树形查询时） |
| parentName | String | 父级名称（树形查询时） |

---

### 2. 字典类型管理（DictTypeController） -- `@RequestMapping("replay/dicttype")`

| Method | Path | Auth | Request | Response |
|--------|------|------|---------|----------|
| POST | `/list` | token | DictTypeListBo (extends PageBo) | `PageUtils<DictTypeListVo>` |
| GET | `/info?id=` | token | id: Long 必填 | DictTypeInfoVo |
| POST | `/save` | token | DictTypeBo | `R<String>` |
| POST | `/update` | token | DictTypeBo | `R<String>` |
| GET | `/delete?id=` | token | id: Long 必填 | `R<String>` |

##### DictTypeBo 字段

| Field | Type | Meaning |
|---|---|---|
| id | Long | ID |
| logo | String | 字典类型标识（唯一，如 `replay_platform_type`） |
| name | String | 字典类型名称 |
| status | Integer | 状态（0启用/1禁用） |

---

## 二、模块内字典查询 API（replay-system 模块）

Base: `com.jiuyu.replay.system.controller`

### SystemDictDataController -- `@RequestMapping("replay/system/dictData")`

| Method | Path | Params | Auth | Response | Note |
|--------|------|--------|------|----------|------|
| GET | `/listDictDataTree?typeId=` | typeId: Long 必填 | 无 | `List<DictDataListVo>` | 树形结构 |
| GET | `/uploadFileDictList` | -- | 无 | `List<DictDataListVo>` | replay_platform_type，过滤根节点 |
| GET | `/dictDataByValue?code=&value=` | code, value: String 必填 | 无 | DictDataListVo | label 为树形路径 |
| GET | `/dictDataByLabel?code=&label=` | code, label: String 必填 | 无 | DictDataListVo | 单条查询 |
| GET | `/dictDataTreeByValue?code=&value=` | code, value: String 必填 | 无 | DictDataListVo | label 为 "父/子/当前" 格式 |
| GET | `/dictDataListByCode?code=` | code: String 必填 | 无 | `List<DictDataListVo>` | 启用态列表 |
| GET | `/dictDataTreeListByCode?code=` | code: String 必填 | 无 | `List<DictDataListVo>` | 树形列表 |
| GET | `/dictDataListByCodes?codes=` | codes: String 必填（逗号分隔） | 无 | `Map<String, List>` | 批量查询 |
| GET | `/listNetworkCheckUrls` | -- | 无 | `List<DictDataListVo>` | 随机10条网络延迟检测URL |

##### 请求参数约定

| Param | Type | Required | Meaning |
|---|---|---|---|
| typeId | Long | true（listDictDataTree） | 字典类型ID |
| code | String | true | 字典类型标识（如 `replay_platform_type`） |
| value | String | true | 字典值 |
| label | String | true | 字典标签 |
| codes | String | true | 多个字典类型标识，逗号分隔 |

> 字典数据缓存：`save/update/delete DictData` 时清除对应 typeLogo 的 Redis key。

---

## 三、登录页轮播图管理（LoginRotateImageController）

`@RequestMapping("replay/loginrotateimage")` `@CrossOrigin` `@Tag(name="客户端登录页轮播图")`

### 管理端接口（需 token）

| Method | Path | Request | Response |
|--------|------|---------|----------|
| POST | `/list` | LoginRotateImageListBo (extends PageBo, keyword 模糊查询) | `PageUtils<LoginRotateImageListVo>` |
| GET | `/info?id=` | id: Long 必填 | LoginRotateImageInfoVo（含文件 url/name） |
| POST | `/save` | LoginRotateImageBo | `R<String>` -- 自动递增排序 |
| POST | `/update` | LoginRotateImageBo | `R<String>` -- 自动递增排序 |
| GET | `/delete?id=` | id: Long 必填 | `R<String>` -- 同时删除关联文件 |

### 客户端公开接口

| Method | Path | Auth | Response |
|--------|------|------|----------|
| POST | `/noPage` | 公开（无 token 也可） | `List<LoginRotateImageListVo>` -- 仅 imgStatus=0 的记录 |

##### LoginRotateImageBo 字段

| Field | Type | Meaning |
|---|---|---|
| id | Long | 主键ID |
| userId | Long | 存入者ID |
| imgStatus | Integer | 状态（0启用/1停止展示） |
| content | String | 每张图底下的文案 |
| fileId | Long | 图片文件ID（关联 tb_file） |
| sort | Integer | 排序，从0开始 |

##### LoginRotateImageListVo 响应字段

| Field | Type | Meaning |
|---|---|---|
| id | Long | ID |
| userId | Long | 存入者ID |
| imgStatus | Integer | 状态 |
| content | String | 文案 |
| fileId | Long | 文件ID |
| sort | Integer | 排序 |
| createDate | Date | 创建时间 |

##### LoginRotateImageInfoVo 响应字段（继承 ListVo，附加文件信息）

| Field | Type | Meaning |
|---|---|---|
| (继承 ListVo 全部字段) | -- | -- |
| name | String | 文件名 |
| url | String | 文件访问URL |
| resourceId | Long | 资源ID |

---

## 四、Feign 接口

### DictDataFeign（`replay-generic/feign/system/DictDataFeign.java`）

| 方法 | 返回类型 | 说明 |
|------|----------|------|
| `dictDataByValue(String code, String value)` | `DictDataListVo` | 按 code+value 查询 |
| `dictDataByLabel(String code, String label)` | `DictDataListVo` | 按 code+label 查询 |
| `dictDataListByCode(String code)` | `List<DictDataListVo>` | 按 code 查列表 |
| `dictDataParentLabelByCode(String code)` | `List<DictDataListVo>` | 列表含树形路径 |
| `dictDataTreeListByCode(String code)` | `List<DictDataListVo>` | 树形结构（含父级名称） |
| `dictDataListByIds(List<Long> ids)` | `List<DictDataListVo>` | 按ID批量查 |

#### Default 便捷方法（无需实现类覆写）

| 方法 | 说明 |
|------|------|
| `getConfigValue(key, label)` | 按 key+label 取配置值（最常用） |
| `getValueDefault(key, label, defaultValue)` | 取配置值含默认值 |
| `getDate(key, label)` | 取配置值转 LocalDateTime |
| `getConfigValue(key, label, Class<T> clazz)` | 取配置值反序列化为 JavaBean |
| `getConfigList(key, label, Class<T> clazz)` | 取配置值反序列化为 List\<T\> |

#### 消费方
`replay-words`, `replay-power`, `replay-ai`, `replay-reward`, `replay-third`, `replay-video`, `replay-api` 等模块均注入 `DictDataFeign`。

---

## 五、注解 / AOP

| 注解 | 用途 |
|------|------|
| `@CrossOrigin` | 所有 Controller 类级别 |
| `@Operation(summary="...")` | OpenAPI 接口标注 |
| `@Tag(name="...")` | OpenAPI 分组标注 |
| `@Parameter(description="...", required=true)` | OpenAPI 参数标注 |

> system 模块未使用 `@NoRepeatSubmit`、`@Validated` 分组校验、自定义 AOP。

---

## 六、错误码

字典模块使用 `RRException`（`com.jiuyu.replay.common.utils.RRException`）：
- `RRException.isNotEmpty(obj, msg)` -- 空值校验，抛出自定义业务提示
- `Constant.CodeMsgEnum.TIP_CUSTOM(1001, "自定义提示")` -- 通过 `R.error(1001, msg)` 返回

模块内 Controller 不直接抛异常，校验在 Bll 层（如 `dictDataByValue` 中 `RRException.isNotEmpty(code, "字典类型标识不能为空")`）。
