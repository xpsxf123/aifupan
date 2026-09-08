# 视频处理定时任务

## 任务信息

| 项目 | 说明 |
|-----|------|
| 类 | VideoTask |
| 方法 | handleVideo |
| XxlJob Handler | videoJobHandler |
| 分布式锁 | governance:task:video:lock（Redis，2分钟过期） |

## 任务描述

定时扫描 `live_video` 表中待处理的视频，按 `(tenant_id, batch_number)` 分组批量处理，将视频数据聚合为场次、计算业绩、保存商品数据。

## 执行流程

```
handleVideo()
│
├─ 1. Redis 分布式锁（setIfAbsent，2分钟过期）
│     ├─ 获取失败 → 跳过本次执行
│     └─ 获取成功 → 继续
│
├─ 2. 重置超时视频
│     └─ resetTimeoutProcessingVideos(30分钟)
│        处理中超过30分钟的视频 → 重置为待处理
│
├─ 3. processVideos()
│     ├─ 3.1 获取待处理分组（最多10组）
│     │     getPendingVideoGroups(10)
│     │     → List<PendingVideoGroup(tenantId, batchNumber)>
│     │
│     └─ 3.2 遍历每组 → processVideoGroup(group)
│           │
│           ├─ fetchAndLockPendingVideos(tenantId, batchNumber)
│           │   查询待处理视频，状态改为「处理中」，返回视频列表
│           │
│           ├─ doProcessVideos(videos)
│           │   └─ liveSessionService.processVideosToSession(videos)
│           │       ├─ 生成/更新场次（live_session）
│           │       ├─ 处理商品数据（product + session_product）
│           │       ├─ 合并实时数据上传OSS
│           │       ├─ 计算场次业绩（session_performance）
│           │       └─ 计算排班业绩（schedule_performance + detail + staff）
│           │
│           └─ finally: 更新处理结果
│               ├─ 成功 → batchUpdateProcessSuccess(videoIds)  → status=2
│               └─ 失败 → batchUpdateProcessFailed(videoIds, reason) → status=3
│
└─ finally: 释放 Redis 锁
```

## processVideosToSession 详细流程

```
processVideosToSession(videos)
│
├─ 1. 场次处理
│     ├─ 查询直播间（通过 secUid）
│     ├─ 查询场次是否存在（tenant_id + batch_number）
│     ├─ 不存在 → 创建新场次（source=系统，合并视频业绩）
│     ├─ 存在 + source=系统 → 乐观锁更新场次数据
│     └─ 存在 + source=手动 → 不更新，记录原始值到 session_original_value
│
├─ 2. 商品处理
│     ├─ 查询视频关联的商品（video_product）
│     ├─ 按 productId 合并（数值取最大值）
│     ├─ 保存/更新商品基础信息（product）
│     └─ 保存/更新场次商品关联（session_product）
│
├─ 3. 合并实时数据上传OSS
│     ├─ 下载每个视频的 zip → 解压 JSON → 反序列化 OceanEngineProcessBo
│     ├─ 按 gatherTimeStamp 去重，排序
│     └─ 序列化 → 压缩 zip → 上传OSS，更新场次 oss_url
│
├─ 4. 计算场次业绩（session_performance）
│     ├─ 不跨天 → 直接使用场次业绩数据
│     └─ 跨天 → 按天拆分
│           ├─ 有实时数据 → 差值计算
│           └─ 无实时数据 → 时间比例分配
│     upsert 规则：
│     ├─ 不存在 → 插入
│     ├─ 存在 + source=系统 → 覆盖更新
│     └─ 存在 + source=手动 → 记录原始值
│
└─ 5. 计算排班业绩（需有实时数据）
      ├─ 查询排班（LiveRoomScheduleService.getRangeTimeLiveSchedule）
      ├─ 计算每个排班与场次的重叠时间段内的业绩差值
      ├─ 保存贡献明细（schedule_performance_detail，按 session_id upsert）
      ├─ 汇总明细 → 回写主表（schedule_performance）
      │   ├─ source=系统 → SUM 回写
      │   └─ source=手动 → 不回写，记录原始值
      └─ 保存排班业绩人员（schedule_performance_staff）
```

## 关联表

| 表名 | 说明 | 操作 |
|-----|------|------|
| live_video | 视频表 | 读取待处理、更新状态 |
| live_session | 场次表 | 创建/更新 |
| video_product | 视频商品表 | 读取 |
| product | 商品表 | 创建/更新 |
| session_product | 场次商品关联表 | 创建/更新 |
| session_performance | 场次业绩表 | 创建/更新 |
| schedule_performance | 排班业绩表 | 创建/更新（汇总回写） |
| schedule_performance_detail | 排班业绩贡献明细表 | 创建/更新（按场次upsert） |
| schedule_performance_staff | 排班业绩人员表 | 创建 |
| session_original_value | 原始值表 | 创建（手动来源时记录） |

## 关键参数

| 参数 | 值 | 说明 |
|-----|---|------|
| LOCK_EXPIRE_MINUTES | 2 | Redis 锁过期时间（分钟） |
| PROCESS_TIMEOUT_MINUTES | 30 | 处理超时阈值（分钟），超时重置为待处理 |
| BATCH_GROUP_LIMIT | 10 | 每次最多处理的分组数 |
| MAX_RETRY | 3 | 乐观锁更新最大重试次数 |

## 错误处理

- 单个分组处理失败不影响其他分组
- 失败时更新视频状态为 `FAILED(3)`，记录失败原因到 `fail_reason`
- 超时的处理中视频会在下一次任务时被重置为待处理
