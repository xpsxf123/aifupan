package com.jiuyu.replay.words.vo;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 用户分析汇总表信息
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-01-19 16:56:41
 */
@Data
@Schema(description = "用户分析汇总表信息")
public class UserAnalysisRollupVo implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	@Schema(description = "ID")
	private Long id;
	/**
	 * 用户ID
	 */
	@Schema(description = "用户ID")
	private Long userId;
	/**
	 * 分析总数
	 */
	@Schema(description = "分析总数")
	private Integer analysisSum;
	/**
	 * 日平均分析
	 */
	@Schema(description = "日平均分析")
	private Integer dayAverageAnalysis;
	/**
	 * 最后一次分析时间
	 */
	@Schema(description = "最后一次分析时间")
	private Date lastAnalysis;
	/**
	 * 创建时间
	 */
	@Schema(description = "创建时间")
	private Date createDate;
	/**
	 * 最后修改时间
	 */
	@Schema(description = "最后修改时间")
	private Date updateDate;
	/**
	 * 是否已删除
	 */
	@Schema(description = "是否已删除")
	private Integer isDeleted;
    /**
     * 对比分析次数
     */
    @Schema(description = "对比分析次数")
    private Integer contrastAnalysis;
    /**
     * 自用抖音号数量
     */
    @Schema(description = "自用抖音号数量")
    private Integer ownCount;
    /**
     * 渠道明细
     */
    @Schema(description = "渠道明细")
    private String promotionName;


}
