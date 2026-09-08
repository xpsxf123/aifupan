package com.jiuyu.replay.words.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 词库信息
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-10-06 16:34:13
 */
@Data
@Schema(description = "词库信息")
public class LexiconVo implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	@Schema(description = "ID")
	private Long id;
	/**
	 * 关联的用户id
	 */
	@Schema(description = "关联的用户id")
	private Long userId;
	/**
	 * 关联的行业id
	 */
	@Schema(description = "关联的行业id")
	private Long tradeId;
	/**
	 * 行业id数组json
	 */
	@Schema(description = "行业id数组json")
	private String tradeIdArr;
	/**
	 * 词库名
	 */
	@Schema(description = "词库名")
	private String name;
	/**
	 * 词库备注
	 */
	@Schema(description = "词库备注")
	private String remarks;
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
