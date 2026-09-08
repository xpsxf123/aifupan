# Patch（通用补丁包）

本目录是“多 Agent 提示词工程兼容补丁”的可复制模板包。

设计目标：

- 单一来源只维护一份（例如 `agnet/`）
- 每个工具只写自己的“适配器”（入口桥 / 规则目录 / hooks）
- 适配器必须把工具默认加载链路引导回 `BOOTSTRAP`

## 使用方式（交给其他 Agent 的一句话）

1) 先自检：你（目标工具/Agent）会自动读取哪些入口文件/目录？是否支持 hooks？  
2) 再对照 `adapter-checklist.md` 落地你的适配器。  
3) 最终让你的入口桥接回单一来源 `BOOTSTRAP.md`。  

## 文件说明

- `agent-bridge.manifest.sample.json`
  - 机器可读的桥接清单示例（建议每个项目复制并改成真实版本）
- `adapter-checklist.md`
  - 适配器实现与验收清单（跨工具通用）

