-- video_product 表新增排序字段
ALTER TABLE `video_product`
ADD COLUMN `sort` INT NOT NULL DEFAULT 1 COMMENT '排序字段，从1开始';
