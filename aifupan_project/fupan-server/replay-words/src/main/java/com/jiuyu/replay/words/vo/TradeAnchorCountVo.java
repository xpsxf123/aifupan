package com.jiuyu.replay.words.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 行业相似达人数量统计VO
 *
 * @author RayChou
 * @date 2025-10-30
 * @description 用于返回各行业30天内更新的相似达人数量统计
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "行业相似达人数量统计VO")
public class TradeAnchorCountVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 行业ID
     */
    @Schema(description = "行业ID", example = "1001")
    private Long tradeId;

    /**
     * 相似达人数量（30天内更新）
     */
    @Schema(description = "相似达人数量（30天内更新）", example = "150")
    private Long count;
}

