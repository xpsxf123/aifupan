package com.jiuyu.replay.generic.bo.words.video;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Update;

import java.io.Serializable;

/**
 * 视频切片信息
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-11-03 11:39:28
 */
@Data
@Schema(description = "视频切片信息")
public class VideoSliceBo implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	@Schema(description = "ID")
	@NotNull(message = "id不能为空", groups = Update.class)
	private Long id;
	/**
	 * 用户id
	 */
	@Schema(description = "用户id")
	private Long userId;
	/**
	 * 租户id
	 */
	@Schema(description = "租户id")
	private Long tenantId;
	/**
	 * 来源id，视频id/文件id
	 */
	@Schema(description = "来源id，视频id/文件id")
	@NotBlank(message = "来源id不能为空", groups = Insert.class)
	private String sourceId;
	/**
	 * 来源类型 0：视频 1：文件
	 */
	@Schema(description = "来源类型 0：视频 1：文件")
	@NotNull(message = "来源类型不能为空", groups = Insert.class)
	private Integer sourceType;
	/**
	 * 切片视频所属原视频id
	 */
	@Schema(description = "切片视频所属原视频id")
	@NotBlank(message = "切片视频所属原视频id不能为空", groups = Insert.class)
	private String sourceParentId;
	/**
	 * 切片类型 0：复盘切片 1：短视频切片
	 */
	@Schema(description = "切片类型 0：复盘切片 1：短视频切片")
	@NotNull(message = "切片类型不能为空", groups = Insert.class)
	private Integer sliceType;
	/**
	 * 切片分类，取字典值
	 */
	@Schema(description = "切片分类，取字典值")
	@NotBlank(message = "切片分类不能为空", groups = Insert.class)
	private String sliceClass;
	/**
	 * 切片开始时间-毫秒
	 */
	@Schema(description = "切片开始时间-毫秒")
	@NotNull(message = "切片开始时间-毫秒不能为空", groups = Insert.class)
	private Long startMillisecond;
	/**
	 * 切片开始时间 mm:ss
	 */
	@Schema(description = "切片开始时间 mm:ss")
	@NotBlank(message = "切片开始时间不能为空", groups = Insert.class)
	private String startTime;
	/**
	 * 切片结束时间-毫秒
	 */
	@Schema(description = "切片结束时间-毫秒")
	@NotNull(message = "切片结束时间-毫秒不能为空", groups = Insert.class)
	private Long endMillisecond;
	/**
	 * 切片结束时间 mm:ss
	 */
	@Schema(description = "切片结束时间 mm:ss")
	@NotBlank(message = "切片结束时间不能为空", groups = Insert.class)
	private String endTime;
	/**
	 * 切片备注
	 */
	@Schema(description = "切片备注")
	private String remarks;
	/**
	 * 是否自动上传云空间 0：否 1：是
	 */
	@Schema(description = "是否自动上传云空间 0：否 1：是")
	@NotNull(message = "是否自动上传云空间不能为空", groups = Insert.class)
	private Integer isAutoUploadCloud;
	/**
	 * 路径保存类型 0：保存在原视频文件夹 1：自定义文件夹
	 */
	@Schema(description = "路径保存类型 0：保存在原视频文件夹 1：自定义文件夹")
	@NotNull(message = "路径保存类型不能为空", groups = Insert.class)
	private Integer savePathType;
	/**
	 * 保存路径
	 */
	@Schema(description = "保存路径")
	@NotBlank(message = "保存路径不能为空", groups = Insert.class)
	private String savePath;
	/**
	 * 切片视频名称
	 */
	@Schema(description = "切片视频名称")
	@NotBlank(message = "切片视频名称不能为空", groups = Insert.class)
	private String sliceVideoName;
	/**
	 * 切片时间类型 0：视频时间 1：北京时间
	 */
	@Schema(description = "切片时间类型 0：视频时间 1：北京时间")
	private Integer sliceTimeType;

}
