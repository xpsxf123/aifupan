<!-- module: common -->
<!-- area: api -->
<!-- generated-by: field-level-scan -->
<!-- last-scan: 2026-05-20 -->
<!-- source-paths: replay-api/src/main/java/com/jiuyu/replay/api/controller/common/ -->

# Common/Cross-cutting API -- 接口契约 (field-level)

> 公共/横切 Controller 集合。分布在 `replay-api/.../controller/common/`，涵盖文章、文件、客户端日志/更新、系统配置/KV、操作日志、COS Token、图片上传。所有 Controller 返回 `R<T>`，标 `@CrossOrigin` / `@Tag(...)`，鉴权走全局 AOP 拦截。

---

## 1. 文章管理 -- ArticleController

`@RequestMapping("replay/article")` `@CrossOrigin` `@Tag(name="文章")`

#### POST /replay/article/list
- **Summary:** 文章列表 | **Auth:** token
- **Request fields (ArticleListBo extends PageBo):**
  | Field | Type | Required | Meaning |
  |---|---|---|---|
  | keyword | String | no | 模糊搜索 |
  | type | Integer | no | 按文章类型筛选 |
  | page | Integer | no | 当前页, default=1 |
  | limit | Integer | no | 每页记录数, default=10 |
- **Response `data`:** `PageUtils<ArticleListVo>`，list 项 (ArticleVo):
  | Field | Type | Meaning |
  |---|---|---|
  | id | Long | ID |
  | userId | Long | 创建人 ID |
  | title | String | 文章标题 |
  | remarks | String | 备注 |
  | type | Integer | 类型 |
  | content | String | 富文本内容 |
  | createDate/updateDate | Date | 创建/修改时间 |
  | isDeleted | Integer | 是否已删除 |

#### info / save / update / delete (标准 CRUD)
| Method | Path | Request | Response | Note |
|--------|------|---------|----------|------|
| GET | `/info` | id (Long, req) | `ArticleInfoVo` (同 ArticleVo) | - |
| POST | `/save` | `ArticleBo` (同 ArticleVo 字段, 无 id) | `String` | - |
| POST | `/update` | `ArticleBo` (必须带 id) | `String` | - |
| GET | `/delete` | id (Long, req) | `String` | - |

---

## 2. 客户端日志 -- ClientLogController

`@RequestMapping("replay/clientlog")` `@CrossOrigin` `@Tag(name="客户端日志")`

#### POST /replay/clientlog/pagelist/{pageIndex}/{pageSize}
- **Summary:** 分页获取客户端日志 | **Auth:** token
- **Request params:** pageIndex (Integer, PathVariable) / pageSize (Integer, PathVariable)
- **Request fields (RequestBo):**
  | Field | Type | Required | Meaning |
  |---|---|---|---|
  | actionName | String | no | 动作名称 |
  | startTime | String | no | 查询开始时间 |
  | endTime | String | no | 查询结束时间 |
  | logType | Integer | no | 日志类型 0:正常, 1:错误 |
- **Response `data`:** `PageUtils<ClientLogVo>`，list 项:
  | Field | Type | Meaning |
  |---|---|---|
  | id | Long | 主键 |
  | actionName | String | 动作名称 |
  | actionInfo | String | 动作信息 |
  | errorMsg | String | 错误信息 |
  | clientVersion | String | 客户端版本 |
  | clientUser | String | 客户端用户 |
  | executeTime | Date | 执行时间 |
  | logType | Integer | 日志类型 0:正常, 1:错误 |
  | userId | Long | 用户 ID |

#### POST /replay/clientlog/delete
- **Summary:** 按查询条件删除 | **Request fields:** `RequestBo` (同 pagelist) | **Response:** `String`

#### POST /replay/clientlog/deleteByIds
- **Summary:** 按选中数据删除 | **Request body:** `List<Long>` | **Response:** `String`

---

## 3. 客户端版本更新 -- ClientUpdateController

`@RequestMapping("replay/clientupdate")` `@CrossOrigin` `@Tag(name="客户端版本更新")`

#### GET /replay/clientupdate/pagelist
- **Summary:** 分页获取 | **Auth:** token
- **Request params (ClientUpdateListBo extends PageBo):**
  | Field | Type | Required | Meaning |
  |---|---|---|---|
  | keyword | String | no | 模糊搜索 |
  | versionNum | String | no | 版本号筛选 |
  | isFront | Integer | no | 类型 0:主程序, 1:更新程序, 2:补丁 |
  | parentId | Long | no | 父 ID |
  | page | Integer | no | 当前页, default=1 |
  | limit | Integer | no | 每页记录数, default=10 |
