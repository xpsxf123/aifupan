---
name: "business-schedule"
description: "排班调度与业绩模块核心业务规则：排班防错引擎、时间重叠检测、业绩分摊算法。开发 schedule 相关功能时调用。"
---

# Business Domain: Schedule & Performance (排班调度与业绩模块)

This skill defines the most complex and critical business logic in the system: the Scheduling Engine and the Performance Split Algorithm.

## 1. 排班数据结构与拆分 (Schedule Data Structure)

排班数据由两张核心表组成，实现了**班次**与**人员**的解耦：
- `work_schedule` (班次表): 记录基础时间框架。核心字段：`live_room_id` (直播间), `work_day` (归属日期, Date), `start_work` (开始时间, Integer 如 1430), `end_work` (结束时间, Integer), `schedule_duration` (班次时长), `rest_duration` (休息时长)。
- `schedule_employee` (人员绑定表): 记录具体的排班人员。核心字段：`schedule_id` (关联班次), `employee_id` (员工), `position_id` (岗位), `work_day` (冗余日期，**用于极大优化查询性能**)。

### 1.1 跨天时间计算 (Cross-day Computation)

由于 `start_work` 和 `end_work` 存储为整数（如 2300 和 0200），必须使用 `WorkSchedule.workRange()` 将其转换为绝对时间戳（`LocalDateTime`）。如果 `end_work <= start_work`，系统会自动将结束时间推算至 `plusDays(1)`。

## 2. 排班调度防错引擎 (Scheduling Anti-Error Engine)

排班是直播行业的高频操作。排班接口必须挂载极为严格的防错检测（在落库前执行）。核心实现位于 `LiveRoomScheduleConflictHandler`。

### 2.1 强阻断规则 (Hard Blockers)

1. **直播间时间重叠**：同一个直播间，在相同的**绝对时间段**内，绝对不允许存在两条有效排班记录。（注意跨天问题，需使用内存计算绝对时间交集）。
2. **单人时间重叠**：同一个员工，在相同的绝对时间段内，绝对不允许存在两条有效排班记录。（不同的员工可以在同一个直播间的同一时间排班，如双主播）。
3. **未开始校验**：对排班进行修改时间、删除、添加人员、移除人员等微调操作时，必须前置校验该排班是否**未开始**。如果当前时间已超过排班的 `start_work`，则强制拦截（"无法操作已开始的排班"）。

### 2.2 批量排班生成器 (Batch Generator)

实现位于 `LiveRoomScheduleBatchGenerator`。
- 支持单天排班、多场次、多人同时绑定。
- 根据 `batchConfig` (包含 `cycleDays` 星期几和 `endDate` 截止日期)，利用日期推演自动将首日的排班模板扩展到结束日期前的所有指定星期。
- 必须同时向 `ScheduleEmployee` 实体写入推演出的 `workDay` 字段。

## 3. 业绩核算与分摊算法 (Performance Split Algorithm)

这是本系统的最高业务价值点：将粗颗粒度的"整场直播数据"精准分摊给细颗粒度的"不同排班人员"。

### 3.1 核心算法：时间重叠比例切割法

当一场直播结束（平台推送了 `end_sales`、`view_count` 等数据），系统按以下步骤执行：

1. **获取场次时间**：假设场次实际开播 `10:00 - 14:00` (总时长4h)，净销售额 `40000`。
2. **寻找交集排班**：查出在此绝对时间段内，该直播间所有相关的有效排班记录。
   - 主播A排班：`10:00 - 12:00` (重叠2h)
   - 主播B排班：`12:00 - 14:00` (重叠2h)
   - 场控C排班：`10:00 - 14:00` (重叠4h)
3. **计算个人重叠比例**：个人重叠时长 / 场次总时长。
   - A = 2/4 = 50% ; B = 2/4 = 50% ; C = 4/4 = 100%
4. **平行分配指标**：各岗位之间平行计算，**不相互稀释**。
   - 主播A业绩 = `40000 * 50% = 20000`
   - 主播B业绩 = `40000 * 50% = 20000`
   - 场控C业绩 = `40000 * 100% = 40000`
5. **落库处理**：将结果写入 `schedule_session` (排班场次明细表)。

### 3.2 边缘与异常处理 (Edge Cases)

- **浮点数精度**：中间比例计算保留 4 位小数，最终落库金额采用 `ROUND_HALF_UP` 保留 2 位小数。允许总分摊额与总场次额有极微小的几分钱误差。
- **跨天切割**：若直播从 `23:00` 到次日 `02:00`，业绩必须严格按重叠比例，分别切分给 `23:00-24:00` 的排班（算作昨日业绩）和 `00:00-02:00` 的排班（算作今日业绩）。
- **无主业绩**：如果场次有数据，但找不到任何交集排班，标记为"无主数据"，触发系统告警要求人工补录排班。
- **历史联动**：如果管理员强行修改了**过去几天（历史）**的排班记录，系统必须**自动发送 MQ 或触发事件**，对这段时间内该直播间的场次业绩进行**重新核算与分摊**。
