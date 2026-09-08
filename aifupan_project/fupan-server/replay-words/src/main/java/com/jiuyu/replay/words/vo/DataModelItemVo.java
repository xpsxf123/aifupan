package com.jiuyu.replay.words.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "模型的具体项")
public class DataModelItemVo {

    /**
     * 关键词分类id
     */
    @Schema(description = "关键词分类id")
    private Long cruxTypeId;
    /**
     * 关键词分类名称
     */
    @Schema(description = "关键词分类名称")
    private String cruxTypeName;
    /**
     * 占比，如0.2就是20%
     */
    @Schema(description = "占比，如0.2就是20%")
    private Double cruxTypeScale;
}
