<!-- module: system -->
<!-- area: domain -->
<!-- generated-by: reverse-scan -->
<!-- last-scan: 2026-05-20 -->
<!-- source-paths: replay-system/src/main/java/com/jiuyu/replay/system/entity/ -->

# System Domain -- 业务概念与词汇表

> replay-system 模块核心业务概念定义。承载字典数据管理、字典类型管理、登录页轮播图等系统级配置能力。Agent 在 Explorer/Propose 阶段必须使用此术语表，避免领域漂移。

---

## 核心概念

| 概念 | 定义 | 关联概念 | 状态机 |
|------|------|----------|--------|
| **DictType (字典类型)** | 字典分类标识，每个类型下挂多组字典条目。通过唯一 `logo` 字段作为跨模块查找 key | [[DictData]] | `status`: 0启用/1禁用 |
| **DictData (字典数据)** | 字典键值对条目，支持父子层级树形结构。存储在一个类型下的 label/value 对，供前端下拉选项、配置项等使用 | [[DictType]] | `status`: 0启用/1禁用 |
| **LoginRotateImage (登录页轮播图)** | 客户端登录页轮播图配置，管理图片 fileId、展示内容、排序、启用/停用状态 | [[FileBll]] (replay-common 文件服务) | `imgStatus`: 0启用/1停用 |

---

## 状态机定义

### DictData.status -- 字典数据启用状态
| 值 | 状态 | 说明 |
|----|------|------|
| 0 | ENABLED | 启用，默认查询结果包含 |
| 1 | DISABLED | 禁用，分页查询默认排除（isAll=0 时过滤）；列表查询（dictDataListByCode）也过滤 |

### DictType.status -- 字典类型启用状态
| 值 | 状态 | 说明 |
|----|------|------|
| 0 | ENABLED | 启用 |
| 1 | DISABLED | 禁用 |

### LoginRotateImage.imgStatus -- 轮播图展示状态
| 值 | 状态 | 说明 |
|----|------|------|
| 0 | ENABLED | 启用，客户端可见 |
| 1 | DISABLED | 停用 |

### Constant.CodeMsgEnum -- 模块内业务消息码
| Code | 含义 |
|------|------|
| 1001 | TIP_CUSTOM -- 自定义提示（业务校验异常） |

---

## 核心工作流

### 1. 字典类型 CRUD (管理后台)
```
管理端 POST /system/dicttype/save (DictTypeBo)
  → DictTypeBll.save()
  → DictTypeProducer.save()
  → SnowflakeManager.nextValue() 生成 ID
  → createDate/updateDate = new Date()
  → dictTypeService.save(entity)
  → 返回 DictTypeInfoVo

修改/删除：DictTypeBll.update/delete → DictTypeProducer.update/deleteById
  注意：delete 使用 removeById（物理删除，非软删除）
```

### 2. 字典数据 CRUD + 缓存失效 (管理后台)
```
管理端 POST /system/dictdata/save (DictDataBo)
  → DictDataBll.save()
  → DictDataProducer.save()
    → 删除 Redis 缓存: replay:dict:type-logo:{typeLogo}  (通过 typeId 查出对应的 DictType.logo)
    → SnowflakeManager.nextValue() 生成 ID
    → dictDataService.save(entity)
    → 返回 DictDataInfoVo

修改/删除时同步清除新旧两个 typeLogo 对应的缓存 key。

分页查询 (queryPage):
  → 按 keyword/label/value/typeId 过滤
  → 如传入 typeLogo，先查 DictType getId 再过滤
  → isAll=0 时过滤 status!=0 的条目
  → 结果装配 typeName（反 JOIN: 先查 dictData 分页 → 取 typeIds → 批量查 dictType → 内存 Map 装配）
```

### 3. 字典数据查询 (高频读, Redis 缓存)
```
业务模块调用 DictDataFeign.dictDataListByCode(code)
  → DictDataApi.dictDataListByCode(code)
  → DictDataBll.dictDataListByCode(code)
  → DictDataProducer.listByTypeLogo(typeLogo)
    → Redis key: replay:dict:type-logo:{typeLogo}
    → 命中返回 JSON.parseArray 转 List<DictDataListVo>
    → 未命中: 查 DictType (by logo) → 查 DictData list (by typeId, orderBy sort ASC)
    → 写入 Redis, TTL=5 天
    → 返回 List<DictDataListVo>
```

