package com.jiuyu.replay.words.vo.video;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author ：lujie
 * &#064;description：重要弹幕状态的出参
 * @date ：2025/9/23 15:59
 */
@Schema(description = "重要弹幕状态的出参")
@Data
public class ImportantBarrageStatusVo {

    @Schema(description = "重要弹幕生成状态：0未获取，1获取中，2获取成功，3获取失败")
    private Integer importantBarrageStatus;

    @Schema(description = "错误原因")
    private String errorReason;
}
