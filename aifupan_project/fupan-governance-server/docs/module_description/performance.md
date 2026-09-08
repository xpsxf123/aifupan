---
name: "performance-module"
description: "业绩模块开发规范：数据模型、视频处理流程、分摊算法、乐观锁、多租户隔离、异步任务。开发爱复盘业绩统计相关功能时调用。"
---

# 业绩模块开发规范

业绩模块用于接收爱复盘系统推送的直播数据，进行多维度业绩统计（组织、场次、排班、人员、每日、商品等）。

## 1. 数据模型

### 1.1 通用字段规范

所有表必须包含以下字段：

```java
// 租户隔离
@TableField("tenant_id")
private Long tenantId;

// 审计字段
@TableField(fill = FieldFill.INSERT)
private LocalDateTime createDate;

@TableField(fill = FieldFill.INSERT_UPDATE)
private LocalDateTime updateDate;

// 逻辑删除
@TableLogic
@TableField("is_deleted")
private Integer isDeleted;
```

### 1.2 数据表清单

| 表名 | 说明 | 详细定义 |
|-----|------|----------|
| live_session | 场次表 | [live_session.md](../db/live_session.md) |
| live_video | 视频表 | [live_video.md](../db/live_video.md) |
| video_product | 视频商品表 | [video_product.md](../db/video_product.md) |
| schedule_session | 排班与场次表 | [schedule_session.md](../db/schedule_session.md) |
| schedule_performance | 排班业绩表 | [schedule_performance.md](../db/schedule_performance.md) |
| schedule_performance_detail | 排班业绩贡献明细表 | [schedule_performance_detail.md](../db/schedule_performance_detail.md) |
| schedule_performance_staff | 排班业绩人员表 | [schedule_performance_staff.md](../db/schedule_performance_staff.md) |
| session_performance | 场次业绩表 | [session_performance.md](../db/session_performance.md) |
| session_original_value | 原始值表 | [session_original_value.md](../db/session_original_value.md) |
| daily_stats | 每日统计表 | [daily_stats.md](../db/daily_stats.md) |
| product | 商品表 | [product.md](../db/product.md) |
| session_product | 场次商品关联表 | [session_product.md](../db/session_product.md) |

### 1.3 表关系图

```
live_video (N) ──────┐
      │              │
      ▼              ▼
video_product   live_session (1)
      (N)              │
                 ┌─────┼───────────┐
                 ▼           ▼           ▼
          schedule_session  session_product
                 │                       │
                 ▼                       ▼
          staff_performance           product
                 │
                 ▼
            daily_stats
```

## 2. 业务逻辑规范

### 2.1 视频处理流程

```
推送接口 → 写入 live_video (status=0) → 返回成功
                    ↓
定时任务扫描 (status=0) → 处理中 (status=1)
                    ↓
    查找/创建 live_session (乐观锁更新)
                    ↓
    匹配排班，计算分摊 → 写入 schedule_session
                    ↓
    处理商品数据 → 写入 session_product
                    ↓
    提交事务 → 异步触发后置任务
                    ↓
    更新 status=2 (成功) 或 status=3 (失败)
```

### 2.2 分摊算法

```java
/**
 * 计算排班分摊指标
 * 
 * @param videoStart   视频开始时间
 * @param videoEnd     视频结束时间
 * @param scheduleStart 排班开始时间
 * @param scheduleEnd   排班结束时间
 * @param deltaValue   差值指标（场观/销售额/退款等）
 * @return 分摊后的指标值
 */
public BigDecimal calculateShare(LocalDateTime videoStart, LocalDateTime videoEnd,
                                  LocalDateTime scheduleStart, LocalDateTime scheduleEnd,
                                  BigDecimal deltaValue) {
    // 计算重叠时长
    LocalDateTime overlapStart = videoStart.isAfter(scheduleStart) ? videoStart : scheduleStart;
    LocalDateTime overlapEnd = videoEnd.isBefore(scheduleEnd) ? videoEnd : scheduleEnd;
    
    long overlapSeconds = Duration.between(overlapStart, overlapEnd).getSeconds();
    if (overlapSeconds <= 0) {
        return BigDecimal.ZERO;
    }
    
    // 计算视频时长
    long videoSeconds = Duration.between(videoStart, videoEnd).getSeconds();
    
    // 分摊比例
    BigDecimal ratio = BigDecimal.valueOf(overlapSeconds)
            .divide(BigDecimal.valueOf(videoSeconds), 6, RoundingMode.HALF_UP);
    
    // 分摊值
    return deltaValue.multiply(ratio).setScale(2, RoundingMode.HALF_UP);
}
```

