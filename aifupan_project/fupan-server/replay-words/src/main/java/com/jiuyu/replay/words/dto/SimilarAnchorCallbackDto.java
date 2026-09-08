package com.jiuyu.replay.words.dto;

import com.jiuyu.replay.words.vo.SimilarAnchorVo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 相似主播回调DTO
 *
 * @author RayChou
 * @date 2025-10-27
 * @description 相似主播异步回调数据传输对象
 */
@Data
@Schema(description = "相似主播回调DTO")
public class SimilarAnchorCallbackDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 当前主播抖音号 (anchor_number)
     */
    @Schema(description = "当前主播抖音号")
    private String currentAnchorId;

    /**
     * 请求ID（用于请求和回调的唯一标识）
     */
    @Schema(description = "请求ID")
    private String requestId;

    /**
     * 相似主播列表
     */
    @Schema(description = "相似主播列表")
    private List<SimilarAnchorVo> similarAnchorList;
}

