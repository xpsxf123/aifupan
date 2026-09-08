package com.jiuyu.replay.order.bo;


import com.jiuyu.replay.common.bo.PageBo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 邀请码列表查询参数
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-10-15 15:57:30
 */
@Data
@Schema(description = "邀请码列表查询参数")
public class InvitationCodeListBo extends PageBo implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * 模糊搜索查询条件
	 */
	@Schema(description = "模糊搜索查询条件")
	private String code;
	/**
	 * 批次id
	 */
	@Schema(description = "批次id")
	private Long codeBatchId;
	/**
	 * 使用状态
	 */
	@Schema(description = "使用状态")
	private Integer useStatus;
	/**
	 * 状态
	 */
	@Schema(description = "状态")
	private Integer status;

	@Schema(description = "不包含的批次id")
	private List<Long> notCodeBatchIds;

}
