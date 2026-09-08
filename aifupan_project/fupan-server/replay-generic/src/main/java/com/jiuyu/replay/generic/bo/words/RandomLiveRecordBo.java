package com.jiuyu.replay.generic.bo.words;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 随机获取直播录制记录请求Bo
 *
 * @author System
 * @date 2026-04-09
 */
@Data
@Schema(description = "随机获取直播录制记录请求")
public class RandomLiveRecordBo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 行业ID
     */
    @NotNull(message = "行业ID不能为空")
    @Schema(description = "行业ID", required = true)
    private Long tradeId;
}
