---
name: "需求版本管理器"
description: "管理 docs/需求版本库 的版本化需求与接口入口。用于新增版本、查询历史版本、排查版本差异，并将稳定摘要蒸馏到 agent/MEMORY.md。"
---

# 需求版本管理器

## 目标
- 把“每个版本的需求/接口/原型/整合结论”按版本号归档到 `docs/需求版本库/<版本>/`
- 支持按版本快速定位：PRD、整合版、接口文档、差异/缺失报告
- 支持排查：对比当前版本与历史版本的入口变化与关键结论差异
- 只把“稳定入口与规则摘要”写入 `agent/MEMORY.md`，不把版本细节写入记忆库

## 何时使用
- 新增/更新某个版本的 PRD、接口文档、原型数据，需要登记入口并形成可追溯索引
- 线上/测试出现问题，需要按版本回溯“当时的需求口径/接口口径/验收口径”
- 需要把版本库结构与使用规则固化为团队共识

## 输入
- `version`：必填。版本号，例如 `2.60.3`
- `changes`：选填。本次新增/更新的文件清单与变更摘要
- `evidence`：选填。来源线索（PRD 链接、接口文档、原型节点、发布说明等）

## 输出
- `version_entry`：版本库登记结果（README 行/段落 + 版本目录结构）
- `meta_json`：版本目录 `meta.json` 的新增/更新建议
- `query_guide`：按版本查询的最短路径说明
- `diff_checklist`：版本差异排查清单
- `memory_patch`：可写入 `agent/MEMORY.md` 的稳定摘要（仅结构与入口规则）

## 目录与文件约定（以仓库现状为准）
- 版本库入口：`docs/需求版本库/README.md`
- 单版本目录：`docs/需求版本库/<版本>/`
- 单版本必备文件（推荐最小集）：
  - `meta.json`：登记 canonical 入口与说明
  - `PRD.md`：主 PRD（唯一准绳）
  - `integrated.md`：整合版（补齐流程/文案，并集中记录待确认）
  - `source.extracted.md`：抽取版（仅辅助定位原始口径）
  - `接口.md` / `接口缺失报告.md`：接口说明与缺口
  - `figma/`：原型数据与导出（如有）

## 新增/更新版本流程（推荐）
1. 创建/更新 `docs/需求版本库/<版本>/` 目录与文件
2. 填写/更新 `docs/需求版本库/<版本>/meta.json`
   - `canonical.primary_prd` / `canonical.integrated` / `canonical.source_extracted`
   - 如有原型：登记 `canonical.figma_dir` 与 `source.figma.meta/nodes`
3. 在 `docs/需求版本库/README.md` 登记版本入口（版本号 → 目录 → 关键文件）
4. 若发现冲突口径：只追加到该版本 `integrated.md` 的“待确认事项”，不要写进 MEMORY
5. 蒸馏摘要：仅把“入口规则与版本库结构”更新到 `agent/MEMORY.md`

## 按版本查询（最短路径）
1. 打开 `docs/需求版本库/README.md`，找到目标版本
2. 进入 `docs/需求版本库/<版本>/meta.json`
3. 优先阅读 `canonical.primary_prd`，再读 `canonical.integrated`，最后按需读接口/原型/抽取版

## 版本差异排查清单（建议）
- 入口差异：对比两个版本的 `meta.json.canonical` 指向是否变化
- 口径差异：对比两个版本 `PRD.md` 的 Requirements/Acceptance
- 接口差异：对比 `接口.md`/缺失报告 是否新增/废弃接口或字段
- 原型差异：对比 figma nodes 是否变化、是否存在关键页面新增/删除
- 待确认项：对比 integrated.md 的“待确认事项”是否已关闭/转移

## MEMORY 蒸馏规则（强制）
- 允许进入 `agent/MEMORY.md`：
  - 版本库入口路径、目录结构、使用优先级规则（PRD > integrated > extracted）
- 禁止进入 `agent/MEMORY.md`：
  - 某个版本的具体需求细节、接口字段细节、临时结论、待确认项细节

## 失败模式
- 只更新了版本目录但忘记更新 `docs/需求版本库/README.md`，导致入口不可发现
- 把“待确认事项”写进 MEMORY，导致长期记忆污染
- 以整合版替代主 PRD 作为实现准绳，导致口径漂移
