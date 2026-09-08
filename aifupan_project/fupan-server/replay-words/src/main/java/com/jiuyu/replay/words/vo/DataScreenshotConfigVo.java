package com.jiuyu.replay.words.vo;

import com.jiuyu.replay.generic.vo.common.FileShowVo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 数据截图配置信息
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2025-03-11 17:30:33
 */
@Data
@Schema(description = "数据截图配置信息")
public class DataScreenshotConfigVo implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * id
	 */
	@Schema(description = "id")
	private Long id;
	/**
	 * code
	 */
	@Schema(description = "screenshotCode")
	private String screenshotCode;
	/**
	 * 标题
	 */
	@Schema(description = "标题")
	private String title;
	/**
	 * 备注
	 */
	@Schema(description = "备注")
	private String remarks;
	/**
	 * 排序
	 */
	@Schema(description = "排序")
	private Integer sort;
	/**
	 * 图片来源 0：oss
	 */
	@Schema(description = "图片来源 0：oss")
	private Integer sourceType;
	/**
	 * 示例图片
	 */
	@Schema(description = "示例图片")
	private String example;
	/**
	 * 示例图片列表
	 */
	@Schema(description = "示例图片列表")
	private List<FileShowVo> exampleList;
	/**
	 * 更新时间
	 */
	@Schema(description = "更新时间")
	private Date updateDate;
	/**
	 * 创建时间
	 */
	@Schema(description = "创建时间")
	private Date createDate;


}
