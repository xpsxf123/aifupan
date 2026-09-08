-- 删除 product_show_view_ratio 列，修改 product_view_show_ratio 语义为曝光观看率
ALTER TABLE video_product
    DROP COLUMN product_show_view_ratio,
    MODIFY COLUMN product_view_show_ratio DECIMAL(10, 4) NULL COMMENT '曝光观看率 — 商品曝光人数/直播间观看人数';
