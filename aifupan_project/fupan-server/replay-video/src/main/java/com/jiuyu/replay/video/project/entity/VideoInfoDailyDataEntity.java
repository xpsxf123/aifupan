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
 * 视频每日数据表
 * </p>
 *
 * @author RayChou
 * @since 2025-08-21
 */
@Getter
@Setter
@Accessors(chain = true)
@TableName("tb_video_info_daily_data")
public class VideoInfoDailyDataEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID（雪花ID）
     */
    @TableId("id")
    private Long id;

    /**
     * 视频ID(关联tb_video_info.id)
     */
    @TableField("video_id")
    private Long videoId;

    /**
     * 数据日期
     */
    @TableField("data_date")
    private LocalDate dataDate;

    /**
     * 点赞数
     */
    @TableField("like_count")
    private Long likeCount;

    /**
     * 评论数
     */
    @TableField("comment_count")
    private Long commentCount;

    /**
     * 分享数
     */
    @TableField("share_count")
    private Long shareCount;

    /**
     * 收藏数
     */
    @TableField("collect_count")
    private Long collectCount;

    /**
     * 点赞增量
     */
    @TableField("like_increment")
    private Long likeIncrement;

    /**
     * 评论增量
     */
    @TableField("comment_increment")
    private Long commentIncrement;

    /**
     * 分享增量
     */
    @TableField("share_increment")
    private Long shareIncrement;

    /**
     * 收藏增量
     */
    @TableField("collect_increment")
    private Long collectIncrement;

    /**
     * 采集时间
     */
    @TableField("collection_time")
    private LocalDateTime collectionTime;

    /**
     * 数据来源: 1-订阅采集, 2-手动采集, 3-API同步
     */
    @TableField("data_source")
    private Byte dataSource;

    /**
     * 创建时间
     */
    @TableField("created_date")
    private LocalDateTime createdDate;

    /**
     * 更新时间
     */
    @TableField("updated_date")
    private LocalDateTime updatedDate;

    /**
     * 是否删除: 0-否, 1-是
     */
    @TableField("is_deleted")
    @TableLogic
    private Byte isDeleted;
}
