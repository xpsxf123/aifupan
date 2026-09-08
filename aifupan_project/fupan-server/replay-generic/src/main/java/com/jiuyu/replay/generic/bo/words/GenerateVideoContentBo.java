package com.jiuyu.replay.generic.bo.words;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/5/26 下午8:34
 */
@Schema(description = "生成视频内容参数")
@Data
public class GenerateVideoContentBo {

    @Schema(description = "来源id")
    private String sourceId;

    @Schema(description = "内容类型 1自然原文，2优化原文")
    private Integer type;

    @Schema(description = "来源类型0视频、1文件、2对比分析")
    private Integer sourceType;

    @Schema(description = "用户id")
    private Long userId;

    @Schema(description = "租户id")
    private Long tenantId;


}
