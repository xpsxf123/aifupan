package com.jiuyu.replay.generic.vo.governance;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 企业后台（governance）商品项 VO — 用于反序列化 productPage 接口返回的 ProductItem
 *
 * @author jy
 * @date 2026-08-06
 */
@Data
@Schema(description = "企业后台商品项")
public class GovernanceProductItemVo implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 排序，从1开始 */
    @Schema(description = "排序")
    private Integer sort;

    /** 商品标题 */
    @Schema(description = "商品标题")
    private String title;

    /** 商品封面图 */
    @Schema(description = "商品封面图")
    private String imageUri;

    /** 曝光人数 */
    @Schema(description = "曝光人数")
    private Long productShowUcnt;

    /** 曝光观看率(%) → 映射为"商品曝光率"列 */
    @Schema(description = "曝光观看率")
    private BigDecimal productViewShowRatio;

    /** 点击人数 */
    @Schema(description = "点击人数")
    private Long productClickUcnt;

    /** 曝光点击率(%) → 映射为"商品点击率"列 */
    @Schema(description = "曝光点击率")
    private BigDecimal productShowClickUcntRatio;

    /** 点击成交转化率(%) */
    @Schema(description = "点击成交转化率")
    private BigDecimal productClickPayUcntRatio;

    /** 成交单价(元) */
    @Schema(description = "成交单价")
    private BigDecimal avgPayAmtPerOrder;

    /** 订单量 */
    @Schema(description = "订单量")
    private Long payCnt;

    /** 成交金额(元) */
    @Schema(description = "成交金额")
    private BigDecimal payAmt;

    /** 讲解次数 */
    @Schema(description = "讲解次数")
    private Integer explainCnt;
}