- **Response `data`:** `PageUtils<ClientUpdateVo>`，list 项:
  | Field | Type | Meaning |
  |---|---|---|
  | id | Long | 主键 |
  | versionNum | String | 版本号 |
  | updateInfo | String | 更新描述 |
  | updateTime | Date | 上传时间 |
  | updateType | Integer | 更新方式 0:手动下载, 1:强制更新 |
  | isFront | Integer | 类型 0:主程序, 1:更新程序, 2:补丁 |
  | isPreserve | Integer | 是否保留 0:否, 1:是 |
  | fileList | List\<FileVo\> | 客户端文件信息列表 |
  | version | Double | 版本编号（越大越新） |
  | fileMd5 | String | 文件 MD5 |
  | parentId | Long | 父级 ID |
  | status | Integer | 状态 0:开发, 1:发布 |
  | cosKey | String | 文件 COS Key |

#### save / update / delete
| Method | Path | Request | Response | Note |
|--------|------|---------|----------|------|
| POST | `/save` | `ClientUpdateBo` (同 ClientUpdateVo 字段, 无 id, fileIds 为 List\<String\>) | `String` | - |
| POST | `/update` | `ClientUpdateBo` (必须带 id) | `String` | - |
| POST | `/delete` | `List<Long>` (body) | `String` | 批量删除 |

#### GET /replay/clientupdate/info
- **Summary:** 详情 | **Request:** id (Long, req) | **Response:** `ClientUpdateVo`

#### GET /replay/clientupdate/updateStatus
- **Summary:** 修改状态 | **Request:** id (Long, req) / status (Integer, req) 0:开发,1:发布 | **Response:** `String`

#### POST /replay/clientupdate/uploadFile
- **Summary:** 上传文件 | **Request:** Multipart file + version (String) | **Response:** `Map<String, String>`

---

## 4. 公共工具 -- CommonController

`@RequestMapping("replay/common")` `@CrossOrigin` `@Tag(name="公共")`

#### GET /replay/common/video
- **Summary:** 显示视频（调试，硬编码本地路径） | **Auth:** Session | **Response:** void (流式输出)

#### POST /replay/common/uploadImg
- **Summary:** 上传图片 | **Auth:** token | **Request (multipart/form-data):**
  | Field | Type | Required | Default | Meaning |
  |---|---|---|---|---|
  | file | MultipartFile | yes | - | 图片文件 |
  | flag | Integer | no | 0 | 压缩 0:正常, 1:超级, 2:不压缩 |
  | sort | Integer | no | - | 排序 |
  | resourceType | Integer | no | - | 来源类型 |
  | isCheckSecurity | Integer | no | - | 安全检测 0:否, 1:是 |
- **Response `data` (FileShowVo):** id(Long)/name(String)/resourceId(Long)/url(String)/sort(Integer)

#### GET /replay/common/img/{fileName}
- **Summary:** 显示图片 | **Request:** fileName (String, PathVariable) | **Response:** `byte[]` (JPEG)

#### GET /replay/common/serverCurrentTime
- **Summary:** 服务器当前时间 | **Response:** `String` (毫秒值)

---

## 5. 文件管理 -- FileController

`@RequestMapping("replay/file")` `@CrossOrigin` `@Tag(name="文件")`

#### POST /replay/file/list
- **Summary:** 文件列表 | **Auth:** token
- **Request fields (FileListBo extends PageBo):** keyword(String, no) / page(Integer, no, default=1) / limit(Integer, no, default=10)
- **Response `data`:** `PageUtils<FileListVo>`，list 项 (FileVo):
  | Field | Type | Meaning |
  |---|---|---|
  | id | Long | ID |
  | fileName | String | 文件名字 |
  | fileSize | Long | 文件大小 |
  | fileType | String | 文件类型 (png/word/pdf) |
  | fileUrl | String | 文件存放 URL |
  | resourceId | Long | 来源 ID |
  | resourceType | Integer | 来源类型 0:用户头像, 1:商品主图, 2:详情图, 999:未定义 |
  | remarks | String | 描述 |
  | sort | Integer | 排序 |
  | createDate/updateDate | Date | 创建/修改时间 |
  | isDeleted | Integer | 是否已删除 |

#### showOne / updateFile / delete / uploadAifuPa
| Method | Path | Request | Response |
|--------|------|---------|----------|
| GET | `/showOne` | resourceId (Long, opt) / resourceType (Integer, req) | `FileShowVo` |
| POST | `/updateFile` | `UpdateFileBo`: id(Long,req)/resourceId(Long)/resourceType(Integer)/remarks(String) | `String` |
| GET | `/delete` | id (Long, req) | `String` |
| GET | `/uploadAifuPa` | file (MultipartFile, req) | `String` |

---

## 6. 操作日志 -- OperationLogController

`@RequestMapping("replay/common/operationlog")` `@CrossOrigin` `@Tag(name="操作日志表")`

