package com.jiuyu.replay.words.vo;

import com.jiuyu.replay.generic.vo.words.TradeListVo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "行业模型")
public class TradeModelVo {

    /**
     * 行业信息
     */
    @Schema(description = "行业信息")
    private TradeListVo tradeVo;
    /**
     * 行业的模型列表
     */
    @Schema(description = "行业的模型列表")
    private List<TradeModelItemVo> tradeModelList;
}
