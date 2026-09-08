package com.jiuyu.replay.words.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@Schema(description = "数据截图记录信息")
public class DataScreenshotVo implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * id
	 */
	@Schema(description = "id")
	private Long id;
	/**
	 *租户id
	 */
	@Schema(description = "租户id")
	private Long tenantId;
	/**
	 *用户id
	 */
	@Schema(description = "用户id")
	private Long userId;
	/**
	 * 数据截图code
	 */
	@Schema(description = "数据截图screenshotCode")
	private String screenshotCode;
	/**
	 * 数据类型 0视频，1文件，2对比分析
	 */
	@Schema(description = "数据类型 0视频，1文件，2对比分析")
	private Integer sourceType;
	/**
	 * 来源id
	 */
	@Schema(description = "来源id")
	private String sourceId;
	/**
	 * 图片来源 0：oss
	 */
	@Schema(description = "图片来源 0：oss")
	private Integer sourceImagesType;
	/**
	 * 上传的图片地址
	 */
	@Schema(description = "上传的图片地址")
	private String sourceImagesAddress;
	/**
	 * ai识别的内容
	 */
	@Schema(description = "ai识别的内容")
	private String aiContent;
	/**
	 * 状态；0:图片未上传，1：图片已上传，2：ai识别中，3：ai识别完成，4：ai识别失败
	 */
	@Schema(description = "状态；0:图片未上传，1：图片已上传，2：ai识别中，3：ai识别完成，4：ai识别失败")
	private Integer screenshotStatus;
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
