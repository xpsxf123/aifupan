package com.jiuyu.replay.order.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-09 16:13:50
 */
@Data
@Schema(description = "检查用户是否可以使用资产参数")
public class IsPropertyHaveBo {

    @Schema(description = "用户id")
    private Long userId;

    @Schema(description = "商品code (aiAnalysisTime :ai语音分析时长;  videoTaggingTime :视频标注时长; " +
            "textTaggingWordCount :文本标注字数;  monitorNum :监控位;  anchorNum :可添加主播数;  storageNum :空间容量; " +
            "subAccountCount :拥有子账号数量;  child_monitorNum :子账号监控位;  child_anchorNum :子账号可添加主播数")
    private String code;

    @Schema(description = "使用数量")
    private Long thisUseNum;

}
