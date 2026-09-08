package com.jiuyu.replay.words.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * AI分析表信息
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-01-09 15:54:12
 */
@Data
@Schema(description = "AI分析表信息")
public class AiAnalysisBo implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	@Schema(description = "ID")
	private Long id;
	/**
	 * 视频或文件唯一标识
	 */
	@Schema(description = "视频或文件唯一标识")
	private String uuid;
	/**
	 * 会话的上下文缓存ID
	 */
	@Schema(description = "会话的缓存ID")
	private String contextId;
	/**
	 * 类型  0：录制视频 1：上传文件
	 */
	@Schema(description = "类型  0：录制视频 1：上传文件")
	private Integer type;
	/**
	 * 同一个对话中的类型 0：提示词 1：答案
	 */
	@Schema(description = "同一个对话中的类型 0：提示词 1：答案")
	private Integer textType;
	/**
	 * 排序
	 */
	@Schema(description = "排序")
	private Integer sort;
	/**
	 * json数据文件存储路径
	 */
	@Schema(description = "json数据文件存储路径")
	private String storeFileName;
	/**
	 * ossKey
	 */
	@Schema(description = "ossKey")
	private String ossKey;
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
	 * ai模型id
	 */
	@Schema(description = "ai模型id")
	private Long modelId;

	/**
	 * 给AI赋予的身份
	 */
	@Schema(description = "systemContent")
	private String aiModelRole;

	/**
	 * 需要分析的内容
	 */
	@Schema(description = "analysisContent")
	private String analysisContent;

	/**
	 * 分析的提示词
	 */
	@Schema(description = "promptWords")
	private String promptWords;

	/**
	 * 分析完毕后的答案
	 */
	@Schema(description = "answerList")
	private List<String> answerList;

	/**
	 * 是否需要以表格的形式输出内容 0：不需要，1：需要
	 */
	@Schema(description = "outPutFormat")
	private Integer outPutFormat;

}
