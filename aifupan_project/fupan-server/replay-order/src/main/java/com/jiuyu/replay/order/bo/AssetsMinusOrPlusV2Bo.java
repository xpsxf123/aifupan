package com.jiuyu.replay.order.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author ：lujie
 * @description：
 * @date ：2026/2/27 16:25
 */
@Data
public class AssetsMinusOrPlusV2Bo extends AssetsMinusOrPlusBo {

    @Schema(description = "模型代码")
    private String modelCode;

    @Schema(description = "缓存命中的 token 数")
    private Long cachedTokens;

}
