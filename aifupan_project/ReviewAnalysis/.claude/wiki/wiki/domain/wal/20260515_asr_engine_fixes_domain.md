---
name: asr-engine-fixes-domain-20260515
description: 本地ASR引擎后处理修复 — CIF时间戳对齐、标点处理、引擎管道架构经验
metadata:
  type: project
---

## ASR 引擎后处理修复 (2026-05-15)

### CIF 时间戳对齐修复

**根本问题**: `ComputeTimestamps` 原签名接收 raw tokenIds，导致 fire point k 被错误映射到 raw token 位置 k，而非 filtered content token 位置 k。lang/textnorm 前缀 token 造成系统性偏移，超出范围的 token 返回 -1。

**修复**: 签名改为 `ComputeTimestamps(cifPeak, filteredCount)`，timestamps 按 filtered 顺序建立（`timestamps[i]` 对应 filteredToken[i]），MergeWords 直接用 `i` 索引。

**架构约束**: CIF fire point k → filtered content token k（直接一一对应）。raw token 中的 lang/textnorm/blank 不消耗 CIF fire。

### 标点处理修复

**问题**: 句首或连续标点（`currentChars.Count == 0` 时遇到 isPunct）被静默丢弃。

**修复**: 增加 else 分支，孤立标点作为独立 `ASRWordEntity` 输出（`Word = "。"`, 带 timestamp）。

**对下游影响**: Tencent 路径不受影响；SVS 路径极罕见触发（仅在静音段后模型输出首位标点时）。

### CIF 累积 while 修复

原 `if (accumulated >= 1.0f)` 改为 `while`，处理单帧权重 > 1.0 的极端情形（防止漏 fire）。

### FilterSpecialTokens 简化

`FilterSpecialTokensWithIndices` 返回 (tokens, originalIndices) 的设计已无必要（original indices 仅用于时间戳查找，现在已按 filtered 顺序建立）。简化为 `FilterSpecialTokens` 返回 `int[]`。
