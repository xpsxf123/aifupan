package com.jiuyu.replay.words.vo;

import com.jiuyu.replay.generic.vo.words.TradeVo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
@Data
@Schema(description = "行业信息")
public class AnchorTradeListVo extends TradeVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主播数量
     */
    @Schema(description = "主播数量")
    private Integer anchorNum;
}
