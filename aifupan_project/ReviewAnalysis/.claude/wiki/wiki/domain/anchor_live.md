# 主播与直播域 (Anchor & Live)

## 核心概念

### Anchor (主播)
直播主播实体，是系统的核心管理对象。每个主播通过 `secUid` 唯一标识，包含录制设置、平台授权状态、诊断配置等属性。

**实体文件**: `entity/anchor/AnchorEntity.cs`

**关键字段**:

| 字段 | 类型 | 说明 |
|---|---|---|
| id | long | 主键 |
| anchorInfo | AnchorUrlInfoVo | 主播 URL 信息 |
| anchorUrlSecUid | string | 主播 SecUid (唯一标识) |
| remarksName | string | 备注名 |
| folderName | string | 存储文件夹名 |
| isTop | int | 是否置顶 |
| smsTip | int | 通知模式 (0无/1开播/2下播/3全部) |

### Recording (录制)
直播录制功能，支持自动检测开播录制、排班录制、分段录制等多种模式。

**相关字段 (AnchorEntity)**:

| 字段 | 类型 | 说明 |
|---|---|---|
| isAutoRecord | int | 是否自动录制 |
| isScheduleRecord | int? | 是否排班录制 |
| recordTime | string | 录制时间范围, 如 `06:00:00-19:00:00` |
| recordDefinition | int? | 画质 (-1跟随系统/0标清/1高清/2超清/3蓝光) |
| recordLimitType | int? | 录制模式 (-1跟随/0无限/1限时单段/2限时分段) |
| recordLimitValue | int? | 限制时长(分钟) |
| recordTimeMode | int? | 时间模式 (0按视频时长/1按北京时间/2按固定时间点) |
| segmentTimePoints | string | 分段时间点, 如 `09:30,10:40,11:20` |
| lastRecordTime | string | 最后一次录制时间 |
| isRemoveRecord | int | 移除状态 (0正常/1已移除/2从回收站移除) |
| deleteDate | string | 移除时间 |

**录制控制流程**:
1. Controller 触发录制 → `AnchorBll` 管理录制生命周期
2. 更新 `recordingList` (当前正在录制的主播集合)
3. `WebsocketConnection` 根据 `recordingList` 自动开启/关闭弹幕采集
4. 启动前检查：本地时钟精度 + 磁盘空间 ≥ 10GB

### BarrageGrab (弹幕抓取)
外部弹幕采集子进程 `BarrageGrab.exe`，通过 stdout 输出解析后的直播间事件。

**事件类型**:

| 标记 | 事件 | 说明 |
|---|---|---|
| fensituan | FensTuan | 粉丝团加入 |
| danmu | DanMu | 弹幕消息 |
| guanzhu | GuanZhu | 关注 |
| renshu | RenShu + ChangGuan | 在线人数 + 场观 |
| 主播下播 | 错误/状态 | 主播已下播 |
| 网络断掉 | 错误 | 网络连接断开 |
| 30s内无数据 | 错误 | 无数据超时 |

### WebSocket 连接
通过 `WebsocketConnection` 统一管理所有主播的弹幕/数据 WebSocket 连接。

**两种采集模式**:
- `gatherWay = 0`: 直接模式，通过 BarrageGrab.exe 连接
- `gatherWay = 1`: 浏览器模式，通过 CefSharp 获取 WebSocket 地址

**连接状态 (websocketLinkStatus)**:
- `0` — 未连接
- `1` — 已连接
- `2` — 启动中
- `3` — 待重连
- `4` — 错误

**数据流**: BarrageGrab.exe stdout → `SendWebsocketDataFromLine` → `SendWebsocketData` → `WebsocketDataHandle.Push` → 持久化 + UI 推送

## 状态机

### 主播移除状态 (isRemoveRecord)
```
正常(0) → 已移除(1) → 从回收站移除(2)
             ↓
         可恢复(reAddAnchor)
```

### 录制生命周期
```
待机 → 检测开播 → 开始录制 → 录制中 → 停止录制 → (自动分析)
                                         ↑
                                    分段录制循环
```
