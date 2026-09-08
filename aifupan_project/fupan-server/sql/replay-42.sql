ALTER TABLE `tb_basic_settings`
    MODIFY COLUMN `roi_accuracy` varchar(50) DEFAULT NULL COMMENT 'ROI观测精度，使用字典roi_accuracy的值';
