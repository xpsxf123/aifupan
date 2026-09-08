package com.jiuyu.replay.words.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 视频的分析记录信息
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-12-03 11:26:50
 */
@Data
@Schema(description = "视频的分析记录信息")
public class VideoAnalysisRecordBo implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	@Schema(description = "ID")
	private Long id;
	/**
	 * 用户id
	 */
	@Schema(description = "用户id")
	private Long userId;
	/**
	 * 视频唯一标识
	 */
	@Schema(description = "视频唯一标识")
	private String videoId;
	/**
	 * 行业id
	 */
	@Schema(description = "行业id")
	private Long tradeId;
	/**
	 * json数据文件存储路径
	 */
	@Schema(description = "json数据文件存储路径")
	private String storeFileName;
	/**
	 * json数据文件存储路径_2.0版本
	 */
	@Schema(description = "json数据文件存储路径_2.0版本")
	private String storeFileNameNew;
	/**
	 * json数据文件oss存储的key
	 */
	@Schema(description = "json数据文件oss存储的key")
	private String storeFileOssKey;
	/**
	 * 版本号，从0开始
	 */
	@Schema(description = "版本号，从0开始")
	private Integer version;
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
	/**
	 * 关键词数量
	 */
	@Schema(description = "关键词数量")
	private Integer cruxWordNum;
	/**
	 * 敏感词数量
	 */
	@Schema(description = "敏感词数量")
	private Integer sensitiveWordNum;
	/**
	 * 文本总字数
	 */
	@Schema(description = "文本总字数")
	private Integer contentNum;

}
