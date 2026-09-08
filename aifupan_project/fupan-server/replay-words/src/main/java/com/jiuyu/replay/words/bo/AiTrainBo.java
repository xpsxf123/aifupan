package com.jiuyu.replay.words.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * AI训练信息
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-02-20 18:47:39
 */
@Data
@Schema(description = "AI训练信息")
public class AiTrainBo implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	@Schema(description = "ID")
	private Long id;
	/**
	 * 用户id
	 */
	@Schema(description = "用户id")
	private Long userId;
	/**
	 * 租户id
	 */
	@Schema(description = "租户id")
	private Long tenantId;
	/**
	 * 视频唯一标识
	 */
	@Schema(description = "视频唯一标识")
	private String videoId;
	/**
	 * 行业id
	 */
	@Schema(description = "行业id")
	private Long tradeId;
	/**
	 * 状态 0：训练中 1：管理员训练完成 2：超时训练完成
	 */
	@Schema(description = "状态 0：训练中 1：管理员训练完成 2：超时训练完成")
	private Integer aiStatus;
	/**
	 * 进步幅度 如：0.0015就是0.15%
	 */
	@Schema(description = "进步幅度 如：0.0015就是0.15%")
	private Double progressRange;
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


}
