---
name: naudio-wasapi-dependency
description: 不使用 NAudio.dll——MP3 解码改用 FFmpeg 转 WAV，WAV 用纯 C# 读取
metadata:
  type: rules
---

本项目已彻底移除所有 NAudio 依赖（NAudio.dll / NAudio.Core / NAudio.Wasapi）。

**Why:** NAudio.dll (net472) 的清单依赖链：NAudio.Wasapi → NAudio.WinMM → NAudio.Asio → NAudio.Midi → NAudio.WinForms，缺任意一个就 FileNotFoundException 崩溃。根本原因是项目仅用 AudioFileReader 做 MP3→PCM 解码，却拖入了整套音频设备子包。

**现有方案 (WavFrontend.DecodeMp3):**
- 输入是 .wav → 纯 C# BinaryReader 解析 WAV header 读 PCM float[]
- 其他格式 → 调 tools\ffmpeg 转为临时 WAV(16kHz mono) → 读取 → 删除临时文件

**How to apply:** 禁止重新引入任何 NAudio 包。音频解码需求统一走 FFmpeg (tools\ffmpeg.exe)。
