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
import java.time.LocalDateTime;

/**
 * <p>
 * 爆款搜索视频关联表
 * </p>
 *
 * @author RayChou
 * @since 2025-09-02
 */
@Getter
@Setter
@Accessors(chain = true)
@TableName("tb_video_hot_search_video")
public class VideoHotSearchVideoEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID（雪花ID）
     */
    @TableId("id")
    private Long id;

    /**
     * 搜索ID
     */
    @TableField("search_id")
    private Long searchId;

    /**
     * 视频ID
     */
    @TableField("video_id")
    private Long videoId;

    /**
     * 达人平台类型: 1-抖音, 2-快手, 3-视频号
     */
    @TableField("influencer_platform_type")
    private Byte influencerPlatformType;

    /**
     * 达人平台用户ID
     */
    @TableField("influencer_platform_user_id")
    private String influencerPlatformUserId;

    /**
     * 达人昵称
     */
    @TableField("influencer_nickname")
    private String influencerNickname;

    /**
     * 达人头像URL
     */
    @TableField("influencer_avatar")
    private String influencerAvatar;

    /**
     * 达人粉丝数
     */
    @TableField("influencer_followers_count")
    private Long influencerFollowersCount;

    /**
     * 在搜索结果中的排序
     */
    @TableField("sort_order")
    private Integer sortOrder;

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
