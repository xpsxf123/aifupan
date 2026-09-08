# WAL — ASR 后端接口变更

**日期**: 2026-05-18
**类型**: API WAL

## 新增接口调用

### POST `/audio/asr-engine`
- **位置**: `api/AsrApi.GetAvailableEnginesAsync(string language)`
- **用途**: 查询当前用户可用的 ASR 引擎列表（权限查询，非识别）
- **请求体**: `{ language: "16k_zh" }`
- **响应**: `{ code: 0, data: ["sense-voice", "tencent"] }`（顺序 = 优先级）
- **失败处理**: 返回 null，调用方兜底默认 Composite(SVS + Tencent)
- **注意**: 后端不做识别，识别全程在客户端本地完成

## 旧接口保留
- GET `/audio/getTempToken` — 获取腾讯 ASR 临时凭证，未变动
