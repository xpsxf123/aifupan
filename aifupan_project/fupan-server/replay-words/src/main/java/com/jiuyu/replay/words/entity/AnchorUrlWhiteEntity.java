package com.jiuyu.replay.words.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 主播白名单表
 *
 * @author hts
 * @email 1776764427@qq.com
 * @date 2024-09-15 09:50:55
 */
@Data
@TableName("tb_anchor_url_white")
public class AnchorUrlWhiteEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 
	 */
	@TableId(value = "id", type = IdType.INPUT)
	private Long id;
	/**
	 * 主播唯一标识
	 */
	private String secUid;
	/**
	 * 用户ID
	 */
	private Long userId;
	/**
	 * 创建时间
	 */
	private Date createDate;


}
