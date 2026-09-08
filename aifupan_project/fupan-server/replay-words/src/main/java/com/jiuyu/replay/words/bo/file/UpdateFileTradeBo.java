package com.jiuyu.replay.words.bo.file;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "修改文件的行业参数")
public class UpdateFileTradeBo {

    /**
     * 文件id
     */
    @Schema(description = "文件id")
    private String fileId;
    /**
     * 行业id
     */
    @Schema(description = "行业id")
    private Long tradeId;
    /**
     * 用户id
     */
    @Schema(description = "用户id")
    private Long userId;
    /**
     * 租户id
     */
    @Schema(description = "租户id")
    private Long tenantId;
}