### 2.3 乐观锁更新

场次更新必须使用乐观锁，最多重试3次：

```java
@Transactional(rollbackFor = Exception.class)
public void updateSessionWithOptimisticLock(Long sessionId, SessionUpdateDTO dto) {
    int maxRetries = 3;
    for (int i = 0; i < maxRetries; i++) {
        LiveSession session = liveSessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "场次不存在");
        }
        
        // 累加指标
        session.setViewCount(session.getViewCount() + dto.getDeltaView());
        session.setEndSales(session.getEndSales().add(dto.getDeltaSales()));
        session.setRefund(session.getRefund().add(dto.getDeltaRefund()));
        // ... 其他指标
        
        // 乐观锁更新
        int updated = liveSessionMapper.update(session, 
            new LambdaUpdateWrapper<LiveSession>()
                .eq(LiveSession::getId, sessionId)
                .eq(LiveSession::getVersion, session.getVersion())
                .set(LiveSession::getVersion, session.getVersion() + 1));
        
        if (updated > 0) {
            return; // 更新成功
        }
        log.warn("[业绩] 乐观锁冲突，重试 {}/{}, sessionId={}", i + 1, maxRetries, sessionId);
    }
    throw new BusinessException(ErrorCode.FAIL, "更新场次失败，请重试");
}
```

### 2.4 差值计算

```java
/**
 * 计算视频差值指标
 */
public VideoMetricsDelta calculateDelta(LiveVideo video) {
    return VideoMetricsDelta.builder()
        .deltaView(video.getEndCumulativeView() - video.getStartCumulativeView())
        .deltaSales(video.getEndCumulativeSales().subtract(video.getStartCumulativeSales()))
        .deltaRefund(video.getEndCumulativeRefund().subtract(video.getStartCumulativeRefund()))
        .build();
}
```

## 3. 多租户隔离

### 3.1 租户上下文

所有操作必须携带租户ID：

```java
// Controller 层获取租户ID
@PostMapping("/push")
public ApiResponse<Void> pushVideo(@RequestBody VideoPushRequest request, AccessUser accessUser) {
    Long tenantId = accessUser.currentTenantId();
    return videoService.pushVideo(request, tenantId);
}

// 定时任务使用视频记录的租户ID
@Scheduled(fixedDelay = 5000)
public void processVideos() {
    List<LiveVideo> videos = videoMapper.selectPendingVideos();
    for (LiveVideo video : videos) {
        // 使用视频记录的租户ID作为上下文
        processVideoWithTenant(video, video.getTenantId());
    }
}
```

### 3.2 查询必须携带租户条件

```java
// 正确示例
lambdaQuery()
    .eq(LiveSession::getTenantId, tenantId)
    .eq(LiveSession::getId, sessionId)
    .one();

// 错误示例（缺少租户条件）
lambdaQuery()
    .eq(LiveSession::getId, sessionId)
    .one();
```

## 4. 数据来源区分

### 4.1 来源枚举

```java
@Getter
@AllArgsConstructor
public enum DataSource {
    SYSTEM(1, "系统"),
    MANUAL(2, "手动");

    private final int code;
    private final String desc;
}
```

### 4.2 人员业绩计算规则

- **仅统计自动分摊记录**：`schedule_session.source = 1`
- 手动录入的排班关联不参与人员业绩计算
- 手动录入数据参与每日统计

```java
// 人员业绩计算：仅统计自动分摊
List<ScheduleSession> autoRecords = scheduleSessionMapper.selectList(
    new LambdaQueryWrapper<ScheduleSession>()
        .eq(ScheduleSession::getTenantId, tenantId)
        .eq(ScheduleSession::getScheduleId, scheduleId)
        .eq(ScheduleSession::getSource, DataSource.SYSTEM)
        .eq(ScheduleSession::getIsDeleted, 0)
);
```

## 5. 异步任务处理

### 5.1 后置任务触发

