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
 * 达人每日数据表
 * </p>
 *
 * @author RayChou
 * @since 2025-08-21
 */
@Getter
@Setter
@Accessors(chain = true)
@TableName("tb_video_influencer_daily_data")
public class VideoInfluencerDailyDataEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID（雪花ID）
     */
    @TableId("id")
    private Long id;

    /**
     * 达人ID(关联tb_video_influencer_info.id)
     */
    @TableField("influencer_id")
    private Long influencerId;

    /**
     * 数据日期
     */
    @TableField("data_date")
    private LocalDate dataDate;

    /**
     * 粉丝数
     */
    @TableField("followers_count")
    private Long followersCount;

    /**
     * 关注数
     */
    @TableField("following_count")
    private Long followingCount;

    /**
     * 作品数
     */
    @TableField("video_count")
    private Integer videoCount;

    /**
     * 获赞数
     */
    @TableField("like_count")
    private Long likeCount;

    /**
     * 粉丝增量
     */
    @TableField("followers_increment")
    private Long followersIncrement;

    /**
     * 关注增量
     */
    @TableField("following_increment")
    private Long followingIncrement;

    /**
     * 作品增量
     */
    @TableField("video_increment")
    private Integer videoIncrement;

    /**
     * 获赞增量
     */
    @TableField("like_increment")
    private Long likeIncrement;

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
