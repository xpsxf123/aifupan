package com.jiuyu.replay.ai.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/5/27 下午4:07
 */
@Data
@Schema(description = "分享链接记录列表查询参数")
public class UpdateLikesStatusBo {

    @Schema(description = "助手类型", required = true)
    private Integer type;

    @Schema(description = "来源id", required = true)
    private String sourceId;

    @Schema(description = "来源类型", required = true)
    private Integer sourceType;

    @Schema(description = "数据类型 0视频，1文件，2对比分析", required = true)
    private String code;

    @Schema(description = "数据id", required = true)
    private String id;

    @Schema(description = "点赞状态 -1未点赞 0点赞，1踩", required = true)
    private Integer giveStatuc;

}
