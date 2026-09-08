package com.jiuyu.replay.order.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * @ClassName : lujie
 * @Description : 资产减少或增加的参数
 */
@Data
@Schema(description = "资产减少或增加的参数")
public class AssetsMinusOrPlusBo implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "用户id")
    private Long userId;

    @Schema(description = "用户名称-不用传")
    private String userName;

    @Schema(description = "加减的类型 1：（监控位数量）monitorNum 2：（语音分析时长）aiAnalysisTime  3：（添加主播数量）anchorNum")
    private Integer type;

    @Schema(description = "资产code (aiAnalysisTime :ai语音分析时长;  videoTaggingTime :视频标注时长; " +
            "textTaggingWordCount :文本标注字数;  monitorNum :监控位;  anchorNum :可添加主播数;  storageNum :空间容量; " +
            "subAccountCount :拥有子账号数量;  child_monitorNum :子账号监控位;  child_anchorNum :子账号可添加主播数)")
    private String code;

    @Schema(description = "减少或增加的数量, 整数就是加的负数就是减")
    private Long num;

    @Schema(description = "redis缓存id")
    private Long redisId;

    @Schema(description = "预扣id")
    private String withholdId;

    @Schema(description = "扣款类型：0正常扣款，1多退少扣完")
    private Integer deductionType;

    @Schema(description = "aiToken记录id")
    private List<Long> aiTokenIds;

    @Schema(description = "备注")
    private String remarks;

    /**
     * 清空redis 预扣的缓存
     */
    private BiConsumer<String, Long> clearWithholdCache;
}
