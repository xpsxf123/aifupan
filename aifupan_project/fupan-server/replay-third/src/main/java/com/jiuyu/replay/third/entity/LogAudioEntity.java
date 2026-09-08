package com.jiuyu.replay.third.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * qps调用记录表
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-11-08 16:27:43
 */
@Data
@TableName("tb_log_audio")
public class LogAudioEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	@TableId(value = "id", type = IdType.INPUT)
	private Long id;
	/**
	 * 用户id
	 */
	private Long userId;
	/**
	 * 调用时间
	 */
	private Date callDate;
	/**
	 * 调用状态 0：成功 1：失败
	 */
	private Integer status;
	/**
	 * 调了多少次才成功
	 */
	private Integer successNum;
	/**
	 * 失败状态码
	 */
	private String errCode;
	/**
	 * 失败原因
	 */
	private String errMsg;
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


}