#### POST /replay/common/operationlog/list
- **Summary:** 操作日志列表 | **Auth:** token
- **Request fields (OperationLogListBo extends PageBo):**
  | Field | Type | Required | Meaning |
  |---|---|---|---|
  | keyword | String | no | 模糊搜索 |
  | userId | Long | no | 用户 userId |
  | businessType | String | no | 业务类型 USER_DETAILS/USER/ORDER/OTHER |
  | page | Integer | no | 当前页, default=1 |
  | limit | Integer | no | 每页记录数, default=10 |
- **Response `data`:** `PageUtils<OperationLogListVo>`，list 项 (OperationLogVo +):
  | Field | Type | Meaning |
  |---|---|---|
  | id | Long | 主键 ID |
  | businessType | String | 业务类型 |
  | businessId | Long | 业务 ID |
  | businessUserId | Long | 业务用户 ID |
  | operationType | String | 操作类型 UPDATE/DELETE |
  | beforeData / afterData | String | 操作前/后数据 (JSON) |
  | beforeObjData / afterObjData | Map\<S,O\> | 操作前/后数据（解析后，仅 ListVo） |
  | operatorId | Long | 操作人 ID |
  | operatorName | String | 操作人姓名 |
  | operationTime | Date | 操作时间 |
  | operationIp | String | 操作 IP |
  | remark | String | 备注 |
  | createDate/updateDate | Date | 创建/修改时间 |
  | isDeleted | Integer | 是否已删除 |

#### info / save / update / delete (标准 CRUD)
| Method | Path | Request | Response | Note |
|--------|------|---------|----------|------|
| GET | `/info` | id (Long, req) | `OperationLogInfoVo` (同 Vo) | - |
| POST | `/save` | `OperationLogBo` (同 Vo 字段, 无 id) | `String` | - |
| POST | `/update` | `OperationLogBo` (必须带 id) | `String` | - |
| GET | `/delete` | id (Long, req) | `String` | - |

---

## 7. 系统配置 -- SystemConfigController

`@RequestMapping("replay/systemconfig")` `@CrossOrigin` `@Tag(name="系统配置")`

#### POST /replay/systemconfig/list
- **Summary:** 系统配置列表 | **Auth:** token
- **Request fields (SystemConfigListBo extends PageBo):** keyword(String, no) / page(Integer, no, default=1) / limit(Integer, no, default=10)
- **Response `data`:** `PageUtils<SystemConfigListVo>`，list 项 (SystemConfigVo):
  | Field | Type | Meaning |
  |---|---|---|
  | id | Long | ID |
  | autoCreateDataViewingSecond | Integer | 超过多少秒视频自动生成看盘数据 |
  | dataViewingTimeDifference | Long | 允许匹配前后多少毫秒的数据看板 |
  | dataViewingTimeSend | Integer | 多少秒内相同主播场次的数据看板请求不发送 |
  | createDate/updateDate | Date | 创建/修改时间 |
  | isDeleted | Integer | 是否已删除 |

#### info / save / update / delete (同 CRUD 模式)
| Method | Path | Request | Response |
|--------|------|---------|----------|
| GET | `/info` | id (Long, req) | `SystemConfigInfoVo` |
| POST | `/save` | `SystemConfigBo` (同 Vo, 无 id) | `String` |
| POST | `/update` | `SystemConfigBo` (带 id) | `String` |
| GET | `/delete` | id (Long, req) | `String` |

---

## 8. 系统 KV 配置 -- SystemKvController

`@RequestMapping("replay/systemkv")` `@CrossOrigin` `@Tag(name="系统配置的键值对")`

#### POST /replay/systemkv/updateImgConfig
- **Summary:** 修改图片配置 | **Auth:** token
- **Request fields (ImgConfigBo):**
  | Field | Type | Required | Meaning |
  |---|---|---|---|
  | h5ImgId | Long | no | H5 兜底销售二维码图片文件 ID |
  | clientSaleImgId | Long | no | 客户端销售二维码图片文件 ID |
  | pureRecordImgId | Long | no | 纯录制版本客户端销售二维码图片文件 ID |
- **Response:** `String`

#### GET /replay/systemkv/getImgConfig
- **Summary:** 获取图片配置
- **Response `data` (ImgConfigVo):** h5ImgVo(FileShowVo) / clientSaleImgVo(FileShowVo) / pureRecordImgVo(FileShowVo)

#### POST /replay/systemkv/list
- **Summary:** KV 列表 | **Auth:** token
- **Request fields (SystemKvListBo extends PageBo):** keyword(String, no) / page(Integer, no, default=1) / limit(Integer, no, default=10)
- **Response `data`:** `PageUtils<SystemKvListVo>`，list 项 (SystemKvVo):
  | Field | Type | Meaning |
  |---|---|---|
  | id | Long | ID |
  | kvKey | String | KV Key (code) |
  | kvValue | String | KV Value |
  | remarks | String | 备注 |
  | createDate/updateDate | Date | 创建/修改时间 |
  | isDeleted | Integer | 是否已删除 |

