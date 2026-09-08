package com.jiuyu.replay.agent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 代理商
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-10 19:12:27
 */
@Data
@TableName("tb_agent")
public class AgentEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	@TableId(value = "id", type = IdType.INPUT)
	private Long id;
	/**
	 * 代理商名称
	 */
	private String agentName;
	/**
	 * 父代理商id
	 */
	private Long parentId;
	/**
	 * 代理商类型 0：普通代理商 1：渠道代理商
	 */
	private Integer agentType;
	/**
	 * 渠道id
	 */
	private Long channelId;
	/**
	 * 运营人员用户id
	 */
	private Long operationUserId;
	/**
	 * 联系人姓名
	 */
	private String contactName;
	/**
	 * 联系人手机号
	 */
	private String contactPhone;
	/**
	 * 联系地址
	 */
	private String contactAddress;
	/**
	 * 行业id
	 */
	private Long tradeId;
	/**
	 * 新签佣金比例
	 */
	private Double commissionRate;
	/**
	 * 续费佣金比例
	 */
	private Double renewalCommissionRate;
	/**
	 * 代理商状态 0：未启用 1：启用中
	 */
	private Integer agentStatus;
	/**
	 * 创建人用户id
	 */
	private Long createUserId;
	/**
	 * 代理商URL链接code码
	 */
	private String agentUrlCode;
	/**
	 * 海报图片文件id，多个用_隔开
	 */
	private String posterImgIds;
	/**
	 * 按钮颜色
	 */
	private String btnBgColor;
	/**
	 * 按钮文案
	 */
	private String btContent;
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
     * 用户id
     */
    private Long userId;

    /**
     * 员工状态 0离职  1在职
     */
    private Integer employeeStatus;
}
