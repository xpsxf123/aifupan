package com.jiuyu.replay.third.bo;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * qps调用记录表信息
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-11-08 16:27:43
 */
@Data
@Schema(description = "qps调用记录表信息")
public class LogAudioBo implements Serializable {
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
	 * 调用时间
	 */
	@Schema(description = "调用时间")
	private Date callDate;
	/**
	 * 调用状态 0：成功 1：失败
	 */
	@Schema(description = "调用状态 0：成功 1：失败")
	private Integer status;
	/**
	 * 调了多少次才成功
	 */
	@Schema(description = "调了多少次才成功")
	private Integer successNum;
	/**
	 * 失败状态码
	 */
	@Schema(description = "失败状态码")
	private String errCode;
	/**
	 * 失败原因
	 */
	@Schema(description = "失败原因")
	private String errMsg;
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
