# SEO 内容管理 API（前端对接稿）

> 官网 `https://www.ifupan.com` 的内容承载：文章、分类、标签三套管理 + Markdown 批量导入。
> 本模块所有响应均符合 [`R<T>` 统一响应封装](./_response_envelope.md)；本文档不重复说明 `code` / `msg` / `data`，**响应 data 结构**指 `R<T>` 内的 `T`。
> Snowflake ID（19 位）→ 前端必须用 `string` 接收，禁 `number`。后端 JacksonSerializerConfig 已全局序列化为 String。
>
> 错误码全集 → [./_error_codes.md](./_error_codes.md)。本模块**未新增错误码**，业务失败统一走 `BusinessException`（通用码 + 具体 msg），前端直接展示 `msg` 即可。
>
> 来源：`replay-system/.../controller/Seo{Article,Category,Tag,Common}Controller.java` + 各 BO/VO。
> 实现状态：✅ 全部接口已实装。
>
> **与 `replay/article`（客户端 H5 协议页）无关**——那是 `tb_article`，面向 App，字典分类、无标签/封面/SEO 字段。本模块独立建表、独立接口，两者不要混用。

---

## 接口列表

| # | 方法 | 路径 | 简述 |
|---|---|---|---|
| 1 | POST | `/replay/seoArticle/list` | 文章分页，含分类名与标签，**不返回正文** |
| 2 | GET | `/replay/seoArticle/info` | 文章详情，含正文 |
| 3 | POST | `/replay/seoArticle/save` | 新增文章 |
| 4 | POST | `/replay/seoArticle/update` | 修改文章 |
| 5 | GET | `/replay/seoArticle/delete` | 单条逻辑删除 |
| 6 | POST | `/replay/seoArticle/deleteByIds` | 批量逻辑删除 |
| 7 | POST | `/replay/seoArticle/changeStatus` | 发布 / 下架 |
| 8 | POST | `/replay/seoArticle/import` | 批量导入（multipart），立即返回 taskId |
| 9 | GET | `/replay/seoArticle/importProgress` | 轮询导入进度与逐篇结果 |
| 10 | POST | `/replay/seoCategory/list` | 分类分页，含关联文章数 |
| 11 | GET | `/replay/seoCategory/all` | 分类全量，供下拉，按 sort 升序 |
| 12 | GET | `/replay/seoCategory/info` | 分类详情 |
| 13 | POST | `/replay/seoCategory/save` `/update` | 新增 / 修改分类 |
| 14 | GET | `/replay/seoCategory/delete` | 删除分类（下有文章时被拒） |
| 15 | POST | `/replay/seoTag/list` | 标签分页，含关联文章数，可切排序 |
| 16 | GET | `/replay/seoTag/enabled` | 启用中的标签，供文章表单下拉 |
| 17 | GET | `/replay/seoTag/info` | 标签详情 |
| 18 | POST | `/replay/seoTag/save` `/update` | 新增 / 修改标签 |
| 19 | GET | `/replay/seoTag/delete` | 单条删除 |
| 20 | POST | `/replay/seoTag/deleteByIds` | 批量删除 |
| 21 | POST | `/replay/seoTag/changeStatus` | 启用 / 禁用，支持批量 |
| 22 | POST | `/replay/seoCommon/slugSuggest` | 拼音生成 + 唯一性探测 + 语义备选，一次返回 |

> **鉴权**：走 `LoginInterceptor`（`Token` 请求头）。本模块未标 `@APIKey`，因此不参与签名校验——导入接口用 `fetch` 直传时只带 `Token` 即可，与 `common/uploadImg` 一致。

---

## 枚举值

### articleStatus（文章）

| 值 | 含义 | 官网表现 |
|---|---|---|
| 1 | 已发布 | 可访问，出现在列表页、分类页、标签页、站点地图 |
| 0 | 已下架 | 页面不可访问，从各聚合页与站点地图移除 |

### tagStatus（标签）

| 值 | 含义 | 官网表现 |
|---|---|---|
| 1 | 启用 | 聚合页可访问，文章详情页展示该标签 |
| 0 | 禁用 | 聚合页不可访问，文章详情页不展示；**已有的文章-标签关联保留**，重新启用即恢复 |

