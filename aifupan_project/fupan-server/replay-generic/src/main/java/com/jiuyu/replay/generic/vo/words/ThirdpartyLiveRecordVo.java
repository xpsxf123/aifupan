package com.jiuyu.replay.generic.vo.words;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 第三方直播录制记录Vo
 *
 * @author System
 * @date 2026-04-09
 */
@Data
@Schema(description = "第三方直播录制记录")
public class ThirdpartyLiveRecordVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 云空间链接
     */
    @Schema(description = "云空间链接")
    private String cloudUrl;

    /**
     * 场观
     */
    @Schema(description = "场观")
    private Long viewers;

    /**
     * 月销售额
     */
    @Schema(description = "月销售额")
    private String monthlySales;
}
