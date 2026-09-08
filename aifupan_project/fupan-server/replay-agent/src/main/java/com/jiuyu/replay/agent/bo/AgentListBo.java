package com.jiuyu.replay.agent.bo;


import com.jiuyu.replay.common.bo.PageBo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 代理商列表查询参数
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-10 19:12:27
 */
@Data
@Schema(description = "代理商列表查询参数")
public class AgentListBo extends PageBo implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * 代理商名称
	 */
	@Schema(description = "代理商名称")
	private String agentName;
	/**
	 * 联系人姓名/手机号
	 */
	@Schema(description = "联系人姓名/手机号")
	private String contactKeyword;
	/**
	 * 运营人员用户id
	 */
	@Schema(description = "运营人员用户id")
	private Long operationUserId;
	/**
	 * 渠道id
	 */
	@Schema(description = "渠道id")
	private Long channelId;
	/**
	 * 渠道id集合
	 */
	@Schema(description = "渠道id集合")
	private List<Long> channelIds;

    /**
     * 员工状态 0离职  1在职
     */
    @Schema(description = "员工状态 0离职  1在职")
    private Integer employeeStatus;

    /**
     * 代理商类型 0：代理商  1：渠道商
     */
    @Schema(description = "代理商类型 0：代理商  1：渠道商")
    private Integer agentType;
}
