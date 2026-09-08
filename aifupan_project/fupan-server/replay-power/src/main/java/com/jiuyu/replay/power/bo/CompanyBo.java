package com.jiuyu.replay.power.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 公司表信息
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-21 15:26:13
 */
@Data
@Schema(description = "公司表信息")
public class CompanyBo implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * id
	 */
	@Schema(description = "id")
	private Long id;
	/**
	 * 公司名称
	 */
	@Schema(description = "公司名称")
	private String name;
	/**
	 * 注册号，唯一标识公司
	 */
	@Schema(description = "注册号，唯一标识公司")
	private String registrationNumber;
	/**
	 * 行业id
	 */
	@Schema(description = "行业id")
	private Long tradeId;
	/**
	 * 规模 ：人数
	 */
	@Schema(description = "规模 ：人数")
	private String scales;
	/**
	 * 公司联系人
	 */
	@Schema(description = "公司联系人")
	private String linkman;
	/**
	 * 公司联系电话
	 */
	@Schema(description = "公司联系电话")
	private String phones;
	/**
	 * 公司地址
	 */
	@Schema(description = "公司地址")
	private String address;
	/**
	 * 邮箱
	 */
	@Schema(description = "邮箱")
	private String email;
	/**
	 * 成立日期
	 */
	@Schema(description = "成立日期")
	private Date foundingDate;
	/**
	 * 年收入
	 */
	@Schema(description = "年收入")
	private String annualRevenue;
	/**
	 * 公司状态（0正常，1暂停营业）
	 */
	@Schema(description = "公司状态（0正常，1暂停营业）")
	private Integer status;
	/**
	 * 公司描述
	 */
	@Schema(description = "公司描述")
	private String description;
	/**
	 * 创建时间
	 */
	@Schema(description = "创建时间")
	private Date createDate;
	/**
	 * 更新时间
	 */
	@Schema(description = "更新时间")
	private Date updateDate;
	/**
	 * 是否删除 （0未删除 1删除）
	 */
	@Schema(description = "是否删除 （0未删除 1删除）")
	private Integer isDeleted;


}
