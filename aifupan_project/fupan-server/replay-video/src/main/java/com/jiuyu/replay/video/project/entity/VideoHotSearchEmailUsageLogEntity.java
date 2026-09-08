package com.jiuyu.replay.video.project.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
@Accessors(chain = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_video_hot_search_email_usage_log")
@Schema(description = "热搜视频邮箱账号使用记录表")
public class VideoHotSearchEmailUsageLogEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.INPUT)
    @Schema(description = "主键ID（雪花ID）", example = "1")
    private Long id;

    @TableField("email_account_id")
    @Schema(description = "邮箱账号ID", example = "1")
    private Long emailAccountId;

    @TableField("client_ip")
    @Schema(description = "客户端IP", example = "192.168.1.100")
    private String clientIp;

    @TableField("client_city")
    @Schema(description = "客户端城市", example = "上海")
    private String clientCity;

    @TableField("use_time")
    @Schema(description = "使用时间", example = "2025-11-28 10:00:00")
    private LocalDateTime useTime;

    @TableField("release_time")
    @Schema(description = "释放时间", example = "2025-11-28 11:00:00")
    private LocalDateTime releaseTime;

    @TableField("created_date")
    @Schema(description = "创建时间", example = "2025-11-25 10:00:00")
    private LocalDateTime createdDate;

    @TableField("update_date")
    @Schema(description = "更新时间", example = "2025-11-25 10:30:00")
    private LocalDateTime updateDate;

    @TableField("is_deleted")
    @Schema(description = "是否删除：0-未删除 1-已删除", example = "0")
    private Byte isDeleted;
}