### slugSuggest 的 type

`article` / `category` / `tag`，决定查重范围与生成规则。

### taskStatus（导入任务）

| 值 | 含义 |
|---|---|
| 0 | 处理中 |
| 1 | 已完成（**含部分失败**——逐篇独立事务，不整批回滚） |
| 2 | 任务级失败（线程池拒绝、执行器异常、超期未完成） |

---

## 三个前端强依赖的响应结构

### 1. 文章列表 / 详情的 `tags`

```json
{ "id": "1955...", "tagName": "直播复盘", "tagStatus": 1 }
```

**必须按 `tagStatus` 区别展示**：列表里禁用标签置灰标注；编辑表单里禁用标签仍要**保留在已选项中**（并入下拉选项、标注「已禁用」）。

> ⚠️ 若把禁用标签从已选值里剔除，运营打开编辑、什么都不改点一次保存，那些关联就被静默解除了。`enabled` 接口只返回启用中的标签，所以这一步必须由前端把详情返回的禁用标签并进选项。

### 2. save / update 的响应

```json
{ "code": 0, "msg": "添加成功", "data": {
    "id": "1955...", "slug": "zhibo-fupan-4827", "slugAppended": true } }
```

`slugAppended=true` 表示 slug 撞了唯一索引、被自动追加了 4 位随机数。**这是运营唯一一次得知最终 URL 的机会**，前端必须把 `data.slug` 展示出来（用 warning 级提示，给足阅读时间），不能只报「保存成功」。

### 3. importProgress

```json
{ "code": 0, "msg": "获取成功", "data": {
    "taskId": "xxx", "taskStatus": 1,
    "total": 11, "processed": 11, "successCount": 5, "failCount": 6,
    "errorMsg": "",
    "results": [
      { "file": "a.md", "title": "直播复盘进阶", "success": true,
        "notes": ["正文 4 张图片，4 张已转存图床"] },
      { "file": "b.md", "title": "投放策略拆解", "success": false,
        "notes": ["分类「投放优化」不存在，请先在分类管理中创建，或勾选「分类不存在时自动创建」"] }
    ] } }
```

`notes` 的文案**由后端给全**，前端只负责展示、不做拼接——失败原因有十来类，拼接规则散到前端必然与后端判断漂移。

---

## 入参要点

### `seoArticle/list`

```json
{ "page": 1, "limit": 10, "title": "复盘", "categoryId": "1955...", "articleStatus": 0, "tagId": "1955..." }
```

四个筛选条件均可空、可组合。`tagId` 供标签管理页「关联文章数」点击跳转带入，服务端走 `tb_seo_article_tag` 的 EXISTS 半连接。

### `seoTag/list`

```json
{ "page": 1, "limit": 10, "tagName": "复盘", "tagStatus": 1, "orderBy": "count" }
```

`orderBy ∈ {count, new}`，默认 `count`（按关联文章数倒序）。

> `count` 排序是**内存排序**：关联文章数是聚合值、不落库，无法在 SQL 层排序+分页，因此该模式下先全量取回再切页。标签量级预计几十到几百，可接受；量级上去了需要落一个冗余计数列。

### `seoArticle/changeStatus` 与 `seoTag/changeStatus`

入参已拆成两个独立 BO（此前共用一个、同时挂 id 与 ids，传错字段会被静默忽略）：

| 接口 | 入参 |
|---|---|
| `seoArticle/changeStatus` | `{ id, articleStatus }` —— 单条操作，PRD 未要求批量 |
| `seoTag/changeStatus` | `{ id?, ids?, tagStatus }` —— `id` 与 `ids` 都读并合并，两种传法都行 |

### `seoArticle/import`（multipart）

| 字段 | 说明 |
|---|---|
| `files` | 多个 `.md` 文件。**不支持 zip** |
| `autoCreateCategory` | `1` 表示分类不存在时自动创建，其他值/不传为否 |

整批级校验只有三条：文件全空、数量 > 50、总大小 > 20MB。**单个文件的问题（扩展名不对、超 2MB、非 UTF-8 编码）降级为单篇失败**，不否决整批。

