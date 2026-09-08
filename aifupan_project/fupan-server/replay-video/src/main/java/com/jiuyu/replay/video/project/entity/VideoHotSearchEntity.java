package com.jiuyu.replay.video.project.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 爆款表
 * </p>
 *
 * @author RayChou
 * @since 2025-09-01
 */
@Getter
@Setter
@Accessors(chain = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_video_hot_search")
public class VideoHotSearchEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID（雪花ID）
     */
    @TableId("id")
    private Long id;

    /**
     * 平台类型: 1-抖音, 2-快手, 3-视频号
     */
    @TableField("platform_type")
    private Byte platformType;

    /**
     * 搜索关键词
     */
    @TableField("search_keyword")
    private String searchKeyword;

    /**
     * 视频总数
     */
    @TableField("video_count")
    private Integer videoCount;

    /**
     * 最后同步时间
     */
    @TableField("last_sync_time")
    private LocalDateTime lastSyncTime;

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
