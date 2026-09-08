package com.jiuyu.replay.third.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 点赞问答文件上传cos记录表信息
 *
 * @author DearSil
 * @email 2437495924@qq.com
 * @date 2025-02-21 16:50:31
 */
@Data
@Schema(description = "点赞问答文件上传cos记录表信息")
public class CosThumbsFileBo implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	@Schema(description = "ID")
	private Long id;
	/**
	 * 行业id，0表示全行业
	 */
	@Schema(description = "行业id，0表示全行业")
	private Long tradeId;
	/**
	 * 用户id
	 */
	@Schema(description = "用户id")
	private Long userId;
	/**
	 * 资源id
	 */
	@Schema(description = "资源id")
	private String sourceId;

	/**
	 * 平台类型 0:单个视频 1: 文件 2：对比分析
	 */
	@Schema(description = "平台类型 0:单个视频 1: 文件 2：对比分析")
	private Integer sourceType;
	/**
	 * 文件大小
	 */
	@Schema(description = "文件大小")
	private Long fileSize;
	/**
	 * cosKey
	 */
	@Schema(description = "cosKey")
	private String coskey;
	/**
	 * 上下文Id
	 */
	@Schema(description = "上下文Id")
	private String contextId;
	/**
	 * 点赞状态 0：未点赞 1：已点赞 2：点踩
	 */
	@Schema(description = "点赞状态 0：未点赞 1：已点赞 2：点踩")
	private Integer thumbState;
	/**
	 * 文件名称
	 */
	@Schema(description = "文件名称")
	private String fileName;
	/**
	 * 问答次数
	 */
	@Schema(description = "问答次数")
	private Integer askCount;
	/**
	 * 上传时间
	 */
	@Schema(description = "上传时间")
	private Date uploadDate;
	/**
	 * 来源类型 0：系统
	 */
	@Schema(description = "来源类型 0：系统")
	private Integer resourceType;
	/**
	 * 平台类型 0：全平台 1：抖音 2：快手 3：微视
	 */
	@Schema(description = "平台类型 0：全平台 1：抖音 2：快手 3：微视")
	private Integer platformType;
	/**
	 * 回话类型 0:运营问题 1:违规
	 */
	@Schema(description = "回话类型 0:运营问题 1:违规")
	private Integer cosType;
	/**
	 * 排序
	 */
	@Schema(description = "排序")
	private Integer sort;
	/**
	 * 描述
	 */
	@Schema(description = "描述")
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