---

## 前端必须实现的几条业务规则

这些规则后端有兜底，但只靠后端会让运营体验很差：

1. **slug 规范化要在输入时即时生效**（大写转小写、全角转半角、空白转连字符、白名单外字符转连字符）。后端 `SeoSlugUtil.normalize` 是最终权威，但等保存完才告诉运营「你输的 `Zhibo-Fupan` 存成了 `zhibo-fupan`」体验很糟。
2. **自动生成 slug 的时机分两种**：文章与分类是**名称失焦**时生成，标签是**输入时实时**生成。文章标题长，中文输入法下每停顿一下就生成会拿半截词做拼音（「直播复盘」输到「直播」时生成 `zhibo`），运营不检查就发布了。
3. **手动改过 slug 之后自动生成必须停止**，只能靠「重新生成 / 由标题生成」按钮恢复。
4. **改已发布内容的 slug 要二次确认**，文案含变更前后完整地址、影响范围、以及「请把新旧地址告知官网配置 301」——本期不做 slug 历史表，这句提醒是防止漏登记的唯一防线。
5. **SEO 标题 60 / 描述 160 是建议值不是上限**（DDL 分别是 varchar(200) / varchar(300)）。超出只在搜索结果里被截断，界面上要计数变红提示但**不能**用 `maxlength` 硬拦或加阻断校验。
6. **导入进度轮询用递归 `setTimeout` 而非 `setInterval`**。单次进度请求可能超过轮询间隔（后端在下载图片），`setInterval` 会让多个请求并发在途，乱序返回互相覆盖。

---

## 数据表

5 张全新表，无外键回指既有表，回滚即 `DROP TABLE`。DDL 与菜单 SQL 见 `sql/replay-45.sql`。

| 表 | 用途 |
|---|---|
| `tb_seo_article` | 文章 |
| `tb_seo_category` | 分类 |
| `tb_seo_tag` | 标签 |
| `tb_seo_article_tag` | 文章-标签关联（多对多） |
| `tb_seo_import_task` | 导入任务（异步，结果落库供刷新后查回） |

**不带 `tenantId`**：SEO 内容是平台级运营数据（面向官网访客），与 `tb_dict_data`、`tb_login_rotate_image` 等平台配置表一致。

**逻辑删除与 slug 唯一并存**：slug 需唯一，但已删记录不得占用它。删除时把 `slug`（标签还有 `tag_name`）改写为 `{原值}__del_{id}`，唯一索引保持单列。字段长度已预留 25 字符后缀空间。改写用 SQL 的 `CONCAT` 就地完成，整批一条 UPDATE。

**列名避开了 MySQL 关键字**：`name` / `status` 是 MySQL 非保留关键字，本次新增的 5 张表改用
`category_name` / `tag_name` / `article_status` / `tag_status` / `task_status`。

**命名在整条链路上统一**——DDL 列名、Java 实体、BO、VO、以及前端读写的字段名全部一致：

| 概念 | 列名 | Java / JSON 字段 |
|---|---|---|
| 分类名称 | `category_name` | `categoryName` |
| 标签名称 | `tag_name` | `tagName` |
| 标签状态 | `tag_status` | `tagStatus` |
| 文章状态 | `article_status` | `articleStatus` |
| 导入任务状态 | `task_status` | `taskStatus` |

> 统一命名让 `BeanUtils.copyProperties` 在实体与 BO/VO 之间照常工作。若只改列名而保留 BO/VO 的
> `name` / `status`，copyProperties 会<b>静默漏拷且不报错</b>，接口返回全 null——每处都得手写显式赋值兜底。
>
> **唯一的例外是 `seoCommon/slugSuggest` 的入参 `name`**：它不对应任何表列，是三种类型
> （文章标题 / 分类名 / 标签名）共用的通用入参，叫 categoryName 或 tagName 都不准确，故保留。

---

## 已知限制

