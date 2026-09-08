package com.jiuyu.replay.generic.vo.agent;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 代理商信息
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-10 19:12:27
 */
@Data
@Schema(description = "代理商信息")
public class AgentVo implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	@Schema(description = "ID")
	private Long id;
	/**
	 * 代理商名称
	 */
	@Schema(description = "代理商名称")
	private String agentName;
	/**
	 * 父代理商id
	 */
	@Schema(description = "父代理商id")
	private Long parentId;
	/**
	 * 代理商类型 0：普通代理商 1：渠道代理商
	 */
	@Schema(description = "代理商类型 0：普通代理商 1：渠道代理商")
	private Integer agentType;
	/**
	 * 渠道id
	 */
	@Schema(description = "渠道id")
	private Long channelId;
	/**
	 * 运营人员用户id
	 */
	@Schema(description = "运营人员用户id")
	private Long operationUserId;
	/**
	 * 联系人姓名
	 */
	@Schema(description = "联系人姓名")
	private String contactName;
	/**
	 * 联系人手机号
	 */
	@Schema(description = "联系人手机号")
	private String contactPhone;
	/**
	 * 联系地址
	 */
	@Schema(description = "联系地址")
	private String contactAddress;
	/**
	 * 行业id
	 */
	@Schema(description = "行业id")
	private Long tradeId;
	/**
	 * 新签佣金比例
	 */
	@Schema(description = "新签佣金比例")
	private Double commissionRate;
	/**
	 * 续费佣金比例
	 */
	@Schema(description = "续费佣金比例")
	private Double renewalCommissionRate;
	/**
	 * 代理商状态 0：未启用 1：启用中
	 */
	@Schema(description = "代理商状态 0：未启用 1：启用中")
	private Integer agentStatus;
	/**
	 * 创建人用户id
	 */
	@Schema(description = "创建人用户id")
	private Long createUserId;
	/**
	 * 代理商URL链接code码
	 */
	@Schema(description = "代理商URL链接code码")
	private String agentUrlCode;
	/**
	 * 海报图片文件id，多个用_隔开
	 */
	@Schema(description = "海报图片文件id，多个用_隔开")
	private String posterImgIds;
	/**
	 * 按钮颜色
	 */
	@Schema(description = "按钮颜色")
	private String btnBgColor;
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
