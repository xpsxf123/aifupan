package com.jiuyu.replay.words.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "行业模型项")
public class TradeModelItemVo extends DataModelVo{

    /**
     * 是否是默认展示模型 0：否 1：是
     */
    @Schema(description = "是否是默认展示模型 0：否 1：是")
    private Integer defaultShow;
}
