-- =====================================================================
-- replay-44: Data Hub 开放接口（/internal/data-hub）配套索引
-- 背景：三条新增查询模式此前均无匹配的二级索引（sql/ 目录 DDL 记录
--       不完整，执行前请先 SHOW INDEX 核对生产实际索引，已存在的跳过）：
--   1. order/reconciliation：tenant_id 过滤 + update_date 增量
--      + (update_date, id) 键集游标
--   2. user/login-stats：tb_user_login_log 按 user_id IN + opera_type=0
--      + opera_status=0 [+ create_date 窗口] GROUP BY user_id
--   3. tenant/analysis-stats：tb_anchor_video 按 tenant_id IN
--      + analysis_status=2 GROUP BY tenant_id, sec_uid
-- 影响：仅新增索引（isolated additive DDL），可直接回滚 DROP INDEX。
-- =====================================================================

ALTER TABLE tb_order ADD INDEX idx_update_date_id (update_date, id);
ALTER TABLE tb_order ADD INDEX idx_tenant_update (tenant_id, update_date);

ALTER TABLE tb_user_login_log ADD INDEX idx_user_opera (user_id, opera_type, opera_status, create_date);

ALTER TABLE tb_anchor_video ADD INDEX idx_tenant_analysis (tenant_id, analysis_status, sec_uid);

-- ============================================================
-- 客户端自动删除本地视频配置：tb_anchor_url_user 新增两个直播级字段。
--
-- 背景 / 为什么这么改：
--   1. 直播级「自动删除时间 / 删除内容」需持久化到 tb_anchor_url_user 并同步到客户端，
--      客户端据此在分析完成后自动删除本地录播视频（本期只加字段，删除逻辑后续实现）。
--   2. 两个字段均可空：空/未设置 = 跟随客户端全局配置（Config/systemConfig.txt）。
--      -1 统一表示「不删除」（两级口径一致）。
--   3. 字段命名 camelCase（autoDeleteTime / deleteContent），列名 snake_case，
--      与既有 recordDefinition / segmentTimePoints 等字段保持一致。
--
-- ⚠️ 数据字典（通过「字典管理」后台创建，ID 由 SnowflakeManager 生成，勿在此手工指定）：
--   tb_dict_type:
--     logo=auto_delete_time  name=自动删除时间
--     logo=delete_content    name=删除内容
--   tb_dict_data（typeId 指向上述类型）：
--     auto_delete_time: -1=不删除, 0=马上删除, 1=1天后删除, 3=3天后删除, 7=7天后删除, 30=30天后删除, 90=90天后删除
--     delete_content:   ts=删除源视频, mp4=删除mp4视频, all=全部删除
-- ============================================================

ALTER TABLE tb_anchor_url_user
  ADD COLUMN auto_delete_time VARCHAR(32) DEFAULT NULL
  COMMENT '自动删除时间 -1不删除/0马上删/N天后删，空=跟随全局配置（字典 auto_delete_time）';

ALTER TABLE tb_anchor_url_user
  ADD COLUMN delete_content VARCHAR(32) DEFAULT NULL
  COMMENT '删除内容 ts源视频/mp4成品/all都删，空=跟随全局配置（字典 delete_content）';
