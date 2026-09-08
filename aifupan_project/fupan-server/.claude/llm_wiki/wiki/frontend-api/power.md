# 用户管理（power 模块）API（前端对接稿）

> 本模块所有响应均符合 [`R<T>` 统一响应封装](./_response_envelope.md)；本文档不重复说明 `code` / `msg` / `data`，**响应 data 结构**指 `R<T>` 内的 `T`。
> Snowflake ID（19 位）→ 前端必须用 `string` 接收，禁 `number`。后端 `JacksonSerializerConfig` 已全局把 **所有 `Long` 序列化为 String**。
>
> 错误码全集 → [./_error_codes.md](./_error_codes.md)
>
> 最后更新：2026-07-24 ｜ 来源（**reverse 回校**，以实现代码为准）：`replay-api/.../controller/power/UserController.java:41,432` + `replay-power/.../vo/UserListVo.java:131-143` + `replay-power/.../vo/LatestRemarkVo.java` + `replay-api/.../logic/power/impl/UserLogicImpl.java:1164-1221`
> 已归档正文（长期可追溯）→ [../../archive/20260724_userlist-power-remark.md](../../archive/20260724_userlist-power-remark.md)
> **实现状态：✅ 已实装**（Phase 6 Archive 已对实现代码做 reverse 回校：字段名 / 类型 / 序列化形态 / 兜底语义均与代码一致）

---

## 接口列表

| # | 方法 | 路径 | 鉴权 | 简述 |
|---|---|---|---|---|
| 1 | POST | `/replay/user/pageListNew` | 登录（管理后台） | 管理后台用户列表分页查询 |

> ⚠️ **路径前缀说明（前端无需改动任何调用代码）**
> - **服务端完整路径**：`/replay/user/pageListNew` —— `UserController` 类级 `@RequestMapping("replay/user")`（`UserController.java:41`）+ 方法级 `@PostMapping("/pageListNew")`（`:432`），服务无 `server.servlet.context-path`。
> - **前端代码里写什么**：仍是 `'/user/pageListNew'`。因为 `fupan-admin` 的 `VITE_BASE_URL` 已包含 `/replay`（见 `.env.*`，如 `https://testapi.aifupan.com.cn/replay`），axios `baseURL` 拼接后即为完整路径。现有 `src/api/request-api.js:20` 的写法**正确，不要改**。
> - ❗ **切勿**在前端调用里再加 `/replay` 前缀 —— 会拼成 `/replay/replay/user/pageListNew` 导致 404。

> 本文档目前**只覆盖 `POST /replay/user/pageListNew` 的本次字段增量**，不是该端点的全量契约。请求参数与其余既有响应字段以现网实现为准，本次变更**一律不动**。

---

## 1. POST `/replay/user/pageListNew`

**描述**：管理后台用户列表分页查询（既有端点）。本次变更为 **additive**：仅在响应 `data.list[*]` 每行新增三个**只读**字段，供前端 hover 展示。

**请求参数**：**完全不变**。不新增、不修改任何请求字段（`UserListBo` 不动），排序白名单也不变。前端**无需**为本次变更改动任何请求代码。

### 1.1 本次新增的响应字段（`data.list[*]`）

| 字段 | 后端类型 | JSON 类型 | 可空 | 说明 |
|---|---|---|---|---|
| `userPowerConsume` | Long | string | 否（空取 `0`） | 该用户累计算力消耗，单位 = **原始 token 数**。rollup 表无对应行时后端兜底为 `0`，不会返回 `null` |
| `tenantPowerConsume` | Long | string | 否（空取 `0`） | 该用户**所属租户（主账号维度）全体**累计算力消耗，单位 = 原始 token 数。无对应行时兜底为 `0` |
| `latestRemark` | LatestRemarkVo | object | **是**（无跟进记录时整体为 `null`） | 最新一条跟进记录。按 `create_date DESC, id DESC` 取首条 |
| `latestRemark.remark` | String | string | 否 | 跟进内容 |
| `latestRemark.remarkTime` | Date | string | 否 | 记录时间（跟进记录的 `create_date`），格式 `yyyy-MM-dd HH:mm:ss`，时区 GMT+8 |
| `latestRemark.createName` | String | string | **是** | 跟进人姓名。跟进人账号已删除 / 解析不到时为 `null`，前端需做空值兜底 |

> **`latestRemark` 的降级行为**：跟进记录为**旁路数据**，后端批量查询失败时会捕获异常并降级为空（`UserLogicImpl.java:1173-1175`），此时**整页所有行**的 `latestRemark` 均为 `null`，但列表主数据照常返回。前端必须容忍"整页无跟进"，不得据此判定接口失败。

> **`Long` → JSON `string`**：`userPowerConsume` / `tenantPowerConsume` 后端类型为 `Long`，受项目全局 `JacksonSerializerConfig` 影响，**线上以 JSON 字符串下发**（如 `"1234567"`）。前端做数值运算/格式化前需先 `Number()` / `BigInt()` 转换。参见 [_response_envelope.md 字段类型约定](./_response_envelope.md)。

### 1.2 前端必须知道的两个业务口径 ⚠️

**口径 ①：数据新鲜度 —— 两个算力值不是实时值（T+1）**

`userPowerConsume` / `tenantPowerConsume` 均取自**离线汇总表**，由每日定时任务（XXL-Job `timingUpdatePowerConsume`）刷新，**当日产生的消耗次日才可见**。