#### info / getByKey / save / update / delete
| Method | Path | Request | Response | Note |
|--------|------|---------|----------|------|
| GET | `/info` | id (Long, req) | `SystemKvInfoVo` | - |
| GET | `/getByKey` | key (String, req) | `SystemKvInfoVo` | 按 Key 查询 |
| POST | `/save` | `SystemKvBo` (同 Vo, 无 id) | `String` | - |
| POST | `/update` | `SystemKvBo` (带 id) | `String` | - |
| GET | `/delete` | id (Long, req) | `String` | - |

---

## 9. 腾讯 COS Token -- TencentCosController

`@RequestMapping("replay/tencentCos")` `@CrossOrigin` `@Tag(name="cos控制器")`

#### GET /replay/tencentCos/cosPublicReadTempToken
- **Summary:** 获取COS私有写、公有读临时token | **Auth:** token
- **Response `data` (TencentCosTokenVo extends TencentTempTokenVo):**
  | Field | Type | Meaning |
  |---|---|---|
  | token | String | 临时 token |
  | tempSecretId | String | 临时 SecretId |
  | tempSecretKey | String | 临时 SecretKey |
  | savePrefix | String | 保存前缀 |
  | region | String | COS 存储区域 |
  | bucketName | String | COS 存储桶名称 |

---

## 路由全表

| Method + Path | Summary | Controller |
|------|------|------|
| POST `/replay/article/list` | 文章列表 | ArticleController |
| GET `/replay/article/info?id=` | 文章详情 | ArticleController |
| POST `/replay/article/save` / `/update` | 新增/修改文章 | ArticleController |
| GET `/replay/article/delete?id=` | 删除文章 | ArticleController |
| POST `/replay/clientlog/pagelist/{pageIndex}/{pageSize}` | 客户端日志分页 | ClientLogController |
| POST `/replay/clientlog/delete` / `/deleteByIds` | 删除日志 | ClientLogController |
| GET `/replay/clientupdate/pagelist` | 版本更新分页 | ClientUpdateController |
| GET `/replay/clientupdate/info?id=` | 版本更新详情 | ClientUpdateController |
| POST `/replay/clientupdate/save` / `/update` | 新增/修改版本更新 | ClientUpdateController |
| GET `/replay/clientupdate/updateStatus?id=&status=` | 修改状态 | ClientUpdateController |
| POST `/replay/clientupdate/delete` / `/uploadFile` | 删除/上传文件 | ClientUpdateController |
| GET `/replay/common/video` | 显示视频(调试) | CommonController |
| POST `/replay/common/uploadImg` | 上传图片 | CommonController |
| GET `/replay/common/img/{fileName}` / `/serverCurrentTime` | 显示图片/服务器时间 | CommonController |
| POST `/replay/file/list` | 文件列表 | FileController |
| GET `/replay/file/showOne?resourceId=&resourceType=` | 文件查询 | FileController |
| POST `/replay/file/updateFile` | 修改文件 | FileController |
| GET `/replay/file/delete?id=` / `/uploadAifuPa` | 删除/上传压缩文件 | FileController |
| POST `/replay/common/operationlog/list` | 操作日志列表 | OperationLogController |
| GET `/replay/common/operationlog/info?id=` | 操作日志详情 | OperationLogController |
| POST `/replay/common/operationlog/save` / `/update` | 新增/修改操作日志 | OperationLogController |
| GET `/replay/common/operationlog/delete?id=` | 删除操作日志 | OperationLogController |
| POST `/replay/systemconfig/list` | 系统配置列表 | SystemConfigController |
| GET `/replay/systemconfig/info?id=` | 系统配置详情 | SystemConfigController |
| POST `/replay/systemconfig/save` / `/update` | 新增/修改系统配置 | SystemConfigController |
| GET `/replay/systemconfig/delete?id=` | 删除系统配置 | SystemConfigController |
| POST `/replay/systemkv/updateImgConfig` | 修改图片配置 | SystemKvController |
| GET `/replay/systemkv/getImgConfig` | 获取图片配置 | SystemKvController |
| POST `/replay/systemkv/list` | KV 列表 | SystemKvController |
| GET `/replay/systemkv/info?id=` / `/getByKey?key=` | KV 详情/按Key查 | SystemKvController |
| POST `/replay/systemkv/save` / `/update` | 新增/修改 KV | SystemKvController |
| GET `/replay/systemkv/delete?id=` | 删除 KV | SystemKvController |
| GET `/replay/tencentCos/cosPublicReadTempToken` | COS 临时 Token | TencentCosController |
