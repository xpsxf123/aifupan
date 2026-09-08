package com.jiuyu.replay.words.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 信息
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-09-14 09:15:19
 */
@Data
@Schema(description = "信息")
public class UploadFileRecodVo implements Serializable {
	private static final long serialVersionUID = 1L;


	/**
	 * 文件名称
	 */
	@Schema(description = "文件名称")
	private String fileName;
	/**
	 * 文件id
	 */
	@Schema(description = "文件id")
	private Long fileId;
	/**
	 * 文件类型
	 */
	@Schema(description = "文件类型")
	private Integer fileType;
	/**
	 * 时长
	 */
	@Schema(description = "时长")
	private Long fileDuration;
	/**
	 * 行业id
	 */
	@Schema(description = "行业id")
	private Integer tradeId;
	/**
	 * 用户id
	 */
	@Schema(description = "用户id")
	private Long userId;
	/**
	 * 总字数
	 */
	@Schema(description = "总字数")
	private Long sum;
	/**
	 * 上传时间
	 */
	@Schema(description = "上传时间")
	private Date uploadTime;


}