- 前端展示处**必须**标注类似「数据截至昨日」「T+1 更新」的说明，避免用户以为是实时余额/实时用量。
- 用户刚做完一次消耗后立刻刷新列表，数值**不会**变化，这是预期行为，不是 bug。
- 对照：同一行的 `latestRemark` 是**实时值**（跟进一提交，刷新即可见），两者新鲜度不同，文案不要混写。

**口径 ②：数值口径 —— 净额、原始 token 数**

- **净额**：算力消耗已**扣减「执行失败回退」**的部分（失败任务退回的 token 不计入消耗）。因此它是「实际净消耗」，而非「累计发起量」。
- **⚠️ 可能为负**：净额口径下该值**理论上可能为负**（数据异常时，如回退量大于扣减量）。前端**不要**假定非负、不要因负值判定为脏数据或报错；按原样展示即可（负值本身是需要排查的信号）。正常情况下恒为非负。
- **单位 = 原始 token 数**：既不是金额（元/分），也不是「张数」「次数」「条数」。任何面向用户的换算（如 token → 万 token、token → 折算金额）**由前端自行处理**，后端只给原始整数。
- **租户口径**：`tenantPowerConsume` 按用户**所属主账号**归集 —— 主账号自身 + 其名下全部子账号的消耗之和。
  - 子账号行：`userPowerConsume` 是该**子账号自己**的消耗（不并入父账号）；`tenantPowerConsume` 是其主账号所在租户的合计。
  - 主账号行：`userPowerConsume` 是主账号自己的消耗；`tenantPowerConsume` 是其所属租户的合计。
  - 因此**同租户下多行的 `tenantPowerConsume` 相同**，且 `userPowerConsume ≤ tenantPowerConsume`（同租户内），前端不要把两者当成互相独立的两个指标去做加总。

### 1.3 使用限制

| 限制 | 说明 |
|---|---|
| **仅本端点有值** | 三个字段只在 `POST /replay/user/pageListNew` 返回值。其它同样返回 `UserListVo` 的端点（`pageList` / `selectClientList` / `subAccountList` / `listByIds` 等）三字段**恒为 `null`**。前端**不要**依赖它们在别处出现，也不要据此写共用的展示组件默认值 |
| **不支持排序** | 三字段未进排序白名单，不能作为 `orderBy` 参数值 |
| **不支持筛选** | 无对应查询条件，请求侧未新增任何参数 |
| **不支持导出** | 导出链路未接入这三个字段 |
| **只读** | 无任何写入/编辑入口 |

### 1.4 示例响应（仅列新增部分，其余字段一律不变）

```json
{
  "code": 0,
  "msg": "OK",
  "data": {
    "list": [
      {
        "id": "<既有字段，Snowflake string>",
        "userPowerConsume": "1234567",
        "tenantPowerConsume": "98765432",
        "latestRemark": {
          "remark": "客户已确认续费意向",
          "remarkTime": "2026-07-20 15:04:05",
          "createName": "张三"
        }
      }
    ],
    "totalCount": 100,
    "pageSize": 10,
    "totalPage": 10,
    "currPage": 1
  }
}
```

无算力流水 / 无跟进记录时（对应 openspec AC-3）：

```json
{
  "userPowerConsume": "0",
  "tenantPowerConsume": "0",
  "latestRemark": null
}
```

> 分页外层字段名（`totalCount` / `pageSize` / `totalPage` / `currPage` / `list`）取自 `replay-generic/.../utils/PageUtils.java:32-52`，与 [_response_envelope.md](./_response_envelope.md) 中示例的 `total` / `pageNum` 命名不同，本次变更**不改动**它。

### 1.5 TypeScript 类型参考

```typescript
/** 最新一条跟进 */
interface LatestRemarkVo {
  /** 跟进内容 */
  remark: string;
  /** 记录时间，yyyy-MM-dd HH:mm:ss（GMT+8） */
  remarkTime: string;
  /** 跟进人姓名；跟进人已删除时为 null */
  createName: string | null;
}

/** pageListNew 行记录的本次增量部分（其余既有字段省略） */
interface UserListVoPowerDelta {
  /** 用户累计算力消耗（原始 token 数，净额，T+1）。后端 Long → JSON string；空取 "0" */
  userPowerConsume: string;
  /** 所属租户全体累计算力消耗（原始 token 数，净额，T+1）。空取 "0" */
  tenantPowerConsume: string;
  /** 最新跟进（实时）；无跟进记录时为 null */
  latestRemark: LatestRemarkVo | null;
}
```

---

## 变更历史

- 2026-07-24：初次发布 —— `POST /user/pageListNew` 新增 `userPowerConsume` / `tenantPowerConsume` / `latestRemark` 三个只读字段（forward 模式，来源 openspec `Change__2026-07-23_19-18-09__userlist-power-remark` §3 / §4.2 / §7；后端尚未实装）
- 2026-07-24：**reverse 回校（Phase 6 Archive）** —— 字段名 / 类型 / 序列化形态 / 兜底语义与实现完全一致，无字段级漂移。修正三处文档侧偏差：① **端点路径纠错** `/user/pageListNew` → `/replay/user/pageListNew`（漏了 `UserController` 类级前缀 `replay/user`，前端按旧路径会 404）；② 分页外层补 `totalPage`（`PageUtils` 实际含 5 个字段）；③ 补充 `latestRemark` 的**整页降级为 null** 行为（后端捕获异常后降级，列表主数据不受影响）
