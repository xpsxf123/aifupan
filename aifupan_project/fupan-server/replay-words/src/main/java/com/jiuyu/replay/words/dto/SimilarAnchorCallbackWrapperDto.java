package com.jiuyu.replay.words.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 相似主播回调包装DTO
 *
 * @author RayChou
 * @date 2025-10-28
 * @description 第三方平台回调的外层响应结构
 */
@Data
@Schema(description = "相似主播回调包装DTO")
public class SimilarAnchorCallbackWrapperDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 响应码
     */
    @Schema(description = "响应码", example = "0")
    private Integer code;

    /**
     * 响应消息
     */
    @Schema(description = "响应消息", example = "成功")
    private String msg;

    /**
     * 响应数据（相似主播回调数据）
     */
    @Schema(description = "响应数据")
    private SimilarAnchorCallbackDto data;
}

