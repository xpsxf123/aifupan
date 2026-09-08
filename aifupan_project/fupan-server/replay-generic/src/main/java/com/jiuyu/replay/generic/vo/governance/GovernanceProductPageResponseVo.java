package com.jiuyu.replay.generic.vo.governance;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 企业后台（governance）商品分页响应 VO — 对应 VideoProductPageResponse（extends PageData）
 *
 * @author jy
 * @date 2026-08-06
 */
@Data
@Schema(description = "企业后台商品分页响应")
public class GovernanceProductPageResponseVo implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 商品列表 */
    @Schema(description = "商品列表")
    private List<GovernanceProductItemVo> list;

    /** 总记录数 */
    @Schema(description = "总记录数")
    private Long total;

    /** 总页数 */
    @Schema(description = "总页数")
    private Long pages;

    /** 当前页码 */
    @Schema(description = "当前页码")
    private Long pageNum;

    /** 每页大小 */
    @Schema(description = "每页大小")
    private Long pageSize;

    /** 是否已拉取商品数据 */
    @Schema(description = "是否已拉取商品数据")
    private Boolean pullStatus;
}
