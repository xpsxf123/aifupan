---
name: asr-serialization-rules
description: ASR/WordsMark 上报序列化字段名规则：必须 PascalCase + 显式 JsonProperty
metadata:
  type: rules
---

## 规则：ASR 上报对象的 JSON 字段名约定（2026-05-21）

### 规则 1：上报到 `/openapi/v2000/wordsMark` 的 items 字段必须 PascalCase

**Why**：后端 `WordsMarkItemBo` 用 Jackson 注解 `@JsonProperty("Word")` / `@JsonProperty("StartTime")` / `@JsonProperty("EndTime")` 强制匹配 PascalCase。当 JSON 是 camelCase 时，Jackson 反序列化失败 → endTime=null → `getEndTime().longValue()` NPE。

**How to apply**：在 `ASRWordEntity` 上**显式**声明 PascalCase JsonProperty，不依赖默认行为：
```csharp
using Newtonsoft.Json;

public class ASRWordEntity
{
    [JsonProperty("Word")]
    public string Word { get; set; }

    [JsonProperty("StartTime")]
    public long StartTime { get; set; }

    [JsonProperty("EndTime")]
    public long EndTime { get; set; }
}
```

---

### 规则 2：禁止依赖"Newtonsoft 默认按 C# 属性名输出"

**Why**：项目里某处（待查）的 Newtonsoft 行为实测发现会把 PascalCase 属性输出为 camelCase。不能信任默认 ContractResolver。任何要直接发到后端的 DTO 都必须显式 JsonProperty。

**How to apply**：所有用作上报 body 的 C# DTO 类，凡是有约定字段名的字段都加显式 `[JsonProperty("约定名")]`。即使约定名 == 属性名也加，作为契约锁。

---

### 规则 3：Tencent 反序列化路径不受 PascalCase JsonProperty 影响

**Why**：Newtonsoft 反序列化默认大小写不敏感。Tencent 返回 PascalCase JSON，C# 端 `[JsonProperty("Word")]` 也是 PascalCase → 直接匹配；即使 Tencent 某次返回 camelCase，case-insensitive 也能填充。

**How to apply**：放心给同一 DTO 同时加显式 JsonProperty 与 Tencent 路径共用，不会引发回归。

---

### 规则 4：诊断日志前缀 `[ASR][diag]` 仅用于临时排错

**Why**：本次"P1 单字"诊断过程加了多处 `[ASR][diag]` 日志（WordsMark 请求体、SVS 内部 token 流、ComputeTimestamps CIF/CTC 分支、MergeWords 出口等），调试完毕后已全部清除。

**How to apply**：未来再排查 ASR 链路问题：
- 临时日志统一前缀 `[ASR][diag]` 便于 grep
- 排错完成必须清除，不留生产噪音
- 业务级日志（如 `[SVS] 识别完成`、`语音识别完成一共`）保留

---

### 历史教训
- 看到 NPE 报 `endTime is null` 时，第一反应是检查**对端契约**（即后端 BO 的 JsonProperty 注解），不要凭"服务端响应是 camelCase 就推测入参也是 camelCase"
- 服务端响应的 camelCase 是 Java 字段名（小写），与 `@JsonProperty` 入参映射独立
- **改对外契约前必须先看对端代码**

### 相关
- 主域契约：[[asr-audio-text-sync-domain]]
- 编码规范：`coding_standards.md`