事务提交后异步触发后置任务：

```java
@Transactional(rollbackFor = Exception.class)
public void processVideo(LiveVideo video) {
    // ... 核心业务逻辑
    
    // 记录受影响的排班ID
    Set<Long> affectedScheduleIds = new HashSet<>();
    // ... 处理逻辑中收集
    
    // 事务提交后触发异步任务
    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
        @Override
        public void afterCommit() {
            asyncTaskService.recalculateStaffPerformance(affectedScheduleIds);
            asyncTaskService.updateDailyStats(video.getTenantId(), statsDate);
        }
    });
}
```

### 5.2 每日统计更新

使用 `INSERT ... ON DUPLICATE KEY UPDATE` 模式：

```sql
INSERT INTO daily_stats (
    id, tenant_id, stats_date, dimension, company_id, dept_id, team_id, room_id,
    view_count, end_sales, refund, investment, net_sales, roi, session_count, duration
) VALUES (...)
ON DUPLICATE KEY UPDATE
    view_count = VALUES(view_count),
    end_sales = VALUES(end_sales),
    refund = VALUES(refund),
    investment = VALUES(investment),
    net_sales = VALUES(net_sales),
    roi = VALUES(roi),
    session_count = VALUES(session_count),
    duration = VALUES(duration),
    update_date = NOW();
```

## 6. 推送接口规范

### 6.1 接口设计

```java
/**
 * 爱复盘视频推送接口
 * 接收后直接入库，返回成功，由定时任务处理
 */
@PostMapping("/push")
public ApiResponse<Void> pushVideo(@Valid @RequestBody VideoPushRequest request) {
    // 参数校验
    // 写入 live_video 表，status=0
    // 立即返回成功
    return ApiResponse.success();
}
```

### 6.2 推送请求对象

```java
@Getter
@Setter
public class VideoPushRequest {
    
    @NotNull(message = "场次ID不能为空")
    private Long sessionId;
    
    @NotBlank(message = "视频ID不能为空")
    private String videoId;
    
    @NotNull(message = "直播间ID不能为空")
    private Long batchNumber;
    
    private String videoOssUrl;
    
    @NotNull(message = "开始时间不能为空")
    private LocalDateTime startTime;
    
    @NotNull(message = "结束时间不能为空")
    private LocalDateTime endTime;
    
    private Integer startCumulativeView;
    private Integer endCumulativeView;
    private BigDecimal startCumulativeSales;
    private BigDecimal endCumulativeSales;
    private BigDecimal startCumulativeRefund;
    private BigDecimal endCumulativeRefund;
    private BigDecimal investment;
    private String anchorSecUid;
    
    /** 商品列表 */
    private List<ProductDTO> products;
}
```

## 7. 性能要求

| 指标 | 要求 |
|-----|------|
| 推送接口响应时间 | < 200ms |
| 定时任务处理能力 | ≥ 1000条视频/分钟 |
| 统计查询响应时间 | < 500ms |

## 8. 枚举定义

### 8.1 处理状态

```java
@Getter
@AllArgsConstructor
public enum ProcessStatus {
    PENDING(0, "待处理"),
    PROCESSING(1, "处理中"),
    SUCCESS(2, "处理成功"),
    FAILED(3, "处理失败");
    
    private final int code;
    private final String desc;
}
```

```java
@Getter
@AllArgsConstructor
public enum StatsDimension {
    SESSION(1, "场次维度"),
    SCHEDULE(2, "排班维度");
    
    private final int code;
    private final String desc;
}
```

## 9. 接口/任务文档

| 接口/任务 | 说明       | 详细文档 |
|-----|----------|----------|
| POST /api/governance/performance/video/receive | 视频数据接收接口 | [receiveVideo.md](./performance/receiveVideo.md) |
| XxlJob: videoJobHandler | 视频处理定时任务 | [handleVideo.md](./performance/handleVideo.md) |
| POST /api/governance/performance/product/sessions | 商品关联直播场次分页查询 | [ProductController-pageQueryProductSessions.md](./performance/ProductController-pageQueryProductSessions.md) |
| POST /api/governance/performance/product/companies | 商品关联分公司列表查询 | [ProductController-queryProductCompanies.md](./performance/ProductController-queryProductCompanies.md) |
