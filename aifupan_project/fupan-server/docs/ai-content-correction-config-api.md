# AI内容校正配置接口

## 接口信息

| 项目 | 说明 |
|------|------|
| 请求方式 | GET |
| 接口路径 | `/replay/ai/getContentCorrectionConfig` |
| Content-Type | 无需传 Body，参数通过 QueryString 传递 |

## 请求参数

| 参数名 | 类型 | 必填 | 位置 | 说明 |
|--------|------|------|------|------|
| sceneType | int | 是 | Query | 场景类型（见下方枚举） |

### sceneType 枚举

| 值 | 说明 |
|----|------|
| 0 | AI问答助手的纠正检查 |
| 1 | AI问答助手的纠正 |
| 2 | 自然、优化原文的纠正检查 |
| 3 | 自然、优化原文的纠正 |

## 响应结构

```json
{
  "code": 0,
  "msg": "返回成功",
  "data": {
    "modelCode": "模型code",
    "contentPrompt": "提示词内容"
  }
}
```

### 响应字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| code | int | 状态码，0 表示成功 |
| msg | string | 提示信息 |
| data | object | 业务数据 |
| data.modelCode | string | 模型 code |
| data.contentPrompt | string | 提示词内容 |

### 未找到场景配置时的响应

当传入的 sceneType 在字典中未配置时，`data` 为 `null`：

```json
{
  "code": 0,
  "msg": "返回成功",
  "data": null
}
```

## C# 调用示例

### 模型定义

```csharp
/// <summary>
/// 请求参数
/// </summary>
public class AiContentCorrectionConfigRequest
{
    public int SceneType { get; set; }
}

/// <summary>
/// 通用响应封装
/// </summary>
public class ApiResponse<T>
{
    public int Code { get; set; }
    public string Msg { get; set; }
    public T Data { get; set; }
}

/// <summary>
/// AI内容校正配置
/// </summary>
public class AiContentCorrectionConfigVo
{
    public string ModelCode { get; set; }
    public string ContentPrompt { get; set; }
}
```

### 调用代码

```csharp
using System.Net.Http;
using System.Text.Json;
using System.Threading.Tasks;

public async Task<AiContentCorrectionConfigVo> GetContentCorrectionConfig(int sceneType)
{
    var baseUrl = "https://your-server:6606";
    var url = $"{baseUrl}/replay/ai/getContentCorrectionConfig?sceneType={sceneType}";

    using var httpClient = new HttpClient();
    var response = await httpClient.GetAsync(url);
    response.EnsureSuccessStatusCode();

    var json = await response.Content.ReadAsStringAsync();
    var apiResponse = JsonSerializer.Deserialize<ApiResponse<AiContentCorrectionConfigVo>>(json, new JsonSerializerOptions
    {
        PropertyNameCaseInsensitive = true
    });

    if (apiResponse.Code != 0)
    {
        throw new Exception($"接口调用失败：{apiResponse.Msg}");
    }

    return apiResponse.Data;
}
```
