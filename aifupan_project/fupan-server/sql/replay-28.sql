/*
 2.6.01 话术智能监控 B5 — 监控位资产类型种子数据

 新增 3 条 tb_commodity_type 记录，对应三类 AI 监控能力的监控位资产：
   - scriptQualityNum：话术质检监控位
   - scriptFidelityNum：话术还原度监控位
   - interactionPatrolNum：互动巡检监控位

 sub_account_have=0：子账号不独立拥有此类资产，查询时自动回退到主账号。

 ID 说明：4449519005513814020/021/022 按项目 SnowflakeManager 算法预生成的真实形态 ID
 （twepoch=687888001020, workerId=1, sequence=4/5/6, timestamp=2026-06-01 UTC）
 ⚠️ 部署前 DBA 必须确认：该 ID 范围在生产环境 tb_commodity_type 中不冲突

 -- 幂等执行：使用 INSERT IGNORE，重复执行不报错；DBA 部署前仍需 cross-check id 范围

 Target Server Version : MySQL 8.0.39
 File Encoding         : 65001
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- 监控位资产类型种子数据
-- ----------------------------
INSERT IGNORE INTO `tb_commodity_type` (`id`, `name`, `code`, `sub_account_have`, `is_deleted`, `create_date`, `update_date`)
VALUES
    (4449519005513814020, '话术质检监控位', 'scriptQualityNum', 0, 0, NOW(), NOW()),
    (4449519005513814021, '话术还原度监控位', 'scriptFidelityNum', 0, 0, NOW(), NOW()),
    (4449519005513814022, '互动巡检监控位', 'interactionPatrolNum', 0, 0, NOW(), NOW());

SET FOREIGN_KEY_CHECKS = 1;
