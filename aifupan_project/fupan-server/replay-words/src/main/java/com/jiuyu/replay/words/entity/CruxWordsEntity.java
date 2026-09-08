package com.jiuyu.replay.words.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 关键词
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-06-20 16:10:09
 */
@Data
@TableName("tb_crux_words")
public class CruxWordsEntity implements Serializable {
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
	 * 来源类型 0：系统 1：客户自定义
	 */
	private Integer resourceType;
	/**
	 * 关键词类型 0：促单 1：互动 2：其他
	 */
	private Integer type;
	/**
	 * 平台类型 0：全平台 1：抖音 2：快手 3：视频号
	 */
	private Integer platformType;
	/**
	 * 关键词名字
	 */
	private String name;
	/**
	 * 行业id
	 */
	private Long tradeId;
	/**
	 * 行业id数组json
	 */
	private String tradeIdArr;
	/**
	 * 相似词，多个用_隔开
	 */
	private String similarWords;
	/**
	 * 父id
	 */
	@Schema(description = "父id")
	private Long parentId;
	/**
	 * 描述
	 */
	@Schema(description = "描述")
	private String remarks;
	/**
	 * 分组
	 */
	private String groupStr;
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