| 项 | 说明 |
|---|---|
| 浏览量 | 本期不做。`tb_seo_article.view_count` 字段预留、默认 0，接口不返回、不写入。标签列表原定按浏览量排序，改用关联文章数替代 |
| `create_user_id` | 导入任务记录发起人，取 `RequestContext.getUserId()`。轮询按不可枚举的 taskId 取，不做归属校验 |
| DNS rebinding | 图片抓取解析一次 DNS、连接时 JDK 再解析一次，中间存在窗口。彻底封堵需自定义 SocketFactory 或固定出口代理，成本远高于收益 |
| 权限 | 三个页面靠 `tb_menu` + `tb_menu_role` 控制菜单可见性。**接口层面只校验登录、不校验角色**——这是全项目现状（无权限框架），非本模块引入 |

---

## 菜单配置

前端路由由菜单驱动：`permission/index.js` 的 `loadComponent` 把菜单 `url` 拼成 `/src/views${url}/index.vue`。

| 名称 | url | type |
|---|---|---|
| SEO内容管理 | `/seo` | 2 目录 |
| ├ SEO文章 | `/seo/article` | 0 菜单 |
| ├ SEO分类 | `/seo/category` | 0 菜单 |
| └ SEO标签 | `/seo/tag` | 0 菜单 |

> ⚠️ **菜单名必须全局唯一**，它被直接当作 Vue Router 的路由 name：
> `router.addRoute('main', { name: ele.name, path: ele.url, ... })`。
> Vue Router 4 遇到同名路由会先移除已有的那条，于是两条同名菜单只有后注册的 path 生效。
>
> 所以这里叫「SEO文章」而不是「文章管理」——后者已被 `/client/article`（客户端 H5 协议页）占用。
> **撞名的症状极具迷惑性**：菜单正常显示、可以点击，`type`/`url`/组件文件全部正确，但访问就是 404，
> 而且控制台没有任何报错。测试环境实际踩过这个坑。

> ⚠️ **url 必须带前导斜杠**。写成 `seo/article` 会拼出 `/src/viewsseo/article/index.vue`，`loadComponent` 找不到组件时返回 `undefined` 且不抛错——表现是点进去白屏、控制台干净，很难查。
>
> 菜单还要挂到角色上（`tb_menu_role`）才会出现在左侧导航。

---

# 官网公开接口（/replay/site/**）

> 分支 `feature/seo-site-api` ｜ openspec: `.claude/runs/Change__2026-08-18_seo-site-api/`
> 消费方：官网 `fupan-new-official`（Nuxt 3 SSR），分支 `feature/seo-article-pages`。
> 实现状态：✅ 已实装，335 条单测全绿。

## ⚠️ 这组接口与上面那 22 个的根本区别

**它们对匿名公网开放**（`LoginInterceptor.excludePathList` 放行了 `/replay/site/**`），
没有任何登录校验。因此有三条不可放松的约束，改动前务必先读 `SeoSiteController` 的类注释：

1. **只读**——不得新增任何写操作；
2. **只返回已发布内容**（`article_status = 1` 且未删除），下架文章必须让官网 404；
3. **出参只用 `vo.site` 包下的类**——不得复用上面那些后台 VO，
   否则日后有人给后台 VO 加字段时，不会意识到它同时在对匿名公网输出。

## 🚩 articleCount 的口径与后台相反

后台的 `articleCount` 是「未删除、**含已下架**」；
官网的是「**只算已发布**」。这个数字在官网决定两件事：空分类页是否 404、
标签页是否加 `noindex`。用含下架的数会出现「数字是 5、列表却查出 0 篇」的 200 空壳页。

## 接口列表

| # | 方法 | 路径 | 简述 |
|---|---|---|---|
| 1 | GET | `/replay/site/article/list` | 文章列表，列表页/分类页/标签页共用 |
| 2 | GET | `/replay/site/article/detail` | 文章详情，含正文与 301 指示 |
| 3 | GET | `/replay/site/article/related` | 文末相关文章 |
| 4 | GET | `/replay/site/category/list` | 分类列表（含已发布文章数） |
| 5 | GET | `/replay/site/tag/list` | 标签云 |
| 6 | GET | `/replay/site/sitemap` | sitemap 全量数据 |

### 1. GET /replay/site/article/list

入参：`page`（默认 1）、`limit`（默认 10，**上限 50**）、`categorySlug`、`tagSlug`。

