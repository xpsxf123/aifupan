/*
 2.6.01 话术智能监控 — tb_mq_message_record.message_topic 扩字段长度

 背景：B4 修补包-3 引入独立 topic `script_monitor_qa_topic` (23 字符) / `script_monitor_dev_topic` (24 字符)
       超出现网 tb_mq_message_record.message_topic 字段当前长度（VARCHAR 较短），
       导致 INSERT 时 Data truncation: Data too long for column 'message_topic'。
       现网历史 topic（如 activity_qa_topic 17 字符）字段够用，新 script_monitor_* 不够。

 修复：扩 message_topic 字段长度到 64 字符，给未来新 topic 留余量。

 影响：仅扩长，不破坏现有数据。在 test 环境已手动跑通验证。

 Target Server Version : MySQL 8.0.39
*/

SET NAMES utf8mb4;

ALTER TABLE `tb_mq_message_record`
    MODIFY COLUMN `message_topic` VARCHAR(64) NOT NULL COMMENT '消息主题';
