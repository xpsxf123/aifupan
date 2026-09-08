package com.jiuyu.replay.video.project.bo.hotsearch;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

import java.io.Serial;
import java.io.Serializable;

/**
 * 爆款视频列表查询业务对象
 *
 * @author RayChou
 * @date 2025-08-14
 * @description 爆款视频列表查询参数
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "爆款视频列表查询业务对象")
public class VideoHotListQueryBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 爆款搜索ID
     */
    @Schema(description = "爆款搜索ID", example = "1001")
    private Long hotSearchId;

    /**
     * 页码
     */
    @Schema(description = "页码", example = "1")
    @Min(value = 1, message = "页码最小为1")
    private Integer page = 1;

    /**
     * 每页条数
     */
    @Schema(description = "每页条数", example = "20")
    @Min(value = 1, message = "每页条数最小为1")
    @Max(value = 100, message = "每页最大条数不能超过100条")
    private Integer limit = 20;

    /**
     * 关键词
     */
    @Schema(description = "关键词", example = "科三")
    @Length(max = 100, message = "关键词长度不能超过100个字符")
    private String keyword;

    /**
     * 点赞大于
     */
    @Schema(description = "点赞大于", example = "1000")
    @Min(value = 0, message = "点赞数不能小于0")
    private Long likeCountMin;

    /**
     * 转发大于
     */
    @Schema(description = "转发大于", example = "100")
    @Min(value = 0, message = "转发数不能小于0")
    private Long shareCountMin;

    /**
     * 收藏大于
     */
    @Schema(description = "收藏大于", example = "50")
    @Min(value = 0, message = "收藏数不能小于0")
    private Long collectCountMin;

    /**
     * 视频时长
     */
    @Schema(description = "视频时长值", example = "50")
    @Min(value = 0, message = "视频时长开始值不能小于0")
    private Integer durationStart;

    @Schema(description = "视频时长结束值", example = "50")
    @Min(value = 0, message = "视频时长结束值不能小于0")
    private Integer durationEnd;

    /**
     * 发布时间
     */
    @Schema(description = "发布时间", example = "2025-08-14")
    @Length(max = 20, message = "发布时间长度不能超过20个字符")
    private String publishTime;
}
