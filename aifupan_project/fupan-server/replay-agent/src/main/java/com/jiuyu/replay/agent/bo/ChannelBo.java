package com.jiuyu.replay.agent.bo;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 用户来源渠道表信息
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-01-21 10:16:14
 */
@Data
@Schema(description = "用户来源渠道表信息")
public class ChannelBo implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	@Schema(description = "ID")
	private Long id;
	/**
	 * 父ID
	 */
	@Schema(description = "父ID")
	private Long parentId;
	/**
	 * 渠道名称
	 */
	@Schema(description = "渠道名称")
	private String channelName;
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
