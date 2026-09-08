package com.jiuyu.replay.ai.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/5/26 上午9:46
 */
@Data
@Schema(description = "根据问答ids获取对应的问答记录入参")
public class conversationByCueWordsIdsBo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "用户id")
    private Long userId;

    @Schema(description = "租户id")
    private Long tenantId;

    @Schema(description = "视频id")
    private String videoId;

    @Schema(description = "行业id")
    private Long tradeId;

    @Schema(description = "问答ids")
    private List<Long> cueWordsIds;

    @Schema(description = "诊断类型 1:内容诊断 2:数据诊断")
    private Integer diagnosisType;

}