`categorySlug` 与 `tagSlug` **互斥**，同传返回业务错误。
slug 不存在时**返回错误而不是空列表**——官网据此 404，返回 200 空页会被搜索引擎收录。

响应 data：

```jsonc
{
  "page": { "totalCount": 37, "pageSize": 10, "totalPage": 4, "currPage": 2, "list": [
    { "title": "...", "slug": "...", "summary": "...", "coverUrl": "...",
      "categoryName": "直播运营", "categorySlug": "zhibo-yunying",
      "tags": [ { "tagName": "复盘", "slug": "fupan" } ],
      "publishTime": "2026-08-15 10:30:00" }
  ]},
  "category": { "categoryName": "直播运营", "slug": "...", "description": "...", "articleCount": 6 },
  "tag": null
}
```

`category` / `tag` 仅在按对应 slug 查询时非空，供页面渲染 h1 与 meta description，
这样分类页 SSR 只需打一次后端。

### 2. GET /replay/site/article/detail

入参 `slug`。三种结果：

| 情况 | 响应 | 官网行为 |
|---|---|---|
| 命中已发布文章 | 完整详情，`redirectSlug` 为 null | 正常渲染 |
| slug 已迁移且目标仍在 | **只含 `redirectSlug`**，其余字段为 null | 发 301 |
| 不存在 / 已下架 / 已删除 | 业务错误 | 404 |

出参在列表项基础上增加：`content`、`seoTitle`、`seoDescription`、`seoKeywords`、
`updateDate`、`categoryDescription`、`redirectSlug`。

### 3. GET /replay/site/article/related

入参 `slug`、`limit`（默认 4）。共享标签数降序，不足时用同分类最新补齐。
当前文章无标签时不报错，直接走兜底。响应 data 是 `SeoSiteArticleListVo` 数组。

### 4. GET /replay/site/category/list

无入参。按 `sort` 升序，**只返回有已发布文章的分类**（0 篇的不返回）。
出参：`categoryName` / `slug` / `description` / `articleCount`。

### 5. GET /replay/site/tag/list

无入参。只返回启用中且有已发布文章的标签，按文章数降序。
出参：`tagName` / `slug` / `articleCount`。

**不做 `< 3` 过滤**——标签云要展示全部有内容的标签，
薄内容页的 `noindex` 由官网在标签页用 `totalCount` 自行判断。

### 6. GET /replay/site/sitemap

无入参、不分页。

```jsonc
{
  "articles":   [ { "slug": "...", "updateDate": "2026-08-17 10:30:00" } ],
  "categories": [ { "slug": "...", "updateDate": "..." } ],
  "tags":       [ { "slug": "...", "updateDate": "..." } ]
}
```

- `categories` 的 `updateDate` 取该分类下**最新文章**的时间（分类自身的 update_date
  改个描述就会变，不代表内容更新）；
- `tags` **只含已发布文章 ≥ 3 篇的**——阈值 `SeoConstant.SITE_TAG_INDEX_MIN_COUNT`
  由后端统一把控，官网不再重复判断。

## slug 变更与 301

新增表 `tb_seo_slug_history`（`sql/replay-46.sql`）。
文章/分类/标签保存时若 slug 发生变化，自动记一条旧值；**逻辑删除时不记**
（删除后旧地址应当 404 而不是 301）。

**只记 `old_slug`，不记新值**——新值永远从实体表现查。这样多次改名（x→y→z）时，
查 x 和查 y 都一步跳到最终的 z，不产生 301 跳转链。

**查询顺序必须先当前表、后历史表**，该顺序自动处理三个边界：
slug 改回原值、旧 slug 被新实体占用、目标实体已删除。

## 日期格式

`publishTime` / `updateDate` 序列化为 **`yyyy-MM-dd HH:mm:ss`**（全局 JacksonSerializerConfig），
**不是 ISO 8601**。官网侧必须自行转成 ISO 8601 再喂给 `og:article:published_time`
与 JSON-LD 的 `datePublished`，否则搜索引擎静默忽略该字段；
另外 `new Date('2026-08-17 10:30:00')` 在 iOS Safari 上返回 `Invalid Date`。
