package com.jiuyu.replay.order.bo;


import com.jiuyu.replay.common.bo.PageBo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 邀请码-批次列表查询参数
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-10-15 15:57:30
 */
@Data
@Schema(description = "邀请码-批次列表查询参数")
public class InvitationCodeBatchListBo extends PageBo implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * 模糊搜索查询条件
	 */
	@Schema(description = "模糊搜索查询条件")
	private String keyword;
	/**
	 * 按状态筛选
	 */
	@Schema(description = "按状态筛选")
	private Integer status;

	@Schema(description = "不包含类型")
	private List<Integer> notTypeList;

	@Schema(description = "类型 0：机构码 1：个人码  2：激活码")
	private Integer type;

    @Schema(description = "渠道id")
    private Long channelId;
}
