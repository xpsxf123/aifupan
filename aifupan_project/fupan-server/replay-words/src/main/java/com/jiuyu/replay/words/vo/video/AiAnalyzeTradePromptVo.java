package com.jiuyu.replay.words.vo.video;

import com.jiuyu.replay.common.utils.TreePrinterWithMap;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * @author ：liwj
 * @description： 推荐行业的提示词
 * @date ：2025/9/17 14:13
 */
@Schema(description = "推荐行业的提示词出参")
@Data
public class AiAnalyzeTradePromptVo implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "提示词")
    private String cueWord;

    @Schema(description = "ai的身份")
    private String aiIdentity;

    @Schema(description = "使用的ai模型")
    private Integer aiModel;

    @Schema(description = "默认的行业id")
    private Long defTradeId;

    @Schema(description = "默认的行业名称")
    private String defTradeName;

    @Schema(description = "序号对应的行业id")
    private Map<String, TreePrinterWithMap.TradeTemp> tradeList;
}
