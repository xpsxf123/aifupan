package com.jiuyu.replay.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * 场景切片记录表
 *
 * @author lj
 * @date 2026-07-06
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_scene_slice")
@Schema(description = "场景切片记录表")
public class SceneSliceEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.INPUT)
    @Schema(description = "ID")
    private Long id;

    @Schema(description = "视频ID")
    private String videoId;

    @Schema(description = "OSS文件key")
    private String ossKey;

    @Schema(description = "截取秒数(距视频结束)")
    private Integer sliceSeconds;

    @TableField("status_flag")
    @Schema(description = "状态: 0-待开始 1-处理中 2-处理完成 3-处理失败")
    private Integer status;

    @Schema(description = "AI分析结果")
    private String aiResult;

    @Schema(description = "失败原因")
    private String failReason;

    @Schema(description = "AI分析开始时间")
    private Date analysisStartTime;

    @Schema(description = "AI分析完成时间")
    private Date analysisTime;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "租户ID")
    private Long tenantId;

    @Schema(description = "创建时间")
    private Date createDate;

    @Schema(description = "更新时间")
    private Date updateDate;

    @Schema(description = "是否已删除 0-否 1-是")
    private Integer isDeleted;
}
