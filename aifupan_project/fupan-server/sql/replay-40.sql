/*
  修复 MySQL 关键字列名问题

  tb_ai_placeholder: name → placeholder_name, description → remark, status → status_flag
  tb_scene_slice:    status → status_flag
*/

ALTER TABLE `tb_ai_placeholder` RENAME COLUMN `name` TO `placeholder_name`;
ALTER TABLE `tb_ai_placeholder` RENAME COLUMN `description` TO `remark`;
ALTER TABLE `tb_ai_placeholder` RENAME COLUMN `status` TO `status_flag`;
ALTER TABLE `tb_scene_slice` RENAME COLUMN `status` TO `status_flag`;
