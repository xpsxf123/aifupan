package com.jiuyu.replay.common.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "修改文件")
public class UpdateFileBo {

    /**
     * ID
     */
    @Schema(description = "ID")
    private Long id;
    /**
     * 来源id
     */
    @Schema(description = "来源id")
    private Long resourceId;
    /**
     * 来源类型
     */
    @Schema(description = "来源类型")
    private Integer resourceType;
    /**
     * 描述
     */
    @Schema(description = "描述")
    private String remarks;
}
