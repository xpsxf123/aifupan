package com.jiuyu.replay.words.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户分析汇总表
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-01-19 16:56:41
 */
@Data
@TableName("tb_user_analysis_rollup")
public class UserAnalysisRollupEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	@TableId(value = "id", type = IdType.INPUT)
	private Long id;
	/**
	 * 用户ID
	 */
	private Long userId;
	/**
	 * 分析总数
	 */
	private Integer analysisSum;
	/**
	 * 日平均分析
	 */
	private Integer dayAverageAnalysis;
	/**
	 * 最后一次分析时间
	 */
	private Date lastAnalysis;
	/**
	 * 创建时间
	 */
	private Date createDate;
	/**
	 * 最后修改时间
	 */
	private Date updateDate;
	/**
	 * 是否已删除
	 */
	private Integer isDeleted;
    /**
     * 对比分析次数
     */
    private Integer contrastAnalysis;
    /**
     * 自用抖音号数量
     */
    private Integer ownCount;
    /**
     * 渠道明细
     */
    private String promotionName;

}
