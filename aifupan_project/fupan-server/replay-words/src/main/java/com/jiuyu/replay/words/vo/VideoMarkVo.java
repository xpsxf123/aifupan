package com.jiuyu.replay.words.vo;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 视频标记信息
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-10-10 18:09:05
 */
@Data
@Schema(description = "视频标记信息")
public class VideoMarkVo implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	@Schema(description = "ID")
	private Long id;
	/**
	 * 视频/音频文件的唯一标识
	 */
	@Schema(description = "视频/音频文件的唯一标识")
	private String fileUuid;
	/**
	 * 标记的类型
	 */
	@Schema(description = "标记的类型")
	private Integer type;
	/**
	 * 标记类型的文字说明
	 */
	@Schema(description = "标记类型的文字说明")
	private String typeStr;
	/**
	 * 标记开始时间
	 */
	@Schema(description = "标记开始时间")
	private String startDate;
	/**
	 * 标记结束时间
	 */
	@Schema(description = "标记结束时间")
	private String endDate;
	/**
	 * 标记颜色
	 */
	@Schema(description = "标记颜色")
	private String color;
	/**
	 * 备注
	 */
	@Schema(description = "备注")
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
