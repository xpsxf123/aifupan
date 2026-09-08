package com.jiuyu.replay.generic.bo.words;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;


@Data
@Schema(description = "关键词类型并增加占比百分比属性")
public class CruxTypeScaleBo {
    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @Schema(description = "ID")
    private Long cruxTypeId;

    /**
     * cruxTypeName
     */
    @Schema(description = "cruxTypeName")
    private String cruxTypeName;

    /**
     * 占比百分比
     */
    @Schema(description = "占比百分比")
    private double scale;

}
