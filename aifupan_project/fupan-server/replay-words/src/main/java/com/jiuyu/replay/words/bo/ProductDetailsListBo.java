package com.jiuyu.replay.words.bo;

import com.jiuyu.replay.common.bo.PageBo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 商品列表查询参数
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2026-03-19
 */
@Data
@Schema(description = "商品列表查询参数")
public class ProductDetailsListBo extends PageBo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 模糊搜索查询条件
     */
    @Schema(description = "模糊搜索查询条件")
    private String keyword;

    /**
     * 归属批次号
     */
    @Schema(description = "归属批次号")
    private String batchNumber;

    /**
     * 视频唯一标识
     */
    @Schema(description = "视频唯一标识")
    private String videoId;

    /**
     * 商品唯一标识
     */
    @Schema(description = "商品唯一标识")
    private String productId;
}
