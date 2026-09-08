package com.jiuyu.replay.video.project.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * <p>
 * 搜爆款同步历史数据表
 * </p>
 *
 * @author RayChou
 * @since 2025-09-01
 */
@Getter
@Setter
@Accessors(chain = true)
@TableName("tb_video_hot_search_daily_data")
public class VideoHotSearchDailyDataEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID（雪花ID）
     */
    @TableId("id")
    private Long id;

    /**
     * 关联搜索表ID
     */
    @TableField("search_id")
    private Long searchId;

    /**
     * 数据日期
     */
    @TableField("data_date")
    private LocalDate dataDate;

    /**
     * 视频数
     */
    @TableField("video_count")
    private Integer videoCount;

    /**
     * 视频增量
     */
    @TableField("video_increment")
    private Integer videoIncrement;

    /**
     * 采集时间
     */
    @TableField("collection_time")
    private LocalDateTime collectionTime;

    /**
     * 创建时间
     */
    @TableField("created_date")
    private LocalDateTime createdDate;

    /**
     * 更新时间
     */
    @TableField("update_date")
    private LocalDateTime updateDate;

    /**
     * 是否删除: 0-未删除, 1-已删除
     */
    @TableField("is_deleted")
    @TableLogic
    private Byte isDeleted;
}
