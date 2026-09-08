package com.jiuyu.replay.words.vo.viewing;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 数据看盘比例信息
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-04-17 17:45:33
 */
@Data
@Schema(description = "数据看盘比例信息")
public class VideoDataViewingRatioVo implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	@Schema(description = "ID")
	private Long id;
	/**
	 * 名称
	 */
	@Schema(description = "名称")
	private String ratioName;
	/**
	 * 唯一标识码
	 */
	@Schema(description = "唯一标识码")
	private String ratioCode;
	/**
	 * 比例区间-起始
	 */
	@Schema(description = "比例区间-起始")
	private Double ratioStart;
	/**
	 * 比例区间-结束
	 */
	@Schema(description = "比例区间-结束")
	private Double ratioEnd;
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
