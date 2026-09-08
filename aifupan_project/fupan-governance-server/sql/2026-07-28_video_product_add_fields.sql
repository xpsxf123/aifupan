-- video_product 新增商品分析字段
ALTER TABLE video_product
    ADD COLUMN product_view_show_ratio DECIMAL(10, 4) NULL COMMENT '曝光观看率 — 商品曝光人数/直播间观看人数',
    ADD COLUMN avg_pay_amt_per_order DECIMAL(10, 2) NULL COMMENT '成交单价（元）';