### 4. 字典树形结构
```
DictData 通过 parentId 支持层级树:
  - 底层使用 Hutool TreeUtil.build 构建树
  - 前端可用 treeName 模式: label 返回 "父级/子级/当前" 路径
  - buildTreePath 使用迭代+Map<Long,Vo> 预处理, 复杂度 O(n)
  
  listDictDataTree(typeId): 查指定类型全部条目 → TreeUtil 构建树 → 返回 List<DictDataListVo>
  dictDataTreeByValue(code, value): 查指定条目 → 拼接父级路径到 label
```

### 5. 字典多类型批量查询
```
SystemDictDataController.dictDataListByCodes("code1,code2,code3")
  → 按逗号切分 codes
  → 逐个类型查 dictDataProducer.listByTypeLogo(code)
  → 只收集 status=0 的条目 (启用态过滤)
  → 返回 Map<String, List<DictDataListVo>>
```

### 6. 网络延迟检测 URL 随机抽取
```
GET /listNetworkCheckUrls
  → 查 "network_latency_check_url" 字典类型全部条目
  → Collections.shuffle 随机打乱 → 取前 10 条
  → 返回 List<DictDataListVo>
```

### 7. 上传文件平台类型字典
```
GET /uploadFileDictList
  → 查 "replay_platform_type" 字典类型
  → 过滤掉 value="0" 的条目（平台类型根节点）
  → 返回子条目列表
```

### 8. 登录页轮播图 CRUD + 排序自动递增
```
管理端 POST /replay/loginrotateimage/save (LoginRotateImageBo)
  → LoginRotateImageLogicImpl.save()
  → LoginRotateImageBll.save()
  → LoginRotateImageProducer.save()
    → SnowflakeManager.nextValue() 生成 ID
    → imgStatus 默认 0 (启用)
    → sort 默认 0
    → 插入后触发 toUpdateSort(insertedEntity)
      → @Transactional 查询 sort >= 当前 sort 的所有记录
      → 每条 sort += 1 (排它自己)
      → updateBatchById 批量更新

修改后同样触发 toUpdateSort 重排
删除时同步删除关联文件: LoginRotateImageLogicImpl.delete()
  → 先查 info 取 fileId
  → fileBll.delete(fileId) 删除文件
  → loginRotateImageBll.delete(id)

客户端查询(noPage):
  → 只查 imgStatus=0 的记录
  → orderBy sort ASC
  → Logic 层装配文件信息（name/url/resourceId，反 JOIN: 取 fileIds → fileBll.listByFileIds → Map 装配）
```

---

## 字典类型 (logo) 已知清单

字典类型标识 `logo` 是跨模块查找字典的唯一 key。以下为代码中显式引用的类型:

| logo | 用途 | 消费模块 |
|------|------|----------|
| `replay_platform_type` | 文件上传平台类型 | system (自身) |
| `network_latency_check_url` | 网络延迟检测 URL 列表 | system (自身) |
| `assign_mark` | 分配标记 | words / power / ai |
| `sms_config_*` | 短信配置 | third |
| `ai_model_*` | AI 模型配置 | third / ai |
| `reward_*` | 奖励相关配置 | reward |
| `video_extract_*` | 视频提取配置 | video |

> 注意：以上为代码扫描发现的部分引用，完整字典类型清单由业务运维维护。

---

## 跨模块依赖

system 是**配置底座模块**：
- **被依赖**：几乎所有业务模块（words / power / ai / reward / video / third / order）通过 [[DictDataFeign]] 查询字典配置
- **依赖**：LoginRotateImage 逻辑层依赖 `FileBll`（replay-common 文件服务）获取图片 URL

特别说明：`DictDataFeign` 是项目中使用最广泛的 Feign 接口之一，提供了丰富的 default 方法（getConfigValue / getValueDefault / getDate / getConfigValue<Class> / getConfigList）供各模块便捷取配置。
